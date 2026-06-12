package com.drony.stategy.data;

import com.drony.stategy.test.data.BarTestResult;
import com.drony.stategy.utility.WriteExcel;
import com.dukascopy.api.IBar;
import com.dukascopy.api.Instrument;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class DronyLogBar extends WriteExcel implements DronyData {

    private ZonedDateTime createDate;


    @Override
    public List<List<String>> getMatrix() {
        return this.matrix;
    }

    @Override
    public ZonedDateTime getCreatedDate() {
        return this.createDate;
    }

    public DronyLogBar(List<BarTestResultLog> logs, Instrument instrument, boolean verboseOutPut) {
        super(instrument, verboseOutPut);
        this.createDate = ZonedDateTime.now();
        this.writeHistoricalBar(logs);
    }
}
