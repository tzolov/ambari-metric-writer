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
package org.springframework.boot.actuate.metrics.ambari.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("spring.metrics.export.ambari")
public class AmbariMetricProperties {

    /**
     * Ambari Metrics Collector server host to receive exported metrics.
     */
    private String metricsCollectorHost;

    /**
     * Ambari Metrics Collector server port to receive exported metrics. Defaults to 6188.
     */
    private int metricsCollectorPort = 6188;

    /**
     * Uniquely identify service/application within Ambari Metrics Collector.
     */
    @Value("${spring.application.name:application}.${random.value:0000}")
    private String applicationId;

    /**
     * Used as second key part when storing metrics in the Ambari Metrics Collector.
     */
    @Value("${server.address:hostname}")
    private String hostName;

    /**
     * Optional application instance id
     */
    @Value("${spring.application.index:${server.port:${PORT:}}}")
    private String instanceId = null;

    /**
     * Metric buffer size to fill before posting data to server. Defaults to 100.
     */
    private int metricsBufferSize = 100;

    /**
     * Ambari Metric Writer implementation. Accepted values are:
     * <ul>
     * <li>sync (AmbariMetricWriter)</li>
     * <li>async (AsyncAmbariMetricWriter)</li> and
     * <li>dummy (DummyAmbariMetricWriter)</li>
     * 
     * <br/>
     * The default type is <bold>sync</bold>.
     * </ul>
     */
    private String writerType = "sync";

    public String getWriterType() {
        return writerType;
    }

    public void setWriterType(String writerType) {
        this.writerType = writerType;
    }

    public String getMetricsCollectorHost() {
        return metricsCollectorHost;
    }

    public void setMetricsCollectorHost(String metricsCollectorHost) {
        this.metricsCollectorHost = metricsCollectorHost;
    }

    public int getMetricsCollectorPort() {
        return metricsCollectorPort;
    }

    public void setMetricsCollectorPort(int metricsCollectorPort) {
        this.metricsCollectorPort = metricsCollectorPort;
    }

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

    public int getMetricsBufferSize() {
        return metricsBufferSize;
    }

    public void setMetricsBufferSize(int metricsBufferSize) {
        this.metricsBufferSize = metricsBufferSize;
    }
}
