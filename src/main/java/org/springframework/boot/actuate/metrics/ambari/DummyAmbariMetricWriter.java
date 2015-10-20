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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.ambari.domain.TimelineMetrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;

public class DummyAmbariMetricWriter extends AbstractAmbariMetricWriter {

    private static final Logger logger = LoggerFactory.getLogger(DummyAmbariMetricWriter.class);
    private ObjectMapper objectMapper;

    public DummyAmbariMetricWriter(String metricsCollectorHost, String metricsCollectorPort, String applicationId,
            String hostName, int metricsBufferSize) {

        super(applicationId, hostName, metricsBufferSize);

        JaxbAnnotationModule module = new JaxbAnnotationModule();
        objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.registerModule(module);
    }

    @Override
    public void set(Metric<?> metric) {
        // logger.info("Metric Count: " + getBufferedMetricCount().get());
        super.set(metric);
    }

    @Override
    protected void doSendMetrics(TimelineMetrics timelineMetrics) {

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            objectMapper.writeValue(out, timelineMetrics);

            logger.info("Send New Metrics: \n" + out.toString());

            freePoolObjects(timelineMetrics);
        } catch (IOException e) {
            logger.error("", e);
        }
    }
}
