package com.drony.strategy.utility;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Giornale decisionale: una riga CSV per ogni decisione presa dall'algoritmo
 * su una barra (segnale scartato, ordine creato, blocco orario, fill, chiusura...).
 *
 * Serve a rispondere a "perché su questa barra non è successo niente?" senza
 * dover ricostruire il flusso dal report Excel. Si attiva da drony.properties
 * (strategy.decisionsFile); se disattivato, l'istanza {@link #DISABLED} non fa nulla.
 */
public class DecisionLogger implements Closeable {

    public enum Outcome {
        /** Barra mai valutata: fuori orario, ordine già attivo, direzione esclusa dal tipo strategia. */
        BLOCCATO,
        /** Setup valutato e respinto da un filtro (con il dettaglio del filtro che ha fermato). */
        SCARTATO,
        /** Ordine sottomesso. */
        ORDINE,
        /** Ordine pronto ma respinto dalla regola di cluster. */
        RIFIUTATO,
        /** Ordine fillato dal mercato. */
        FILL,
        /** Ordine chiuso (con motivazione). */
        CHIUSURA
    }

    /** Istanza disattivata: tutte le log() sono no-op. */
    public static final DecisionLogger DISABLED = new DecisionLogger();

    private static final Logger log = LoggerFactory.getLogger(DecisionLogger.class);

    private final BufferedWriter writer;
    private final DateFormat dateFormat;

    private DecisionLogger() {
        this.writer = null;
        this.dateFormat = null;
    }

    private DecisionLogger(File file) throws IOException {
        File parent = file.getAbsoluteFile().getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        this.writer = new BufferedWriter(new FileWriter(file, false));
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        this.dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        this.writer.write("time;strategia;direzione;esito;dettaglio");
        this.writer.newLine();
    }

    /** @return logger su file, o {@link #DISABLED} se il file è null o non scrivibile. */
    public static DecisionLogger toFile(File file) {
        if (file == null) {
            return DISABLED;
        }
        try {
            return new DecisionLogger(file);
        } catch (IOException e) {
            log.error("Impossibile aprire il giornale decisionale {}: log disattivato", file, e);
            return DISABLED;
        }
    }

    public void log(long time, String strategy, String direction, Outcome outcome, String detail) {
        if (writer == null) {
            return;
        }
        try {
            writer.write(dateFormat.format(new Date(time)));
            writer.write(';');
            writer.write(sanitize(strategy));
            writer.write(';');
            writer.write(direction == null ? "-" : direction);
            writer.write(';');
            writer.write(outcome.name());
            writer.write(';');
            writer.write(sanitize(detail));
            writer.newLine();
        } catch (IOException e) {
            log.error("Scrittura giornale decisionale fallita", e);
        }
    }

    private static String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return value.replace(';', ',').replace('\n', ' ').replace('\r', ' ');
    }

    @Override
    public void close() {
        if (writer == null) {
            return;
        }
        try {
            writer.close();
        } catch (IOException e) {
            log.error("Chiusura giornale decisionale fallita", e);
        }
    }
}
