package org.sagebionetworks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShutdownHook extends Thread {
    private Thread mainThread;
    private static Logger log = LoggerFactory.getLogger(ShutdownHook.class);
    private volatile boolean shutDownSignalReceived;

    @Override
    public void run() {
        log.info("Shut down signal received.");
        this.shutDownSignalReceived=true;
        mainThread.interrupt();
        try {
            mainThread.join();
        } catch (InterruptedException e) {
        }
        log.info("Shut down complete.");
    }

    public ShutdownHook(Thread mainThread) {
        super();
        this.mainThread = mainThread;
        this.shutDownSignalReceived = false;
        Runtime.getRuntime().addShutdownHook(this);
    }

    public boolean shouldShutDown(){
        return shutDownSignalReceived;
    }

}
