package com.drony.utility.data;

import java.io.Serializable;

public record Pair<F, S>(F first, S second) implements Serializable {

    public F getFirst() {
        return first;
    }

    public S getSecond() {
        return second;
    }
}
