package com.drony.offline;

import com.dukascopy.api.IBar;

/** Barra OHLCV letta da CSV: implementazione minima di IBar per il backtest offline. */
public record Candle(long getTime, double getOpen, double getClose, double getHigh,
                     double getLow, double getVolume) implements IBar {
}
