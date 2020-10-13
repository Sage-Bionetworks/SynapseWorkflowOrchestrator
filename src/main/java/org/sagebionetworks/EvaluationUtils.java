package org.sagebionetworks;

import org.apache.commons.lang.StringUtils;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.SubmissionBundle;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.annotation.AnnotationBase;
import org.sagebionetworks.repo.model.annotation.Annotations;
import org.sagebionetworks.repo.model.annotation.DoubleAnnotation;
import org.sagebionetworks.repo.model.annotation.LongAnnotation;
import org.sagebionetworks.repo.model.annotation.StringAnnotation;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValue;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.sagebionetworks.Constants.EXECUTION_STAGE_PROPERTY_NAME;
import static org.sagebionetworks.Utils.getProperty;

public class EvaluationUtils {
	private static final int PAGE_SIZE = 10;

	// submission annotation names
	public static final String WORKFLOW_JOB_ID = "orgSagebionetworksSynapseWorkflowOrchestratorWorkflowJobId";
	public static final String JOB_STARTED_TIME_STAMP = "orgSagebionetworksSynapseWorkflowOrchestratorExecutionStarted";
	public static final String TIME_REMAINING = "orgSagebionetworksSynapseWorkflowOrchestratorTimeRemaining";
	public static final String FAILURE_REASON = "orgSagebionetworksSynapseWorkflowOrchestratorFailureReason";
	public static final String SUBMISSION_ARTIFACTS_FOLDER = "orgSagebionetworksSynapseWorkflowOrchestratorSubmissionFolder";
	public static final String JOB_LAST_UPDATED_TIME_STAMP = "orgSagebionetworksSynapseWorkflowOrchestratorWorkflowLastUpdated";
	public static final String STATUS_DESCRIPTION = "orgSagebionetworksSynapseWorkflowOrchestratorStatusDescription";
	public static final String USER_ID_PUBLIC = "orgSagebionetworksSynapseWorkflowOrchestratorUserId";
	public static final String LOG_FILE_SIZE_EXCEEDED = "orgSagebionetworksSynapseWorkflowOrchestratorLogFileSizeExceeded";
	public static final String LAST_LOG_UPLOAD = "orgSagebionetworksSynapseWorkflowOrchestratorLastLogUpload";
	public static final String SUBMISSION_PROCESSING_STARTED_SENT = "orgSagebionetworksSynapseWorkflowOrchestratorSubmissionProcessingStartedSent";
	public static final String PROGRESS = "orgSagebionetworksSynapseWorkflowOrchestratorProgress";
	public static final boolean ADMIN_ANNOTS_ARE_PRIVATE = true;

	//annotation values
	public static final String STOPPED_UPON_REQUEST_ANNOTATION_VALUE = "STOPPED UPON REQUEST";
	public static final String DOCKER_PULL_FAILED_ANNOTATION_VALUE = "DOCKER PULL FAILED";
	public static final String ERROR_DURING_EXECUTION_ANNOTATION_VALUE = "ERROR";
	public static final String STOPPED_TIME_OUT_ANNOTATION_VALUE = "EXCEEDED TIME QUOTA";
	public static final String STATUS_ACCEPTED_ANNOTATION_VALUE = "COMPLETED";
	public static final String STATUS_EVAL_IN_PROGRESS_ANNOTATION_VALUE = "IN PROGRESS";

	// for public annotations, set isPrivate to false
	public static final boolean PUBLIC_ANNOTATION_SETTING = false;

	private SynapseClient synapse;

	public EvaluationUtils(SynapseClient synapse) {
		this.synapse=synapse;
	}
	
	public enum EXECUTION_STAGE {
		VALIDATION,
		EXECUTION,
		ALL
	};
	
	public static SubmissionStatusEnum getInitialSubmissionState() {
		String executionStageString = getProperty(EXECUTION_STAGE_PROPERTY_NAME, false);
		EXECUTION_STAGE stage = StringUtils.isEmpty(executionStageString) ? EXECUTION_STAGE.ALL : EXECUTION_STAGE.valueOf(executionStageString);
		switch (stage) {
		case VALIDATION:
			return SubmissionStatusEnum.RECEIVED;
		case EXECUTION:
			return SubmissionStatusEnum.VALIDATED;
		default: // ALL
			return SubmissionStatusEnum.RECEIVED;
		}
	}

	public static SubmissionStatusEnum getInProgressSubmissionState() {
		String executionStageString = getProperty(EXECUTION_STAGE_PROPERTY_NAME, false);
		EXECUTION_STAGE stage = StringUtils.isEmpty(executionStageString) ? EXECUTION_STAGE.ALL : EXECUTION_STAGE.valueOf(executionStageString);
		switch (stage) {
		case VALIDATION:
			return SubmissionStatusEnum.OPEN;
		case EXECUTION:
			return SubmissionStatusEnum.EVALUATION_IN_PROGRESS;
		default: // ALL
			return SubmissionStatusEnum.EVALUATION_IN_PROGRESS;
		}
	}

	public static SubmissionStatusEnum getFinalSubmissionState() {
		String executionStageString = getProperty(EXECUTION_STAGE_PROPERTY_NAME, false);
		EXECUTION_STAGE stage = StringUtils.isEmpty(executionStageString) ? EXECUTION_STAGE.ALL : EXECUTION_STAGE.valueOf(executionStageString);
		switch (stage) {
		case VALIDATION:
			return SubmissionStatusEnum.VALIDATED;
		case EXECUTION:
			return SubmissionStatusEnum.ACCEPTED;
		default: // ALL
			return SubmissionStatusEnum.ACCEPTED;
		}
	}

	public static String getStringAnnotation(SubmissionStatus status, String key) {
		org.sagebionetworks.repo.model.annotation.v2.Annotations annotationsV2 = status.getSubmissionAnnotations();
		if (annotationsV2 != null && annotationsV2.getAnnotations() != null) {
			Map<String, AnnotationsValue> mapValues = annotationsV2.getAnnotations();
			if (mapValues.containsKey(key)) {
				return mapValues.get(key).getValue().toString();
			}
		}
			Annotations annotations = status.getAnnotations();
			if (annotations == null) return null;
			List<StringAnnotation> sas = annotations.getStringAnnos();
			if (sas == null) return null;
			for (StringAnnotation sa : sas) {
				if (sa.getKey().equals(key)) return sa.getValue();
			}
			return null;
	}

	public static Long getLongAnnotation(SubmissionStatus status, String key) {
		org.sagebionetworks.repo.model.annotation.v2.Annotations annotationsV2 = status.getSubmissionAnnotations();
		if (annotationsV2 != null && annotationsV2.getAnnotations() != null) {
			Map<String, AnnotationsValue> mapValues = annotationsV2.getAnnotations();
			if (mapValues.containsKey(key)) {
				if(mapValues.size() > 0) {
					return Long.parseLong(mapValues.get(key).getValue().get(0));
				}
			}
		}

		Annotations annotations = status.getAnnotations();
		if (annotations==null) return null;
		List<LongAnnotation> las = annotations.getLongAnnos();
		if (las==null) return null;
		for (LongAnnotation la : las) {
			if (la.getKey().equals(key)) return la.getValue();
		}
		return null;
	}

	/*
	 * return true iff the (string) annotation is present and is 'true' (case insensitive)
	 */
	public static boolean isBooleanAnnotationPresentAndTrue(SubmissionStatus status, String key) {
		Annotations annotations = status.getAnnotations();
		if (annotations==null) return false;
		List<StringAnnotation> sas = annotations.getStringAnnos();
		if (sas==null) return false;
		for (StringAnnotation sa : sas) {
			if (sa.getKey().equals(key)) return new Boolean(sa.getValue());
		}
		return false;
	}

	public static void setAnnotation(SubmissionStatus status, String key, boolean isPrivate, AnnotationsValue value) {
		org.sagebionetworks.repo.model.annotation.v2.Annotations annotationsV2 = status.getSubmissionAnnotations();
		if ( annotationsV2 == null) {
			annotationsV2 = new org.sagebionetworks.repo.model.annotation.v2.Annotations();
			status.setSubmissionAnnotations(annotationsV2);
		}
		Map<String, AnnotationsValue> annotationValueMap = annotationsV2.getAnnotations();
		if ( annotationValueMap == null ) {
			annotationValueMap = new HashMap<String, AnnotationsValue>();
			annotationsV2.setAnnotations(annotationValueMap);
		}

		annotationValueMap.put(key, value);
	}

	public static void setAnnotation(SubmissionStatus status, String key, String value, boolean isPrivate) {
		if (value!=null && value.length()>499) value = value.substring(0, 499);
		Annotations annotations = status.getAnnotations();
		if (annotations==null) {
			annotations=new Annotations();
			status.setAnnotations(annotations);
		}
		List<StringAnnotation> sas = annotations.getStringAnnos();
		if (sas==null) {
			sas = new ArrayList<StringAnnotation>();
			annotations.setStringAnnos(sas);
		}
		StringAnnotation matchingSa = null;
		for (StringAnnotation existingSa : sas) {
			if (existingSa.getKey().equals(key)) {
				matchingSa = existingSa;
				break;
			}
		}
		if (matchingSa==null) {
			StringAnnotation sa = new StringAnnotation();
			sa.setIsPrivate(isPrivate);
			sa.setKey(key);
			sa.setValue(value);
			sas.add(sa);
		} else {
			matchingSa.setIsPrivate(isPrivate);
			matchingSa.setValue(value);
		}
	}

	public static void setAnnotation(SubmissionStatus status, String key, long value, boolean isPrivate) {
		Annotations annotations = status.getAnnotations();
		if (annotations==null) {
			annotations=new Annotations();
			status.setAnnotations(annotations);
		}
		List<LongAnnotation> las = annotations.getLongAnnos();
		if (las==null) {
			las = new ArrayList<LongAnnotation>();
			annotations.setLongAnnos(las);
		}
		LongAnnotation matchingLa = null;
		for (LongAnnotation existingLa : las) {
			if (existingLa.getKey().equals(key)) {
				matchingLa = existingLa;
				break;
			}
		}
		if (matchingLa==null) {
			LongAnnotation la = new LongAnnotation();
			la.setIsPrivate(isPrivate);
			la.setKey(key);
			la.setValue(value);
			las.add(la);
		} else {
			matchingLa.setIsPrivate(isPrivate);
			matchingLa.setValue(value);
		}
	}

	public static void setAnnotation(SubmissionStatus status, String key, double value, boolean isPrivate) {
		Annotations annotations = status.getAnnotations();
		if (annotations==null) {
			annotations=new Annotations();
			status.setAnnotations(annotations);
		}
		List<DoubleAnnotation> das = annotations.getDoubleAnnos();
		if (das==null) {
			das = new ArrayList<DoubleAnnotation>();
			annotations.setDoubleAnnos(das);
		}
		DoubleAnnotation matchingDa = null;
		for (DoubleAnnotation existingDa : das) {
			if (existingDa.getKey().equals(key)) {
				matchingDa = existingDa;
				break;
			}
		}
		if (matchingDa==null) {
			DoubleAnnotation da = new DoubleAnnotation();
			da.setIsPrivate(isPrivate);
			da.setKey(key);
			da.setValue(value);
			das.add(da);
		} else {
			matchingDa.setIsPrivate(isPrivate);
			matchingDa.setValue(value);
		}
	}

	public static void setStatus(SubmissionStatusModifications statusMods, SubmissionStatusEnum status, WorkflowUpdateStatus containerStatus) {
		statusMods.setStatus(status);
		switch(status) {
		case EVALUATION_IN_PROGRESS:
			setAnnotation(statusMods, STATUS_DESCRIPTION, STATUS_EVAL_IN_PROGRESS_ANNOTATION_VALUE, PUBLIC_ANNOTATION_SETTING);
			statusMods.setCanCancel(true);
			break;
		case ACCEPTED:
			setAnnotation(statusMods, STATUS_DESCRIPTION, STATUS_ACCEPTED_ANNOTATION_VALUE, PUBLIC_ANNOTATION_SETTING);
			statusMods.setCanCancel(false);
			break;
		case CLOSED:
			if (containerStatus==null) {
				setAnnotation(statusMods, STATUS_DESCRIPTION, status.name(), PUBLIC_ANNOTATION_SETTING);
			} else {
				switch (containerStatus) {
				case DOCKER_PULL_FAILED:
					setAnnotation(statusMods, STATUS_DESCRIPTION, DOCKER_PULL_FAILED_ANNOTATION_VALUE, PUBLIC_ANNOTATION_SETTING);
					break;
				case ERROR_ENCOUNTERED_DURING_EXECUTION:
					setAnnotation(statusMods, STATUS_DESCRIPTION, ERROR_DURING_EXECUTION_ANNOTATION_VALUE, PUBLIC_ANNOTATION_SETTING);
					break;
				case STOPPED_UPON_REQUEST:
					setAnnotation(statusMods, STATUS_DESCRIPTION, STOPPED_UPON_REQUEST_ANNOTATION_VALUE, PUBLIC_ANNOTATION_SETTING);
					break;
				case STOPPED_TIME_OUT:
					setAnnotation(statusMods, STATUS_DESCRIPTION, STOPPED_TIME_OUT_ANNOTATION_VALUE, PUBLIC_ANNOTATION_SETTING);
					break;
				default:
					setAnnotation(statusMods, STATUS_DESCRIPTION, containerStatus.name(), PUBLIC_ANNOTATION_SETTING);
				}
			}
			statusMods.setCanCancel(false);
			break;
		default:
			if (containerStatus==null) {
				setAnnotation(statusMods, STATUS_DESCRIPTION, status.name(), PUBLIC_ANNOTATION_SETTING);
			} else {
				setAnnotation(statusMods, STATUS_DESCRIPTION, containerStatus.name(), PUBLIC_ANNOTATION_SETTING);      			
			}
			statusMods.setCanCancel(false);    		
		}
	}
	
	private static void removeAnnotation(SubmissionStatus status, String key) {
		Annotations annotations = status.getAnnotations();
		if (annotations==null) return;

		List<StringAnnotation> sas = annotations.getStringAnnos();
		if (sas!=null) {
			for (Iterator<StringAnnotation> iterator = sas.iterator(); iterator.hasNext();) {
				StringAnnotation existing = iterator.next();
				if (existing.getKey().equals(key)) {
					iterator.remove();
				}
			}
		}

		List<DoubleAnnotation> das = annotations.getDoubleAnnos();
		if (das!=null) {
			for (Iterator<DoubleAnnotation> iterator = das.iterator(); iterator.hasNext();) {
				DoubleAnnotation existing = iterator.next();
				if (existing.getKey().equals(key)) {
					iterator.remove();
				}
			}
		}

		List<LongAnnotation> las = annotations.getLongAnnos();
		if (las!=null) {
			for (Iterator<LongAnnotation> iterator = las.iterator(); iterator.hasNext();) {
				LongAnnotation existing = iterator.next();
				if (existing.getKey().equals(key)) {
					iterator.remove();
				}
			}
		}

		org.sagebionetworks.repo.model.annotation.v2.Annotations annotationsV2 = status.getSubmissionAnnotations();
		if(annotationsV2 == null) return;
		Map<String, AnnotationsValue> annotationsValueMap = annotationsV2.getAnnotations();
		if (annotationsValueMap != null){
			if (annotationsValueMap.containsKey(key)){
				annotationsValueMap.remove(key);
				annotationsV2.setAnnotations(annotationsValueMap);
				status.setSubmissionAnnotations(annotationsV2);
			}
		}
	}
	
	private static void removeAnnotationIntern(SubmissionStatusModifications statusMods, String key) {
		for (Iterator<AnnotationBase> it = statusMods.getAnnotationsToAdd().iterator(); it.hasNext();) {
			if (it.next().getKey().equals(key)) it.remove();
		}
	}

	public static void setAnnotationStringV2(SubmissionStatusModifications statusMods, String key, String value, boolean isPrivate) {
		removeAnnotationIntern(statusMods, key); // make sure the key is not in the list
		AnnotationsValue annotationsValue = new AnnotationsValue();
		annotationsValue.setType(AnnotationsValueType.STRING);
		List<String> stringList = new ArrayList<>();
		stringList.add(value);
		annotationsValue.setValue(stringList);
		//statusMods.getAnnotationsToAdd().add(annotationsValue);


	}

	public static void setAnnotation(SubmissionStatusModifications statusMods, String key, String value, boolean isPrivate) {
		removeAnnotationIntern(statusMods, key); // make sure the key is not in the list
		StringAnnotation annot = new StringAnnotation();
		annot.setKey(key);
		annot.setValue(value);
		annot.setIsPrivate(isPrivate);
		statusMods.getAnnotationsToAdd().add(annot);
		statusMods.getAnnotationNamesToRemove().remove(key); // if a key had been scheduled to be removed, now unschedule
	}
	
	public static void setAnnotation(SubmissionStatusModifications statusMods, String key, long value, boolean isPrivate) {
		removeAnnotationIntern(statusMods, key); // make sure the key is not in the list
		LongAnnotation annot = new LongAnnotation();
		annot.setKey(key);
		annot.setValue(value);
		annot.setIsPrivate(isPrivate);
		statusMods.getAnnotationsToAdd().add(annot);
		statusMods.getAnnotationNamesToRemove().remove(key); // if a key had been scheduled to be removed, now unschedule
	}
	
	public static void setAnnotation(SubmissionStatusModifications statusMods, String key, double value, boolean isPrivate) {
		removeAnnotationIntern(statusMods, key); // make sure the key is not in the list
		DoubleAnnotation annot = new DoubleAnnotation();
		annot.setKey(key);
		annot.setValue(value);
		annot.setIsPrivate(isPrivate);
		statusMods.getAnnotationsToAdd().add(annot);
		statusMods.getAnnotationNamesToRemove().remove(key); // if a key had been scheduled to be removed, now unschedule
	}
	
	public static void removeAnnotation(SubmissionStatusModifications statusMods, String key) {
		statusMods.getAnnotationNamesToRemove().add(key);
		removeAnnotationIntern(statusMods, key); // make sure the key is not in the list
	}

	public static void applyModifications(final SubmissionStatus submissionStatus, final SubmissionStatusModifications statusMods) {
		// BEGIN NEW ANNOTATION CODE
		for (AnnotationBase annot : statusMods.getAnnotationsToAdd()) {
			if (annot instanceof StringAnnotation) 
				setAnnotation(submissionStatus, annot.getKey(), ((StringAnnotation) annot).getValue(), annot.getIsPrivate());
			if (annot instanceof LongAnnotation) 
				setAnnotation(submissionStatus, annot.getKey(), ((LongAnnotation) annot).getValue(), annot.getIsPrivate());
			if (annot instanceof DoubleAnnotation) 
				setAnnotation(submissionStatus, annot.getKey(), ((DoubleAnnotation) annot).getValue(), annot.getIsPrivate());
			if (annot instanceof AnnotationsValue)
				setAnnotation(submissionStatus, annot.getKey(), annot.getIsPrivate(), (AnnotationsValue)annot);
		}

		for (String key : statusMods.getAnnotationNamesToRemove()) removeAnnotation(submissionStatus, key);
		if (statusMods.getStatus()!=null) submissionStatus.setStatus(statusMods.getStatus());
		if (statusMods.getCanCancel()!=null) submissionStatus.setCanCancel(statusMods.getCanCancel());
		if (statusMods.getCancelRequested()!=null) submissionStatus.setCancelRequested(statusMods.getCancelRequested());
	}
	
	public Evaluation getEvaluation(String evaluationId) throws SynapseException {
		return synapse.getEvaluation(evaluationId);
	}
	
	/**
	 * 
	 * @throws SynapseException
	 * @throws IOException
	 * @throws JSONObjectAdapterException
	 */
	public List<SubmissionBundle> selectSubmissions(String evaluationId, SubmissionStatusEnum state) throws SynapseException, IOException, JSONObjectAdapterException {
		long total = Integer.MAX_VALUE;
		List<SubmissionBundle> result = new ArrayList<SubmissionBundle>();
		Set<String> ids = new HashSet<String>();
		for (int offset=0; offset<total; offset+=PAGE_SIZE) {
			PaginatedResults<SubmissionBundle> submissionPGs = null;
			submissionPGs = synapse.getAllSubmissionBundlesByStatus(evaluationId, 
					state, offset, PAGE_SIZE);
			total = (int)submissionPGs.getTotalNumberOfResults();
			List<SubmissionBundle> page = submissionPGs.getResults();
			for (int i=0; i<page.size(); i++) {
				SubmissionBundle bundle = page.get(i);
				result.add(bundle);
				String id = bundle.getSubmission().getId();
				if (ids.contains(id)) throw new IllegalStateException("List has multiple copies of "+id);
				ids.add(id);
				if (!bundle.getSubmissionStatus().getStatus().equals(state))
					throw new RuntimeException("Submission "+id+" has state "+bundle.getSubmissionStatus().getStatus()+" when "+state+" was expected.");
			}
		}
		return result;
	}

	private static String formatInterval(final long l) {
		final long hr = TimeUnit.MILLISECONDS.toHours(l);
		final long min = TimeUnit.MILLISECONDS.toMinutes(l - TimeUnit.HOURS.toMillis(hr));
		final long sec = TimeUnit.MILLISECONDS.toSeconds(l - TimeUnit.HOURS.toMillis(hr) - TimeUnit.MINUTES.toMillis(min));
		final long ms = TimeUnit.MILLISECONDS.toMillis(l - TimeUnit.HOURS.toMillis(hr) - TimeUnit.MINUTES.toMillis(min) - TimeUnit.SECONDS.toMillis(sec));
		return String.format("%02dh:%02dm:%02d.%03ds", hr, min, sec, ms);
	}

	public static Long getTimeRemaining(SubmissionStatus ss) {
		return getLongAnnotation(ss, TIME_REMAINING);
	}

	/*
	 * If time remaining is missing then return true, i.e. the submission is
	 * enabled by default
	 */
	public static boolean hasTimeRemaining(Long timeRemaining) {
		return timeRemaining==null || timeRemaining>0L;
	}
}
