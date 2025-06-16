package org.sagebionetworks;

/*
 * A simple filter interface
 */
public interface Filter {

    /*
     * returns true iff the String matches the filter
     */
    boolean match(String s);

}
