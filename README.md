# Drony

Strategia di trading algoritmico per la piattaforma **Dukascopy JForex**.
Pattern matcher su sequenze di candele: se una serie di filtri configurabili passa,
apre ordini stop (BUYSTOP/SELLSTOP) con stop loss / take profit dinamici,
gestione a cluster e ordini di copertura opzionali (edge order).

Versione perno: **4.2** (ripresa a giugno 2026 dal progetto storico, archivio completo
in `pre_12_06_2026.zip` nella cartella padre).

## Requisiti

- Java 8+ (compila anche con JDK recenti, target 1.8)
- Maven 3.x
- Account **demo** Dukascopy: <https://www.dukascopy.com> (le demo scadono dopo ~14 giorni)

## Setup

```bash
cp drony.properties.example drony.properties
# compilare username/password demo in drony.properties (file in .gitignore, mai committare)
```

Tutta la configurazione (credenziali, strumento, file parametri, cartelle report,
intervallo backtest) sta in `drony.properties`, letta da `com.drony.config.DronyConfig`.
Path alternativo del file: `-Ddrony.config=/percorso/file.properties`.

## Build e run

```bash
mvn clean package
java -jar target/drony-4_2-jar-with-dependencies.jar
```

Si apre la GUI del tester Dukascopy (backtest sull'intervallo configurato).
I report finiscono in `report/` (HTML del tester) e `report/xlsx/` (Excel ordini).

## Struttura

```
src/main/java/com/drony/
├── App.java                  entry point → TesterMainGUIMode
├── DronyV042.java            IStrategy Dukascopy (adapter)
├── config/DronyConfig.java   configurazione esterna
├── stategy/
│   ├── DelegateDrony.java    orchestratore: N strategie da Excel, cluster, report
│   ├── DronyStrategy.java    logica per singola strategia (colonna Excel)
│   ├── test/                 filtri candele: Direction, BodyAbs, BodyPercent,
│   │                         Same, ColorStory, Slope, Mod
│   ├── service/              creazione ordini, SL/TP dinamici
│   ├── edge/                 ordini di copertura (edge order)
│   └── utility/              lettura Excel parametri, scrittura report
└── tester/                   tester GUI Dukascopy
param/                        file Excel parametri (1 colonna = 1 strategia, ~50 righe)
docs/                         PDF spiegazione strategia, appunti, confronti versioni
```

## Parametri Excel

Ogni colonna del file in `param/` definisce una strategia: strumento, periodo,
soglie dei filtri (righe 5–31), SL/TP (14–19), orari trading (23–24), cluster (41–43),
edge order (47–54). Riferimento completo: `stategy/utility/ReaderParam.java` e
`stategy/data/ParamDrony.java`.

## Storia

Il repository originale (Bitbucket, 2019–2021, branch drony04_02/drony04_03/drony05)
non esiste più sul provider; la storia completa è conservata nell'archivio zip.
Nota: il branch `drony04_03` conteneva un'evoluzione dell'edge order
(classe `EdgeOrderToSubmit`, ~340 righe) mai fusa nella 4.2 — recuperabile
dall'archivio se servisse.
