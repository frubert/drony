package com.drony.strategy.data;

import com.drony.strategy.utility.Utility;
import com.drony.strategy.utility.WriterExcelOrder;
import com.drony.utility.data.Pair;
import com.dukascopy.api.IBar;
import com.dukascopy.api.Instrument;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class DronyOrder implements DronyData {

  private final double openPrice;

  private final double deltaTakeProfitUpdateStopLossInPips;

  private final ZonedDateTime createdDate;

  private List<Pair<IBar, IBar>> bars;

  private WriterExcelOrder writerExcel;

  private double takeProfitDelta;

  private double stopLossDelta;

  private double lastPrice;

  private final DirectionEnum direction;

  private String motivationToClose = null;

  private double modOrder = 0;

  private final List<BarTestResultLog> barLogs;

  private final double percentDeltaTakeProfitAddToStartPriceInPrice;

  public DronyOrder(double deltaTakeProfit, double deltaStopLoss, double lastPrice,
      DirectionEnum direction, List<BarTestResultLog> barLogs,
      double percentDeltaTakeProfitUpdateStopLoss,
      double percentDeltaTakeProfitAddToStartPrice,
      Instrument instrument, boolean outputVerbose) {
    this.takeProfitDelta = deltaTakeProfit;
    this.stopLossDelta = deltaStopLoss;
    this.lastPrice = lastPrice;
    this.direction = direction;
    this.barLogs = barLogs;
    this.bars = new ArrayList<>();
    this.writerExcel = new WriterExcelOrder(this, instrument, outputVerbose);
    this.openPrice = lastPrice;
    this.createdDate = ZonedDateTime.now();

    this.deltaTakeProfitUpdateStopLossInPips = percentDeltaTakeProfitUpdateStopLoss == 0 ?
        999999 :
        Utility
            .fromPriceToPip((this.takeProfitDelta * (percentDeltaTakeProfitUpdateStopLoss / 100D)),
                instrument);

    this.percentDeltaTakeProfitAddToStartPriceInPrice =  percentDeltaTakeProfitAddToStartPrice == 0 ?
        0 : (this.takeProfitDelta * (percentDeltaTakeProfitAddToStartPrice / 100D));
  }

  public double getTakeProfitDelta() {
    return takeProfitDelta;
  }

  public double getStopLossDelta() {
    return stopLossDelta;
  }

  public void setTakeProfitDelta(double takeProfitDelta) {
    this.takeProfitDelta = takeProfitDelta;
  }

  public void setStopLossDelta(double stopLossDelta) {
    this.stopLossDelta = stopLossDelta;
  }

  public double getLastPrice() {
    return lastPrice;
  }

  public void setLastPrice(double lastPrice) {
    this.lastPrice = lastPrice;
  }

  public DirectionEnum getDirection() {
    return direction;
  }

  public int getNumBar() {
    return this.bars.size();
  }

  public List<Pair<IBar, IBar>> getBars() {
    return bars;
  }

  public void addBars(IBar askBar, IBar bidBar) {
    this.bars.add(new Pair<>(askBar, bidBar));

    if (direction.equals(DirectionEnum.BUY)) {
      if (openPrice > askBar.getLow()) {
        modOrder = Math.max(Math.abs(openPrice - askBar.getLow()), modOrder);
      }
    } else {
      if (openPrice < bidBar.getHigh()) {
        modOrder = Math.max(Math.abs(openPrice - bidBar.getHigh()), modOrder);
      }
    }
  }

  public WriterExcelOrder getWriterExcel() {
    return writerExcel;
  }

  public void setMotivationToClose(String motivationToClose) {
    if (this.motivationToClose == null) {
      this.motivationToClose = motivationToClose;
    }
  }

  public String getMotivationToClose() {
    return motivationToClose;
  }

  public double getModOrder() {
    return modOrder;
  }

  public ZonedDateTime getCreatedDate() {
    return createdDate;
  }

  public List<BarTestResultLog> getBarLogs() {
    return barLogs;
  }

  public double getDeltaTakeProfitUpdateStopLossInPips() {
    return deltaTakeProfitUpdateStopLossInPips;
  }

  public double getOpenPrice() {
    return openPrice;
  }

  public double getPercentDeltaTakeProfitAddToStartPriceInPrice() {
    return percentDeltaTakeProfitAddToStartPriceInPrice;
  }

  @Override
  public List<List<String>> getMatrix() {
    return this.writerExcel.getMatrix();
  }
}
