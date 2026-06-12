package com.drony.strategy.service;

import com.drony.strategy.data.DirectionEnum;
import com.drony.strategy.data.DronyOrder;
import com.drony.strategy.data.ParamDrony;
import com.drony.strategy.utility.BarUtility;
import com.drony.strategy.utility.Utility;
import com.dukascopy.api.IBar;
import com.dukascopy.api.IConsole;
import com.dukascopy.api.IContext;
import com.dukascopy.api.IOrder;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.JFException;
import java.util.Map;

/**
 * Aggiornamento a ogni barra di stop loss / take profit ("pinza") per gli ordini
 * FILLED e chiusura anticipata degli ordini pendenti la cui barra corrente non
 * rispetta più i filtri di shadow.
 */
public class StopLossTakeProfitService {

  private final ParamDrony paramDrony;

  private final IConsole console;

  private final Map<String, DronyOrder> orders;
  private final ActiveOrderRegistry orderRegistry;

  public StopLossTakeProfitService(IContext context, ParamDrony paramDrony,
      Map<String, DronyOrder> orders, ActiveOrderRegistry orderRegistry) {
    this.paramDrony = paramDrony;

    this.console = context.getConsole();

    this.orders = orders;
    this.orderRegistry = orderRegistry;
  }

  public void updateStopLossAndTakeProfitByBar(IBar askBar, IBar bidBar, Instrument instrument)
      throws JFException {

    for (IOrder order : this.orderRegistry.liveOrders()) {

      DronyOrder dronyOrder = this.orders.get(order.getLabel());
      if (dronyOrder == null) {
        continue;
      }

      if (order.getState() == IOrder.State.FILLED) {
        updateFilledOrder(order, dronyOrder, askBar, bidBar);
      } else {
        /* liveOrders() esclude CLOSED e CANCELED: qui restano CREATED/OPENED/SUBMITTED */
        closePendingOrderIfShadowTooSmall(order, dronyOrder, askBar, bidBar, instrument);
      }
    }
  }

  /**
   * Pinza: dopo waitNBarPinza barre, TP e SL vengono riavvicinati al prezzo corrente
   * con le attenuazioni cap_attn/floor_attn. In modalità monotona il prezzo di
   * riferimento avanza solo se migliora nella direzione dell'ordine.
   */
  private void updateFilledOrder(IOrder order, DronyOrder dronyOrder, IBar askBar, IBar bidBar)
      throws JFException {

    dronyOrder.addBars(askBar, bidBar);

    int numBars = dronyOrder.getNumBar();

    if (numBars <= this.paramDrony.getWaitNBarPinza()) {
      return;
    }

    if (numBars >= this.paramDrony.getOrderNumMaxBar()) {
      dronyOrder.setMotivationToClose(" BY BAR NUMBER EXCESSIVE");
      order.close();
      return;
    }

    boolean isLong = order.getOrderCommand().isLong();
    int sign = isLong ? 1 : -1;

    double priceBar = isLong ? askBar.getClose() : bidBar.getClose();
    boolean needUpdate = isLong
        ? dronyOrder.getLastPrice() < priceBar
        : dronyOrder.getLastPrice() > priceBar;

    double price;
    if (this.paramDrony.isAttivaMonotona()) {
      price = needUpdate ? priceBar : dronyOrder.getLastPrice();
    } else {
      price = priceBar;
    }

    Double newTP = null;
    Double newSL = null;
    Double deltaNewTP = null;
    Double deltaNewSL = null;

    if (this.paramDrony.getCap_attn() != 0) {
      deltaNewTP = dronyOrder.getTakeProfitDelta() * (this.paramDrony.getCap_attn() / 100);
      newTP = price + sign * deltaNewTP;
    }

    if (this.paramDrony.getFloor_attn() != 0) {
      deltaNewSL = dronyOrder.getStopLossDelta() * (this.paramDrony.getFloor_attn() / 100);
      newSL = price - sign * deltaNewSL;
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
      String status = isLong ? "LONG UPDATE " : "SHORT UPDATE ";
      status += needUpdate ? "MODIFY BASE PRICE" : "";

      dronyOrder.getWriterExcel().writeOrder(status, order, price, numBars, newTP, newSL,
          isLong ? askBar : bidBar);
    }
  }

  /**
   * Ordine non ancora fillato: nelle prime numBodyShadowBars barre viene chiuso
   * se il corpo della barra corrente scende sotto le soglie "future shadow".
   */
  private void closePendingOrderIfShadowTooSmall(IOrder order, DronyOrder dronyOrder,
      IBar askBar, IBar bidBar, Instrument instrument) throws JFException {

    int numBars = dronyOrder.getNumBar();

    if (numBars >= this.paramDrony.getNumBodyShadowBars()) {
      return;
    }

    IBar bar = dronyOrder.getDirection() == DirectionEnum.BUY ? askBar : bidBar;

    if (this.paramDrony.getMinBodyShadowPercentage() != 0) {
      double minShadowPercentage = this.paramDrony.getMinFutureBodyShadowPercentage();
      double currentBodyShadow = BarUtility.getBody(bar) / BarUtility.getTotalBarSize(bar);
      if (currentBodyShadow < minShadowPercentage / 100) {
        dronyOrder.setMotivationToClose(" BY FUTURE SHADOW PERCENTAGE");
        order.close();
      }
    }

    if (this.paramDrony.getMinFutureBodyShadow() != 0) {
      double minBodyByPrice = Utility
          .fromPipToPrice(this.paramDrony.getMinFutureBodyShadow(), instrument);
      if (BarUtility.getBody(bar) < minBodyByPrice) {
        dronyOrder.setMotivationToClose(" BY FUTURE SHADOW");
        order.close();
      }
    }
  }
}
