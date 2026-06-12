package com.drony.strategy.data;

import com.drony.strategy.edge.data.EdgeOrderParam;
import com.drony.strategy.test.data.BarTestResult;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.Period;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * Parametri di una singola strategia (una colonna del file Excel), immutabili
 * dopo la lettura e raggruppati per area funzionale nei record annidati.
 *
 * I getter piatti storici sono mantenuti come delega: il resto del codice non
 * dipende dalla struttura interna.
 */
public class ParamDrony implements EdgeOrderParam {

    /** Filtri applicati alla sequenza di barre: corpo, volatilità (mod), pendenza. */
    public record SequenceFilter(
        int n,
        double bodyPercMin, double bodyPercMax,
        double modMin, double modMax,
        double bodyAbsMin, double bodyAbsMax,
        double slopeMin, double slopeMax) {
    }

    /** Parametri di ingresso ordine: indent sul prezzo, slippage, validità in candele. */
    public record EntryConfig(
        double indent,
        double indentPercentPennachio,
        double slippage,
        int numCandlesValid) {
    }

    /** Pinza: SL/TP iniziali (cap/floor) e loro attenuazione dinamica. */
    public record PinzaConfig(
        double capAbs, double capPerc, double capAttn,
        double floorAbs, double floorPerc, double floorAttn,
        boolean attivaMonotona,
        int waitNBarPinza,
        int orderNumMaxBar) {
    }

    /** Finestra oraria di trading e chiusura per il weekend. */
    public record TradingWindow(
        LocalTime startTradingTime,
        LocalTime endTradingTime,
        boolean activeFreeWeekEnd) {
    }

    /** Filtri sulle ombre del corpo barra, su barre passate e future. */
    public record ShadowFilter(
        int numBodyShadowBars,
        double minBodyShadowPercentage,
        double minBodyShadow,
        double minFutureBodyShadowPercentage,
        double minFutureBodyShadow) {
    }

    /** Chiusura a soglia di profitto/perdita controllata a ogni tick. */
    public record MacroPL(boolean active, double profit, double loss) {
    }

    /** Filtro "color story": troppi colori uguali nelle ultime barre. */
    public record ColorStory(int numBars, double sameBars) {
    }

    /** Appartenenza a un cluster di ordini con priorità e tetto. */
    public record ClusterConfig(String name, int priority, int maxOrder) {
    }

    /** Spostamento dello SL sul prezzo di apertura al raggiungimento di una quota di TP. */
    public record BreakEvenConfig(
        double percentDeltaTakeProfitUpdateStopLoss,
        double percentDeltaTakeProfitAddToStartPrice) {
    }

    /** Configurazione dell'ordine di copertura (edge). */
    public record EdgeConfig(
        boolean active,
        BigDecimal orderSize,
        BigDecimal ident,
        BigDecimal indentPercentPennachio,
        BigDecimal indentPercentMod,
        BigDecimal stopLoss,
        BigDecimal takeProfit,
        BigDecimal percentStopLossIdent) {
    }

    private final String name;
    private final Instrument selectedInstrument;
    private final Period selectedPeriod;
    private final double orderSize;
    private final String strategyType;
    private final boolean preventMultipleOrders;

    private final SequenceFilter sequenceFilter;
    private final EntryConfig entry;
    private final PinzaConfig pinza;
    private final TradingWindow tradingWindow;
    private final ShadowFilter shadowFilter;
    private final MacroPL macroPL;
    private final ColorStory colorStory;
    private final ClusterConfig cluster;
    private final BreakEvenConfig breakEven;
    private final EdgeConfig edge;

    public ParamDrony(String name, Instrument selectedInstrument, Period selectedPeriod,
        double orderSize, String strategyType, boolean preventMultipleOrders,
        SequenceFilter sequenceFilter, EntryConfig entry, PinzaConfig pinza,
        TradingWindow tradingWindow, ShadowFilter shadowFilter, MacroPL macroPL,
        ColorStory colorStory, ClusterConfig cluster, BreakEvenConfig breakEven, EdgeConfig edge) {

        this.name = name;
        this.selectedInstrument = selectedInstrument;
        this.selectedPeriod = selectedPeriod;
        this.orderSize = orderSize;
        this.strategyType = strategyType;
        this.preventMultipleOrders = preventMultipleOrders;
        this.sequenceFilter = sequenceFilter;
        this.entry = entry;
        this.pinza = pinza;
        this.tradingWindow = tradingWindow;
        this.shadowFilter = shadowFilter;
        this.macroPL = macroPL;
        this.colorStory = colorStory;
        this.cluster = cluster;
        this.breakEven = breakEven;
        this.edge = edge;
    }

    public BarTestResult getFail(String text) {
        return new BarTestResult(text, false);
    }

    public BarTestResult getOk(String text) {
        return new BarTestResult(text, true);
    }

    /* ----- identità strategia ----- */

    public String getName() {
        return name;
    }

    public Instrument getSelectedInstrument() {
        return selectedInstrument;
    }

    public Period getSelectedPeriod() {
        return selectedPeriod;
    }

    public double getOrderSize() {
        return orderSize;
    }

    public String getStrategyType() {
        return strategyType;
    }

    public boolean isPreventMultipleOrders() {
        return preventMultipleOrders;
    }

    /* ----- filtri sequenza ----- */

    public int getN() {
        return sequenceFilter.n();
    }

    public double getBody_perc_min() {
        return sequenceFilter.bodyPercMin();
    }

    public double getBody_perc_max() {
        return sequenceFilter.bodyPercMax();
    }

    public double getMod_min() {
        return sequenceFilter.modMin();
    }

    public double getMod_max() {
        return sequenceFilter.modMax();
    }

    public double getBody_abs_min() {
        return sequenceFilter.bodyAbsMin();
    }

    public double getBody_abs_max() {
        return sequenceFilter.bodyAbsMax();
    }

    public double getSlope_min() {
        return sequenceFilter.slopeMin();
    }

    public double getSlope_max() {
        return sequenceFilter.slopeMax();
    }

    /** Slope minima espressa in prezzo (pips * pip value dello strumento). */
    public double getSlopeMin() {
        return sequenceFilter.slopeMin() * selectedInstrument.getPipValue();
    }

    /** Slope massima espressa in prezzo (pips * pip value dello strumento). */
    public double getSlopeMax() {
        return sequenceFilter.slopeMax() * selectedInstrument.getPipValue();
    }

    /* ----- ingresso ordine ----- */

    public double getIndent() {
        return entry.indent();
    }

    public double getIndentPercentPennachio() {
        return entry.indentPercentPennachio();
    }

    public double getSlippage() {
        return entry.slippage();
    }

    public int getNumCandlesValid() {
        return entry.numCandlesValid();
    }

    /* ----- pinza SL/TP ----- */

    public double getCap_perc() {
        return pinza.capPerc();
    }

    public double getCap_attn() {
        return pinza.capAttn();
    }

    public double getFloor_perc() {
        return pinza.floorPerc();
    }

    public double getFloor_attn() {
        return pinza.floorAttn();
    }

    /** Cap (take profit fisso) espresso in prezzo. */
    public double getCapAbs() {
        return pinza.capAbs() * selectedInstrument.getPipValue();
    }

    /** Floor (stop loss fisso) espresso in prezzo. */
    public double getFloorAbs() {
        return pinza.floorAbs() * selectedInstrument.getPipValue();
    }

    public boolean isAttivaMonotona() {
        return pinza.attivaMonotona();
    }

    public int getWaitNBarPinza() {
        return pinza.waitNBarPinza();
    }

    public int getOrderNumMaxBar() {
        return pinza.orderNumMaxBar();
    }

    /* ----- finestra di trading ----- */

    public LocalTime getStartTradingTime() {
        return tradingWindow.startTradingTime();
    }

    public LocalTime getEndTradingTime() {
        return tradingWindow.endTradingTime();
    }

    public boolean isActiveFreeWeekEnd() {
        return tradingWindow.activeFreeWeekEnd();
    }

    /* ----- filtri shadow ----- */

    public int getNumBodyShadowBars() {
        return shadowFilter.numBodyShadowBars();
    }

    public double getMinBodyShadowPercentage() {
        return shadowFilter.minBodyShadowPercentage();
    }

    public double getMinBodyShadow() {
        return shadowFilter.minBodyShadow();
    }

    public double getMinFutureBodyShadowPercentage() {
        return shadowFilter.minFutureBodyShadowPercentage();
    }

    public double getMinFutureBodyShadow() {
        return shadowFilter.minFutureBodyShadow();
    }

    /* ----- macro PL ----- */

    public boolean isMacroPL() {
        return macroPL.active();
    }

    public double getMacroPLProfit() {
        return macroPL.profit();
    }

    public double getMacroPLLoss() {
        return macroPL.loss();
    }

    /* ----- color story ----- */

    public int getNumColorStoryBars() {
        return colorStory.numBars();
    }

    public double getColorStorySameBars() {
        return colorStory.sameBars();
    }

    /* ----- cluster ----- */

    public String getOrderCluster() {
        return cluster.name();
    }

    public int getOrderClusterPriority() {
        return cluster.priority();
    }

    public int getMaxOrderByCluster() {
        return cluster.maxOrder();
    }

    /* ----- break even ----- */

    public double getPercentDeltaTakeProfitUpdateStopLoss() {
        return breakEven.percentDeltaTakeProfitUpdateStopLoss();
    }

    public double getPercentDeltaTakeProfitAddToStartPrice() {
        return breakEven.percentDeltaTakeProfitAddToStartPrice();
    }

    /* ----- edge order (interfaccia EdgeOrderParam) ----- */

    @Override
    public boolean isActiveEdgeOrder() {
        return edge.active();
    }

    @Override
    public BigDecimal getOrderSizeEdgeOrder() {
        return edge.orderSize();
    }

    @Override
    public BigDecimal getIdentEdgeOrder() {
        return edge.ident();
    }

    @Override
    public BigDecimal getIndentPercentPennachioEdgeOrder() {
        return edge.indentPercentPennachio();
    }

    @Override
    public BigDecimal getIndentPercentModEdgeOrder() {
        return edge.indentPercentMod();
    }

    @Override
    public BigDecimal getStopLossEdgeOrder() {
        return edge.stopLoss();
    }

    @Override
    public BigDecimal getTakeProfitEdgeOrder() {
        return edge.takeProfit();
    }

    @Override
    public BigDecimal getPercentStopLossIdent() {
        return edge.percentStopLossIdent();
    }

    @Override
    public String toString() {
        return "ParamDrony{" +
            "name='" + name + '\'' +
            ", instrument=" + selectedInstrument +
            ", period=" + selectedPeriod +
            ", orderSize=" + orderSize +
            ", strategyType='" + strategyType + '\'' +
            ", preventMultipleOrders=" + preventMultipleOrders +
            ", " + sequenceFilter +
            ", " + entry +
            ", " + pinza +
            ", " + tradingWindow +
            ", " + shadowFilter +
            ", " + macroPL +
            ", " + colorStory +
            ", " + cluster +
            ", " + breakEven +
            ", " + edge +
            '}';
    }
}
