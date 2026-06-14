#!/usr/bin/env bash
#
# Scarica dati storici Dukascopy in CSV per il backtest offline, SENZA account
# (usa il feed pubblico via dukascopy-node). Richiede node/npx.
#
# Uso:
#   tools/fetch_data.sh <symbol> <from> <to> <timeframe> [outdir]
#
# Esempio (EUR/USD daily 2019-2025):
#   tools/fetch_data.sh eurusd 2019-01-01 2025-12-31 d1 data
#
# timeframe dukascopy-node: m1 m5 m15 m30 h1 h4 d1 mn1
# Output: <outdir>/<symbol>-<tf>-<from>-<to>.csv (formato timestamp,open,high,low,close,volume)

set -euo pipefail

SYMBOL=${1:?symbol mancante (es. eurusd)}
FROM=${2:?data inizio mancante (yyyy-mm-dd)}
TO=${3:?data fine mancante (yyyy-mm-dd)}
TF=${4:-d1}
OUTDIR=${5:-data}

command -v npx >/dev/null || { echo "npx non trovato: installare Node.js"; exit 1; }
mkdir -p "$OUTDIR"

echo "Scarico $SYMBOL $TF $FROM..$TO (feed pubblico Dukascopy, nessun account)"
npx --yes dukascopy-node \
    -i "$SYMBOL" -from "$FROM" -to "$TO" -t "$TF" \
    -f csv -dir "$OUTDIR" -fl true

echo "Fatto. CSV in $OUTDIR/"
ls -la "$OUTDIR"/*.csv 2>/dev/null | tail -3
