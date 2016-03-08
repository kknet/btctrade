package btctrade.jms.data.adaptors;

import javax.jms.Message;

import org.apache.log4j.Logger;

import btctrade.jms.data.BtcWalletProtos.BtcWallet;

import com.google.protobuf.InvalidProtocolBufferException;

public class BtcWalletMessageAdaptor extends BaseMessageAdaptor {

    private static final Logger logger = Logger.getLogger(BtcWalletMessageAdaptor.class);

    public BtcWalletMessageAdaptor(Message message) {
        super(message);
    }

    public BtcWallet getWallet() throws Exception {

        byte[] payload = getBytes();
        if (null != payload) {
            BtcWallet.Builder w;
            try {
                w = BtcWallet.newBuilder().mergeFrom(payload);

                return w.build();
            } catch (InvalidProtocolBufferException e) {
                logger.error("error:", e);
            }
        }

        throw new Exception("error : can not get wallet request from message");
    }
}
