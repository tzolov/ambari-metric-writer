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

import java.util.Map;
import java.util.TreeMap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * JSON encodable object representing a single metric transmitted to the Ambari Metrics Collector:
 * https://cwiki.apache.org/confluence/display/AMBARI/Metrics+Collector+API+Specification
 * 
 * (This class is based on: https://github.com/apache/ambari/blob/291b7cbf5852db3fa37f4f180158d0958241e05b
 * /ambari-metrics/ambari-metrics-common/src/main/java/org/apache/hadoop/metrics2/sink/timeline/TimelineMetric.java)
 * 
 * @author tzolov@apache.org
 *
 */
@XmlRootElement(name = "metric")
@XmlAccessorType(XmlAccessType.NONE)
public class TimelineMetric implements Comparable<TimelineMetric> {

    /**
     * First key part, important consideration while querying from HFile storage.
     */
    private String metricName;

    /**
     * Second key part.
     */
    private String hostName;

    /**
     * Timestamp on server when first metric write request was received
     */
    private long timestamp;

    /**
     * Uniquely identify service.
     */
    private String appId;

    /**
     * (Optional) Second key part to identify instance/ component.
     */
    private String instanceId;

    /**
     * Start of the timeseries data
     */
    private long startTime;

//    /**
//     * Metric number type
//     */
//    private String type;

    /**
     * Metric values represented as a list of (metricTimestamp, metricValue) pairs.
     */
    private Map<Long, Double> metricValues = new TreeMap<Long, Double>();

    public TimelineMetric() {
    }

    // copy constructor
    public TimelineMetric(TimelineMetric metric) {
        setMetricName(metric.getMetricName());
//        setType(metric.getType());
        setTimestamp(metric.getTimestamp());
        setAppId(metric.getAppId());
        setInstanceId(metric.getInstanceId());
        setHostName(metric.getHostName());
        setStartTime(metric.getStartTime());
        setMetricValues(new TreeMap<Long, Double>(metric.getMetricValues()));
    }

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

//    @XmlElement(name = "type")
//    public String getType() {
//        return this.type;
//    }
//
//    public void setType(String type) {
//        this.type = type;
//    }

    @XmlElement(name = "timestamp")
    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @XmlElement(name = "starttime")
    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    @XmlElement(name = "metrics")
    public Map<Long, Double> getMetricValues() {
        return metricValues;
    }

    public void setMetricValues(Map<Long, Double> metricValues) {
        this.metricValues = metricValues;
    }

    public void addMetricValues(Map<Long, Double> metricValues) {
        this.metricValues.putAll(metricValues);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        TimelineMetric metric = (TimelineMetric) o;

        if (startTime != metric.startTime)
            return false;
        if (!appId.equals(metric.appId))
            return false;
        if (hostName != null ? !hostName.equals(metric.hostName) : metric.hostName != null)
            return false;
        if (instanceId != null ? !instanceId.equals(metric.instanceId) : metric.instanceId != null)
            return false;
        if (!metricName.equals(metric.metricName))
            return false;

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