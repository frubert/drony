#!/usr/bin/env python3
"""Ricerca parametri Drony con Optuna (TPE) — opzionale, per quando la ricerca
numerica pura diventa il collo di bottiglia rispetto al ciclo diagnostico /optimize.

Ogni trial Optuna = una combinazione; i trial vengono raggruppati in batch da
BATCH_SIZE combinazioni = un solo backtest multi-colonna (ask-and-tell).

Setup:
    pip install optuna

Uso:
    python3 tools/optuna_search.py --trials 120 \
        --from "2020/07/01 00:00:00" --to "2020/12/31 23:59:00" \
        --method CANDLE:ONE_HOUR

Lo spazio di ricerca si definisce in SEARCH_SPACE qui sotto: chiave = etichetta
di riga del template Excel (colonna A), valore = (tipo, argomenti).
ATTENZIONE: il punteggio è plPips in-sample. Il vincitore va SEMPRE rivalidato
con ALL_TICKS e su un periodo out-of-sample (vedi skill /optimize).
"""

import argparse
import csv
import subprocess
import sys
import tempfile
from pathlib import Path

try:
    import optuna
except ImportError:
    sys.exit("optuna non installato: pip install optuna")

ROOT = Path(__file__).resolve().parent.parent
JAR = ROOT / "target" / "drony-4_2-jar-with-dependencies.jar"
TEMPLATE = ROOT / "param" / "DronyParamV04.xlsx"
BATCH_SIZE = 30

# etichetta riga Excel -> (tipo, parametri per optuna)
SEARCH_SPACE = {
    "Body % Min:": ("int", 5, 40, 5),        # min, max, step
    "Body % Max:": ("int", 60, 100, 10),
    "Slope Max:":  ("int", 20, 500, 20),
    "Mod Max :":   ("int", 50, 500, 50),
    "Cap Abs:":    ("int", 5, 60, 5),
    "Floor abs:":  ("int", 0, 100, 10),
    "Indent:":     ("int", 0, 100, 20),
}


def suggest(trial: "optuna.Trial") -> dict:
    combo = {}
    for label, (kind, *args) in SEARCH_SPACE.items():
        key = label  # l'etichetta è anche il nome del parametro optuna
        if kind == "int":
            lo, hi, step = args
            combo[label] = str(trial.suggest_int(key, lo, hi, step=step))
        elif kind == "float":
            lo, hi = args
            combo[label] = str(trial.suggest_float(key, lo, hi))
        elif kind == "cat":
            combo[label] = str(trial.suggest_categorical(key, list(args)))
        else:
            raise ValueError(f"tipo sconosciuto {kind}")
    return combo


def run_batch(combos: list[dict], date_from: str, date_to: str, method: str,
              workdir: Path) -> list[float]:
    """Esegue un backtest con tutte le combos come colonne; ritorna plPips per combo."""

    ranges_file = workdir / "ranges.txt"
    # BatchGenerator fa il prodotto cartesiano: per un batch di combo arbitrarie
    # serve un file per combo... invece scriviamo un range "degenere" per combo
    # e generiamo singolarmente, poi un unico param multi-colonna via colonne singole.
    # Più semplice: un param file per combo, MA così perdiamo il multi-colonna.
    # Soluzione: scriviamo le combo come righe di un csv e usiamo BatchGenerator
    # in modalità --combos (vedi sotto): qui generiamo un file ranges con UNA
    # combinazione per volta non è efficiente, quindi passiamo dal csv combos.
    combos_csv = workdir / "combos_in.csv"
    labels = list(SEARCH_SPACE.keys())
    with combos_csv.open("w", newline="") as fh:
        writer = csv.writer(fh, delimiter=";")
        writer.writerow(labels)
        for combo in combos:
            writer.writerow([combo[l] for l in labels])

    subprocess.run(
        ["java", "-cp", str(JAR), "com.drony.tools.BatchGenerator",
         "--template", str(TEMPLATE), "--combos", str(combos_csv),
         "--out", str(workdir), "--max-cols", str(BATCH_SIZE)],
        check=True, cwd=ROOT)

    results: dict[str, float] = {}
    for param_file in sorted(workdir.glob("param_*.xlsx")):
        out_dir = workdir / "runs" / param_file.stem
        out_dir.mkdir(parents=True, exist_ok=True)
        subprocess.run(
            ["java", "-cp", str(JAR), "com.drony.tester.HeadlessRunner",
             "--param", str(param_file), "--out", str(out_dir),
             "--from", date_from, "--to", date_to, "--method", method],
            check=True, cwd=ROOT)
        with (out_dir / "results.csv").open() as fh:
            for row in csv.DictReader(fh, delimiter=";"):
                results[row["strategia"]] = float(row["plPips"])

    # le combo sono nominate C0001..C000N nell'ordine di scrittura
    return [results[f"C{i + 1:04d}"] for i in range(len(combos))]


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--trials", type=int, default=60)
    parser.add_argument("--from", dest="date_from", required=True)
    parser.add_argument("--to", dest="date_to", required=True)
    parser.add_argument("--method", default="CANDLE:ONE_HOUR")
    args = parser.parse_args()

    study = optuna.create_study(direction="maximize",
                                sampler=optuna.samplers.TPESampler(seed=42))

    done = 0
    while done < args.trials:
        batch = min(BATCH_SIZE, args.trials - done)
        trials = [study.ask() for _ in range(batch)]
        combos = [suggest(t) for t in trials]

        with tempfile.TemporaryDirectory(prefix="drony_optuna_") as tmp:
            scores = run_batch(combos, args.date_from, args.date_to,
                               args.method, Path(tmp))

        for trial, score in zip(trials, scores):
            study.tell(trial, score)
        done += batch
        print(f"[{done}/{args.trials}] best plPips finora: {study.best_value:.1f} "
              f"con {study.best_params}")

    print("\n=== MIGLIORE (in-sample!) ===")
    print(f"plPips: {study.best_value:.1f}")
    for k, v in study.best_params.items():
        print(f"  {k} = {v}")
    print("\nRicordare: rivalidare con ALL_TICKS e su periodo out-of-sample.")


if __name__ == "__main__":
    main()
