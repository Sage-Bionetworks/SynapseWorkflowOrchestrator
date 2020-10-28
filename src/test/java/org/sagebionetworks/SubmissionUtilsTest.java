package org.sagebionetworks;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.exceptions.SynapseConflictingUpdateException;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.annotation.StringAnnotation;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.SubmissionUtils.getRepoSuffixFromImage;


@RunWith(MockitoJUnitRunner.class)
public class SubmissionUtilsTest {

	private static final String VALID_COMMIT = "docker.synapse.org/syn123/my-repo@sha256:9e25a2dcbacacab7777095e497bd45286d53656440a72224d52d09fa3da3c8d3";

	private File fileToDelete;

	@Mock
	SynapseClient synapse;

	@After 
	public void tearDown() throws Exception {
		if (fileToDelete!=null) fileToDelete.delete();
		fileToDelete = null;
	}

	@Test 
	public void testGetRepoSuffixFromImage() throws Exception {
		assertEquals("foo/bar", getRepoSuffixFromImage("docker.synapse.org/syn12345/foo/bar@sha256:abcdefg"));
		assertEquals("foo/bar", getRepoSuffixFromImage("docker.synapse.org/syn12345/foo/bar"));
		assertEquals("foo", getRepoSuffixFromImage("docker.synapse.org/syn12345/foo@sha256:abcdefg"));
		assertEquals("ubuntu", getRepoSuffixFromImage("ubuntu@sha256:abcdefg"));
		assertEquals("foo/bar", getRepoSuffixFromImage("syn12345/foo/bar@sha256:abcdefg"));
		assertEquals("foo/bar", getRepoSuffixFromImage("docker.synapse.org/foo/bar@sha256:abcdefg"));
	}

	@Test
	public void testGetEntityTypeFromSubmission() throws Exception {
		Submission submission = new Submission();
		EntityBundle bundle = new EntityBundle();
		FileEntity fileEntity = new FileEntity();
		bundle.setEntity(fileEntity);
		JSONObjectAdapter joa = new JSONObjectAdapterImpl();
		bundle.writeToJSONObject(joa);
		submission.setEntityBundleJSON(joa.toJSONString());
		assertEquals(FileEntity.class, SubmissionUtils.getEntityTypeFromSubmission(submission));
	}

	private static final String SUBMISSION_ID = "987654";
	
	@Test
	public void testUpdateSubmissionStatusHappyCase() throws Exception {
		SubmissionUtils submissionUtils = new SubmissionUtils(synapse);
		
		SubmissionStatus submissionStatusPreviouslyRetrieved = new SubmissionStatus();
		submissionStatusPreviouslyRetrieved.setId(SUBMISSION_ID);
		submissionStatusPreviouslyRetrieved.setEtag("1");

		SubmissionStatusModifications statusMods = new SubmissionStatusModifications();
		statusMods.setCanCancel(false);
		
		// method under test
		submissionUtils.updateSubmissionStatus(submissionStatusPreviouslyRetrieved, statusMods);
		
		// one update
		ArgumentCaptor<SubmissionStatus> updateStatusCaptor = ArgumentCaptor.forClass(SubmissionStatus.class);
		verify(synapse).updateSubmissionStatus(updateStatusCaptor.capture());
		
		// expect that this is sent to the back end
		SubmissionStatus expected = new SubmissionStatus();
		expected.setId(SUBMISSION_ID);
		expected.setCanCancel(false);
		expected.setEtag("1");
		assertEquals(expected, updateStatusCaptor.getValue());
		
		// no need to refresh
		verify(synapse, never()).getSubmissionStatus(SUBMISSION_ID);
	}

	@Test
	public void testUpdateSubmissionApplyUpdates() throws Exception {
		SubmissionUtils submissionUtils = new SubmissionUtils(synapse);
		
		SubmissionStatus submissionStatusPreviouslyRetrieved = new SubmissionStatus();
		submissionStatusPreviouslyRetrieved.setId(SUBMISSION_ID);
		submissionStatusPreviouslyRetrieved.setEtag("1");
		SubmissionStatus submissionStatusFromBackend = new SubmissionStatus();
		submissionStatusFromBackend.setId(SUBMISSION_ID);
		submissionStatusFromBackend.setEtag("2");
		
		when(synapse.updateSubmissionStatus(eq(submissionStatusPreviouslyRetrieved))).thenThrow(SynapseConflictingUpdateException.class);
		
		when(synapse.getSubmissionStatus(SUBMISSION_ID)).thenReturn(submissionStatusFromBackend);

		SubmissionStatusModifications statusMods = new SubmissionStatusModifications();
		statusMods.setCanCancel(false);
		
		// method under test
		submissionUtils.updateSubmissionStatus(submissionStatusPreviouslyRetrieved, statusMods);
		
		// one update
		ArgumentCaptor<SubmissionStatus> updateStatusCaptor = ArgumentCaptor.forClass(SubmissionStatus.class);
		verify(synapse, times(2)).updateSubmissionStatus(updateStatusCaptor.capture());
		
		// expect that this is sent to the back end
		SubmissionStatus expected = new SubmissionStatus();
		expected.setId(SUBMISSION_ID);
		expected.setCanCancel(false);
		expected.setEtag("2");
		assertEquals(expected, updateStatusCaptor.getValue());
		
		// no need to refresh
		verify(synapse).getSubmissionStatus(SUBMISSION_ID);
	}

	@Test
	public void testSubmissionStatusModsStringAnnotation() {
		SubmissionStatusModifications mods = new SubmissionStatusModifications();
		assertTrue(mods.getAnnotationsToAdd().isEmpty());
		StringAnnotation stringAnnotation = new StringAnnotation();
		stringAnnotation.setValue("test1");
		mods.getAnnotationsToAdd().add(stringAnnotation);
		assertFalse(mods.getAnnotationsToAdd().isEmpty());
	}


}
