package com.drony.stategy.utility;

import com.drony.stategy.data.DirectionEnum;
import com.dukascopy.api.IBar;

public class BarUtility {

    /* TODO forse sarebbe meglio usare proprio i colori */
    public static DirectionEnum getBarColor(IBar bar) {
        if (bar.getClose() > bar.getOpen()) {
            return DirectionEnum.BUY;
        } else if (bar.getClose() < bar.getOpen()) {
            return DirectionEnum.SELL;
        } else {
            return null;
        }
    }

    public static double getBody(IBar bar) {
        return Math.abs(bar.getOpen() - bar.getClose());
    }

    public static double getTotalBarSize(IBar bar) {
        return bar.getHigh() - bar.getLow();
    }

}
