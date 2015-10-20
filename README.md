# Spring Boot to Apache Ambari Metric Writer

### Overview
The `ambari-metric-writer` allows you to export (at real time) all Spring [application metrics] (http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#production-ready-metrics) to [Apache Ambari Metric Collector](https://cwiki.apache.org/confluence/display/AMBARI/Metrics+Collector+API+Specification)

It provides a Spring Boot `MetricWriter` for the Apache [Ambari Metric System](https://cwiki.apache.org/confluence/display/AMBARI/Metrics), writing metrics to the [HTTP endpoint](https://cwiki.apache.org/confluence/display/AMBARI/Metrics+Collector+API+Specification) provided by the server. Data is buffered according to the `buffer-size` property, and flushed automatically when the buffer size is reached. Users should either manually `flushMetricBuffer()` or consider adding a `Scheduled` task to flush periodically.

The [Spring Boot Actuator](http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#production-ready) includes a [Metrics Service](http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#production-ready-metrics) that automatically records all metrics and expoes them through the `metrics` endpoint. 
The Metric Service provides a [PublicMetrics](http://github.com/spring-projects/spring-boot/tree/v1.2.7.RELEASE/spring-boot-actuator/src/main/java/org/springframework/boot/actuate/endpoint/PublicMetrics.java) interface that you can implement to expose custom metrics (look at [SystemPublicMetrics](http://github.com/spring-projects/spring-boot/tree/v1.2.7.RELEASE/spring-boot-actuator/src/main/java/org/springframework/boot/actuate/endpoint/SystemPublicMetrics.java) for an example). 

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
      <version>0.0.7</version>
    </dependency>    
    
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
```

* Add `AmbariMetricWriter.class` to `@SpringBootApplication` scan path and set the `spring.metrics.export.ambari.timeline-host` property via the `application.properties` or the command line (`--spring.metrics.export.ambari.timeline-host=timelineServerHost`).

```java
import org.springframework.boot.actuate.metrics.ambari.AmbariMetricWriter;

@SpringBootApplication(scanBasePackageClasses = { AmbariMetricWriter.class })
public class YourSpringBootApplication {
	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(YourSpringBootApplication.class);
		app.run(args);
	}
}
```

* Alternatively provide a `@Bean` of type `AmbariMetricWriter` and mark it `@ExportMetricWriter` metrics are exported to Ambari Metric Collector. 

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

Various metrics properties can be specified inside your `application.properties`/`application.yml` file or as command line switches. 

Following section provides a list common Spring Boot metric properties:

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

In addition to the common properties following properties configure the Ambari Metric Export:

| Property        | Default Value           | Description  |
| ------------- |:-------------:| -----|
| spring.metrics.export.ambari.writer-type | sync  | `synch` (default) uses synchronous REST calls to send the metrics to the server. The `async` uses asynchronous REST calls to transmit the metrics and `dummy` prints the metrics to the log.  |
| spring.metrics.export.ambari.enabled | true  | When set to false the ambari export is deactivated  |
| spring.metrics.export.ambari.timeline-host |  | Host of a Ambari Timeline server to receive exported metrics |
| spring.metrics.export.ambari.timeline-port | 6188 | Port of a Ambari Timeline server to receive exported metrics |
| spring.metrics.export.ambari.application-id |  | Uniquely identify service/application within Ambari |
| spring.metrics.export.ambari.host-name |  |  |
| spring.metrics.export.ambari.instance-id | "nil"  |  |
| spring.metrics.export.ambari.metrics-buffer-size | 100 | Metric buffer size to fill before posting data to server |

