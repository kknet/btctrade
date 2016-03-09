package btctrade.tradeengine;

import java.math.BigInteger;
import java.util.Date;

import org.apache.log4j.Logger;
import org.bitcoinj.utils.BtcFixedFormat;
import org.bitcoinj.utils.BtcFormat;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import btctrade.jms.service.TradeStatisticsPublisher;
import btctrade.tradeengine.service.CDAService;
import btctrade.tradeengine.service.ICDAEventListener;

public class App {

    static BtcFormat btcFormat = BtcFixedFormat.getInstance();

    private static final Logger logger = Logger.getLogger(App.class);

    public static void main(String[] args) throws Exception {
        logger.debug("starting trade engine...");

        final ApplicationContext context = new ClassPathXmlApplicationContext(
            new String[] { "btctrade/config/*.xml" });

        final CDAService service = context.getBean(CDAService.class);
        service.addEventListener(new ICDAEventListener() {
            private TradeStatisticsPublisher tradeStatisticsPublisher = context.getBean(TradeStatisticsPublisher.class);

            @Override
            public void onConcludedPriceUpdated(BigInteger price, BigInteger volume, Date timestamp) {
                logger.debug(String.format("latest price : %s latest volume : %s",
                    btcFormat.format(price), btcFormat.format(volume)));
                tradeStatisticsPublisher.publishPrice(price, volume, timestamp);
            }
        });
        service.startAsync();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                logger.info("Shutdown signal received, will shutdown...");
                service.stopAsync();
            }
        });

        logger.info(String.format("service started successfully, thread: %s(%s)", Thread.currentThread().getId(),
            Thread.currentThread().getName()));
    }
}
