package btctrade.jms.service;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.apache.log4j.Logger;

public class QueueSizeCounter
{
    private static final Logger logger = Logger.getLogger(QueueSizeCounter.class);

    private MBeanServerConnection mBeanServerConnection;

    /**
     * @return the mBeanServerConnection
     */
    public MBeanServerConnection getmBeanServerConnection()
    {
        return mBeanServerConnection;
    }

    public Long getQueueSize(String queueName) {
        Long queueSize = null;
        try {
            ObjectName objectNameRequest = new ObjectName(
                "org.apache.activemq:type=Broker,brokerName=localhost,destinationType=Queue,destinationName="
                    + queueName);

            queueSize = (Long) mBeanServerConnection.getAttribute(objectNameRequest, "QueueSize");

            return queueSize;
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        return queueSize;
    }

    /**
     * @param mBeanServerConnection
     *            the mBeanServerConnection to set
     */
    public void setmBeanServerConnection(MBeanServerConnection mBeanServerConnection)
    {
        this.mBeanServerConnection = mBeanServerConnection;
    }
}
