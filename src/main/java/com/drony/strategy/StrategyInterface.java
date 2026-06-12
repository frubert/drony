package com.drony.strategy;

import com.dukascopy.api.IAccount;
import com.dukascopy.api.IBar;
import com.dukascopy.api.IContext;
import com.dukascopy.api.IMessage;
import com.dukascopy.api.ITick;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.JFException;
import com.dukascopy.api.Period;

public interface StrategyInterface {

  void onStart(IContext context) throws JFException;

  void onTick(Instrument instrument, ITick tick) throws JFException;

  void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException;

  void onMessage(IMessage message) throws JFException;

  void onAccount(IAccount account) throws JFException;

  void onStop() throws JFException;
}
