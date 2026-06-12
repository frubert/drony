package com.drony.strategy.test;

import com.drony.strategy.BarTestInit;
import com.drony.strategy.test.data.BarTestResult;
import com.drony.strategy.utility.Utility;

public class BarBodyAbs extends AbstractBarTest {

    final double bodyAbsMin;
    final double bodyAbsMax;

    public BarBodyAbs(BarTestInit barTestInit) {
        super(barTestInit);
        /* TODO Questi valori è possibile pre calcolarli */
        bodyAbsMax = Utility.fromPipToPrice(this.barTestInit.getParam().getBody_abs_max(), barTestInit.getInstrument());
        bodyAbsMin = Utility.fromPipToPrice(this.barTestInit.getParam().getBody_abs_min(), barTestInit.getInstrument());
    }

    @Override
    protected String getTitleTest() {
        return "BODY ABS";
    }

    @Override
    protected BarTestResult testBar(BarTestInit barTestInit) {

        if (this.barTestInit.getBody() <= bodyAbsMin || this.barTestInit.getBody() >= bodyAbsMax) {

            return this.barTestInit.getParam()
                    .getFail(this.customFormat(
                            this.barTestInit.getInstrument(),
                            "Aborting for BarBodyAbs not matching. Val: %pipscale% Min: %pipscale% Max: %pipscale%",
                            this.barTestInit.getBody(),
                            bodyAbsMax,
                            bodyAbsMin));
        }

        return this.barTestInit.getParam().getOk(getTitleTest());
    }
}
