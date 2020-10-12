package org.sagebionetworks;

import org.junit.Test;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.repo.model.annotation.Annotations;
import org.sagebionetworks.repo.model.annotation.StringAnnotation;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.sagebionetworks.Constants.EXECUTION_STAGE_PROPERTY_NAME;
import static org.sagebionetworks.EvaluationUtils.applyModifications;
import static org.sagebionetworks.EvaluationUtils.removeAnnotation;
import static org.sagebionetworks.EvaluationUtils.setAnnotation;
import static org.sagebionetworks.EvaluationUtils.setStatus;


public class EvaluationUtilsTest {

	@Test
	public void testGetInitialSubmissionState() throws Exception {
		System.setProperty(EXECUTION_STAGE_PROPERTY_NAME, "");
		assertEquals(SubmissionStatusEnum.RECEIVED, EvaluationUtils.getInitialSubmissionState());
	}
	
	@Test
	public void testUpdateSubmissionHappyCase() throws Exception {
		SubmissionStatusModifications statusMods = new SubmissionStatusModifications();
		SubmissionStatus expected = new SubmissionStatus();
		SubmissionStatus actual = new SubmissionStatus();
		
		setAnnotation(expected, "foo1", "bar", false);
		setAnnotation(statusMods, "foo1", "bar", false);
		setAnnotation(expected, "foo2", 1L, false);
		setAnnotation(statusMods, "foo2", 1L, false);
		setAnnotation(expected, "foo3", 3.14D, true);
		setAnnotation(statusMods, "foo3", 3.14D, true);
		
		expected.setCanCancel(true);
		statusMods.setCanCancel(true);
		
		expected.setCancelRequested(false);
		statusMods.setCancelRequested(false);
		
		expected.setStatus(SubmissionStatusEnum.VALIDATED);
		statusMods.setStatus(SubmissionStatusEnum.VALIDATED);
		
		applyModifications(actual, statusMods);
		assertEquals(expected, actual);
	}
	
	
	@Test
	public void testUpdateSubmissionRemove() throws Exception {
		
		SubmissionStatusModifications statusMods = new SubmissionStatusModifications();
		SubmissionStatus expected = new SubmissionStatus();
		SubmissionStatus actual = new SubmissionStatus();
		
		removeAnnotation(statusMods, "foo1");

		setAnnotation(statusMods, "foo1", "bar", true);
		setAnnotation(statusMods, "foo2", "bar", false);
		applyModifications(actual, statusMods);
		
		statusMods = new SubmissionStatusModifications();
		setAnnotation(statusMods, "foo2", "baz", false);
		removeAnnotation(statusMods, "foo1");
		applyModifications(actual, statusMods);

		Annotations annotations = new Annotations();
		StringAnnotation sa = new StringAnnotation();
		sa.setKey("foo2");
		sa.setValue("baz");
		sa.setIsPrivate(false);
		annotations.setStringAnnos(Collections.singletonList(sa));
		expected.setAnnotations(annotations);
		
		assertEquals(expected, actual);
	}

	@Test
	public void testSetStatusCancelRequest() throws Exception {
		SubmissionStatusModifications statusMods = new SubmissionStatusModifications();
		assertNotNull(statusMods.getAnnotationsToAdd());
		assertEquals(0, statusMods.getAnnotationsToAdd().size());
		SubmissionStatusEnum submissionStatusEnum = SubmissionStatusEnum.INVALID;
		WorkflowUpdateStatus containerStatus = WorkflowUpdateStatus.STOPPED_UPON_REQUEST;
		setStatus(statusMods, submissionStatusEnum, containerStatus);
		assertNotNull(statusMods);
		assertEquals(submissionStatusEnum, statusMods.getStatus());
		assertNotNull(statusMods.getAnnotationsToAdd());
		assertEquals(1, statusMods.getAnnotationsToAdd().size());
		String expectedKey = "orgSagebionetworksSynapseWorkflowOrchestratorStatusDescription";
		assertNotNull(expectedKey, statusMods.getAnnotationsToAdd().get(0).getKey());
	}

}
