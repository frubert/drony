package com.drony.strategy.utility;

import com.drony.strategy.data.ParamDrony;
import com.drony.utility.data.U;
import com.dukascopy.api.IConsole;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.Period;
import org.dhatim.fastexcel.reader.Cell;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;
import org.dhatim.fastexcel.reader.Sheet;

import java.io.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReaderParam {

    private List<ParamDrony> dronies;

    public ReaderParam(File file, IConsole console) throws IOException {

        try (InputStream is = new FileInputStream(file); ReadableWorkbook wb = new ReadableWorkbook(is)) {
            Sheet sheet = wb.getFirstSheet();
            try {
                dronies = readRow(sheet.read(), console);
            } catch (Exception e){
                console.getErr().println(e.getMessage());
                console.getErr().println(e);
            }
        }
    }

    public List<ParamDrony> getDronies() {
        return dronies;
    }

    private List<ParamDrony> readRow(List<Row> rows, IConsole console) {

        List<ParamDrony> params = new ArrayList<>();

        int colIndex = 2;
        while (rows.get(1).getCellAsString(colIndex).isPresent()
                && !U.isEmptyOrNull(rows.get(1).getCellAsString(colIndex).get())) {

            ParamDrony param = new ParamDrony();

            param.setName(rows.get(1).getCellAsString(colIndex).orElse(""));
            param.setSelectedInstrument(Instrument.valueOf(U.clean(rows.get(2).getCellAsString(colIndex).orElse("EURUSD"))));
            param.setSelectedPeriod(Period.valueOf(U.clean(rows.get(3).getCellAsString(colIndex).orElse("ONE_HOUR"))));
            param.setOrderSize(rows.get(4).getCellAsNumber(colIndex).orElse(new BigDecimal("0.2")).doubleValue());
            param.setN(rows.get(5).getCellAsNumber(colIndex).orElse(new BigDecimal("2")).intValue());

            param.setBody_perc_min(rows.get(6).getCellAsNumber(colIndex).orElse(new BigDecimal("10")).doubleValue());
            param.setBody_perc_max(rows.get(7).getCellAsNumber(colIndex).orElse(new BigDecimal("95")).doubleValue());
            param.setMod_min(rows.get(8).getCellAsNumber(colIndex).orElse(new BigDecimal("10")).doubleValue());
            param.setMod_max(rows.get(9).getCellAsNumber(colIndex).orElse(new BigDecimal("100")).doubleValue());
            param.setBody_abs_min(rows.get(10).getCellAsNumber(colIndex).orElse(new BigDecimal("0.5")).doubleValue());
            param.setBody_abs_max(rows.get(11).getCellAsNumber(colIndex).orElse(new BigDecimal("50")).doubleValue());

            param.setIndent(rows.get(12).getCellAsNumber(colIndex).orElse(new BigDecimal("0")).doubleValue());
            param.setIndentPercentPennachio(rows.get(13).getCellAsNumber(colIndex).orElse(new BigDecimal("0")).doubleValue());

            param.setCap_abs(rows.get(14).getCellAsNumber(colIndex).orElse(new BigDecimal("20")).doubleValue());
            param.setCap_perc(rows.get(15).getCellAsNumber(colIndex).orElse(new BigDecimal("0")).doubleValue());
            param.setCap_attn(rows.get(16).getCellAsNumber(colIndex).orElse(new BigDecimal("90")).doubleValue());

            param.setFloor_abs(rows.get(17).getCellAsNumber(colIndex).orElse(new BigDecimal("40")).doubleValue());
            param.setFloor_perc(rows.get(18).getCellAsNumber(colIndex).orElse(new BigDecimal("0")).doubleValue());
            param.setFloor_attn(rows.get(19).getCellAsNumber(colIndex).orElse(new BigDecimal("10")).doubleValue());

            param.setSlope_min(rows.get(20).getCellAsNumber(colIndex).orElse(new BigDecimal("1")).doubleValue());
            param.setSlope_max(rows.get(21).getCellAsNumber(colIndex).orElse(new BigDecimal("20")).doubleValue());

            param.setSlippage(rows.get(22).getCellAsNumber(colIndex).orElse(new BigDecimal("0")).doubleValue());

            param.setStartTradingTime(rows.get(23).getCellAsDate(colIndex).orElse(LocalDateTime.of(1999,1,1, 7 , 0)).toLocalTime());
            param.setEndTradingTime(rows.get(24).getCellAsDate(colIndex).orElse(LocalDateTime.of(1999,1,1, 17 , 0)).toLocalTime());

            param.setNumCandlesValid(rows.get(25).getCellAsNumber(colIndex).orElse(new BigDecimal("4")).intValue());
            param.setStrategyType(U.clean(rows.get(26).getCellAsString(colIndex).orElse("FULL")));

            param.setNumBodyShadowBars(rows.get(27).getCellAsNumber(colIndex).orElse(new BigDecimal("1")).intValue());
            param.setMinBodyShadowPercentage(rows.get(28).getCellAsNumber(colIndex).orElse(new BigDecimal("20")).doubleValue());
            param.setMinBodyShadow(rows.get(29).getCellAsNumber(colIndex).orElse(new BigDecimal("0")).doubleValue());
            param.setMinFutureBodyShadowPercentage(rows.get(30).getCellAsNumber(colIndex).orElse(new BigDecimal("0")).doubleValue());
            param.setMinFutureBodyShadow(rows.get(31).getCellAsNumber(colIndex).orElse(new BigDecimal("0")).doubleValue());

            param.setMacroPL(parseBoolean(rows.get(32).getCell(colIndex), false));

            param.setMacroPLProfit(rows.get(33).getCellAsNumber(colIndex).orElse(new BigDecimal("30")).doubleValue());
            param.setMacroPLLoss(rows.get(34).getCellAsNumber(colIndex).orElse(new BigDecimal("200")).doubleValue());
            param.setNumColorStoryBars(rows.get(35).getCellAsNumber(colIndex).orElse(new BigDecimal("7")).intValue());

            param.setColorStorySameBars(rows.get(36).getCellAsNumber(colIndex).orElse(new BigDecimal("7")).doubleValue());
            param.setAttivaMonotona(parseBoolean(rows.get(37).getCell(colIndex),true));

            param.setOrderNumMaxBar(rows.get(38).getCellAsNumber(colIndex).orElse(new BigDecimal("10")).intValue());
            param.setPreventMultipleOrders(parseBoolean(rows.get(39).getCell(colIndex), true));

            param.setWaitNBarPinza(rows.get(40).getCellAsNumber(colIndex).orElse(BigDecimal.ZERO).intValue());

            param.setOrderCluster(rows.get(41).getCellAsString(colIndex).orElse(""));
            param.setOrderClusterPriority(rows.get(42).getCellAsNumber(colIndex).orElse(BigDecimal.ZERO).intValue());
            param.setMaxOrderByCluster(rows.get(43).getCellAsNumber(colIndex).orElse(BigDecimal.ONE).intValue());

            param.setActiveFreeWeekEnd(parseBoolean(rows.get(44).getCell(colIndex), false));

            param.setPercentDeltaTakeProfitUpdateStopLoss(rows.get(45).getCellAsNumber(colIndex).orElse(new BigDecimal("0")).doubleValue());
            param.setPercentDeltaTakeProfitAddToStartPrice(rows.get(46).getCellAsNumber(colIndex).orElse(new BigDecimal("0")).doubleValue());

            param.setActiveEdgeOrder(parseBoolean(rows.get(47).getCell(colIndex), false));
            param.setOrderSizeEdgeOrder(rows.get(48).getCell(colIndex).asNumber());
            param.setIdentEdgeOrder(rows.get(49).getCell(colIndex).asNumber());
            param.setIndentPercentPennachioEdgeOrder(rows.get(50).getCell(colIndex).asNumber());
            param.setIndentPercentModEdgeOrder(rows.get(51).getCell(colIndex).asNumber());
            param.setStopLossEdgeOrder(rows.get(52).getCell(colIndex).asNumber());
            param.setPercentStopLossIdent(rows.get(53).getCell(colIndex).asNumber());
            param.setTakeProfitEdgeOrder(rows.get(54).getCell(colIndex).asNumber());


            params.add(param);

            console.getOut().println(param.toString());

            colIndex++;
        }


        return params;
    }

    private boolean parseBoolean(Cell cell, boolean defaultValue) {
        switch (cell.getType()){
            case EMPTY:
                return defaultValue;
            case BOOLEAN:
                return (Boolean) cell.getValue();
            case STRING:
                String value = U.clean(cell.getText()).toLowerCase();
                return value.equals("true") || value.equals("vero");
            case NUMBER:
                return cell.getText().equals("1");
            case FORMULA:
                return cell.getText().toLowerCase().contains("true");
            case ERROR:
                throw new NullPointerException("Read Param cell empty");
        }

        return defaultValue;
    }
}
