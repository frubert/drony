package com.drony.stategy;

import com.drony.stategy.data.ClusterOrderDetail;
import com.drony.stategy.data.DronyData;
import com.drony.stategy.data.DronyOrder;
import com.drony.stategy.data.ParamDrony;
import com.drony.stategy.edge.EdgeOrderService;
import com.drony.stategy.utility.ReaderParam;
import com.drony.stategy.utility.WriterExcelOrder;
import com.drony.utility.data.Pair;
import com.drony.utility.data.U;
import com.dukascopy.api.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
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

  private final Map<String, Integer> clusterMaxOrder = new ConcurrentHashMap<>();
  private final Map<String, List<ClusterOrderDetail>> clusterOrder = new ConcurrentHashMap<>();

  private final EdgeOrderService edgeOrderService;

  public DelegateDrony(File fileExcel, File fileResult, IContext context, Boolean outPutVerboso) {

    this.console = context.getConsole();
    this.context = context;
    this.engine = context.getEngine();

    this.edgeOrderService = new EdgeOrderService(this.engine);

    try {
      ReaderParam reader = new ReaderParam(fileExcel, console);

      Set<Instrument> instruments = new HashSet<>();

      int index = 0;
      for (ParamDrony param : reader.getDronies()) {
        instruments.add(param.getSelectedInstrument());
        DronyStrategy drony = new DronyStrategy(param, outPutVerboso, this, index++);
        dronies.add(drony);
        /*groupOrder.put(U.trimRemoveNull(param.getOrderCluster()), new ArrayList<>());*/
        console.getOut().println(param.toString());
        String clusterName = cleanNameCluster(param.getOrderCluster());
        Integer max = this.clusterMaxOrder.getOrDefault(clusterName, 0);
        this.clusterMaxOrder.put(clusterName, Math.max(max, param.getMaxOrderByCluster()));
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
        e.printStackTrace();
      }
    }
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

  private String cleanNameCluster(String clusterName) {
    return U.trimRemoveNull(clusterName);
  }

  private List<ClusterOrderDetail> getListLabelsByCluster(String cluster) {
    cluster = this.cleanNameCluster(cluster);
    return this.clusterOrder
        .getOrDefault(cluster, new CopyOnWriteArrayList<>());
  }

  public boolean testAndAddOrterToCluster(String cluster, String label, int priority,
      DronyOrder dronyOrder) {
    cluster = this.cleanNameCluster(cluster);
    List<ClusterOrderDetail> list = this.getListLabelsByCluster(cluster);

    boolean orderFilled = list.stream().anyMatch(detail -> isFilled(detail.getLabel()));

    if (orderFilled) {
      return false;
    } else {
      list.add(new ClusterOrderDetail(label, priority, dronyOrder));
      this.clusterOrder.put(cluster, list);
      return true;
    }
  }

  private boolean isFilled(String label) {
    try {
      IOrder order = this.engine.getOrder(label);
      if (order != null) {
        return order.getState().equals(IOrder.State.FILLED);
      } else {
        return false;
      }
    } catch (JFException e) {
      this.console.getErr().println(e.getMessage());
      this.console.getErr().println(e.toString());
      return false;
    }
  }

  private String getType(ClusterOrderDetail detail) {
    try {
      IOrder order = this.engine.getOrder(detail.getLabel());
      if (order != null) {
        if (order.getState() != IOrder.State.CANCELED
            && order.getState() != IOrder.State.CLOSED
            && order.getState() != IOrder.State.FILLED) {
          return "NOT FILLED";
        } else if (order.getState() == IOrder.State.FILLED) {
          return "FILLED";
        }
      }
    } catch (JFException e) {
      this.console.getErr().println(e.getMessage());
      this.console.getErr().println(e.toString());
    }

    return "EMPTY";
  }

  public void closeOtherOrder(String clusterStr, String label, int priority,
      DronyOrder dronyOrder) {
    String cluster = this.cleanNameCluster(clusterStr);
    List<ClusterOrderDetail> list = getListLabelsByCluster(cluster);

    Map<String, List<ClusterOrderDetail>> orderMap = list.stream()
        .filter(detail -> !detail.getLabel().equals(label))
        .collect(Collectors.groupingBy(this::getType));

    orderMap.getOrDefault("NOT FILLED", new ArrayList<>())
        .forEach(clusterOrderDetail -> {
          try {
            IOrder order = this.engine.getOrder(clusterOrderDetail.getLabel());
            order.close();
            clusterOrderDetail.getDronyOrder().setMotivationToClose(" BY CLUSTER RULE " + cluster);
          } catch (JFException e) {
            this.console.getErr().println(e.getMessage());
            this.console.getErr().println(e.toString());
          }
        });

    List<ClusterOrderDetail> orderFilled = orderMap.getOrDefault("FILLED", new ArrayList<>());

    orderFilled.add(new ClusterOrderDetail(label, priority, dronyOrder));
    int max = this.clusterMaxOrder.get(cluster);

    if (orderFilled.size() <= max) {
      return;
    }

    List<ClusterOrderDetail> orderOpen = orderFilled.stream()
        .sorted((d1, d2) -> Integer.compare(d2.getPriority(), d1.getPriority()))
        .collect(Collectors.toList());

    for (int i = max; i < orderOpen.size(); i++) {
      try {
        ClusterOrderDetail detail = orderOpen.get(i);
        IOrder order = this.engine.getOrder(detail.getLabel());
        order.close();
        dronyOrder.setMotivationToClose(" BY CLUSTER RULE PRIORITY " + cluster);
      } catch (JFException e) {
        this.console.getErr().println(e.getMessage());
        this.console.getErr().println(e.toString());
      }
    }
  }

  public EdgeOrderService getEdgeOrderService() {
    return edgeOrderService;
  }
}
