/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.metrics.ambari;

import static org.springframework.util.CollectionUtils.isEmpty;
import it.unimi.dsi.fastutil.longs.Long2FloatArrayMap;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.ambari.domain.TimelineMetric;
import org.springframework.boot.actuate.metrics.ambari.domain.TimelineMetrics;
import org.springframework.boot.actuate.metrics.ambari.pool.TimelineMetricFactory;
import org.springframework.boot.actuate.metrics.ambari.pool.TimelineMetricsFactory;
import org.springframework.boot.actuate.metrics.writer.Delta;
import org.springframework.boot.actuate.metrics.writer.MetricWriter;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * A {@link MetricWriter} for the Aapache Ambari Timeline Server (version 2.1+), writing metrics to the HTTP endpoint
 * provided by the server. Data are buffered according to the {@link #setBufferSize(int) bufferSize} property, and only
 * flushed automatically when the buffer size is reached. Users should either manually {@link #flushMetricBuffer()}
 * after writing a batch of data if that makes sense, or consider adding a {@link Scheduled Scheduled} task to flush
 * periodically.
 * 
 * This is an common abstract class. Extensions should implement the {@link #doSendMetrics(TimelineMetrics)} to allow
 * transition of SpringBoot metrics to the Timeline Server.
 * 
 * The implementation of the {@link #doSendMetrics(TimelineMetrics)} must call the
 * {@link #freePoolObjects(TimelineMetrics)} after transmitting the metrics, to return thetimelineMetrics to the object
 * pool.
 * 
 * @author christian.tzolov@gmail.com
 *
 */
public abstract class AbstractAmbariMetricWriter implements MetricWriter /* , Closeable */{

    private static final Logger logger = LoggerFactory.getLogger(AbstractAmbariMetricWriter.class);

    private final static long FIVE_SECONDS = 5 * 1000;

    /**
     * Uniquely identify service/application within Ambari.
     */
    private String applicationId;

    /**
     * Used as second key part when storing metrics in the Timeline server.
     */
    private String hostName;

    /**
     * Instance id (optional).
     */
    private String instanceId = "nil";

    /**
     * Lock used to synchronize the writing of new metrics and their transition to the server.
     */
    private ReentrantLock bufferLock;

    /**
     * Metric buffer to fill before posting data to server.
     */
    private Map<String, Map<Long, Float>> metricBuffer;

    /**
     * Keep the count of all metric entries collected in the namedMetricsBuffer map.
     */
    private AtomicLong bufferedMetricCount;

    /**
     * Metric buffer size to fill before posting data to server.
     */
    private int bufferSize;

    /**
     * Object pools used for TimelineMetric and TimelineMetrics objects.
     */
    private GenericObjectPool<TimelineMetrics> timelineMetricsPool;

    private GenericObjectPool<TimelineMetric> timelineMetricPool;

    public AbstractAmbariMetricWriter(String applicationId, String hostName, int metricsBufferSize) {

        this.applicationId = applicationId;
        this.hostName = hostName;
        this.bufferSize = metricsBufferSize;

        this.timelineMetricPool = new GenericObjectPool<TimelineMetric>(new TimelineMetricFactory());
        this.timelineMetricPool.setMaxTotal(Math.max(1000, bufferSize * 10));
        this.timelineMetricPool.setMaxWaitMillis(FIVE_SECONDS);

        this.timelineMetricsPool = new GenericObjectPool<TimelineMetrics>(new TimelineMetricsFactory());
        this.timelineMetricsPool.setMaxTotal(1000);
        this.timelineMetricsPool.setMaxWaitMillis(FIVE_SECONDS);

        this.bufferLock = new ReentrantLock();

        this.bufferedMetricCount = new AtomicLong(0);

        this.metricBuffer = new HashMap<String, Map<Long, Float>>();
    }

    @Override
    public void increment(Delta<?> delta) {
        throw new UnsupportedOperationException("Counters not supported via increment");
    }

    @Override
    public void reset(String metricName) {
        logger.debug("Reset: " + metricName);
        set(new Metric<Long>(metricName, 0L));
    }

    @Override
    public void set(Metric<?> metric) {

        logger.debug("Set: " + metric);

        // Obtain a metrics sent lock. Only one thread can send the metrics over time!
        try {
            if (bufferLock.tryLock() || bufferLock.tryLock(5, TimeUnit.SECONDS)) {
                try {

                    addMetricToBuffer(metric);

                    // Flush the buffer if filled up
                    if (bufferedMetricCount.incrementAndGet() > bufferSize) {
                        flushMetricBuffer();
                    }

                } finally {
                    bufferLock.unlock();
                }
            }
        } catch (InterruptedException e) {
            logger.warn("Failed to buffer metric: " + metric + " due to locked thread!");
        }
    }

    private void addMetricToBuffer(Metric<?> metric) {

        String metricName = metric.getName();

        if (!metricBuffer.containsKey(metricName)) {
            metricBuffer.put(metricName, new Long2FloatArrayMap());
        }

        metricBuffer.get(metricName).put(metric.getTimestamp().getTime(), metric.getValue().floatValue());
    }

    /**
     * Flush the buffer without waiting for it to fill any further.
     */
    public void flushMetricBuffer() {

        if (isEmpty(metricBuffer)) {
            return;
        }

        doFlushBufferedMetrics(getBufferSnapshot());
    }

    private Map<String, Map<Long, Float>> getBufferSnapshot() {

        HashMap<String, Map<Long, Float>> snapshot = new HashMap<String, Map<Long, Float>>();
        try {
            if (bufferLock.tryLock() || bufferLock.tryLock(5, TimeUnit.SECONDS)) {
                for (Entry<String, Map<Long, Float>> metricEntry : metricBuffer.entrySet()) {
                    String metricName = metricEntry.getKey();
                    Map<Long, Float> metricValues = metricEntry.getValue();

                    if (!isEmpty(metricValues)) {
                        snapshot.put(metricName, ((Long2FloatArrayMap) metricValues).clone());
                        metricValues.clear();
                    }
                }

                // Reset the buffer metric count
                bufferedMetricCount.set(0);
            }
        } catch (InterruptedException e) {
            logger.warn("Metric Buffer flush failed due to locked thread!");
        } finally {
            bufferLock.unlock();
        }

        return snapshot;
    }

    /**
     * Converts the namedMetricsBuffer into {@link TimelineMetrics} instance and send the later to the Ambari Timeline
     * Server through the {@link #doSendMetrics(TimelineMetrics)} abstract method.
     * 
     * It uses the object pools to minimize the creation of {@link TimelineMetric} and {@link TimelineMetrics} object
     * instance. Therefore the {@link #doSendMetrics(TimelineMetrics)} must implement the return of the used objects to
     * the pool!
     * 
     * @param namedMetricsBuffer
     */
    private void doFlushBufferedMetrics(Map<String, Map<Long, Float>> namedMetricsBuffer) {

        if (isEmpty(namedMetricsBuffer)) {
            return;
        }

        try {

            TimelineMetrics timelineMetrics = timelineMetricsPool.borrowObject();

            Iterator<String> metricNames = namedMetricsBuffer.keySet().iterator();

            while (metricNames.hasNext()) {

                String metricName = metricNames.next();
                Map<Long, Float> metricValues = namedMetricsBuffer.get(metricName);
                if (!isEmpty(metricValues)) {
                    try {

                        TimelineMetric metric = timelineMetricPool.borrowObject();

                        metric.setMetricName(metricName);
                        metric.setAppId(applicationId);
                        metric.setHostName(hostName);
                        metric.setInstanceId(instanceId);
                        metric.setMetricValues(metricValues);
                        metric.setStartTime(System.currentTimeMillis());

                        // Add metric to the list of metrics to send
                        timelineMetrics.getMetrics().add(metric);

                    } catch (Exception e) {
                        logger.error("Failed to borrow TimelineMetric object for:" + metricName, e);
                    }
                }
            }

            // Send the metrics to Ambari Timeline Server
            doSendMetrics(timelineMetrics);

        } catch (Exception e1) {
            logger.error("Failed to borrow TimelineMetrics form the pool! Skip sending metrics!", e1);
        }
    }

    /**
     * To implement the transition of the {@link TimelineMetrics} to the Ambari Timeline Server.
     * 
     * Implementation of this method must use the {@link #freePoolObjects(TimelineMetrics)} to return thetimelineMetrics
     * to the object pool.
     * 
     * @param timelineMetrics
     *            {@link TimelineMetrics} to send to the server.
     */
    protected abstract void doSendMetrics(TimelineMetrics timelineMetrics);

    /**
     * Return the unused {@link TimelineMetrics} and {@link TimelineMetric} objects to their pools.
     * 
     * @param timelineMetrics
     *            {@link TimelineMetrics} object to return to the timelineMetricsPool. {@link TimelineMetrics} contains
     *            list of {@link TimelineMetric} in trun returned to the timelineMetricPool.
     */
    protected void freePoolObjects(TimelineMetrics timelineMetrics) {

        if (timelineMetrics == null) {
            return;
        }

        if (!isEmpty(timelineMetrics.getMetrics())) {
            for (TimelineMetric metric : timelineMetrics.getMetrics()) {
                try {
                    // Clears the metricValues list as well!
                    this.timelineMetricPool.returnObject(metric);
                } catch (Exception e) {
                    logger.warn("Failed to return TimelineMetric object to the pool", e);
                }
            }
        }

        // Clears the metrics list as well
        this.timelineMetricsPool.returnObject(timelineMetrics);
    }

    // @Override
    // public void close() throws IOException {
    //
    // try {
    // if (bufferLock.tryLock() || bufferLock.tryLock(5, TimeUnit.SECONDS)) {
    // try {
    // metricBuffer.clear();
    //
    // timelineMetricPool.clear();
    // timelineMetricPool.close();
    // timelineMetricsPool.clear();
    // timelineMetricsPool.close();
    // } finally {
    // bufferLock.unlock();
    // }
    // }
    // } catch (InterruptedException e) {
    // logger.warn("", e);
    // }
    // }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public long getBufferedMetricCount() {
        return bufferedMetricCount.get();
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    // Test only
    GenericObjectPool<TimelineMetrics> getTimelineMetricsPool() {
        return timelineMetricsPool;
    }

    GenericObjectPool<TimelineMetric> getTimelineMetricPool() {
        return timelineMetricPool;
    }
}
