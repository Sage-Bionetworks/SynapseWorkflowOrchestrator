package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import com.google.common.io.Files;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class WorkflowManagerDockerTest {

	@Mock
	private URLInterface mockURLInterface;

	@Mock
	private URLInterface mockURLInterfaceForDescriptor;

	@Mock
	private URLFactory mockURLFactory;

	private String returnJsonData = "[{\"file_type\":\"PRIMARY_DESCRIPTOR\",\"path\":\"biowardrobe_chipseq_se.cwl\"}" +
			",{\"file_type\":\"TEST_FILE\",\"path\"" +
			":\"biowardrobe_chipseq_se.yaml\"},{\"file_type\":\"SECONDARY_DESCRIPTOR\"" +
			",\"path\":\"subworkflows/bam-bedgraph-bigwig.cwl\"}]";

	private String returnJsonDataDescriptor = "{\"file_type\":\"PRIMARY_DESCRIPTOR\",\"path\":\"biowardrobe_chipseq_se.cwl\",\"content\":\"blahblah\"}";

	@Test
	public void testDownloadWorkflowFromUrlWithGa4ghApi() throws Exception {
		String workflowUrlString = "https://dockstore.org/api/api/ga4gh/v2/tools/%23workflow%2Fgithub.com%2FBarski-lab%2Fga4gh_challenge/versions/v0.0.4/CWL";
		File folder = Files.createTempDir();
		String entryPoint = "biowardrobe_chipseq_se.cwl";
		List<String> expectedFiles = Arrays.asList("biowardrobe_chipseq_se.cwl", "subworkflows/bam-bedgraph-bigwig.cwl");

		when(mockURLFactory.createURL(workflowUrlString)).thenReturn(mockURLInterface);
		when(mockURLFactory.createURL(workflowUrlString + "/files")).thenReturn(mockURLInterface);
		when(mockURLInterface.openStream()).thenReturn(new ByteArrayInputStream(returnJsonData.getBytes()));
		when(mockURLInterface.getPath()).thenReturn(workflowUrlString);
		when(mockURLInterface.toString()).thenReturn(workflowUrlString);
		when(mockURLFactory.createURL(startsWith(workflowUrlString + "/descriptor/"))).thenReturn(mockURLInterfaceForDescriptor);
		when(mockURLInterfaceForDescriptor.openStream()).thenReturn(IOUtils.toInputStream(returnJsonDataDescriptor, StandardCharsets.UTF_8),
				IOUtils.toInputStream(returnJsonDataDescriptor, StandardCharsets.UTF_8));

		Utils utils = new Utils(mockURLFactory);
		// Call under test
		utils.downloadWorkflowFromURL(workflowUrlString, entryPoint, folder);
		verify(mockURLFactory).createURL(workflowUrlString);
		verify(mockURLFactory).createURL(workflowUrlString + "/files");
		for (String expectedFile : expectedFiles) {
			verify(mockURLFactory).createURL(workflowUrlString + "/descriptor/" + expectedFile);
			assertTrue(new File(folder, expectedFile).exists());
		}
	}

}
