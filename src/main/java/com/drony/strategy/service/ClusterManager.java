package com.drony.strategy.service;

import com.drony.strategy.data.ClusterOrderDetail;
import com.drony.strategy.data.DronyOrder;
import com.drony.strategy.data.ParamDrony;
import com.drony.utility.data.U;
import com.dukascopy.api.IConsole;
import com.dukascopy.api.IEngine;
import com.dukascopy.api.IOrder;
import com.dukascopy.api.JFException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Regole di cluster tra strategie: un nuovo ordine entra nel cluster solo se
 * nessun ordine del cluster è già FILLED; quando un ordine viene fillato,
 * gli ordini pendenti dello stesso cluster vengono chiusi e i FILLED in
 * eccesso rispetto a maxOrder vengono chiusi per priorità.
 */
public class ClusterManager {

  private enum OrderFillState {FILLED, PENDING, GONE}

  private static final int DEFAULT_MAX_ORDER = 1;

  private final IEngine engine;
  private final IConsole console;

  private final Map<String, Integer> clusterMaxOrder = new HashMap<>();
  private final Map<String, List<ClusterOrderDetail>> clusterOrder = new HashMap<>();

  public ClusterManager(IEngine engine, IConsole console) {
    this.engine = engine;
    this.console = console;
  }

  /** Registra il cluster di una strategia; il max ordini del cluster è il massimo tra le strategie che lo usano. */
  public void registerCluster(ParamDrony param) {
    String clusterName = cleanName(param.getOrderCluster());
    int max = this.clusterMaxOrder.getOrDefault(clusterName, 0);
    this.clusterMaxOrder.put(clusterName, Math.max(max, param.getMaxOrderByCluster()));
  }

  /** @return true se l'ordine può entrare nel cluster (nessun ordine del cluster già FILLED). */
  public boolean testAndAddOrderToCluster(String cluster, String label, int priority,
      DronyOrder dronyOrder) {
    List<ClusterOrderDetail> list = detailsOf(cluster);

    boolean orderFilled = list.stream()
        .anyMatch(detail -> fillStateOf(detail) == OrderFillState.FILLED);

    if (orderFilled) {
      return false;
    }
    list.add(new ClusterOrderDetail(label, priority, dronyOrder));
    return true;
  }

  /**
   * Da chiamare quando un ordine del cluster viene fillato: chiude gli ordini
   * pendenti del cluster e, se i FILLED superano il massimo, chiude i meno
   * prioritari.
   */
  public void onOrderFilled(String clusterStr, String label, int priority,
      DronyOrder dronyOrder) {
    String cluster = cleanName(clusterStr);

    Map<OrderFillState, List<ClusterOrderDetail>> byState = detailsOf(cluster).stream()
        .filter(detail -> !detail.getLabel().equals(label))
        .collect(Collectors.groupingBy(this::fillStateOf));

    for (ClusterOrderDetail detail : byState.getOrDefault(OrderFillState.PENDING, List.of())) {
      close(detail.getLabel(),
          () -> detail.getDronyOrder().setMotivationToClose(" BY CLUSTER RULE " + cluster));
    }

    List<ClusterOrderDetail> filled =
        new ArrayList<>(byState.getOrDefault(OrderFillState.FILLED, List.of()));
    filled.add(new ClusterOrderDetail(label, priority, dronyOrder));

    int max = this.clusterMaxOrder.getOrDefault(cluster, DEFAULT_MAX_ORDER);

    if (filled.size() <= max) {
      return;
    }

    filled.sort((d1, d2) -> Integer.compare(d2.getPriority(), d1.getPriority()));

    for (int i = max; i < filled.size(); i++) {
      ClusterOrderDetail detail = filled.get(i);
      close(detail.getLabel(),
          () -> dronyOrder.setMotivationToClose(" BY CLUSTER RULE PRIORITY " + cluster));
    }
  }

  private void close(String label, Runnable setMotivation) {
    try {
      IOrder order = this.engine.getOrder(label);
      if (order != null) {
        order.close();
        setMotivation.run();
      }
    } catch (JFException e) {
      this.console.getErr().println(e.getMessage());
      this.console.getErr().println(e.toString());
    }
  }

  private OrderFillState fillStateOf(ClusterOrderDetail detail) {
    try {
      IOrder order = this.engine.getOrder(detail.getLabel());
      if (order != null) {
        if (order.getState() == IOrder.State.FILLED) {
          return OrderFillState.FILLED;
        }
        if (order.getState() != IOrder.State.CANCELED
            && order.getState() != IOrder.State.CLOSED) {
          return OrderFillState.PENDING;
        }
      }
    } catch (JFException e) {
      this.console.getErr().println(e.getMessage());
      this.console.getErr().println(e.toString());
    }
    return OrderFillState.GONE;
  }

  private List<ClusterOrderDetail> detailsOf(String cluster) {
    return this.clusterOrder.computeIfAbsent(cleanName(cluster), k -> new ArrayList<>());
  }

  private static String cleanName(String clusterName) {
    return U.trimRemoveNull(clusterName);
  }
}
