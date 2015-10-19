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

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.springframework.boot.actuate.metrics.ambari.domain.TimelineMetrics;

public class TimelineMetricsFactory extends BasePooledObjectFactory<TimelineMetrics> {

	@Override
	public TimelineMetrics create() throws Exception {
		return new TimelineMetrics();
	}

	@Override
	public PooledObject<TimelineMetrics> wrap(TimelineMetrics tlms) {
		return new DefaultPooledObject<TimelineMetrics>(tlms);
	}

	@Override
	public void passivateObject(PooledObject<TimelineMetrics> tlms) throws Exception {
		// When the TimelineMetrics is returned to the pool, clean the previous state
		tlms.getObject().getMetrics().clear();
	}
}
