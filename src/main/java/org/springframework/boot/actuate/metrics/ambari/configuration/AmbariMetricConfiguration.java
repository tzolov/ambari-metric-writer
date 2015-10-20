package org.springframework.boot.actuate.metrics.ambari.configuration;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.ExportMetricWriter;
import org.springframework.boot.actuate.metrics.ambari.AmbariMetricWriter;
import org.springframework.boot.actuate.metrics.ambari.AsyncAmbariMetricWriter;
import org.springframework.boot.actuate.metrics.ambari.DummyAmbariMetricWriter;
import org.springframework.boot.actuate.metrics.writer.MetricWriter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@ConditionalOnProperty(value = "spring.metrics.export.ambari.enabled", matchIfMissing = true)
@EnableConfigurationProperties
public class AmbariMetricConfiguration {

    @Autowired
    private AmbariMetricProperties properties;

    @Bean
    @ExportMetricWriter
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "spring.metrics.export.ambari", name = "timeline-host")
    public MetricWriter ambariMetricExporter() {

        if (StringUtils.isEmpty(properties.getWriterType())
                || !Arrays.asList("sync", "async", "dummy").contains(properties.getWriterType().trim())) {

            throw new java.lang.IllegalArgumentException("The spring.metrics.export.ambari.writer-type "
                    + " properterty must be set to: sync, async or dummy");
        }

        MetricWriter metricWriter = null;

        if (properties.getWriterType().trim().equalsIgnoreCase("sync")) {

            metricWriter = new AmbariMetricWriter(properties.getTimelineHost(), "" + properties.getTimelinePort(),
                    properties.getApplicationId(), properties.getHostName(), properties.getMetricsBufferSize());

        } else if (properties.getWriterType().trim().equalsIgnoreCase("async")) {

            metricWriter = new AsyncAmbariMetricWriter(properties.getTimelineHost(), "" + properties.getTimelinePort(),
                    properties.getApplicationId(), properties.getHostName(), properties.getMetricsBufferSize());
        } else {

            metricWriter = new DummyAmbariMetricWriter(properties.getTimelineHost(), "" + properties.getTimelinePort(),
                    properties.getApplicationId(), properties.getHostName(), properties.getMetricsBufferSize());
        }

        return metricWriter;
    }
}
