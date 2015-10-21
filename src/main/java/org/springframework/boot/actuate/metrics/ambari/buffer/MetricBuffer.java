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
package org.springframework.boot.actuate.metrics.ambari.buffer;

import static org.springframework.util.CollectionUtils.isEmpty;
import it.unimi.dsi.fastutil.longs.Long2FloatArrayMap;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.metrics.Metric;

/**
 * Thread safe Metric buffer implementation. Converts the input {@link Metric} objects into Map<String, Map<Long,
 * Float>> entries. The Map<Long, Float> values can directly be used by the TimelineMetric#getMetricValues().
 * 
 * @author tzolov@apache.org
 *
 */
public class MetricBuffer implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(MetricBuffer.class);

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

    public MetricBuffer() {
        this.bufferLock = new ReentrantLock();
        this.bufferedMetricCount = new AtomicLong(0);
        this.metricBuffer = new HashMap<String, Map<Long, Float>>();
    }

    /**
     * Add new {@link Metric} to the buffer (thread safe)
     * 
     * @param metric
     */
    public void add(Metric<?> metric) {

        try {
            if (bufferLock.tryLock() || bufferLock.tryLock(5, TimeUnit.SECONDS)) {
                try {

                    String metricName = metric.getName();

                    if (!metricBuffer.containsKey(metricName)) {
                        metricBuffer.put(metricName, new Long2FloatArrayMap());
                    }

                    metricBuffer.get(metricName).put(metric.getTimestamp().getTime(), metric.getValue().floatValue());

                    bufferedMetricCount.incrementAndGet();

                } finally {
                    bufferLock.unlock();
                }
            }
        } catch (InterruptedException e) {
            logger.warn("Failed to buffer metric: " + metric + " due to locked thread!");
        }
    }

    /**
     * @return Returns the number of {@link Metric} added to buffer;
     */
    public long size() {
        return bufferedMetricCount.get();
    }

    /**
     * @return cleans the buffer and returns the last state.
     */
    public Map<String, Map<Long, Float>> flush() {

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

    @Override
    public void close() throws IOException {
        flush();
    }
}
