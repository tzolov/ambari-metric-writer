package org.springframework.boot.actuate.metrics.ambari.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.ExportMetricWriter;
import org.springframework.boot.actuate.metrics.ambari.DummyAmbariMetricWriter;
import org.springframework.boot.actuate.metrics.writer.MetricWriter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
		return new DummyAmbariMetricWriter(properties.getTimelineHost(), "" + properties.getTimelinePort(),
				properties.getApplicationId(), properties.getHostName(), properties.getMetricsBufferSize());
	}
}
