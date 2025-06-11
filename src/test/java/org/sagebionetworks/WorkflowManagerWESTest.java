package org.sagebionetworks;

import static org.sagebionetworks.Utils.getTempDir;

import java.io.ByteArrayOutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class WorkflowManagerWESTest {

	@Before
	public void setUp() throws Exception {
		System.setProperty("WES_ENDPOINT", "http://localhost:8082/ga4gh/wes/v1");
		System.setProperty("SYNAPSE_USERNAME", "---------------");
		System.setProperty("SYNAPSE_PAT", "---------------");
	}
	
	@After
	public void tearDown() throws Exception {
		System.clearProperty("WES_ENDPOINT");
	}

	@Ignore
	@Test
	public void testCreateJob() throws Exception {
		WorkflowManagerWES wdm = new WorkflowManagerWES(new WorkflowURLDownloader());
		String workflowUrlString = "https://github.com/Sage-Bionetworks/SynapseWorkflowExample/archive/master.zip";
		String entrypoint = "SynapseWorkflowExample-master/workflow-entrypoint.cwl";
		String submissionId = "9687029";
		String adminUploadSynId = "syn16936498";
		String submitterUploadSynId = "syn16936496";
		String synapseWorkflowReference = "syn16936386";
		WorkflowParameters workflowParameters = 
				new WorkflowParameters(submissionId, synapseWorkflowReference, adminUploadSynId, submitterUploadSynId);

		byte[] synapseConfigFileContent;
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			Utils.writeSynapseConfigFile(baos);
			synapseConfigFileContent = baos.toByteArray();
		}
		wdm.createWorkflowJob(workflowUrlString, entrypoint, workflowParameters, synapseConfigFileContent);
	}

	@Ignore
	@Test
	public void testCheckStatus() throws Exception {
		WorkflowManagerWES wdm = new WorkflowManagerWES(new WorkflowURLDownloader());
		List<WorkflowJob> jobs = wdm.listWorkflowJobs(null);
		System.out.println("Found "+jobs.size()+" jobs.");
		for (WorkflowJob job : jobs) {
			System.out.println("\nJob: "+job.getWorkflowId());
			WorkflowStatus status = wdm.getWorkflowStatus(job);
			System.out.println("status: "+status);
			Path logFile = FileSystems.getDefault().getPath(getTempDir().getAbsolutePath(), job.getWorkflowId()+"_log.txt");
			String logTail = wdm.getWorkflowLog(job, logFile, 100000);
			System.out.println("Logs:");
			System.out.println(logTail);
		}
	}

	@Ignore
	@Test
	public void testDeleteAllJobs() throws Exception {
		WorkflowManagerWES wdm = new WorkflowManagerWES(new WorkflowURLDownloader());

		List<WorkflowJob> jobs = wdm.listWorkflowJobs(null);
		System.out.println("Found "+jobs.size()+" jobs.");
		
		for (WorkflowJob job : jobs) {
			wdm.deleteWorkFlowJob(job);
		}
		
		System.out.println("Deleted "+jobs.size()+" jobs.");
	}


}
