package com.drony.strategy.edge;

import com.drony.strategy.edge.data.EdgeOrder;
import com.drony.strategy.edge.data.EdgeOrderParam;
import com.drony.utility.data.U;
import com.dukascopy.api.IEngine;
import com.dukascopy.api.IMessage;
import com.dukascopy.api.IOrder;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.JFException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EdgeOrderService {

    private static final Logger log = LoggerFactory.getLogger(EdgeOrderService.class);

    /** Good-till-time degli ordini edge: 60 giorni dal fill dell'ordine principale. */
    private static final long EDGE_GTT_MILLIS = 60L * 24 * 60 * 60 * 1000;

    private static final long RANDOM_LABEL_MAX = 100_000L;

    private final Map<String, EdgeOrder> edgeMaps;

    private final IEngine engine;

    public EdgeOrderService(IEngine engine) {
        this.edgeMaps = new HashMap<>();
        this.engine = engine;
    }

    public void setInfoOrder(
        String label, Instrument instrument, BigDecimal basePrice, BigDecimal stopLoss, BigDecimal mod, BigDecimal barClose, BigDecimal barOpen, BigDecimal barHigh, BigDecimal barLow, EdgeOrderParam edgeOrderParam) {
        if (edgeOrderParam.isActiveEdgeOrder()) {
            this.edgeMaps.put(label, new EdgeOrder(label, instrument, basePrice, stopLoss, mod, barClose, barOpen, barHigh, barLow, edgeOrderParam));
        }
    }

    public void checkOrderService(IMessage message) throws JFException {

        IOrder order = message.getOrder();

        if (order != null) {

            if (!this.edgeMaps.containsKey(order.getLabel())) {
                return;
            }

            IMessage.Type messageType = message.getType();

            switch (messageType) {
                case ORDER_FILL_OK:
                    createEdge(order);
                    break;
                case ORDER_CLOSE_OK:
                    removeEdgeOrder(order);
            }
        }
    }

    /** Garantisce che ogni ordine principale FILLED tracciato abbia il suo ordine edge, ricreandolo se sparito. */
    public void manageActiveEdge() throws JFException {

        for (IOrder order : this.engine.getOrders()) {

            if (order.getState() != IOrder.State.FILLED) {
                continue;
            }

            EdgeOrder edgeOrder = this.edgeMaps.get(order.getLabel());
            if (edgeOrder == null) {
                continue;
            }

            String labelEdge = edgeOrder.getLabelEdge();

            try {
                IOrder orderEdge = labelEdge == null ? null : this.engine.getOrder(labelEdge);

                if (orderEdge == null) {
                    this.createEdge(order);
                }
            } catch (JFException e) {
                log.error("manageActiveEdge failed for order {}", order.getLabel(), e);
            }
        }
    }

    private void removeEdgeOrder(IOrder order) throws JFException {
        EdgeOrder edgeOrder = this.edgeMaps.get(order.getLabel());

        if (edgeOrder == null) {
            return;
        }

        String labelEdge = edgeOrder.getLabelEdge();
        IOrder orderEdge = labelEdge == null ? null : this.engine.getOrder(labelEdge);

        if (orderEdge != null
                && (orderEdge.getState() == IOrder.State.FILLED || orderEdge.getState() == IOrder.State.OPENED || orderEdge.getState() == IOrder.State.CREATED)
                && orderEdge.getProfitLossInUSD() <= 0) {
            orderEdge.close();
        }
    }


    private void createEdge(IOrder order) throws JFException {

        EdgeOrder edgeOrder = this.edgeMaps.get(order.getLabel());
        EdgeOrderParam edgeOrderParam = edgeOrder.getEdgeOrderParam();

        String orderLabel = "EDGE_" + order.getLabel() + "_EDGE_" + U.randomLong(RANDOM_LABEL_MAX);
        String comment = "EDGE ";

        IEngine.OrderCommand oreOrderCommand;
        BigDecimal price, stopLost, takeProfit;


        BigDecimal identFixed = this.fromPipToPrice(edgeOrderParam.getIdentEdgeOrder(), edgeOrder.getInstrument());
        BigDecimal orderStopLossDelta = new BigDecimal((Math.abs(order.getStopLossPrice() - order.getOpenPrice())), MathContext.DECIMAL64)
                .movePointLeft(2).multiply(edgeOrderParam.getPercentStopLossIdent());

        if (order.isLong()) {

            comment = "SELL";
            oreOrderCommand = IEngine.OrderCommand.SELLSTOP;

            price = edgeOrder.getBasePrice()
                    .subtract(edgeOrderParam.getIndentPercentPennachioEdgeOrder().movePointLeft(2).multiply(edgeOrder.getBarClose().subtract(edgeOrder.getBarLow())).max(
                        BigDecimal.ZERO))
                    .subtract(edgeOrderParam.getIndentPercentModEdgeOrder().movePointLeft(2).multiply(edgeOrder.getMod()))
                    .subtract(identFixed)
                    .subtract(orderStopLossDelta);

            takeProfit = edgeOrder.getStopLoss().subtract(this.fromPipToPrice(edgeOrderParam.getTakeProfitEdgeOrder(), edgeOrder.getInstrument()));
            stopLost = price.add(this.fromPipToPrice(edgeOrderParam.getStopLossEdgeOrder(), edgeOrder.getInstrument()));

        } else {
            comment += "BUY";
            oreOrderCommand = IEngine.OrderCommand.BUYSTOP;

            price = edgeOrder.getBasePrice()
                    .add(edgeOrderParam.getIndentPercentPennachioEdgeOrder().movePointLeft(2).multiply(edgeOrder.getBarHigh().subtract(edgeOrder.getBarClose())).max(
                        BigDecimal.ZERO))
                    .add(edgeOrderParam.getIndentPercentModEdgeOrder().movePointLeft(2).multiply(edgeOrder.getMod()))
                    .add(identFixed)
                    .add(orderStopLossDelta);

            takeProfit = edgeOrder.getStopLoss().add(this.fromPipToPrice(edgeOrderParam.getTakeProfitEdgeOrder(), edgeOrder.getInstrument()));
            stopLost = price.subtract(this.fromPipToPrice(edgeOrderParam.getStopLossEdgeOrder(), edgeOrder.getInstrument()));
        }

        long gtt = order.getFillTime() + EDGE_GTT_MILLIS;

        IOrder edgeOrderSubmit = this.engine.submitOrder(
                orderLabel,
                edgeOrder.getInstrument(),
                oreOrderCommand,
                edgeOrderParam.getOrderSizeEdgeOrder().doubleValue(),
                fromBigDecimalToDouble(price, edgeOrder.getInstrument()),
                0,
                fromBigDecimalToDouble(stopLost, edgeOrder.getInstrument()),
                fromBigDecimalToDouble(takeProfit, edgeOrder.getInstrument()),
                gtt,
                comment);

        edgeOrder.setLabelEdge(edgeOrderSubmit.getLabel());
    }

    private BigDecimal fromPipToPrice(BigDecimal pips, Instrument instrument) {
        if (pips.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        BigDecimal pipvalue = new BigDecimal(instrument.getPipValue(), MathContext.DECIMAL64);
        return pips.multiply(pipvalue);
    }

    private double fromBigDecimalToDouble(BigDecimal price, Instrument instrument) {
        return price.setScale(instrument.getPipScale(), RoundingMode.HALF_EVEN).doubleValue();
    }

}
