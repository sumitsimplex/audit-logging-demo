# Audit Logging Demo to Elastic Stack

A demo application for audit logging interceptor and logging integration to elastic stack.

## Installation

<pre>
mvm clean install
mvn spring-boot:run
</pre>

(optional) Run Spring Boot app with java -jar command java -jar target/demo-0.0.1-SNAPSHOT.jar.

<pre>
java -jar target/demo-0.0.1-SNAPSHOT.jar
</pre>

## Usage

### API Endpoints
This application provides the following RESTful API endpoints for demo:

- [GET] '/api/hello'
- [GET] '/api/Food-Details'

# **Documentation**
## 1. **<u>Audit logging event interceptor</u>**
Actuator has support for making it easier to publish audit events for your application, both to track authentication, authorization and other security events, but also to make it easier to add your own custom events.

Actuator for audit logs requires to do two things:

1.1 Add the dependency
```
...
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
...

```

1.2 To store the audit events, create **AuditEventRepository** bean, in this demo application I create a default implementation with in-memory storage of audit logs, as below.
````agsl
package ie.rsa.auditlogging.demo.config;

import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.boot.actuate.audit.InMemoryAuditEventRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuditingConfiguration {
  @Bean
  public AuditEventRepository auditEventRepository() {
    // constructor also takes a default number of items to store in-memory for tuning
    return new InMemoryAuditEventRepository();
  }
}
````
Note: It will be required to have a self implemented AuditEventRepository for production setup as per below document from spring.io

link => https://docs.spring.io/spring-boot/docs/3.0.5/reference/htmlsingle/#actuator

"Once Spring Security is in play, Spring Boot Actuator has a flexible audit framework that publishes events (by default, “authentication success”, “failure” and “access denied” exceptions). This feature can be very useful for reporting and for implementing a lock-out policy based on authentication failures.

Auditing can be enabled by providing a bean of type AuditEventRepository in your application’s configuration. For convenience, Spring Boot offers an InMemoryAuditEventRepository. InMemoryAuditEventRepository has limited capabilities and we recommend using it only for development environments. For production environments, consider creating your own alternative AuditEventRepository implementation."

Source: https://docs.spring.io/spring-boot/docs/3.0.5/reference/htmlsingle/#actuator.auditing

It is not required as we plan to use logging through event listener to Elastic stack (with Kafka).


## 2. **<u>Logback configuration with Kafka appender</u>**
(optionally can be configured directly to logstash, but not recommended)

Logback configuration requires two steps:

2.1 Add all the dependencies, for logback, logstash and kafka
```
...
    <!-- https://mvnrepository.com/artifact/net.logstash.logback/logstash-logback-encoder -->
        <dependency>
            <groupId>net.logstash.logback</groupId>
            <artifactId>logstash-logback-encoder</artifactId>
            <version>6.6</version>
        </dependency>
        <dependency>
            <groupId>de.idealo.whitelabels</groupId>
            <artifactId>logstash-logback-http</artifactId>
            <version>1.1.1</version>
        </dependency>
    <!-- https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-starter-log4j2 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-log4j2</artifactId>
            <version>3.0.4</version>
        </dependency>
        <dependency>
            <groupId>com.github.danielwegener</groupId>
            <artifactId>logback-kafka-appender</artifactId>
            <version>0.2.0-RC1</version>
            <scope>runtime</scope>
        </dependency>
...

```

2.2 logback-spring.xml as below with kafka.host and kafka.port specified in application.properties
````agsl
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <springProperty scope="context" name="kafkaHost" source="kafka.host"/>
    <springProperty scope="context" name="kafkaPort" source="kafka.port"/>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern> %d{dd-MM-yyyy HH:mm:ss.SSS} [%thread] %-5level %logger{36}.%M - %msg%n </pattern>
        </encoder>
    </appender>
    <appender name="SAVE-TO-FILE" class="ch.qos.logback.core.FileAppender">
        <file>logs/application.log</file>
        <encoder class="ch.qos.logback.classic.encoder.PatternLayoutEncoder">
            <Pattern> %d{dd-MM-yyyy HH:mm:ss.SSS} [%thread] %-5level %logger{36}.%M - %msg%n </Pattern>
        </encoder>
    </appender>
    <appender name="kafkaAppender" class="com.github.danielwegener.logback.kafka.KafkaAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
        <topic>audit-log-test</topic>
        <keyingStrategy class="com.github.danielwegener.logback.kafka.keying.NoKeyKeyingStrategy" />
        <deliveryStrategy class="com.github.danielwegener.logback.kafka.delivery.AsynchronousDeliveryStrategy" />
        <!-- Optional parameter to use a fixed partition -->
        <!-- <partition>0</partition> -->
        <!-- Optional parameter to include log timestamps into the kafka message -->
        <!-- <appendTimestamp>true</appendTimestamp> -->
        <!-- each <producerConfig> translates to regular kafka-client config (format: key=value) -->
        <!-- producer configs are documented here: https://kafka.apache.org/documentation.html#newproducerconfigs -->
        <!-- bootstrap.servers is the only mandatory producerConfig -->
        <producerConfig>bootstrap.servers=${kafkaHost}:${kafkaPort}</producerConfig>
        <!-- this is the fallback appender if kafka is not available. -->
        <appender-ref ref="SAVE-TO-FILE" />
    </appender>
    <logger name="ie.rsa.auditlogging.demo" additivity="false" level="info">
        <appender-ref ref="SAVE-TO-FILE" />
        <appender-ref ref="STDOUT" />
        <appender-ref ref="kafkaAppender" />
    </logger>
    <root level="INFO">
        <appender-ref ref="STDOUT" />
    </root>
</configuration>
````