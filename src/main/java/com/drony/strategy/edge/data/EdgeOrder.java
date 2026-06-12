package com.drony.strategy.edge.data;

import com.dukascopy.api.Instrument;
import java.math.BigDecimal;

public class EdgeOrder {

    private final String label;

    private final Instrument instrument;

    private final BigDecimal basePrice;

    private final BigDecimal stopLoss;

    private final BigDecimal mod;

    private final BigDecimal barClose;

    private final BigDecimal barOpen;

    private final BigDecimal barHigh;

    private final BigDecimal barLow;

    private final EdgeOrderParam edgeOrderParam;

    private String labelEdge;

    public EdgeOrder(String label, Instrument instrument, BigDecimal basePrice, BigDecimal stopLoss, BigDecimal mod, BigDecimal barClose, BigDecimal barOpen, BigDecimal barHigh, BigDecimal barLow, EdgeOrderParam edgeOrderParam) {
        this.label = label;
        this.instrument = instrument;
        this.basePrice = basePrice;
        this.stopLoss = stopLoss;
        this.mod = mod;
        this.barClose = barClose;
        this.barOpen = barOpen;
        this.barHigh = barHigh;
        this.barLow = barLow;
        this.edgeOrderParam = edgeOrderParam;
        this.labelEdge = null;
    }

    public String getLabel() {
        return label;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public BigDecimal getStopLoss() {
        return stopLoss;
    }

    public BigDecimal getMod() {
        return mod;
    }

    public BigDecimal getBarClose() {
        return barClose;
    }

    public BigDecimal getBarOpen() {
        return barOpen;
    }

    public BigDecimal getBarHigh() {
        return barHigh;
    }

    public BigDecimal getBarLow() {
        return barLow;
    }

    public Instrument getInstrument() {
        return instrument;
    }

    public String getLabelEdge() {
        return labelEdge;
    }

    public void setLabelEdge(String labelEdge) {
        this.labelEdge = labelEdge;
    }

    public EdgeOrderParam getEdgeOrderParam() {
        return edgeOrderParam;
    }
}
