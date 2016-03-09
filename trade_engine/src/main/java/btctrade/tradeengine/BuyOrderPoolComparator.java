package btctrade.tradeengine;

import java.math.BigInteger;
import java.util.Comparator;

import btctrade.jms.data.OrderProtos.Order;

public class BuyOrderPoolComparator implements Comparator<Order> {

    public int compare(Order o1, Order o2) {
        BigInteger price1 = new BigInteger(o1.getPrice().toByteArray());
        BigInteger price2 = new BigInteger(o2.getPrice().toByteArray());

        if (price1.compareTo(price2) == 0) {
            return Long.compare(o1.getTimestamp(), o2.getTimestamp());
        } else {
            return price2.compareTo(price1);
        }
    }
}
