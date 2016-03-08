package btctrade.jms.data.adaptors;

import javax.jms.Message;

import org.apache.log4j.Logger;

import btctrade.jms.data.MatchedOrderProtos.MatchedOrder;

import com.google.protobuf.InvalidProtocolBufferException;

public class MatchedOrderMessageAdaptor extends BaseMessageAdaptor {

	private static final Logger logger = Logger.getLogger(MatchedOrderMessageAdaptor.class);

	public MatchedOrderMessageAdaptor(Message message) {
		super(message);
	}

	public MatchedOrder getMatchedOrder() throws Exception {

		byte[] payload = getBytes();
		if (null != payload) {
			MatchedOrder.Builder mo;
			try {
				mo = MatchedOrder.newBuilder().mergeFrom(payload);

				return mo.build();
			} catch (InvalidProtocolBufferException e) {
				logger.error("error:", e);
			}
		}

		throw new Exception("error : can not get matched order from message");
	}

}
