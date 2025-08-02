# Reload4j Appender

This module provides a Loki appender for reload4j (log4j 1.x compatible logging framework).

## Overview

The reload4j appender allows you to send log messages directly to Grafana Loki from applications using reload4j or log4j 1.x. Since reload4j is binary compatible with log4j 1.x, this appender works with both frameworks.

## Usage

### Maven Dependency

```xml
<dependency>
    <groupId>pl.tkowalcz.tjahzi</groupId>
    <artifactId>reload4j-appender</artifactId>
    <version>${tjahzi.version}</version>
</dependency>
```

For a dependency-free version (includes all dependencies in a single jar):

```xml
<dependency>
    <groupId>pl.tkowalcz.tjahzi</groupId>
    <artifactId>reload4j-appender-nodep</artifactId>
    <version>${tjahzi.version}</version>
</dependency>
```

### Configuration

#### log4j.properties

```properties
# Root logger configuration
log4j.rootLogger=INFO, console, loki

# Console appender
log4j.appender.console=org.apache.log4j.ConsoleAppender
log4j.appender.console.layout=org.apache.log4j.PatternLayout
log4j.appender.console.layout.ConversionPattern=%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n

# Loki appender configuration
log4j.appender.loki=pl.tkowalcz.tjahzi.reload4j.LokiAppender
log4j.appender.loki.layout=org.apache.log4j.PatternLayout
log4j.appender.loki.layout.ConversionPattern=%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n

# Loki appender specific configuration
log4j.appender.loki.host=localhost
log4j.appender.loki.port=3100
log4j.appender.loki.logLevelLabel=level
log4j.appender.loki.loggerNameLabel=logger
log4j.appender.loki.threadNameLabel=thread
log4j.appender.loki.connectionTimeoutMillis=5000
log4j.appender.loki.requestTimeoutMillis=60000
log4j.appender.loki.bufferSizeMegabytes=32
log4j.appender.loki.maxRetries=3
```

#### log4j.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE log4j:configuration SYSTEM "log4j.dtd">
<log4j:configuration xmlns:log4j="http://jakarta.apache.org/log4j/">
    
    <appender name="loki" class="pl.tkowalcz.tjahzi.reload4j.LokiAppender">
        <param name="host" value="localhost"/>
        <param name="port" value="3100"/>
        <param name="logLevelLabel" value="level"/>
        <param name="loggerNameLabel" value="logger"/>
        <param name="threadNameLabel" value="thread"/>
        <param name="connectionTimeoutMillis" value="5000"/>
        <param name="requestTimeoutMillis" value="60000"/>
        <param name="bufferSizeMegabytes" value="32"/>
        <param name="maxRetries" value="3"/>
        
        <layout class="org.apache.log4j.PatternLayout">
            <param name="ConversionPattern" value="%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n"/>
        </layout>
    </appender>
    
    <root>
        <priority value="INFO"/>
        <appender-ref ref="loki"/>
    </root>
    
</log4j:configuration>
```

## Configuration Properties

| Property | Description | Default |
|----------|-------------|---------|
| `url` | Complete Loki URL (alternative to host/port) | - |
| `host` | Loki server hostname | `localhost` |
| `port` | Loki server port | `3100` |
| `logEndpoint` | Loki push endpoint | `/loki/api/v1/push` |
| `connectionTimeoutMillis` | Connection timeout in milliseconds | `5000` |
| `requestTimeoutMillis` | Request timeout in milliseconds | `60000` |
| `logLevelLabel` | Label name for log level | - |
| `loggerNameLabel` | Label name for logger name | - |
| `threadNameLabel` | Label name for thread name | - |
| `bufferSizeMegabytes` | Buffer size in megabytes | `32` |
| `maxRetries` | Maximum number of retries | `3` |
| `useOffHeapBuffer` | Use off-heap buffer | `true` |

## Features

- **Binary compatibility**: Works with both reload4j and log4j 1.x
- **Asynchronous logging**: Non-blocking log message processing
- **Configurable labels**: Support for log level, logger name, and thread name labels
- **MDC support**: Automatic inclusion of MDC properties as labels
- **Buffering**: Efficient batching of log messages
- **Error handling**: Robust error handling with configurable retries
- **Monitoring**: Built-in monitoring and statistics support

## Compatibility

- reload4j 1.2.25+
- log4j 1.2.x
- Java 8+

## License

This project is licensed under the MIT License - see the LICENSE file for details.