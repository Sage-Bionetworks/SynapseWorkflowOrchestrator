package org.sagebionetworks;

import static org.mockito.Mockito.when;
import static org.sagebionetworks.Constants.ROOT_TEMPLATE_ANNOTATION_NAME;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValue;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;

@ExtendWith(MockitoExtension.class)
public class WorkflowAdminTest {

	@Mock
	SynapseClient mockSynapseClient;

	@Mock
	Archiver mockArchiver;

	private WorkflowAdmin workflowAdmin;

	private static final URL WORKFLOW_URL;

	static {

		try {
			WORKFLOW_URL = new URL("https://github.com/Sage-Bionetworks/SynapseWorkflowExample/archive/master.zip");
		}catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@BeforeEach
	public void setUp() throws Exception {
		workflowAdmin = new WorkflowAdmin(mockSynapseClient, mockArchiver);
	}


	@Test
	public void getExternalFileEntity() throws Exception, Throwable {
		String id = "100";
		String parentId = "200";
		String dataFileHandleId = "300";
		String rootTemplate = "rootTemplate";

		ExternalFileHandle efh = new ExternalFileHandle();
		efh.setExternalURL(WORKFLOW_URL.toString());
		ExternalFileHandle efhResult = new ExternalFileHandle();
		efhResult.setExternalURL(WORKFLOW_URL.toString());
		efhResult.setId(dataFileHandleId);
		when(mockSynapseClient.createExternalFileHandle(efh)).thenReturn(efhResult);

		FileEntity fileEntity = new FileEntity();
		fileEntity.setDataFileHandleId(efhResult.getId());
		fileEntity.setParentId(parentId);
		FileEntity fileEntityResult = new FileEntity();
		fileEntityResult.setDataFileHandleId(efhResult.getId());
		fileEntityResult.setParentId(parentId);
		fileEntityResult.setId(id);
		when(mockSynapseClient.createEntity(fileEntity)).thenReturn(fileEntityResult);

		Annotations annotations = new Annotations();
		annotations.setAnnotations(new HashMap<String, AnnotationsValue>());
		when(mockSynapseClient.getAnnotationsV2(fileEntityResult.getId())).thenReturn(annotations);

		// Call under test
		String entity = workflowAdmin.createExternalFileEntity(WORKFLOW_URL.toString(), parentId, rootTemplate);
		Assertions.assertEquals(id, entity);
		Assertions.assertNotNull(annotations);
		Assertions.assertEquals(1, annotations.getAnnotations().size());
		Assertions.assertTrue(annotations.getAnnotations().containsKey(ROOT_TEMPLATE_ANNOTATION_NAME));
		Assertions.assertEquals(1,annotations.getAnnotations().get(ROOT_TEMPLATE_ANNOTATION_NAME).getValue().size());
		Assertions.assertEquals(rootTemplate, annotations.getAnnotations().get(ROOT_TEMPLATE_ANNOTATION_NAME).getValue().get(0));
	}

}
