package btctrade.jms.data.adaptors;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;

import org.apache.log4j.Logger;

public abstract class BaseMessageAdaptor {

	private static final Logger logger = Logger.getLogger(BaseMessageAdaptor.class);

	private Message message = null;

	public BaseMessageAdaptor(Message message) {
		this.message = message;
	}

	public byte[] getBytes() {

		logger.debug(String.format("message type : %s", message.getClass()));

		if (message instanceof BytesMessage) {

			BytesMessage msg = (BytesMessage) message;

			int bodyLen;
			try {
				bodyLen = (int) msg.getBodyLength();

				byte[] payLoad = new byte[bodyLen];
				msg.readBytes(payLoad);

				return payLoad;

			} catch (JMSException e) {
				logger.error("error: ", e);
			}
		} else {
			logger.info("This is not bytes message");
		}

		return null;
	}

	public Message getMessage() {
		return message;
	}
}
