package btctrade.tradeengine.service;

import java.math.BigInteger;
import java.util.Date;

/**
 * The listener interface for receiving ICDAEvent events. The class that is
 * interested in processing a ICDAEvent event implements this interface, and the
 * object created with that class is registered with a component using the
 * component's <code>addICDAEventListener<code> method. When
 * the ICDAEvent event occurs, that object's appropriate
 * method is invoked.
 * 
 * @see ICDAEventEvent
 */
public interface ICDAEventListener {

    /**
     * Invoked when on concluded price update occurs.
     * 
     * @param price
     *            the price
     * @param volume
     *            the volume
     * @param timestamp
     *            the timestamp
     */
    public void onConcludedPriceUpdated(BigInteger price, BigInteger volume, Date timestamp);
}
