package com.drony.strategy.data;

import com.dukascopy.api.IEngine;
import com.dukascopy.api.IOrder;
import com.dukascopy.api.JFException;

public class OrderCounter {

  int buyOrders = 0;
  int sellOrders = 0;

  public OrderCounter(IEngine engine, String identifier) throws JFException {
    for (IOrder order : engine.getOrders()) {
      if (order.getLabel().startsWith(identifier) && order.getState() != IOrder.State.CANCELED
          && order.getState() != IOrder.State.CLOSED) {
        this.updateCounterOrder(order);
      }
    }
  }

  public int getBuyOrders() {
    return buyOrders;
  }

  public int getSellOrders() {
    return sellOrders;
  }

  public void incSellOrder() {
    this.sellOrders++;
  }

  public void incBuyOrder() {
    this.buyOrders++;
  }

  private void updateCounterOrder(IOrder order) {
    if (order.getOrderCommand().isLong()) {
      incBuyOrder();
    } else if (order.getOrderCommand().isShort()) {
      incSellOrder();
    }
  }
}
