# Ottimizzazione EUR/USD — giugno 2026

## Configurazione consegnata (colonna C0004 di `param/ParamEURUSD_v1.xlsx`)

Differenze dal template Brent: **EURUSD DAILY**, `candleFilter WEEKENDS`,
`OrderSize 0.01`, `Body abs Min 1`, `Body % Min 20`, `numBodyShadowBars 5`,
`minBodyShadow% 0`, `Floor % 600`, **`Cap Abs 40`**, `Cap % 0`, break-even
`%a=100 / %b=25`. (Le colonne C0001-C0003 sono le varianti di TP testate.)

## Numeri (tutti ALL_TICKS tranne lo screening)

| Finestra | Ruolo | C0004 |
|---|---|---|
| 2019-2020 (candele 1H) | screening in-sample | linea Floor%600: +260/+550 pips, 103-114 trade |
| Q4 2020 (tick) | conferma in-sample | **+72.4 pips**, 11/11 |
| **Q2 2021 (tick)** | **out-of-sample mai visto** | **+37.7 pips**, 7/7 |

Profilo dei trade: la maggioranza chiude a 0.0 (il break-even sposta lo SL
sull'apertura appena il trade va in utile), ~1 trade per trimestre corre al TP.

## Avvertenze oneste

1. **Rischio di coda reale**: lo SL largo (6×mod ≈ −170 pips) può scattare
   prima che il break-even si armi — è successo nel Q1 2021 (−173.6, finestra
   bruciata dalla prima iterazione). Frequenza osservata ~3 stop in 2 anni.
2. **Pochi trade**: 7-12 per trimestre; la significatività statistica è bassa,
   un trimestre fortunato non è una garanzia.
3. Validato su **demo** (spread/fill demo). Prima di soldi veri: walk-forward
   su più anni (2017-2018, 2022+) e paper trading live.

## Storia del ciclo (7 round)

I round a zero trade hanno scovato 4 difetti sistemici, tutti corretti e
committati: candele weekend doji che uccidevano i segnali del lunedì (→
parametro `candleFilter`), celle orario corrotte dal generatore (finestra
00:00-00:00), order size Brent sotto il minimo forex (submit silenziose, ora
loggate nel giornale), parser ranges ed etichette con '='. Round 6 ha mostrato
che il margine vero è nella struttura del rischio: stop stretti (Floor% 50-200)
sono strutturalmente perdenti per questa strategia (−936 pips il peggiore),
lo stop largo con break-even è l'assetto che sopravvive.

## Verifica 2025 (anno intero, tick reali — aggiunta 12/06/2026)

| Combo | TP | 2025: trades | plPips | Stop subiti | DD pips |
|---|---|---|---|---|---|
| **C0001** | **Cap Abs 20** | 61 | **+239.4** | **0** | **0** |
| C0002 | 20 + Cap% 40 | 58 | +169.1 | 2 | 139.7 |
| C0003 | Cap Abs 10 | 62 | +118.5 | 0 | 0 |
| C0004 | Cap Abs 40 | 57 | +37.8 | 3 | 197.2 |

Tutte e quattro le varianti positive anche nel 2025 (regime di mercato a 4-5
anni dall'ottimizzazione, volume di setup ~8× il 2021): la struttura regge.

**Raccomandazione aggiornata: C0001 (Cap Abs 20)**, non più C0004. Motivo
strutturale, non solo statistico: il trigger del break-even è proporzionale al
TP (%a = 100% del delta TP), quindi con TP 20 la protezione si arma a metà
strada rispetto al TP 40 — finestra di esposizione allo stop dimezzata. Nel
2025: C0001 zero stop subiti su 61 trade, C0004 tre stop (−139.7, −123.8,
−57.5) che hanno quasi azzerato l'anno.

Storico completo C0001: Q4 2020 +34.1 · Q2 2021 +19.5 · 2025 +239.4 — sempre
positivo, mai uno stop pieno nelle finestre a tick.

## Walk-forward 2022-2024 (tick reali — aggiunta 12/06/2026, sera)

| Anno | C0001 (TP20) | C0002 (TP20+%40) | C0003 (TP10) | C0004 (TP40) |
|---|---|---|---|---|
| Q4 2020 | +34.1 | +64.4 | +13.9 | +72.4 |
| Q2 2021 | +19.5 | +37.7 | +15.1 | +37.7 |
| **2022** | **−250.5** | **−385.1** | **−417.8** | **−613.0** |
| **2023** | **−141.8** | **−242.9** | +103.6 | **−253.1** |
| 2024 | +76.2 | +121.6 | +0.7 | +175.2 |
| 2025 | +239.4 | +169.1 | +118.5 | +37.8 |
| **Totale** | **≈ −23** | **≈ −235** | **≈ −166** | **≈ −543** |

### Verdetto finale: NON consegnabile così com'è

Nessuna variante sopravvive al walk-forward completo. Il 2022 (crollo EUR
sotto la parità, trend a senso unico) è letale per la struttura stop-largo +
break-even: drawdown 591-970 pips. Il 2023 conferma. I risultati positivi di
2021/2024/2025 sono dipendenza dal regime (anni laterali o di trend mite),
non robustezza.

Lezione: la strategia in questa forma guadagna poco e spesso nei mercati
laterali e perde molto nei trend forti. Possibili direzioni future (non
testate): un filtro di regime/trend che spenga la strategia nei trend forti,
varianti LONG/SHORT asimmetriche, o walk-forward optimization con ritaratura
periodica. Qualunque sviluppo deve essere rivalidato sull'intero 2020-2025.
