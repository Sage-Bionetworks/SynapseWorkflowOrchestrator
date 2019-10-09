package org.sagebionetworks;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.google.common.io.Files;

public class WorkflowManagerDockerTest {

	@Test
	public void testDownloadWorkflowFromURL() throws Exception {
		URL workflowUrl = new URL("https://dockstore.org:8443/api/ga4gh/v2/tools/%23workflow%2Fgithub.com%2Fdenis-yuen%2Fhello-dockstore-workflow%2Fhello-world/versions/1.0/CWL");
		File folder = Files.createTempDir();
		Utils.downloadWorkflowFromURL(workflowUrl, "Dockstore.cwl", folder);
		List<String> workflowFiles = Arrays.asList(folder.list());
		assertTrue(workflowFiles.contains("Dockstore.cwl"));
		assertTrue(workflowFiles.contains("grep.cwl"));
		assertTrue(workflowFiles.contains("wc.cwl"));
	}

}
