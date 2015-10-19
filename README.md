# Spring Boot to Apache Ambari Metric Writer

### Overview

[Spring Boot Actuator](http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#production-ready) includes a [Metrics Service](http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#production-ready-metrics). Later provides a [PublicMetrics](http://github.com/spring-projects/spring-boot/tree/v1.2.7.RELEASE/spring-boot-actuator/src/main/java/org/springframework/boot/actuate/endpoint/PublicMetrics.java) interface that you can implement to expose metrics that you cannot record via one of those two mechanisms. Look at [SystemPublicMetrics](http://github.com/spring-projects/spring-boot/tree/v1.2.7.RELEASE/spring-boot-actuator/src/main/java/org/springframework/boot/actuate/endpoint/SystemPublicMetrics.java) for an example. Metrics for all HTTP requests are automatically recorded and expoed through the `metrics` endpoint. 


The `ambari-metric-writer` allows you to export (at real time) all application metrics to [Apache Ambari Metric Collector](https://cwiki.apache.org/confluence/display/AMBARI/Metrics+Collector+API+Specification)


A {@link MetricWriter} for the Aapache Ambari Timeline Server (version 2.1+), writing metrics to the HTTP endpoint provided by the server. Data are buffered according to the {@link #setBufferSize(int) bufferSize} property, and only flushed automatically when the buffer size is reached. Users should either manually {@link #flushMetricBuffer()} after writing a batch of data if that makes sense, or consider adding a {@link Scheduled Scheduled} task to flush periodically.

### How To Use


* Add the big-data maven repository to your pom:

```xml
    <repositories>
      <repository>
        <id>bintray-big-data-maven</id>
        <name>bintray</name>
        <url>http://dl.bintray.com/big-data/maven</url>
      </repository>
    </repositories>    
```

* Add the `ambari-metric-writer` and `spring-boot-starter-actuator` dependencies to your pom:

```xml
    <dependency>
      <groupId>org.springframework.boot.actuate.metrics</groupId>
      <artifactId>ambari-metric-writer</artifactId>
      <version>0.0.5</version>
    </dependency>    
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
```

* Provide a `@Bean` of type `AmbariMetricWriter` and mark it `@ExportMetricWriter` metrics are exported to Ambari Metric Collector. 

```java
import org.springframework.boot.actuate.autoconfigure.ExportMetricWriter;
import org.springframework.boot.actuate.metrics.ambari.AmbariMetricWriter;
import org.springframework.boot.actuate.metrics.writer.MetricWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MySpringBootAppConfiguration {

	@Bean
	@ExportMetricWriter
	public MetricWriter metricExporter() {
		return new AmbariMetricWriter(
			"AmbariMetricCollectorHost",  // AMS host name
			"AmbariMetricsCollectorPort", // AMS port number
			"AppID666",                   // metric primary key
			"hostName9",                  // metric secondary key
			50                            // Buffer size
		);
	}
	....
}
```
### Metrics Application Properties

Various metrics properties can be specified inside your `application.properties`/`application.yml` file or as command line switches. This section provides a list common Spring Boot metric properties and references to the underlying classes that consume them.

| Property        | Default Value           | Description  |
| ------------- |:-------------:| -----|
| spring.metrics.export.aggregate.key-pattern | | pattern that tells the aggregator what to do with the keys from the source repository |
| spring.metrics.export.aggregate.prefix | | prefix for global repository if active |
| spring.metrics.export.enabled | true | flag to disable all metric exports (assuming any MetricWriters are available) |
| spring.metrics.export.delay-millis | 5000 | delay in milliseconds between export ticks |
| spring.metrics.export.send-latest | true | flag to switch off any available optimizations based on not exporting unchanged metric values |
| spring.metrics.export.includes | | list of patterns for metric names to include |
| spring.metrics.export.excludes | | list of patterns for metric names to exclude. Applied after the includes |
| spring.metrics.export.triggers.* | | specific trigger properties per MetricWriter bean name |


