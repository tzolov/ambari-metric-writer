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

package org.springframework.boot.actuate.metrics.ambari.domain;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Map;

@XmlRootElement(name = "metric")
@XmlAccessorType(XmlAccessType.NONE)
public class TimelineMetric implements Comparable<TimelineMetric> {

  private String metricName;
  private String appId;
  private String instanceId;
  private String hostName;
  private long startTime;
  private Map<Long, Float> metricValues;

  @XmlElement(name = "metricname")
  public String getMetricName() {
    return metricName;
  }

  public void setMetricName(String metricName) {
    this.metricName = metricName;
  }

  @XmlElement(name = "appid")
  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  @XmlElement(name = "instanceid")
  public String getInstanceId() {
    return instanceId;
  }

  public void setInstanceId(String instanceId) {
    this.instanceId = instanceId;
  }

  @XmlElement(name = "hostname")
  public String getHostName() {
    return hostName;
  }

  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  @XmlElement(name = "starttime")
  public long getStartTime() {
    return startTime;
  }

  public void setStartTime(long startTime) {
    this.startTime = startTime;
  }

  @XmlElement(name = "metrics")
  public Map<Long, Float> getMetricValues() {
    return metricValues;
  }

  public void setMetricValues(Map<Long, Float> metricValues) {
    this.metricValues = metricValues;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    TimelineMetric metric = (TimelineMetric) o;

    if (startTime != metric.startTime) return false;
    if (!appId.equals(metric.appId)) return false;
    if (hostName != null ? !hostName.equals(metric.hostName) : metric.hostName != null)
      return false;
    if (instanceId != null ? !instanceId.equals(metric.instanceId) : metric.instanceId != null)
      return false;
    if (!metricName.equals(metric.metricName)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = metricName.hashCode();
    result = 31 * result + appId.hashCode();
    result = 31 * result + (instanceId != null ? instanceId.hashCode() : 0);
    result = 31 * result + (hostName != null ? hostName.hashCode() : 0);
    result = 31 * result + (int) (startTime ^ (startTime >>> 32));
    return result;
  }

  @Override
  public int compareTo(TimelineMetric other) {
    if (startTime > other.startTime) {
      return -1;
    } else if (startTime < other.startTime) {
      return 1;
    } else {
      return metricName.compareTo(other.metricName);
    }
  }
}