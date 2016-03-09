package btctrade.tradeengine.jms;

import java.util.Date;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.bitcoinj.utils.BtcFixedFormat;
import org.bitcoinj.utils.BtcFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

import btctrade.jms.data.OrderProtos.Order;
import btctrade.jms.data.OrderProtos.Order.Builder;
import btctrade.jms.data.OrderProtos.Order.OrderType;

import com.google.bitcoin.core.Utils;
import com.google.protobuf.ByteString;

public class SimpleMQSender {

    private static long nextOrderID = 1;

    public static synchronized long getNextOrderID() {
        long id = nextOrderID;
        ++nextOrderID;
        return id;
    }

    @Autowired
    @Qualifier("orderRequestMQTemplate_test")
    protected JmsTemplate requestMQTemplate;

    public void placeOrder(String orderTypeSign, String userID, String price, String volume) throws Exception {
        Builder o = Order.newBuilder();
        o.setOrderId(getNextOrderID());
        o.setUserID(Integer.parseInt(userID));
        ByteString priceByteArray = ByteString.copyFrom(Utils.toNanoCoins(price).toByteArray());
        o.setPrice(priceByteArray);
        ByteString volumeByteArray = ByteString.copyFrom(Utils.toNanoCoins(volume).toByteArray());
        o.setVolume(volumeByteArray);
        o.setTimestamp(new Date().getTime());
        if ("+".equalsIgnoreCase(orderTypeSign))
            o.setOrderType(OrderType.BUY_ORDER);
        else if ("-".equalsIgnoreCase(orderTypeSign))
            o.setOrderType(OrderType.SELL_ORDER);
        else
            throw new Exception("invalid order sign, must be +(plus) or -(minus)");

        final Order order = o.build();
        requestMQTemplate.send(new MessageCreator() {
            public Message createMessage(Session session) throws JMSException {
                BytesMessage message = session.createBytesMessage();
                message.writeBytes(order.toByteArray());
                return message;
            }
        });
    }
}
