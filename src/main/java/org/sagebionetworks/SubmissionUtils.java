package org.sagebionetworks;

import org.apache.commons.lang.StringUtils;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.exceptions.SynapseConflictingUpdateException;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseLockedException;
import org.sagebionetworks.client.exceptions.SynapseServerException;
import org.sagebionetworks.client.exceptions.SynapseServiceUnavailable;
import org.sagebionetworks.client.exceptions.SynapseTableUnavailableException;
import org.sagebionetworks.client.exceptions.SynapseTooManyRequestsException;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.evaluation.model.SubmissionContributor;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.util.DockerNameUtil;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.sagebionetworks.Constants.DEFAULT_NUM_RETRY_ATTEMPTS;
import static org.sagebionetworks.Constants.NO_RETRY_EXCEPTIONS;
import static org.sagebionetworks.Constants.NO_RETRY_STATUSES;
import static org.sagebionetworks.EvaluationUtils.FAILURE_REASON;
import static org.sagebionetworks.EvaluationUtils.JOB_LAST_UPDATED_TIME_STAMP;
import static org.sagebionetworks.EvaluationUtils.PUBLIC_ANNOTATION_SETTING;
import static org.sagebionetworks.EvaluationUtils.applyModifications;

public class SubmissionUtils {

	private static Logger log = LoggerFactory.getLogger(SubmissionUtils.class);

	// commit must have the form reponame@sha256:digest, where digest is 64 hex chars

	// a valid name with a trailing '/' appended will cause this regex to hang forever
	private static final String DOCKER_NAME_REGEX = "^"+DockerNameUtil.NameRegexp+"$";

	private static final String DOCKER_DIGEST_REGEX = "^sha256:[0-9a-f]{64}$";

	public static final String SYNID_REGEX = "syn[0-9]+";

	private static final String AUTH_USERS_PRINCIPAL_ID = "273948";
	private static final String PUBLIC_PRINCIPAL_ID = "273949";

	private SynapseClient synapse;

	public SubmissionUtils(SynapseClient synapse) {
		this.synapse=synapse;
	}
	
	private static final ExponentialBackoffRunner SUBMISSION_STATUS_UPDATE_RUNNER;
	static {
		List<Class<? extends SynapseServerException>> noRetryExceptions = new ArrayList<Class<? extends SynapseServerException>>(NO_RETRY_EXCEPTIONS);
		// this is the one exception we DO want to retry on!
		boolean foundit = noRetryExceptions.remove(SynapseConflictingUpdateException.class);
		// these are being retried on the lower level so we do NOT want to retry on them here too
		noRetryExceptions.add(SynapseServiceUnavailable.class);
		noRetryExceptions.add(SynapseTooManyRequestsException.class);
		noRetryExceptions.add(SynapseLockedException.class);
		noRetryExceptions.add(SynapseTableUnavailableException.class);
		
		SUBMISSION_STATUS_UPDATE_RUNNER = new ExponentialBackoffRunner(noRetryExceptions, NO_RETRY_STATUSES, DEFAULT_NUM_RETRY_ATTEMPTS);
	}
	
	public SubmissionStatus updateSubmissionStatus(SubmissionStatus submissionStatus, SubmissionStatusModifications statusMods) throws SynapseException {
		try {
			return SUBMISSION_STATUS_UPDATE_RUNNER.execute(new Executable<SubmissionStatus,SubmissionStatus>(){
				public SubmissionStatus execute(SubmissionStatus status) throws SynapseException {
					applyModifications(status, statusMods);
					log.info(status.getEntityId() + " entity id");
					log.info(status.getId() + " id");
					log.info(status.getAnnotations()+ " annotations");
					log.info(status.getSubmissionAnnotations() + " annotations v2");
					log.info(status.getReport() + " report");
					return synapse.updateSubmissionStatus(status);
				}
				public SubmissionStatus refreshArgs(SubmissionStatus status) throws SynapseException {
					SubmissionStatus newStatus = synapse.getSubmissionStatus(status.getId());
					log.warn("Failed to update submission status "+status.getId()+" my etag was "+status.getEtag()+" but Synapse has "+newStatus.getEtag());
					return newStatus;
				}
			}, submissionStatus);
		} catch (SynapseException s) {
			throw s;
		} catch (RuntimeException e) {
			throw e;
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
	}

	public void closeSubmissionAndSendNotification(
			final String messageRecipientId, 
			final SubmissionStatus ss, 
			final SubmissionStatusModifications statusMods,
			final SubmissionStatusEnum status,
			final WorkflowUpdateStatus containerStatus,
			final Throwable t,
			final String messageSubject,
			final String messageBody) throws Throwable {
		EvaluationUtils.setStatus(statusMods, status, containerStatus);
		if (t==null) {
			EvaluationUtils.removeAnnotation(statusMods, FAILURE_REASON);
		} else {
			EvaluationUtils.setAnnotation(statusMods, FAILURE_REASON, t.getMessage(), PUBLIC_ANNOTATION_SETTING);
		}
		EvaluationUtils.setAnnotation(statusMods, JOB_LAST_UPDATED_TIME_STAMP, System.currentTimeMillis(), false);
		updateSubmissionStatus(ss, statusMods);
		(new MessageUtils(synapse)).sendMessage(messageRecipientId, messageSubject,  messageBody);
	}

	// docker.synapse.org/syn123/foo/bar@sha256:...  ->  foo/bar
	public static String getRepoSuffixFromImage(String s) {
		int at = s.indexOf("@");
		String repoWithoutDigest = at<0 ? s : s.substring(0, at);
		String host = DockerNameUtil.getRegistryHost(repoWithoutDigest);
		String repoPath = StringUtils.isEmpty(host) ? repoWithoutDigest : repoWithoutDigest.substring(host.length());
		if (repoPath.startsWith("/")) repoPath = repoPath.substring(1);
		String[] pathSegments = repoPath.split("/");
		if (pathSegments.length<2) return repoPath;
		if (!Pattern.matches(SYNID_REGEX, pathSegments[0])) return repoPath;
		StringBuilder sb = new StringBuilder();
		for (int i=1; i<pathSegments.length; i++) {
			if (i>1) sb.append("/");
			sb.append(pathSegments[i]);
		}
		return sb.toString();
	}

	public static String getRepoNameForDockerImage(String image) {
		int i = image.indexOf("@sha256:");
		return image.substring(0, i);
	}

	private static final long TEAM_MEMBERS_PAGE_SIZE = 50;


	public static Class<Entity> getEntityTypeFromSubmission(Submission sub) throws JSONObjectAdapterException {
		EntityBundle bundle = EntityFactory.createEntityFromJSONString(
				sub.getEntityBundleJSON(), EntityBundle.class);
		return (Class<Entity>)bundle.getEntity().getClass();
	}

	public static String getSubmittingUserOrTeamId(Submission submission) {
		return submission.getTeamId()==null ? submission.getUserId(): submission.getTeamId();    	
	}
	
	private Map<String,String> idToNameCache = new HashMap<String,String>();

	public Submitter getSubmitter(Submission sub) throws SynapseException {
		String submittingUserOrTeamId;
		String submittingUserOrTeamName;
		if (sub.getTeamId()==null) {
			submittingUserOrTeamId = sub.getUserId();
			submittingUserOrTeamName=idToNameCache.get(submittingUserOrTeamId);
			if (submittingUserOrTeamName==null) {
				UserProfile userProfile = synapse.getUserProfile(sub.getUserId());
				submittingUserOrTeamName = MessageUtils.getDisplayNameWithUserName(userProfile);
				idToNameCache.put(submittingUserOrTeamId, submittingUserOrTeamName);
			}
		} else {
			submittingUserOrTeamId = sub.getTeamId();
			submittingUserOrTeamName=idToNameCache.get(submittingUserOrTeamId);
			if (submittingUserOrTeamName==null) {
				Team team = synapse.getTeam(sub.getTeamId());
				submittingUserOrTeamName = team.getName();
				idToNameCache.put(submittingUserOrTeamId, submittingUserOrTeamName);
			}
		}
		return new Submitter(submittingUserOrTeamId, submittingUserOrTeamName);
	}

	public static List<String> getSubmissionContributors(Submission submission) {
		List<String> result = new ArrayList<String>();
		for (SubmissionContributor contributor : submission.getContributors()) {
			result.add(contributor.getPrincipalId());
		}
		return result;
	}



}
