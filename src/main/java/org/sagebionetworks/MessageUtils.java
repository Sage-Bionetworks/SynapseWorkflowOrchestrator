package org.sagebionetworks;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.regex.Matcher;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.message.MessageToUser;

public class MessageUtils {
	private static final String WORKFLOW_COMPLETE_TEMPLATE = "WorkflowCompleteTemplate.txt";
	private static final String WORKFLOW_FAILED_TEMPLATE = "WorkflowFailedTemplate.txt";
	private static final String PIPELINE_FAILURE_TEMPLATE = "PipelineFailureTemplate.txt";
	public  static final String WORKFLOW_FAILURE_SUBJECT = "Workflow Failed";
	public  static final String SUBMISSION_PIPELINE_FAILURE_SUBJECT = "Submission Pipeline Failed";
	public  static final String WORKFLOW_TERMINATED_SUBJECT = "Workflow Terminated";
	public  static final String WORKFLOW_COMPLETE_SUBJECT = "Workflow Complete";
	private static final String SUBMISSION_PROCESSING_STARTED = "SubmissionProcessingStarted.txt";
	public  static final String SUBMISSION_PROCESSING_STARTED_SUBJECT = "Submission Processing Started in ";
	
	private SynapseClient synapse;
	
	public MessageUtils(SynapseClient synapse) {
		this.synapse=synapse;
	}
	
	private static final String LOGS_AVAILABLE_STATEMENT = " Log files produced while your workflow is running will be periodically uploaded here: https://www.synapse.org/#!Synapse:";

	public static String createSubmissionStartedMessage(String teamName, String submissionId, String logsFolderId) throws IOException {
		InputStream is = MessageUtils.class.getClassLoader().getResourceAsStream(SUBMISSION_PROCESSING_STARTED);
		if (is==null) throw new RuntimeException("Could not find file "+SUBMISSION_PROCESSING_STARTED);
		try {
			String template = new String(IOUtils.toByteArray(is));
			template = template.replaceAll("##team##", teamName);
			if (!StringUtils.isEmpty(submissionId)) {
				template = template.replaceAll("##submissionId##", "(submission ID "+submissionId+")");
			} else {
				template = template.replaceAll("##submissionId##", "");    			
			}
			if (logsFolderId==null) {
				template = template.replaceAll("##logsAvailableMessage##", "");    			
			} else {
				template = template.replaceAll("##logsAvailableMessage##", LOGS_AVAILABLE_STATEMENT+logsFolderId);    			
			}
			return template;

		} finally {
			is.close();
		}
	}

	public static String createWorkflowFailedMessage(String teamName, String submissionId, String errorMessage, String logs, String submissionFolderId) throws IOException {
		InputStream is = MessageUtils.class.getClassLoader().getResourceAsStream(WORKFLOW_FAILED_TEMPLATE);
		if (is==null) throw new RuntimeException("Could not find file "+WORKFLOW_FAILED_TEMPLATE);
		try {
			String template = new String(IOUtils.toByteArray(is));
			template = template.replaceAll("##team##", teamName);
			if (!StringUtils.isEmpty(submissionId)) {
				template = template.replaceAll("##submissionId##", "(submission ID "+submissionId+")");
			} else {
				template = template.replaceAll("##submissionId##", "");    			
			}
			if (!StringUtils.isEmpty(errorMessage)) {
				template = template.replaceAll("##errorMessage##", Matcher.quoteReplacement(" The message is:\n\n\t"+errorMessage+"\n\n"));
			} else {
				template = template.replaceAll("##errorMessage##", "");
			}
			if (!StringUtils.isEmpty(logs)) {
				template = template.replaceAll("##logs##", Matcher.quoteReplacement("Logs:\n\n"+logs+"\n"));
			} else {
				template = template.replaceAll("##logs##", "");
			}
			if (!StringUtils.isEmpty(submissionFolderId)) {
				// this is an extra check (since not sharing logs in the leaderboard is so critical)
				template = template.replaceAll("##submissionFolderId##", " Your logs are available here: https://www.synapse.org/#!Synapse:"+submissionFolderId+".");
			} else {
				template = template.replaceAll("##submissionFolderId##", "");
			}
			return template;
		} finally {
			is.close();
		}
	}

	public static String createWorkflowCompleteMessage(String teamName, String submissionId, String submissionFolderId) throws IOException {
		InputStream is = MessageUtils.class.getClassLoader().getResourceAsStream(WORKFLOW_COMPLETE_TEMPLATE);
		if (is==null) throw new RuntimeException("Could not find file "+WORKFLOW_COMPLETE_TEMPLATE);
		try {
			String template = new String(IOUtils.toByteArray(is));
			template = template.replaceAll("##team##", teamName);
			if (!StringUtils.isEmpty(submissionId)) {
				template = template.replaceAll("##submissionId##", "(submission ID "+submissionId+")");
			} else {
				template = template.replaceAll("##submissionId##", "");    			
			}
			if (!StringUtils.isEmpty(submissionFolderId)) {
				// this is an extra check (since not sharing logs in the leaderboard is so critical)
				template = template.replaceAll("##submissionFolderId##", " Your logs are available here: https://www.synapse.org/#!Synapse:"+submissionFolderId+".");
			} else {
				template = template.replaceAll("##submissionFolderId##", "");
			}
			return template;
		} finally {
			is.close();
		}
	}

	public static String createPipelineFailureMessage(String submissionId, String workflowDescription, String errorMessage) throws IOException {
		InputStream is = MessageUtils.class.getClassLoader().getResourceAsStream(PIPELINE_FAILURE_TEMPLATE);
		if (is==null) throw new RuntimeException("Could not find file "+PIPELINE_FAILURE_TEMPLATE);
		try {
			String template = new String(IOUtils.toByteArray(is));
			if (!StringUtils.isEmpty(submissionId)) {
				template = template.replaceAll("##submissionId##", submissionId);
			} else {
				template = template.replaceAll("##submissionId##", "");    			
			}
			if (!StringUtils.isEmpty(workflowDescription)) {
				template = template.replaceAll("##workflowDescription##", " The workflow description is: "+workflowDescription+"\n");
			} else {
				template = template.replaceAll("##workflowDescription##", "");    			
			}
			if (!StringUtils.isEmpty(errorMessage)) {
				template = template.replaceAll("##errorMessage##", Matcher.quoteReplacement(" The message is:\n\n\t"+errorMessage+"\n\n"));
			} else {
				template = template.replaceAll("##errorMessage##", "");
			}
			return template;
		} finally {
			is.close();
		}
	}

	public static String getDisplayNameWithUserName(UserProfile userProfile) {
		String userName = userProfile.getUserName();
		if (userName==null) throw new IllegalArgumentException("userName is required");
		String displayName = getDisplayName(userProfile);
		if (displayName!=null) {
			displayName = displayName+" ("+userName+")";
		} else {
			displayName = userName;
		}
		return displayName;
	}

	public static String getDisplayName(UserProfile userProfile) {
		String firstName = userProfile.getFirstName();
		String lastName = userProfile.getLastName();
		if (firstName==null && lastName==null) return null;
		StringBuilder displayName = new StringBuilder();
		if (firstName!=null) displayName.append(firstName);
		if (lastName!=null) {
			if (firstName!=null) displayName.append(" ");
			displayName.append(lastName);
		}
		return displayName.toString();
	}

	public void sendMessage(final String userId, final String subject, final String body) throws SynapseException {
		final MessageToUser messageMetadata = new MessageToUser();
		messageMetadata.setRecipients(Collections.singleton(userId));
		messageMetadata.setSubject(subject);
		try {
			(new ExponentialBackoffRunner()).execute(new NoRefreshExecutableAdapter<Void,Void>() {
				@Override
				public Void execute(Void args) throws Throwable {
					synapse.sendStringMessage(messageMetadata, body);
					return null;
				}}, null);
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
	}

	public static String getMessageForCompletionStatus(WorkflowUpdateStatus s) {
		switch(s) {
		case DONE:
			return("Finished normally.");
		case REJECTED:
			return("Rejected.");
		case ERROR_ENCOUNTERED_DURING_EXECUTION:
			return("Error encountered during execution.");
		case STOPPED_UPON_REQUEST:
			return("Submission halted upon user request.");
		case STOPPED_TIME_OUT:
			return("Submission exceeded allotted time.");
		default:
			throw new IllegalArgumentException(s.toString());
		}
	}


}
