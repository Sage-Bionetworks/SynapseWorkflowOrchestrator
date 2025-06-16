package org.sagebionetworks;

import static org.sagebionetworks.Constants.DEFAULT_NUM_RETRY_ATTEMPTS;
import static org.sagebionetworks.Constants.NO_RETRY_EXCEPTIONS;
import static org.sagebionetworks.Constants.NO_RETRY_STATUSES;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.SynapseProfileProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SynapseClientFactory {

    private static Logger log = LoggerFactory.getLogger(SynapseClientFactory.class);

    private static SynapseClient createSynapseClientIntern() {
        SynapseClientImpl scIntern = new SynapseClientImpl();
        scIntern.setAuthEndpoint("https://repo-prod.prod.sagebase.org/auth/v1");
        scIntern.setRepositoryEndpoint("https://repo-prod.prod.sagebase.org/repo/v1");
        scIntern.setFileEndpoint("https://repo-prod.prod.sagebase.org/file/v1");

        return SynapseProfileProxy.createProfileProxy(scIntern);
    }

    public static <T,V> V foo(T bar) {
        return null;
    }

    public static <T extends V,V> V createRetryingProxy(final T underlying, final Class<V> implementedInterface) {
        final ExponentialBackoffRunner exponentialBackoffRunner = new ExponentialBackoffRunner(
                NO_RETRY_EXCEPTIONS, NO_RETRY_STATUSES, DEFAULT_NUM_RETRY_ATTEMPTS);

        InvocationHandler handler = new InvocationHandler() {
            public Object invoke(final Object proxy, final Method method, final Object[] outerArgs)
                    throws Throwable {
                return exponentialBackoffRunner.execute(new Executable<Object,Object[]>() {
                    public Object execute(Object[] args) throws Throwable {
                        try {
                            Object result = method.invoke(underlying, args);
                            return result;
                        } catch (IllegalAccessException  e) {
                            throw new RuntimeException(e);
                        } catch (InvocationTargetException e) {
                            if (e.getCause()==null) throw e; else throw e.getCause();
                        }
                    }
                    public Object[] refreshArgs(Object[] args) {
                        return args; // NO-OP
                    }
                }, outerArgs);
            }
        };

        return (V) Proxy.newProxyInstance(SynapseClientFactory.class.getClassLoader(),
                new Class[] {implementedInterface },
                handler);
    }

    public static SynapseClient createSynapseClient() {
        final SynapseClient synapseClientIntern = createSynapseClientIntern();
        return createRetryingProxy(synapseClientIntern, SynapseClient.class);
    }

}
