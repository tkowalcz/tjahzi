# Reload4j Appender

The `reload4j` appender allows you to send log messages directly to
Grafana Loki from applications using `reload4j` or `log4j1.x.` Since
`reload4j` is binary compatible with `log4j1.x`, this appender works
with
both frameworks.

> NOTE: Due to nested configuration for labels, headers, and MDC, the
> appender requires an XML configuration format.

## Quick start guide

1. Grab the no-dependency version of the appender:

   ```xml
   <dependency>
     <groupId>pl.tkowalcz.tjahzi</groupId>
     <artifactId>reload4j-appender-nodep</artifactId>
     <version>0.9.38</version>
   </dependency>
   ```

   If you already use a compatible version of Netty in your project,
   then to reduce the size of your dependencies, include
   regular appender distribution:

   ```xml
   <dependency>
     <groupId>pl.tkowalcz.tjahzi</groupId>
     <artifactId>reload4j-appender</artifactId>
     <version>0.9.38</version>
   </dependency>
   ```

1. Include minimal appender configuration:

```xml

<appender name="loki" class="pl.tkowalcz.tjahzi.reload4j.LokiAppender">
    <param name="host" value="${loki.host}"/>
    <param name="port" value="${loki.port}"/>

    <layout class="org.apache.log4j.PatternLayout">
        <param name="ConversionPattern" value="%-5p %c - %m%n"/>
    </layout>

    <label>
        <name>server</name>
        <value>${hostName}</value>
    </label>
</appender>
```

1. Reference the appender from inside one of your logger definitions:

```xml

<root>
    <priority value="INFO"/>
    <appender-ref ref="loki"/>
</root>
```

### Note on Loki HTTP endpoint and host/port configuration

Tjahzi by default sends `POST` requests to `/loki/api/v1/push` HTTP
endpoint. Specifying
e.g. `<host>loki.mydomain.com</host><port>3100</port>`
will configure the appender to call to URL:
`http://loki.mydomain.com:3100/loki/api/v1/push`.

### Grafana Cloud configuration

Tjahzi can send logs to Grafana Cloud. It needs two things to be
configured:

- Set port number to `443` which switches an HTTP client into HTTPS
  mode.
- Specify `username` and `password` for HTTP basic authentication that
  Grafana uses.

Password is your "Grafana.com API Key" and can be generated in "Grafana
datasource settings". The host in below example
is just for illustrative purposes.

```xml

<appender name="loki" class="pl.tkowalcz.tjahzi.reload4j.LokiAppender">
    <!-- example host -->
    <param name="host" value="logs-prod-us-central1.grafana.net"/>
    <param name="port" value="443"/>

    <param name="username" value="..."/>
    <param name="password" value="..."/>
    ...
</appender>
```

## Advanced configuration

As per the `log4j1.x` convention configuration parameters are set using
`param` tag, e.g.:

```xml

<param name="host" value="localhost"/>
```

The appender implements `UnrecognizedElementHandler` interface and
handles additional nested tags: `label`, `header`, and `mdcLabel`:

```xml

<appender name="loki" class="pl.tkowalcz.tjahzi.reload4j.LokiAppender">
    ...
    <header>
        <name>X-Org-Id</name>
        <value>Circus</value>
    </header>

    <label>
        <name>test</name>
        <value>...</value>
    </label>

    <mdcLogLabel>
        trace_id
    </mdcLogLabel>
</appender>
```

The `label` and `header` contents can be specified using `param` tags:
```xml

<label>
    <param name="name" value="test"/>
    <param name="value" value="..."/>
</label>
```

### Configuring connection parameters individually and using URL

Connection is configured by providing parameters like host or port
explicitly in dedicated tags or by using a URL
that has them all embedded. First, we will show how the individual
parameters work. At a minimum, Tjahzi needs host and
port configuration to connect to Loki, e.g.:

```xml

<param name="host" value="example.com"/>
<param name="port" value="3100"/>
```

If port is equal to `443` then SSL will be used. You can also configure
SSL manually:

```xml

<param name="host" value="example.com"/>
<param name="port" value="3100"/>

<param name="useSSL" value="true"/>
```

You can also override the default endpoint to which Tjahzi sends data.
This can be useful if Loki is behind the reverse proxy and
additional path mapping is used:

```xml

<param name="host" value="example.com"/>
<param name="port" value="3100"/>

<param name="logEndpoint" value="/monitoring/loki/api/v1/push"/>
```

All these parameters can be configured in one place using a URL:

```xml

<param name="url"
       value="https://example.com:56654/monitoring/loki/api/v1/push"/>
```

Note that all previously mentioned tags (host, port, useSSL,
logEndpoint) cannot be used when using URL.

URL consists of four parts: protocol, host, port, and path. Some of them
may be omitted, and there are defaults that depend
on the contents of other parts of the URL. This table has a rundown of
all viable configurations:

| Section  | Default                    | Comment                                                                              |
|----------|----------------------------|--------------------------------------------------------------------------------------|
| Protocol | None (must be provided)    | Supported protocols are `http` and `https`. Https is equivalent to setting `useUSSL` |
| Host     | None (must be provided)    |                                                                                      |
| Port     | 80 for http, 443 for https | You can use any port and SSL will still be used if protocol is set to https          |
| Path     | '/loki/api/v1/push'        |                                                                                      |

Some examples of correct URLs:

```xml

<param name="url" value="http://example.com"/>
<param name="url" value="https://example.com:56654"/>
<param name="url"
       value="http://example.com/monitoring/loki/api/v1/push"/>
<param name="url" value="https://example.com:3100/monitoring/foo/bar"/>
```

#### Optional configuration parameters

| Tag                            | Default value         | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
|--------------------------------|-----------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| url                            | -                     | Configure connection in one place instead of using host, port etc. See [this section](#configuring-connection-parameters-individually-and-using-URL).                                                                                                                                                                                                                                                                                                       |
| useSSL                         | true if port == '443' | Enable secure (HTTPS) communication regardless of configured port number.                                                                                                                                                                                                                                                                                                                                                                                   |
| logEndpoint                    | `/loki/api/v1/push`   | Overrides the default endpoint to which Tjahzi sends data. This can be useful if Loki is behind reverse proxy and additional path mapping is used.                                                                                                                                                                                                                                                                                                          |
| header                         | -                     | This tag can be used multiple times to specify additional headers that are passed to Loki instance. One example is to pass a `X-Scope-OrgID` header when [running Loki in multi-tenant mode](https://grafana.com/docs/loki/latest/operations/authentication/).                                                                                                                                                                                              |
| label                          | -                     | Specify additional labels attached to each log line sent via this appender instance. See also note about [label naming](https://github.com/tkowalcz/tjahzi/wiki/Label-naming).                                                                                                                                                                                                                                                                              |
| logLevelLabel                  | -                     | If defined then log level label of configured name will be added to each line sent to Loki. It will contain log level e.g. `INFO`, `WARN` etc. See also note about [label naming](https://github.com/tkowalcz/tjahzi/wiki/Label-naming).                                                                                                                                                                                                                    |
| bufferSizeMegabytes            | 32 MB                 | Size of the `log buffer`. Must be power of two between 1MB and 1GB. See [log buffer sizing](https://github.com/tkowalcz/tjahzi/wiki/Log-buffer-sizing) for more explanations.                                                                                                                                                                                                                                                                               |
| maxRetries                     | 3                     | Maximum number of retries to perform when delivering log message to Loki. Log buffer data is delivered in order, one batch after the other, so too much retries will block delivery of subsequent log batches (on the other hand if we need to retry many times then next batches will probably fail too).                                                                                                                                                  |
| connectTimeoutMillis           | 5s                    | This configures socket connect timeout when connecting to Loki. After unsuccessful connection attempt it will continue to retry indefinitely employing exponential backoff (initial backoff = 250ms, maximum backoff = 30s, multiplier = 3).                                                                                                                                                                                                                |
| readTimeoutMillis              | 60s                   | Sets socket read timeout on Loki connection.                                                                                                                                                                                                                                                                                                                                                                                                                |
| batchSize                      | 100 KB                | Like in [promtail configuration](https://grafana.com/docs/loki/latest/clients/promtail/configuration/) `maximum batch size (in bytes) of logs to accumulate before sending the batch to Loki`.                                                                                                                                                                                                                                                              |
| batchWait                      | 5s                    | Like in [promtail configuration](https://grafana.com/docs/loki/latest/clients/promtail/configuration/) `maximum amount of time to wait before sending a batch, even if that batch isn't full`.                                                                                                                                                                                                                                                              |
| logShipperWakeupIntervalMillis | 10                    | The agent that reads data from log buffer, compresses it and sends to Loki via http is called `LogShipper`. This property controls how often it wakes up to perform its duties. Other properties control how often the data should be sent to Loki (`batchSize`, `batchWait`) this one just control how often to wake up and check for these conditions. In versions before `0.9.17` it was left at default 1ms which caused high CPU usage on some setups. |
| shutdownTimeoutSeconds         | 10s                   | On logging system shutdown (or config reload) Tjahzi will flush its internal buffers so that no logs are lost. This property sets limit on how long to wait for this to complete before proceeding with shutdown.                                                                                                                                                                                                                                           |
| useDaemonThreads               | false                 | If set to true Tjahzi will run all it's threads as daemon threads. Use this option if you do not want to explicitly close the logging system and still want to make sure Tjahzi internal threads will not prevent JVM from closing down. Note that this can result in unflushed logs not being delivered when the JVM is closed.                                                                                                                            |
| verbose                        | false                 | If set to true, Tjahzi will log internal errors and connection errors to Log4j internal error logging system. This includes agent errors, pipeline errors, dropped log entries, HTTP errors, failed HTTP requests, and connection issues. When enabled, these errors will be logged using LogLog internal logging system. To output them to the console add `debug="true"` to the `log4j:configuration` tag.                                                |

## Log4j1.x compatibility

If you are still using `log4j1.x` you can use this appender while
excluding the `reload4j` dependency manually. In maven:

```xml

<dependency>
    <groupId>pl.tkowalcz.tjahzi</groupId>
    <artifactId>reload4j-appender</artifactId>
    <version>0.9.38</version>
    <exclusions>
        <exclusion>
            <groupId>ch.qos.reload4j</groupId>
            <artifactId>reload4j</artifactId>
        </exclusion>
    </exclusions>
</dependency>

```
