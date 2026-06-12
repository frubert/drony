package com.drony;

import java.io.File;

import com.drony.stategy.DelegateDrony;
import com.dukascopy.api.*;


@Library("drony-4_2-jar-with-dependencies.jar")
public class DronyV042 implements IStrategy {

    @Configurable(value = "Excel parametri")
    public File fileParam;

    @Configurable(value = "Cartella destinazione risultati")
    public File fileResult;

    @Configurable(value = "Log Verboso")
    public Boolean outPutVerboso;


    private IConsole console;
    private IContext context;

    private DelegateDrony delegate;

    public void onStart(IContext context) throws JFException {
        this.console = context.getConsole();
        this.context = context;

        console.getOut().println("Version: " + DelegateDrony.version);
        console.getOut().println("Read EXCEL params");

        delegate = new DelegateDrony(fileParam, fileResult, context, outPutVerboso);

        console.getOut().println("Read EXCEL params");

        delegate.onStart(context);

    }

    public void onAccount(IAccount account) throws JFException {
        if (delegate != null) {
            delegate.onAccount(account);
        }
    }

    public void onMessage(IMessage message) throws JFException {
        delegate.onMessage(message);
    }

    public void onStop() throws JFException {
        delegate.onStop();
    }

    public void onTick(Instrument instrument, ITick tick) throws JFException {
        delegate.onTick(instrument, tick);
    }
    
    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {
        delegate.onBar(instrument, period, askBar, bidBar);
    }
}
