package org.sagebionetworks;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.Constants.AGENT_SHARED_DIR_DEFAULT;
import static org.sagebionetworks.Constants.AGENT_SHARED_DIR_PROPERTY_NAME;
import static org.sagebionetworks.Constants.COMPOSE_PROJECT_NAME_ENV_VAR;
import static org.sagebionetworks.Constants.DOCKER_ENGINE_URL_PROPERTY_NAME;
import static org.sagebionetworks.Constants.SHARED_VOLUME_NAME;

import java.io.File;
import java.util.HashMap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class WorkflowManagerDockerTest {

	@Mock
	private DockerUtils mockDockerUtils;

	@Mock
	private WorkflowParameters mockWorkflowParameters;

	@InjectMocks
	private WorkflowManagerDocker workflowManagerDocker;


	private String workflowUrlString = "https://dockstore.org/api/api/ga4gh/v2/tools/%23workflow%2Fgithub.com%2FBarski-lab%2Fga4gh_challenge/versions/v0.0.4/CWL";
	private String entryPoint = "biowardrobe_chipseq_se.cwl";


	@BeforeEach
	public void setUp() throws Exception {
		System.setProperty(DOCKER_ENGINE_URL_PROPERTY_NAME, "unix:///var/run/docker.sock");
		System.setProperty(COMPOSE_PROJECT_NAME_ENV_VAR, "project");
		System.setProperty(AGENT_SHARED_DIR_PROPERTY_NAME, System.getProperty("java.io.tmpdir"));
	}

	@AfterEach
	public void tearDown() throws Exception {
		System.clearProperty("WORKFLOW_OUTPUT_ROOT_ENTITY_ID");
		System.clearProperty(DOCKER_ENGINE_URL_PROPERTY_NAME);
		System.clearProperty("EVALUATION_TEMPLATES");
		System.setProperty(AGENT_SHARED_DIR_PROPERTY_NAME, AGENT_SHARED_DIR_DEFAULT);
	}

	@Test
	public void testWorkflowManagerDocker() throws Exception {

		byte[] synapseConfigFileContent = new byte[100];
		String mountPoint = "mountpoint";
		String imageReference = "sagebionetworks/synapse-workflow-orchestrator-toil";
        Boolean privileged = false;

        when(mockDockerUtils.getVolumeMountPoint(Utils.dockerComposeName(SHARED_VOLUME_NAME))).thenReturn(mountPoint);

		// Call under test
		workflowManagerDocker.createWorkflowJob(workflowUrlString, entryPoint,
				mockWorkflowParameters, synapseConfigFileContent);

		verify(mockDockerUtils).createModelContainer(eq(imageReference), anyString(), eq(new HashMap<File,String>()), anyMap(),
				anyList(), anyList(), anyString(), eq(privileged));
		verify(mockDockerUtils).startContainer(any());
		verify(mockDockerUtils).getVolumeMountPoint(Utils.dockerComposeName(SHARED_VOLUME_NAME));

	}

}
