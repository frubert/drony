package com.drony.strategy;

import com.drony.strategy.service.ActiveOrderRegistry;
import com.drony.strategy.service.DronyOrderService;
import com.drony.strategy.service.StopLossTakeProfitService;
import com.drony.strategy.utility.DecisionLogger;
import com.drony.strategy.utility.DecisionLogger.Outcome;
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
  private final StrategyStats stats;


  private static final String STRATEGY = "DRONY_42";

  private DateFormat dateFormat;
  private DecimalFormat priceFormat;

  private DelegateDrony delegateDrony;

  private DronyOrderService dronyOrderService;
  private StopLossTakeProfitService stopLossTakeProfitService;
  private DecisionLogger decisions = DecisionLogger.DISABLED;

  public DronyStrategy(ParamDrony paramDrony, Boolean outputVerbose, DelegateDrony delegateDrony,
      int index) {

    this.outputVerbose = outputVerbose;
    this.paramDrony = paramDrony;
    this.delegateDrony = delegateDrony;
    this.identifier =
        U.cleanNameForDCOrder(RandomStringUtils.randomAlphabetic(5) + "_" + paramDrony.getName())
            + "_" + index + "_" + STRATEGY + "_";
    this.stats = new StrategyStats(paramDrony.getName(),
        paramDrony.getSelectedInstrument().name(), paramDrony.getSelectedPeriod().name());
  }

  public StrategyStats getStats() {
    return stats;
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

    this.decisions = this.delegateDrony.getDecisionLogger();
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
      this.stats.onOrderClosed(order.getProfitLossInPips(), order.getProfitLossInUSD());
      this.decisions.log(message.getCreationTime(), paramDrony.getName(),
          dronyOrder.getDirection().name(), Outcome.CHIUSURA,
          order.getLabel() + " P&L " + order.getProfitLossInPips() + " pips, motivo:"
              + (dronyOrder.getMotivationToClose() == null ? " " + message.getReasons()
              : dronyOrder.getMotivationToClose()));
    } else if (message.getType() == IMessage.Type.ORDER_FILL_OK) {
      this.decisions.log(message.getCreationTime(), paramDrony.getName(),
          dronyOrder.getDirection().name(), Outcome.FILL,
          order.getLabel() + " @ " + order.getOpenPrice());
      this.delegateDrony.getClusterManager().onOrderFilled(
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

    long currentBarTime = bidBar.getTime();

    /* Check Trading Time */
    if (!TimeUtility.checkTradingTimeLimit(this.paramDrony, bidBar)) {
      decisions.log(currentBarTime, paramDrony.getName(), null, Outcome.BLOCCATO,
          "fuori orario trading (" + paramDrony.getStartTradingTime()
              + " - " + paramDrony.getEndTradingTime() + ")");
      return;
    }

    int buyOrders = this.orderRegistry.countBuyOrders();
    int sellOrders = this.orderRegistry.countSellOrders();

    /* Prevent Multiple orders */
    if (paramDrony.isPreventMultipleOrders() && (sellOrders > 0 || buyOrders > 0)) {
      decisions.log(currentBarTime, paramDrony.getName(), null, Outcome.BLOCCATO,
          "ordine già attivo (buy=" + buyOrders + " sell=" + sellOrders
              + "), preventMultipleOrders");
      return;
    }

    DirectionEnum barColor = BarUtility.getBarColor(bidBar);

    if (barColor == DirectionEnum.DOJI) {
      decisions.log(currentBarTime, paramDrony.getName(), null, Outcome.SCARTATO,
          "barra doji, nessuna direzione");
      return;
    }

    if (barColor == DirectionEnum.SELL && sellOrders == 0) {
      if (paramDrony.getStrategyType().equals(StrategyTypeEnum.FULL)
          || paramDrony.getStrategyType().equals(StrategyTypeEnum.SHORT)) {

        final List<IBar> sellBars = history
            .getBars(instrument, period, OfferSide.BID, paramDrony.getCandleFilter(),
                Math.max(paramDrony.getN(), 2), currentBarTime, 0);
        roboStrategyBar(instrument, period, sellBars, DirectionEnum.SELL);
      } else {
        decisions.log(currentBarTime, paramDrony.getName(), DirectionEnum.SELL.name(),
            Outcome.BLOCCATO, "strategyType " + paramDrony.getStrategyType() + " esclude SELL");
      }
    }

    if (barColor == DirectionEnum.BUY && buyOrders == 0) {
      if (paramDrony.getStrategyType().equals(StrategyTypeEnum.FULL)
          || paramDrony.getStrategyType().equals(StrategyTypeEnum.LONG)) {

        final List<IBar> buyBars = history
            .getBars(instrument, period, OfferSide.ASK, paramDrony.getCandleFilter(),
                Math.max(paramDrony.getN(), 2), currentBarTime, 0);
        roboStrategyBar(instrument, period, buyBars, DirectionEnum.BUY);
      } else {
        decisions.log(currentBarTime, paramDrony.getName(), DirectionEnum.BUY.name(),
            Outcome.BLOCCATO, "strategyType " + paramDrony.getStrategyType() + " esclude BUY");
      }
    }

    console.getOut().println("END BAR ------------------------ " + dateStr);
  }

  /**
   * Valuta il setup su una sequenza di barre: prima i test per-barra su tutta
   * la sequenza, poi i test di contesto sulla barra corrente; se tutto passa
   * delega la creazione dell'ordine.
   */
  private boolean roboStrategyBar(Instrument instrument, Period period, List<IBar> historyBars,
      DirectionEnum direction) throws JFException {

    List<BarTestResultLog> logs = new ArrayList<>();

    if (!validateEachBar(historyBars, instrument, direction, logs)) {
      return failAndLog(logs, instrument, direction);
    }

    if (!validateCurrentBar(historyBars, instrument, period, direction, logs)) {
      return failAndLog(logs, instrument, direction);
    }

    IBar bar = historyBars.get(historyBars.size() - 1);
    IBar prevBar = historyBars.get(historyBars.size() - (Math.min(historyBars.size(), paramDrony.getN())));
    return this.dronyOrderService.createOrder(bar, prevBar, direction, instrument, logs);
  }

  /** Test eseguiti su ogni barra della sequenza: direzione, corpo assoluto, corpo percentuale. */
  private boolean validateEachBar(List<IBar> historyBars, Instrument instrument,
      DirectionEnum direction, List<BarTestResultLog> logs) {

    for (IBar barCurrent : historyBars) {

      BarTestInit init = new BarTestInit(barCurrent, this.paramDrony, instrument, direction);

      Pair<Boolean, List<BarTestResult>> result = runTests(List.of(
          new BarDirection(init),
          new BarBodyAbs(init),
          new BarBodyPercent(init)));

      logs.add(new BarTestResultLog(barCurrent, result));

      if (!result.getFirst()) {
        return false;
      }
    }
    return true;
  }

  /** Test di contesto eseguiti solo sull'ultima barra: shadow, storia colori, pendenza, volatilità. */
  private boolean validateCurrentBar(List<IBar> historyBars, Instrument instrument, Period period,
      DirectionEnum direction, List<BarTestResultLog> logs) throws JFException {

    IBar bar = historyBars.get(historyBars.size() - 1);
    IBar slopeBar = historyBars.get(0);

    BarTestInit init =
        new BarTestInit(bar, paramDrony, instrument, direction, this.history, period, slopeBar);

    Pair<Boolean, List<BarTestResult>> result = runTests(List.of(
        new BarSame(init),
        new BarColorStory(init),
        new BarSlope(init),
        new BarMod(init)));

    logs.get(logs.size() - 1).update(result);

    return result.getFirst();
  }

  private boolean failAndLog(List<BarTestResultLog> logs, Instrument instrument,
      DirectionEnum direction) {
    if (this.outputVerbose) {
      this.dronyLogBars.add(new DronyLogBar(logs, instrument, this.outputVerbose));
    }

    BarTestResultLog lastLog = logs.get(logs.size() - 1);
    String detail = lastLog.getTestResults().stream()
        .filter(r -> !r.isResult())
        .map(BarTestResult::getMessage)
        .findFirst()
        .orElse("filtro non superato");
    decisions.log(lastLog.getBar().getTime(), paramDrony.getName(), direction.name(),
        Outcome.SCARTATO, detail);

    return false;
  }

  private Pair<Boolean, List<BarTestResult>> runTests(List<AbstractBarTest> tests) {

    List<BarTestResult> testResults = new ArrayList<>();

    for (AbstractBarTest test : tests) {
      BarTestResult result = test.testBar();
      testResults.add(result);
      if (!result.isResult()) {
        return new Pair<>(false, testResults);
      }
    }
    return new Pair<>(true, testResults);
  }
}

