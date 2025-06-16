package org.sagebionetworks;

public interface URLFactory {

    /*
     * return a URLInterface implementation for the given urlString
     */
    public URLInterface createURL(String urlString);

}
