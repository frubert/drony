# Registro dei backtest

Tracciamento di ogni lancio: la scelta (perché), il comando, l'esito.
Le righe `[AUTO]` sono appese automaticamente dagli script con timestamp.
I log grezzi di ogni run stanno in `runs/<dir>/runs/<combo>/run.log`.

Convenzioni: tutte le date UTC. Screening = `CANDLE:ONE_HOUR` (veloce, impreciso
sui fill); validazione = `ALL_TICKS`. `jobs=1` sempre (l'account demo va in lock
823 con login concorrenti).

---

## Storico sintetico (round 1-7, ottimizzazione EUR/USD, 12 giu 2026)

Dettaglio completo in `docs/ottimizzazione-eurusd-2026-06.md`.

| Round | Scelta | Esito |
|---|---|---|
| 1-4 | screening filtri EUR/USD | 0 trade — scoperti 3 difetti (weekend doji, celle orario, order size forex) |
| 5 | funnel sbloccato | 48/48 combo con trade |
| 6 | struttura rischio (Floor%, Cap, break-even) | linea Floor%600 vincente in-sample |
| final | ALL_TICKS Q4 2020 + OOS Q2 2021 | C0004 +37.7 OOS, ma poi bocciato |
| verifica 2025 | C0001 (TP20) | +239.4, diventa raccomandato |
| walk-forward 2022-2024 | tutte le varianti | **bocciate**: 2022 letale (DD 591-970), dipendenza dal regime |

Conclusione round 1-7: nessuna config regge il ciclo completo a parametri fissi.
Prossimo: punto 2 (strategyType per lato) + punto 3 (walk-forward con ritaratura).

---

## Sessione 14 giu 2026 — punto 2 e walk-forward

Account demo: DEMO2KnzVY (il precedente era andato in lock 823).

### Punto 2 — strategyType FULL/LONG/SHORT × TP {10,20,40} cross-regime
Scelta: capire se un lato (o un TP) regge tutti gli anni. Ingressi fissi su C0001 (DAILY, candleFilter WEEKENDS, Floor%600). 9 combo, screening CANDLE.
Batch: runs/eurusd_side (combos.csv per la mappa combo→parametri)

| anno | inizio | fine | esito |
|---|---|---|---|
| 2021 | 10:22:23 | 10:22:24 | FAIL: Account locked. Error: 823 |
| 2022 | 10:22:24 | 10:22:25 | FAIL: Account locked. Error: 823 |
| 2023 | 10:22:25 | 10:22:26 | FAIL: Account locked. Error: 823 |
| 2024 | 10:22:26 | 10:22:27 | FAIL: Account locked. Error: 823 |
| 2025 | 10:22:27 | 10:22:28 | FAIL: Account locked. Error: 823 |

**Diagnosi:** non è un lock da connessioni concorrenti — l'header della risposta
Dukascopy dice `statusmessage=[Account is expired]`. Codice 823 = account demo
SCADUTO (le demo durano ~14 giorni). Vale sia per il vecchio username sia per
DEMO2KnzVY. Serve registrare un nuovo account demo fresco su
https://www.dukascopy.com/europe/italiano/forex/demo-fx-account/ e aggiornare
dukascopy.username/password in drony.properties. Tooling punto 2 e walk-forward
pronto, riparte invariato appena le credenziali sono valide.

### Opzione (b) — backtest offline senza account
Scelta: eliminare la dipendenza dall'account demo (scaduto). Nuovo motore
`com.drony.offline.OfflineBacktest`: legge barre da CSV, riusa i 7 filtri reali,
motore ordini semplificato (no pinza/break-even/edge/cluster/tick). Pre-screening
illimitato; i candidati si riconfermano col tester JForex ALL_TICKS.
Dati via `tools/fetch_data.sh` (feed pubblico Dukascopy, nessun account).

Smoke test su CSV sintetico (data/EURUSD_synth_daily.csv, 1304 barre, dati FINTI):
pipeline OK — 9 combo, strategyType discrimina (FULL 42 / LONG 30 / SHORT 14
trade), P&L e drawdown calcolati. Numeri privi di significato (dati sintetici),
serviva solo validare il funzionamento end-to-end.

NB: lo scaricamento dati reali con `npx dukascopy-node` va autorizzato dall'utente
(il classifier blocca l'esecuzione di pacchetti npm esterni). In alternativa,
export ufficiale: https://www.dukascopy.com/swiss/english/marketwatch/historical/

### Ri-verifica account (14 giu, su richiesta: "non dovrebbe avere scadenza")
1 run di test (2024, CANDLE). Esito invariato: server Dukascopy risponde
`statuscode=[823] statusmessage=[Account is expired]`. username=DEMO2KnzVY,
endpoint=platform.dukascopy.com/demo (DEMO). Il messaggio "Account is expired"
è del server, non della libreria. Probabili cause del disallineamento: (a)
credenziali nel file sono di un vecchio demo, non del nuovo account; (b)
l'account valido è di tipo diverso (LIVE / altro) e l'endpoint demo lo rifiuta.
Da chiarire con chi ha fornito l'account. Nessun ulteriore tentativo per ora.
