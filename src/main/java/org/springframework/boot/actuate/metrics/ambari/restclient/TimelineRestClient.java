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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.metrics.ambari.domain.TimelineMetrics;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;

public class TimelineRestClient {

	private static final Logger logger = LoggerFactory.getLogger(TimelineRestClient.class);

	private static final String AMBARI_METRICS_COLLECTOR_URL = "http://{ambari-metrics-collector}:{port}/ws/v1/timeline/metrics";

	private String ambariMetricsCollectorHost = "localhost";

	private String ambariMetricsCollectorPort = "6188";

	/**
	 * The media type to use to serialize and accept responses from the server. Defaults to "application/json".
	 */
	private MediaType mediaType = MediaType.APPLICATION_JSON;

	private RestTemplate restTemplate = null;

	public TimelineRestClient(String ambariMetricsCollectorHost, String ambariMetricsCollectorPort) {
		this.ambariMetricsCollectorHost = ambariMetricsCollectorHost;
		this.ambariMetricsCollectorPort = ambariMetricsCollectorPort;
		this.restTemplate = createTimelineClient();
	}

	@SuppressWarnings("rawtypes")
	public boolean putMetrics(TimelineMetrics metrics) {

		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(this.mediaType));
		headers.setContentType(this.mediaType);

		ResponseEntity<Map> response = restTemplate.postForEntity(AMBARI_METRICS_COLLECTOR_URL,
				new HttpEntity<TimelineMetrics>(metrics, headers), Map.class, ambariMetricsCollectorHost,
				ambariMetricsCollectorPort);

		if (!response.getStatusCode().is2xxSuccessful()) {
			logger.warn("Cannot write metrics " + metrics + " values): " + response.getBody());
		}

		return response.getStatusCode().is2xxSuccessful();
	}

	private RestTemplate createTimelineClient() {

		MappingJackson2HttpMessageConverter mc = new MappingJackson2HttpMessageConverter();
		JaxbAnnotationModule module = new JaxbAnnotationModule();
		mc.getObjectMapper().registerModule(module);

		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getMessageConverters().clear();
		restTemplate.getMessageConverters().add(mc);

		return restTemplate;
	}

	// test only
	public RestTemplate getRestTemplate() {
		return restTemplate;
	}
}
