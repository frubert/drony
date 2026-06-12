package com.drony.strategy.utility;

import com.drony.strategy.data.ParamDrony;
import com.drony.utility.data.U;
import com.dukascopy.api.IConsole;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.Period;
import org.dhatim.fastexcel.reader.Cell;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;
import org.dhatim.fastexcel.reader.Sheet;

import java.io.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Legge il file Excel dei parametri: una colonna per strategia (dalla colonna C in poi),
 * una riga per parametro. Le righe sono individuate dall'etichetta in colonna A,
 * non dalla posizione: righe spostate o aggiunte nel foglio non rompono la lettura.
 */
public class ReaderParam {

    private static final int FIRST_STRATEGY_COL = 2;

    private List<ParamDrony> dronies;

    private List<Row> rows;
    private final Map<String, Integer> labelIndex = new HashMap<>();

    public ReaderParam(File file, IConsole console) throws IOException {

        try (InputStream is = new FileInputStream(file); ReadableWorkbook wb = new ReadableWorkbook(is)) {
            Sheet sheet = wb.getFirstSheet();
            try {
                this.rows = sheet.read();
                indexLabels();
                dronies = readColumns(console);
            } catch (Exception e) {
                console.getErr().println(e.getMessage());
                console.getErr().println(e);
                throw new IOException("Errore lettura parametri da " + file.getName(), e);
            }
        }
    }

    public List<ParamDrony> getDronies() {
        return dronies;
    }

    /**
     * Normalizza un'etichetta di riga: minuscole, '%' diventa "perc", tutto il resto
     * dei caratteri non alfanumerici viene rimosso. Rende il match insensibile a
     * spazi, maiuscole e punteggiatura ("End   TradingTime:" == "endtradingtime").
     */
    public static String normalize(String label) {
        return label.toLowerCase()
                .replace("%", "perc")
                .replaceAll("[^a-z0-9]", "");
    }

    private void indexLabels() {
        for (int i = 0; i < rows.size(); i++) {
            Optional<String> label = rows.get(i).getCellAsString(0);
            if (label.isPresent() && !U.isEmptyOrNull(label.get())) {
                labelIndex.putIfAbsent(normalize(label.get()), i);
            }
        }
    }

    private Row rowOf(String label) {
        Integer index = labelIndex.get(normalize(label));
        if (index == null) {
            throw new IllegalStateException(
                    "Etichetta parametro non trovata in colonna A del file Excel: '" + label + "'");
        }
        return rows.get(index);
    }

    private String stringValue(String label, int colIndex, String defaultValue) {
        return U.clean(rowOf(label).getCellAsString(colIndex).orElse(defaultValue));
    }

    private double numberValue(String label, int colIndex, String defaultValue) {
        return rowOf(label).getCellAsNumber(colIndex).orElse(new BigDecimal(defaultValue)).doubleValue();
    }

    private int intValue(String label, int colIndex, String defaultValue) {
        return rowOf(label).getCellAsNumber(colIndex).orElse(new BigDecimal(defaultValue)).intValue();
    }

    private BigDecimal requiredNumber(String label, int colIndex) {
        Cell cell = rowOf(label).getCell(colIndex);
        if (cell == null) {
            throw new IllegalStateException("Parametro obbligatorio '" + label + "' vuoto (colonna " + colIndex + ")");
        }
        return cell.asNumber();
    }

    private boolean booleanValue(String label, int colIndex, boolean defaultValue) {
        return parseBoolean(label, rowOf(label).getCell(colIndex), defaultValue);
    }

    private List<ParamDrony> readColumns(IConsole console) {

        List<ParamDrony> params = new ArrayList<>();

        int colIndex = FIRST_STRATEGY_COL;
        while (rowOf("Name").getCellAsString(colIndex).isPresent()
                && !U.isEmptyOrNull(rowOf("Name").getCellAsString(colIndex).get())) {

            ParamDrony.SequenceFilter sequenceFilter = new ParamDrony.SequenceFilter(
                    intValue("N Bars:", colIndex, "2"),
                    numberValue("Body % Min:", colIndex, "10"),
                    numberValue("Body % Max:", colIndex, "95"),
                    numberValue("Mod Min :", colIndex, "10"),
                    numberValue("Mod Max :", colIndex, "100"),
                    numberValue("Body abs Min:", colIndex, "0.5"),
                    numberValue("Body abs Max :", colIndex, "50"),
                    numberValue("Slope Min:", colIndex, "1"),
                    numberValue("Slope Max:", colIndex, "20"));

            ParamDrony.EntryConfig entry = new ParamDrony.EntryConfig(
                    numberValue("Indent:", colIndex, "0"),
                    numberValue("Indent % shadow:", colIndex, "0"),
                    numberValue("Slippage:", colIndex, "0"),
                    intValue("Num CandlesValid;", colIndex, "4"));

            ParamDrony.PinzaConfig pinza = new ParamDrony.PinzaConfig(
                    numberValue("Cap Abs:", colIndex, "20"),
                    numberValue("Cap %:", colIndex, "0"),
                    numberValue("Cap attn:", colIndex, "90"),
                    numberValue("Floor abs:", colIndex, "40"),
                    numberValue("Floor %:", colIndex, "0"),
                    numberValue("Floor attn:", colIndex, "10"),
                    booleanValue("Pinza monotona de/crescente", colIndex, true),
                    intValue("waitNBarPinza", colIndex, "0"),
                    intValue("Num max barre per ordine", colIndex, "10"));

            ParamDrony.TradingWindow tradingWindow = new ParamDrony.TradingWindow(
                    rowOf("Start TradingTime:").getCellAsDate(colIndex)
                            .orElse(LocalDateTime.of(1999, 1, 1, 7, 0)).toLocalTime(),
                    rowOf("End TradingTime:").getCellAsDate(colIndex)
                            .orElse(LocalDateTime.of(1999, 1, 1, 17, 0)).toLocalTime(),
                    booleanValue("FreeWeekEnd", colIndex, false));

            ParamDrony.ShadowFilter shadowFilter = new ParamDrony.ShadowFilter(
                    intValue("numBodyShadowBars:", colIndex, "1"),
                    numberValue("minBodyShadow%", colIndex, "20"),
                    numberValue("minBodyShadow", colIndex, "0"),
                    numberValue("minFutureBodyShadow%", colIndex, "0"),
                    numberValue("minFutureBodyShadow", colIndex, "0"));

            ParamDrony.MacroPL macroPL = new ParamDrony.MacroPL(
                    booleanValue("macroPL", colIndex, false),
                    numberValue("macroPLProfit", colIndex, "30"),
                    numberValue("macroPLLoss", colIndex, "200"));

            ParamDrony.ColorStory colorStory = new ParamDrony.ColorStory(
                    intValue("numColorStoryBars", colIndex, "7"),
                    numberValue("colorStorySameBars", colIndex, "7"));

            ParamDrony.ClusterConfig cluster = new ParamDrony.ClusterConfig(
                    rowOf("order cluster").getCellAsString(colIndex).orElse(""),
                    intValue("order cluster priority", colIndex, "0"),
                    intValue("Max order by cluster", colIndex, "1"));

            ParamDrony.BreakEvenConfig breakEven = new ParamDrony.BreakEvenConfig(
                    numberValue("IF %a TP > SL = OpenPrice + %bTP)", colIndex, "0"),
                    numberValue("%b TP to add on OpenPrice", colIndex, "0"));

            ParamDrony.EdgeConfig edge = new ParamDrony.EdgeConfig(
                    booleanValue("activeEdgeOrder", colIndex, false),
                    requiredNumber("EdgeOrderSize", colIndex),
                    requiredNumber("identEdgeOrder", colIndex),
                    requiredNumber("indentPercentPennachioEdgeOrder", colIndex),
                    requiredNumber("indentPercentModEdgeOrder", colIndex),
                    requiredNumber("stopLossEdgeOrder", colIndex),
                    requiredNumber("takeProfitEdgeOrder", colIndex),
                    requiredNumber("percentStopLossIdent", colIndex));

            ParamDrony param = new ParamDrony(
                    stringValue("Name", colIndex, ""),
                    Instrument.valueOf(stringValue("Selected Instrument:", colIndex, "EURUSD")),
                    Period.valueOf(stringValue("Selected Period:", colIndex, "ONE_HOUR")),
                    numberValue("OrderSize:", colIndex, "0.2"),
                    stringValue("strategyType", colIndex, "FULL"),
                    booleanValue("preventMultipleOrders", colIndex, true),
                    sequenceFilter, entry, pinza, tradingWindow, shadowFilter,
                    macroPL, colorStory, cluster, breakEven, edge);

            params.add(param);

            console.getOut().println(param.toString());

            colIndex++;
        }

        return params;
    }

    private boolean parseBoolean(String label, Cell cell, boolean defaultValue) {
        if (cell == null) {
            return defaultValue;
        }
        switch (cell.getType()) {
            case EMPTY:
                return defaultValue;
            case BOOLEAN:
                return (Boolean) cell.getValue();
            case STRING:
            case FORMULA:
                String value = U.clean(cell.getText()).toLowerCase();
                return value.equals("true") || value.equals("vero") || value.equals("1");
            case NUMBER:
                return cell.getText().equals("1");
            case ERROR:
                throw new IllegalStateException(
                        "Cella in errore per il parametro '" + label + "': " + cell.getAddress());
            default:
                return defaultValue;
        }
    }
}
