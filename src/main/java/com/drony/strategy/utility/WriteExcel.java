package com.drony.strategy.utility;

import com.drony.strategy.data.BarTestResultLog;
import com.drony.strategy.test.data.BarTestResult;
import com.dukascopy.api.IBar;
import com.dukascopy.api.Instrument;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class WriteExcel {

    public static DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd,HH:mm");
    static {
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    protected static DecimalFormat priceFormat = new DecimalFormat("0.#####");

    protected Instrument instrument;
    protected boolean verboseOutPut;
    protected List<List<String>> matrix = new ArrayList<>();


    public WriteExcel(Instrument instrument, boolean verboseOutPut){
        this.instrument = instrument;
        this.verboseOutPut = verboseOutPut;
    }

    protected String fromPriceToPip(double price) {
        return priceFormat.format(Utility.fromPriceToPip(price, this.instrument));
    }

    public void writeHistoricalBar(List<BarTestResultLog> logs) {

        int numBar = 1;
        for (BarTestResultLog log : logs) {

            IBar bar = log.getBar();
            List<String> row = new ArrayList<>();
            writeBar(row, "BAR " + numBar, bar);
            matrix.add(row);

            if (verboseOutPut) {
                for (BarTestResult test : log.getTestResults()) {
                    List<String> rowTest = new ArrayList<>();
                    rowTest.add("");
                    rowTest.add(test.isResult() ? "OK" : "FAIL");
                    rowTest.add(test.getMessage());
                    matrix.add(rowTest);
                }
            }
            numBar++;
        }
    }

    protected void writeBar(List<String> row, String title, IBar bar){
        row.add(title);
        row.add(dateFormat.format(new Date(bar.getTime())));
        row.add(priceFormat.format(bar.getOpen()));
        row.add(priceFormat.format(bar.getClose()));
        /*row.add(priceFormat.format(bar.getHigh()));
        row.add(priceFormat.format(bar.getLow()));*/
    }
}
