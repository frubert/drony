#!/usr/bin/env bash
#
# Lancia in parallelo un backtest headless per ogni param_*.xlsx di un batch
# e aggrega i results.csv in un'unica classifica.
#
# Uso:
#   tools/run_batch.sh <batch_dir> <from> <to> [jobs] [method] [instruments]
#
# Esempio:
#   tools/run_batch.sh runs/batch01 "2020/07/01 00:00:00" "2020/12/31 23:59:00" 4 CANDLE:ONE_HOUR BRENTCMDUSD
#
# Output:
#   <batch_dir>/runs/<param>/...        report, results.csv, decisions.csv per run
#   <batch_dir>/all_results.csv         tutte le strategie, ordinate per plPips decrescente
#
# Nota: ogni job apre una connessione demo Dukascopy; se il server rifiuta
# sessioni multiple, ridurre jobs a 1-2. La cache tick locale è condivisa.

set -euo pipefail

BATCH_DIR=${1:?batch_dir mancante}
FROM=${2:?data inizio mancante}
TO=${3:?data fine mancante}
JOBS=${4:-4}
METHOD=${5:-ALL_TICKS}
INSTRUMENTS=${6:-}

cd "$(dirname "$0")/.."
JAR=target/drony-4_2-jar-with-dependencies.jar
[ -f "$JAR" ] || { echo "Jar mancante: $JAR — eseguire 'mvn package' prima"; exit 1; }

run_one() {
    local param=$1
    local name out
    name=$(basename "$param" .xlsx)
    out=$BATCH_DIR/runs/$name
    mkdir -p "$out"
    local extra=()
    [ -n "$INSTRUMENTS" ] && extra=(--instruments "$INSTRUMENTS")
    if java -cp "$JAR" com.drony.tester.HeadlessRunner \
            --param "$param" --out "$out" \
            --from "$FROM" --to "$TO" --method "$METHOD" \
            "${extra[@]}" > "$out/run.log" 2>&1; then
        echo "OK   $name"
    else
        echo "FAIL $name (vedi $out/run.log)"
    fi
}
export -f run_one
export BATCH_DIR FROM TO METHOD INSTRUMENTS JAR

ls "$BATCH_DIR"/param_*.xlsx | xargs -P "$JOBS" -I{} bash -c 'run_one "$@"' _ {}

# --- aggregazione ---
ALL=$BATCH_DIR/all_results.csv
first=$(ls "$BATCH_DIR"/runs/*/results.csv 2>/dev/null | head -1)
if [ -z "$first" ]; then
    echo "Nessun results.csv prodotto"; exit 1
fi
head -1 "$first" > "$ALL"
tail -q -n +2 "$BATCH_DIR"/runs/*/results.csv \
    | sort -t';' -k7,7 -g -r >> "$ALL"

echo
echo "=== Top 10 per plPips ($ALL) ==="
column -s';' -t < "$ALL" | head -11
