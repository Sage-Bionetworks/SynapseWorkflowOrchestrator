package org.sagebionetworks;

import java.io.IOException;
import java.io.InputStream;

public interface URLInterface {
	public InputStream openStream() throws IOException;
	public String getPath() throws IOException;
}