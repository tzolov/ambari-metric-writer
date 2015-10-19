# Spring Boot to Apache Ambari Metric Writer

### Overview

[Spring Boot Actuator](http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#production-ready) includes a [Metrics Service](http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#production-ready-metrics). Later provides a [PublicMetrics](http://github.com/spring-projects/spring-boot/tree/v1.2.7.RELEASE/spring-boot-actuator/src/main/java/org/springframework/boot/actuate/endpoint/PublicMetrics.java) interface that you can implement to expose metrics that you cannot record via one of those two mechanisms. Look at [SystemPublicMetrics](http://github.com/spring-projects/spring-boot/tree/v1.2.7.RELEASE/spring-boot-actuator/src/main/java/org/springframework/boot/actuate/endpoint/SystemPublicMetrics.java) for an example. Metrics for all HTTP requests are automatically recorded and expoed through the `metrics` endpoint. 


The `ambari-metric-writer` allows you to export (at real time) all application metrics to [Apache Ambari Metric Collector](https://cwiki.apache.org/confluence/display/AMBARI/Metrics+Collector+API+Specification)


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
