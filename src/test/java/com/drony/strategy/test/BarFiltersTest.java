package com.drony.strategy.test;

import static com.drony.strategy.test.TestSupport.bar;
import static com.drony.strategy.test.TestSupport.defaultColorStory;
import static com.drony.strategy.test.TestSupport.defaultParam;
import static com.drony.strategy.test.TestSupport.defaultSequence;
import static com.drony.strategy.test.TestSupport.defaultShadow;
import static com.drony.strategy.test.TestSupport.init;
import static com.drony.strategy.test.TestSupport.initWithHistory;
import static com.drony.strategy.test.TestSupport.param;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.drony.strategy.data.DirectionEnum;
import com.drony.strategy.data.ParamDrony;
import com.dukascopy.api.IBar;
import java.util.List;
import org.junit.Test;

/**
 * Comportamento dei 7 filtri su barre sintetiche (EURUSD: 1 pip = 0.0001).
 * Documentazione eseguibile delle regole descritte anche in docs/NOTE-COMPORTAMENTO.md.
 */
public class BarFiltersTest {

    /* ---------- BarDirection: colore barra == direzione cercata ---------- */

    @Test
    public void direction_barRialzista_passaPerBuy_falliscePerSell() {
        IBar buyBar = bar(1.1000, 1.1010, 0.0005);

        assertTrue(new BarDirection(init(buyBar, defaultParam(), DirectionEnum.BUY))
                .testBar().isResult());
        assertFalse(new BarDirection(init(buyBar, defaultParam(), DirectionEnum.SELL))
                .testBar().isResult());
    }

    @Test
    public void direction_doji_fallisceSempre() {
        IBar doji = bar(1.1000, 1.1000, 0.0005);

        assertFalse(new BarDirection(init(doji, defaultParam(), DirectionEnum.BUY))
                .testBar().isResult());
        assertFalse(new BarDirection(init(doji, defaultParam(), DirectionEnum.SELL))
                .testBar().isResult());
    }

    /* ---------- BarBodyAbs: corpo in pips dentro (min, max) esclusi ---------- */

    @Test
    public void bodyAbs_corpoNelRange_passa() {
        IBar tenPipsBody = bar(1.1000, 1.1010, 0.0005); // 10 pips in (0.5, 50)

        assertTrue(new BarBodyAbs(init(tenPipsBody, defaultParam(), DirectionEnum.BUY))
                .testBar().isResult());
    }

    @Test
    public void bodyAbs_corpoTroppoPiccoloOTroppoGrande_fallisce() {
        IBar tiny = bar(1.10000, 1.10002, 0.0005);  // 0.2 pips <= min 0.5
        IBar huge = bar(1.1000, 1.1060, 0.0005);    // 60 pips >= max 50

        assertFalse(new BarBodyAbs(init(tiny, defaultParam(), DirectionEnum.BUY))
                .testBar().isResult());
        assertFalse(new BarBodyAbs(init(huge, defaultParam(), DirectionEnum.BUY))
                .testBar().isResult());
    }

    /* ---------- BarBodyPercent: corpo % del range high-low dentro (min, max) ---------- */

    @Test
    public void bodyPercent_cinquantaPercento_passa() {
        IBar half = bar(1.1000, 1.1010, 0.0005); // body 10 pips, range 20 pips = 50% in (10, 95)

        assertTrue(new BarBodyPercent(init(half, defaultParam(), DirectionEnum.BUY))
                .testBar().isResult());
    }

    @Test
    public void bodyPercent_corpoTroppoEsiguo_fallisce() {
        IBar sliver = bar(1.10000, 1.10001, 0.00095); // body 0.1 pips, range ~2 pips = 5% <= 10

        assertFalse(new BarBodyPercent(init(sliver, defaultParam(), DirectionEnum.BUY))
                .testBar().isResult());
    }

    /* ---------- BarSlope: |centro(slopeBar) - centro(barra)| dentro (min, max) in prezzo ---------- */

    @Test
    public void slope_dieciPips_passa() throws Exception {
        IBar slopeBar = bar(1.1000, 1.1002, 0.0005); // centro 1.1001
        IBar current = bar(1.1010, 1.1012, 0.0005);  // centro 1.1011, slope 10 pips in (1, 20)

        assertTrue(new BarSlope(initWithHistory(current, slopeBar, List.of(slopeBar, current),
                defaultParam(), DirectionEnum.BUY)).testBar().isResult());
    }

    @Test
    public void slope_nulla_fallisce() throws Exception {
        IBar slopeBar = bar(1.1000, 1.1002, 0.0005);
        IBar current = bar(1.1002, 1.1000, 0.0005); // stesso centro: slope 0 <= min

        assertFalse(new BarSlope(initWithHistory(current, slopeBar, List.of(slopeBar, current),
                defaultParam(), DirectionEnum.BUY)).testBar().isResult());
    }

    /* ---------- BarMod: |open(prima backward bar) - close(corrente)| dentro (min, max) ----------
       NB: prima barra delle backwardBars, non della sequenza N — vedi NOTE-COMPORTAMENTO.md §2 */

    @Test
    public void mod_trentaPips_passa() throws Exception {
        IBar first = bar(1.1000, 1.1010, 0.0005);
        IBar current = bar(1.1020, 1.1030, 0.0005); // mod = |1.1000 - 1.1030| = 30 pips in (10, 100)

        assertTrue(new BarMod(initWithHistory(current, first, List.of(first, current),
                defaultParam(), DirectionEnum.BUY)).testBar().isResult());
    }

    @Test
    public void mod_cinquePipsSottoMinimo_fallisce() throws Exception {
        IBar first = bar(1.1000, 1.1010, 0.0005);
        IBar current = bar(1.1003, 1.1005, 0.0005); // mod = 5 pips <= min 10

        assertFalse(new BarMod(initWithHistory(current, first, List.of(first, current),
                defaultParam(), DirectionEnum.BUY)).testBar().isResult());
    }

    /* ---------- BarSame: le ultime numBodyShadowBars devono avere corpo >= minBodyShadow% ---------- */

    @Test
    public void same_corpiPieni_passa_corpoEsiguo_fallisce() throws Exception {
        ParamDrony param = param(defaultSequence(),
                new ParamDrony.ShadowFilter(2, 20, 0, 0, 0),   // 2 barre, corpo >= 20%
                new ParamDrony.ColorStory(2, 0));              // maxBars = 2, color story spento

        IBar full = bar(1.1000, 1.1010, 0.0005);   // corpo 50%
        IBar slim = bar(1.1000, 1.1001, 0.00045);  // corpo 10% < 20%

        assertTrue(new BarSame(initWithHistory(full, full, List.of(full, full),
                param, DirectionEnum.BUY)).testBar().isResult());
        assertFalse(new BarSame(initWithHistory(full, full, List.of(full, slim),
                param, DirectionEnum.BUY)).testBar().isResult());
    }

    /* ---------- BarColorStory: fallisce se >= sameBars barre dello stesso colore della direzione ---------- */

    @Test
    public void colorStory_treSuTreStessoColore_fallisce_conUnaDiversa_passa() throws Exception {
        ParamDrony param = param(defaultSequence(),
                new ParamDrony.ShadowFilter(1, 0, 0, 0, 0),    // shadow spento
                new ParamDrony.ColorStory(3, 3));              // 3 barre, max 2 dello stesso colore

        IBar buy = bar(1.1000, 1.1010, 0.0005);
        IBar sell = bar(1.1010, 1.1000, 0.0005);

        assertFalse(new BarColorStory(initWithHistory(buy, buy, List.of(buy, buy, buy),
                param, DirectionEnum.BUY)).testBar().isResult());
        assertTrue(new BarColorStory(initWithHistory(buy, buy, List.of(buy, sell, buy),
                param, DirectionEnum.BUY)).testBar().isResult());
    }
}
