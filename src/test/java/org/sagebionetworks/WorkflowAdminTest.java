package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.Constants.ROOT_TEMPLATE_ANNOTATION_NAME;

import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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

	@InjectMocks
	private WorkflowAdmin workflowAdmin;

	private String WORKFLOW_URL = "https://github.com/Sage-Bionetworks/SynapseWorkflowExample/archive/master.zip";

	@Test
	public void testCreateExternalFileEntity() throws Exception, Throwable {
		String id = "100";
		String parentId = "200";
		String dataFileHandleId = "300";
		String rootTemplate = "rootTemplate";

		ExternalFileHandle efh = new ExternalFileHandle();
		efh.setExternalURL(WORKFLOW_URL);
		ExternalFileHandle efhResult = new ExternalFileHandle();
		efhResult.setExternalURL(WORKFLOW_URL);
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
		String entity = workflowAdmin.createExternalFileEntity(WORKFLOW_URL, parentId, rootTemplate);
		verify(mockSynapseClient, times(1)).createExternalFileHandle(efh);
		verify(mockSynapseClient, times(1)).createEntity(fileEntity);
		verify(mockSynapseClient, times(1)).getAnnotationsV2(fileEntityResult.getId());
		verify(mockSynapseClient, times(1)).updateAnnotationsV2(fileEntityResult.getId(), annotations);
		assertEquals(id, entity);
		assertNotNull(annotations);
		assertEquals(1, annotations.getAnnotations().size());
		assertTrue(annotations.getAnnotations().containsKey(ROOT_TEMPLATE_ANNOTATION_NAME));
		assertEquals(1,annotations.getAnnotations().get(ROOT_TEMPLATE_ANNOTATION_NAME).getValue().size());
		assertEquals(rootTemplate, annotations.getAnnotations().get(ROOT_TEMPLATE_ANNOTATION_NAME).getValue().get(0));
	}

}
