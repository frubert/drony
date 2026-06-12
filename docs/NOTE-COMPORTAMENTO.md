# Note di comportamento — particolarità intenzionali

Documento di tracciamento delle scelte di comportamento non ovvie emerse durante
il refactoring di giugno 2026. Questi punti **sono considerati voluti** (conferma
di Francesco, 12/06/2026): non vanno "corretti" senza una decisione esplicita,
perché cambiarli cambia i risultati di trading.

Ogni nota indica dove vive il comportamento nel codice attuale, così resta
rintracciabile anche dopo futuri refactoring.

---

## 1. Filtro shadow sugli ordini pendenti: flag e soglia da parametri diversi

**Dove**: `StopLossTakeProfitService.closePendingOrderIfShadowTooSmall()`

Per gli ordini non ancora fillati, nelle prime `numBodyShadowBars` barre:

- il **flag di attivazione** del controllo percentuale è `minBodyShadowPercentage != 0`
  (parametro Excel `minBodyShadow%`, riga usata anche dal test storico `BarSame`);
- la **soglia confrontata** è però `minFutureBodyShadowPercentage`
  (parametro Excel `minFutureBodyShadow%`).

In pratica: il controllo "future shadow" percentuale si accende col parametro
delle barre *passate* ma usa il valore delle barre *future*. Con
`minBodyShadow% = 0` il controllo future percentuale non scatta mai, anche se
`minFutureBodyShadow%` è valorizzato.

Il secondo controllo (assoluto in pips) è invece coerente: flag e soglia sono
entrambi `minFutureBodyShadow`.

**Stato**: voluto. Se in futuro si volesse rendere il flag coerente
(`minFutureBodyShadowPercentage != 0`), va rifatto un backtest comparativo.

---

## 2. Calcolo di `mod` diverso tra filtro e creazione ordine

`mod` rappresenta l'escursione di prezzo della sequenza, ma è calcolato in due
modi diversi nei due punti in cui è usato:

| Dove | Formula | Barre coinvolte |
|---|---|---|
| `BarMod.testBar()` (filtro) | `abs(open(prima barra della sequenza) − close(barra corrente))` | `backwardBars.get(0)` → barra corrente |
| `DronyOrderService.createOrder()` (SL/TP e edge) | `abs(close(barra corrente) − open(prevBar))` | `prevBar = historyBars.get(size − min(size, N))` |

Le due "prime barre" non sono necessariamente la stessa: il filtro usa la prima
delle backward bars caricate per i test (dipende da `maxBars` =
max(`numColorStoryBars`, `numBodyShadowBars`)), la creazione ordine usa la prima
delle `N` barre della sequenza. Quindi il `mod` validato dal filtro
(`Mod Min/Max`) e il `mod` che dimensiona SL/TP (`Cap %`, `Floor %`) e l'edge
order possono avere valori diversi sulla stessa barra.

**Stato**: voluto. Il filtro valuta l'escursione "storica lunga", l'ordine si
dimensiona sull'escursione della sequenza operativa.

---

## Altre particolarità preservate (minori)

- **`waitNBarPinza = 0`**: la pinza (aggiornamento SL/TP) parte dalla barra
  successiva alla prima (`numBars <= waitNBarPinza` → salta). Con 0 quindi la
  prima barra dopo il fill non aggiorna mai. Comportamento storico invariato.
- **`removeEdgeOrder` non rimuove l'entry dalla mappa** (`EdgeOrderService`):
  alla chiusura dell'ordine principale l'edge viene chiuso solo se in perdita
  (`P&L <= 0`), e l'entry resta in `edgeMaps`. Già così nell'originale (la
  rimozione era commentata): un edge in profitto sopravvive al main e
  `manageActiveEdge` può ricrearne uno se il main risulta ancora FILLED.
- **GTT degli ordini edge = 60 giorni** dal fill del principale
  (`EdgeOrderService.EDGE_GTT_MILLIS`).
- **Cluster**: il massimo ordini di un cluster è il **massimo** tra i
  `Max order by cluster` di tutte le strategie che lo usano
  (`ClusterManager.registerCluster`).
- **Colonna B dell'Excel parametri è template/note**: le strategie vere partono
  dalla colonna C (`ReaderParam.FIRST_STRATEGY_COL = 2`).
- **Barre doji** (open == close): `BarUtility.getBarColor` ritorna
  `DirectionEnum.DOJI`; in `onBar` una doji non innesca né ramo BUY né SELL,
  come quando ritornava `null`.

---

*Aggiornare questo documento quando una di queste scelte viene modificata
deliberatamente, indicando data e motivo.*
