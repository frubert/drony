package com.drony.strategy;

import com.drony.strategy.data.DronyData;
import com.drony.strategy.data.ParamDrony;
import com.drony.strategy.edge.EdgeOrderService;
import com.drony.strategy.service.ClusterManager;
import com.drony.strategy.utility.DecisionLogger;
import com.drony.strategy.utility.ReaderParam;
import com.drony.strategy.utility.WriterExcelOrder;
import com.drony.utility.data.Pair;
import com.drony.utility.data.U;
import com.dukascopy.api.*;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

public class DelegateDrony {

  private final static Logger log = LoggerFactory.getLogger(DelegateDrony.class);

  public static String version = "V 4.2.0";

  public final Comparator<DronyData> COMPARATOR_DRONY = Comparator
      .comparing(DronyData::getCreatedDate);
  private final IEngine engine;

  private IConsole console;
  private IContext context;

  private final List<DronyStrategy> dronies = new ArrayList<>();
  private File fileResult;

  private final ClusterManager clusterManager;
  private final EdgeOrderService edgeOrderService;
  private final DecisionLogger decisionLogger;

  public DelegateDrony(File fileExcel, File fileResult, File fileDecisions, IContext context,
      Boolean outputVerbose) {

    this.console = context.getConsole();
    this.context = context;
    this.engine = context.getEngine();

    this.edgeOrderService = new EdgeOrderService(this.engine);
    this.clusterManager = new ClusterManager(this.engine, this.console);
    this.decisionLogger = DecisionLogger.toFile(fileDecisions);

    try {
      ReaderParam reader = new ReaderParam(fileExcel, console);

      Set<Instrument> instruments = new HashSet<>();

      int index = 0;
      for (ParamDrony param : reader.getDronies()) {
        instruments.add(param.getSelectedInstrument());
        DronyStrategy drony = new DronyStrategy(param, outputVerbose, this, index++);
        dronies.add(drony);
        console.getOut().println(param.toString());
        this.clusterManager.registerCluster(param);
      }

      context.setSubscribedInstruments(instruments, true);

    } catch (Exception e) {
      console.getErr().println(e.getMessage());
      log.error(e.getMessage(), e);
    }

    String originName = "";

    if (fileResult == null) {
      originName = "report" + File.separator + "csvReport";
    } else if (fileResult.isDirectory()) {
      originName = fileResult.getPath() + File.separator + "DronyReport_" + U
          .clearStringForPath(version);
      fileResult = null;
    }

    if (fileResult == null) {
      final String exst = ".xlsx";

      String fileName = originName;

      boolean fileNameNotFound = true;
      int prog = 1;

      while (fileNameNotFound) {
        if ((new File(fileName + exst)).exists()) {
          fileName = originName + " (" + prog + ")";
          prog++;
        } else {
          fileNameNotFound = false;
        }
      }

      fileResult = new File(fileName + exst);
    }

    if (fileResult == null || fileResult.getPath().equals("")) {
      console.getErr().println("File not selected");
      context.stop();
      return;
    }

    this.fileResult = fileResult;
  }


  public void onStart(IContext context) throws JFException {
    for (DronyStrategy drony : dronies) {
      drony.onStart(context);
    }
  }

  public void onAccount(IAccount account) throws JFException {
    for (DronyStrategy drony : dronies) {
      drony.onAccount(account);
    }
  }

  public void onMessage(IMessage message) throws JFException {
    for (DronyStrategy drony : dronies) {
      drony.onMessage(message);
    }

    this.getEdgeOrderService().checkOrderService(message);
  }

  public void onStop() throws JFException {

    if (!dronies.isEmpty()) {

      try {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {

          Workbook wb = new Workbook(os, "Drony 4.1", null);

          for (DronyStrategy drony : dronies) {

            Pair<String, List<DronyData>> ordersNamed = drony.onStopData();

            Worksheet ws = wb.newWorksheet(ordersNamed.getFirst());

            List<List<String>> page = new ArrayList<>();

            page.add(WriterExcelOrder.getHeader());

            List<DronyData> orders = new ArrayList<>(ordersNamed.getSecond());

            orders.sort(COMPARATOR_DRONY);

            for (DronyData order : orders) {
              page.addAll(order.getMatrix());
              page.add(new ArrayList<>());
            }

            for (int indexRow = 0; indexRow < page.size(); indexRow++) {
              List<String> row = page.get(indexRow);
              for (int indexCol = 0; indexCol < row.size(); indexCol++) {
                ws.value(indexRow, indexCol, row.get(indexCol));
                if (indexRow == 0) {
                  ws.style(indexRow, indexCol).bold().set();
                }
              }
            }

            ws.flush();
            ws.finish();
          }

          wb.finish();

          try (OutputStream outputStream = new FileOutputStream(fileResult)) {
            os.writeTo(outputStream);
          }
        }

      } catch (IOException e) {
        log.error("Errore scrittura report {}", fileResult, e);
      }
    }

    this.decisionLogger.close();
  }

  public void onTick(Instrument instrument, ITick tick) throws JFException {
    for (DronyStrategy drony : dronies) {
      drony.onTick(instrument, tick);
    }

    this.getEdgeOrderService().manageActiveEdge();
  }

  public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar)
      throws JFException {

    if (askBar.getVolume() == 0 && bidBar.getVolume() == 0) {
      return; /* SPEED UP FOR SATURDAY AND SUNDAY (FOREX CLOSED) */
    }

    for (DronyStrategy drony : dronies) {
      drony.onBar(instrument, period, askBar, bidBar);
    }
  }

  public ClusterManager getClusterManager() {
    return clusterManager;
  }

  public DecisionLogger getDecisionLogger() {
    return decisionLogger;
  }

  public EdgeOrderService getEdgeOrderService() {
    return edgeOrderService;
  }
}
