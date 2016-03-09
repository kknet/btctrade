package btctrade.jms.service;

import java.math.BigInteger;
import java.util.Date;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

import btctrade.jms.data.OrderProtos.Order;
import btctrade.jms.data.OrderProtos.Order.OrderType;
import btctrade.jms.data.TradeStatisticProtos.TradeStatistic;
import btctrade.jms.data.TradeStatisticProtos.TradeStatistic.TradeStatisticType;

import com.google.protobuf.ByteString;

public class TradeStatisticsPublisher {

	@Autowired
	@Qualifier("tradeStatisticsTopicTemplate")
	private JmsTemplate tradeStatisticsTopicTemplate;

	public void publishOrder(Order order, boolean isEnqueue){
		final TradeStatistic.Builder st = TradeStatistic.newBuilder();
		if(order.getOrderType()==OrderType.BUY_ORDER&&isEnqueue)
			st.setTradeStatisticType(TradeStatisticType.UPDATE_BID_POSITION_ENQUEUE);
		else if(order.getOrderType()==OrderType.BUY_ORDER&&!isEnqueue)
			st.setTradeStatisticType(TradeStatisticType.UPDATE_BID_POSITION_DEQUEUE);
		else if(order.getOrderType()==OrderType.SELL_ORDER&&isEnqueue)
			st.setTradeStatisticType(TradeStatisticType.UPDATE_ASK_POSITION_ENQUEUE);
		else if(order.getOrderType()==OrderType.SELL_ORDER&&!isEnqueue)
			st.setTradeStatisticType(TradeStatisticType.UPDATE_ASK_POSITION_DEQUEUE);
		// TO-DO
	}
	
	public void publishPrice(BigInteger price, BigInteger volume, Date timestamp) {
		final TradeStatistic.Builder st = TradeStatistic.newBuilder();
		st.setTradeStatisticType(TradeStatisticType.UPDATE_PRICE_VOLUME);
		st.setPrice(ByteString.copyFrom(price.toByteArray()));
		st.setVolume(ByteString.copyFrom(volume.toByteArray()));
		if (null == timestamp) {
			timestamp = new Date();
		}
		st.setTimestamp(timestamp.getTime());
		tradeStatisticsTopicTemplate.send(new MessageCreator() {
			@Override
			public Message createMessage(Session session) throws JMSException {
				BytesMessage message = session.createBytesMessage();
				message.writeBytes(st.build().toByteArray());
				return message;
			}
		});
	}
}
