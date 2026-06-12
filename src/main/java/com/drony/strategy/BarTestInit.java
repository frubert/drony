package com.drony.strategy;

import com.drony.strategy.data.DirectionEnum;
import com.drony.strategy.data.ParamDrony;
import com.drony.strategy.utility.BarUtility;
import com.dukascopy.api.*;

import java.util.List;

public class BarTestInit {

    private final IBar bar;

    private final ParamDrony param;

    private final Instrument instrument;

    private final DirectionEnum direction;

    private final double body;

    private final double totalBarSize;

    private final OfferSide offerSide;

    private List<IBar> backwardBars;

    private int maxBars;

    private IBar slopeBar;

    public BarTestInit(IBar bar, ParamDrony paramRoboBar, Instrument instrument, DirectionEnum direction) {
        this.bar = bar;
        this.param = paramRoboBar;
        this.body = BarUtility.getBody(bar);
        this.totalBarSize = BarUtility.getTotalBarSize(bar);
        this.instrument = instrument;
        this.direction = direction;
        this.offerSide = direction.equals(DirectionEnum.BUY) ? OfferSide.ASK : OfferSide.BID;
    }


    public BarTestInit(IBar bar, ParamDrony paramRoboBar, Instrument instrument, DirectionEnum direction, IHistory history, Period period, IBar slopeBar) throws JFException {
        this(bar, paramRoboBar, instrument, direction);
        this.slopeBar = slopeBar;
        this.maxBars = Math.max(this.param.getNumColorStoryBars(), this.param.getNumBodyShadowBars());
        this.backwardBars = history.getBars(instrument, period, offerSide, Filter.NO_FILTER, maxBars, bar.getTime(), 0);
    }

    public IBar getBar() {
        return bar;
    }

    public ParamDrony getParam() {
        return param;
    }

    public Instrument getInstrument() {
        return instrument;
    }

    public DirectionEnum getDirection() {
        return direction;
    }

    public double getBody() {
        return body;
    }

    public double getTotalBarSize() {
        return totalBarSize;
    }

    public OfferSide getOfferSide() {
        return offerSide;
    }

    public List<IBar> getBackwardBars() {
        return backwardBars;
    }

    public int getMaxBars() {
        return maxBars;
    }

    public IBar getSlopeBar() {
        return slopeBar;
    }

}
