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

import org.junit.Before;
import org.springframework.test.web.client.MockRestServiceServer;

public class AmbariMetricWriterTest extends AbstractAmbariMetricWriterTest {

    private int metricsBufferSize = 10;
    private String ambariMetricsCollectorHost = "localhost";
    private String ambariMetricsCollectorPort = "6188";
    private String applicationId = "applicationId";
    private String instanceId = "instanceId";
    private String hostName = "hostName";

    @Before
    public void before() {

        ambariMetricWriter = new AmbariMetricWriter(ambariMetricsCollectorHost, ambariMetricsCollectorPort,
                applicationId, hostName, instanceId, metricsBufferSize);

        mockServer = MockRestServiceServer.createServer(((AmbariMetricWriter) ambariMetricWriter)
                .getTimelineRestClient().getRestTemplate());
    }
}
