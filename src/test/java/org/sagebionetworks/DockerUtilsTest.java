package org.sagebionetworks;

import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;

import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Device;

public class DockerUtilsTest {

	@Ignore
	@Test
	public void testDockerLogs() throws Exception {
		DockerUtils dockerUtils = new DockerUtils();
		String containerId = dockerUtils.createContainer("hello-world", "containerName", 
				(List<Bind>)Collections.EMPTY_LIST, (List<Device>)Collections.EMPTY_LIST, null, null, null, false);
		dockerUtils.startContainer(containerId);
		Path logFile = Files.createTempFile(containerId, ".txt");
		dockerUtils.getLogs(containerId, logFile, null);
		assertTrue(logFile.toFile().exists());
		IOUtils.copy(new FileInputStream(logFile.toFile()), System.out);
		dockerUtils.removeContainer(containerId, true);
	}

	@Ignore
	@Test
	public void testListContainers() throws Exception {
		DockerUtils dockerUtils = new DockerUtils();
		Map<String, Container> result = dockerUtils.listContainers(new Filter(){
			public boolean match(String s) {
				return true;
			}});
		for (String name : result.keySet()) {
			System.out.println(name);
		}
	}
	
	@Ignore
	@Test
	public void testPull() throws Exception {
		System.setProperty("DOCKER_ENGINE_URL", "unix:///var/run/docker.sock");
		DockerUtils dockerUtils = new DockerUtils();
		dockerUtils.pullImageWithRetry("sagebionetworks/synapse-workflow-orchestrator-toil:1.0");
	}

}
