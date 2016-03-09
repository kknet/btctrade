package btctrade.jms.service;

import java.util.concurrent.LinkedBlockingQueue;

import javax.jms.Message;
import javax.jms.MessageListener;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import btctrade.jms.data.OrderProtos.Order;
import btctrade.jms.data.OrderProtos.Order.OrderType;
import btctrade.jms.data.adaptors.TradeOrderMessageAdaptor;
import btctrade.jms.data.utility.BtcProtoCommonUtility;

public class OrderRequestListener implements MessageListener {
    private static final Logger logger = Logger.getLogger(OrderRequestListener.class);

    @Autowired
    @Qualifier("buyOrderBlockingQueue")
    private LinkedBlockingQueue<Order> buyOrderBlockingQueue;

    @Autowired
    @Qualifier("cancelOrderBlockingQueue")
    private LinkedBlockingQueue<Order> cancelOrderBlockingQueue;

    private boolean readable = true;

    @Autowired
    @Qualifier("sellOrderBlockingQueue")
    private LinkedBlockingQueue<Order> sellOrderBlockingQueue;

    @Autowired
    private QueueSizeCounter sizeCounter;

    public void onMessage(Message message) {
        logger.info(String.format("message received, %d message in queue.",
            sizeCounter.getQueueSize("OrderRequestQueue-qa")));

        logger.debug(String.format("Thread: %s(%s)", Thread.currentThread().getId(), Thread.currentThread().getName()));

        Order order;
        try {
            order = new TradeOrderMessageAdaptor(message).getOrder();

            if (null != order) {
                logger.debug(String.format("new order request: %s", BtcProtoCommonUtility.format(order, readable)));
                try {
                    if (order.getOrderType() == OrderType.BUY_ORDER) {
                        buyOrderBlockingQueue.put(order);
                        logger.debug(String.format("buy blocking queue size: %d", buyOrderBlockingQueue.size()));
                    } else if (order.getOrderType() == OrderType.SELL_ORDER) {
                        sellOrderBlockingQueue.put(order);
                        logger.debug(String.format("sell blocking queue size: %d", buyOrderBlockingQueue.size()));
                    } else if (order.getOrderType() == OrderType.CANCEL_ORDER) {
                        cancelOrderBlockingQueue.put(order);
                        logger.debug(String.format("cancel request blocking queue size: %d",
                            cancelOrderBlockingQueue.size()));
                    } else
                        throw new Exception("wrong order type");

                } catch (InterruptedException e) {
                    logger.error(String.format("Order %d was not enqueued", order.getOrderId()));
                }
            }
        } catch (Exception e) {
            logger.error("error", e);
        }
    }
}
