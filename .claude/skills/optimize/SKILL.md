---
name: optimize
description: Ciclo di ottimizzazione parametri Drony guidato dalla diagnosi - genera batch di combinazioni, lancia backtest headless paralleli, legge results.csv e decisions.csv, diagnostica perché le combinazioni falliscono e propone il batch successivo. Usare quando l'utente vuole cercare/ottimizzare parametri della strategia, lanciare batch di backtest, o analizzare risultati di run. Trigger - /optimize, "ottimizza i parametri", "cerca i parametri migliori", "lancia un batch".
---

# Ottimizzazione parametri Drony

Ciclo iterativo: genera → esegui → diagnostica → restringi. La diagnosi sul
`decisions.csv` è ciò che distingue questo ciclo da una ricerca cieca: non
"questa combinazione perde", ma "questa combinazione non apre ordini perché
il 95% dei segnali muore su BarSlope".

## Prerequisiti

- `drony.properties` con credenziali demo valide
- `mvn package` eseguito (`target/drony-4_2-jar-with-dependencies.jar` presente)
- Capire i parametri: etichette in colonna A di `param/*.xlsx`, semantica in
  `docs/NOTE-COMPORTAMENTO.md` e `src/test/java/com/drony/strategy/test/BarFiltersTest.java`

## Disciplina anti-overfitting (NON derogabile)

1. Dividere SEMPRE il periodo: **in-sample** (ottimizzazione) e **out-of-sample**
   (validazione finale, mai usato durante la ricerca). Es: ottimizza su
   2020/07–2020/12, valida su 2021/01–2021/03.
2. Lo screening usa `CANDLE:ONE_HOUR` (veloce, impreciso sui fill); i candidati
   finali vanno SEMPRE riconfermati con `ALL_TICKS`.
3. Diffidare dei vincitori con pochi trade (< 20): chiedere all'utente prima di
   proporli come buoni.
4. Riportare sempre il risultato out-of-sample accanto a quello in-sample.

## Ciclo operativo

### 1. Genera il batch

Scrivere un file di range (etichette come in colonna A del template):

```
# ranges.txt
Body % Min: = 10, 20, 30
Slope Max:  = 20, 50, 100
Cap Abs:    = 10, 20, 40
```

```bash
java -cp target/drony-4_2-jar-with-dependencies.jar com.drony.tools.BatchGenerator \
  --template param/DronyParamV04.xlsx --ranges ranges.txt --out runs/batch_NN
```

Produce `param_*.xlsx` (max 30 combinazioni per file = 30 strategie per
backtest, un solo passaggio dati) e `combos.csv` (mappa combinazione → valori).
Tenere il prodotto cartesiano sotto ~200 combinazioni per iterazione.

### 2. Esegui

```bash
tools/run_batch.sh runs/batch_NN "2020/07/01 00:00:00" "2020/12/31 23:59:00" 4 CANDLE:ONE_HOUR
```

Stampa la top 10 e scrive `runs/batch_NN/all_results.csv`
(strategia;strumento;periodo;trades;vincenti;winRate;plPips;plUsd;profitFactor;maxDrawdownPips).
Se i run falliscono per sessioni Dukascopy multiple rifiutate, ridurre jobs a 1-2.

### 3. Diagnostica (il passo che conta)

Incrociare `all_results.csv` con `combos.csv` (chiave = nome combo) per vedere
quali PARAMETRI distinguono i vincitori dai perdenti, non solo chi ha vinto.

Per le combinazioni con 0 trade o poche aperture, aggregare il giornale decisionale:

```bash
# Distribuzione esiti per combo
awk -F';' 'NR>1 {print $2, $4}' runs/batch_NN/runs/*/decisions.csv | sort | uniq -c | sort -rn | head -20
# Quale filtro scarta di più
awk -F';' '$4=="SCARTATO" {print $5}' runs/batch_NN/runs/*/decisions.csv | cut -c1-60 | sort | uniq -c | sort -rn | head
```

Pattern tipici e risposta:
- quasi tutto SCARTATO da un filtro → range di quel filtro fuori scala per lo
  strumento: allargarlo, non toccare il resto
- molti BLOCCATO "fuori orario" → finestra di trading troppo stretta per il periodo
- molti ORDINE ma pochi FILL → indent troppo lontano dal prezzo (ordini stop mai raggiunti)
- molti FILL ma CHIUSURA "BY BAR NUMBER EXCESSIVE" → orderNumMaxBar troppo basso o TP irraggiungibile
- RIFIUTATO dal cluster → le combo si bloccano a vicenda: usare cluster vuoti nei batch

### 3b. Walk-forward (validazione di robustezza nel tempo)

Quando una configurazione sembra buona su un periodo, NON fidarsi: usare
`tools/walk_forward.sh` per simulare la ritaratura periodica reale. Ottimizza su
una finestra di training, applica solo il migliore sulla finestra di test
successiva (mai vista), concatena i test. Se il totale out-of-sample è positivo
e stabile la strategia regge; se no, era overfitting/dipendenza dal regime.

```bash
tools/walk_forward.sh runs/wf runs/ranges_eurusd_wf.txt CANDLE:ONE_HOUR tools/wf_windows_eurusd.txt
```

`strategyType` (FULL/LONG/SHORT) è un parametro: includerlo nei range del
walk-forward permette alla ritaratura di scegliere anche il lato per finestra.

### 4. Itera o concludi

- Restringere i range intorno ai vincitori, o spostare la ricerca sui parametri
  che la diagnosi indica come vincolanti. Nuova iterazione dal passo 1.
- Quando 1-3 candidate sono stabili: rivalidarle con ALL_TICKS sul periodo
  in-sample, poi UNA SOLA volta su out-of-sample. Riportare entrambi i numeri.

## Report finale all'utente

Tabella: combo, parametri, trades, plPips in-sample, plPips out-of-sample,
profit factor, max drawdown + 2-3 righe di diagnosi su cosa rende vincenti i
vincitori. Mai consegnare un vincitore senza out-of-sample.
