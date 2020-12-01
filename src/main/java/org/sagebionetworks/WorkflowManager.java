package org.sagebionetworks;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * This interface provides the operations supported by the Worklow Execution Schema
 * (create, list, delete workflow jobs).  Specific implementations can either use 
 * WES or create jobs locally without making web API calls.
 * 
 * For more, see http://ga4gh.github.io/workflow-execution-service-schemas
 * 
 * @author bhoff
 *
 */
public interface WorkflowManager {

	/**
	 * Create a workflow
	 * @param workflowUrlString the URL to the archive of workflow files
	 * @param entrypoint the entry point (a file path) within an unzipped workflow archive
	 * @param workflowParameters the parameters to be passed to the workflow
	 * @return the created workflow job
	 * @throws IOException
	 */
	WorkflowJob createWorkflowJob(String workflowUrlString, String entrypoint, WorkflowParameters workflowParameters,
			byte[] synapseConfigFileContent) throws IOException;

	/*
	 * This is analogous to GET /workflows in WES
	 * @param running true: list only the running jobs, false: list only the non-running jobs, null: list all jobs
	 */
	List<WorkflowJob> listWorkflowJobs(Boolean running);

	/*
	 * This is analogous to GET /workflows/{workflow_id}/status in WES
	 */
	WorkflowStatus getWorkflowStatus(WorkflowJob job) throws IOException;

	/*
	 * This would be provided by GET /workflows/{workflow_id} WES
	 * 
	 * Writes full log to a file and optionally returns just the tail (if 'maxTailLengthInCharacters' is not null)
	 */
	String getWorkflowLog(WorkflowJob job, Path outPath, Integer maxTailLengthInCharacters) throws IOException;

	/*
	 * This has no analogy in WES.  The idea is to have a state for a workflow in which it is
	 * interrupted but not deleted.  In this state the logs can be interrogated to show just
	 * where the interruption occurred.
	 */
	void stopWorkflowJob(WorkflowJob job);

	/*
	 * This is analogous to DELETE /workflows/{workflow_id} in WES
	 */
	void deleteWorkFlowJob(WorkflowJob job);

}