package com.drony.tester;

import com.drony.config.DronyConfig;
import com.dukascopy.api.IStrategy;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.LoadingProgressListener;
import com.dukascopy.api.system.ISystemListener;
import com.dukascopy.api.system.ITesterClient;
import com.dukascopy.api.system.TesterFactory;
import com.dukascopy.api.system.tester.ITesterExecution;
import com.dukascopy.api.system.tester.ITesterUserInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.Future;

class RoboClientRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoboClientRunner.class);

    ITesterClient client;

    void start(String jnlpUrl, String userName, String password, Instrument instrument,
               ITesterExecution testerExecution, ITesterUserInterface testerUserInterface,
               ISystemListener systemListener, IStrategy strategy) throws Exception {
        client = TesterFactory.getDefaultInstance();

        client.setSystemListener(systemListener);
        tryToConnect(jnlpUrl, userName, password);
        DronyConfig config = DronyConfig.get();
        setDataInterval(config.backtestFrom(), config.backtestTo());
        subscribeInstrument(instrument);
        downloadDataAndWaitForResult();

        //start the strategy
        LOGGER.info("Starting strategy");

        client.startStrategy(
                strategy,
                getLoadingProgressListener(),
                testerExecution,
                testerUserInterface
        );
        //now it's running
    }

    private LoadingProgressListener getLoadingProgressListener() {
        return new LoadingProgressListener() {
            @Override
            public void dataLoaded(long startTime, long endTime, long currentTime, String information) {
                //LOGGER.info(information);
            }

            @Override
            public void loadingFinished(boolean allDataLoaded, long startTime, long endTime, long currentTime) {
            }

            @Override
            public boolean stopJob() {
                return false;
            }
        };
    }

    private void downloadDataAndWaitForResult() throws InterruptedException, java.util.concurrent.ExecutionException {
        client.setInitialDeposit(Instrument.EURUSD.getSecondaryJFCurrency(), 100000);
        LOGGER.info("Downloading data");
        Future<?> future = client.downloadData(null);
        future.get();
    }

    private void tryToConnect(String jnlpUrl, String userName, String password) throws Exception {
        LOGGER.info("Connecting...");
        //connect to the server using jnlp, user name and password
        //connection is needed for data downloading
        client.connect(jnlpUrl, userName, password);

        //wait for it to connect
        int i = 10; //wait max ten seconds
        while (i > 0 && !client.isConnected()) {
            Thread.sleep(1000);
            i--;
        }
        if (!client.isConnected()) {
            LOGGER.error("Failed to connect Dukascopy servers");
            System.exit(1);
        }
    }


    private void subscribeInstrument(Instrument instrument) {
        final Set<Instrument> instruments = new HashSet<>();
        instruments.add(instrument);

        LOGGER.info("Subscribing instruments...");
        client.setSubscribedInstruments(instruments);

    }

    private void setDataInterval(String dateFrom, String dateTo) throws ParseException {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        Date dateFromObject = dateFormat.parse(dateFrom);
        Date dateToObject = dateFormat.parse(dateTo);

        System.out.println("Date start : " + dateFromObject.toString() + " date end: " + dateToObject.toString());

        client.setDataInterval(ITesterClient.DataLoadingMethod.ALL_TICKS, dateFromObject.getTime(), dateToObject.getTime());
        LOGGER.info("from: " + dateFrom + " to: " + dateTo);
    }
}
