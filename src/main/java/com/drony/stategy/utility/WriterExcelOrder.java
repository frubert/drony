package com.drony.stategy.utility;

import com.drony.stategy.data.BarTestResultLog;
import com.drony.stategy.data.DronyOrder;
import com.drony.stategy.test.data.BarTestResult;
import com.dukascopy.api.IBar;
import com.dukascopy.api.IMessage;
import com.dukascopy.api.IOrder;
import com.dukascopy.api.Instrument;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

public class WriterExcelOrder extends WriteExcel{

    private final DronyOrder dronyOrder;

    public WriterExcelOrder(DronyOrder dronyOrder, Instrument instrument, boolean verboseOutPut) {
        super(instrument, verboseOutPut);
        this.dronyOrder = dronyOrder;
        this.instrument = instrument;
        this.verboseOutPut = verboseOutPut;
    }

    public static List<String> getHeader() {
        List<String> row = new ArrayList<>();

        row.add("BAR NUMBER");
        row.add("TIME");
        row.add("OPEN PRICE");
        row.add("CLOSE PRICE");
        /*row.add("HIGH PRICE");
        row.add("LOW PRICE");*/

        row.add("ORDER STATUS");
        /*row.add("LABEL");*/
        row.add("LAST PRICE");
        row.add("TAKE PROFIT");
        row.add("DELTA TAKE PROFIT ORIGINAL");
        row.add("STOP LOSS");
        row.add("DELTA STOP LOSS ORIGINAL");
        row.add("BAR PRICE");
        row.add("NUM BAR");
        row.add("NEW TAKE PROFIT");
        row.add("NEW STOP LOSS");

        return row;
    }

    public void writeHistoricalBar() {
        this.writeHistoricalBar(this.dronyOrder.getBarLogs());
    }



    private void addSpace(List<String> list, int spaceNum){
        for (int i=0; i< spaceNum; i++){
            list.add("");
        }
    }

    public void writeOrder(String action, IOrder order, IBar bar) {
        writeOrder(action, order, null, null, null, null, bar);
    }

    public void writeOrder(String action, IOrder order, Double price, Integer numBars, Double newTP, Double newSL, IBar bar) {

        List<String> row = new ArrayList<>();

        writeBar(row, "", bar);

        row.add(action);
        //row.add(order.getLabel());
        /*if (price == null) {
            row.add(order.getLabel());
        } else {
            row.add("");
        }*/

        row.add(priceFormat.format(dronyOrder.getLastPrice()));
        row.add(fromPriceToPip(order.getTakeProfitPrice()));
        row.add(fromPriceToPip(dronyOrder.getTakeProfitDelta()));
        row.add(fromPriceToPip(order.getStopLossPrice()));
        row.add(fromPriceToPip(dronyOrder.getStopLossDelta()));

        if (price != null) {
            row.add(priceFormat.format(price));
            row.add(numBars.toString());
            row.add((newTP != null ? fromPriceToPip(newTP) : ""));
            row.add((newSL != null ? fromPriceToPip(newSL) : ""));
        }

        matrix.add(row);
    }

    public void writeFooter() {
        List<String> row = new ArrayList<>();

        addSpace(row, 4);

        row.add("ORDER CLOSE " + dronyOrder.getMotivationToClose());
        row.add("NUM BAR: " + dronyOrder.getNumBar());
        row.add("D.D.: " + fromPriceToPip(dronyOrder.getModOrder()));

        matrix.add(row);
    }

    public List<List<String>> getMatrix() {
        return matrix;
    }


    public void writeMessage(IMessage message) {

        List<String> row = new ArrayList<>();

        row.add("MESSAGE");
        row.add(dateFormat.format(new Date(message.getCreationTime())));
        row.add(message.getType().toString());
        row.add(message.getReasons().stream().map(r -> r.toString()).collect(Collectors.joining(" | ")));
        row.add(message.getContent());

        matrix.add(row);
    }

}
