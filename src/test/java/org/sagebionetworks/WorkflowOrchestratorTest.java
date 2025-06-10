package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.Constants.AGENT_SHARED_DIR_DEFAULT;
import static org.sagebionetworks.Constants.AGENT_SHARED_DIR_PROPERTY_NAME;
import static org.sagebionetworks.Constants.COMPOSE_PROJECT_NAME_ENV_VAR;
import static org.sagebionetworks.Constants.DOCKER_ENGINE_URL_PROPERTY_NAME;
import static org.sagebionetworks.Constants.ROOT_TEMPLATE_ANNOTATION_NAME;
import static org.sagebionetworks.Constants.SHARED_VOLUME_NAME;
import static org.sagebionetworks.Constants.SYNAPSE_PAT_PROPERTY;
import static org.sagebionetworks.Constants.SYNAPSE_USERNAME_PROPERTY;
import static org.sagebionetworks.Utils.dockerComposeName;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.evaluation.model.SubmissionBundle;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValue;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleResults;

@ExtendWith(MockitoExtension.class)
public class WorkflowOrchestratorTest {
	
	@Mock
	SynapseClient mockSynapse;

	@Mock
	EvaluationUtils mockEvaluationUtils;
	
	@Mock
	DockerUtils mockDockerUtils;
	
	@Mock
	SubmissionUtils mockSubmissionUtils;
	
	private WorkflowOrchestrator workflowOrchestrator;
	
	private static final String USER_ID = "000";
	private static final String EVALUATION_ID = "111";
	private static final String SUBMISSION_ID = "222";
	private static final String WORKFLOW_SYN_ID = "3333";
	
	private static final WorkflowURLEntrypointAndSynapseRef WORKFLOW_REF;
	
	private static final String WORKFLOW_OUTPUT_ROOT_ENTITY_ID = "syn1234";
	private static final String FOLDER_ID = "syn5678";
	
	private static final URL WORKFLOW_URL;
	private static final String WORKFLOW_ENTRYPOINT = "SynapseWorkflowExample-master/workflow-entrypoint.cwl";
	
	static {
		
		try {
			WORKFLOW_URL = new URL("https://github.com/Sage-Bionetworks/SynapseWorkflowExample/archive/master.zip");
			WORKFLOW_REF = new WorkflowURLEntrypointAndSynapseRef(WORKFLOW_URL, WORKFLOW_ENTRYPOINT, WORKFLOW_SYN_ID);
		}catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@BeforeEach
	public void setUp() throws Exception {
		System.setProperty("WORKFLOW_OUTPUT_ROOT_ENTITY_ID", WORKFLOW_OUTPUT_ROOT_ENTITY_ID);
		System.setProperty("SYNAPSE_USERNAME", "foo");
		System.setProperty("SYNAPSE_PAT", "bar");
		System.setProperty(DOCKER_ENGINE_URL_PROPERTY_NAME, "unix:///var/run/docker.sock");
		System.setProperty(COMPOSE_PROJECT_NAME_ENV_VAR, "project");

		System.setProperty(AGENT_SHARED_DIR_PROPERTY_NAME, System.getProperty("java.io.tmpdir"));
		
		long sleepTimeMillis = 1*60*1000L;
		workflowOrchestrator = new WorkflowOrchestrator(
				mockSynapse, mockEvaluationUtils,
				mockDockerUtils, mockSubmissionUtils, sleepTimeMillis);

	}
	
	@AfterEach
	public void tearDown() throws Exception {
		System.clearProperty("WORKFLOW_OUTPUT_ROOT_ENTITY_ID");
		System.clearProperty("SYNAPSE_USERNAME");
		System.clearProperty("SYNAPSE_PAT");
		System.clearProperty(DOCKER_ENGINE_URL_PROPERTY_NAME);
		System.clearProperty("EVALUATION_TEMPLATES");
		System.setProperty(AGENT_SHARED_DIR_PROPERTY_NAME, AGENT_SHARED_DIR_DEFAULT);
	}

	@Test
	public void testCreateNewWorkflowJobs() throws Throwable {
		SubmissionBundle bundle = new SubmissionBundle();
		Submission submission = new Submission();
		submission.setId(SUBMISSION_ID);
		submission.setUserId(USER_ID);
		bundle.setSubmission(submission);
		SubmissionStatus submissionStatus = new SubmissionStatus();
		bundle.setSubmissionStatus(submissionStatus);
		when(mockDockerUtils.getVolumeMountPoint(dockerComposeName(SHARED_VOLUME_NAME))).thenReturn(System.getProperty("java.io.tmpdir"));
		when(mockEvaluationUtils.selectSubmissions(EVALUATION_ID, SubmissionStatusEnum.RECEIVED)).thenReturn(Collections.singletonList(bundle));
		UserProfile profile = new UserProfile();
		profile.setOwnerId("1111");
		Folder folder = new Folder();
		folder.setId(FOLDER_ID);
		when(mockSynapse.createEntity(any(Folder.class))).thenReturn(folder);
		// method under test
		workflowOrchestrator.createNewWorkflowJobs(EVALUATION_ID, WORKFLOW_REF);
		
		verify(mockEvaluationUtils).selectSubmissions(EVALUATION_ID, SubmissionStatusEnum.RECEIVED);
	}

	@Test
	public void getWorkflowURLAndEntrypointNullAnnotations() throws Exception {
		JSONObject o = new JSONObject();
		o.put(EVALUATION_ID,  WORKFLOW_OUTPUT_ROOT_ENTITY_ID);
		System.setProperty("EVALUATION_TEMPLATES", o.toString());
		FileHandleResults results = new FileHandleResults();
		List<FileHandle> fileHandleList = new ArrayList<>();
		ExternalFileHandle fileHandle = new ExternalFileHandle();
		fileHandle.setExternalURL(WORKFLOW_URL.toString());
		fileHandleList.add(fileHandle);
		results.setList(fileHandleList);
		when(mockSynapse.getEntityFileHandlesForCurrentVersion(any())).thenReturn(results);
		when(mockSynapse.getAnnotationsV2(any())).thenReturn(new Annotations());
		String expectedErrorMessage = "Expected annotation called "+
				ROOT_TEMPLATE_ANNOTATION_NAME+" for "+ WORKFLOW_OUTPUT_ROOT_ENTITY_ID +" but the entity has null or empty annotation map.";
		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			workflowOrchestrator.getWorkflowURLAndEntrypoint();
		});
		assertEquals(expectedErrorMessage, exception.getMessage());
	}

	@Test
	public void getWorkflowURLAndEntrypointMissingValue() throws Exception {
		JSONObject o = new JSONObject();
		o.put(EVALUATION_ID,  WORKFLOW_OUTPUT_ROOT_ENTITY_ID);
		System.setProperty("EVALUATION_TEMPLATES", o.toString());
		FileHandleResults results = new FileHandleResults();
		List<FileHandle> fileHandleList = new ArrayList<>();
		ExternalFileHandle fileHandle = new ExternalFileHandle();
		fileHandle.setExternalURL(WORKFLOW_URL.toString());
		fileHandleList.add(fileHandle);
		results.setList(fileHandleList);
		when(mockSynapse.getEntityFileHandlesForCurrentVersion(any())).thenReturn(results);
		Annotations annotations = new Annotations();
		Map<String, AnnotationsValue> valueMap = new HashMap<>();
		valueMap.put("otherAnnotation", new AnnotationsValue());
		annotations.setAnnotations(valueMap);
 		when(mockSynapse.getAnnotationsV2(any())).thenReturn(annotations);
		String expectedErrorMessage = WORKFLOW_OUTPUT_ROOT_ENTITY_ID + " has no annotation called "+ROOT_TEMPLATE_ANNOTATION_NAME;
		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			workflowOrchestrator.getWorkflowURLAndEntrypoint();
		});
		assertEquals(expectedErrorMessage, exception.getMessage());
	}

	@Test
	public void getWorkflowURLAndEntrypointWrongAnnotationType() throws Exception {
		JSONObject o = new JSONObject();
		o.put(EVALUATION_ID,  WORKFLOW_OUTPUT_ROOT_ENTITY_ID);
		System.setProperty("EVALUATION_TEMPLATES", o.toString());
		FileHandleResults results = new FileHandleResults();
		List<FileHandle> fileHandleList = new ArrayList<>();
		ExternalFileHandle fileHandle = new ExternalFileHandle();
		fileHandle.setExternalURL(WORKFLOW_URL.toString());
		fileHandleList.add(fileHandle);
		results.setList(fileHandleList);
		when(mockSynapse.getEntityFileHandlesForCurrentVersion(any())).thenReturn(results);
		Annotations annotations = new Annotations();
		Map<String, AnnotationsValue> valueMap = new HashMap<>();
		AnnotationsValue annotationsValue = new AnnotationsValue();
		annotationsValue.setValue(Collections.singletonList("stringValue"));
		annotationsValue.setType(AnnotationsValueType.DOUBLE);
		valueMap.put(ROOT_TEMPLATE_ANNOTATION_NAME, annotationsValue );
		annotations.setAnnotations(valueMap);
		when(mockSynapse.getAnnotationsV2(any())).thenReturn(annotations);
		String expectedErrorMessage = ROOT_TEMPLATE_ANNOTATION_NAME + " has wrong annotation type";
		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			workflowOrchestrator.getWorkflowURLAndEntrypoint();
		});
		assertEquals(expectedErrorMessage, exception.getMessage());
	}

	@Test 
	public void testUpdateWorkflowJobs() throws Throwable {
		workflowOrchestrator.updateWorkflowJobs(Collections.singletonList(EVALUATION_ID));
	}

	private static String ZIP_FILE_URL = "https://github.com/Sage-Bionetworks/SynapseWorkflowExample/archive/master.zip";
	private static String ROOT_TEMPLATE = "SynapseWorkflowExample-master/workflow-entrypoint.cwl";

	@Disabled
	@Test
	public void testGetWorkflowURLAndEntrypoint() throws Throwable {
		System.setProperty(SYNAPSE_USERNAME_PROPERTY, "username");
		System.setProperty(SYNAPSE_PAT_PROPERTY, "AUTH_TOKEN");
		WorkflowAdmin workflowAdmin = new WorkflowAdmin();
		String projectId = workflowAdmin.createProject();
		String fileEntityId = workflowAdmin.createExternalFileEntity(ZIP_FILE_URL, projectId, ROOT_TEMPLATE);

		DockerUtils dockerUtils = new DockerUtils();

		WorkflowOrchestrator wh = new WorkflowOrchestrator(workflowAdmin.getSynapseClient(), null, dockerUtils, null, 1000L);
		JSONObject o = new JSONObject();
		o.put(EVALUATION_ID,  fileEntityId);
		System.setProperty("EVALUATION_TEMPLATES", o.toString());
		Map<String,WorkflowURLEntrypointAndSynapseRef> map = wh.getWorkflowURLAndEntrypoint();
		assertTrue(map.containsKey(EVALUATION_ID));
		WorkflowURLEntrypointAndSynapseRef result = map.get(EVALUATION_ID);
		assertEquals(fileEntityId, result.getSynapseId());
		assertEquals(ZIP_FILE_URL, result.getWorkflowUrl().toString());
		assertEquals(ROOT_TEMPLATE, result.getEntryPoint());

		Project project = new Project();
		project.setId(projectId);
		mockSynapse.deleteEntity(project, true);
	}
}
