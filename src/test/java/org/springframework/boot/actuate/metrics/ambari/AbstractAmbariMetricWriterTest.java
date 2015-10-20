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

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Date;
import java.util.Random;

import org.junit.Test;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;

public abstract class AbstractAmbariMetricWriterTest {

    protected MockRestServiceServer mockServer;
    protected AbstractAmbariMetricWriter ambariMetricWriter;

    private Random random = new Random();

    @Test
    public void postSuccessfullyOnFlush() {
        mockServer.expect(requestTo("http://localhost:6188/ws/v1/timeline/metrics")).andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.metrics[*].metricname", contains("metric1"))).andRespond(withSuccess());

        ambariMetricWriter.set(metric("metric1", random.nextLong(), 666f));

        ambariMetricWriter.flushMetricBuffer();

        mockServer.verify();
    }

    @Test
    public void flushAutomaticlly() {
        mockServer.expect(requestTo("http://localhost:6188/ws/v1/timeline/metrics")).andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.metrics[*].metricname", contains("metric1"))).andRespond(withSuccess());

        ambariMetricWriter.setBufferSize(0);

        ambariMetricWriter.set(metric("metric1", random.nextLong(), 666f));

        mockServer.verify();

        assertEquals(1, ambariMetricWriter.getTimelineMetricsPool().getBorrowedCount());
        assertEquals(1, ambariMetricWriter.getTimelineMetricPool().getBorrowedCount());

        assertEquals("Not released TimelineMetrics pool object", 0, ambariMetricWriter.getTimelineMetricsPool()
                .getNumActive());
        assertEquals("Not released TimelineMetric pool object", 0, ambariMetricWriter.getTimelineMetricPool()
                .getNumActive());
    }

    @Test
    public void consecutiveAutomaticFlushes() {

        int bufferSize = 20;

        // Total number of new metrics set
        long metricSetCount = 220;

        int expectedFlushCount = (int) metricSetCount / (bufferSize + 1);

        ambariMetricWriter.setBufferSize(bufferSize);

        for (int c = 0; c < expectedFlushCount; c++) {
            mockServer.expect(requestTo("http://localhost:6188/ws/v1/timeline/metrics"))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(jsonPath("$.metrics[*].metricname", contains("metric1"))).andRespond(withSuccess());
        }

        for (int b = 0; b < metricSetCount; b++) {
            ambariMetricWriter.set(metric("metric1", random.nextLong(), 666f));
        }

        mockServer.verify();

        assertEquals("Not released TimelineMetrics pool object", 0, ambariMetricWriter.getTimelineMetricsPool()
                .getNumActive());
        assertEquals("Not released TimelineMetric pool object", 0, ambariMetricWriter.getTimelineMetricPool()
                .getNumActive());
    }

    private static Metric<Number> metric(String name, long timestamp, float value) {
        return new Metric<Number>(name, value, new Date(timestamp));
    }
}
