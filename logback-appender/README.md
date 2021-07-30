# Logback Appender

On top of the `Core` component that sends logs to Loki this project gives you Logback appender.

## Quick start guide

1. Grab the no-dependency version of the appender:

   ```xml
   <dependency>
     <groupId>pl.tkowalcz.tjahzi</groupId>
     <artifactId>logback-appender-nodep</artifactId>
     <version>0.1.0</version>
   </dependency>
   ```

   If you already use a compatible version of Netty in your project then to reduce the size of your dependencies include
   regular appender distribution:

   ```xml
   <dependency>
     <groupId>pl.tkowalcz.tjahzi</groupId>
     <artifactId>logback-appender</artifactId>
     <version>0.1.0</version>
   </dependency>
   ```

1. Include minimal appender configuration in your existing logback configuration:

```xml
<appender name="Loki" class="pl.tkowalcz.tjahzi.logback.LokiAppender">
    <host>${loki.host}</host>
    <port>${loki.port}</port>

    <efficientLayout>
        <pattern>%-4relative [%thread] %-5level %logger{35} - %msg%n</pattern>
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

Tjahzi sends `POST` requests to `/loki/api/v1/push` HTTP endpoint. Specifying
e.g. `<host>loki.mydomain.com</host><port>3100</port>`
will configure the appender to call to URL: `http://loki.mydomain.com:3100/loki/api/v1/push`.

## Custom pattern layout and decreasing allocations

Tjahzi provides its own pattern layout class `ch.qos.logback.core.pattern.EfficientPatternLayout`. It is based on
existing `PatternLayout` from Logback and uses exact same code for formatting events. The key difference is that it
swaps out some parts of the implementation and introduces reusable thread local buffers for constructing log line and
allocation free string to bytes encoder.

Tjahzi appender supports all kinds of layouts. To enable `EfficientPatternLayout` us following configuration:

```xml
<appender name="Loki" class="pl.tkowalcz.tjahzi.logback.LokiAppender">
    ...
    <efficientLayout>
        <pattern>%-4relative [%thread] %-5level %logger{35} - %msg%n</pattern>
    </efficientLayout>
</appender>
```

## Advanced configuration

This example sets up a root logger with Loki appender. The appender definition must point to
class `pl.tkowalcz.tjahzi.logback.LokiAppender`.

```xml
<configuration>
    <appender name="Loki" class="pl.tkowalcz.tjahzi.logback.LokiAppender">
        <host>${loki.host}</host>
        <port>${loki.port}</port>

        <efficientLayout>
            <pattern>%-4relative [%thread] %-5level %logger{35} - %msg%n</pattern>
        </efficientLayout>

        <header>
            <name>X-Org-Id</name>
            <value>Codewise</value>
        </header>

        <label>
            <name>server</name>
            <value>127.0.0.1</value>
        </label>

        <logLevelLabel>
            log_level
        </logLevelLabel>
    </appender>

    <root level="debug">
        <appender-ref ref="Loki"/>
    </root>
</configuration>
``` 

### Lookups / variable substitution

Contents of the properties are automatically interpolated by Logback (
see [here](http://logback.qos.ch/manual/configuration.html#variableSubstitution)). All environment, system etc. variable
references will be replaced by their values during initialization of the appender.

### MDC support

MDC is supported via `mdcLogLabel` tag. It will dynamically extract MDC value associated with its content and will turn
it into a label.

<details>
    <summary>Click to expand example</summary>

```xml
<configuration debug="true">
    <appender name="Loki" class="pl.tkowalcz.tjahzi.logback.LokiAppender">
        <host>${loki.host}</host>
        <port>${loki.port}</port>

        <efficientLayout>
            <pattern>%-4relative [%thread] %-5level %logger{35} - %msg%n</pattern>
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

## Details

Let's go through the example config above and analyze configuration options (**Note: Tags are case-insensitive**).

#### Mandatory configuration parameters

| Tag | Description |
|-----|-------------|
| Host | Network host address of Loki instance. Either IP address or host name. It will be passed to Netty and end up being resolved by call to `InetSocketAddress.createUnresolved`. |
| Port | Self-explanatory :) |
| Encoder | You need to specify pattern for the layout. See section below |

#### Pattern definition

You can define layout and pattern in a traditional Logback way:

```xml
<encoder>
    <pattern>%-4relative [%thread] %-5level %logger{35} - %msg%n</pattern>
</encoder>
```

or use Tjahzi optimized efficient, low allocation encoder:

```xml
<efficientLayout>
    <pattern>%-4relative [%thread] %-5level %logger{35} - %msg%n</pattern>
</efficientLayout>
```

#### Optional configuration parameters

| Tag | Default value | Description |
|-----|---------------|-------------|
| Header | - | This tag can be used multiple times to specify additional headers that are passed to Loki instance. One example is to pass a `X-Scope-OrgID` header when [running Loki in multi-tenant mode](https://grafana.com/docs/loki/latest/operations/authentication/). |
| Label | - | Specify additional labels attached to each log line sent via this appender instance. See also note about [label naming](https://github.com/tkowalcz/tjahzi/wiki/Label-naming). |
| LogLevelLabel | - | If defined then log level label of configured name will be added to each line sent to Loki. It will contain Logback log level e.g. `INFO`, `WARN` etc. See also note about [label naming](https://github.com/tkowalcz/tjahzi/wiki/Label-naming). |
| BufferSizeMegabytes | 32 MB | Size of the `log buffer`. Must be power of two between 1MB and 1GB. See [log buffer sizing](https://github.com/tkowalcz/tjahzi/wiki/Log-buffer-sizing) for more explanations. |
| MaxRetries | 3 | Maximum number of retries to perform when delivering log message to Loki. Log buffer data is delivered in order, one batch after the other, so too much retries will block delivery of subsequent log batches (on the other hand if we need to retry many times then next batches will probably fail too). |
| ConnectTimeoutMillis | 5s | This configures socket connect timeout when connecting to Loki. After unsuccessful connection attempt it will continue to retry indefinitely employing exponential backoff (initial backoff = 250ms, maximum backoff = 30s, multiplier = 3). |
| ReadTimeoutMillis | 60s | Sets socket read timeout on Loki connection. |
| UseOffHeapBuffer | true | Whether Tjahzi should allocate native buffer for `Log buffer` component. We can go into a rabbit hole of divagations what are the implications of this. Most important in our view is that having 10s or 100s of MB of space taken out of heap is not very friendly to garbage collector which might have to occasionally copy it around. |
| BatchSize | 100 KB | Like in [promtail configuration](https://grafana.com/docs/loki/latest/clients/promtail/configuration/) `maximum batch size (in bytes) of logs to accumulate before sending the batch to Loki`.|
| BatchWait | 5s | Like in [promtail configuration](https://grafana.com/docs/loki/latest/clients/promtail/configuration/) `maximum amount of time to wait before sending a batch, even if that batch isn't full`.|
| shutdownTimeoutSeconds | 10s | On logging system shutdown (or config reload) Tjahzi will flush its internal buffers so that no logs are lost. This property sets limit on how long to wait for this to complete before proceeding with shutdown. |
