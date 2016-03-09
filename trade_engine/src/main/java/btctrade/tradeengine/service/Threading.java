package btctrade.tradeengine.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.annotation.Nonnull;

/**
 * The Class Threading.
 */
public class Threading {

    /** The Constant CDA_EVENT_HANDLER_THREAD. */
    public static final ExecutorService CDA_EVENT_HANDLER_THREAD;

    static {
        CDA_EVENT_HANDLER_THREAD = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Nonnull
            @Override
            public Thread newThread(@Nonnull Runnable runnable) {
                Thread t = new Thread(runnable);
                t.setName("cda event handler thread");
                t.setDaemon(true);
                return t;
            }
        });
    }
}
