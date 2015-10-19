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
package org.springframework.boot.actuate.metrics.ambari.pool;

import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.actuate.metrics.ambari.domain.TimelineMetric;
import org.springframework.boot.actuate.metrics.ambari.domain.TimelineMetrics;
import org.springframework.util.Assert;

public class TimelineMetricsFactoryTest {

	private TimelineMetricsFactory timelineMetricsFactory;

	@Before
	public void before() {
		timelineMetricsFactory = new TimelineMetricsFactory();
	}

	@Test
	public void testCreate() throws Exception {
		TimelineMetrics metrics = timelineMetricsFactory.create();

		Assert.notNull(metrics);
		Assert.notNull(metrics.getMetrics());
		Assert.isTrue(metrics.getMetrics().size() == 0);
	}

	@Test
	public void testPassivateObject() throws Exception {
		TimelineMetrics metrics = timelineMetricsFactory.create();

		metrics.getMetrics().add(new TimelineMetric());
		metrics.getMetrics().add(new TimelineMetric());
		metrics.getMetrics().add(new TimelineMetric());

		Assert.notNull(metrics);
		Assert.isTrue(metrics.getMetrics().size() == 3);

		timelineMetricsFactory.passivateObject(timelineMetricsFactory.wrap(metrics));

		Assert.notNull(metrics);
		Assert.isTrue(metrics.getMetrics().size() == 0);
	}
}
