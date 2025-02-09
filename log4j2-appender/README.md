# Log4j2 Appender

On top of a `Core` component with rather simplistic API we intend to build several layers that make it truly useful.
Log4j2 appender seemed like a good first.

## Quick start guide

1. Grab the no-dependency version of the appender:

   ```xml
   <dependency>
     <groupId>pl.tkowalcz.tjahzi</groupId>
     <artifactId>log4j2-appender-nodep</artifactId>
     <version>0.9.17</version>
   </dependency>
   ```

   If you already use a compatible version of Netty in your project then to reduce the size of your dependencies include
   regular appender distribution:

   ```xml
   <dependency>
     <groupId>pl.tkowalcz.tjahzi</groupId>
     <artifactId>log4j2-appender</artifactId>
     <version>0.9.17</version>
   </dependency>
   ```

1. Add `packages="pl.tkowalcz.tjahzi.log4j2"` attribute to your existing log4j2 configuration file.
1. Include minimal appender configuration:

```xml

<Loki name="loki-appender">
    <host>${sys:loki.host}</host>
    <port>${sys:loki.port}</port>

    <PatternLayout>
        <Pattern>%X{tid} [%t] %d{MM-dd HH:mm:ss.SSS} %5p %c{1} - %m%n%exception{full}</Pattern>
    </PatternLayout>

    <Label name="server" value="${hostName}"/>
</Loki>
```

1. Reference the appender from inside one of your logger definitions:

```xml

<Root level="INFO">
    <AppenderRef ref="Loki"/>
</Root>
```

### Note on Loki HTTP endpoint and host/port configuration

Tjahzi by default sends `POST` requests to `/loki/api/v1/push` HTTP endpoint. Specifying
e.g. `<host>loki.mydomain.com</host><port>3100</port>`
will configure the appender to call to URL: `http://loki.mydomain.com:3100/loki/api/v1/push`.

### Grafana Cloud configuration

Tjahzi can send logs to Grafana Cloud. It needs two things to be configured:

- Set port number to `443` which switches HTTP client into HTTPS mode.
- Specify `username` and `password` for HTTP basic authentication that Grafana uses.

Password is your "Grafana.com API Key" and can be generated in "Grafana datasource settings". The host in below example
is just for illustrative purposes.

```xml

<Loki name="loki-appender">
    <!-- example host -->
    <host>logs-prod-us-central1.grafana.net</host>
    <port>443</port>

    <username>...</username>
    <password>...</password>
    ...
</Loki>
```

## Advanced configuration

This example sets up a root logger with a Loki appender. Note that `pl.tkowalcz.tjahzi.log4j2` is added to `packages`
attribute of configuration so that the appender can be found.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration packages="pl.tkowalcz.tjahzi.log4j2">
    <Loggers>
        <Root level="INFO">
            <AppenderRef ref="Loki"/>
        </Root>
    </Loggers>

    <appenders>
        <Loki name="Loki" bufferSizeMegabytes="64">
            <host>${sys:loki.host}</host>
            <port>${sys:loki.port}</port>

            <ThresholdFilter level="ALL"/>
            <PatternLayout>
                <Pattern>%X{tid} [%t] %d{MM-dd HH:mm:ss.SSS} %5p %c{1} - %m%n%exception{full}</Pattern>
            </PatternLayout>

            <Header name="X-Scope-OrgID" value="Circus"/>
            <Label name="server" value="127.0.0.1"/>

            <LogLevelLabel>log_level</LogLevelLabel>
        </Loki>
    </appenders>
</configuration>
``` 

### Configuring connection parameters individually and using URL

Connection is configured by providing parameters like host or port explicitly in dedicated tags or by using a URL 
that has them all "inline". First we will show how the individual parameters work. At a minimum Tjahzi needs host and 
port configuration to connect to Loki, e.g.:

```xml
<host>example.com</host>
<port>3100</port>
```

If port is equal to `443` then SSL will be used. You can also configure SSL manually:

```xml
<host>example.com</host>
<port>3100</port>

<useSSL>true</useSSL>
```

You can also override the default endpoint to which Tjahzi sends data. This can be useful if Loki is behind reverse proxy and
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

Note that all previously mentioned tags (host, port, useSSL, logEndpoint) cannot be used when using URL.

URL consists of four parts: protocol, host, port, path. Some of them may be omitted and there are defaults that depend 
on contents of other parts of the URL. This table has a rundown of all viable configurations:

| Section  | Default                    | Comment                                                                             |
|----------|----------------------------|-------------------------------------------------------------------------------------|
| Protocol | None (must be provided)    | Supported protocols are `http` and `https`. Https is equivlent to setting `useUSSL` |
| Host     | None (must be provided)    |                                                                                     |
| Port     | 80 for http, 443 for https | You can use any port and SSL will still be used if protocol is set to https         |
| Path     | '/loki/api/v1/push'        |                                                                                     |

Some examples of correct URLs:

```xml
<url>http://example.com</url>
<url>https://example.com:56654</url>
<url>http://example.com/monitoring/loki/api/v1/push</url>
<url>https://example.com:3100/monitoring/foo/bar</url>
```

### Lookups / variable substitution

Contents of the properties
are [automatically interpolated by Log4j2](https://logging.apache.org/log4j/log4j-2.2/manual/configuration.html#PropertySubstitution)
. All environment, system etc. variable references will be replaced by their values during initialization of the
appender. The exception to this rule is context/MDC (`${ctx:foo}`) value lookup - it is performed for each message at
runtime (allocation free).

*NOTE: This process could have been executed for every lookup type at runtime (for each log message). This approach was
deemed too expensive. If you need a mechanism to replace a variable (other than context/MDC) after logging system
initialization I would love to hear your use case - please file an issue.*

## Patterns in Labels

Alternative way of specifying label contents is via pattern attribute:

```xml

<Label name="server" pattern="%C{1.}"/>
```

This pattern is compatible with
Log4j [pattern layout](https://logging.apache.org/log4j/2.x/manual/layouts.html#PatternLayout). In fact, we reuse log4j
internal classes for this implementation. It is generally efficient and allocation free as
per [documentation](https://logging.apache.org/log4j/log4j-2.12.1/manual/garbagefree.html#PatternLayout).

## Properties file based configuration

Properties file is a simple configuration format, but it is not always clear how to implement more advanced features
such as components instantiated more than once. For basic overview of how to configure log4j using properties file
see [official documentation](https://logging.apache.org/log4j/2.x/manual/configuration.html#Properties).

<details>
    <summary>Click to expand an example that defines multiple labels.</summary>

```properties
#Loads Tjahzi plugin definition
packages="pl.tkowalcz.tjahzi.log4j2"

# Allows this configuration to be modified at runtime. The file will be checked every 30 seconds.
monitorInterval=30

# Standard stuff
rootLogger.level=INFO
rootLogger.appenderRefs=loki
rootLogger.appenderRef.loki.ref=loki-appender

#Loki configuration
appender.loki.name=loki-appender
appender.loki.type=Loki
appender.loki.host=${sys:loki.host}
appender.loki.port=${sys:loki.port}

appender.loki.logLevelLabel=log_level

# Layout
appender.loki.layout.type=PatternLayout
appender.loki.layout.pattern=%X{tid} [%t] %d{MM-dd HH:mm:ss.SSS} %5p %c{1} - %m%n%exception{full}

# Labels
appender.loki.labels[0].type=label
appender.loki.labels[0].name=server
appender.loki.labels[0].value=127.0.0.1

appender.loki.labels[1].type=label
appender.loki.labels[1].name=source
appender.loki.labels[1].value=log4j
```

</details>

## Configuration reference

Let's go through the example config used in previous sections and analyze configuration options (**Note: Tags are
case-insensitive**).

#### Host (required unless URL is specified)

Network host address of Loki instance. Either IP address or host name. It will be passed to Netty and end up being
resolved by call to `InetSocketAddress.createUnresolved`.

#### Port (required unless URL is specified)

Port used for connecting to running Loki. Tjahzi by default uses plain HTTP but if the port is `443` then it will
automatically switch to HTTPS.

#### useSSL (optional)

Enable secure (HTTPS) communication regardless of configured port number.

#### logEndpoint (optional)

Overrides the default endpoint to which Tjahzi sends data. This can be useful if Loki is behind reverse proxy and
additional path mapping is used.

#### URL (optional - replaces usage of  host, port, useSSL, logEndpoint)

Configure connection in one place instead of using host, port etc. See [this section](#configuring-connection-parameters-individually-and-using-URL).

#### Username (optional)

Username for HTTP basic auth.

#### Password (optional)

Password for HTTP basic auth.

#### Header (optional)

This tag can be used multiple times to specify additional headers that are passed to Loki instance. One example is to
pass a `X-Scope-OrgID` header
when [running Loki in multi-tenant mode](https://grafana.com/docs/loki/latest/operations/authentication/).

#### Label (optional)

Specify additional labels attached to each log line sent via this appender instance. See also note
about [label naming](https://github.com/tkowalcz/tjahzi/wiki/Label-naming).

You can use value attribute to specify static text. You can use `${}` variable substitution inside that text and Tjahzi
will resolve variables once at startup. If the varaible is a context/MDC lookup it will be resolved dynamically for each
log line.

This tag also supports `pattern` attribute where you can use pattern layout expressions that will be resolved at
runtime.

#### LogLevelLabel (optional)

If defined then log level label of configured name will be added to each line sent to Loki. It will contain Log4j log
level e.g. `INFO`, `WARN` etc. See also note about [label naming](https://github.com/tkowalcz/tjahzi/wiki/Label-naming).

#### bufferSizeMegabytes (optional, default = 32)

Size of the `log buffer`. Must be power of two between 1MB and 1GB.
See [log buffer sizing](https://github.com/tkowalcz/tjahzi/wiki/Log-buffer-sizing) for more explanations.

#### maxLogLineSizeKilobytes (optional, default = 10)

Size of an intermediate thread local buffer that is used by log4j to serialise single log message into. Log lines larger
than that will be split into multiple log entries (
see [wiki](https://github.com/tkowalcz/tjahzi/wiki/Log-buffer-sizing#note-on-thread-local-buffer) for discussion).

#### maxRetries (optional, default = 3)

Maximum number of retries to perform when delivering log message to Loki. Log buffer data is delivered in order, one
batch after the other, so too much retries will block delivery of subsequent log batches (on the other hand if we need
to retry many times then next batches will probably fail too).

#### connectTimeoutMillis (optional, default = 5000)

This configures socket connect timeout when connecting to Loki. After unsuccessful connection attempt it will continue
to retry indefinitely employing exponential backoff (initial backoff = 250ms, maximum backoff = 30s, multiplier = 3).

#### readTimeoutMillis (optional, default = 60 000)

Sets socket read timeout on Loki connection.

#### batchSize (optional, default = 10_2400)

Like
in [promtail configuration](https://grafana.com/docs/loki/latest/clients/promtail/configuration/) `maximum batch size (in bytes) of logs to accumulate before sending the batch to Loki`
.

#### batchWait (optional, default = 5s)

Like
in [promtail configuration](https://grafana.com/docs/loki/latest/clients/promtail/configuration/) `maximum amount of time to wait before sending a batch, even if that batch isn't full`
.

#### logShipperWakeupIntervalMillis (optional, default = 10)

The agent that reads data from log buffer, compresses it and sends to Loki via http is called `LogShipper`. This property
controls how often it wakes up to perform its duties. Other properties control how often the data should be sent to Loki
(`batchSize`, `batchWait`) this one just control how often to wake up and check for these conditions. In versions before
`0.9.17` it was left at default 1ms which caused high CPU usage on some setups.

#### shutdownTimeoutSeconds (optional, default = 10s)

On logging system shutdown (or config reload) Tjahzi will flush its internal buffers so that no logs are lost. This
property sets limit on how long to wait for this to complete before proceeding with shutdown.

#### useDaemonThreads (optional, default = false)

If set to true Tjahzi will run all it's threads as daemon threads. 

Use this option if you do not want to explicitly close the logging system and still want to make sure Tjahzi internal 
threads will not prevent JVM from closing down. Note that this can result in unflushed logs not being delivered when the
JVM is closed.
