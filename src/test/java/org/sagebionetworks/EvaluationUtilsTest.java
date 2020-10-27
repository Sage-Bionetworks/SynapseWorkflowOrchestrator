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
import static org.sagebionetworks.EvaluationUtils.getLongAnnotation;
import static org.sagebionetworks.EvaluationUtils.getLongAnnotationV2;
import static org.sagebionetworks.EvaluationUtils.getStringAnnotation;
import static org.sagebionetworks.EvaluationUtils.getStringAnnotationV2;
import static org.sagebionetworks.EvaluationUtils.removeAnnotation;
import static org.sagebionetworks.EvaluationUtils.setAnnotation;
import static org.sagebionetworks.EvaluationUtils.setAnnotationDoubleV2;
import static org.sagebionetworks.EvaluationUtils.setAnnotationLongV2;
import static org.sagebionetworks.EvaluationUtils.setAnnotationStringV2;
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
		setAnnotationStringV2(expected, "foo1", "bar", false);
		setAnnotation(statusMods, "foo1", "bar", false);

		setAnnotation(expected, "foo2", 1L, false);
		setAnnotationLongV2(expected, "foo2", 1L, false);;
		setAnnotation(statusMods, "foo2", 1L, false);

		setAnnotation(expected, "foo3", 3.14D, true);
		setAnnotationDoubleV2(expected, "foo3", 3.14D, false);;
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
		setAnnotationStringV2(expected, "foo2", "bar", false);
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
		setAnnotationStringV2(expected, "foo2", "baz", false);
		
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

	@Test
	public void testApplyModificationsString() throws Exception {
		SubmissionStatusModifications statusMods = new SubmissionStatusModifications();
		SubmissionStatus expected = new SubmissionStatus();
		SubmissionStatus actual = new SubmissionStatus();

		setAnnotation(expected, "foo1", "bar", false);
		setAnnotationStringV2(expected, "foo1", "bar", false);
		setAnnotation(statusMods, "foo1", "bar", false);

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
	public void testApplyModificationsLong() throws Exception {
		SubmissionStatusModifications statusMods = new SubmissionStatusModifications();
		SubmissionStatus expected = new SubmissionStatus();
		SubmissionStatus actual = new SubmissionStatus();

		setAnnotation(expected, "foo2", 1L, false);
		setAnnotationLongV2(expected, "foo2", 1L, false);
		setAnnotation(statusMods, "foo2", 1L, false);

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
	public void testApplyModificationsDouble() throws Exception {
		SubmissionStatusModifications statusMods = new SubmissionStatusModifications();
		SubmissionStatus expected = new SubmissionStatus();
		SubmissionStatus actual = new SubmissionStatus();

		setAnnotation(expected, "foo3", 3.14D, true);
		setAnnotationDoubleV2(expected, "foo3", 3.14D, false);
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
	public void testRemoveAnnotationsStringV2() throws Exception {
		SubmissionStatus expected = new SubmissionStatus();
		SubmissionStatus actual = new SubmissionStatus();
		setAnnotationStringV2(actual, "foo1", "baz1", false);
		// Call under test
		removeAnnotation(actual, "foo1");
		assertEquals(expected, actual);
	}

	@Test
	public void testRemoveAnnotationsLongV2() throws Exception {
		SubmissionStatus expected = new SubmissionStatus();
		SubmissionStatus actual = new SubmissionStatus();
		setAnnotationLongV2(actual, "foo1", 1L, false);
		removeAnnotation(actual, "foo1");
		assertEquals(expected, actual);
	}

	@Test
	public void testRemoveAnnotationsDoubleV2() throws Exception {
		SubmissionStatus expected = new SubmissionStatus();
		SubmissionStatus actual = new SubmissionStatus();
		setAnnotationDoubleV2(actual, "foo1", 3.14D, false);
		// Call under test
		removeAnnotation(actual, "foo1");
		assertEquals(expected, actual);
	}

	@Test
	public void testGetStringAnnotation() throws Exception {
		SubmissionStatus status = new SubmissionStatus();
		String expectedValue = "bar1";
		String key = "foo1";
		setAnnotation(status, key, expectedValue, false);
		// Call under test
		String actualValue = getStringAnnotation(status, key);
		assertEquals(expectedValue, actualValue);
	}

	@Test
	public void testGetStringAnnotationV2() throws Exception {
		SubmissionStatus status = new SubmissionStatus();
		String expectedValue = "bar1";
		String key = "foo1";
		setAnnotationStringV2(status, key, expectedValue, false);
		// Call under test
		String actualValue = getStringAnnotationV2(status, key);
		assertEquals(expectedValue, actualValue);
	}

	@Test
	public void testGetLongAnnotation() throws Exception {
		SubmissionStatus status = new SubmissionStatus();
		Long expectedValue = 1L;
		String key = "foo1";
		setAnnotation(status, key, expectedValue, false);
		// Call under test
		Long actualValue = getLongAnnotation(status, key);
		assertEquals(expectedValue, actualValue);
	}

	@Test
	public void testGetLongAnnotationV2() throws Exception {
		SubmissionStatus status = new SubmissionStatus();
		Long expectedValue = 1L;
		String key = "foo1";
		setAnnotationLongV2(status, key, expectedValue, false);
		// Call under test
		Long actualValue = getLongAnnotationV2(status, key);
		assertEquals(expectedValue, actualValue);
	}
}
