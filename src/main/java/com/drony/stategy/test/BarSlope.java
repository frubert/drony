package com.drony.stategy.test;

import com.drony.stategy.BarTestInit;
import com.drony.stategy.test.data.BarTestResult;

public class BarSlope extends AbstractBarTest {

    public BarSlope(BarTestInit barTestInit) {
        super(barTestInit);
    }

    @Override
    protected String getTitleTest() {
        return "SLOPE BAR";
    }

    @Override
    protected BarTestResult testBar(BarTestInit init) {

        double slopeMin = init.getParam().getSlope_min();
        double slopeMax = init.getParam().getSlope_max();

        if (slopeMin != 0 && slopeMax != 0) {
            double center1 = (init.getSlopeBar().getOpen() + init.getSlopeBar().getClose()) / 2;
            double center2 = (init.getBar().getOpen() + init.getBar().getClose()) / 2;
            double slope = Math.abs(center1 - center2);
            if (slope <= init.getParam().getSlopeMin()
                    || slope >= init.getParam().getSlopeMax()) {

                return this.barTestInit.getParam().getFail(
                        this.customFormat(
                                barTestInit.getInstrument(),
                                "Aborting for Slope not matching. Val: %pipscale% Min: %pipscale% Max: %pipscale%",
                                slope,
                                init.getParam().getSlopeMin(),
                                init.getParam().getSlopeMax()));
            }
        }

        return this.barTestInit.getParam().getOk(getTitleTest());
    }

}
