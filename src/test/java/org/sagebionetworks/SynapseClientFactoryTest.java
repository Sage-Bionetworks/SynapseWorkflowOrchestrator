package org.sagebionetworks;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SynapseClientFactoryTest {

    @Test
    public void testCreateRetryingProxy() {
        assertEquals(2, SynapseClientFactory.createRetryingProxy(new IncrementerImpl(), Incrementer.class).increment(1));
    }

}
