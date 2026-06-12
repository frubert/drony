package com.drony.stategy.edge.data;

import java.math.BigDecimal;

public interface EdgeOrderParam {

   public boolean isActiveEdgeOrder();

   public BigDecimal getOrderSizeEdgeOrder();

   public BigDecimal getIdentEdgeOrder();

   public BigDecimal getIndentPercentPennachioEdgeOrder();

   public BigDecimal getIndentPercentModEdgeOrder();

   public BigDecimal getStopLossEdgeOrder();

   public BigDecimal getTakeProfitEdgeOrder();

   public BigDecimal getPercentStopLossIdent();
}
