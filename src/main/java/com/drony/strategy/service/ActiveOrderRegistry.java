package com.drony.strategy.service;

import com.dukascopy.api.IOrder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cache degli ordini vivi di una singola strategia.
 *
 * Evita di scandire engine.getOrders() a ogni tick/barra: gli ordini vengono
 * registrati alla submit e rimossi quando un messaggio li segnala chiusi o
 * cancellati. Gli IOrder sono oggetti vivi e le letture filtrano comunque
 * sullo stato corrente, quindi un'eventuale entry non ancora rimossa non
 * altera il comportamento, costa solo una voce di mappa.
 */
public class ActiveOrderRegistry {

    private final Map<String, IOrder> liveOrders = new LinkedHashMap<>();

    public void register(IOrder order) {
        liveOrders.put(order.getLabel(), order);
    }

    /** Da chiamare per ogni messaggio relativo a un ordine di questa strategia. */
    public void onOrderMessage(IOrder order) {
        IOrder.State state = order.getState();
        if (state == IOrder.State.CLOSED || state == IOrder.State.CANCELED) {
            liveOrders.remove(order.getLabel());
        }
    }

    /** Snapshot degli ordini vivi: si può chiamare order.close() durante l'iterazione. */
    public List<IOrder> liveOrders() {
        List<IOrder> snapshot = new ArrayList<>(liveOrders.size());
        for (IOrder order : liveOrders.values()) {
            if (isLive(order)) {
                snapshot.add(order);
            }
        }
        return snapshot;
    }

    public int countBuyOrders() {
        return countByDirection(true);
    }

    public int countSellOrders() {
        return countByDirection(false);
    }

    private int countByDirection(boolean isLong) {
        int count = 0;
        for (IOrder order : liveOrders.values()) {
            if (isLive(order) && order.getOrderCommand().isLong() == isLong) {
                count++;
            }
        }
        return count;
    }

    private static boolean isLive(IOrder order) {
        IOrder.State state = order.getState();
        return state != IOrder.State.CLOSED && state != IOrder.State.CANCELED;
    }
}
