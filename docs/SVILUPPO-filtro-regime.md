# Sviluppo: filtro di regime (trend) — specifica

> Punto 1 delle direzioni emerse dall'ottimizzazione EUR/USD (giugno 2026).
> A differenza dei punti 2 (strategyType) e 3 (walk-forward), che si applicano
> solo cambiando parametri/orchestrazione, **questo richiede codice nuovo**.

## Perché serve

Il walk-forward 2020-2025 ha mostrato che la strategia, in ogni configurazione
testata, **guadagna poco e spesso nei mercati laterali (2021, 2024, 2025) e
perde molto nei trend forti (2022 EUR sotto parità: drawdown 591-970 pips)**.
La struttura stop-largo + break-even è un'aspettativa positiva solo finché il
prezzo oscilla; in un trend direzionale lo stop largo viene preso e cancella
decine di scratch a zero.

Idea: **non operare quando il mercato è in trend forte.** Un filtro che misura
la "forza di trend" della barra corrente e blocca l'ingresso sopra una soglia.
È un ottavo `BarTest`, sullo stesso modello dei sette esistenti, più una riga
parametro letta dall'Excel — esattamente lo schema con cui è stato aggiunto
`candleFilter`.

## Misura del regime: due opzioni

**Opzione A — ADX (consigliata).** L'Average Directional Index è la misura
standard di forza di trend (non di direzione): ADX basso = laterale, ADX alto =
trend. Soglia tipica 25-30. JForex la espone già via `IIndicators.adx(...)`,
quindi niente da calcolare a mano.

**Opzione B — distanza da media mobile.** `|close - SMA(n)| / ATR`: quanto il
prezzo è lontano dalla sua media in unità di volatilità. Più semplice
concettualmente ma meno standard. Usare A salvo motivi specifici.

## Dove agganciare nel codice (percorsi attuali)

1. **Nuovo filtro** `src/main/java/com/drony/strategy/test/BarTrendFilter.java`,
   estende `AbstractBarTest` come gli altri sette. Ma attenzione: i `BarTest`
   attuali lavorano solo su `IBar` e `ParamDrony` (vedi `BarTestInit`), **non
   hanno accesso a `IIndicators`**. L'ADX richiede `IIndicators` + storico.
   Due strade:
   - **(consigliata)** calcolare l'ADX in `DronyStrategy.onBar` *prima* di
     chiamare `roboStrategyBar`, e passare il valore già calcolato dentro
     `BarTestInit` (nuovo campo `double adx`), così il filtro resta una funzione
     pura testabile come gli altri. `this.indicators` è già disponibile in
     `DronyStrategy` (campo a riga 36, inizializzato in `onStart` riga 83).
   - alternativa: dare a `BarTestInit` il riferimento a `IIndicators` (come già
     fa col secondo costruttore per `IHistory`). Meno pulito: lega i test
     all'API Dukascopy e complica i test unitari.

2. **Calcolo ADX in onBar** (`DronyStrategy.java`, dentro `onBar`, dopo il
   check orario e prima di valutare la direzione):
   ```java
   double[] adx = indicators.adx(instrument, period, OfferSide.BID,
       paramDrony.getAdxPeriod(),                       // es. 14
       Filter.ALL_FLATS, 1, bidBar.getTime(), 0);
   double adxNow = adx.length > 0 ? adx[0] : 0;
   ```
   (verificare firma esatta di `IIndicators.adx` nella Javadoc JForex della
   versione in pom: `JForex-API 2.13.99`).

3. **Nuovo parametro Excel** in `ParamDrony`: aggiungere un sotto-record
   `RegimeFilter(boolean active, int adxPeriod, double adxMax)` accanto agli
   altri (vedi i record già presenti: `SequenceFilter`, `PinzaConfig`...).
   Poi un getter delega `getAdxMax()` / `isRegimeFilterActive()`.

4. **Lettura in `ReaderParam`**: tre righe nuove con `optionalStringValue` /
   `numberValue` (il metodo `optionalStringValue` esiste già e tollera
   l'etichetta mancante → retrocompatibile coi file Excel vecchi). Etichette
   suggerite in colonna A: `regimeFilterActive`, `adxPeriod`, `adxMax`.

5. **Il filtro vero** (`BarTrendFilter.testBar`): se `active` e
   `init.getAdx() >= adxMax` → `getFail("Aborting for trend regime ADX %s >= %s")`,
   altrimenti `getOk()`. Aggiungerlo alla lista in
   `DronyStrategy.validateCurrentBar` (è un test di contesto sulla barra
   corrente, come BarSlope/BarMod).

6. **Logging**: nessun lavoro extra — passando per il flusso `BarTestResult`,
   lo scarto finisce già nel giornale decisionale come `SCARTATO` con il
   messaggio del filtro (vedi `DronyStrategy.failAndLog`).

## Test

- **Unitario** (`src/test/java/com/drony/strategy/test/`): sul modello di
  `BarFiltersTest` + `TestSupport`. Poiché l'ADX arriva già calcolato in
  `BarTestInit`, basta costruire un init con `adx` alto/basso e verificare
  fail/pass — nessuna connessione Dukascopy necessaria. Aggiungere il campo
  `adx` al `TestSupport.param(...)` / costruttori.
- **Backtest**: rifare il **walk-forward completo 2020-2025** (`tools/walk_forward.sh`)
  includendo `adxMax` nei range (es. `adxMax = 20, 25, 30, 100` dove 100 =
  filtro di fatto spento, per confronto). Criterio di successo: il totale
  out-of-sample concatenato diventa positivo **e** il 2022 smette di essere
  catastrofico. Se il 2022 resta negativo, l'ADX da solo non basta e va
  combinato con strategyType per lato.

## Criterio di accettazione

Il filtro è utile solo se, a parità di tutto il resto, **riduce il drawdown del
2022/2023 senza azzerare i guadagni del 2024/2025**. Va validato sull'intero
ciclo, non su un anno: un filtro che migliora il 2022 ma uccide il 2025 non
serve. Default retrocompatibile: `regimeFilterActive = false` → comportamento
identico a oggi.

## Stima

~1 sessione di lavoro: 1 classe filtro + 1 record/getter in ParamDrony + 3
righe in ReaderParam + il calcolo ADX in onBar + 1 campo in BarTestInit + test
unitari. Il grosso del tempo è poi nel walk-forward di validazione (vincolato
dalla velocità dei backtest e dai limiti dell'account demo, non dal codice).
