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

import java.util.Arrays;
import java.util.Map;

import org.springframework.boot.actuate.metrics.ambari.domain.TimelineMetrics;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.client.AsyncRestTemplate;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;

/**
 * Asynchronous client that uses the Ambari Metrics Collector REST API to transmit {@link TimelineMetrics}.
 * https://cwiki.apache.org/confluence/display/AMBARI/Metrics+Collector+API+Specification
 * 
 * @author tzolov@apache.org
 */
public class AsyncAmbariMetricsCollectorRestClient {

    private static final String AMBARI_METRICS_COLLECTOR_URL = "http://{host}:{port}/ws/v1/timeline/metrics";

    private String ambariMetricsCollectorHost = "localhost";

    private String ambariMetricsCollectorPort = "6188";

    /**
     * The media type to use to serialize and accept responses from the server. Defaults to "application/json".
     */
    private MediaType mediaType = MediaType.APPLICATION_JSON;

    private AsyncRestTemplate restTemplate = null;

    public AsyncAmbariMetricsCollectorRestClient(String ambariMetricsCollectorHost, String ambariMetricsCollectorPort) {
        this.ambariMetricsCollectorHost = ambariMetricsCollectorHost;
        this.ambariMetricsCollectorPort = ambariMetricsCollectorPort;
        this.restTemplate = createTimelineClient();
    }

    @SuppressWarnings("rawtypes")
    public void putMetrics(TimelineMetrics metrics, ListenableFutureCallback<ResponseEntity<Map>> callback) {

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(this.mediaType));
        headers.setContentType(this.mediaType);

        ListenableFuture<ResponseEntity<Map>> asyncResponse = restTemplate.postForEntity(AMBARI_METRICS_COLLECTOR_URL,
                new HttpEntity<TimelineMetrics>(metrics, headers), Map.class, ambariMetricsCollectorHost,
                ambariMetricsCollectorPort);

        asyncResponse.addCallback(callback);
    }

    private AsyncRestTemplate createTimelineClient() {

        MappingJackson2HttpMessageConverter mc = new MappingJackson2HttpMessageConverter();
        JaxbAnnotationModule module = new JaxbAnnotationModule();
        mc.getObjectMapper().registerModule(module);
        mc.getObjectMapper().setSerializationInclusion(Include.NON_NULL);
        
        AsyncRestTemplate restTemplate = new AsyncRestTemplate();
        restTemplate.getMessageConverters().clear();
        restTemplate.getMessageConverters().add(mc);

        SimpleClientHttpRequestFactory requestFactory = (SimpleClientHttpRequestFactory) restTemplate
                .getAsyncRequestFactory();
        requestFactory.setReadTimeout(5000);
        requestFactory.setConnectTimeout(5000);

        return restTemplate;
    }

    // test only
    public AsyncRestTemplate getRestTemplate() {
        return restTemplate;
    }
}
