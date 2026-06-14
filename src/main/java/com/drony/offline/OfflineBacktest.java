package com.drony.offline;

import com.drony.strategy.BarTestInit;
import com.drony.strategy.data.DirectionEnum;
import com.drony.strategy.data.ParamDrony;
import com.drony.strategy.data.StrategyStats;
import com.drony.strategy.data.StrategyTypeEnum;
import com.drony.strategy.test.AbstractBarTest;
import com.drony.strategy.test.BarBodyAbs;
import com.drony.strategy.test.BarBodyPercent;
import com.drony.strategy.test.BarColorStory;
import com.drony.strategy.test.BarDirection;
import com.drony.strategy.test.BarMod;
import com.drony.strategy.test.BarSame;
import com.drony.strategy.test.BarSlope;
import com.drony.strategy.utility.ReaderParam;
import com.drony.strategy.utility.TimeUtility;
import com.drony.strategy.utility.Utility;
import com.dukascopy.api.IBar;
import com.dukascopy.api.IConsole;
import com.dukascopy.api.IHistory;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.JFException;
import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Backtest OFFLINE a barre: nessuna connessione Dukascopy, nessun account demo.
 * Riusa al 100% i filtri di ingresso reali (le classi Bar* via BarTestInit), con
 * un motore di ordini semplificato per l'uscita.
 *
 * <pre>
 * java -cp drony-4_2-jar-with-dependencies.jar com.drony.offline.OfflineBacktest \
 *   --param runs/eurusd_side/param_001.xlsx --csv data/EURUSD_Daily.csv --out runs/offline01
 * </pre>
 *
 * SCOPO: pre-screening illimitato e istantaneo (apre trade? in che regime? con
 * che segno grezzo?) per scartare le combinazioni morte senza spendere account.
 * NON sostituisce il tester JForex: i candidati promettenti vanno SEMPRE
 * riconfermati con HeadlessRunner --method ALL_TICKS.
 *
 * Cosa è fedele: ingresso (i 7 filtri, identici al runtime reale), prezzo/SL/TP
 * iniziali, scadenza ordine, chiusura per numero barre, finestra oraria,
 * strategyType, preventMultipleOrders.
 * Cosa NON è simulato (documentato): pinza dinamica (cap_attn/floor_attn),
 * break-even, edge order, cluster, fill intrabar coi tick, spread. Per questo i
 * pips offline divergono dal tester reale (in particolare mancano gli scratch a
 * zero del break-even); affidabili sono il CONTEGGIO trade e il SEGNO.
 */
public class OfflineBacktest {

    private enum State {PENDING, FILLED}

    private static final class SimOrder {
        DirectionEnum direction;
        double trigger;
        double stopLoss;
        double takeProfit;
        long gtt;
        State state = State.PENDING;
        double entry;
        int barsFilled;
    }

    public static void main(String[] args) throws Exception {
        Map<String, String> opts = parseArgs(args);
        File paramFile = new File(required(opts, "param"));
        Path csv = Path.of(required(opts, "csv"));
        File outDir = new File(required(opts, "out"));
        outDir.mkdirs();

        List<IBar> bars = CsvBarReader.read(csv);
        if (bars.size() < 10) {
            throw new IllegalArgumentException("Troppe poche barre nel CSV: " + bars.size());
        }
        System.out.printf("Barre lette: %d (%s .. %s)%n", bars.size(),
                Utility.formatDateTime(bars.get(0).getTime()),
                Utility.formatDateTime(bars.get(bars.size() - 1).getTime()));

        List<ParamDrony> params = new ReaderParam(paramFile, nullConsole()).getDronies();
        System.out.println("Strategie: " + params.size());

        File resultsFile = new File(outDir, "results.csv");
        try (PrintWriter out = new PrintWriter(resultsFile)) {
            out.println(StrategyStats.csvHeader());
            for (ParamDrony param : params) {
                StrategyStats stats = backtestOne(param, bars);
                out.println(stats.toCsvRow());
                System.out.printf("  %-14s trades=%-4d plPips=%.1f%n",
                        param.getName(), stats.getTrades(), stats.getPlPips());
            }
        }
        System.out.println("Results in " + resultsFile.getAbsolutePath());
    }

    private static StrategyStats backtestOne(ParamDrony param, List<IBar> bars) throws JFException {
        StrategyStats stats = new StrategyStats(param.getName(),
                param.getSelectedInstrument().name(), param.getSelectedPeriod().name());

        Instrument instrument = param.getSelectedInstrument();
        double pipValue = instrument.getPipValue();
        long interval = param.getSelectedPeriod().getInterval();
        IHistory history = fakeHistory(bars);

        List<SimOrder> active = new ArrayList<>();

        for (int i = 0; i < bars.size(); i++) {
            IBar bar = bars.get(i);

            /* 1. gestione ordini esistenti contro la barra corrente */
            active.removeIf(o -> processOrder(o, bar, param, pipValue, stats));

            /* 2. nuovo segnale (come DronyStrategy.onBar) */
            if (!TimeUtility.checkTradingTimeLimit(param, bar)) {
                continue;
            }
            if (param.isPreventMultipleOrders() && !active.isEmpty()) {
                continue;
            }
            DirectionEnum color = bar.getClose() > bar.getOpen() ? DirectionEnum.BUY
                    : bar.getClose() < bar.getOpen() ? DirectionEnum.SELL : DirectionEnum.DOJI;
            if (color == DirectionEnum.DOJI) {
                continue;
            }
            if (color == DirectionEnum.SELL && allowed(param, StrategyTypeEnum.SHORT)) {
                trySignal(param, history, instrument, bar.getTime(), DirectionEnum.SELL, active, interval);
            } else if (color == DirectionEnum.BUY && allowed(param, StrategyTypeEnum.LONG)) {
                trySignal(param, history, instrument, bar.getTime(), DirectionEnum.BUY, active, interval);
            }
        }
        return stats;
    }

    private static boolean allowed(ParamDrony param, String side) {
        return param.getStrategyType().equals(StrategyTypeEnum.FULL)
                || param.getStrategyType().equals(side);
    }

    /** Valuta i 7 filtri (identici al runtime reale); se passano crea un ordine stop. */
    private static void trySignal(ParamDrony param, IHistory history, Instrument instrument,
            long time, DirectionEnum direction, List<SimOrder> active, long interval)
            throws JFException {

        int n = Math.max(param.getN(), 2);
        com.dukascopy.api.OfferSide side =
                direction == DirectionEnum.BUY ? com.dukascopy.api.OfferSide.ASK
                        : com.dukascopy.api.OfferSide.BID;
        List<IBar> seq = history.getBars(instrument, param.getSelectedPeriod(), side,
                param.getCandleFilter(), n, time, 0);
        if (seq.size() < 2) {
            return;
        }

        for (IBar b : seq) {
            BarTestInit init = new BarTestInit(b, param, instrument, direction);
            if (!pass(new BarDirection(init), new BarBodyAbs(init), new BarBodyPercent(init))) {
                return;
            }
        }

        IBar bar = seq.get(seq.size() - 1);
        IBar slopeBar = seq.get(0);
        BarTestInit ctx = new BarTestInit(bar, param, instrument, direction,
                history, param.getSelectedPeriod(), slopeBar);
        if (!pass(new BarSame(ctx), new BarColorStory(ctx), new BarSlope(ctx), new BarMod(ctx))) {
            return;
        }

        IBar prevBar = seq.get(seq.size() - Math.min(seq.size(), param.getN()));
        active.add(buildOrder(param, instrument, bar, prevBar, direction, time, interval));
    }

    private static boolean pass(AbstractBarTest... tests) {
        for (AbstractBarTest t : tests) {
            if (!t.testBar().isResult()) {
                return false;
            }
        }
        return true;
    }

    /** Replica di DronyOrderService.createOrder: prezzo, SL, TP, scadenza. */
    private static SimOrder buildOrder(ParamDrony param, Instrument instrument, IBar bar,
            IBar prevBar, DirectionEnum direction, long time, long interval) {

        double mod = Math.abs(bar.getClose() - prevBar.getOpen());
        double identFixed = Utility.fromPipToPrice(param.getIndent(), instrument);
        double stopLossDelta = param.getFloorAbs() + (param.getFloor_perc() / 100) * mod;
        double takeProfitDelta = param.getCapAbs() + (param.getCap_perc() / 100) * mod;

        SimOrder o = new SimOrder();
        o.direction = direction;
        if (direction == DirectionEnum.BUY) {
            o.trigger = Utility.roundByDefaultPrecision(bar.getClose()
                    + Math.max((param.getIndentPercentPennachio() / 100)
                    * (bar.getHigh() - bar.getClose()), 0) + identFixed, instrument);
            o.stopLoss = Utility.roundByDefaultPrecision(o.trigger - stopLossDelta, instrument);
            o.takeProfit = Utility.roundByDefaultPrecision(o.trigger + takeProfitDelta, instrument);
        } else {
            o.trigger = Utility.roundByDefaultPrecision(bar.getClose()
                    - (Math.max((param.getIndentPercentPennachio() / 100)
                    * (bar.getClose() - bar.getLow()), 0) + identFixed), instrument);
            o.stopLoss = Utility.roundByDefaultPrecision(o.trigger + stopLossDelta, instrument);
            o.takeProfit = Utility.roundByDefaultPrecision(o.trigger - takeProfitDelta, instrument);
        }
        o.gtt = time + (long) param.getNumCandlesValid() * interval;
        return o;
    }

    /**
     * Avanza un ordine di una barra. @return true se l'ordine è terminato
     * (cancellato/chiuso) e va rimosso.
     */
    private static boolean processOrder(SimOrder o, IBar bar, ParamDrony param, double pipValue,
            StrategyStats stats) {

        boolean isLong = o.direction == DirectionEnum.BUY;

        if (o.state == State.PENDING) {
            if (bar.getTime() >= o.gtt) {
                return true; /* scaduto, mai fillato */
            }
            boolean triggered = isLong ? bar.getHigh() >= o.trigger : bar.getLow() <= o.trigger;
            if (!triggered) {
                return false;
            }
            o.state = State.FILLED;
            o.entry = o.trigger;
            o.barsFilled = 0;
            /* nella stessa barra del fill puo' gia' scattare SL/TP (gap) */
        }

        o.barsFilled++;

        boolean hitSL = isLong ? bar.getLow() <= o.stopLoss : bar.getHigh() >= o.stopLoss;
        boolean hitTP = isLong ? bar.getHigh() >= o.takeProfit : bar.getLow() <= o.takeProfit;

        if (hitSL) { /* conservativo: in caso di ambiguita' SL prima di TP */
            close(o, o.stopLoss, pipValue, stats);
            return true;
        }
        if (hitTP) {
            close(o, o.takeProfit, pipValue, stats);
            return true;
        }
        if (o.barsFilled >= param.getOrderNumMaxBar()) {
            close(o, bar.getClose(), pipValue, stats);
            return true;
        }
        return false;
    }

    private static void close(SimOrder o, double exit, double pipValue, StrategyStats stats) {
        double sign = o.direction == DirectionEnum.BUY ? 1 : -1;
        double plPips = sign * (exit - o.entry) / pipValue;
        stats.onOrderClosed(plPips, plPips);
    }

    /** IHistory finto: serve getBars(count, time) dalla lista in memoria. */
    private static IHistory fakeHistory(List<IBar> bars) {
        Map<Long, Integer> indexByTime = new HashMap<>();
        for (int i = 0; i < bars.size(); i++) {
            indexByTime.put(bars.get(i).getTime(), i);
        }
        return (IHistory) Proxy.newProxyInstance(
                IHistory.class.getClassLoader(),
                new Class<?>[]{IHistory.class},
                (proxy, method, mArgs) -> {
                    if (method.getName().equals("getBars")) {
                        int count = ((Number) mArgs[4]).intValue();
                        long time = ((Number) mArgs[5]).longValue();
                        Integer idx = indexByTime.get(time);
                        if (idx == null) {
                            return new ArrayList<IBar>();
                        }
                        int from = Math.max(0, idx - count + 1);
                        return new ArrayList<>(bars.subList(from, idx + 1));
                    }
                    if (method.getReturnType().equals(boolean.class)) {
                        return false;
                    }
                    return null;
                });
    }

    private static IConsole nullConsole() {
        PrintStream sink = new PrintStream(PrintStream.nullOutputStream());
        return (IConsole) Proxy.newProxyInstance(
                IConsole.class.getClassLoader(),
                new Class<?>[]{IConsole.class},
                (proxy, method, a) ->
                        PrintStream.class.isAssignableFrom(method.getReturnType()) ? sink : null);
    }

    private static String required(Map<String, String> opts, String key) {
        String v = opts.get(key);
        if (v == null) {
            throw new IllegalArgumentException("Argomento obbligatorio mancante: --" + key);
        }
        return v;
    }

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
