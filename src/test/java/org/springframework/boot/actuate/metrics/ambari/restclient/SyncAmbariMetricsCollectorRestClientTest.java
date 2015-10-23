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
package org.springframework.boot.actuate.metrics.ambari.restclient;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.actuate.metrics.ambari.domain.TimelineMetric;
import org.springframework.boot.actuate.metrics.ambari.domain.TimelineMetrics;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;

public class SyncAmbariMetricsCollectorRestClientTest {

    private MockRestServiceServer mockServer;
    private SyncAmbariMetricsCollectorRestClient restClient;

    private String ambariMetricsCollectorHost = "localhost";
    private String ambariMetricsCollectorPort = "6188";

    @Before
    public void before() {

        restClient = new SyncAmbariMetricsCollectorRestClient(ambariMetricsCollectorHost, ambariMetricsCollectorPort);
        mockServer = MockRestServiceServer.createServer(restClient.getRestTemplate());
    }

    @Test
    public void sendSuccess() {

        mockServer
                .expect(requestTo("http://" + ambariMetricsCollectorHost + ":" + ambariMetricsCollectorPort
                        + "/ws/v1/timeline/metrics"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(
                        content().string(
                                "{\"metrics\":[{\"metricname\":\"Metric Name\",\"hostname\":\"a host\",\"timestamp\":0,"
                                        + "\"appid\":\"appid\",\"instanceid\":\"instance id\",\"starttime\":696969,"
                                        + "\"metrics\":{\"666666\":666.666,\"999999\":999.999}}]}"))
                .andRespond(withSuccess());

        TimelineMetric tm = new TimelineMetric();
        tm.setAppId("appid");
        tm.setHostName("a host");
        tm.setInstanceId("instance id");
        tm.setMetricName("Metric Name");
        tm.setStartTime(696969L);
        Map<Long, Double> metricValues = new TreeMap<Long, Double>();
        metricValues.put(666666L, 666.666);
        metricValues.put(999999L, 999.999);
        tm.setMetricValues(metricValues);

        TimelineMetrics tms = new TimelineMetrics();
        tms.setMetrics(Arrays.asList(tm));

        boolean successful = restClient.putMetrics(tms);

        mockServer.verify();

        Assert.assertTrue(successful);
    }
}
