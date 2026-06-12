package com.drony.strategy;

import com.drony.strategy.service.ActiveOrderRegistry;
import com.drony.strategy.service.DronyOrderService;
import com.drony.strategy.service.StopLossTakeProfitService;
import com.drony.strategy.test.*;
import com.drony.strategy.test.data.BarTestResult;
import com.drony.strategy.utility.BarUtility;
import com.drony.strategy.utility.TimeUtility;
import com.drony.strategy.utility.Utility;
import com.drony.strategy.data.*;
import com.drony.strategy.utility.WriteExcel;
import com.drony.utility.data.Pair;
import com.drony.utility.data.U;
import com.dukascopy.api.*;
import com.dukascopy.api.Period;

import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.*;
import java.util.*;
import org.apache.commons.lang3.RandomStringUtils;

public class DronyStrategy implements StrategyInterface {

  private ParamDrony paramDrony;

  private IEngine engine;
  private IConsole console;
  private IHistory history;
  private IContext context;
  private IIndicators indicators;
  private IUserInterface userInterface;
  private String identifier;

  private boolean outputVerbose;

  private final Map<String, DronyOrder> orders = new HashMap<>();
  private final List<DronyLogBar> dronyLogBars = new ArrayList<>();
  private final ActiveOrderRegistry orderRegistry = new ActiveOrderRegistry();


  private static final String STRATEGY = "DRONY_42";

  private DateFormat dateFormat;
  private DecimalFormat priceFormat;

  private DelegateDrony delegateDrony;

  private DronyOrderService dronyOrderService;
  private StopLossTakeProfitService stopLossTakeProfitService;

  public DronyStrategy(ParamDrony paramDrony, Boolean outputVerbose, DelegateDrony delegateDrony,
      int index) {

    this.outputVerbose = outputVerbose;
    this.paramDrony = paramDrony;
    this.delegateDrony = delegateDrony;
    this.identifier =
        U.cleanNameForDCOrder(RandomStringUtils.randomAlphabetic(5) + "_" + paramDrony.getName())
            + "_" + index + "_" + STRATEGY + "_";
  }

  @Override
  public void onStart(IContext context) {

    this.engine = context.getEngine();
    this.console = context.getConsole();
    this.history = context.getHistory();
    this.context = context;
    this.indicators = context.getIndicators();
    this.userInterface = context.getUserInterface();

    dateFormat = new SimpleDateFormat("yyyy.MM.dd,HH:mm");
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

    priceFormat = new DecimalFormat("0.#####");

    this.console.getInfo().println("Start Engine with:" + this.paramDrony.toString());

    this.dronyOrderService =
        new DronyOrderService(context, this.paramDrony, this.delegateDrony, this.identifier,
            this.orders, this.orderRegistry, this.outputVerbose);

    this.stopLossTakeProfitService = new StopLossTakeProfitService(context, this.paramDrony,
        this.orders, this.orderRegistry);
  }

  @Override
  public void onMessage(IMessage message) {

    IOrder order = message.getOrder();
    if (order == null) {
      return;
    }

    DronyOrder dronyOrder = null;
    if (order.getLabel().startsWith(this.identifier)) {
      this.orderRegistry.onOrderMessage(order);
      dronyOrder = this.orders.get(order.getLabel());
    }

    if (dronyOrder == null) {
      return;
    }
    dronyOrder.getWriterExcel().writeMessage(message);
    console.getOut().println(paramDrony.getName() + " " + message + " " + order.getLabel());

    if (message.getType() == IMessage.Type.ORDER_CLOSE_OK) {
      if (message.getReasons().contains(IMessage.Reason.ORDER_CLOSED_BY_SL)) {
        dronyOrder.setMotivationToClose(" BY STOP LOSS");
      } else if (message.getReasons().contains(IMessage.Reason.ORDER_CLOSED_BY_TP)) {
        dronyOrder.setMotivationToClose(" BY TAKE PROFIT");
      }
    } else if (message.getType() == IMessage.Type.ORDER_FILL_OK) {
      this.delegateDrony.closeOtherOrder(
          this.paramDrony.getOrderCluster(),
          order.getLabel(),
          this.paramDrony.getOrderClusterPriority(),
          this.orders.get(order.getLabel()));
    }
  }

  public void onAccount(IAccount account) throws JFException {
  }

  @Override
  public void onStop() throws JFException {

  }

  public Pair<String, List<DronyData>> onStopData() throws JFException, IOException {

    for (DronyOrder dronyOrder : orders.values()) {
      dronyOrder.getWriterExcel().writeFooter();
    }

    console.getOut().println("Stopped " + this.paramDrony.getName());

    List<DronyData> list = new ArrayList<>();
    list.addAll(orders.values());
    list.addAll(this.dronyLogBars);

    return new Pair<>(paramDrony.getName(), list);
  }

  @Override
  public void onTick(Instrument instrument, ITick tick) throws JFException {

    if (!instrument.equals(this.paramDrony.getSelectedInstrument())) {
      return;
    }

    this.dronyOrderService.closeAllOrderOpenAfterTime(tick.getTime());

    for (IOrder order : this.orderRegistry.liveOrders()) {

      if (order.getState() == IOrder.State.FILLED) {

        if (paramDrony.isMacroPL()) {
          if (order.getProfitLossInPips() > paramDrony.getMacroPLProfit()) {
            order.close();
          }
          if (order.getProfitLossInPips() < (paramDrony.getMacroPLLoss() * -1)) {
            order.close();
          }
        }

        DronyOrder dronyorder = this.orders.get(order.getLabel());

        if (dronyorder != null && order.getProfitLossInPips() >= dronyorder
            .getDeltaTakeProfitUpdateStopLossInPips()) {
          if (dronyorder.getDirection().equals(DirectionEnum.BUY)) {
            order.setStopLossPrice(dronyorder.getOpenPrice()
                + dronyorder.getPercentDeltaTakeProfitAddToStartPriceInPrice());
          } else {
            order.setStopLossPrice(dronyorder.getOpenPrice()
                - dronyorder.getPercentDeltaTakeProfitAddToStartPriceInPrice());
          }
          order.waitForUpdate(10000);
        }
      }
    }
  }

  @Override
  public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar)
      throws JFException {

    if (!instrument.equals(paramDrony.getSelectedInstrument())
        || !period.equals(paramDrony.getSelectedPeriod())) {
      return;
    }

    String dateStr = WriteExcel.dateFormat.format(new Date(askBar.getTime()));

    console.getOut().println("START BAR ------------------------ " + dateStr);

    this.stopLossTakeProfitService.updateStopLossAndTakeProfitByBar(askBar, bidBar, instrument);

    /* Check Trading Time */
    if (!TimeUtility.checkTradingTimeLimit(this.paramDrony, bidBar)) {
      return;
    }

    int buyOrders = this.orderRegistry.countBuyOrders();
    int sellOrders = this.orderRegistry.countSellOrders();

    /* Prevent Multiple orders */
    if (paramDrony.isPreventMultipleOrders() && (sellOrders > 0 || buyOrders > 0)) {
      return;
    }

    long currentBarTime = bidBar.getTime();

    if (BarUtility.getBarColor(bidBar) == DirectionEnum.SELL && sellOrders == 0) {
      if (paramDrony.getStrategyType().equals(StrategyTypeEnum.FULL)
          || paramDrony.getStrategyType().equals(StrategyTypeEnum.SHORT)) {

        final List<IBar> sellBars = history
            .getBars(instrument, period, OfferSide.BID, Filter.NO_FILTER,
                Math.max(paramDrony.getN(), 2), currentBarTime, 0);
        roboStrategyBar(instrument, period, sellBars, DirectionEnum.SELL);
      }
    }

    if (BarUtility.getBarColor(bidBar) == DirectionEnum.BUY && buyOrders == 0) {
      if (paramDrony.getStrategyType().equals(StrategyTypeEnum.FULL)
          || paramDrony.getStrategyType().equals(StrategyTypeEnum.LONG)) {

        final List<IBar> buyBars = history
            .getBars(instrument, period, OfferSide.ASK, Filter.NO_FILTER,
                Math.max(paramDrony.getN(), 2), currentBarTime, 0);
        roboStrategyBar(instrument, period, buyBars, DirectionEnum.BUY);
      }
    }

    console.getOut().println("END BAR ------------------------ " + dateStr);
  }

  private boolean roboStrategyBar(Instrument instrument, Period period, List<IBar> historyBars,
      DirectionEnum direction) throws JFException {

    IBar bar = historyBars.get(historyBars.size() - 1);
    IBar slopeBar = historyBars.get(0);

    List<BarTestResultLog> logs = new ArrayList<>();

    for (IBar barCurrent : historyBars) {

      BarTestInit init = new BarTestInit(barCurrent, this.paramDrony, instrument, direction);

      List<AbstractBarTest> testForAllBar = new ArrayList<>();
      testForAllBar.add(new BarDirection(init));
      testForAllBar.add(new BarBodyAbs(init));
      testForAllBar.add(new BarBodyPercent(init));

      Pair<Boolean, List<BarTestResult>> result = runTest(testForAllBar);

      logs.add(new BarTestResultLog(barCurrent, result));

      if (!result.getFirst()) {
        if (this.outputVerbose) {
          this.dronyLogBars.add(new DronyLogBar(logs, instrument, this.outputVerbose));
        }
        return false;
      }
    }

    BarTestInit init = new BarTestInit(bar, paramDrony, instrument, direction, this.history, period, slopeBar);

    List<AbstractBarTest> testForAllBar = new ArrayList<>();
    testForAllBar.add(new BarSame(init));
    testForAllBar.add(new BarColorStory(init));
    testForAllBar.add(new BarSlope(init));
    testForAllBar.add(new BarMod(init));

    Pair<Boolean, List<BarTestResult>> result = runTest(testForAllBar);

    logs.get(logs.size() - 1).update(result);

    if (!result.getFirst()) {
      if (this.outputVerbose) {
        this.dronyLogBars.add(new DronyLogBar(logs, instrument, this.outputVerbose));
      }
      return false;
    }

    IBar prevBar = historyBars.get(historyBars.size() - (Math.min(historyBars.size(), paramDrony.getN())));
    return this.dronyOrderService.createOrder(bar, prevBar, direction, instrument, logs);
  }

  private Pair<Boolean, List<BarTestResult>> runTest(List<AbstractBarTest> testForAllBar) {

    List<BarTestResult> testResults = new ArrayList<>();

    for (AbstractBarTest test : testForAllBar) {
      BarTestResult result = test.testBar();
      testResults.add(result);
      if (!result.isResult()) {
        return new Pair<>(false, testResults);
      }
    }
    return new Pair<>(true, testResults);
  }
}

