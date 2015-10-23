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

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.ExportMetricWriter;
import org.springframework.boot.actuate.metrics.ambari.SyncAmbariMetricWriter;
import org.springframework.boot.actuate.metrics.ambari.AsyncAmbariMetricWriter;
import org.springframework.boot.actuate.metrics.ambari.DummyAmbariMetricWriter;
import org.springframework.boot.actuate.metrics.writer.MetricWriter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@ConditionalOnProperty(value = "spring.metrics.export.ambari.enabled", matchIfMissing = true)
@EnableConfigurationProperties
public class AmbariMetricConfiguration {

    @Autowired
    private AmbariMetricProperties properties;

    @Bean
    @ExportMetricWriter
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "spring.metrics.export.ambari", name = "metrics-collector-host")
    public MetricWriter ambariMetricExporter() {

        if (StringUtils.isEmpty(properties.getWriterType())
                || !Arrays.asList("sync", "async", "dummy").contains(properties.getWriterType().trim())) {

            throw new java.lang.IllegalArgumentException("The spring.metrics.export.ambari.writer-type "
                    + " properterty must be set to: sync, async or dummy");
        }

        MetricWriter metricWriter = null;

        if (properties.getWriterType().trim().equalsIgnoreCase("sync")) {

            metricWriter = new SyncAmbariMetricWriter(properties.getMetricsCollectorHost(), ""
                    + properties.getMetricsCollectorPort(), properties.getApplicationId(), properties.getHostName(),
                    properties.getInstanceId(), properties.getMetricsBufferSize());

        } else if (properties.getWriterType().trim().equalsIgnoreCase("async")) {

            metricWriter = new AsyncAmbariMetricWriter(properties.getMetricsCollectorHost(), ""
                    + properties.getMetricsCollectorPort(), properties.getApplicationId(), properties.getHostName(),
                    properties.getInstanceId(), properties.getMetricsBufferSize());
        } else {

            metricWriter = new DummyAmbariMetricWriter(properties.getMetricsCollectorHost(), ""
                    + properties.getMetricsCollectorPort(), properties.getApplicationId(), properties.getHostName(),
                    properties.getInstanceId(), properties.getMetricsBufferSize());
        }

        return metricWriter;
    }
}
