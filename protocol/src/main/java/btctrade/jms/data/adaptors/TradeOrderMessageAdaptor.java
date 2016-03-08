package btctrade.jms.data.adaptors;

import javax.jms.Message;

import org.apache.log4j.Logger;

import com.google.protobuf.InvalidProtocolBufferException;

import btctrade.jms.data.OrderProtos.Order;

public class TradeOrderMessageAdaptor extends BaseMessageAdaptor {
	private static final Logger logger = Logger.getLogger(TradeOrderMessageAdaptor.class);

	public TradeOrderMessageAdaptor(Message message) {
		super(message);
	}

	public Order getOrder() throws Exception {
		byte[] payload = getBytes();
		if (null != payload) {
			Order.Builder orderMsg;
			try {
				orderMsg = Order.newBuilder().mergeFrom(payload);
				return orderMsg.build();
			} catch (InvalidProtocolBufferException e) {
				logger.error("error:", e);
			}
		}
		throw new Exception("error : can not get trade order from message");
	}
}
