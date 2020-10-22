package org.sagebionetworks;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
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

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.Constants.AGENT_SHARED_DIR_DEFAULT;
import static org.sagebionetworks.Constants.AGENT_SHARED_DIR_PROPERTY_NAME;
import static org.sagebionetworks.Constants.COMPOSE_PROJECT_NAME_ENV_VAR;
import static org.sagebionetworks.Constants.DOCKER_ENGINE_URL_PROPERTY_NAME;
import static org.sagebionetworks.Constants.ROOT_TEMPLATE_ANNOTATION_NAME;
import static org.sagebionetworks.Constants.SHARED_VOLUME_NAME;
import static org.sagebionetworks.Constants.SYNAPSE_PASSWORD_PROPERTY;
import static org.sagebionetworks.Constants.SYNAPSE_USERNAME_PROPERTY;
import static org.sagebionetworks.Utils.dockerComposeName;

@ExtendWith(MockitoExtension.class)
public class WorkflowOrchestratorTest {
	
	@Mock
	SynapseClient synapse;
	
	@Mock
	EvaluationUtils evaluationUtils;
	
	@Mock
	DockerUtils dockerUtils;
	
	@Mock
	SubmissionUtils submissionUtils;
	
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
	
	@Before
	public void setUp() throws Exception {
		System.setProperty("WORKFLOW_OUTPUT_ROOT_ENTITY_ID", WORKFLOW_OUTPUT_ROOT_ENTITY_ID);
		System.setProperty("SYNAPSE_USERNAME", "foo");
		System.setProperty("SYNAPSE_PASSWORD", "bar");
		System.setProperty(DOCKER_ENGINE_URL_PROPERTY_NAME, "unix:///var/run/docker.sock");
		System.setProperty(COMPOSE_PROJECT_NAME_ENV_VAR, "project");
		
		when(dockerUtils.getVolumeMountPoint(dockerComposeName(SHARED_VOLUME_NAME))).thenReturn(System.getProperty("java.io.tmpdir"));
		System.setProperty(AGENT_SHARED_DIR_PROPERTY_NAME, System.getProperty("java.io.tmpdir"));
		
		long sleepTimeMillis = 1*60*1000L;
		workflowOrchestrator = new WorkflowOrchestrator(
				synapse, evaluationUtils,
				dockerUtils, submissionUtils, sleepTimeMillis);

	}
	
	@After
	public void tearDown() throws Exception {
		System.clearProperty("WORKFLOW_OUTPUT_ROOT_ENTITY_ID");
		System.clearProperty("SYNAPSE_USERNAME");
		System.clearProperty("SYNAPSE_PASSWORD");
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
		when(evaluationUtils.selectSubmissions(EVALUATION_ID, SubmissionStatusEnum.RECEIVED)).thenReturn(Collections.singletonList(bundle));
		UserProfile profile = new UserProfile();
		profile.setOwnerId("1111");
		when(synapse.getMyProfile()).thenReturn(profile);
		Folder folder = new Folder();
		folder.setId(FOLDER_ID);
		when(synapse.createEntity(any(Folder.class))).thenReturn(folder);
		// method under test
		workflowOrchestrator.createNewWorkflowJobs(EVALUATION_ID, WORKFLOW_REF);
		
		
		verify(evaluationUtils).selectSubmissions(EVALUATION_ID, SubmissionStatusEnum.RECEIVED);
	}
	
	@Test 
	public void testUpdateWorkflowJobs() throws Throwable {
		workflowOrchestrator.updateWorkflowJobs(Collections.singletonList(EVALUATION_ID));
	}
	
	private static String ZIP_FILE_URL = "https://github.com/Sage-Bionetworks/SynapseWorkflowExample/archive/master.zip";
	private static String ROOT_TEMPLATE = "SynapseWorkflowExample-master/workflow-entrypoint.cwl";


	@Test
	public void testGetWorkflowURLAndEntrypointException() throws Throwable {
		String test = "test";
		Annotations v2 = new Annotations();
		v2.setAnnotations(null);
		when(synapse.getAnnotationsV2(test)).thenReturn(v2);

		WorkflowAdmin workflowAdmin = new WorkflowAdmin();
		String projectId = workflowAdmin.createProject();
		String fileEntityId = workflowAdmin.createExternalFileEntity(ZIP_FILE_URL, projectId, ROOT_TEMPLATE);

		DockerUtils dockerUtils = new DockerUtils();

		WorkflowOrchestrator wh = new WorkflowOrchestrator(workflowAdmin.getSynapseClient(), null, dockerUtils, null, 1000L);
		JSONObject o = new JSONObject();
		o.put(EVALUATION_ID,  fileEntityId);
		System.setProperty("EVALUATION_TEMPLATES", o.toString());

		String expectedMessage = "Expected string annotation called "+
				ROOT_TEMPLATE_ANNOTATION_NAME+" for " + fileEntityId + " but the entity has no string annotations.";
		IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			wh.getWorkflowURLAndEntrypoint();
		});
	}

	@Test
	public void testGetWorkflowURLAndEntrypointExceptionNullAnnotationsValue() throws Throwable {
		String test = "test";
		Annotations v2 = new Annotations();
		v2.setAnnotations(new HashMap<String, AnnotationsValue>());
		when(synapse.getAnnotationsV2(test)).thenReturn(v2);

		WorkflowAdmin workflowAdmin = new WorkflowAdmin();
		String projectId = workflowAdmin.createProject();
		String fileEntityId = workflowAdmin.createExternalFileEntity(ZIP_FILE_URL, projectId, ROOT_TEMPLATE);

		DockerUtils dockerUtils = new DockerUtils();

		WorkflowOrchestrator wh = new WorkflowOrchestrator(workflowAdmin.getSynapseClient(), null, dockerUtils, null, 1000L);
		JSONObject o = new JSONObject();
		o.put(EVALUATION_ID,  fileEntityId);
		System.setProperty("EVALUATION_TEMPLATES", o.toString());

		String expectedMessage = fileEntityId + " has no AnnotationValue called "+ROOT_TEMPLATE_ANNOTATION_NAME ;
		IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			wh.getWorkflowURLAndEntrypoint();
		});
	}

	@Test
	public void testGetWorkflowURLAndEntrypointExceptionNullValue() throws Throwable {
		String test = "test";
		Annotations v2 = new Annotations();
		Map<String, AnnotationsValue> map = new HashMap<String, AnnotationsValue>();
		map.put(ROOT_TEMPLATE_ANNOTATION_NAME, null);
		v2.setAnnotations(map);
		when(synapse.getAnnotationsV2(test)).thenReturn(v2);

		WorkflowAdmin workflowAdmin = new WorkflowAdmin();
		String projectId = workflowAdmin.createProject();
		String fileEntityId = workflowAdmin.createExternalFileEntity(ZIP_FILE_URL, projectId, ROOT_TEMPLATE);

		DockerUtils dockerUtils = new DockerUtils();

		WorkflowOrchestrator wh = new WorkflowOrchestrator(workflowAdmin.getSynapseClient(), null, dockerUtils, null, 1000L);
		JSONObject o = new JSONObject();
		o.put(EVALUATION_ID,  fileEntityId);
		System.setProperty("EVALUATION_TEMPLATES", o.toString());

		String expectedMessage = fileEntityId + " has no AnnotationValue called "+ROOT_TEMPLATE_ANNOTATION_NAME ;
		IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			wh.getWorkflowURLAndEntrypoint();
		});
	}

	@Ignore
	@Test
	public void testGetWorkflowURLAndEntrypoint() throws Throwable {
		System.setProperty(SYNAPSE_USERNAME_PROPERTY, "xxx");
		System.setProperty(SYNAPSE_PASSWORD_PROPERTY, "xxx");
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
		synapse.deleteEntity(project, true);
	}

}
