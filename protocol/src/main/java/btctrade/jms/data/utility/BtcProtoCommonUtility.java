package btctrade.jms.data.utility;

import java.math.BigInteger;

import org.bitcoinj.utils.BtcFixedFormat;
import org.bitcoinj.utils.BtcFormat;

import btctrade.jms.data.MatchedOrderProtos.MatchedOrder;
import btctrade.jms.data.OrderProtos.Order;

public class BtcProtoCommonUtility {

    static BtcFormat btcFormat = BtcFixedFormat.getInstance();

    public static String format(MatchedOrder mo, boolean readable) {
        if (readable) {
            String price = btcFormat.format(new BigInteger(mo.getPrice().toByteArray()));
            String volume = btcFormat.format(new BigInteger(mo.getVolume().toByteArray()));
            return String.format("[price: %s, volume: %s, timestamp: %d, \n\tbuy order: %s\n\tsell order: %s]", price,
                volume, mo.getTimestamp(), format(mo.getBuyOrder(), readable), format(mo.getSellOrder(), readable));
        } else
            return mo.toString();
    }

    public static String format(Order order, boolean readable) {
        if (readable) {
            String price = btcFormat.format(new BigInteger(order.getPrice().toByteArray()));
            String volume = btcFormat.format(new BigInteger(order.getVolume().toByteArray()));
            return String.format("[orderID: %d, userID: %d, price: %s, volume: %s, orderType: %s, timestamp: %d]",
                order.getOrderId(), order.getUserID(), price, volume, order.getOrderType(), order.getTimestamp());
        } else
            return order.toString();
    }
}
