package btctrade.tradeengine.jms;

import java.util.concurrent.LinkedBlockingQueue;

import javax.jms.Message;
import javax.jms.MessageListener;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import btctrade.jms.data.MatchedOrderProtos.MatchedOrder;
import btctrade.jms.data.MatchedOrderProtos.MatchedOrder.MatchedOrderType;
import btctrade.jms.data.adaptors.MatchedOrderMessageAdaptor;

public class MatchedOrderListener implements MessageListener {
	private static final Logger logger = Logger.getLogger(MatchedOrderListener.class);

	@Autowired
	@Qualifier("matchedOrderBlockingQueue")
	private LinkedBlockingQueue<MatchedOrder> matchedOrderBlockingQueue;
	
	@Override
	public void onMessage(Message msg) {
		MatchedOrderMessageAdaptor adaptor = new MatchedOrderMessageAdaptor(msg);

		MatchedOrder order = null;
		try {
			order = adaptor.getMatchedOrder();
		} catch (Exception e) {
			logger.error("error", e);
			return;
		}

		if (null != order) {
			try {
				if (order.getOrderType() == MatchedOrderType.FULL_FILLED) {
					matchedOrderBlockingQueue.add(order);
				} else if (order.getOrderType() == MatchedOrderType.CANCELED) {
					// orderService.orderCanceled(order);
				} else
					throw new Exception("invalid order type");
			} catch (Exception e) {
				logger.error(String.format("MatchedOrder %s was not handled", msg), e);
				throw new RuntimeException(e);
			}
		}
	}

}