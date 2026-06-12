package com.drony.tester;

import com.drony.DronyV042;
import com.drony.config.DronyConfig;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.LoadingProgressListener;
import com.dukascopy.api.OfferSide;
import com.dukascopy.api.Period;
import com.dukascopy.api.system.ISystemListener;
import com.dukascopy.api.system.ITesterClient;
import com.dukascopy.api.system.TesterFactory;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Backtest senza GUI, lanciabile in batch da script:
 *
 * <pre>
 * java -cp drony-4_2-jar-with-dependencies.jar com.drony.tester.HeadlessRunner \
 *   --param param/DronyParamV04.xlsx --out runs/test01 \
 *   --from "2020/07/01 00:00:00" --to "2020/09/30 23:59:00" \
 *   --instruments BRENTCMDUSD --method ALL_TICKS
 * </pre>
 *
 * Credenziali e default da drony.properties (o -Ddrony.config=...). Output nella
 * cartella --out: report.html, results.csv (metriche per strategia), decisions.csv
 * (giornale decisionale), DronyReport_*.xlsx.
 *
 * --method controlla velocità vs precisione:
 *   ALL_TICKS                 tutti i tick (lento, massima precisione)
 *   DIFFERENT_PRICE_TICKS     solo tick con prezzo diverso dal precedente
 *   PIVOT_TICKS               solo tick di pivot
 *   CANDLE:<PERIOD>           candele interpolate (es. CANDLE:ONE_HOUR) — veloce, per screening
 */
public class HeadlessRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeadlessRunner.class);

    public static void main(String[] args) throws Exception {

        Map<String, String> opts = parseArgs(args);
        DronyConfig config = DronyConfig.get();

        File paramFile = new File(opts.getOrDefault("param", config.paramFile().getPath()));
        File outDir = new File(opts.getOrDefault("out", "runs/run"));
        outDir.mkdirs();

        String from = opts.getOrDefault("from", config.backtestFrom());
        String to = opts.getOrDefault("to", config.backtestTo());
        String method = opts.getOrDefault("method", "ALL_TICKS");

        Set<Instrument> instruments = Arrays
                .stream(opts.getOrDefault("instruments", config.instrument()).split(","))
                .map(String::trim)
                .map(Instrument::valueOf)
                .collect(Collectors.toSet());

        ITesterClient client = TesterFactory.getDefaultInstance();

        CountDownLatch strategyDone = new CountDownLatch(1);
        client.setSystemListener(new ISystemListener() {
            @Override
            public void onStart(long processId) {
                LOGGER.info("Strategy started: {}", processId);
            }

            @Override
            public void onStop(long processId) {
                LOGGER.info("Strategy stopped: {}", processId);
                try {
                    client.createReport(processId, new File(outDir, "report.html"));
                } catch (Exception e) {
                    LOGGER.error("createReport failed", e);
                }
                strategyDone.countDown();
            }

            @Override
            public void onConnect() {
                LOGGER.info("Connected");
            }

            @Override
            public void onDisconnect() {
            }
        });

        LOGGER.info("Connecting...");
        client.connect(config.jnlpUrl(), config.userName(), config.password());
        for (int i = 10; i > 0 && !client.isConnected(); i--) {
            Thread.sleep(1000);
        }
        if (!client.isConnected()) {
            LOGGER.error("Failed to connect to Dukascopy servers");
            System.exit(1);
        }

        setDataInterval(client, method, from, to);
        client.setSubscribedInstruments(instruments);
        client.setInitialDeposit(Instrument.EURUSD.getSecondaryJFCurrency(), 100000);

        LOGGER.info("Downloading data...");
        Future<?> download = client.downloadData(null);
        download.get();

        DronyV042 strategy = new DronyV042();
        strategy.fileParam = paramFile;
        strategy.fileResult = outDir;
        strategy.fileDecisions = new File(outDir, "decisions.csv");
        strategy.outputVerbose = Boolean.parseBoolean(opts.getOrDefault("verbose", "false"));

        LOGGER.info("Starting strategy: param={} out={} interval={} -> {} method={}",
                paramFile, outDir, from, to, method);
        client.startStrategy(strategy, progressListener());

        boolean finished = strategyDone.await(
                Long.parseLong(opts.getOrDefault("timeoutMinutes", "120")), TimeUnit.MINUTES);
        if (!finished) {
            LOGGER.error("Timeout: strategia non terminata");
            System.exit(2);
        }

        LOGGER.info("Done. Output in {}", outDir.getAbsolutePath());
        System.exit(0);
    }

    private static void setDataInterval(ITesterClient client, String method, String from, String to)
            throws Exception {

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        long fromMillis = dateFormat.parse(from).getTime();
        long toMillis = dateFormat.parse(to).getTime();

        if (method.startsWith("CANDLE:")) {
            Period period = Period.valueOf(method.substring("CANDLE:".length()));
            client.setDataInterval(period, OfferSide.BID,
                    ITesterClient.InterpolationMethod.FOUR_TICKS, fromMillis, toMillis);
        } else {
            client.setDataInterval(ITesterClient.DataLoadingMethod.valueOf(method),
                    fromMillis, toMillis);
        }
    }

    private static LoadingProgressListener progressListener() {
        return new LoadingProgressListener() {
            @Override
            public void dataLoaded(long start, long end, long current, String information) {
            }

            @Override
            public void loadingFinished(boolean allDataLoaded, long start, long end, long current) {
            }

            @Override
            public boolean stopJob() {
                return false;
            }
        };
    }

    /** Parser minimale: --chiave valore. */
    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> opts = new HashMap<>();
        for (int i = 0; i < args.length - 1; i += 2) {
            if (!args[i].startsWith("--")) {
                throw new IllegalArgumentException("Argomento non riconosciuto: " + args[i]);
            }
            opts.put(args[i].substring(2), args[i + 1]);
        }
        return opts;
    }
}
