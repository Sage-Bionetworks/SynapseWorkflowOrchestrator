package org.sagebionetworks;

/*
 * Implements an Executable that has a 'no-op' behavior when refreshArgs is called
 */
public abstract class NoRefreshExecutableAdapter<T,V>  implements Executable<T, V> {

    @Override
    public V refreshArgs(V args) throws Throwable {
        return args;
    }

}
