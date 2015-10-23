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

import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.actuate.metrics.ambari.domain.TimelineMetric;
import static org.junit.Assert.*;

public class TimelineMetricFactoryTest {

    private TimelineMetricFactory timelineMetricFactory;

    @Before
    public void before() {
        timelineMetricFactory = new TimelineMetricFactory();
    }

    @Test
    public void testCreate() throws Exception {
        TimelineMetric metric = timelineMetricFactory.create();

        
        assertNotNull(metric);
        assertNull(metric.getAppId());
        assertNull(metric.getHostName());
        assertNull(metric.getInstanceId());
        assertTrue(metric.getStartTime() == 0);
        assertNull(metric.getMetricName());
        assertNotNull(metric.getMetricValues());
    }

    @Test
    public void testPassivateObject() throws Exception {
        TimelineMetric metric = timelineMetricFactory.create();

        metric.setAppId("appId");
        metric.setHostName("hostName");
        metric.setInstanceId("instanceId");
        metric.setStartTime(666666);
        metric.setMetricName("MetricName");
        metric.setMetricValues(new HashMap<Long, Double>());

        assertNotNull(metric);
        assertNotNull(metric.getAppId());
        assertNotNull(metric.getHostName());
        assertNotNull(metric.getInstanceId());
        assertTrue(metric.getStartTime() > 0);
        assertNotNull(metric.getMetricName());
        assertNotNull(metric.getMetricValues());

        timelineMetricFactory.passivateObject(timelineMetricFactory.wrap(metric));

        assertNull(metric.getAppId());
        assertNull(metric.getHostName());
        assertNull(metric.getInstanceId());
        assertTrue(metric.getStartTime() < 0);
        assertNull(metric.getMetricName());
        assertNotNull(metric.getMetricValues());
    }
}
