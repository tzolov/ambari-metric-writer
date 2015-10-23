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

import static java.util.Collections.min;
import static org.springframework.util.CollectionUtils.isEmpty;

import java.util.Collection;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.ambari.buffer.MetricBuffer;
import org.springframework.boot.actuate.metrics.ambari.domain.TimelineMetric;
import org.springframework.boot.actuate.metrics.ambari.domain.TimelineMetrics;
import org.springframework.boot.actuate.metrics.ambari.pool.MetricObjectPool;
import org.springframework.boot.actuate.metrics.writer.Delta;
import org.springframework.boot.actuate.metrics.writer.MetricWriter;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * A {@link MetricWriter} for the Apache Ambari Metrics Service, writing metrics to the HTTP endpoint provided by the
 * server. Data are buffered according to the {@link #setMetricBufferSize(int) bufferSize} property, and only flushed
 * automatically when the buffer size is reached. Users should either manually {@link #flushMetricBuffer()} after
 * writing a batch of data if that makes sense, or consider adding a {@link Scheduled Scheduled} task to flush
 * periodically.
 * 
 * This is an common abstract class. Extensions should implement the {@link #doSendMetrics(TimelineMetrics)} to allow
 * transition of SpringBoot metrics to the Ambari Metrics Collector.
 * 
 * The implementation of the {@link #doSendMetrics(TimelineMetrics)} must call the
 * {@link #returnMetricPoolObjects(TimelineMetrics)} after transmitting the metrics, to return thetimelineMetrics to the
 * object pool.
 * 
 * @author tzolov@apache.org
 *
 */
public abstract class AmbariMetricWriter implements MetricWriter {

    private static final Logger logger = LoggerFactory.getLogger(AmbariMetricWriter.class);

    /**
     * Uniquely identify service/application within Ambari Metrics Collector.
     */
    private String metricApplicationId;

    /**
     * Used as second key part when storing metrics in the Ambari Metrics Collector.
     */
    private String metricHostName;

    /**
     * Metric instance id (optional).
     */
    private String metricInstanceId;

    /**
     * Metric buffer to patch the input metrics and post them in batches. The batch approach reduces the number of
     * remote HTTP calls.
     */
    private final MetricBuffer metricBuffer;

    /**
     * Metric buffer size to fill before posting data to server.
     */
    private int metricBufferSize;

    /**
     * TimelineMetric/TimelineMetrics object pool. The object pool minimizes the repetitive creation of data objects and
     * improves the GC.
     */
    private final MetricObjectPool metricObjectPool;

    public AmbariMetricWriter(String applicationId, String hostName, String instanceId, int metricsBufferSize) {

        this.metricApplicationId = applicationId;
        this.metricHostName = hostName;
        this.metricInstanceId = instanceId;

        this.metricBuffer = new MetricBuffer();
        this.metricBufferSize = metricsBufferSize;

        this.metricObjectPool = new MetricObjectPool(1000, Math.max(1000, metricBufferSize * 10));
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

        metricBuffer.add(metric);

        if (metricBuffer.size() > metricBufferSize) {
            flushMetricBuffer();
        }
    }

    /**
     * Flushes the metric buffer without waiting for it to fill any further. Converts the metricSnapsht into
     * {@link TimelineMetrics} instance and sends it to the Ambari Metrics Collector using the abstract
     * {@link #sendMetricsAndCleanPool(TimelineMetrics)}.
     */
    public void flushMetricBuffer() {

        if (metricBuffer.size() <= 0) {
            return;
        }

        Map<String, Map<Long, Double>> metricsSnapshot = metricBuffer.flush();

        if (!isEmpty(metricsSnapshot)) {
            // Send the metrics to Ambari Metrics Collector
            sendMetricsAndCleanPool(toTimelineMetrics(metricsSnapshot));
        }
    }

    /**
     * Sends the {@link TimelineMetrics} to the Ambari Metric Collector and then returns the used transfer objects to
     * the metric objects pool.
     * 
     * @param timelineMetrics
     *            {@link TimelineMetrics} to send
     */
    protected void sendMetricsAndCleanPool(TimelineMetrics timelineMetrics) {
        try {
            // Send the metrics to Ambari Metrics Collector
            doSendMetrics(timelineMetrics);
        } finally {
            // Always return the TimelineMetric(s) objects to the pool
            returnMetricPoolObjects(timelineMetrics);
        }
    }

    /**
     * Converts the metricsSnapshots into {@link TimelineMetrics} instance. It uses the object pools to minimize the
     * creation of {@link TimelineMetric} and {@link TimelineMetrics} object instance.
     * 
     * @param metricsSnapshot
     * @return Returns {@link TimelineMetrics}
     */
    private TimelineMetrics toTimelineMetrics(Map<String, Map<Long, Double>> metricsSnapshot) {

        try {
            TimelineMetrics timelineMetrics = metricObjectPool.getMetrics();

            for (String metricName : metricsSnapshot.keySet()) {

                Map<Long, Double> metricValues = metricsSnapshot.get(metricName);

                // Filter out metrics with no values
                if (!isEmpty(metricValues)) {
                    try {
                        TimelineMetric metric = metricObjectPool.getMetricFor(timelineMetrics);

                        metric.setMetricName(metricName);
                        metric.setAppId(metricApplicationId);
                        metric.setHostName(metricHostName);
                        metric.setInstanceId(metricInstanceId);
                        metric.setType(metricBuffer.getMetricType(metricName));
                        long startTime = computeStartTime(metricValues.keySet());
                        metric.setStartTime(startTime);
                        metric.setTimestamp(startTime); // Not sure of the exact semantics?
                        metric.setMetricValues(metricValues);

                    } catch (Exception e) {
                        logger.error("Failed to borrow TimelineMetric object for:" + metricName, e);
                    }
                }
            }

            return timelineMetrics;

        } catch (Exception e1) {
            logger.error("Failed to send TimelineMetrics!", e1);
            return null;
        }
    }

    private long computeStartTime(Collection<Long> times) {
        return isEmpty(times) ? 0 : min(times);
    }

    /**
     * Transmits {@link TimelineMetrics} object to the Ambari Metric Collector.
     * 
     * @param timelineMetrics
     *            {@link TimelineMetrics} to send to the server.
     */
    protected abstract void doSendMetrics(TimelineMetrics timelineMetrics);

    /**
     * Return the unused {@link TimelineMetrics} and {@link TimelineMetric} objects to their pools.
     * 
     * @param timelineMetrics
     *            {@link TimelineMetrics} object to return to the timelineMetricsPool. The {@link TimelineMetrics}
     *            contains list of {@link TimelineMetric} which in turn are returned to the pool.
     */
    protected void returnMetricPoolObjects(TimelineMetrics timelineMetrics) {
        metricObjectPool.returnObjects(timelineMetrics);
    }

    // ------------------------------------------------------------------------
    // Getters/Setters used for test purposes only
    // ------------------------------------------------------------------------
    public String getMetricApplicationId() {
        return metricApplicationId;
    }

    public void setMetricApplicationId(String applicationId) {
        this.metricApplicationId = applicationId;
    }

    public String getMetricHostName() {
        return metricHostName;
    }

    public void setMetricHostName(String hostName) {
        this.metricHostName = hostName;
    }

    public String getMetricInstanceId() {
        return metricInstanceId;
    }

    public void setMetricInstanceId(String instanceId) {
        this.metricInstanceId = instanceId;
    }

    public int getMetricBufferSize() {
        return metricBufferSize;
    }

    public void setMetricBufferSize(int bufferSize) {
        this.metricBufferSize = bufferSize;
    }

    public MetricObjectPool getMetricObjectPool() {
        return this.metricObjectPool;
    }
}
