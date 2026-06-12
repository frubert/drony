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
