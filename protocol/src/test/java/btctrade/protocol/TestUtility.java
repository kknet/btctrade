package btctrade.protocol;

import java.math.BigInteger;

import org.apache.log4j.Logger;
import org.bitcoinj.utils.BtcFixedFormat;
import org.junit.Test;

import btctrade.jms.data.adaptors.BaseMessageAdaptor;

public class TestUtility {

    private static final Logger logger = Logger.getLogger(BaseMessageAdaptor.class);

    @Test
    public void test() {
        String value = BtcFixedFormat.getInstance().format(new BigInteger("1234567890987654321"));
        logger.info(value);
    }

}
