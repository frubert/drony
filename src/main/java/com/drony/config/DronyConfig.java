package com.drony.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configurazione esterna caricata da file properties.
 * Path del file: system property -Ddrony.config=..., altrimenti ./drony.properties
 */
public class DronyConfig {

    private static final String CONFIG_PATH_PROPERTY = "drony.config";
    private static final String DEFAULT_CONFIG_FILE = "drony.properties";

    private static DronyConfig instance;

    private final Properties props = new Properties();

    private DronyConfig(File configFile) {
        if (configFile.exists()) {
            try (InputStream in = new FileInputStream(configFile)) {
                props.load(in);
            } catch (IOException e) {
                throw new IllegalStateException("Impossibile leggere il file di configurazione: " + configFile.getAbsolutePath(), e);
            }
        } else {
            throw new IllegalStateException("File di configurazione non trovato: " + configFile.getAbsolutePath()
                    + " (copiare drony.properties.example in drony.properties e compilare le credenziali)");
        }
    }

    public static synchronized DronyConfig get() {
        if (instance == null) {
            String path = System.getProperty(CONFIG_PATH_PROPERTY, DEFAULT_CONFIG_FILE);
            instance = new DronyConfig(new File(path));
        }
        return instance;
    }

    private String required(String key) {
        String value = props.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("Proprietà obbligatoria mancante in drony.properties: " + key);
        }
        return value.trim();
    }

    private String optional(String key, String defaultValue) {
        String value = props.getProperty(key);
        return (value == null || value.trim().isEmpty()) ? defaultValue : value.trim();
    }

    public String jnlpUrl() {
        return optional("dukascopy.jnlpUrl", "http://platform.dukascopy.com/demo/jforex.jnlp");
    }

    public String userName() {
        return required("dukascopy.username");
    }

    public String password() {
        return required("dukascopy.password");
    }

    public String instrument() {
        return optional("strategy.instrument", "BRENTCMDUSD");
    }

    public File paramFile() {
        return new File(required("strategy.paramFile"));
    }

    public File resultDir() {
        return new File(optional("strategy.resultDir", "report/xlsx"));
    }

    public String reportFileName() {
        return optional("strategy.reportFileName", "report/report");
    }

    /** File CSV del giornale decisionale, o null se non configurato (log disattivato). */
    public File decisionsFile() {
        String path = optional("strategy.decisionsFile", "");
        return path.isEmpty() ? null : new File(path);
    }

    public boolean verbose() {
        return Boolean.parseBoolean(optional("strategy.verbose", "true"));
    }

    public String backtestFrom() {
        return optional("backtest.from", "2020/06/24 00:00:00");
    }

    public String backtestTo() {
        return optional("backtest.to", "2021/03/30 23:59:00");
    }
}
