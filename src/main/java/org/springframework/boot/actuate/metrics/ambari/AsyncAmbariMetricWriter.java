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

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.metrics.ambari.domain.TimelineMetrics;
import org.springframework.boot.actuate.metrics.ambari.restclient.AsyncAmbariMetricsCollectorRestClient;
import org.springframework.http.ResponseEntity;
import org.springframework.util.concurrent.ListenableFutureCallback;

public class AsyncAmbariMetricWriter extends AmbariMetricWriter {

    private static final Logger logger = LoggerFactory.getLogger(AsyncAmbariMetricWriter.class);

    private AsyncAmbariMetricsCollectorRestClient metricsCollectorRestClient;

    public AsyncAmbariMetricWriter(String metricsCollectorHost, String metricsCollectorPort, String applicationId,
            String hostName, String instanceId, int metricsBufferSize) {

        super(applicationId, hostName, instanceId, metricsBufferSize);

        this.metricsCollectorRestClient = new AsyncAmbariMetricsCollectorRestClient(metricsCollectorHost,
                metricsCollectorPort);
    }

    @Override
    protected void sendMetricsAndCleanPool(TimelineMetrics timelineMetrics) {
        // Override the default behavior to allow cleaning the pool object asynchronously
        doSendMetrics(timelineMetrics);
    }

    @Override
    protected void doSendMetrics(TimelineMetrics timelineMetrics) {
        // Send the metrics to the Ambari Metrics Collector
        metricsCollectorRestClient.putMetrics(timelineMetrics, new ResponseListener(timelineMetrics));
    }

    @SuppressWarnings("rawtypes")
    private class ResponseListener implements ListenableFutureCallback<ResponseEntity<Map>> {

        private TimelineMetrics timelineMetrics;

        public ResponseListener(TimelineMetrics timelineMetrics) {
            this.timelineMetrics = timelineMetrics;
        }

        @Override
        public void onFailure(Throwable ex) {
            logger.warn("Failed to send timeline metrics!", ex);
            // Return the TimelineMetric objects to the pool
            returnMetricPoolObjects(timelineMetrics);
        }

        @Override
        public void onSuccess(ResponseEntity<Map> result) {
            // Return the TimelineMetric objects to the pool
            returnMetricPoolObjects(timelineMetrics);
        }
    }

    // Test purpose only
    public AsyncAmbariMetricsCollectorRestClient getTimelineRestClient() {
        return metricsCollectorRestClient;
    }
}
