package com.drony.strategy.data;

public class ClusterOrderDetail {

    private final String label;

    private final int priority;

    private final DronyOrder dronyOrder;

    public ClusterOrderDetail(String label, int priority, DronyOrder dronyOrder) {
        this.label = label;
        this.priority = priority;
        this.dronyOrder = dronyOrder;
    }

    public String getLabel() {
        return label;
    }

    public int getPriority() {
        return priority;
    }

    public DronyOrder getDronyOrder() {
        return dronyOrder;
    }
}
