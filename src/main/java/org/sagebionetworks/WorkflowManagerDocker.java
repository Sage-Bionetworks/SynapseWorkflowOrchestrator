package org.sagebionetworks;
import static org.sagebionetworks.Constants.AGENT_SHARED_DIR_PROPERTY_NAME;
import static org.sagebionetworks.Constants.DOCKER_CERT_PATH_HOST_PROPERTY_NAME;
import static org.sagebionetworks.Constants.DOCKER_ENGINE_URL_PROPERTY_NAME;
import static org.sagebionetworks.Constants.DUMP_PROGRESS_SHELL_COMMAND;
import static org.sagebionetworks.Constants.NUMBER_OF_PROGRESS_CHARACTERS;
import static org.sagebionetworks.Constants.RUN_WORKFLOW_CONTAINER_IN_PRIVILEGED_MODE_PROPERTY_NAME;
import static org.sagebionetworks.Constants.SHARED_VOLUME_NAME;
import static org.sagebionetworks.Constants.TOIL_CLI_OPTIONS_PROPERTY_NAME;
import static org.sagebionetworks.Constants.UNIX_SOCKET_PREFIX;
import static org.sagebionetworks.Constants.WORKFLOW_ENGINE_DOCKER_IMAGES_PROPERTY_NAME;
import static org.sagebionetworks.DockerUtils.PROCESS_TERMINATED_ERROR_CODE;
import static org.sagebionetworks.Utils.WORKFLOW_FILTER;
import static org.sagebionetworks.Utils.archiveContainerName;
import static org.sagebionetworks.Utils.createTempFile;
import static org.sagebionetworks.Utils.dockerComposeName;
import static org.sagebionetworks.Utils.findRunningWorkflowJobs;
import static org.sagebionetworks.Utils.getProperty;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.github.dockerjava.api.command.InspectContainerResponse.ContainerState;
import com.github.dockerjava.api.model.Container;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class WorkflowManagerDocker implements WorkflowManager {
	private static Logger log = LoggerFactory.getLogger(WorkflowManagerDocker.class);

	private DockerUtils dockerUtils;
	private Utils utils;
	
	static {
		System.setProperty("https.protocols", "TLSv1,TLSv1.1,TLSv1.2"); // needed for some https resources
	}
		
	public WorkflowManagerDocker(DockerUtils dockerUtils) {
		this.dockerUtils=dockerUtils;
		this.utils = new Utils();
	}
	
	private ContainerRelativeFile createDirInHostMountedSharedDir() {
		String name = UUID.randomUUID().toString();
		String mountPoint = dockerUtils.getVolumeMountPoint(dockerComposeName(SHARED_VOLUME_NAME));
		ContainerRelativeFile result = new ContainerRelativeFile(name, 
				new File(System.getProperty(AGENT_SHARED_DIR_PROPERTY_NAME)), new File(mountPoint));
		File dir = result.getContainerPath();
		if (!dir.mkdir()) throw new RuntimeException("Unable to create "+dir.getAbsolutePath());
		return result;
	}
	
	
	private ContainerRelativeFile createWorkflowParametersYamlFile(WorkflowParameters params, ContainerRelativeFile targetFolder,
			File hostSynapseConfig) throws IOException {
		File workflowParameters = createTempFile(".yaml", targetFolder.getContainerPath());
		try (FileOutputStream fos = new FileOutputStream(workflowParameters)) {
			IOUtils.write("submissionId: "+params.getSubmissionId()+"\n", fos, StandardCharsets.UTF_8);
			IOUtils.write("workflowSynapseId: "+params.getSynapseWorkflowReference()+"\n", fos, StandardCharsets.UTF_8);
			IOUtils.write("submitterUploadSynId: "+params.getSubmitterUploadSynId()+"\n", fos, StandardCharsets.UTF_8);
			IOUtils.write("adminUploadSynId: "+params.getAdminUploadSynId()+"\n", fos, StandardCharsets.UTF_8);
			IOUtils.write("synapseConfig:\n  class: File\n  path: "+hostSynapseConfig.getAbsolutePath()+"\n", fos, StandardCharsets.UTF_8);
		}
		return new ContainerRelativeFile(workflowParameters.getName(), targetFolder.getContainerPath(), targetFolder.getHostPath());
	}

	private static final String[] DISALLOWED_USER_TOIL_PARAMS = {
			"LinkImports", // we apply noLinkImports.  User must use neither LinkImports nor noLinkImports
			"workDir" // we specify workDir.  User must not
	};
	
	/**
	 * This is analogous to POST /workflows in WES
	 * @param workflowUrlString the URL to the archive of workflow files
	 * @param entrypoint the entry point (a file path) within an unzipped workflow archive
	 * @param workflowParameters the parameters to be passed to the workflow
	 * @return the created workflow job
	 * @throws IOException
	 */
	@Override
	public WorkflowJob createWorkflowJob(String workflowUrlString, String entrypoint,
			WorkflowParameters workflowParameters, byte[] synapseConfigFileContent) throws IOException {
		ContainerRelativeFile workflowFolder = createDirInHostMountedSharedDir();

		utils.downloadWorkflowFromURL(workflowUrlString, entrypoint, workflowFolder.getContainerPath());
		
		// The folder with the workflow and param's, from the POV of the host
		File hostWorkflowFolder = workflowFolder.getHostPath();
		// To run Toil inside Docker we need to make certain settings as explained here:
		// https://github.com/brucehoff/wiki/wiki/Problem-running-Toil-in-a-container
		// For one thing, the path to the folder from the workflow's POV must be the SAME as from the host's POV.
		File workflowRunnerWorkflowFolder = hostWorkflowFolder;
		
		// write the synapse config file into the workflow folder
		// this is NOT secure but there's no good option today
		try (FileOutputStream fos=new FileOutputStream(new File(workflowFolder.getContainerPath(), ".synapseConfig"))) {
			IOUtils.write(synapseConfigFileContent, fos);
		}
		
		// Note that we create the param's file within the folder to which we've downloaded the workflow template
		// This gives us a single folder to mount to the Toil container
		ContainerRelativeFile workflowParametersFile = createWorkflowParametersYamlFile(workflowParameters, workflowFolder, 
				new File(workflowFolder.getHostPath(), ".synapseConfig"));
		
		List<String> cmd = new ArrayList<String>();
		cmd.add("toil-cwl-runner");

		String userToilParams = getProperty(TOIL_CLI_OPTIONS_PROPERTY_NAME, false);
		if (StringUtils.isEmpty(userToilParams)) userToilParams="";
		for (String disallowed : DISALLOWED_USER_TOIL_PARAMS) {
			if (userToilParams.toLowerCase().contains(disallowed.toLowerCase())) {
				throw new IllegalArgumentException("may not specify "+disallowed+" in Toil CLI options.");
			}
		}
		for (String toilParam : userToilParams.split("\\s+")) {
			if (!StringUtils.isEmpty(toilParam)) cmd.add(toilParam);
		}
		
		// further, we must set 'workDir' and 'noLinkImports':
		cmd.addAll(Arrays.asList("--workDir",  
				workflowRunnerWorkflowFolder.getAbsolutePath(), 
				"--noLinkImports",
				(new File(workflowRunnerWorkflowFolder.getAbsolutePath(), entrypoint)).getAbsolutePath(),
				workflowParametersFile.getHostPath().getAbsolutePath()
				));

		Map<File,String> rwVolumes = new HashMap<File,String>();
		
		String containerName = Utils.createContainerName();
		String containerId = null;
		Map<File,String> roVolumes = new HashMap<File,String>();
		
		List<String> containerEnv = new ArrayList<String>();
		
		// workDir doesn't seem to work.  Let's try TMPDIR, TEMP, TMP, as per
		// https://toil.readthedocs.io/en/latest/running/cliOptions.html
		containerEnv.add("TMPDIR="+workflowRunnerWorkflowFolder.getAbsolutePath());
		containerEnv.add("TEMP="+workflowRunnerWorkflowFolder.getAbsolutePath());
		containerEnv.add("TMP="+workflowRunnerWorkflowFolder.getAbsolutePath());
		
		
		// pass Docker daemon URL and cert's folder, if any, so the container we launch can run Docker too
		// the volume mount and env setting to let the Docker client access the daemon are defined here:
		// https://docs.docker.com/engine/security/https/
		String dockerHost = getProperty(DOCKER_ENGINE_URL_PROPERTY_NAME);
		containerEnv.add("DOCKER_HOST="+dockerHost);
		// mount certs folder, if any
		String hostCertsFolder = getProperty(DOCKER_CERT_PATH_HOST_PROPERTY_NAME, false);
		if (!StringUtils.isEmpty(hostCertsFolder)) {
			roVolumes.put(new File(hostCertsFolder), "/root/.docker/");
		}
		
		if (dockerHost.startsWith(UNIX_SOCKET_PREFIX)) {
			String volumeToMount = dockerHost.substring(UNIX_SOCKET_PREFIX.length());
			log.info("Mounting: "+volumeToMount);
			rwVolumes.put(new File(volumeToMount), volumeToMount);
		}

		rwVolumes.put(hostWorkflowFolder, workflowRunnerWorkflowFolder.getAbsolutePath());
		String workingDir = workflowRunnerWorkflowFolder.getAbsolutePath();
		
		log.info("workingDir: "+workingDir);
		log.info("toil cmd: "+cmd);
		
		String workflowEngineImage = getProperty(WORKFLOW_ENGINE_DOCKER_IMAGES_PROPERTY_NAME, false);
		// normally would use quay.io ("quay.io/ucsc_cgl/toil")
		// but sagebionetworks/synapse-workflow-orchestrator-toil:1.0 incorporates the Synapse client as well at Toil and Docker
		if (StringUtils.isEmpty(workflowEngineImage)) workflowEngineImage = "sagebionetworks/synapse-workflow-orchestrator-toil";
		
		boolean privileged = new Boolean(getProperty(RUN_WORKFLOW_CONTAINER_IN_PRIVILEGED_MODE_PROPERTY_NAME, false));
		
		try {
			containerId = dockerUtils.createModelContainer(
					workflowEngineImage,
					containerName, 
					roVolumes,
					rwVolumes, 
					containerEnv,
					cmd,
					workingDir,
					privileged);

			dockerUtils.startContainer(containerId);
		} catch (DockerPullException e) {
			if (containerId!=null) dockerUtils.removeContainer(containerId, true);

			throw e;
		}

		WorkflowJobDocker workflowJob = new WorkflowJobDocker();
		workflowJob.setContainerName(containerName);
		return workflowJob;
	}

	/*
	 * This is analogous to GET /workflows in WES
	 */
	@Override
	public List<WorkflowJob> listWorkflowJobs(Boolean running) {
		return findRunningWorkflowJobs(dockerUtils.listContainers(WORKFLOW_FILTER, running));
	}
	
	/*
	 * This is analogous to GET /workflows/{workflow_id}/status in WES
	 */
	@Override
	public WorkflowStatus getWorkflowStatus(WorkflowJob job) throws IOException {
		WorkflowJobDocker j = (WorkflowJobDocker)job;
		Container container = j.getContainer();
		WorkflowStatus result = new WorkflowStatus();
		ContainerState containerState = dockerUtils.getContainerState(container.getId());
		result.setRunning(containerState.getRunning());

		ExitStatus exitStatus = null;
		int exitCode = containerState.getExitCode();
		if (exitCode==0) {
			exitStatus=ExitStatus.SUCCESS;
		} else if (exitCode==PROCESS_TERMINATED_ERROR_CODE) {
			exitStatus=ExitStatus.CANCELED;
		} else {
			exitStatus=ExitStatus.FAILURE;
		}
		result.setExitStatus(exitStatus);
		if (containerState.getRunning()) {
			String execOutput = dockerUtils.exec(container.getId(), DUMP_PROGRESS_SHELL_COMMAND);
			result.setProgress(Utils.getProgressPercentFromString(execOutput, NUMBER_OF_PROGRESS_CHARACTERS));        		
		}
		return result;
	}
	
	/*
	 * This would be provided by GET /workflows/{workflow_id} WES
	 * 
	 * Writes full log to a file and optionally returns just the tail (if 'maxTailLengthInCharacters' is not null)
	 */
	@Override
	public String getWorkflowLog(WorkflowJob job, Path outPath, Integer maxTailLengthInCharacters) throws IOException {
		WorkflowJobDocker j = (WorkflowJobDocker)job;
		return dockerUtils.getLogs(j.getContainer().getId(), outPath, maxTailLengthInCharacters);
	}
	
	/*
	 * This has no analogy in WES.  The idea is to have a state for a workflow in which it is
	 * interrupted but not deleted.  In this state the logs can be interrogated to show just
	 * where the interruption occurred.
	 */
	@Override
	public void stopWorkflowJob(WorkflowJob job) {
		WorkflowJobDocker j = (WorkflowJobDocker)job;
		ContainerState containerState = dockerUtils.getContainerState(j.getContainer().getId());
		if (containerState.getRunning()) {
			dockerUtils.stopContainerWithRetry(j.getContainer().getId());			
		}		
	}

	/*
	 * This is analogous to DELETE /workflows/{workflow_id} in WES
	 */
	@Override
	public void deleteWorkFlowJob(WorkflowJob job) {
		// stop it if it's running
		stopWorkflowJob(job);
		
		// now delete or archive the job
		WorkflowJobDocker j = (WorkflowJobDocker)job;
		if (Constants.ARCHIVE_CONTAINER) {
			dockerUtils.renameContainer(j.getContainer().getId(), archiveContainerName(j.getContainerName()));
		} else {
			dockerUtils.removeContainer(j.getContainer().getId(), true);
		}
	}

}
