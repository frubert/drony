package com.drony.stategy.test;

import com.drony.stategy.BarTestInit;
import com.drony.stategy.test.data.BarTestResult;
import com.drony.stategy.utility.Utility;

public class BarMod extends AbstractBarTest {

    private final double modMin;
    private final double modMax;


    public BarMod(BarTestInit init) {
        super(init);
        /* TODO Questi valori è possibile pre calcolarli */
        modMin = Utility.fromPipToPrice(init.getParam().getMod_min(), init.getInstrument());
        modMax = Utility.fromPipToPrice(init.getParam().getMod_max(), init.getInstrument());
    }

    @Override
    protected String getTitleTest() {
        return "BAR MOD";
    }

    @Override
    protected BarTestResult testBar(BarTestInit init) {

        if (init.getParam().getMod_min() != 0 && init.getParam().getMod_max() != 0) {

            double mod = Math.abs(init.getBackwardBars().get(0).getOpen() - init.getBar().getClose());

            if (mod <= modMin || mod >= modMax) {

                return this.barTestInit.getParam().getFail(
                        this.customFormat(
                                barTestInit.getInstrument(),
                                "Aborting for Mod not matching. Val: %pipscale% Min: %pipscale% Max: %pipscale%",
                                mod,
                                modMin,
                                modMax));
            }
        }
        return this.barTestInit.getParam().getOk(getTitleTest());
    }
}
