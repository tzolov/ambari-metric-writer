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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.metrics.ambari.domain.TimelineMetrics;
import org.springframework.boot.actuate.metrics.ambari.restclient.TimelineRestClient;

public class AmbariMetricWriter extends AbstractAmbariMetricWriter {

	private static final Logger logger = LoggerFactory.getLogger(AmbariMetricWriter.class);

	private TimelineRestClient timelineRestClient;

	public AmbariMetricWriter(String metricsCollectorHost, String metricsCollectorPort, String applicationId,
			String hostName, int metricsBufferSize) {

		super(metricsCollectorHost, metricsCollectorPort, applicationId, hostName, metricsBufferSize);

		this.timelineRestClient = new TimelineRestClient(metricsCollectorHost, metricsCollectorPort);
	}

	@Override
	protected void doSendMetrics(TimelineMetrics timelineMetrics) {

		logger.debug("Send metrics");

		// REST call to send the metrics to the Ambari Timeline Server
		timelineRestClient.putMetrics(timelineMetrics);

		// Return the TimelineMetric objects to the pool
		freePoolObjects(timelineMetrics);
	}

	// Test purpose only
	public TimelineRestClient getTimelineRestClient() {
		return timelineRestClient;
	}

}
