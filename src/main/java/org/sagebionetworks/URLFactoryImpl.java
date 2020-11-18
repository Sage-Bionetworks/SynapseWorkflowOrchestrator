package org.sagebionetworks;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class URLFactoryImpl implements URLFactory {

	/*
	 * return a URL for the given urlString
	 */
	public URLInterface createURL(final String urlString) {
		return new URLInterface() {

			@Override
			public InputStream openStream() throws IOException {
				return (new URL(urlString)).openStream();
			}

			@Override
			public String getPath() throws IOException {
				return (new URL(urlString)).getPath();
			}

			@Override
			public String toString() {
				try {
					return (new URL(urlString)).toString();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}

		};
	}
}
