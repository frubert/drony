package com.drony.strategy.utility;

import com.dukascopy.api.Instrument;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class Utility {

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss dd.MM.YY");

    public static String formatDateTime(long datetime) {
        if (datetime == 0) {
            return "";
        }
        return DATE_FORMAT.format(datetime);
    }


    public static double roundByDefaultPrecision(double value, Instrument instrument) {
        int scale = (int) Math.pow(10, instrument.getPipScale());
        return (double) Math.round(value * scale) / scale;
    }

    public static String generateRandom(int n) {
        int randomNumber = (int) (Math.random() * n);
        String answer = "" + randomNumber;
        if (answer.length() > 3) {
            answer = answer.substring(0, 4);
        }
        return answer;
    }

    public static String customFormat(Instrument instrument, String str, Object... args) {
        String scale = String.valueOf(instrument.getPipScale());
        str = str.replace("%pipscale%", "%." + scale + "f");
        return String.format(str, args);
    }

    public static double fromPriceToPip(double priceD, Instrument instrument){
        if (priceD == 0D) return 0D;

        BigDecimal price = new BigDecimal(priceD,  MathContext.DECIMAL64);
        BigDecimal pipvalue = new BigDecimal(instrument.getPipValue(),  MathContext.DECIMAL64);

        return price.divide(pipvalue, instrument.getPipScale(), RoundingMode.HALF_EVEN).doubleValue();
    }

    public static double fromPipToPrice(double pipsD, Instrument instrument){
        if (pipsD == 0D) return 0D;
        BigDecimal pips = new BigDecimal(pipsD,  MathContext.DECIMAL64);
        BigDecimal pipvalue = new BigDecimal(instrument.getPipValue(),  MathContext.DECIMAL64);

        return pips.multiply(pipvalue).doubleValue();
    }
}
