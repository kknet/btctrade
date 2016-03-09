package btctrade.tradeengine;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import btctrade.jms.data.MatchedOrderProtos.MatchedOrder;
import btctrade.jms.data.utility.BtcProtoCommonUtility;
import btctrade.tradeengine.jms.SimpleMQSender;

import com.google.bitcoin.core.Utils;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:/btctrade/jms/config/JMSConfig.xml",
		"classpath:/btctrade/jms/config/MQControl.xml", "classpath:/btctrade/jms/config/TradeEngineConfig.xml",
		"classpath:/btctrade/tradeengine/config/JMSConfig_test.xml",
		"classpath:/btctrade/tradeengine/config/AppConfig_test.xml" })
public class AppTest {

	protected static Thread engine;

	private static final Logger logger = Logger.getLogger(AppTest.class);

	@AfterClass
	public static void shutdownEngine() throws InterruptedException {
		if (null != engine)
			engine.stop();
	}

	@BeforeClass
	public static void startEngine() {
		engine = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					App.main(null);
				} catch (Exception e) {
					logger.error("error", e);
				}
			}
		});
		engine.start();
	}

	@Autowired
	@Qualifier("matchedOrderBlockingQueue")
	private LinkedBlockingQueue<MatchedOrder> matchedOrderBlockingQueue;

	@Autowired
	private SimpleMQSender mq;

	@Before
	public void beforeTest() {
		/** runs before each test */
	}

	@Test
	public void test1() throws Exception {
		testTradeEngine("./src/test/java/btctrade/tradeengine/test_data_1.txt");
	}

	protected void testTradeEngine(String testFile) throws Exception {
		File f = new File(testFile);
		if (!f.exists())
			throw new Exception(String.format("no such test file: %s", testFile));

		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(f));
			String line;

			/** read the input order */
			Pattern inputPattern = Pattern.compile("^(\\+|-)\\s+(\\d+)\\s+(\\S+)\\s+(\\S+)$");
			while (true) {
				line = reader.readLine().trim();
				Matcher m = inputPattern.matcher(line);
				if (m.matches())
					mq.placeOrder(m.group(1), m.group(2), m.group(3), m.group(4));
				else
					break;
			}

			/** read the expected number of matched orders */
			long expectedMatchedOrder = -1;
			Pattern moNumPattern = Pattern.compile("^\\d+$");
			while (true) {
				line = reader.readLine().trim();
				Matcher m = moNumPattern.matcher(line);
				if (m.matches()) {
					expectedMatchedOrder = Integer.parseInt(m.group());
					break;
				} else if (line.length() > 0)
					throw new Exception("invalid expected number of matched orders");
			}

			/** read expected output and validate output from trade engine */
			while (expectedMatchedOrder > 0) {
				if (!matchedOrderBlockingQueue.isEmpty()) {
					MatchedOrder mo = matchedOrderBlockingQueue.take();
					logger.info(String.format("---> %s", BtcProtoCommonUtility.format(mo, true)));
					Matcher m = nextExpectedOutput(reader);
					assertTrue(isActualMatchedOrder(mo, m.group(1), m.group(2), m.group(3), m.group(4)));
					--expectedMatchedOrder;
				} else
					Thread.sleep(1000);
			}
		} finally {
			if (null != reader)
				reader.close();
		}
	}

	Pattern outputPattern = Pattern.compile("^(\\d+)\\s+(\\d+)\\s+(\\S+)\\s+(\\S+)$");

	protected Matcher nextExpectedOutput(BufferedReader reader) throws IOException {
		while (true) {
			String line = reader.readLine();
			if (null == line)
				return null;
			Matcher m = outputPattern.matcher(line.trim());
			if (m.matches())
				return m;
		}

	}

	protected boolean isActualMatchedOrder(MatchedOrder actualMatchedOrder, String buyOrderID, String sellOrderID,
			String price, String volume) {
		if (actualMatchedOrder.getBuyOrder().getOrderId() != Long.parseLong(buyOrderID))
			return false;
		if (actualMatchedOrder.getSellOrder().getOrderId() != Long.parseLong(sellOrderID))
			return false;
		BigInteger p1 = Utils.toNanoCoins(price);
		BigInteger p2 = new BigInteger(actualMatchedOrder.getPrice().toByteArray());
		if (p1.compareTo(p2) != 0)
			return false;
		BigInteger v1 = Utils.toNanoCoins(volume);
		BigInteger v2 = new BigInteger(actualMatchedOrder.getVolume().toByteArray());
		if (v1.compareTo(v2) != 0)
			return false;
		return true;
	}
}
