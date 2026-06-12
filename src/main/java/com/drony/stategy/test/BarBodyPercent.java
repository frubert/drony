package com.drony.stategy.test;

import com.drony.stategy.BarTestInit;
import com.drony.stategy.test.data.BarTestResult;
import com.drony.utility.data.U;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class BarBodyPercent extends AbstractBarTest {

    final BigDecimal bodyPercMin;
    final BigDecimal bodyPercMax;

    public BarBodyPercent(BarTestInit barTestInit) {
        super(barTestInit);
        /* TODO Questi valori è possibile pre calcolarli */
        bodyPercMin = U.toBigDecimal(barTestInit.getParam().getBody_perc_min());
        bodyPercMax = U.toBigDecimal(barTestInit.getParam().getBody_perc_max());
    }

    @Override
    protected String getTitleTest() {
        return "BODY PERCENT";
    }

    @Override
    protected BarTestResult testBar(BarTestInit barTestInit) {
        BigDecimal bodyPercent = U.toBigDecimal(barTestInit.getBody())
                .divide(U.toBigDecimal(barTestInit.getTotalBarSize()), barTestInit.getInstrument().getPipScale(), RoundingMode.HALF_EVEN);
        bodyPercent = bodyPercent.multiply(U.toBigDecimal("100"));

        if (bodyPercent.compareTo(bodyPercMin) <= 0 || bodyPercent.compareTo(bodyPercMax) >= 0) {

            return this.barTestInit.getParam()
                    .getFail(this.customFormat(
                            this.barTestInit.getInstrument(),
                            "Aborting for BarBodyPercent not matching. Val: %pipscale% Min: %pipscale% Max: %pipscale%",
                            bodyPercent,
                            bodyPercMin,
                            bodyPercMax));
        }

        return this.barTestInit.getParam().getOk(getTitleTest());
    }
}
