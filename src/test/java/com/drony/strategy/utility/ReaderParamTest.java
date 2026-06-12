package com.drony.strategy.utility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.drony.strategy.data.ParamDrony;
import com.dukascopy.api.IConsole;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.Period;
import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Proxy;
import java.util.List;
import org.junit.Test;

public class ReaderParamTest {

    /** IConsole finto: inghiotte l'output, serve solo a soddisfare il costruttore. */
    private static IConsole fakeConsole() {
        PrintStream sink = new PrintStream(PrintStream.nullOutputStream());
        return (IConsole) Proxy.newProxyInstance(
                IConsole.class.getClassLoader(),
                new Class<?>[]{IConsole.class},
                (proxy, method, args) -> method.getReturnType().isAssignableFrom(PrintStream.class) ? sink : null);
    }

    @Test
    public void readsStrategiesFromRealParamFileByLabel() throws Exception {
        File file = new File("param/DronyParamV04.xlsx");
        assertTrue("File parametri di riferimento mancante: " + file.getAbsolutePath(), file.exists());

        List<ParamDrony> dronies = new ReaderParam(file, fakeConsole()).getDronies();

        assertFalse("Nessuna strategia letta dal file parametri", dronies.isEmpty());

        ParamDrony first = dronies.get(0);
        assertEquals("BRENT USD 1D_1C2", first.getName());
        assertEquals(Instrument.BRENTCMDUSD, first.getSelectedInstrument());
        assertEquals(Period.DAILY, first.getSelectedPeriod());
        assertEquals(1, first.getN());
        assertEquals(20.0, first.getBody_perc_min(), 0.0001);
        assertEquals(100.0, first.getBody_perc_max(), 0.0001);
        assertEquals(60.0, first.getIndent(), 0.0001);
        assertEquals("CLUSTER 4", first.getOrderCluster().trim());
        assertEquals(1, first.getOrderClusterPriority());
        assertTrue(first.isAttivaMonotona());
        assertTrue(first.isPreventMultipleOrders());
        assertFalse(first.isMacroPL());
        assertFalse(first.isActiveEdgeOrder());
        assertEquals(30, first.getOrderNumMaxBar());
    }
}
