package org.sagebionetworks;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.google.common.io.Files;

public class WorkflowManagerDockerTest {

	@Test
	public void testDownloadWorkflowFromURL() throws Exception {
		String workflowUrlString = "https://dockstore.org/api/api/ga4gh/v2/tools/%23workflow%2Fgithub.com%2FBarski-lab%2Fga4gh_challenge/versions/v0.0.4/CWL";
		File folder = Files.createTempDir();
		String entryPoint = "biowardrobe_chipseq_se.cwl";
		URLFactory urlFactory = new URLFactoryImpl(); // TODO use a factory which returns objects that MOCK java.net.URL 
		Utils utils = new Utils(urlFactory);
		utils.downloadWorkflowFromURL(workflowUrlString, entryPoint, folder);
		List<String> workflowFiles = Arrays.asList(folder.list());
		assertTrue(workflowFiles.contains(entryPoint));
		assertTrue(workflowFiles.contains("subworkflows"));
		assertTrue(workflowFiles.contains("tools"));
	}

}

