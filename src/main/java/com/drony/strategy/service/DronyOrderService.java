package com.drony.strategy.service;

import com.drony.strategy.DelegateDrony;
import com.drony.strategy.data.BarTestResultLog;
import com.drony.strategy.data.DirectionEnum;
import com.drony.strategy.data.DronyOrder;
import com.drony.strategy.data.ParamDrony;
import com.drony.strategy.utility.TimeUtility;
import com.drony.strategy.utility.Utility;
import com.drony.utility.data.U;
import com.dukascopy.api.IBar;
import com.dukascopy.api.IConsole;
import com.dukascopy.api.IContext;
import com.dukascopy.api.IEngine;
import com.dukascopy.api.IOrder;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.JFException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

public class DronyOrderService {

  private static final DateFormat ORDER_DATE_FORMAT = new SimpleDateFormat("ddMMyy_HHmmss");

  private static final int RANDOM_LABEL_MAX = 10_000;

  private final ParamDrony paramDrony;

  private final IConsole console;
  private final IEngine engine;

  private final DelegateDrony delegateDrony;
  private final String identifier;
  private final Map<String, DronyOrder> orders;
  private final boolean outputVerbose;

  public DronyOrderService(IContext context, ParamDrony paramDrony, DelegateDrony delegateDrony,
      String identifier,
      Map<String, DronyOrder> orders, boolean outputVerbose) {

    this.console = context.getConsole();
    this.engine = context.getEngine();

    this.paramDrony = paramDrony;
    this.delegateDrony = delegateDrony;
    this.identifier = identifier;
    this.orders = orders;
    this.outputVerbose = outputVerbose;
  }

  public boolean createOrder(IBar bar, IBar prevBar, DirectionEnum direction,
      Instrument instrument, List<BarTestResultLog> logs) {

    double mod = Math.abs(bar.getClose() - prevBar.getOpen());

    long gtt =
        bar.getTime() + this.paramDrony.getNumCandlesValid() * this.paramDrony.getSelectedPeriod()
            .getInterval();

    double stopLoss, takeProfit, stopLostDelta, takeProfitDelta, price;

    double identFixed = Utility.fromPipToPrice(this.paramDrony.getIndent(), instrument);

    if (direction.equals(DirectionEnum.BUY)) {
      price = bar.getClose() + (Math.max(
          ((this.paramDrony.getIndentPercentPennachio() / 100) * (bar.getHigh() - bar.getClose())),
          0)
          + identFixed);
      price = Utility.roundByDefaultPrecision(price, instrument);
    } else {
      price = bar.getClose() - (Math.max(
          ((this.paramDrony.getIndentPercentPennachio() / 100) * (bar.getClose() - bar.getLow())), 0)
          + identFixed);
      price = Utility.roundByDefaultPrecision(price, instrument);
    }

    /* tp e sl at time zero */

    stopLostDelta = this.paramDrony.getFloorAbs() + ((this.paramDrony.getFloor_perc() / 100) * mod);
    takeProfitDelta = this.paramDrony.getCapAbs() + ((this.paramDrony.getCap_perc() / 100) * mod);

    if (direction.equals(DirectionEnum.BUY)) {
      stopLoss = Utility.roundByDefaultPrecision(price - stopLostDelta, instrument);
      takeProfit = Utility.roundByDefaultPrecision(price + takeProfitDelta, instrument);
    } else {
      stopLoss = Utility.roundByDefaultPrecision(price + stopLostDelta, instrument);
      takeProfit = Utility.roundByDefaultPrecision(price - takeProfitDelta, instrument);
    }

    // TODO: implement body shadow filter
    try {
      IOrder order;
      String orderLabel = getLabel(gtt);
      String comment;
      IEngine.OrderCommand oreOrderCommand;

      if (direction.equals(DirectionEnum.BUY)) {
        orderLabel += "_B";
        comment = "BUY";
        oreOrderCommand = IEngine.OrderCommand.BUYSTOP;
      } else {
        orderLabel += "_S";
        comment = "SELL";
        oreOrderCommand = IEngine.OrderCommand.SELLSTOP;
      }

      com.drony.strategy.data.DronyOrder dronyOrder = new com.drony.strategy.data.DronyOrder(
          takeProfitDelta, stopLostDelta, price, direction, logs,
          this.paramDrony.getPercentDeltaTakeProfitUpdateStopLoss(),
          this.paramDrony.getPercentDeltaTakeProfitAddToStartPrice(),
          instrument,
          this.outputVerbose); // TODO ASK ARDUINO come primo prezzo di riferimento prendo il prezzo di chiusura delle barre +/- ident

      if (delegateDrony.testAndAddOrderToCluster(this.paramDrony.getOrderCluster(), orderLabel,
          this.paramDrony.getOrderClusterPriority(), dronyOrder)) {

        order = this.engine
            .submitOrder(orderLabel, instrument, oreOrderCommand, this.paramDrony.getOrderSize(),
                price,
                this.paramDrony.getSlippage(), stopLoss, takeProfit, gtt, comment);

        this.delegateDrony.getEdgeOrderService()
            .setInfoOrder(orderLabel,
                this.paramDrony.getSelectedInstrument(),
                U.toBigDecimal(bar.getClose()),
                // BigDecimal stopLoss, BigDecimal mod, BigDecimal barClose, BigDecimal barOpen, BigDecimal barHigh, BigDecimal barLow, EdgeOrderParam edgeOrderParam
                U.toBigDecimal(stopLoss),
                U.toBigDecimal(mod),
                U.toBigDecimal(bar.getClose()),
                U.toBigDecimal(bar.getOpen()),
                U.toBigDecimal(bar.getHigh()),
                U.toBigDecimal(bar.getLow()),
                this.paramDrony);

      } else {
        this.console.getOut().println(
            " CLuster " + this.paramDrony.getOrderCluster() + " is full, not order submit");
        return false;
      }

      if (order.getState() == IOrder.State.CREATED) {
        this.orders.put(orderLabel, dronyOrder);

        dronyOrder.getWriterExcel().writeHistoricalBar();
        dronyOrder.getWriterExcel().writeOrder("CREATE ORDER " + direction + " ", order, bar);

        this.console.getOut().println(orderLabel);
        return true;
      }

    } catch (JFException e) {
      this.console.getErr().println(Utility.customFormat(instrument, "Error %s", e.toString()));
      return false;
    }

    return false;
  }

  private String getLabel(long time) {
    return this.identifier + "_" + DronyOrderService.ORDER_DATE_FORMAT.format(time)
        + Utility.generateRandom(RANDOM_LABEL_MAX)
        + Utility.generateRandom(RANDOM_LABEL_MAX);
  }

  public void closeAllOrderOpenAfterTime(Long time) {
    try {
      if (!TimeUtility.checkTradingTimeLimit(this.paramDrony, time)) {

        for (IOrder order : this.engine.getOrders()) {
          if (order.getLabel().startsWith(this.identifier)) {
            if (order.getState() != IOrder.State.FILLED
                && order.getState() != IOrder.State.CANCELED
                && order.getState() != IOrder.State.CLOSED) {
              DronyOrder dronyOrder = orders.get(order.getLabel());
              dronyOrder.setMotivationToClose(" BY ENDING TIME LIMIT");
              order.close();
            }
          }
        }
      }

      if (this.paramDrony.isActiveFreeWeekEnd()) {
        ZonedDateTime date = Instant.ofEpochMilli(time).atZone(ZoneId.of("UTC"));
        if ((date.getDayOfWeek().equals(DayOfWeek.FRIDAY) && date.getHour() >= 18)
            || date.getDayOfWeek().equals(DayOfWeek.SATURDAY) || date.getDayOfWeek()
            .equals(DayOfWeek.SUNDAY)) {
          for (IOrder order : this.engine.getOrders()) {
            if (order.getLabel().startsWith(this.identifier)) {
              if (order.getState() == IOrder.State.FILLED
                  || order.getState() == IOrder.State.OPENED
                  || order.getState() == IOrder.State.CREATED) {
                DronyOrder dronyOrder = orders.get(order.getLabel());
                dronyOrder.setMotivationToClose(" BY FREE WEEKEND");
                order.close();
              }
            }
          }
        }
      }
    } catch (JFException e) {
      this.console.getErr().println(e.getMessage());
      this.console.getErr().println(e.toString());
    }
  }
}
