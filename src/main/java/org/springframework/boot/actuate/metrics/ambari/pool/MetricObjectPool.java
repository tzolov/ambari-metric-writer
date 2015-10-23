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
package org.springframework.boot.actuate.metrics.ambari.pool;

import static org.springframework.util.CollectionUtils.isEmpty;

import java.io.Closeable;
import java.io.IOException;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.metrics.ambari.domain.TimelineMetric;
import org.springframework.boot.actuate.metrics.ambari.domain.TimelineMetrics;

/**
 * Helper class that keeps pools of {@link TimelineMetric} and {@link TimelineMetrics} objects to reduce the GC.
 * 
 * @author tzolov@apache.org
 *
 */
public class MetricObjectPool implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(MetricObjectPool.class);

    private final static long FIVE_SECONDS = 5 * 1000;

    /**
     * Object pools used for TimelineMetric and TimelineMetrics objects.
     */
    private GenericObjectPool<TimelineMetrics> timelineMetricsPool;
    private GenericObjectPool<TimelineMetric> timelineMetricPool;

    public MetricObjectPool(int timelineMetricsPoolSize, int timelineMeticPoolSize) {
        this.timelineMetricPool = new GenericObjectPool<TimelineMetric>(new TimelineMetricFactory());
        this.timelineMetricPool.setMaxTotal(timelineMeticPoolSize);
        this.timelineMetricPool.setMaxWaitMillis(FIVE_SECONDS);

        this.timelineMetricsPool = new GenericObjectPool<TimelineMetrics>(new TimelineMetricsFactory());
        this.timelineMetricsPool.setMaxTotal(timelineMetricsPoolSize);
        this.timelineMetricsPool.setMaxWaitMillis(FIVE_SECONDS);
    }

    /**
     * @return Returns a {@link TimelineMetrics} instance that represents and holds set of {@link TimelineMetric}
     *         objects.
     * @throws Exception
     *             Thrown if it fails to obtain a new or reused object instance.
     */
    public TimelineMetrics getMetrics() throws Exception {
        return timelineMetricsPool.borrowObject();
    }

    /**
     * @param metrics
     *            {@link TimelineMetrics} object that contains the {@link TimelineMetric} object returned. Note that you
     *            can not borrow a {@link TimelineMetric} object that is not assigned a {@link TimelineMetrics}!
     * @return Returns a {@link TimelineMetric} object part of the provided metrics set.
     * @throws Exception
     *             Thrown if it fails to obtain a new or reused object instance.
     */
    public TimelineMetric getMetricFor(TimelineMetrics metrics) throws Exception {
        TimelineMetric metric = timelineMetricPool.borrowObject();
        metrics.getMetrics().add(metric);
        return metric;
    }

    /**
     * Return the unused {@link TimelineMetrics} and {@link TimelineMetric} objects to their pools.
     * 
     * @param timelineMetrics
     *            {@link TimelineMetrics} object to return to the timelineMetricsPool. {@link TimelineMetrics} contains
     *            list of {@link TimelineMetric} in turn returned to the timelineMetricPool.
     */
    public void returnObjects(TimelineMetrics timelineMetrics) {

        if (timelineMetrics == null) {
            return;
        }

        // Return any TimelineMetric contained by the timelineMetrics object
        if (!isEmpty(timelineMetrics.getMetrics())) {
            for (TimelineMetric metric : timelineMetrics.getMetrics()) {
                try {
                    this.timelineMetricPool.returnObject(metric);
                } catch (Exception e) {
                    logger.warn("Failed to return TimelineMetric object to the pool", e);
                }
            }
        }

        this.timelineMetricsPool.returnObject(timelineMetrics);
    }

    @Override
    public void close() throws IOException {
        timelineMetricsPool.clear();
        timelineMetricPool.clear();
        timelineMetricsPool.close();
        timelineMetricPool.close();
    }

    // ------------------------------------------------------------------------
    // Getters/Setters used for test purposes only
    // ------------------------------------------------------------------------
    public GenericObjectPool<TimelineMetrics> getTimelineMetricsPool() {
        return timelineMetricsPool;
    }

    public GenericObjectPool<TimelineMetric> getTimelineMetricPool() {
        return timelineMetricPool;
    }
}
