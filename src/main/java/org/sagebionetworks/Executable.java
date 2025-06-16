package org.sagebionetworks;



public interface Executable<T,V> {
    T execute(V args) throws Throwable;
    V refreshArgs(V args) throws Throwable;
}
