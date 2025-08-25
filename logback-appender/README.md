# Logback Appender

On top of the `Core` component that sends logs to Loki this project
gives you Logback appender.

## Quick start guide

1. Grab the no-dependency version of the appender:

   ```xml
   <dependency>
     <groupId>pl.tkowalcz.tjahzi</groupId>
     <artifactId>logback-appender-nodep</artifactId>
     <version>0.9.39</version>
   </dependency>
   ```

   If you already use a compatible version of Netty in your project then
   to reduce the size of your dependencies include
   regular appender distribution:

   ```xml
   <dependency>
     <groupId>pl.tkowalcz.tjahzi</groupId>
     <artifactId>logback-appender</artifactId>
     <version>0.9.39</version>
   </dependency>
   ```

1. Include minimal appender configuration in your existing logback
   configuration:

```xml

<appender name="Loki" class="pl.tkowalcz.tjahzi.logback.LokiAppender">
    <host>${loki.host}</host>
    <port>${loki.port}</port>

    <efficientLayout>
        <pattern>%-4relative [%thread] %-5level %logger{35} - %msg%n
        </pattern>
    </efficientLayout>

    <label>
        <name>server</name>
        <value>${HOSTNAME}</value>
    </label>
</appender>
```

1. Reference the appender from inside one of your logger definitions:

```xml

<root level="debug">
    <appender-ref ref="Loki"/>
</root>
```

### Note on Loki HTTP endpoint and host/port configuration

Tjahzi sends `POST` requests to `/loki/api/v1/push` HTTP endpoint.
Specifying
e.g. `<host>loki.mydomain.com</host><port>3100</port>`
will configure the appender to call URL:
`http://loki.mydomain.com:3100/loki/api/v1/push`.

## Custom pattern layout and decreasing allocations

Tjahzi provides its own pattern layout class
`ch.qos.logback.core.pattern.EfficientPatternLayout`. It is based on
existing `PatternLayout` from Logback and uses exact same code for
formatting events. The key difference is that it
swaps out some parts of the implementation and introduces reusable
thread local buffers for constructing log line and
allocation free string to bytes encoder.

Tjahzi appender supports all kinds of layouts. To enable
`EfficientPatternLayout` us following configuration:

```xml

<appender name="Loki" class="pl.tkowalcz.tjahzi.logback.LokiAppender">
    ...
    <efficientLayout>
        <pattern>%-4relative [%thread] %-5level %logger{35} - %msg%n
        </pattern>
    </efficientLayout>
</appender>
```

## Advanced configuration

This example sets up a root logger with Loki appender. The appender
definition must point to
class `pl.tkowalcz.tjahzi.logback.LokiAppender`.

```xml

<configuration>
    <appender name="Loki"
              class="pl.tkowalcz.tjahzi.logback.LokiAppender">
        <host>${loki.host}</host>
        <port>${loki.port}</port>

        <efficientLayout>
            <pattern>%-4relative [%thread] %-5level %logger{35} -
                %msg%n
            </pattern>
        </efficientLayout>

        <header>
            <name>X-Org-Id</name>
            <value>Codewise</value>
        </header>

        <label>
            <name>server</name>
            <value>127.0.0.1</value>
        </label>

        <metadata>
            <name>environment</name>
            <value>production</value>
        </metadata>

        <logLevelLabel>
            log_level
        </logLevelLabel>
    </appender>

    <root level="debug">
        <appender-ref ref="Loki"/>
    </root>
</configuration>
``` 

### Configuring connection parameters individually and using URL

Connection is configured by providing parameters like host or port
explicitly in dedicated tags or by using a URL
that has them all "inline". First, we will show how the individual
parameters work. At a minimum, Tjahzi needs host and
port configuration to connect to Loki, e.g.:

```xml

<host>example.com</host>
<port>3100</port>
```

If port is equal to `443` then SSL will be used. You can also configure
SSL manually:

```xml

<host>example.com</host>
<port>3100</port>

<useSSL>true</useSSL>
```

You can also override the default endpoint to which Tjahzi sends data.
This can be useful if Loki is behind the reverse proxy and
additional path mapping is used:

```xml

<host>example.com</host>
<port>3100</port>

<logEndpoint>/monitoring/loki/api/v1/push</logEndpoint>
```

All these parameters can be configured in one place using a URL:

```xml

<url>https://example.com:56654/monitoring/loki/api/v1/push</url>
```

Note that all previously mentioned tags (host, port, useSSL,
logEndpoint) cannot be used when using URL.

URL consists of four parts: protocol, host, port, path. Some of them may
be omitted, and there are defaults that depend
on the contents of other parts of the URL. This table has a rundown of all
viable configurations:

| Section  | Default                    | Comment                                                                              |
|----------|----------------------------|--------------------------------------------------------------------------------------|
| Protocol | None (must be provided)    | Supported protocols are `http` and `https`. Https is equivalent to setting `useUSSL` |
| Host     | None (must be provided)    |                                                                                      |
| Port     | 80 for http, 443 for https | You can use any port and SSL will still be used if protocol is set to https          |
| Path     | '/loki/api/v1/push'        |                                                                                      |

Some examples of correct URLs:

```xml

<url>http://example.com</url>
<url>https://example.com:56654</url>
<url>http://example.com/monitoring/loki/api/v1/push</url>
<url>https://example.com:3100/monitoring/foo/bar</url>
```

### Lookups / variable substitution

Contents of the properties are automatically interpolated by Logback (see [here](http://logback.qos.ch/manual/configuration.html#variableSubstitution)).
All environment, system etc. variable references will be replaced by their 
values during initialization of the appender.

### MDC support

MDC is supported via `mdcLogLabel` tag. It will dynamically extract MDC
value associated with its content and will turn
it into a label.

<details>
    <summary>Click to expand example</summary>

```xml

<configuration debug="true">
    <appender name="Loki"
              class="pl.tkowalcz.tjahzi.logback.LokiAppender">
        <host>${loki.host}</host>
        <port>${loki.port}</port>

        <efficientLayout>
            <pattern>%-4relative [%thread] %-5level %logger{35} -
                %msg%n
            </pattern>
        </efficientLayout>

        <label>
            <name>server</name>
            <value>127.0.0.1</value>
        </label>

        <!-- MDC -->
        <mdcLogLabel>
            trace_id
        </mdcLogLabel>

        <!-- MDC -->
        <mdcLogLabel>
            span_id
        </mdcLogLabel>
    </appender>

    <root level="debug">
        <appender-ref ref="Loki"/>
    </root>
</configuration>
```

</details>

### Structured metadata support

Specify structured metadata attached to each log line sent via this appender instance. Unlike labels, structured metadata 
does not affect log stream grouping and is stored alongside the log entry. See the [official documentation](https://grafana.com/docs/loki/latest/get-started/labels/structured-metadata/) of Loki.

> **Note:** Structured metadata was added to chunk format V4 and will not work prior to version [2.9](https://grafana.com/docs/loki/latest/release-notes/v2-9/) of Loki.

```xml
<appender name="Loki" class="pl.tkowalcz.tjahzi.logback.LokiAppender">

    ...

    <metadata>
        <name>environment</name>
        <value>production</value>
    </metadata>

    <metadata>
        <name>service_version</name>
        <value>${SERVICE_VERSION}</value>
    </metadata>
</appender>
```

Structured metadata supports variable substitution just like labels. 
All environment, system etc. variable references will be replaced by 
their values during initialization of the appender.

### Logger name and thread name in labels

You can include the logger name and logging thread name as labels by using
dedicated configuration tags. It will dynamically extract these values
and will turn
them into a label.

```xml

<appender name="Loki" class="pl.tkowalcz.tjahzi.logback.LokiAppender">

    ...

    <loggerNameLabel>
        logger_name
    </loggerNameLabel>

    <threadNameLabel>
        thread_name
    </threadNameLabel>
</appender>
```

## Details

Let's go through the example the config above and analyze configuration
options (**Note: Tags are case-insensitive**).

#### Mandatory configuration parameters

| Tag     | Description                                                                                                                                                                  |
|---------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Host    | Network host address of Loki instance. Either IP address or host name. It will be passed to Netty and end up being resolved by call to `InetSocketAddress.createUnresolved`. |
| Port    | Self-explanatory :)                                                                                                                                                          |
| Encoder | You need to specify pattern for the layout. See section below                                                                                                                |

Note: Instead of 'host' and 'port' you can specify URL.
See [this section](#configuring-connection-parameters-individually-and-using-URL).

#### Pattern definition

You can define layout and pattern in a traditional Logback way:

```xml

<encoder>
    <pattern>%-4relative [%thread] %-5level %logger{35} - %msg%n
    </pattern>
</encoder>
```

or use Tjahzi optimized efficient, low-allocation encoder:

```xml

<efficientLayout>
    <pattern>%-4relative [%thread] %-5level %logger{35} - %msg%n
    </pattern>
</efficientLayout>
```

#### Optional configuration parameters

| Tag                            | Default value         | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
|--------------------------------|-----------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Url                            | -                     | Configure connection in one place instead of using host, port etc. See [this section](#configuring-connection-parameters-individually-and-using-URL).                                                                                                                                                                                                                                                                                                                                                                        |
| UseSSL                         | true if port == '443' | Enable secure (HTTPS) communication regardless of configured port number.                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| LogEndpoint                    | `/loki/api/v1/push`   | Overrides the default endpoint to which Tjahzi sends data. This can be useful if Loki is behind reverse proxy and additional path mapping is used.                                                                                                                                                                                                                                                                                                                                                                           |
| Header                         | -                     | This tag can be used multiple times to specify additional headers that are passed to Loki instance. One example is to pass a `X-Scope-OrgID` header when [running Loki in multi-tenant mode](https://grafana.com/docs/loki/latest/operations/authentication/).                                                                                                                                                                                                                                                               |
| Label                          | -                     | Specify additional labels attached to each log line sent via this appender instance. See also note about [label naming](https://github.com/tkowalcz/tjahzi/wiki/Label-naming).                                                                                                                                                                                                                                                                                                                                               |
| LogLevelLabel                  | -                     | If defined then log level label of configured name will be added to each line sent to Loki. It will contain Logback log level e.g. `INFO`, `WARN` etc. See also note about [label naming](https://github.com/tkowalcz/tjahzi/wiki/Label-naming).                                                                                                                                                                                                                                                                             |
| Metadata                       | -                     | Specify structured metadata attached to each log line sent via this appender instance. Unlike labels, structured metadata does not affect log stream grouping and is stored separately alongside the log entry. This feature is useful for adding contextual information that doesn't need to be indexed for stream identification. Supports both static values and variable substitution.                                                                                                                                   |
| BufferSizeMegabytes            | 32 MB                 | Size of the `log buffer`. Must be power of two between 1MB and 1GB. See [log buffer sizing](https://github.com/tkowalcz/tjahzi/wiki/Log-buffer-sizing) for more explanations.                                                                                                                                                                                                                                                                                                                                                |
| MaxRetries                     | 3                     | Maximum number of retries to perform when delivering log message to Loki. Log buffer data is delivered in order, one batch after the other, so too much retries will block delivery of subsequent log batches (on the other hand if we need to retry many times then next batches will probably fail too).                                                                                                                                                                                                                   |
| ConnectTimeoutMillis           | 5s                    | This configures socket connect timeout when connecting to Loki. After unsuccessful connection attempt it will continue to retry indefinitely employing exponential backoff (initial backoff = 250ms, maximum backoff = 30s, multiplier = 3).                                                                                                                                                                                                                                                                                 |
| ReadTimeoutMillis              | 60s                   | Sets socket read timeout on Loki connection.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| BatchSize                      | 100 KB                | Like in [promtail configuration](https://grafana.com/docs/loki/latest/clients/promtail/configuration/) `maximum batch size (in bytes) of logs to accumulate before sending the batch to Loki`.                                                                                                                                                                                                                                                                                                                               |
| BatchWait                      | 5s                    | Like in [promtail configuration](https://grafana.com/docs/loki/latest/clients/promtail/configuration/) `maximum amount of time to wait before sending a batch, even if that batch isn't full`.                                                                                                                                                                                                                                                                                                                               |
| logShipperWakeupIntervalMillis | 10                    | The agent that reads data from log buffer, compresses it and sends to Loki via http is called `LogShipper`. This property controls how often it wakes up to perform its duties. Other properties control how often the data should be sent to Loki (`batchSize`, `batchWait`) this one just control how often to wake up and check for these conditions. In versions before `0.9.17` it was left at default 1ms which caused high CPU usage on some setups.                                                                  |
| shutdownTimeoutSeconds         | 10s                   | On logging system shutdown (or config reload) Tjahzi will flush its internal buffers so that no logs are lost. This property sets limit on how long to wait for this to complete before proceeding with shutdown.                                                                                                                                                                                                                                                                                                            |
| useDaemonThreads               | false                 | If set to true Tjahzi will run all it's threads as daemon threads. Use this option if you do not want to explicitly close the logging system and still want to make sure Tjahzi internal threads will not prevent JVM from closing down. Note that this can result in unflushed logs not being delivered when the JVM is closed.                                                                                                                                                                                             |
| verbose                        | false                 | If set to true, Tjahzi will log internal errors and connection errors to Logback's internal error logging system. This includes agent errors, pipeline errors, dropped log entries, HTTP errors, failed HTTP requests, and connection issues. When enabled, these errors will be logged using Logback's `addError()` method, which typically outputs to the console or configured status destination. See the [logback docs](https://logback.qos.ch/manual/configuration.html#automaticStatusPrinting) for more information. |
