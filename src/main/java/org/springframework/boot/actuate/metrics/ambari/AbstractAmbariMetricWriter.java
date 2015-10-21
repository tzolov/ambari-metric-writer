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
import java.util.Iterator;
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
 * server. Data are buffered according to the {@link #setBufferSize(int) bufferSize} property, and only flushed
 * automatically when the buffer size is reached. Users should either manually {@link #flushMetricBuffer()} after
 * writing a batch of data if that makes sense, or consider adding a {@link Scheduled Scheduled} task to flush
 * periodically.
 * 
 * This is an common abstract class. Extensions should implement the {@link #doSendMetrics(TimelineMetrics)} to allow
 * transition of SpringBoot metrics to the Ambari Metrics Collector.
 * 
 * The implementation of the {@link #doSendMetrics(TimelineMetrics)} must call the
 * {@link #cleanMetricPool(TimelineMetrics)} after transmitting the metrics, to return thetimelineMetrics to the object
 * pool.
 * 
 * @author tzolov@apache.org
 *
 */
public abstract class AbstractAmbariMetricWriter implements MetricWriter {

    private static final Logger logger = LoggerFactory.getLogger(AbstractAmbariMetricWriter.class);

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
     * Metric buffer size to fill before posting data to server.
     */
    private int bufferSize;

    /**
     * Metric buffer to patch the input metrcis and post them in batches.
     */
    private MetricBuffer metricBuffer;

    /**
     * Object pools used for TimelineMetric and TimelineMetrics objects.
     */
    private MetricObjectPool metricObjectPool;

    public AbstractAmbariMetricWriter(String applicationId, String hostName, String instanceId, int metricsBufferSize) {

        this.metricApplicationId = applicationId;
        this.metricHostName = hostName;
        this.metricInstanceId = instanceId;

        this.metricBuffer = new MetricBuffer();
        this.bufferSize = metricsBufferSize;

        this.metricObjectPool = new MetricObjectPool(1000, Math.max(1000, bufferSize * 10));
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

        if (metricBuffer.size() > bufferSize) {
            flushMetricBuffer();
        }
    }

    /**
     * Flush the buffer without waiting for it to fill any further. Converts the namedMetricsBuffer into
     * {@link TimelineMetrics} instance and send the later to the Ambari Metrics Collector through the
     * {@link #doSendMetrics(TimelineMetrics)} abstract method.
     */
    public void flushMetricBuffer() {

        if (metricBuffer.size() <= 0) {
            return;
        }

        Map<String, Map<Long, Float>> metricsSnapshot = metricBuffer.flush();

        if (!isEmpty(metricsSnapshot)) {
            // Send the metrics to Ambari Metrics Collector
            sendMetricsAndCleanPool(toTimelineMetrics(metricsSnapshot));
        }
    }

    /**
     * Sends the {@link TimelineMetrics} to the Ambari Metric Collector and cleans the Metric Objects Pool.
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
            cleanMetricPool(timelineMetrics);
        }
    }

    /**
     * Converts the metricsSnapshots into {@link TimelineMetrics} instance. It uses the object pools to minimize the
     * creation of {@link TimelineMetric} and {@link TimelineMetrics} object instance.
     * 
     * @param metricsSnapshot
     * @return Returns {@link TimelineMetrics}
     */
    private TimelineMetrics toTimelineMetrics(Map<String, Map<Long, Float>> metricsSnapshot) {

        try {
            TimelineMetrics timelineMetrics = metricObjectPool.getTimelineMetrics();

            Iterator<String> metricNames = metricsSnapshot.keySet().iterator();

            while (metricNames.hasNext()) {
                String metricName = metricNames.next();
                Map<Long, Float> metricValues = metricsSnapshot.get(metricName);

                if (!isEmpty(metricValues)) {
                    try {
                        TimelineMetric metric = metricObjectPool.getTimelineMetricFor(timelineMetrics);

                        metric.setMetricName(metricName);
                        metric.setAppId(metricApplicationId);
                        metric.setHostName(metricHostName);
                        metric.setInstanceId(metricInstanceId);
                        metric.setStartTime(getStartTime(metricValues.keySet()));
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

    private long getStartTime(Collection<Long> times) {
        return isEmpty(times) ? 0 : min(times);
    }

    /**
     * To implement the transition of the {@link TimelineMetrics} to the Ambari Metric Collector.
     * 
     * Implementation of this method must use the {@link #cleanMetricPool(TimelineMetrics)} to return thetimelineMetrics
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
    protected void cleanMetricPool(TimelineMetrics timelineMetrics) {
        metricObjectPool.returnObjects(timelineMetrics);
    }

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

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public MetricObjectPool getMetricObjectPool() {
        return this.metricObjectPool;
    }
}
