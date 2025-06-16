package org.sagebionetworks;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseServerException;
import org.sagebionetworks.client.exceptions.SynapseServiceUnavailable;
import org.sagebionetworks.client.exceptions.SynapseTooManyRequestsException;
import org.sagebionetworks.client.exceptions.UnknownSynapseServerException;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import static org.sagebionetworks.Constants.DEFAULT_NUM_RETRY_ATTEMPTS;


public class ExponentialBackoffRunner {
    private static Logger log = Logger.getLogger(ExponentialBackoffRunner.class.getName());

    private static int NUM_503_RETRY_ATTEMPTS = 16; // 272 min (4h:32m)
    private static long INITIAL_BACKOFF_MILLIS = 500L;
    private static long BACKOFF_MULTIPLIER = 2L;

    private int numRetryAttempts;
    private List<Class<? extends SynapseServerException>> noRetryTypes  = null;
    private List<Integer> noRetryStatuses;

    public ExponentialBackoffRunner(List<Class<? extends SynapseServerException>> noRetryTypes, Integer[] noRetryStatuses, int numRetryAttempts) {
        this.noRetryTypes=noRetryTypes;
        this.noRetryStatuses=Arrays.asList(noRetryStatuses);
        this.numRetryAttempts=numRetryAttempts;
    }

    public ExponentialBackoffRunner() {
        this.noRetryTypes=Collections.EMPTY_LIST;
        this.noRetryStatuses = Collections.EMPTY_LIST;
        this.numRetryAttempts=DEFAULT_NUM_RETRY_ATTEMPTS;
    }

    private static String exceptionMessage(Throwable e) {
        if (e==null) return null;
        return e.getMessage();
    }

    /**
     *
     * Note, the total sleep time before giving up is:
     * INITIAL_BACKOFF_MILLIS * (BACKOFF_MULTIPLIER ^ (NUM_ATTEMPTS-1) - 1)  /  (BACKOFF_MULTIPLIER-1)
     * For INITIAL_BACKOFF_MILLIS=500msec, BACKOFF_MULTIPLIER=2, NUM_ATTEMPTS=7 this is 31.5 sec
     *
     * @param executable
     * @return
     * @throws IOException
     * @throws ServiceException
     */
    public <T,V> T execute(Executable<T,V> executable, V args) throws Throwable {
        long backoff = INITIAL_BACKOFF_MILLIS;
        Throwable lastException=null;
        int i = 0;
        while (true) {
            try {
                return executable.execute(args);
            } catch (UnknownSynapseServerException e) {
                Integer statusCode = e.getStatusCode();
                if (noRetryStatuses.contains(statusCode)) {
                        log.severe("Found status code "+statusCode+". Will not retry: "+exceptionMessage(e));
                        throw e;
                }
                lastException=e;
            } catch (SynapseException e) {
                if (noRetryTypes.contains(e.getClass())) {
                        log.severe("Will not retry: "+exceptionMessage(e));
                        throw e;
                }
                lastException=e;
            }
            log.warning("Encountered exception on attempt "+i+": "+exceptionMessage(lastException));
            if (i==0 && lastException!=null && (lastException instanceof SynapseTooManyRequestsException)) {
                // we're getting a 429 so start with a greater backoff
                backoff *= (BACKOFF_MULTIPLIER*BACKOFF_MULTIPLIER);
            }
            i++;
            if (lastException!=null && (lastException instanceof SynapseServiceUnavailable)) {
                if (i>=NUM_503_RETRY_ATTEMPTS) break;
            } else {
                if (i>=numRetryAttempts) break;
            }
            try {
                Thread.sleep(backoff);
            } catch(InterruptedException e) {
                throw lastException;
            }
            backoff *= BACKOFF_MULTIPLIER;
            args = executable.refreshArgs(args); // implementation could be a no-op, simply returning the passed in value
        }
        log.severe("Exhausted retries. Throwing exception: "+exceptionMessage(lastException));
        throw lastException;
    }

}
