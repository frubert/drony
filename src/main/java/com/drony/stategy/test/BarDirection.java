package com.drony.stategy.test;

import com.drony.stategy.BarTestInit;
import com.drony.stategy.test.data.BarTestResult;
import com.drony.stategy.utility.BarUtility;

public class BarDirection extends AbstractBarTest {

    public BarDirection(BarTestInit barTestInit) {
        super(barTestInit);
    }

    @Override
    protected String getTitleTest() {
        return "BAR DIRECTION";
    }

    @Override
    protected BarTestResult testBar(BarTestInit barTestInit) {
        if (BarUtility.getBarColor(barTestInit.getBar()) != barTestInit.getDirection()) {
            return this.barTestInit.getParam().getFail(String.format("Direction error find %s look %s", barTestInit.getDirection(), BarUtility.getBarColor(barTestInit.getBar())));
        } else return this.barTestInit.getParam().getOk(getTitleTest());
    }
}
