/*
 * DIVISION INDUSTRIES CONFIDENTIAL
 * __________________________________
 *
 *  2015-2019 Division Industries LLC
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains the property of Division Industries LLC
 * and its suppliers, if any. The intellectual and technical concepts contained herein are proprietary
 * to Division Industries LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents
 * in process, and are protected by trade secret or copyright law. Dissemination of this information
 * or reproduction of this material is strictly forbidden unless prior written permission is obtained
 * from Division Industries LLC.
 */

package com.divisionind.fdb.scheduler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class AtomicScheduler {

    private ScheduledExecutorService pool;

    public AtomicScheduler(ScheduledExecutorService pool) {
        this.pool = pool;
    }

    public AtomicScheduler() {
        if(DaemonThreadFactory.getThreadFactory() == null) DaemonThreadFactory.init();
        this.pool = Executors.newSingleThreadScheduledExecutor(DaemonThreadFactory.getThreadFactory());
    }

    public AtomicScheduler(int i) {
        if(DaemonThreadFactory.getThreadFactory() == null) DaemonThreadFactory.init();
        this.pool = Executors.newScheduledThreadPool(i, DaemonThreadFactory.getThreadFactory());
    }

    public ScheduledFuture<?> delay(Runnable r, long delay, TimeUnit u) {
        return this.pool.schedule(r, delay, u);
    }

    public ScheduledFuture<?> delay(Runnable r, long delay) {
        return this.pool.schedule(r, delay, TimeUnit.MILLISECONDS);
    }

    public ScheduledFuture<?> repeating(Runnable r, long initialDelay, long period, TimeUnit u) {
        return this.pool.scheduleWithFixedDelay(r, initialDelay, period, u);
    }

    public ScheduledFuture<?> repeating(Runnable r, long initialDelay, long period) {
        return repeating(r, initialDelay, period, TimeUnit.MILLISECONDS);
    }

    public void shutdown(long i) {
        if(i == 0L) this.pool.shutdownNow();

        try {
            this.pool.shutdown();
            this.pool.awaitTermination(i, TimeUnit.MILLISECONDS);
        } catch (InterruptedException var7) { } finally {
            this.pool.shutdownNow();
        }
    }
}