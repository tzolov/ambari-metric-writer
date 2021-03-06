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
import org.springframework.boot.actuate.metrics.ambari.domain.TimelineMetrics;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;

/**
 * Encodes the {@link TimelineMetrics} into JSON and prints it in the log.
 * 
 * @author tzolov@apache.org
 *
 */
public class DummyAmbariMetricWriter extends AmbariMetricWriter {

    private static final Logger logger = LoggerFactory.getLogger(DummyAmbariMetricWriter.class);
    private ObjectMapper objectMapper;

    public DummyAmbariMetricWriter(String metricsCollectorHost, String metricsCollectorPort, String applicationId,
            String hostName, String instanceId, int metricsBufferSize) {

        super(applicationId, hostName, instanceId, metricsBufferSize);

        JaxbAnnotationModule module = new JaxbAnnotationModule();
        objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.registerModule(module);
        objectMapper.setSerializationInclusion(Include.NON_NULL);
    }

    @Override
    protected void doSendMetrics(TimelineMetrics timelineMetrics) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            objectMapper.writeValue(out, timelineMetrics);
            logger.info("New Metrics: \n" + out.toString());
        } catch (IOException e) {
            logger.error("", e);
        }
    }
}
