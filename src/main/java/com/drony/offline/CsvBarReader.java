package com.drony.offline;

import com.dukascopy.api.IBar;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Legge barre OHLCV da CSV. Rileva due formati dall'intestazione:
 *
 *  - dukascopy-node (--format csv):  timestamp,open,high,low,close,volume
 *    con timestamp in millisecondi epoch o ISO-8601.
 *  - export ufficiale Dukascopy:     Gmt time,Open,High,Low,Close,Volume
 *    con time "dd.MM.yyyy HH:mm:ss.SSS".
 *
 * Le barre vengono restituite in ordine cronologico crescente.
 */
public final class CsvBarReader {

    private static final DateTimeFormatter DUKA_OFFICIAL =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss.SSS");

    private CsvBarReader() {
    }

    public static List<IBar> read(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file);
        if (lines.isEmpty()) {
            throw new IOException("CSV vuoto: " + file);
        }

        String header = lines.get(0).toLowerCase();
        boolean official = header.contains("gmt time");
        char sep = header.contains(";") ? ';' : ',';

        List<IBar> bars = new ArrayList<>(lines.size());
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] f = splitRespectingFormat(line, sep);
            if (f.length < 6) {
                continue;
            }
            long time = official ? parseOfficialTime(f[0]) : parseEpochOrIso(f[0]);
            double open = Double.parseDouble(f[1]);
            double high = Double.parseDouble(f[2]);
            double low = Double.parseDouble(f[3]);
            double close = Double.parseDouble(f[4]);
            double volume = f[5].isBlank() ? 0 : Double.parseDouble(f[5]);
            bars.add(new Candle(time, open, close, high, low, volume));
        }
        bars.sort((a, b) -> Long.compare(a.getTime(), b.getTime()));
        return bars;
    }

    private static String[] splitRespectingFormat(String line, char sep) {
        return line.split("\\s*" + (sep == ';' ? ";" : ",") + "\\s*");
    }

    private static long parseOfficialTime(String s) {
        return LocalDateTime.parse(s.trim(), DUKA_OFFICIAL).toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    private static long parseEpochOrIso(String s) {
        s = s.trim();
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return LocalDateTime.parse(s.replace("Z", "")).toInstant(ZoneOffset.UTC).toEpochMilli();
        }
    }
}
