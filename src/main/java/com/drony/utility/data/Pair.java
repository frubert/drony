package com.drony.utility.data;

import java.io.Serializable;
import java.util.Objects;

public class Pair<F, S> implements Serializable {

    private static final long serialVersionUID = 1L;

    private F first;

    private S second;

    private Pair() { }

    public Pair(F first, S second) {
        this.first = first;
        this.second = second;
    }

    public F getFirst() {
        return first;
    }

    public Pair<F, S> first(F a) {
        this.first = a;

        return this;
    }

    public void setFirst(F first) {
        this.first = first;
    }

    public S getSecond() {
        return second;
    }

    public Pair<F, S> second(S second) {
        this.second = second;

        return this;
    }

    public void setSecond(S second) {
        this.second = second;
    }

    @Override
    public String toString() {
        return "Pair{" + "first=" + first + ", second=" + second + '}';
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 71 * hash + Objects.hashCode(this.first);
        hash = 71 * hash + Objects.hashCode(this.second);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj)
            return true;

        if (obj == null)
            return false;

        if (getClass() != obj.getClass())
            return false;

        final Pair<?, ?> other = (Pair<?, ?>) obj;

        return Objects.equals(this.first, other.first) && Objects.equals(this.second, other.second);

    }
}
