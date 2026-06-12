package com.drony.stategy.test;

import com.drony.stategy.BarTestInit;
import com.drony.stategy.test.data.BarTestResult;
import com.drony.stategy.utility.Utility;
import com.dukascopy.api.Instrument;

public abstract class AbstractBarTest {

    protected final BarTestInit barTestInit;

    public AbstractBarTest(BarTestInit barTestInit) {
        this.barTestInit = barTestInit;
    }

    protected abstract String getTitleTest();

    protected abstract BarTestResult testBar(BarTestInit init);

    public BarTestResult testBar() {
        BarTestResult result = testBar(this.barTestInit);
        //writeCSVBar("TEST ", barTestInit.getBar(), getTitleTest(), result); // TODO
        return result;
    }

    protected String customFormat(Instrument instrument, String str, Object... args) {
        return Utility.customFormat(instrument, str, args);
    }
}
