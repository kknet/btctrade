package btctrade.jms.data.adaptors;

import javax.jms.Message;

import org.apache.log4j.Logger;

import btctrade.jms.data.TradeStatisticProtos.TradeStatistic;

import com.google.protobuf.InvalidProtocolBufferException;

public class TradeStatisticMessageAdaptor extends BaseMessageAdaptor {

	private static final Logger logger = Logger.getLogger(TradeStatistic.class);

	public TradeStatisticMessageAdaptor(Message message) {
		super(message);
	}

	public TradeStatistic getTradeStatistic() throws Exception {

		byte[] payload = getBytes();
		if (null != payload) {
			TradeStatistic.Builder st;
			try {
				st = TradeStatistic.newBuilder().mergeFrom(payload);
				return st.build();
			} catch (InvalidProtocolBufferException e) {
				logger.error("error:", e);
			}
		}

		throw new Exception("error : can not get wallet request from message");
	}
}
