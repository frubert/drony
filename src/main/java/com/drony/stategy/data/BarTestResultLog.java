package com.drony.stategy.data;

import com.drony.stategy.test.data.BarTestResult;
import com.drony.utility.data.Pair;
import com.dukascopy.api.IBar;

import java.util.List;

public class BarTestResultLog {

    private final IBar bar;

    private final List<BarTestResult> testResults;

    private boolean result;

    public BarTestResultLog(IBar bar, Pair<Boolean, List<BarTestResult>> results) {
        this.bar = bar;
        this.result = results.getFirst();
        this.testResults = results.getSecond();
    }

    public IBar getBar() {
        return bar;
    }

    public List<BarTestResult> getTestResults() {
        return testResults;
    }

    public boolean isResult() {
        return result;
    }

    public void update(Pair<Boolean, List<BarTestResult>> result) {
        this.result = result.getFirst();
        this.testResults.addAll(result.getSecond());
    }
}
