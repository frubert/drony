#!/usr/bin/env bash
#
# Walk-forward optimization: per ogni finestra ottimizza i parametri sul periodo
# di training, applica SOLO la combinazione migliore sul periodo di test
# (out-of-sample, mai usato per scegliere), e concatena i risultati di test.
#
# È il test di robustezza più onesto: simula l'uso reale (ritari periodicamente
# sui dati passati, operi sui dati futuri che non hai visto). Se la somma dei
# test è positiva e stabile, la strategia regge con ritaratura; se no, i buoni
# risultati in-sample erano overfitting.
#
# Uso:
#   tools/walk_forward.sh <batch_dir> <ranges_file> <method> <windows_file>
#
# windows_file: una finestra per riga "train_from|train_to|test_from|test_to",
# con date in formato "yyyy/MM/dd HH:mm:ss". Esempio fornito in
# tools/wf_windows_eurusd.txt.
#
# Output in <batch_dir>/wf/:
#   train_<testfrom>.csv / test_<testfrom>.csv   classifiche per finestra
#   walkforward.csv                              riepilogo: finestra, best, params, plPips OOS

set -euo pipefail

BATCH_DIR=${1:?batch_dir mancante}
RANGES=${2:?ranges_file mancante}
METHOD=${3:-CANDLE:ONE_HOUR}
WINDOWS=${4:?windows_file mancante}

cd "$(dirname "$0")/.."
JAR=target/drony-4_2-jar-with-dependencies.jar
[ -f "$JAR" ] || { echo "Jar mancante: $JAR"; exit 1; }

# 1. genera il batch una volta sola (stesse combinazioni per ogni finestra)
java -cp "$JAR" com.drony.tools.BatchGenerator \
    --template param/DronyParamV04.xlsx --ranges "$RANGES" --out "$BATCH_DIR"

WF=$BATCH_DIR/wf
mkdir -p "$WF"
SUMMARY=$WF/walkforward.csv
echo "finestra_test;best_combo;params;plPips_train;plPips_oos;trades_oos;maxDD_oos" > "$SUMMARY"

# esegue il batch su una finestra e copia la classifica aggregata in $1
run_window() {
    local dest=$1 from=$2 to=$3
    # jobs=1: l'account demo Dukascopy va in lock (errore 823) con login concorrenti
    tools/run_batch.sh "$BATCH_DIR" "$from" "$to" 1 "$METHOD" EURUSD > /dev/null 2>&1
    cp "$BATCH_DIR/all_results.csv" "$dest"
}

total_oos=0
while IFS='|' read -r tr_from tr_to te_from te_to; do
    [ -z "${tr_from:-}" ] && continue
    case "$tr_from" in \#*) continue;; esac

    label=$(echo "$te_from" | cut -d'/' -f1-2 | tr '/' '-')
    echo ">>> finestra test $label : train [$tr_from .. $tr_to]  test [$te_from .. $te_to]"

    run_window "$WF/train_$label.csv" "$tr_from" "$tr_to"
    # best = combo con plPips massimo nel training (colonna 7)
    best=$(tail -n +2 "$WF/train_$label.csv" | sort -t';' -k7,7 -g -r | head -1 | cut -d';' -f1)
    pl_train=$(awk -F';' -v b="$best" '$1==b {print $7}' "$WF/train_$label.csv")
    params=$(awk -F';' -v b="$best" '$1==b {print $2";"$3";"$4}' "$BATCH_DIR/combos.csv" 2>/dev/null || echo "")

    run_window "$WF/test_$label.csv" "$te_from" "$te_to"
    oos=$(awk -F';' -v b="$best" '$1==b {print $7}' "$WF/test_$label.csv")
    trades=$(awk -F';' -v b="$best" '$1==b {print $4}' "$WF/test_$label.csv")
    dd=$(awk -F';' -v b="$best" '$1==b {print $10}' "$WF/test_$label.csv")

    echo "$label;$best;$params;$pl_train;$oos;$trades;$dd" >> "$SUMMARY"
    total_oos=$(awk -v a="$total_oos" -v b="${oos:-0}" 'BEGIN{print a+b}')
    echo "    best=$best  train=$pl_train  OOS=$oos pips ($trades trade)"
done < "$WINDOWS"

echo
echo "=== WALK-FORWARD ($SUMMARY) ==="
column -s';' -t < "$SUMMARY"
echo
echo "TOTALE out-of-sample concatenato: $total_oos pips"
