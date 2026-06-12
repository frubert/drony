package com.drony.tools;

import com.drony.strategy.utility.ReaderParam;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.dhatim.fastexcel.reader.Cell;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;

/**
 * Genera file Excel di parametri per l'ottimizzazione: prodotto cartesiano dei
 * range indicati, una combinazione per colonna, partendo dai valori della
 * colonna base di un template.
 *
 * <pre>
 * java -cp drony-4_2-jar-with-dependencies.jar com.drony.tools.BatchGenerator \
 *   --template param/DronyParamV04.xlsx --ranges ranges.txt --out runs/batch01
 * </pre>
 *
 * Formato del file ranges (etichette come in colonna A del template, # = commento):
 * <pre>
 * Body % Min: = 10, 20, 30
 * Slope Max:  = 20, 50, 100
 * macroPL     = true, false
 * Start TradingTime: = 07:00, 09:00
 * </pre>
 *
 * Output: param_001.xlsx, param_002.xlsx... (max --max-cols combinazioni per file,
 * default 30) + combos.csv con la mappa combinazione → valori, per incrociare i
 * results.csv dei run.
 */
public class BatchGenerator {

    private static final int LABEL_COL = 0;
    private static final int NOTE_COL = 1;
    private static final int BASE_COL = 2;
    private static final int FIRST_OUT_COL = 2;

    public static void main(String[] args) throws Exception {
        Map<String, String> opts = parseArgs(args);

        File template = new File(required(opts, "template"));
        File outDir = new File(required(opts, "out"));
        int maxCols = Integer.parseInt(opts.getOrDefault("max-cols", "30"));
        outDir.mkdirs();

        List<Row> rows = readRows(template);
        Map<String, Integer> labelIndex = indexLabels(rows);

        List<Map<String, String>> combos;
        if (opts.containsKey("combos")) {
            /* lista esplicita di combinazioni (una per riga, header = etichette) — usata da Optuna */
            combos = parseCombosCsv(new File(opts.get("combos")), labelIndex);
        } else {
            LinkedHashMap<String, List<String>> ranges =
                    parseRanges(new File(required(opts, "ranges")), labelIndex);
            combos = cartesianProduct(ranges);
        }

        System.out.printf("Combinazioni: %d%n", combos.size());

        writeCombosCsv(outDir, combos);

        int fileIndex = 1;
        for (int start = 0; start < combos.size(); start += maxCols, fileIndex++) {
            List<Map<String, String>> chunk =
                    combos.subList(start, Math.min(start + maxCols, combos.size()));
            File outFile = new File(outDir, String.format("param_%03d.xlsx", fileIndex));
            writeParamFile(outFile, rows, labelIndex, chunk, start);
            System.out.println("Scritto " + outFile + " (" + chunk.size() + " combinazioni)");
        }
    }

    private static List<Row> readRows(File template) throws IOException {
        try (InputStream is = new FileInputStream(template);
                ReadableWorkbook wb = new ReadableWorkbook(is)) {
            return wb.getFirstSheet().read();
        }
    }

    private static Map<String, Integer> indexLabels(List<Row> rows) {
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < rows.size(); i++) {
            Optional<String> label = rows.get(i).getCellAsString(LABEL_COL);
            if (label.isPresent() && !label.get().trim().isEmpty()) {
                index.putIfAbsent(ReaderParam.normalize(label.get()), i);
            }
        }
        return index;
    }

    private static LinkedHashMap<String, List<String>> parseRanges(File file,
            Map<String, Integer> labelIndex) throws IOException {

        LinkedHashMap<String, List<String>> ranges = new LinkedHashMap<>();

        for (String line : Files.readAllLines(file.toPath())) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            int eq = line.indexOf('=');
            if (eq < 0) {
                throw new IllegalArgumentException("Riga ranges senza '=': " + line);
            }
            String label = line.substring(0, eq).trim();
            String normalized = ReaderParam.normalize(label);
            if (!labelIndex.containsKey(normalized)) {
                throw new IllegalArgumentException(
                        "Etichetta '" + label + "' non trovata nel template");
            }
            List<String> values = new ArrayList<>();
            for (String v : line.substring(eq + 1).split(",")) {
                values.add(v.trim());
            }
            ranges.put(normalized, values);
        }

        if (ranges.isEmpty()) {
            throw new IllegalArgumentException("Nessun range definito in " + file);
        }
        return ranges;
    }

    private static List<Map<String, String>> cartesianProduct(
            LinkedHashMap<String, List<String>> ranges) {

        List<Map<String, String>> combos = new ArrayList<>();
        combos.add(new LinkedHashMap<>());

        for (Map.Entry<String, List<String>> range : ranges.entrySet()) {
            List<Map<String, String>> next = new ArrayList<>();
            for (Map<String, String> combo : combos) {
                for (String value : range.getValue()) {
                    Map<String, String> extended = new LinkedHashMap<>(combo);
                    extended.put(range.getKey(), value);
                    next.add(extended);
                }
            }
            combos = next;
        }
        return combos;
    }

    private static List<Map<String, String>> parseCombosCsv(File file,
            Map<String, Integer> labelIndex) throws IOException {

        List<String> lines = Files.readAllLines(file.toPath());
        if (lines.size() < 2) {
            throw new IllegalArgumentException("File combos vuoto: " + file);
        }

        String[] labels = lines.get(0).split(";");
        List<String> normalized = new ArrayList<>();
        for (String label : labels) {
            String norm = ReaderParam.normalize(label);
            if (!labelIndex.containsKey(norm)) {
                throw new IllegalArgumentException(
                        "Etichetta '" + label + "' non trovata nel template");
            }
            normalized.add(norm);
        }

        List<Map<String, String>> combos = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).trim().isEmpty()) {
                continue;
            }
            String[] values = lines.get(i).split(";");
            Map<String, String> combo = new LinkedHashMap<>();
            for (int c = 0; c < normalized.size(); c++) {
                combo.put(normalized.get(c), values[c].trim());
            }
            combos.add(combo);
        }
        return combos;
    }

    private static void writeCombosCsv(File outDir, List<Map<String, String>> combos)
            throws IOException {

        try (PrintWriter out = new PrintWriter(new FileWriter(new File(outDir, "combos.csv")))) {
            if (!combos.isEmpty()) {
                out.println("combo;" + String.join(";", combos.get(0).keySet()));
            }
            for (int i = 0; i < combos.size(); i++) {
                out.println(comboName(i) + ";" + String.join(";", combos.get(i).values()));
            }
        }
    }

    private static String comboName(int index) {
        return String.format("C%04d", index + 1);
    }

    private static void writeParamFile(File outFile, List<Row> rows,
            Map<String, Integer> labelIndex, List<Map<String, String>> combos, int comboOffset)
            throws IOException {

        int nameRow = labelIndex.get(ReaderParam.normalize("Name"));

        try (FileOutputStream os = new FileOutputStream(outFile);
                Workbook wb = new Workbook(os, "DronyBatch", null)) {
            Worksheet ws = wb.newWorksheet("Param");

            for (int r = 0; r < rows.size(); r++) {
                Row row = rows.get(r);

                copyCell(ws, r, LABEL_COL, row.getCell(LABEL_COL));
                copyCell(ws, r, NOTE_COL, row.getCell(NOTE_COL));

                String normalized = row.getCellAsString(LABEL_COL)
                        .map(ReaderParam::normalize).orElse("");

                for (int c = 0; c < combos.size(); c++) {
                    int outCol = FIRST_OUT_COL + c;

                    if (r == nameRow) {
                        ws.value(r, outCol, comboName(comboOffset + c));
                    } else if (combos.get(c).containsKey(normalized)) {
                        writeOverride(ws, r, outCol, normalized, combos.get(c).get(normalized));
                    } else if (isTimeLabel(normalized)) {
                        /* le celle orario vanno riscritte come data formattata, non come numero grezzo */
                        final int rowIdx = r;
                        final int colIdx = outCol;
                        row.getCellAsDate(BASE_COL).ifPresent(date -> {
                            ws.value(rowIdx, colIdx, date);
                            ws.style(rowIdx, colIdx).format("HH:mm").set();
                        });
                    } else {
                        copyCell(ws, r, outCol, row.getCell(BASE_COL));
                    }
                }
            }
            ws.finish();
        }
    }

    private static boolean isTimeLabel(String normalizedLabel) {
        return normalizedLabel.equals("starttradingtime") || normalizedLabel.equals("endtradingtime");
    }

    private static void writeOverride(Worksheet ws, int r, int c, String normalizedLabel,
            String value) {
        if (isTimeLabel(normalizedLabel)) {
            LocalTime time = LocalTime.parse(value.length() == 4 ? "0" + value : value);
            ws.value(r, c, LocalDateTime.of(1999, 1, 1, time.getHour(), time.getMinute()));
            ws.style(r, c).format("HH:mm").set();
            return;
        }
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            ws.value(r, c, Boolean.parseBoolean(value));
            return;
        }
        try {
            ws.value(r, c, new BigDecimal(value));
        } catch (NumberFormatException e) {
            ws.value(r, c, value);
        }
    }

    private static void copyCell(Worksheet ws, int r, int c, Cell cell) {
        if (cell == null) {
            return;
        }
        switch (cell.getType()) {
            case NUMBER:
                ws.value(r, c, (BigDecimal) cell.getValue());
                break;
            case BOOLEAN:
                ws.value(r, c, (Boolean) cell.getValue());
                break;
            case STRING:
            case FORMULA:
                ws.value(r, c, cell.getText());
                break;
            default:
                /* EMPTY/ERROR: niente da copiare */
        }
    }

    private static String required(Map<String, String> opts, String key) {
        String value = opts.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Argomento obbligatorio mancante: --" + key);
        }
        return value;
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> opts = new HashMap<>();
        for (int i = 0; i < args.length - 1; i += 2) {
            if (!args[i].startsWith("--")) {
                throw new IllegalArgumentException("Argomento non riconosciuto: " + args[i]);
            }
            opts.put(args[i].substring(2), args[i + 1]);
        }
        return opts;
    }
}
