package org.sagebionetworks;

import java.io.File;
import static org.junit.Assert.*;

import org.junit.Test;

public class ContainerRelativeFileTest {

    @Test
    public void test() {
        ContainerRelativeFile f = new ContainerRelativeFile("relative-path", new File("/container-root"), new File("/host-root"));
        assertEquals(new File("/container-root", "relative-path"), f.getContainerPath());
        assertEquals(new File("/host-root", "relative-path"), f.getHostPath());
        assertEquals("/container-root/relative-path", f.getContainerPath().getAbsolutePath());
    }

}
