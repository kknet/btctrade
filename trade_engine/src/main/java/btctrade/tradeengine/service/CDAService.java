package btctrade.tradeengine.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;

import javax.annotation.PostConstruct;
import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.apache.log4j.Logger;
import org.bitcoinj.utils.ListenerRegistration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

import btctrade.jms.data.MatchedOrderProtos.MatchedOrder;
import btctrade.jms.data.MatchedOrderProtos.MatchedOrder.MatchedOrderType;
import btctrade.jms.data.OrderProtos.Order;
import btctrade.jms.data.utility.BtcProtoCommonUtility;
import btctrade.tradeengine.service.exceptions.CDAServiceException;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.protobuf.ByteString;

/**
 * The Class CDAService.
 */
public class CDAService extends AbstractExecutionThreadService {

    /**
     * The Class PriceVolumePair.
     */
    class PriceVolumePair {

        /** The price. */
        BigInteger price;

        /** The volume. */
        BigInteger volume;

        /**
         * Gets the price.
         * 
         * @return the price
         */
        public BigInteger getPrice() {
            return price;
        }

        /**
         * Gets the volume.
         * 
         * @return the volume
         */
        public BigInteger getVolume() {
            return volume;
        }

        /**
         * Sets the price.
         * 
         * @param price
         *            the new price
         */
        public void setPrice(BigInteger price) {
            this.price = price;
        }

        /**
         * Sets the volume.
         * 
         * @param volume
         *            the new volume
         */
        public void setVolume(BigInteger volume) {
            this.volume = volume;
        }
    }

    /**
     * The Enum Profile.
     */
    public enum Profile {

        /** The dev. */
        DEV,
        /** The prod. */
        PROD
    }

    /** The Constant logger. */
    private static final Logger logger = Logger.getLogger(CDAService.class);

    /** The buy order blocking queue. */
    @Autowired
    @Qualifier("buyOrderBlockingQueue")
    private LinkedBlockingQueue<Order> buyOrderBlockingQueue;

    /** The buy order pool. */
    @Autowired
    @Qualifier("buyOrderPool")
    private PriorityQueue<Order> buyOrderPool;

    /** The cancel order blocking queue. */
    @Autowired
    @Qualifier("cancelOrderBlockingQueue")
    private LinkedBlockingQueue<Order> cancelOrderBlockingQueue;

    /** The event listeners. */
    private transient CopyOnWriteArrayList<ListenerRegistration<ICDAEventListener>> eventListeners;

    /** The matched order MQ template. */
    @Autowired
    @Qualifier("jmsMatchedOrderMQTemplate")
    private JmsTemplate matchedOrderMQTemplate;

    /** The order pools. */
    @Autowired
    @Qualifier("orderPools")
    private LinkedList<PriorityQueue<Order>> orderPools;

    /** The persistence data dir. */
    @Value("${tradeengine.persistence_data.dir}")
    private String persistenceDataDir;

    /** The persistence data dir path. */
    protected Path persistenceDataDirPath;

    /** The profile. */
    @Value("${tradeengine.profile}")
    private Profile profile;

    /** The readable logging enabled. */
    boolean readableLoggingEnabled = true;

    /** The sell order pool. */
    @Autowired
    @Qualifier("sellOrderPool")
    private PriorityQueue<Order> sellOrderPool;

    /** The sell order queue. */
    @Autowired
    @Qualifier("sellOrderBlockingQueue")
    private LinkedBlockingQueue<Order> sellOrderQueue;

    /**
     * Instantiates a new CDA service.
     */
    public CDAService() {
        eventListeners = new CopyOnWriteArrayList<ListenerRegistration<ICDAEventListener>>();
    }

    /**
     * Adds the event listener.
     * 
     * @param listener
     *            the listener
     */
    public void addEventListener(ICDAEventListener listener) {
        addEventListener(listener, Threading.CDA_EVENT_HANDLER_THREAD);
        logger.debug("event listener added");
    }

    /**
     * Adds the event listener.
     * 
     * @param listener
     *            the listener
     * @param executor
     *            the executor
     */
    public void addEventListener(ICDAEventListener listener, Executor executor) {
        eventListeners.add(new ListenerRegistration<ICDAEventListener>(listener, executor));
        logger.debug(String.format("number of event listeners : %d", eventListeners.size()));
    }

    /**
     * Deserialize.
     * 
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     * @throws ClassNotFoundException
     *             the class not found exception
     */
    private void deserialize() throws IOException, ClassNotFoundException {
        File buyOrderBlockingQueueFile = persistenceDataDirPath.resolve("buyOrderQueue.ser").toFile();
        if (buyOrderBlockingQueueFile.exists()) {
            logger.info("deserializing buy order blocking queue");
            ObjectInputStream buyOrderBlockingQueueStream = new ObjectInputStream(new FileInputStream(
                buyOrderBlockingQueueFile));
            for (Object o : (Object[]) buyOrderBlockingQueueStream.readObject())
                buyOrderBlockingQueue.add((Order) o);
            buyOrderBlockingQueueStream.close();
            logger.info(String.format("%d buy orders loaded into blocking queue", buyOrderBlockingQueue.size()));
        }
        File sellOrderBlockingQueueFile = persistenceDataDirPath.resolve("sellOrderQueue.ser").toFile();
        if (sellOrderBlockingQueueFile.exists()) {
            logger.info("deserializing sell order blocking queue");
            ObjectInputStream sellOrderBlockingQueueStream = new ObjectInputStream(new FileInputStream(
                sellOrderBlockingQueueFile));
            for (Object o : (Object[]) sellOrderBlockingQueueStream.readObject())
                sellOrderQueue.add((Order) o);
            sellOrderBlockingQueueStream.close();
            logger.info(String.format("%d sell orders loaded into blocking queue", sellOrderQueue.size()));
        }
        File buyOrderPoolFile = persistenceDataDirPath.resolve("buyOrderPool.ser").toFile();
        if (buyOrderPoolFile.exists()) {
            logger.info("deserializing buy order pool");
            ObjectInputStream buyOrderPoolStream = new ObjectInputStream(new FileInputStream(buyOrderPoolFile));
            for (Object o : (Object[]) buyOrderPoolStream.readObject())
                buyOrderPool.add((Order) o);
            buyOrderPoolStream.close();
            logger.info(String.format("%d buy orders loaded into engine pool", buyOrderPool.size()));
        }
        File sellOrderPoolFile = persistenceDataDirPath.resolve("sellOrderPool.ser").toFile();
        if (sellOrderPoolFile.exists()) {
            logger.info("deserializing sell order pool");
            ObjectInputStream sellOrderPoolStream = new ObjectInputStream(new FileInputStream(sellOrderPoolFile));
            for (Object o : (Object[]) sellOrderPoolStream.readObject())
                sellOrderPool.add((Order) o);
            sellOrderPoolStream.close();
            logger.info(String.format("%d sell orders loaded into engine pool", sellOrderPool.size()));
        }
    }

    /**
     * Inits the.
     * 
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     * @throws CDAServiceException
     *             the cDA service exception
     */
    @PostConstruct
    public void init() throws IOException, CDAServiceException {
        if (profile != Profile.DEV && profile != Profile.PROD)
            profile = Profile.DEV;
        logger.info(String.format("tradeengine profile: %s, Thread: %s(%s)", profile, Thread.currentThread().getId(),
            Thread.currentThread().getName()));

        persistenceDataDirPath = Paths.get(persistenceDataDir);
        if (!Files.exists(persistenceDataDirPath)) {
            Files.createDirectories(persistenceDataDirPath);
        } else if (!Files.isDirectory(persistenceDataDirPath))
            throw new CDAServiceException(String.format("persistence data path: %s is not directory",
                persistenceDataDir));

    }

    /**
     * Make deal.
     * 
     * @return the price volume pair
     * @throws CDAServiceException
     *             the cDA service exception
     */
    private PriceVolumePair makeDeal() throws CDAServiceException {
        Order bo = null;
        Order so = null;

        if (profile == Profile.DEV) {
            bo = buyOrderPool.peek();
            so = sellOrderPool.peek();
        } else if (profile == Profile.PROD) {
            boolean exitLoop = false;
            for (Order b : buyOrderPool) {
                logger.debug(String.format("buy Order loop: %s",
                    BtcProtoCommonUtility.format(b, readableLoggingEnabled)));
                for (Order s : sellOrderPool) {
                    logger.debug(String.format("sell Order loop: %s",
                        BtcProtoCommonUtility.format(s, readableLoggingEnabled)));
                    BigInteger b_price = new BigInteger(b.getPrice().toByteArray());
                    BigInteger s_price = new BigInteger(s.getPrice().toByteArray());
                    if (b_price.compareTo(s_price) >= 0) {
                        if (b.getUserID() != s.getUserID()) {
                            bo = b;
                            so = s;
                            exitLoop = true;
                            break;
                        }
                    } else if (s.equals(sellOrderPool.peek())) {
                        exitLoop = true;
                        break;
                    } else
                        break;
                }
                if (exitLoop)
                    break;
            }
        } else
            throw new CDAServiceException(String.format("invalid profile: %s", profile));
        if (null == bo || null == so)
            return null;

        BigInteger bo_price = new BigInteger(bo.getPrice().toByteArray());
        BigInteger bo_volume = new BigInteger(bo.getVolume().toByteArray());
        BigInteger so_price = new BigInteger(so.getPrice().toByteArray());
        BigInteger so_volume = new BigInteger(so.getVolume().toByteArray());

        if (bo_price.compareTo(so_price) >= 0) {

            buyOrderPool.remove();
            sellOrderPool.remove();

            /** make the deal */
            BigInteger price = Long.compare(bo.getTimestamp(), so.getTimestamp()) < 0 ? bo_price : so_price;
            BigInteger volume = bo_volume.compareTo(so_volume) < 0 ? bo_volume : so_volume;
            PriceVolumePair pv = new PriceVolumePair();
            pv.setPrice(price);
            pv.setVolume(volume);

            if (bo_volume.compareTo(volume) == 0) {
                logger.info("buy order was fullfiled");
            } else {
                Order.Builder newBuyOrder = Order.newBuilder(bo);
                newBuyOrder.setVolume(ByteString.copyFrom(bo_volume.subtract(volume).toByteArray()));
                buyOrderPool.add(newBuyOrder.build());
            }

            if (so_volume.compareTo(volume) == 0) {
                logger.info("sell order was fullfilled");
            } else {
                Order.Builder newSellOrder = Order.newBuilder(so);
                newSellOrder.setVolume(ByteString.copyFrom(so_volume.subtract(volume).toByteArray()));
                sellOrderPool.add(newSellOrder.build());
            }

            MatchedOrder.Builder matchedOrder = MatchedOrder.newBuilder();
            matchedOrder.setOrderType(MatchedOrderType.FULL_FILLED);
            matchedOrder.setBuyOrder(bo);
            matchedOrder.setSellOrder(so);
            matchedOrder.setPrice(ByteString.copyFrom(price.toByteArray()));
            matchedOrder.setVolume(ByteString.copyFrom(volume.toByteArray()));
            matchedOrder.setTimestamp(new Date().getTime());

            final MatchedOrder mo = matchedOrder.build();
            matchedOrderMQTemplate.send(new MessageCreator() {
                public Message createMessage(Session session) throws JMSException {
                    BytesMessage message = session.createBytesMessage();
                    message.writeBytes(mo.toByteArray());
                    return message;
                }
            });

            logger.info(String.format("deal was made: %s", BtcProtoCommonUtility.format(mo, readableLoggingEnabled)));

            return pv;
        }
        return null;
    }

    /**
     * Send message through Message Queue to notify the order has been canceled.
     * 
     * @param orderCanceled
     *            the order canceled
     */
    private void notifyOrderCanceled(Order orderCanceled) {
        MatchedOrder.Builder matchedOrder = MatchedOrder.newBuilder();
        matchedOrder.setOrderToCancel(orderCanceled);
        matchedOrder.setOrderType(MatchedOrderType.CANCELED);
        matchedOrder.setTimestamp(new Date().getTime());

        final MatchedOrder mo = matchedOrder.build();
        matchedOrderMQTemplate.send(new MessageCreator() {
            public Message createMessage(Session session) throws JMSException {
                BytesMessage message = session.createBytesMessage();
                message.writeBytes(mo.toByteArray());
                return message;
            }
        });
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.google.common.util.concurrent.AbstractExecutionThreadService#run()
     */
    @Override
    protected void run() throws Exception {
        while (this.isRunning()) {
            if (!cancelOrderBlockingQueue.isEmpty()) {
                Order orderToCancel = cancelOrderBlockingQueue.take();
                logger.debug(String.format("handling order cancel request: %s",
                    BtcProtoCommonUtility.format(orderToCancel, readableLoggingEnabled)));

                for (PriorityQueue<Order> pool : orderPools) {
                    Order orderInPool = null;
                    if (null != pool) {
                        logger.debug("--- iterating orders from pool ---");
                        for (Order o : pool) {
                            logger.debug(BtcProtoCommonUtility.format(o, readableLoggingEnabled));
                            if (o.getOrderId() == orderToCancel.getOrderId())
                                orderInPool = o;
                        }
                        logger.debug("--- iterate finished ---");
                    }
                    if (null != orderInPool) {
                        pool.remove(orderInPool);
                        logger.info(String.format("order: %s was removed from pool",
                            BtcProtoCommonUtility.format(orderInPool, readableLoggingEnabled)));
                        break;
                    }
                }
                notifyOrderCanceled(orderToCancel);
            }
            PriceVolumePair pv = null;
            if (!buyOrderBlockingQueue.isEmpty()) {
                Order buyOrder = buyOrderBlockingQueue.take();
                buyOrderPool.add(buyOrder);
                if (!sellOrderPool.isEmpty()) {
                    pv = makeDeal();
                }
            } else if (!sellOrderQueue.isEmpty()) {
                Order sellOrder = sellOrderQueue.take();
                sellOrderPool.add(sellOrder);
                if (!buyOrderPool.isEmpty()) {
                    pv = makeDeal();
                }
            } else if (!buyOrderPool.isEmpty() && !sellOrderPool.isEmpty()) {
                pv = makeDeal();
            }
            if (null != pv) {
                updateConcludedPrice(pv.getPrice(), pv.getVolume(), new Date());
                logger.debug(String.format("number of event listeners : %d", eventListeners.size()));
            } else
                Thread.sleep(1000);
        }
        logger.debug("CDAService run loop stopped");
    }

    /**
     * Serialize.
     * 
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    private void serialize() throws IOException {
        logger.debug(String.format("buy blocking queue size: %d, sell blocking queue size: %d",
            buyOrderBlockingQueue.size(), sellOrderQueue.size()));
        logger.debug(String.format("buy order pool size: %d, sell order pool size: %d", buyOrderPool.size(),
            sellOrderPool.size()));
        /** serialize buy order blocking queue */
        logger.info("serializing buy order blocking queue");
        ObjectOutputStream buyOrderBlockingQueueStream = new ObjectOutputStream(new FileOutputStream(
            persistenceDataDirPath.resolve("buyOrderQueue.ser").toFile()));
        buyOrderBlockingQueueStream.writeObject(buyOrderBlockingQueue.toArray());
        buyOrderBlockingQueueStream.close();
        /** serialize sell order blocking queue */
        logger.info(String.format("%d buy orders serialized from blocking queue", buyOrderBlockingQueue.size()));
        logger.info("serializing sell order blocking queue");
        ObjectOutputStream sellOrderBlockingQueueStream = new ObjectOutputStream(new FileOutputStream(
            persistenceDataDirPath.resolve("sellOrderQueue.ser").toFile()));
        sellOrderBlockingQueueStream.writeObject(sellOrderQueue.toArray());
        sellOrderBlockingQueueStream.close();
        /** serialize buy order pool */
        logger.info(String.format("%d sell orders serialized from blocking queue", sellOrderQueue.size()));
        logger.info("serializing buy order pool");
        ObjectOutputStream buyOrderPoolStream = new ObjectOutputStream(new FileOutputStream(persistenceDataDirPath
            .resolve("buyOrderPool.ser").toFile()));
        buyOrderPoolStream.writeObject(buyOrderPool.toArray());
        buyOrderPoolStream.close();
        /** serialize sell order pool */
        logger.info(String.format("%d buy orders serialized from engine pool", buyOrderPool.size()));
        logger.info("serializing sell order pool");
        ObjectOutputStream sellOrderPoolStream = new ObjectOutputStream(new FileOutputStream(persistenceDataDirPath
            .resolve("sellOrderPool.ser").toFile()));
        sellOrderPoolStream.writeObject(sellOrderPool.toArray());
        sellOrderPoolStream.close();
        logger.info(String.format("%d sell orders serialized from engine pool", sellOrderPool.size()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.google.common.util.concurrent.AbstractExecutionThreadService#shutDown
     * ()
     */
    @Override
    protected void shutDown() throws Exception {
        serialize();

        logger.debug(String.format("CDSSerivce is going to shutdown, thread: %s(%s)", Thread.currentThread().getId(),
            Thread.currentThread().getName()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.google.common.util.concurrent.AbstractExecutionThreadService#startUp
     * ()
     */
    @Override
    protected void startUp() throws Exception {
        // run in separate thread
        logger.debug(String.format("startUp, thread: %s(%s)", Thread.currentThread().getId(), Thread.currentThread()
            .getName()));

        deserialize();
    }

    /**
     * Update concluded price.
     * 
     * @param price
     *            the price
     * @param volume
     *            the volume
     * @param timestamp
     *            the timestamp
     */
    public void updateConcludedPrice(final BigInteger price, final BigInteger volume, final Date timestamp) {
        for (final ListenerRegistration<ICDAEventListener> registration : eventListeners) {
            registration.executor.execute(new Runnable() {
                @Override
                public void run() {
                    registration.listener.onConcludedPriceUpdated(price, volume, timestamp);
                }
            });
        }
    }

}
