package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.sagebionetworks.Constants.EXECUTION_STAGE_PROPERTY_NAME;
import static org.sagebionetworks.EvaluationUtils.TIME_REMAINING;
import static org.sagebionetworks.EvaluationUtils.applyModifications;
import static org.sagebionetworks.EvaluationUtils.getLongAnnotation;
import static org.sagebionetworks.EvaluationUtils.getStringAnnotation;
import static org.sagebionetworks.EvaluationUtils.getTimeRemaining;
import static org.sagebionetworks.EvaluationUtils.removeAnnotation;
import static org.sagebionetworks.EvaluationUtils.setAnnotation;
import static org.sagebionetworks.EvaluationUtils.setStatus;

import java.util.ArrayList;
import java.util.Collections;

import org.junit.Test;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.repo.model.annotation.Annotations;
import org.sagebionetworks.repo.model.annotation.LongAnnotation;
import org.sagebionetworks.repo.model.annotation.StringAnnotation;


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

		expected.setSubmissionAnnotations(AnnotationsTranslator.translateToAnnotationsV2(expected.getAnnotations()));

		
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
		expected.setSubmissionAnnotations(AnnotationsTranslator.translateToAnnotationsV2(expected.getAnnotations()));
		
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
		org.sagebionetworks.repo.model.annotation.v2.Annotations annot2 = AnnotationsTranslator.translateToAnnotationsV2(expected.getAnnotations());
		expected.setSubmissionAnnotations(annot2);
		setAnnotation(statusMods, "foo1", "bar", false);

		// Call under test
		applyModifications(actual, statusMods);
		assertEquals(expected, actual);
	}

	@Test
	public void testApplyModificationsLong() throws Exception {
		SubmissionStatusModifications statusMods = new SubmissionStatusModifications();
		SubmissionStatus expected = new SubmissionStatus();
		SubmissionStatus actual = new SubmissionStatus();

		setAnnotation(expected, "foo2", 1L, false);
		org.sagebionetworks.repo.model.annotation.v2.Annotations annot2 = AnnotationsTranslator.translateToAnnotationsV2(expected.getAnnotations());
		expected.setSubmissionAnnotations(annot2);
		setAnnotation(statusMods, "foo2", 1L, false);

		// Call under test
		applyModifications(actual, statusMods);
		assertEquals(expected, actual);
	}

	@Test
	public void testApplyModificationsDouble() throws Exception {
		SubmissionStatusModifications statusMods = new SubmissionStatusModifications();
		SubmissionStatus expected = new SubmissionStatus();
		SubmissionStatus actual = new SubmissionStatus();

		setAnnotation(expected, "foo3", 3.14D, true);
		org.sagebionetworks.repo.model.annotation.v2.Annotations annot2 = AnnotationsTranslator.translateToAnnotationsV2(expected.getAnnotations());
		expected.setSubmissionAnnotations(annot2);
		setAnnotation(statusMods, "foo3", 3.14D, true);

		// Call under test
		applyModifications(actual, statusMods);
		assertEquals(expected, actual);
	}

	@Test
	public void testRemoveAnnotationsV2OneValue() throws Exception {
		SubmissionStatus actual = new SubmissionStatus();
		setAnnotation(actual, "foo1", "baz1", false);
		org.sagebionetworks.repo.model.annotation.v2.Annotations annot2 = AnnotationsTranslator.translateToAnnotationsV2(actual.getAnnotations());
		actual.setSubmissionAnnotations(annot2);
		// Call under test
		removeAnnotation(actual, "foo1");
		assertNotNull(actual);
		assertNotNull(actual.getSubmissionAnnotations());
		assertNotNull(actual.getSubmissionAnnotations().getAnnotations());
		assertEquals(0, actual.getSubmissionAnnotations().getAnnotations().size());
		assertNotNull(actual.getAnnotations());
		assertNotNull(actual.getAnnotations().getStringAnnos());
		assertEquals(0, actual.getAnnotations().getStringAnnos().size());
	}

	@Test
	public void testRemoveAnnotationsMultipleValuesV2() throws Exception {
		SubmissionStatus actual = new SubmissionStatus();
		Long valueToRemove = 1L;
		Long secondValue = 2L;
		setAnnotation(actual, "foo1", valueToRemove, false);
		setAnnotation(actual, "foo2", secondValue, false);
		actual.setSubmissionAnnotations(AnnotationsTranslator.translateToAnnotationsV2(actual.getAnnotations()));
		// Call under test
		removeAnnotation(actual, "foo1");
		assertNotNull(actual);
		assertNotNull(actual.getSubmissionAnnotations());
		assertNotNull(actual.getSubmissionAnnotations().getAnnotations());
		assertEquals(1, actual.getSubmissionAnnotations().getAnnotations().size());
		assertFalse(actual.getSubmissionAnnotations().getAnnotations().containsKey("foo1"));
		assertTrue(actual.getSubmissionAnnotations().getAnnotations().containsKey("foo2"));
		assertEquals(secondValue.toString(), actual.getSubmissionAnnotations().getAnnotations().get("foo2").getValue().get(0));
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
	public void testTimeRemaining() throws Exception {
		SubmissionStatus status = new SubmissionStatus();
		org.sagebionetworks.repo.model.annotation.Annotations annotations = new org.sagebionetworks.repo.model.annotation.Annotations();
		Long expectedTime = 5L;
		LongAnnotation longAnnotation = new LongAnnotation();
		longAnnotation.setKey(TIME_REMAINING);
		longAnnotation.setValue(expectedTime);
		annotations.setLongAnnos(new ArrayList<LongAnnotation>());
		annotations.getLongAnnos().add(longAnnotation);
		status.setAnnotations(annotations);
		Long result = getTimeRemaining(status);
		assertEquals(expectedTime, result);
	}
}
