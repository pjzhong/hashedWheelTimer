package com.z.timer;

import java.util.concurrent.Delayed;
import java.util.concurrent.ScheduledFuture;

public interface Registration<T> extends ScheduledFuture<T>, Runnable {

    enum Status {
        CANCELLED,
        READ
    }

    boolean ready();

    boolean isCancelAfterUse();

    int rounds();

    int getOffset();

    void decrement();

    void reeset();


    default int compareTo(Delayed o) {
        Registration other = (Registration) o;
        long r1 = rounds();
        long r2 = other.rounds();
        if (r1 == r2) {
            return other == this ? 0 : -1;
        } else {
            return Long.compare(r1, r2);
        }
    }
}
