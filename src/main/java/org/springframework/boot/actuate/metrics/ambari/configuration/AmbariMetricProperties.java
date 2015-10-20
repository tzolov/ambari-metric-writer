package org.springframework.boot.actuate.metrics.ambari.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("spring.metrics.export.ambari")
public class AmbariMetricProperties {

    /**
     * Host of a Ambari Timeline server to receive exported metrics.
     */
    private String timelineHost;

    /**
     * Port of a Ambari Timeline server to receive exported metrics.
     */
    private int timelinePort = 6188;

    /**
     * Uniquely identify service/application within Ambari Timeline Server.
     */
    @Value("${spring.application.name:application}.${random.value:0000}")
    private String applicationId;

    /**
     * Used as second key part when storing metrics in the Timeline server.
     */
    @Value("${server.address:hostname}")
    private String hostName;

    /**
     * Optional application instance id
     */
    @Value("${spring.application.index:${server.port:${PORT:null}}}")
    private String instanceId;

    /**
     * Metric buffer size to fill before posting data to server.
     */
    private int metricsBufferSize = 100;

    /**
     * Ambari Metric Writer implementation. Accepted values are:
     * <ul>
     * <li>sync (AmbariMetricWriter)</li>
     * <li>async (AsyncAmbariMetricWriter)</li> and
     * <li>dummy (DummyAmbariMetricWriter)</li>
     * 
     * <br/>
     * The default type is <bold>sync</bold>.
     * </ul>
     */
    private String writerType = "sync";

    public String getWriterType() {
        return writerType;
    }

    public void setWriterType(String writerType) {
        this.writerType = writerType;
    }

    public String getTimelineHost() {
        return timelineHost;
    }

    public void setTimelineHost(String timelineHost) {
        this.timelineHost = timelineHost;
    }

    public int getTimelinePort() {
        return timelinePort;
    }

    public void setTimelinePort(int timelinePort) {
        this.timelinePort = timelinePort;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public int getMetricsBufferSize() {
        return metricsBufferSize;
    }

    public void setMetricsBufferSize(int metricsBufferSize) {
        this.metricsBufferSize = metricsBufferSize;
    }
}
