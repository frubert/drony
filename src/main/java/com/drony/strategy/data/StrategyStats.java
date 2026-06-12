package com.drony.strategy.data;

import java.util.Locale;

/**
 * Statistiche aggregate di una strategia su un run, aggiornate alla chiusura
 * di ogni ordine. Finiscono in results.csv: una riga per strategia, leggibile
 * da script e dall'ottimizzatore.
 */
public class StrategyStats {

    private final String name;
    private final String instrument;
    private final String period;

    private int trades;
    private int wins;
    private double plPips;
    private double plUsd;
    private double grossWinPips;
    private double grossLossPips;

    /* equity in pips per il calcolo del max drawdown */
    private double equityPeak;
    private double maxDrawdownPips;

    public StrategyStats(String name, String instrument, String period) {
        this.name = name;
        this.instrument = instrument;
        this.period = period;
    }

    public void onOrderClosed(double profitLossPips, double profitLossUsd) {
        trades++;
        plPips += profitLossPips;
        plUsd += profitLossUsd;

        if (profitLossPips >= 0) {
            wins++;
            grossWinPips += profitLossPips;
        } else {
            grossLossPips += -profitLossPips;
        }

        equityPeak = Math.max(equityPeak, plPips);
        maxDrawdownPips = Math.max(maxDrawdownPips, equityPeak - plPips);
    }

    public static String csvHeader() {
        return "strategia;strumento;periodo;trades;vincenti;winRate;plPips;plUsd;profitFactor;maxDrawdownPips";
    }

    public String toCsvRow() {
        double winRate = trades == 0 ? 0 : (double) wins / trades * 100;
        double profitFactor = grossLossPips == 0
                ? (grossWinPips > 0 ? Double.POSITIVE_INFINITY : 0)
                : grossWinPips / grossLossPips;
        return String.format(Locale.US, "%s;%s;%s;%d;%d;%.1f;%.1f;%.2f;%.2f;%.1f",
                name.replace(';', ','), instrument, period,
                trades, wins, winRate, plPips, plUsd, profitFactor, maxDrawdownPips);
    }

    public int getTrades() {
        return trades;
    }

    public double getPlPips() {
        return plPips;
    }
}
