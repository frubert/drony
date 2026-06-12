package com.drony.stategy.data;

import com.drony.stategy.edge.data.EdgeOrderParam;
import com.drony.stategy.test.data.BarTestResult;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.Period;

import java.math.BigDecimal;
import java.time.LocalTime;

public class ParamDrony implements EdgeOrderParam {

    public BarTestResult getFail(String text) {
        return new BarTestResult(text, false);
    }

    public BarTestResult getOk(String text) {
        return new BarTestResult(text, true);
    }

    private String name;

    //@Configurable("selectedInstrument:")
    private Instrument selectedInstrument = Instrument.EURUSD;

    //@Configurable("selectedPeriod:")
    private Period selectedPeriod = Period.ONE_HOUR;

    //@Configurable("OrderSize:")
    private double orderSize = 0.2;
    //N=2 , K1=10% e X1=1.5  G1=1  G2= 2  G3=20% G4=40%

    //@Configurable("N Bars:")
    private int n = 2;

    //@Configurable("Body % Min:")
    private double body_perc_min = 10;

    //@Configurable("Body % Max:")
    private double body_perc_max = 95;

    //@Configurable("Mod Min :")
    private double mod_min = 10;

    //@Configurable("Mod Max :")
    private double mod_max = 100;

    //@Configurable("Body abs Min:")
    private double body_abs_min = 0.5;

    //@Configurable("Body abs Max :")
    private double body_abs_max = 50;

    //@Configurable("Indent:")
    private double indent = 0;

    private double indentPercetPennachio = 0;

    //@Configurable("Cap Abs:")
    private double cap_abs = 20;  // valore fisso su tp

    //@Configurable("Cap %:")
    private double cap_perc = 0; //Valore % su MOD del valore di cap mobile

    //@Configurable("Cap attn:")
    private double cap_attn = 90; //Valore % di attenuazione/accrescimento su valore corrente di TP    mobile  +/-%

    //@Configurable("Floor abs:")
    private double floor_abs = 40; //Valore fisso sl

    //@Configurable("Floor %:")
    private double floor_perc = 0; //Valore % su MOD del valore di floor mobile

    //@Configurable("Floor attn:")
    private double floor_attn = 10; //Valore % di attenuazione/accrescimento su valore corrente di SL mobile  +/-%

    //@Configurable("Slope Min:")
    private double slope_min = 1; //Filtro Slope, indicatore di pendenza : delta fra  baricentro SEQ1 e SEQ2 (SEQn) >punti/barra

    //@Configurable("Slope Max:")
    private double slope_max = 20; //Filtro Slope, indicatore di pendenza : delta fra  baricentro SEQ1 e SEQ2 (SEQn) >punti/barra

    //@Configurable("Slippage:")
    private double slippage = 0; //Slippage

    //@Configurable("startTradingTime:")
    private LocalTime startTradingTime = LocalTime.of(7,00);   // "09:00" OR "09:00 AM"

    //@Configurable("endTradingTime:")
    private LocalTime endTradingTime = LocalTime.of(17,30);   // "19:00" OR "07:00 PM"

    //@Configurable("numCandlesValid;")
    private int numCandlesValid = 4;   // default valid

    //@Configurable(value = "strategyType", options = {StrategyTypeEnum.FULL, StrategyTypeEnum.LONG, StrategyTypeEnum.SHORT})
//, options = { StrategyTypeEnum.FULL,StrategyTypeEnum.LONG,StrategyTypeEnum.SHORT})
    private String strategyType = StrategyTypeEnum.FULL;

    //@Configurable("numBodyShadowBars:")
    private int numBodyShadowBars = 1;

    //@Configurable("minBodyShadowPercentage:")
    private double minBodyShadowPercentage = 20;

    private double minBodyShadow = 1000000;

    private double minFutureBodyShadowPercentage = 0;

    private double minFutureBodyShadow = 0;

    //@Configurable("macroPL")
    private boolean macroPL = false;

    //@Configurable("macroPLProfit")
    private double macroPLProfit = 30;

    //@Configurable("macroPLLoss")
    private double macroPLLoss = 200;

    //@Configurable("numColorStoryBars")
    private int numColorStoryBars = 7;

    //@Configurable("colorStorySameBars")
    private double colorStorySameBars = 7;

    //@Configurable("Pinza monotona de/cresente")
    private boolean attivaMonotona = true;

    //@Configurable("Num max barre per ordine")
    private int orderNumMaxBar = 10;

    //@Configurable("preventMultipleOrders")
    private boolean preventMultipleOrders = true;

    private int waitNBarPinza = 1;

    private String orderCluster;

    private int orderClusterPriority;

    private boolean activeFreeWeekEnd; /* No trade after 18:00 after FRIDAY */

    private double percentDeltaTakeProfitUpdateStopLoss;

    private double percentDeltaTakeProfitAddToStartPrice;

    private int maxOrderByCluster;

    /* ----- EDGE ORDER ------ */

    private boolean activeEdgeOrder;

    private BigDecimal orderSizeEdgeOrder;

    private BigDecimal identEdgeOrder;

    private BigDecimal indentPercentPennachioEdgeOrder;

    private BigDecimal indentPercentModEdgeOrder;

    private BigDecimal stopLossEdgeOrder;

    private BigDecimal takeProfitEdgeOrder;

    private BigDecimal percentStopLossIdent;

    /* ----- EDGE ORDER ------ */


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Instrument getSelectedInstrument() {
        return selectedInstrument;
    }

    public void setSelectedInstrument(Instrument selectedInstrument) {
        this.selectedInstrument = selectedInstrument;
    }

    public Period getSelectedPeriod() {
        return selectedPeriod;
    }

    public void setSelectedPeriod(Period selectedPeriod) {
        this.selectedPeriod = selectedPeriod;
    }

    public double getOrderSize() {
        return orderSize;
    }

    public void setOrderSize(double orderSize) {
        this.orderSize = orderSize;
    }

    public int getN() {
        return n;
    }

    public void setN(int n) {
        this.n = n;
    }

    public double getBody_perc_min() {
        return body_perc_min;
    }

    public void setBody_perc_min(double body_perc_min) {
        this.body_perc_min = body_perc_min;
    }

    public double getBody_perc_max() {
        return body_perc_max;
    }

    public void setBody_perc_max(double body_perc_max) {
        this.body_perc_max = body_perc_max;
    }

    public double getMod_min() {
        return mod_min;
    }

    public void setMod_min(double mod_min) {
        this.mod_min = mod_min;
    }

    public double getMod_max() {
        return mod_max;
    }

    public void setMod_max(double mod_max) {
        this.mod_max = mod_max;
    }

    public double getBody_abs_min() {
        return body_abs_min;
    }

    public void setBody_abs_min(double body_abs_min) {
        this.body_abs_min = body_abs_min;
    }

    public double getBody_abs_max() {
        return body_abs_max;
    }

    public void setBody_abs_max(double body_abs_max) {
        this.body_abs_max = body_abs_max;
    }

    public void setIndent(double indent) {
        this.indent = indent;
    }

    public double getCap_abs() {
        return cap_abs;
    }

    public void setCap_abs(double cap_abs) {
        this.cap_abs = cap_abs;
    }

    public double getCap_perc() {
        return cap_perc;
    }

    public void setCap_perc(double cap_perc) {
        this.cap_perc = cap_perc;
    }

    public double getCap_attn() {
        return cap_attn;
    }

    public void setCap_attn(double cap_attn) {
        this.cap_attn = cap_attn;
    }

    public double getFloor_abs() {
        return floor_abs;
    }

    public void setFloor_abs(double floor_abs) {
        this.floor_abs = floor_abs;
    }

    public double getFloor_perc() {
        return floor_perc;
    }

    public void setFloor_perc(double floor_perc) {
        this.floor_perc = floor_perc;
    }

    public double getFloor_attn() {
        return floor_attn;
    }

    public void setFloor_attn(double floor_attn) {
        this.floor_attn = floor_attn;
    }

    public double getSlope_min() {
        return slope_min;
    }

    public void setSlope_min(double slope_min) {
        this.slope_min = slope_min;
    }

    public double getSlope_max() {
        return slope_max;
    }

    public void setSlope_max(double slope_max) {
        this.slope_max = slope_max;
    }

    public double getSlippage() {
        return slippage;
    }

    public void setSlippage(double slippage) {
        this.slippage = slippage;
    }

    public LocalTime getStartTradingTime() {
        return startTradingTime;
    }

    public void setStartTradingTime(LocalTime startTradingTime) {
        this.startTradingTime = startTradingTime;
    }

    public LocalTime getEndTradingTime() {
        return endTradingTime;
    }

    public void setEndTradingTime(LocalTime endTradingTime) {
        this.endTradingTime = endTradingTime;
    }

    public int getNumCandlesValid() {
        return numCandlesValid;
    }

    public void setNumCandlesValid(int numCandlesValid) {
        this.numCandlesValid = numCandlesValid;
    }

    public String getStrategyType() {
        return strategyType;
    }

    public void setStrategyType(String strategyType) {
        this.strategyType = strategyType;
    }

    public int getNumBodyShadowBars() {
        return numBodyShadowBars;
    }

    public void setNumBodyShadowBars(int numBodyShadowBars) {
        this.numBodyShadowBars = numBodyShadowBars;
    }

    public double getMinBodyShadowPercentage() {
        return minBodyShadowPercentage;
    }

    public void setMinBodyShadowPercentage(double minBodyShadowPercentage) {
        this.minBodyShadowPercentage = minBodyShadowPercentage;
    }

    public boolean isMacroPL() {
        return macroPL;
    }

    public void setMacroPL(boolean macroPL) {
        this.macroPL = macroPL;
    }

    public double getMacroPLProfit() {
        return macroPLProfit;
    }

    public void setMacroPLProfit(double macroPLProfit) {
        this.macroPLProfit = macroPLProfit;
    }

    public double getMacroPLLoss() {
        return macroPLLoss;
    }

    public void setMacroPLLoss(double macroPLLoss) {
        this.macroPLLoss = macroPLLoss;
    }

    public int getNumColorStoryBars() {
        return numColorStoryBars;
    }

    public void setNumColorStoryBars(int numColorStoryBars) {
        this.numColorStoryBars = numColorStoryBars;
    }

    public double getColorStorySameBars() {
        return colorStorySameBars;
    }

    public void setColorStorySameBars(double colorStorySameBars) {
        this.colorStorySameBars = colorStorySameBars;
    }

    public boolean isAttivaMonotona() {
        return attivaMonotona;
    }

    public void setAttivaMonotona(boolean attivaMonotona) {
        this.attivaMonotona = attivaMonotona;
    }

    public int getOrderNumMaxBar() {
        return orderNumMaxBar;
    }

    public void setOrderNumMaxBar(int orderNumMaxBar) {
        this.orderNumMaxBar = orderNumMaxBar;
    }

    public boolean isPreventMultipleOrders() {
        return preventMultipleOrders;
    }

    public void setPreventMultipleOrders(boolean preventMultipleOrders) {
        this.preventMultipleOrders = preventMultipleOrders;
    }

    public double getSlopeMin() {
        return this.slope_min * selectedInstrument.getPipValue();
    }

    public double getSlopeMax() {
        return this.slope_max * selectedInstrument.getPipValue();
    }

    public double getIndent() {
        return this.indent;
    }

    public double getCapAbs() {
        return this.cap_abs * selectedInstrument.getPipValue();
    }

    public double getFloorAbs() {
        return this.floor_abs * selectedInstrument.getPipValue();
    }

    public double getMinBodyShadow() {
        return minBodyShadow;
    }

    public void setMinBodyShadow(double minBodyShadow) {
        this.minBodyShadow = minBodyShadow;
    }

    public double getMinFutureBodyShadowPercentage() {
        return minFutureBodyShadowPercentage;
    }

    public void setMinFutureBodyShadowPercentage(double minFutureBodyShadowPercentage) {
        this.minFutureBodyShadowPercentage = minFutureBodyShadowPercentage;
    }

    public double getMinFutureBodyShadow() {
        return minFutureBodyShadow;
    }

    public void setMinFutureBodyShadow(double minFutureBodyShadow) {
        this.minFutureBodyShadow = minFutureBodyShadow;
    }

    public int getWaitNBarPinza() {
        return waitNBarPinza;
    }

    public void setWaitNBarPinza(int waitNBarPinza) {
        this.waitNBarPinza = waitNBarPinza;
    }

    public double getIndentPercetPennachio() {
        return indentPercetPennachio;
    }

    public void setIndentPercetPennachio(double indentPercetPennachio) {
        this.indentPercetPennachio = indentPercetPennachio;
    }

    public String getOrderCluster() {
        return orderCluster;
    }

    public void setOrderCluster(String orderCluster) {
        this.orderCluster = orderCluster;
    }

    public int getOrderClusterPriority() {
        return orderClusterPriority;
    }

    public void setOrderClusterPriority(int orderClusterPriority) {
        this.orderClusterPriority = orderClusterPriority;
    }

    public boolean isActiveFreeWeekEnd() {
        return activeFreeWeekEnd;
    }

    public void setActiveFreeWeekEnd(boolean activeFreeWeekEnd) {
        this.activeFreeWeekEnd = activeFreeWeekEnd;
    }

    public double getPercentDeltaTakeProfitUpdateStopLoss() {
        return percentDeltaTakeProfitUpdateStopLoss;
    }

    public void setPercentDeltaTakeProfitUpdateStopLoss(
        double percentDeltaTakeProfitUpdateStopLoss) {
        this.percentDeltaTakeProfitUpdateStopLoss = percentDeltaTakeProfitUpdateStopLoss;
    }

    public double getPercentDeltaTakeProfitAddToStartPrice() {
        return percentDeltaTakeProfitAddToStartPrice;
    }

    public void setPercentDeltaTakeProfitAddToStartPrice(double percentDeltaTakeProfitAddToStartPrice) {
        this.percentDeltaTakeProfitAddToStartPrice = percentDeltaTakeProfitAddToStartPrice;
    }

    public int getMaxOrderByCluster() {
        return maxOrderByCluster;
    }

    public void setMaxOrderByCluster(int maxOrderByCluster) {
        this.maxOrderByCluster = maxOrderByCluster;
    }

    public boolean isActiveEdgeOrder() {
        return activeEdgeOrder;
    }

    public void setActiveEdgeOrder(boolean activeEdgeOrder) {
        this.activeEdgeOrder = activeEdgeOrder;
    }

    public BigDecimal getOrderSizeEdgeOrder() {
        return orderSizeEdgeOrder;
    }

    public void setOrderSizeEdgeOrder(BigDecimal orderSizeEdgeOrder) {
        this.orderSizeEdgeOrder = orderSizeEdgeOrder;
    }

    public BigDecimal getIdentEdgeOrder() {
        return identEdgeOrder;
    }

    public void setIdentEdgeOrder(BigDecimal identEdgeOrder) {
        this.identEdgeOrder = identEdgeOrder;
    }

    public BigDecimal getIndentPercentPennachioEdgeOrder() {
        return indentPercentPennachioEdgeOrder;
    }

    public void setIndentPercentPennachioEdgeOrder(BigDecimal indentPercentPennachioEdgeOrder) {
        this.indentPercentPennachioEdgeOrder = indentPercentPennachioEdgeOrder;
    }

    public BigDecimal getIndentPercentModEdgeOrder() {
        return indentPercentModEdgeOrder;
    }

    public void setIndentPercentModEdgeOrder(BigDecimal indentPercentModEdgeOrder) {
        this.indentPercentModEdgeOrder = indentPercentModEdgeOrder;
    }

    public BigDecimal getStopLossEdgeOrder() {
        return stopLossEdgeOrder;
    }

    public void setStopLossEdgeOrder(BigDecimal stopLossEdgeOrder) {
        this.stopLossEdgeOrder = stopLossEdgeOrder;
    }

    public BigDecimal getTakeProfitEdgeOrder() {
        return takeProfitEdgeOrder;
    }

    public void setTakeProfitEdgeOrder(BigDecimal takeProfitEdgeOrder) {
        this.takeProfitEdgeOrder = takeProfitEdgeOrder;
    }

    public BigDecimal getPercentStopLossIdent() {
        return percentStopLossIdent;
    }

    public void setPercentStopLossIdent(BigDecimal percentStopLossIdent) {
        this.percentStopLossIdent = percentStopLossIdent;
    }

    @Override
    public String toString() {
        return "ParamDrony{" +
            "name='" + name + '\'' +
            ", selectedInstrument=" + selectedInstrument +
            ", selectedPeriod=" + selectedPeriod +
            ", orderSize=" + orderSize +
            ", n=" + n +
            ", body_perc_min=" + body_perc_min +
            ", body_perc_max=" + body_perc_max +
            ", mod_min=" + mod_min +
            ", mod_max=" + mod_max +
            ", body_abs_min=" + body_abs_min +
            ", body_abs_max=" + body_abs_max +
            ", indent=" + indent +
            ", indentPercetPennachio=" + indentPercetPennachio +
            ", cap_abs=" + cap_abs +
            ", cap_perc=" + cap_perc +
            ", cap_attn=" + cap_attn +
            ", floor_abs=" + floor_abs +
            ", floor_perc=" + floor_perc +
            ", floor_attn=" + floor_attn +
            ", slope_min=" + slope_min +
            ", slope_max=" + slope_max +
            ", slippage=" + slippage +
            ", startTradingTime=" + startTradingTime +
            ", endTradingTime=" + endTradingTime +
            ", numCandlesValid=" + numCandlesValid +
            ", strategyType='" + strategyType + '\'' +
            ", numBodyShadowBars=" + numBodyShadowBars +
            ", minBodyShadowPercentage=" + minBodyShadowPercentage +
            ", minBodyShadow=" + minBodyShadow +
            ", minFutureBodyShadowPercentage=" + minFutureBodyShadowPercentage +
            ", minFutureBodyShadow=" + minFutureBodyShadow +
            ", macroPL=" + macroPL +
            ", macroPLProfit=" + macroPLProfit +
            ", macroPLLoss=" + macroPLLoss +
            ", numColorStoryBars=" + numColorStoryBars +
            ", colorStorySameBars=" + colorStorySameBars +
            ", attivaMonotona=" + attivaMonotona +
            ", orderNumMaxBar=" + orderNumMaxBar +
            ", preventMultipleOrders=" + preventMultipleOrders +
            ", waitNBarPinza=" + waitNBarPinza +
            ", orderCluster='" + orderCluster + '\'' +
            ", orderClusterPriority=" + orderClusterPriority +
            ", activeFreeWeekEnd=" + activeFreeWeekEnd +
            ", percentDeltaTakeProfitUpdateStopLoss=" + percentDeltaTakeProfitUpdateStopLoss +
            ", percentDeltaTakeProfitAddToStartPrice=" + percentDeltaTakeProfitAddToStartPrice +
            ", maxOrderByCluster=" + maxOrderByCluster +
            ", activeEdgeOrder=" + activeEdgeOrder +
            ", orderSizeEdgeOrder=" + orderSizeEdgeOrder +
            ", identEdgeOrder=" + identEdgeOrder +
            ", indentPercentPennachioEdgeOrder=" + indentPercentPennachioEdgeOrder +
            ", indentPercentModEdgeOrder=" + indentPercentModEdgeOrder +
            ", stopLossEdgeOrder=" + stopLossEdgeOrder +
            ", takeProfitEdgeOrder=" + takeProfitEdgeOrder +
            ", percentStopLossIdent=" + percentStopLossIdent +
            '}';
    }
}
