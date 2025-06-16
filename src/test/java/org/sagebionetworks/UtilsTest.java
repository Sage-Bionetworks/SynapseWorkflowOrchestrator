package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.sagebionetworks.Constants.SUBMISSION_FAILED;
import static org.sagebionetworks.Constants.SUBMITTER_NOTIFICATION_MASK_PARAM_NAME;
import static org.sagebionetworks.Constants.SUBMISSION_STARTED;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.docker.DockerRepository;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;


public class UtilsTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
        System.clearProperty(SUBMITTER_NOTIFICATION_MASK_PARAM_NAME);
    }

    @Test
    public void testGetDockerRepositoryNameFromEntityBundle() throws Exception {
        String repoName = "docker.synapse.org/syn1234/foo";
        EntityBundle bundle = new EntityBundle();
        DockerRepository repoEntity = new DockerRepository();
        repoEntity.setRepositoryName(repoName);
        bundle.setEntity(repoEntity);
        JSONObjectAdapter joa = new JSONObjectAdapterImpl();
        bundle.writeToJSONObject(joa);
        assertEquals(repoName, Utils.getDockerRepositoryNameFromEntityBundle(joa.toJSONString()));
    }

    @Test
    public void testGetProgressFromString() {
        assertEquals(92.85D, Utils.getProgressPercentFromString("STDOUT: 92.85714285714286", 5), 1e-5);
    }

    @Test
    public void testNotificationEnabled() {
        System.setProperty(SUBMITTER_NOTIFICATION_MASK_PARAM_NAME, "28");
        assertFalse(Utils.notificationEnabled(SUBMISSION_STARTED));
        assertFalse(Utils.notificationEnabled(SUBMISSION_STARTED));
        assertTrue(Utils.notificationEnabled(SUBMISSION_FAILED));
    }

}
