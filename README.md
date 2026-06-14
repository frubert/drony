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

### Giornale decisionale

Con `strategy.decisionsFile` valorizzato in `drony.properties`, ogni decisione
dell'algoritmo finisce in un CSV (una riga per barra × strategia): segnale
scartato e quale filtro l'ha fermato con i valori misurati, blocchi (orario,
ordine già attivo, doji, tipo strategia), ordini creati/rifiutati dal cluster,
fill e chiusure con motivazione. È lo strumento per rispondere a "perché su
questa barra non è successo niente?".

### Test

`mvn test` — oltre al test di regressione sui parametri Excel,
`BarFiltersTest` esercita i 7 filtri candela con barre sintetiche, senza
connessione Dukascopy: documentazione eseguibile delle regole di ingresso.

## Ottimizzazione parametri

Pipeline per cercare i parametri in batch, senza GUI:

```bash
# 1. backtest singolo headless
java -cp target/drony-4_2-jar-with-dependencies.jar com.drony.tester.HeadlessRunner \
  --param param/DronyParamV04.xlsx --out runs/test01 \
  --from "2020/07/01 00:00:00" --to "2020/12/31 23:59:00" --method CANDLE:ONE_HOUR

# 2. genera combinazioni da range (una per colonna Excel = un solo passaggio dati)
java -cp target/drony-4_2-jar-with-dependencies.jar com.drony.tools.BatchGenerator \
  --template param/DronyParamV04.xlsx --ranges ranges.txt --out runs/batch01

# 3. esegui il batch in parallelo e aggrega la classifica
tools/run_batch.sh runs/batch01 "2020/07/01 00:00:00" "2020/12/31 23:59:00" 4 CANDLE:ONE_HOUR
```

Ogni run produce `results.csv` (trades, win rate, plPips, profit factor, max
drawdown per strategia) e `decisions.csv` per la diagnosi. `--method` regola
velocità/precisione: `CANDLE:ONE_HOUR` per screening (~10-50× più veloce),
`ALL_TICKS` per la validazione finale.

Il ciclo guidato dall'AI (genera → esegui → diagnostica dal giornale
decisionale → restringi, con validazione out-of-sample) è la skill
**`/optimize`** di Claude Code (`.claude/skills/optimize/`). Per la ricerca
numerica pura c'è `tools/optuna_search.py` (richiede `pip install optuna`).

Per la robustezza nel tempo, `tools/walk_forward.sh` ottimizza su finestre di
training e applica i vincitori su finestre di test mai viste, concatenando
l'out-of-sample (ritaratura periodica simulata). Esiti dell'ottimizzazione
EUR/USD e specifica del filtro di regime ancora da implementare in
`docs/ottimizzazione-eurusd-2026-06.md` e `docs/SVILUPPO-filtro-regime.md`.

> Nota account demo Dukascopy: i backtest concorrenti (jobs > 1) o troppi login
> ravvicinati possono bloccare l'account (errore 823). Usare jobs=1 e, se
> bloccato, ri-autenticarsi dalla piattaforma JForex e attendere.

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

## Comportamenti non ovvi

Alcune scelte di comportamento intenzionali ma non evidenti dal codice
(calcolo `mod`, filtri shadow, edge order, cluster) sono documentate in
[docs/NOTE-COMPORTAMENTO.md](docs/NOTE-COMPORTAMENTO.md). Leggerlo prima
di modificare la logica di trading.

## Storia

Il repository originale (Bitbucket, 2019–2021, branch drony04_02/drony04_03/drony05)
non esiste più sul provider; la storia completa è conservata nell'archivio zip.
Nota: il branch `drony04_03` conteneva un'evoluzione dell'edge order
(classe `EdgeOrderToSubmit`, ~340 righe) mai fusa nella 4.2 — recuperabile
dall'archivio se servisse.
