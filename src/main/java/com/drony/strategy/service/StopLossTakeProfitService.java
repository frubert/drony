package com.drony.strategy.service;

import com.drony.strategy.data.DirectionEnum;
import com.drony.strategy.data.DronyOrder;
import com.drony.strategy.data.ParamDrony;
import com.drony.strategy.utility.BarUtility;
import com.drony.strategy.utility.Utility;
import com.dukascopy.api.IBar;
import com.dukascopy.api.IConsole;
import com.dukascopy.api.IContext;
import com.dukascopy.api.IEngine;
import com.dukascopy.api.IOrder;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.JFException;
import java.util.Map;

public class StopLossTakeProfitService {

  private final ParamDrony paramDrony;

  private final IConsole console;
  private final IEngine engine;

  private final String identifier;
  private final Map<String, DronyOrder> orders;

  public StopLossTakeProfitService(IContext context, ParamDrony paramDrony, String identifier,
      Map<String, DronyOrder> orders) {
    this.paramDrony = paramDrony;

    this.console = context.getConsole();
    this.engine = context.getEngine();

    this.identifier = identifier;
    this.orders = orders;
  }

  public void updateStopLossAndTakeProfitByBar(IBar askBar, IBar bidBar, Instrument instrument)
      throws JFException {

    for (IOrder order : this.engine.getOrders()) {

      if (order.getLabel().startsWith(this.identifier)
          && order.getState() == IOrder.State.FILLED) {

        DronyOrder dronyOrder = this.orders.get(order.getLabel());
        dronyOrder.addBars(askBar, bidBar);

        double price, priceBar;
        int numBars = dronyOrder.getNumBar();
        boolean needUpdate;
        Double newTP = null;
        Double newSL = null;
        Double deltaNewTP = null;
        Double deltaNewSL = null;

        if (numBars <= this.paramDrony.getWaitNBarPinza()) {
          continue;
        }

        if (numBars >= this.paramDrony.getOrderNumMaxBar()) {
          dronyOrder.setMotivationToClose(" BY BAR NUMBER EXCESSIVE");
          order.close();
          continue;
        }

        if (order.getOrderCommand().isLong()) { // LONG

          priceBar = askBar.getClose();
          //numBars = this.calculateBars(askBar.getTime(), order.getFillTime(), this.selectedPeriod);
          needUpdate = dronyOrder.getLastPrice() < priceBar;

        } else { //if (order.getOrderCommand().isShort()) { // SHORT
          priceBar = bidBar.getClose();
          //numBars = this.calculateBars(bidBar.getTime(), order.getFillTime(), this.selectedPeriod);
          needUpdate = dronyOrder.getLastPrice() > priceBar;
        }

        if (this.paramDrony.isAttivaMonotona()) {
          price = needUpdate ? priceBar : dronyOrder.getLastPrice();
        } else {
          price = priceBar;
        }

        if (this.paramDrony.getCap_attn() != 0) {
          deltaNewTP = dronyOrder.getTakeProfitDelta() * (this.paramDrony.getCap_attn() / 100);
          if (order.getOrderCommand().isLong()) {
            newTP = price + deltaNewTP;
          } else {
            newTP = price - deltaNewTP;
          }
        }

        if (this.paramDrony.getFloor_attn() != 0) {
          deltaNewSL = dronyOrder.getStopLossDelta() * (this.paramDrony.getFloor_attn() / 100);
          if (order.getOrderCommand().isLong()) {
            newSL = price - deltaNewSL;
          } else {
            newSL = price + deltaNewSL;
          }
        }

        if (newTP != null) {
          order.setTakeProfitPrice(newTP);
          dronyOrder.setTakeProfitDelta(deltaNewTP);
        }

        if (newSL != null) {
          order.setStopLossPrice(newSL);
          dronyOrder.setStopLossDelta(deltaNewSL);
        }

        if (newSL != null || newTP != null) {
          order.waitForUpdate(10000);
        }

        if (needUpdate) {
          dronyOrder.setLastPrice(priceBar);
        }

        if (this.paramDrony.getFloor_attn() != 0 || this.paramDrony.getCap_attn() != 0) {

          String status = order.getOrderCommand().isLong() ? "LONG UPDATE " : "SHORT UPDATE ";
          status += needUpdate ? "MODIFY BASE PRICE" : "";

          dronyOrder.getWriterExcel().writeOrder(status, order, price, numBars, newTP, newSL,
              order.getOrderCommand().isLong() ? askBar : bidBar);
        }

      } else if (order.getLabel().startsWith(this.identifier)
          && order.getState() != IOrder.State.CANCELED
          && order.getState() != IOrder.State.CLOSED
          && order.getState() != IOrder.State.FILLED) {

        DronyOrder dronyOrder = this.orders.get(order.getLabel());

        int numBars = dronyOrder.getNumBar();
        double minBodyShadowPercentage = this.paramDrony.getMinFutureBodyShadowPercentage();
        double minBodyByPrice = Utility
            .fromPipToPrice(this.paramDrony.getMinFutureBodyShadow(), instrument);

        if (numBars < this.paramDrony.getNumBodyShadowBars()) {
          IBar bar;
          if (dronyOrder.getDirection() == DirectionEnum.BUY) {
            bar = askBar;
          } else {
            bar = bidBar;
          }
          if (this.paramDrony.getMinBodyShadowPercentage() != 0) {
            double currentBodyShadow = BarUtility.getBody(bar) / BarUtility.getTotalBarSize(bar);
            if (currentBodyShadow < minBodyShadowPercentage / 100) {
              dronyOrder.setMotivationToClose(" BY FUTURE SHADOW PERCENTAGE");
              order.close();
            }
          }

          if (this.paramDrony.getMinFutureBodyShadow() != 0) {
            if (BarUtility.getBody(bar) < minBodyByPrice) {
              dronyOrder.setMotivationToClose(" BY FUTURE SHADOW");
              order.close();
            }
          }
        }
      }
    }
  }
}
