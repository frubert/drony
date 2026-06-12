package com.drony.stategy.test;

import com.drony.stategy.BarTestInit;
import com.drony.stategy.test.data.BarTestResult;
import com.drony.stategy.utility.BarUtility;
import com.drony.stategy.utility.Utility;
import com.dukascopy.api.IBar;

public class BarSame extends AbstractBarTest {

    public BarSame(BarTestInit barTestInit) {
        super(barTestInit);
    }

    @Override
    protected String getTitleTest() {
        return "BAR SAME DIRECTION";
    }

    @Override
    protected BarTestResult testBar(BarTestInit barTestInit) {
        int numBodyShadowBars = barTestInit.getParam().getNumBodyShadowBars();
        double minBodyShadowPercentage = barTestInit.getParam().getMinBodyShadowPercentage();

        if (barTestInit.getParam().getMinBodyShadowPercentage() != 0) {
            for (int k = barTestInit.getMaxBars() - numBodyShadowBars; k < barTestInit.getBackwardBars().size(); k++) {
                IBar backwardBar = barTestInit.getBackwardBars().get(k);
                double currentBodyShadow = BarUtility.getBody(backwardBar) / BarUtility.getTotalBarSize(backwardBar);
                if (currentBodyShadow < minBodyShadowPercentage / 100) {
                    return this.barTestInit.getParam().getFail(this.customFormat(
                            barTestInit.getInstrument(),
                            "Aborting for too body shadow percentage Current: %s Expected min: %s",
                            currentBodyShadow * 100,
                            minBodyShadowPercentage * 100));
                }
            }
        }

        double minBodyByPrice = Utility.fromPipToPrice(barTestInit.getParam().getMinBodyShadow(), barTestInit.getInstrument());
        if (barTestInit.getParam().getMinBodyShadow() != 0) {
            for (int k = barTestInit.getMaxBars() - numBodyShadowBars; k < barTestInit.getBackwardBars().size(); k++) {
                IBar backwardBar = barTestInit.getBackwardBars().get(k);
                if (BarUtility.getBody(backwardBar) < minBodyByPrice) {
                    return this.barTestInit.getParam().getFail(this.customFormat(
                            barTestInit.getInstrument(),
                            "Aborting for too body shadow Current: %s Expected min: %s",
                            BarUtility.getBody(backwardBar),
                            minBodyByPrice));
                }
            }
        }

        return this.barTestInit.getParam().getOk(getTitleTest());
    }
}
