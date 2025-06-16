package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class WorkflowURLDownloaderTest {

    @Mock
    private URLInterface mockURLInterface;

    @Mock
    private URLInterface mockURLInterfaceForDescriptor;

    @Mock
    private URLFactory mockURLFactory;

    @InjectMocks
    private WorkflowURLDownloader workflowURLDownloader;

    private String returnJsonData = "[{\"file_type\":\"PRIMARY_DESCRIPTOR\",\"path\":\"biowardrobe_chipseq_se.cwl\"}" +
            ",{\"file_type\":\"SECONDARY_DESCRIPTOR\",\"path\"" +
            ":\"biowardrobe_chipseq_se.yaml\"},{\"file_type\":\"SECONDARY_DESCRIPTOR\"" +
            ",\"path\":\"subworkflows/bam-bedgraph-bigwig.cwl\"}]";

    private String returnJsonDataUnexpectedFileType = "[{\"file_type\":\"PRIMARY_DESCRIPTOR\",\"path\":\"biowardrobe_chipseq_se.cwl\"}" +
            ",{\"file_type\":\"TEST_FILE\",\"path\"" +
            ":\"biowardrobe_chipseq_se.yaml\"}]";

    private String returnJsonDataDescriptor = "{\"file_type\":\"PRIMARY_DESCRIPTOR\",\"path\":\"biowardrobe_chipseq_se.cwl\",\"content\":\"blahblah\"}";
    private String returnJsonDataDescriptor2 = "{\"file_type\":\"SECONDARY_DESCRIPTOR\",\"path\":\"biowardrobe_chipseq_se.cwl\",\"content\":\"blahblah\"}";
    private String returnJsonDataDescriptor3 = "{\"file_type\":\"SECONDARY_DESCRIPTOR\",\"path\":\"subworkflows/bam-bedgraph-bigwig.cwl\",\"content\":\"blahblah\"}";

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
                IOUtils.toInputStream(returnJsonDataDescriptor2, StandardCharsets.UTF_8),
                IOUtils.toInputStream(returnJsonDataDescriptor3, StandardCharsets.UTF_8));

        // Call under test
        workflowURLDownloader.downloadWorkflowFromURL(workflowUrlString, entryPoint, folder);
        verify(mockURLFactory).createURL(workflowUrlString);
        verify(mockURLFactory).createURL(workflowUrlString + "/files");
        for (String expectedFile : expectedFiles) {
            verify(mockURLFactory).createURL(workflowUrlString + "/descriptor/" + expectedFile);
            assertTrue(new File(folder, expectedFile).exists());
        }
    }

    @Test
    public void testDownloadWorkflowFromUrlWithUnexpectedFileType() throws Exception {
        String workflowUrlString = "https://dockstore.org/api/api/ga4gh/v2/tools/%23workflow%2Fgithub.com%2FBarski-lab%2Fga4gh_challenge/versions/v0.0.4/CWL";
        File folder = Files.createTempDir();
        String entryPoint = "biowardrobe_chipseq_se.cwl";
        List<String> expectedFiles = Arrays.asList("biowardrobe_chipseq_se.cwl");

        when(mockURLFactory.createURL(workflowUrlString)).thenReturn(mockURLInterface);
        when(mockURLFactory.createURL(workflowUrlString + "/files")).thenReturn(mockURLInterface);
        when(mockURLInterface.openStream()).thenReturn(new ByteArrayInputStream(returnJsonDataUnexpectedFileType.getBytes()));
        when(mockURLInterface.getPath()).thenReturn(workflowUrlString);
        when(mockURLInterface.toString()).thenReturn(workflowUrlString);
        when(mockURLFactory.createURL(startsWith(workflowUrlString + "/descriptor/"))).thenReturn(mockURLInterfaceForDescriptor);
        when(mockURLInterfaceForDescriptor.openStream()).thenReturn(IOUtils.toInputStream(returnJsonDataDescriptor, StandardCharsets.UTF_8));

        String expectedMessage = "Unexpected file_type TEST_FILE";
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            // Call under test
            workflowURLDownloader.downloadWorkflowFromURL(workflowUrlString, entryPoint, folder);
        });
        assertEquals(expectedMessage, exception.getMessage());

        verify(mockURLFactory).createURL(workflowUrlString);
        verify(mockURLFactory).createURL(workflowUrlString + "/files");
        for (String expectedFile : expectedFiles) {
            verify(mockURLFactory).createURL(workflowUrlString + "/descriptor/" + expectedFile);
            assertTrue(new File(folder, expectedFile).exists());
        }
    }
}
