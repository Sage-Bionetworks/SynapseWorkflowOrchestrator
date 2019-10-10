package org.sagebionetworks;

import static org.sagebionetworks.Constants.ACCEPT_NEW_SUBMISSIONS_PROPERTY_NAME;
import static org.sagebionetworks.Constants.DEFAULT_MAX_CONCURRENT_WORKFLOWS;
import static org.sagebionetworks.Constants.DOCKER_ENGINE_URL_PROPERTY_NAME;
import static org.sagebionetworks.Constants.MAX_CONCURRENT_WORKFLOWS_PROPERTY_NAME;
import static org.sagebionetworks.Constants.MAX_LOG_ANNOTATION_CHARS;
import static org.sagebionetworks.Constants.NOTIFICATION_PRINCIPAL_ID;
import static org.sagebionetworks.Constants.ROOT_TEMPLATE_ANNOTATION_NAME;
import static org.sagebionetworks.Constants.SUBMISSION_COMPLETED;
import static org.sagebionetworks.Constants.SUBMISSION_FAILED;
import static org.sagebionetworks.Constants.SUBMISSION_STARTED;
import static org.sagebionetworks.Constants.SUBMISSION_STOPPED_BY_USER;
import static org.sagebionetworks.Constants.SUBMISSION_TIMED_OUT;
import static org.sagebionetworks.Constants.SYNAPSE_PASSWORD_PROPERTY;
import static org.sagebionetworks.Constants.SYNAPSE_USERNAME_PROPERTY;
import static org.sagebionetworks.Constants.WES_ENDPOINT_PROPERTY_NAME;
import static org.sagebionetworks.EvaluationUtils.ADMIN_ANNOTS_ARE_PRIVATE;
import static org.sagebionetworks.EvaluationUtils.FAILURE_REASON;
import static org.sagebionetworks.EvaluationUtils.JOB_LAST_UPDATED_TIME_STAMP;
import static org.sagebionetworks.EvaluationUtils.JOB_STARTED_TIME_STAMP;
import static org.sagebionetworks.EvaluationUtils.LAST_LOG_UPLOAD;
import static org.sagebionetworks.EvaluationUtils.LOG_FILE_SIZE_EXCEEDED;
import static org.sagebionetworks.EvaluationUtils.PROGRESS;
import static org.sagebionetworks.EvaluationUtils.PUBLIC_ANNOTATION_SETTING;
import static org.sagebionetworks.EvaluationUtils.STATUS_DESCRIPTION;
import static org.sagebionetworks.EvaluationUtils.SUBMISSION_ARTIFACTS_FOLDER;
import static org.sagebionetworks.EvaluationUtils.SUBMISSION_PROCESSING_STARTED_SENT;
import static org.sagebionetworks.EvaluationUtils.WORKFLOW_JOB_ID;
import static org.sagebionetworks.EvaluationUtils.applyModifications;
import static org.sagebionetworks.EvaluationUtils.getFinalSubmissionState;
import static org.sagebionetworks.EvaluationUtils.getInProgressSubmissionState;
import static org.sagebionetworks.EvaluationUtils.getInitialSubmissionState;
import static org.sagebionetworks.EvaluationUtils.setStatus;
import static org.sagebionetworks.MessageUtils.SUBMISSION_PIPELINE_FAILURE_SUBJECT;
import static org.sagebionetworks.MessageUtils.SUBMISSION_PROCESSING_STARTED_SUBJECT;
import static org.sagebionetworks.MessageUtils.WORKFLOW_COMPLETE_SUBJECT;
import static org.sagebionetworks.MessageUtils.WORKFLOW_FAILURE_SUBJECT;
import static org.sagebionetworks.MessageUtils.createPipelineFailureMessage;
import static org.sagebionetworks.MessageUtils.createSubmissionStartedMessage;
import static org.sagebionetworks.MessageUtils.createWorkflowCompleteMessage;
import static org.sagebionetworks.MessageUtils.createWorkflowFailedMessage;
import static org.sagebionetworks.Utils.getProperty;
import static org.sagebionetworks.Utils.notificationEnabled;
import static org.sagebionetworks.WorkflowUpdateStatus.DONE;
import static org.sagebionetworks.WorkflowUpdateStatus.ERROR_ENCOUNTERED_DURING_EXECUTION;
import static org.sagebionetworks.WorkflowUpdateStatus.IN_PROGRESS;
import static org.sagebionetworks.WorkflowUpdateStatus.STOPPED_TIME_OUT;
import static org.sagebionetworks.WorkflowUpdateStatus.STOPPED_UPON_REQUEST;

import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.exceptions.SynapseConflictingUpdateException;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.evaluation.model.SubmissionBundle;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.auth.LoginRequest;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class WorkflowOrchestrator  {
	private static Logger log = LoggerFactory.getLogger(WorkflowOrchestrator.class);

	private static final String LOGS_SUFFIX = "_logs";
	private static final long UPLOAD_PERIOD_MILLIS = 30*60*1000L; // 30 min in millis

	private SynapseClient synapse;
	private EvaluationUtils evaluationUtils;
	private SubmissionUtils submissionUtils;
	private MessageUtils messageUtils;
	private Archiver archiver;
	private ShutdownHook shutdownHook;
	private long sleepTimeMillis;
	private WorkflowManager workflowManager;

	public static void main( String[] args ) throws Throwable {
		SynapseClient synapse = SynapseClientFactory.createSynapseClient();
		String userName = getProperty(SYNAPSE_USERNAME_PROPERTY);
		String password = getProperty(SYNAPSE_PASSWORD_PROPERTY);
		LoginRequest loginRequest = new LoginRequest();
		loginRequest.setUsername(userName);
		loginRequest.setPassword(password);
		synapse.login(loginRequest);
		EvaluationUtils evaluationUtils = new EvaluationUtils(synapse);
		DockerUtils dockerUtils = configuredForDocker() ? new DockerUtils() : null;
		SubmissionUtils submissionUtils = new SubmissionUtils(synapse);
		long sleepTimeMillis = 10*1000L;
		WorkflowOrchestrator agent = new WorkflowOrchestrator(
				synapse, evaluationUtils,
				dockerUtils, submissionUtils, sleepTimeMillis);
		agent.execute();
		log.info("At end of 'main'");
	}
	
	private static boolean configuredForDocker() {
		return StringUtils.isNotEmpty(getProperty(DOCKER_ENGINE_URL_PROPERTY_NAME, false));
	}

	private static boolean configuredForWES() {
		return StringUtils.isNotEmpty(getProperty(WES_ENDPOINT_PROPERTY_NAME, false));
	}

	public WorkflowOrchestrator(SynapseClient synapse, 
			EvaluationUtils evaluationUtils, 
			DockerUtils dockerUtils, 
			SubmissionUtils submissionUtils,
			long sleepTimeMillis) throws SynapseException {
		this.sleepTimeMillis=sleepTimeMillis;
		this.shutdownHook = new ShutdownHook(Thread.currentThread());
		this.synapse=synapse;
		this.evaluationUtils=evaluationUtils;
		this.submissionUtils=submissionUtils;
		this.messageUtils=new MessageUtils(synapse);
		if (configuredForDocker()) {
			if (configuredForWES()) throw new IllegalStateException("Cannot configure both Docker Engine and WES Endpoint.");
			// precheck
			dockerUtils.getInfo();
			this.workflowManager = new WorkflowManagerDocker(dockerUtils);
		} else if (configuredForWES()) {
			this.workflowManager = new WorkflowManagerWES();
		} else {
			throw new IllegalStateException("Must configure either Docker Engine or WES Endpoint.");
		}
		this.archiver = new Archiver(synapse, workflowManager);

		log.info("Precheck completed successfully.");

	}
	
	public static List<String> getEvaluationIds() throws JSONException {
		return new ArrayList<String>(getTemplateSynapseIds().keySet());
	}

	public static Map<String,String> getTemplateSynapseIds() throws JSONException {
		String json = getProperty("EVALUATION_TEMPLATES");
		JSONObject templateMap = new JSONObject(json);
		Map<String,String> result = new HashMap<String,String>();
		for (Iterator<String> evaluationIdIterator=templateMap.keys(); evaluationIdIterator.hasNext();) {
			String evaluationId = evaluationIdIterator.next();
			String templateId = templateMap.getString(evaluationId);
			Utils.validateSynapseId(templateId);
			result.put(evaluationId, templateId);
		}
		return result;
	}

	public Map<String,WorkflowURLEntrypointAndSynapseRef> getWorkflowURLAndEntrypoint() throws Exception {
		Map<String,String> evaluationToSynIDMap = getTemplateSynapseIds();
		Map<String,WorkflowURLEntrypointAndSynapseRef> result = new HashMap<String,WorkflowURLEntrypointAndSynapseRef>();
		for (String evaluationId : evaluationToSynIDMap.keySet()) {
			String entityId = evaluationToSynIDMap.get(evaluationId);
			FileHandleResults fhr = synapse.getEntityFileHandlesForCurrentVersion(entityId);
			ExternalFileHandle efh = null;
			for (FileHandle fh : fhr.getList()) {
				if (fh instanceof ExternalFileHandle) {
					if (efh!=null) throw new IllegalStateException("Expected one but found two external file handles in entity "+entityId+":  \n"+fhr);
					efh = (ExternalFileHandle)fh;
				}
			}
			if (efh==null) throw new IllegalStateException("Expected external file handle in entity "+entityId+":  \n"+fhr);
			String urlString = efh.getExternalURL();
			URL url = new URL(urlString);
			// get annotation for the CWL entry point.  Does the file exist?
			Annotations annotations = synapse.getAnnotations(entityId);
			Map<String, List<String>> annotationsMap = annotations.getStringAnnotations();
			if (annotationsMap==null) throw 
				new IllegalStateException("Expected string annotation called "+
					ROOT_TEMPLATE_ANNOTATION_NAME+" for "+entityId+" but the entity has no string annotations.");
			List<String> stringAnnotations = annotationsMap.get(ROOT_TEMPLATE_ANNOTATION_NAME);
			if (stringAnnotations==null || stringAnnotations.isEmpty()) throw 
				new IllegalStateException(entityId+" has no string annotation called "+ROOT_TEMPLATE_ANNOTATION_NAME);
			String rootTemplateString = stringAnnotations.get(0);
			result.put(evaluationId, new WorkflowURLEntrypointAndSynapseRef(url, rootTemplateString, entityId));
		}
		return result;
	}
	
	private String myOwnPrincipalId = null;
	
	private String getNotificationPrincipalId() throws SynapseException {
		String id = getProperty(NOTIFICATION_PRINCIPAL_ID, false);
		if (!StringUtils.isEmpty(id)) return id;
		// if not set then just use my own ID
		if (myOwnPrincipalId!=null) return myOwnPrincipalId;
		myOwnPrincipalId=synapse.getMyProfile().getOwnerId();
		return myOwnPrincipalId;
	}

	public void execute() throws Throwable {
		Map<String,WorkflowURLEntrypointAndSynapseRef> evaluationIdToTemplateMap = getWorkflowURLAndEntrypoint();
		while (!shutdownHook.shouldShutDown()) { // this allows a system shut down to shut down the agent
			log.info("Top level loop: checking progress or starting new job.");

			
			String acceptNewSubmissionsString = getProperty(ACCEPT_NEW_SUBMISSIONS_PROPERTY_NAME, false);
			if (StringUtils.isEmpty(acceptNewSubmissionsString) || Boolean.getBoolean(acceptNewSubmissionsString)) {
				for (String evaluationId : getEvaluationIds()) {
					WorkflowURLEntrypointAndSynapseRef workflow = evaluationIdToTemplateMap.get(evaluationId);
					createNewWorkflowJobs(evaluationId, workflow);
				}
			}
			updateWorkflowJobs(getEvaluationIds());

			try {
				Thread.sleep(sleepTimeMillis);
			} catch (InterruptedException e) {
				// continue
			}
		} // end while()
	} // end execute()
	
	private static int getMaxConcurrentWorkflows() {
		String maxString = getProperty(MAX_CONCURRENT_WORKFLOWS_PROPERTY_NAME, false);
		if (StringUtils.isEmpty(maxString)) return DEFAULT_MAX_CONCURRENT_WORKFLOWS;
		return Integer.parseInt(maxString);
	}

	public void createNewWorkflowJobs(String evaluationId, WorkflowURLEntrypointAndSynapseRef workflow) throws Throwable {
		int currentWorkflowCount = workflowManager.listWorkflowJobs().size();
		int maxConcurrentWorkflows = getMaxConcurrentWorkflows();
		List<SubmissionBundle> receivedSubmissions=null;
		try {
			receivedSubmissions = 
					evaluationUtils.selectSubmissions(evaluationId, getInitialSubmissionState() );
		} catch (IllegalStateException e ) {
			log.warn("Got IllegalStateException when calling selectSubmissions().  Will retry.  Message is: "+e.getMessage());
		}
		for (SubmissionBundle sb : receivedSubmissions) {
			String submissionId=sb.getSubmission().getId();
			SubmissionStatus submissionStatus = sb.getSubmissionStatus();
			try {
				if (BooleanUtils.isTrue(submissionStatus.getCancelRequested())) {
					SubmissionStatusModifications statusMods = new SubmissionStatusModifications();
					setStatus(statusMods, SubmissionStatusEnum.INVALID, WorkflowUpdateStatus.STOPPED_UPON_REQUEST);
					try {
						submissionUtils.updateSubmissionStatus(submissionStatus, statusMods);
					} catch (SynapseConflictingUpdateException e) {
						// do nothing
					}
					continue;
				}
				
				if (currentWorkflowCount>=maxConcurrentWorkflows) {
					log.info("We have met or exceeded the maximum concurrent workflow count, "+maxConcurrentWorkflows+", so we will not start "+submissionId+" at this time.");
					continue;
				}
 
				SubmissionStatusModifications statusMods = new SubmissionStatusModifications();
				initializeSubmissionAnnotations(statusMods);
				String workflowId = null;

				String submittingUserOrTeamId = SubmissionUtils.getSubmittingUserOrTeamId(sb.getSubmission());
				Folder sharedFolder=archiver.getOrCreateSubmissionUploadFolder(submissionId, submittingUserOrTeamId, true);
				Folder lockedFolder=archiver.getOrCreateSubmissionUploadFolder(submissionId, submittingUserOrTeamId, false);
				WorkflowParameters workflowParameters = new WorkflowParameters(
						sb.getSubmission().getId(), workflow.getSynapseId(), lockedFolder.getId(), sharedFolder.getId());
				byte[] synapseConfigFileContent;
				try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
					Utils.writeSynapseConfigFile(baos);
					synapseConfigFileContent = baos.toByteArray();
				}
				WorkflowJob newJob = workflowManager.createWorkflowJob(workflow.getWorkflowUrl(), workflow.getEntryPoint(), workflowParameters, synapseConfigFileContent);
				workflowId = newJob.getWorkflowId();
				EvaluationUtils.setAnnotation(statusMods, WORKFLOW_JOB_ID, workflowId, PUBLIC_ANNOTATION_SETTING);

				try {
					submissionUtils.updateSubmissionStatus(submissionStatus, statusMods);
				} catch (Exception e) {
					throw new IllegalStateException("Started job "+workflowId+", but could not update submission "+submissionId, e);
				}
				currentWorkflowCount++;
			} catch (final Throwable t) {
				log.error("Submission failed", t);
				String errorMessage = createPipelineFailureMessage(submissionId, null, ExceptionUtils.getStackTrace(t));
				// send this notification to an admin, not to the submitter
				messageUtils.sendMessage(getNotificationPrincipalId(), SUBMISSION_PIPELINE_FAILURE_SUBJECT, 
						errorMessage);
				throw t;
			}
		}
	}

	private static void initializeSubmissionAnnotations(SubmissionStatusModifications statusMods) {
		statusMods.setCancelRequested(false);
		statusMods.setCanCancel(true);
		EvaluationUtils.removeAnnotation(statusMods, WORKFLOW_JOB_ID);
		EvaluationUtils.removeAnnotation(statusMods, FAILURE_REASON);
		EvaluationUtils.removeAnnotation(statusMods, STATUS_DESCRIPTION);

		long now = System.currentTimeMillis();
		EvaluationUtils.setAnnotation(statusMods, JOB_STARTED_TIME_STAMP, now, ADMIN_ANNOTS_ARE_PRIVATE);
		EvaluationUtils.setAnnotation(statusMods, JOB_LAST_UPDATED_TIME_STAMP, now, PUBLIC_ANNOTATION_SETTING);
		statusMods.setStatus(getInProgressSubmissionState());    	
	}
	
	// returns map from workflow ID to Submission Bundle
	public static Map<String, SubmissionBundle> workflowIdsForSubmissions(List<SubmissionBundle> bundles) {
		Map<String, SubmissionBundle> result = new HashMap<String, SubmissionBundle>();
		for (SubmissionBundle b : bundles) {
			String workflowId = EvaluationUtils.getStringAnnotation(b.getSubmissionStatus(), WORKFLOW_JOB_ID);
			if (workflowId==null) throw new IllegalStateException("Submission "+b.getSubmission().getId()+" has no workflow job ID.");
			result.put(workflowId, b);
		}
		return result;
	}

	public static Map<String, WorkflowJob> workflowIdsForJobs(List<WorkflowJob> jobs) {
		Map<String, WorkflowJob> result = new HashMap<String, WorkflowJob>();
		for (WorkflowJob j : jobs) result.put(j.getWorkflowId(), j);
		return result;
	}

	public void updateWorkflowJobs(List<String> evaluationIds) throws Throwable {
		// list the running jobs according to Synapse
		List<SubmissionBundle> runningSubmissions=new ArrayList<SubmissionBundle>();
		for (String evaluationId : evaluationIds) {
			try {
				runningSubmissions.addAll(evaluationUtils.
						selectSubmissions(evaluationId, getInProgressSubmissionState()));
			} catch (IllegalStateException e ) {
				log.warn("Got IllegalStateException when calling selectSubmissions().  Will retry.  Message is: "+e.getMessage());
			}
		}
		
		// list the running jobs according to the workflow system
		List<WorkflowJob> jobs = workflowManager.listWorkflowJobs();
		// the two lists should be the same ...
		Map<String, SubmissionBundle> workflowIdToSubmissionMap = workflowIdsForSubmissions(runningSubmissions);
		{
			Map<String, WorkflowJob> workflowIdToJobMap = workflowIdsForJobs(jobs);
			Set<WorkflowJob> jobsWithoutSubmissions = new HashSet<WorkflowJob>();
			for (String workflowId : workflowIdToJobMap.keySet()) {
				WorkflowJob jobForWorkflowId = workflowIdToJobMap.get(workflowId);
				SubmissionBundle submissionForWorkflowId = workflowIdToSubmissionMap.get(workflowId);
				if (submissionForWorkflowId==null) {
					jobsWithoutSubmissions.add(jobForWorkflowId);
				}
			}
			// WES does not provide a way to remove a completed job so we cannot do this part of the reconciliation if using WES
			if (configuredForDocker()) {
				// if there are any running workflow jobs not in the EIP list, throw an IllegalStateException
				if (!jobsWithoutSubmissions.isEmpty()) {
					StringBuffer msg = new StringBuffer("The following workflow job(s) are running but have no corresponding open Synapse submissions.");
					for (WorkflowJob job : jobsWithoutSubmissions) {
						msg.append("\n\t");
						msg.append(job.getWorkflowId());
					}
					msg.append("\nOne way to recover is to delete the workflow job(s).");
					final String errorMessage = createPipelineFailureMessage(null, null, msg.toString());
					messageUtils.sendMessage(getNotificationPrincipalId(), SUBMISSION_PIPELINE_FAILURE_SUBJECT, 
							errorMessage);
					throw new IllegalStateException(msg.toString());
					// Note: An alternative is to kill the workflow(s) and let the WorkflowHook keep running.
					// For now we let the submission queue administrator do this, to ensure they are aware of the issue.
				}
			} else {
				// just remove the jobs lacking submissions
				jobs.removeAll(jobsWithoutSubmissions);
			}

			Set<SubmissionBundle> submissionsWithoutJobs = new HashSet<SubmissionBundle>();
			for (String workflowId : workflowIdToSubmissionMap.keySet()) {
				if (workflowIdToJobMap.get(workflowId)==null) submissionsWithoutJobs.add(workflowIdToSubmissionMap.get(workflowId));
			}
			for (SubmissionBundle submissionBundle : submissionsWithoutJobs) {
				String messageBody = createPipelineFailureMessage(submissionBundle.getSubmission().getId(), null, "No running workflow found for submission.");
				submissionUtils.closeSubmissionAndSendNotification(getNotificationPrincipalId(), submissionBundle.getSubmissionStatus(), new SubmissionStatusModifications(),
						SubmissionStatusEnum.INVALID, WorkflowUpdateStatus.ERROR_ENCOUNTERED_DURING_EXECUTION, null, WORKFLOW_FAILURE_SUBJECT, messageBody);
			}
		}

		String shareImmediatelyString = getProperty("SHARE_RESULTS_IMMEDIATELY", false);
		boolean shareImmediately = StringUtils.isEmpty(shareImmediatelyString) ? true : new Boolean(shareImmediatelyString);

		// Now go through the list of running jobs, checking and updating each
		for (WorkflowJob job : jobs) {
			final SubmissionBundle submissionBundle = workflowIdToSubmissionMap.get(job.getWorkflowId());
			final Submission submission = submissionBundle.getSubmission();
			final SubmissionStatus submissionStatus = submissionBundle.getSubmissionStatus();
			final SubmissionStatusModifications statusMods = new SubmissionStatusModifications();
			
			String sharedSubmissionFolderId = shareImmediately ? EvaluationUtils.getStringAnnotation(submissionStatus, SUBMISSION_ARTIFACTS_FOLDER) : null;

			try {
				Double progress = null;
				WorkflowUpdateStatus containerCompletionStatus = null;
				{
					WorkflowStatus initialWorkflowStatus = workflowManager.getWorkflowStatus(job);
					progress = initialWorkflowStatus.getProgress();
					containerCompletionStatus = updateJob(job, initialWorkflowStatus, submissionBundle, statusMods);
					
					// we will apply the statusMods to the submissionStatus at the time we update in Synapse,
					// but we do so here just so that we have access to the submisson's history and the recent updates
					// in a single object
					applyModifications(submissionStatus, statusMods);
				}
				switch(containerCompletionStatus) {
				case IN_PROGRESS:
					statusMods.setStatus(getInProgressSubmissionState());
					break;
				case DONE:
					statusMods.setStatus(getFinalSubmissionState());
					EvaluationUtils.removeAnnotation(statusMods, FAILURE_REASON);
					if (notificationEnabled(SUBMISSION_COMPLETED)) {
						Submitter submitter = submissionUtils.getSubmitter(submission);
						String messageBody = createWorkflowCompleteMessage(submitter.getName(), submission.getId(), sharedSubmissionFolderId);
						messageUtils.sendMessage(submitter.getId(), WORKFLOW_COMPLETE_SUBJECT,  messageBody);
					}
					break;
				case REJECTED:
					statusMods.setStatus(SubmissionStatusEnum.REJECTED);
					break;
				case ERROR_ENCOUNTERED_DURING_EXECUTION:
				case STOPPED_UPON_REQUEST:
				case STOPPED_TIME_OUT:
					statusMods.setStatus(SubmissionStatusEnum.INVALID);
					if (containerCompletionStatus==ERROR_ENCOUNTERED_DURING_EXECUTION && notificationEnabled(SUBMISSION_FAILED) || 
						containerCompletionStatus==STOPPED_UPON_REQUEST && notificationEnabled(SUBMISSION_STOPPED_BY_USER) || 
						containerCompletionStatus==STOPPED_TIME_OUT && notificationEnabled(SUBMISSION_TIMED_OUT)) {
						Submitter submitter = submissionUtils.getSubmitter(submission);
						String messageBody = createWorkflowFailedMessage(submitter.getName(), submission.getId(), 
								EvaluationUtils.getStringAnnotation(submissionStatus, FAILURE_REASON), 
								null, sharedSubmissionFolderId);
						messageUtils.sendMessage(submitter.getId(), WORKFLOW_FAILURE_SUBJECT,  messageBody);
					}
					break;
				default:
					throw new IllegalStateException(containerCompletionStatus.toString());
				}
				EvaluationUtils.setAnnotation(statusMods, JOB_LAST_UPDATED_TIME_STAMP, System.currentTimeMillis(), PUBLIC_ANNOTATION_SETTING);
				if (progress!=null) {
					EvaluationUtils.setAnnotation(statusMods, PROGRESS, progress, false);
				}
				submissionUtils.updateSubmissionStatus(submissionStatus, statusMods);
			} catch (final Throwable t) {
				log.error("Pipeline failed", t);
				final String submissionId = submission==null?null:submission.getId();
				final String workflowDescription = job==null?null:job.toString();
				final String errorMessage = createPipelineFailureMessage(submissionId, workflowDescription, ExceptionUtils.getStackTrace(t));
				// send this notification to an admin, not to the submitter
				messageUtils.sendMessage(getNotificationPrincipalId(), SUBMISSION_PIPELINE_FAILURE_SUBJECT, 
						errorMessage);

				throw t;
			}
		}
	}

	/*
	 * The possible states and the corresponding actions:
	 * 
	 * - container is running and there's no need to stop it or upload logs -> update the time stamp
	 * - container is running and there's no need to stop it but it's time to upload logs ->
	 * 		upload logs and (if haven't already) notify submitter where they can find their logs, update time stamp
	 * - container is running and either it's exceeded its time limit or user has requested stop ->
	 * 		stop the container, upload the final logs, update time stamp
	 * - container is stopped with exit code 0 ->
	 * 		upload the final logs, update time stamp
	 * - container is stopped with non-zero exit code ->
	 * 		upload the final logs, notify submitter that it failed, update time stamp
	 * 
	 * return the ContainerCompletionStatus
	 */
	public WorkflowUpdateStatus updateJob(final WorkflowJob job, WorkflowStatus workflowStatus, 
			SubmissionBundle submissionBundle, SubmissionStatusModifications statusMods) throws Throwable {
		final Submission submission = submissionBundle.getSubmission();
		final SubmissionStatus submissionStatus = submissionBundle.getSubmissionStatus();

		boolean isRunning = workflowStatus.isRunning();
		ExitStatus exitStatus = null; // null means that the container is running
		WorkflowUpdateStatus workflowUpdateStatus = null;

		SubmissionStatusEnum updatedStatus = null;
		String failureReason = null;
		Long updatedWhenLogFileSizeExceeded = null;
		Boolean updatedHasSubmissionStartedMessageBeenSent = null;
		Double progress = null;
		if (isRunning) {
			boolean userHasRequestedStop=submissionStatus.getCancelRequested()!=null && submissionStatus.getCancelRequested();
			boolean teamHasTimeRemaining = EvaluationUtils.hasTimeRemaining(EvaluationUtils.getTimeRemaining(submissionStatus));
			if (userHasRequestedStop || !teamHasTimeRemaining) {
				workflowManager.stopWorkflowJob(job);
				if (userHasRequestedStop) {
					workflowUpdateStatus = STOPPED_UPON_REQUEST;
				} else if (!teamHasTimeRemaining) {
					workflowUpdateStatus = STOPPED_TIME_OUT;
				}
				updatedStatus = SubmissionStatusEnum.CLOSED;
				isRunning=false;
			} else {
				progress = workflowStatus.getProgress();       
				log.info("PROGRESS: "+progress);
				workflowUpdateStatus = IN_PROGRESS;
			}
		} else {
			exitStatus = workflowStatus.getExitStatus();
			if (exitStatus==ExitStatus.SUCCESS) {
				workflowUpdateStatus = DONE;
				updatedStatus = SubmissionStatusEnum.CLOSED;
			} else {
				if (exitStatus==ExitStatus.CANCELED) {
					workflowUpdateStatus = STOPPED_TIME_OUT;
					failureReason = STOPPED_TIME_OUT.toString();
				} else {
					workflowUpdateStatus = ERROR_ENCOUNTERED_DURING_EXECUTION;
					failureReason = ERROR_ENCOUNTERED_DURING_EXECUTION.toString();
				}
				updatedStatus = SubmissionStatusEnum.CLOSED;
			}
		}

		
		Long lastLogUploadTimeStamp = EvaluationUtils.getLongAnnotation(submissionStatus, LAST_LOG_UPLOAD);
		boolean timeToUploadLogs = lastLogUploadTimeStamp==null ||
				lastLogUploadTimeStamp+UPLOAD_PERIOD_MILLIS<System.currentTimeMillis();

		SubmissionFolderAndLogTail submissionFolderAndLogTail = null;
		String submissionFolderId = null;
		// we upload logs periodically and when container finally finishes, unless we've exceeded the maximum log size
		if ((true/*TODO not if log file size exceeded*/) && (timeToUploadLogs || !isRunning)) {
			String submittingUserOrTeamId = SubmissionUtils.getSubmittingUserOrTeamId(submission);
			Submitter submitter = submissionUtils.getSubmitter(submission);

			submissionFolderAndLogTail = archiver.uploadLogs(
					job, 
					submission.getId(),
					submittingUserOrTeamId, 
					LOGS_SUFFIX,
					MAX_LOG_ANNOTATION_CHARS);
			Folder submissionFolder = submissionFolderAndLogTail.getSubmissionFolder();
			submissionFolderId = submissionFolder==null?null:submissionFolder.getId();

			if (ERROR_ENCOUNTERED_DURING_EXECUTION.toString().equals(failureReason)) {
				failureReason = submissionFolderAndLogTail.getLogTail();
			}

			String hasSubmissionStartedMessageBeenSentString = EvaluationUtils.getStringAnnotation(submissionStatus, SUBMISSION_PROCESSING_STARTED_SENT);
			boolean hasSubmissionStartedMessageBeenSent = hasSubmissionStartedMessageBeenSentString!=null && new Boolean(hasSubmissionStartedMessageBeenSentString);
			if (isRunning && !hasSubmissionStartedMessageBeenSent && notificationEnabled(SUBMISSION_STARTED)) {
				String shareImmediatelyString = getProperty("SHARE_RESULTS_IMMEDIATELY", false);
				boolean shareImmediately = StringUtils.isEmpty(shareImmediatelyString) ? true : new Boolean(shareImmediatelyString);
				String sharedSubmissionFolderId = shareImmediately ? submissionFolderId : null;
				String messageBody = createSubmissionStartedMessage(submitter.getName(), submission.getId(), sharedSubmissionFolderId);
				messageUtils.sendMessage(submittingUserOrTeamId, SUBMISSION_PROCESSING_STARTED_SUBJECT, messageBody);
				updatedHasSubmissionStartedMessageBeenSent=true;
			}
		} // end uploading logs

		if (!isRunning) {
			workflowManager.deleteWorkFlowJob(job);
		}

		EvaluationUtils.setAnnotation(statusMods, JOB_LAST_UPDATED_TIME_STAMP, System.currentTimeMillis(), PUBLIC_ANNOTATION_SETTING);
		if (submissionFolderId!=null) {
			EvaluationUtils.setAnnotation(statusMods, LAST_LOG_UPLOAD, System.currentTimeMillis(), ADMIN_ANNOTS_ARE_PRIVATE);    				
			EvaluationUtils.setAnnotation(statusMods, SUBMISSION_ARTIFACTS_FOLDER, submissionFolderId, PUBLIC_ANNOTATION_SETTING);
		}
		if (updatedWhenLogFileSizeExceeded!=null) {
			EvaluationUtils.setAnnotation(statusMods, LOG_FILE_SIZE_EXCEEDED, updatedWhenLogFileSizeExceeded.toString(), ADMIN_ANNOTS_ARE_PRIVATE);
		}
		if (updatedHasSubmissionStartedMessageBeenSent!=null) {
			EvaluationUtils.setAnnotation(statusMods, SUBMISSION_PROCESSING_STARTED_SENT, updatedHasSubmissionStartedMessageBeenSent.toString(), ADMIN_ANNOTS_ARE_PRIVATE);
		}
		if (updatedStatus!=null) {
			EvaluationUtils.setStatus(statusMods, updatedStatus, workflowUpdateStatus);
		}
		if (failureReason!=null) {
			EvaluationUtils.setAnnotation(statusMods, FAILURE_REASON, failureReason, PUBLIC_ANNOTATION_SETTING);
		} else {
			EvaluationUtils.removeAnnotation(statusMods, FAILURE_REASON);
		}
		if (progress!=null) {
			EvaluationUtils.setAnnotation(statusMods, PROGRESS, progress, false);
		}

		if (workflowUpdateStatus==null) throw new IllegalStateException("Failed to set workflowUpdateStatus");
		return workflowUpdateStatus;
	}

}
