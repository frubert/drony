package com.drony.strategy.utility;

import com.drony.strategy.data.DirectionEnum;
import com.dukascopy.api.IBar;

public class BarUtility {

    public static DirectionEnum getBarColor(IBar bar) {
        if (bar.getClose() > bar.getOpen()) {
            return DirectionEnum.BUY;
        } else if (bar.getClose() < bar.getOpen()) {
            return DirectionEnum.SELL;
        } else {
            return DirectionEnum.DOJI;
        }
    }

    public static double getBody(IBar bar) {
        return Math.abs(bar.getOpen() - bar.getClose());
    }

    public static double getTotalBarSize(IBar bar) {
        return bar.getHigh() - bar.getLow();
    }

}
