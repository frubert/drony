package com.drony.strategy.test;

import com.drony.strategy.BarTestInit;
import com.drony.strategy.data.DirectionEnum;
import com.drony.strategy.data.ParamDrony;
import com.dukascopy.api.IBar;
import com.dukascopy.api.IHistory;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.JFException;
import com.dukascopy.api.Period;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

/**
 * Supporto per testare i filtri Bar* con barre sintetiche, senza connessione
 * Dukascopy: barre costruite a mano, parametri di default sovrascrivibili per
 * gruppo, IHistory finto che restituisce una lista preparata.
 */
final class TestSupport {

    static final Instrument INSTRUMENT = Instrument.EURUSD;
    static final Period PERIOD = Period.ONE_HOUR;

    private TestSupport() { }

    /** Barra sintetica. */
    record TestBar(long getTime, double getOpen, double getClose, double getHigh, double getLow)
            implements IBar {

        @Override
        public double getVolume() {
            return 1;
        }
    }

    /** Barra con high/low derivati: high = max(open,close)+shadow, low = min(open,close)-shadow. */
    static IBar bar(double open, double close, double shadow) {
        return new TestBar(0, open, close, Math.max(open, close) + shadow,
                Math.min(open, close) - shadow);
    }

    static ParamDrony.SequenceFilter defaultSequence() {
        return new ParamDrony.SequenceFilter(2, 10, 95, 10, 100, 0.5, 50, 1, 20);
    }

    static ParamDrony.ShadowFilter defaultShadow() {
        return new ParamDrony.ShadowFilter(1, 20, 0, 0, 0);
    }

    static ParamDrony.ColorStory defaultColorStory() {
        return new ParamDrony.ColorStory(2, 0);
    }

    static ParamDrony param(ParamDrony.SequenceFilter sequence, ParamDrony.ShadowFilter shadow,
            ParamDrony.ColorStory colorStory) {
        return new ParamDrony(
                "TEST", INSTRUMENT, PERIOD, 0.1, "FULL", true,
                com.dukascopy.api.Filter.NO_FILTER,
                sequence,
                new ParamDrony.EntryConfig(0, 0, 0, 4),
                new ParamDrony.PinzaConfig(20, 0, 90, 40, 0, 10, true, 0, 10),
                new ParamDrony.TradingWindow(LocalTime.of(0, 0), LocalTime.of(23, 59), false),
                shadow,
                new ParamDrony.MacroPL(false, 30, 200),
                colorStory,
                new ParamDrony.ClusterConfig("", 0, 1),
                new ParamDrony.BreakEvenConfig(0, 0),
                new ParamDrony.EdgeConfig(false, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
    }

    static ParamDrony defaultParam() {
        return param(defaultSequence(), defaultShadow(), defaultColorStory());
    }

    /** Init per i test eseguiti su ogni barra (senza storico). */
    static BarTestInit init(IBar bar, ParamDrony param, DirectionEnum direction) {
        return new BarTestInit(bar, param, INSTRUMENT, direction);
    }

    /** Init per i test di contesto: storico finto con le backwardBars date. */
    static BarTestInit initWithHistory(IBar bar, IBar slopeBar, List<IBar> backwardBars,
            ParamDrony param, DirectionEnum direction) throws JFException {
        IHistory history = (IHistory) Proxy.newProxyInstance(
                IHistory.class.getClassLoader(),
                new Class<?>[]{IHistory.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getBars")) {
                        return backwardBars;
                    }
                    throw new UnsupportedOperationException(
                            "IHistory finto: metodo non previsto " + method.getName());
                });
        return new BarTestInit(bar, param, INSTRUMENT, direction, history, PERIOD, slopeBar);
    }
}
