package com.drony.strategy.test;

import com.drony.strategy.BarTestInit;
import com.drony.strategy.test.AbstractBarTest;
import com.drony.strategy.test.data.BarTestResult;
import com.drony.strategy.utility.BarUtility;
import com.dukascopy.api.IBar;

public class BarColorStory extends AbstractBarTest {

    public BarColorStory(BarTestInit barTestInit) {
        super(barTestInit);
    }

    @Override
    protected String getTitleTest() {
        return "BAR COLOR STORY";
    }

    @Override
    protected BarTestResult testBar(BarTestInit init) {

        double colorStorySameBars = init.getParam().getColorStorySameBars();

        if (colorStorySameBars != 0) {
            int sameColorCounter = 0;
            for (int k = init.getMaxBars() - init.getParam().getNumColorStoryBars(); k < init.getBackwardBars().size(); k++) {
                IBar backwardBar = init.getBackwardBars().get(k);
                if (BarUtility.getBarColor(backwardBar) == init.getDirection()) {
                    sameColorCounter++;
                }
                if (sameColorCounter >= colorStorySameBars) {
                    return this.barTestInit.getParam().getFail(this.customFormat(
                            barTestInit.getInstrument(),
                            "Aborting for too many same color. Current: %s Expected %s",
                            sameColorCounter,
                            colorStorySameBars));
                }
            }
        }

        return this.barTestInit.getParam().getOk(getTitleTest());
    }
}
