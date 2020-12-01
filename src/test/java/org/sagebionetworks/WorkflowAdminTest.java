package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.Constants.ROOT_TEMPLATE_ANNOTATION_NAME;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValue;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;

@ExtendWith(MockitoExtension.class)
public class WorkflowAdminTest {

	@Mock
	SynapseClient mockSynapseClient;

	@Mock
	Archiver mockArchiver;

	@InjectMocks
	private WorkflowAdmin workflowAdmin;

	private static String WORKFLOW_URL = "https://github.com/Sage-Bionetworks/SynapseWorkflowExample/archive/master.zip";

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

		Annotations expectedAnnotations = new Annotations();
		Map<String, AnnotationsValue> expectedMap = new HashMap<>();
		AnnotationsValue annotationsValue = new AnnotationsValue();
		annotationsValue.setValue(Collections.singletonList(rootTemplate));
		annotationsValue.setType(AnnotationsValueType.STRING);
		expectedMap.put(ROOT_TEMPLATE_ANNOTATION_NAME, annotationsValue);
		expectedAnnotations.setAnnotations(expectedMap);

		// Call under test
		String entity = workflowAdmin.createExternalFileEntity(WORKFLOW_URL, parentId, rootTemplate);
		verify(mockSynapseClient).createExternalFileHandle(efh);
		verify(mockSynapseClient).createEntity(fileEntity);
		verify(mockSynapseClient).getAnnotationsV2(fileEntityResult.getId());
		verify(mockSynapseClient).updateAnnotationsV2(fileEntityResult.getId(), expectedAnnotations);
		assertEquals(id, entity);
		assertEquals(expectedAnnotations, annotations);
	}

}
