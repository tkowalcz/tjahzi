# Log4j2 Appender

On top of a `Core` component  with rather simplistic API we intend to build several layers that make it truly useful. Log4j2
appender seemed like a good first.

## Quick start guide

1. Grab the no-dependency version of the appender:

   ```xml
   <dependency>
     <groupId>pl.tkowalcz.tjahzi</groupId>
     <artifactId>log4j2-appender-nodep</artifactId>
     <version>0.9.5</version>
   </dependency>
   ```

   If you already use a compatible version of Netty in your project then to reduce the size of your dependencies include
   regular appender distribution:

   ```xml
   <dependency>
     <groupId>pl.tkowalcz.tjahzi</groupId>
     <artifactId>log4j2-appender</artifactId>
     <version>0.9.5</version>
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

        <Label name="server" value="${sys:hostname}"/>
    </Loki>
```

1. Reference the appender from inside one of your logger definitions:

```xml
    <Root level="INFO">
        <AppenderRef ref="Loki"/>
    </Root>
```

### Note on Loki HTTP endpoint and host/port configuration

Tjahzi sends `POST` requests to `/loki/api/v1/push` HTTP endpoint. Specifying e.g. `<host>loki.mydomain.com</host><port>3100</port>` 
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

This example sets up a root logger with a Loki appender. Note that `pl.tkowalcz.tjahzi.log4j2` is added to `packages` attribute
of configuration so that the appender can be found.

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

### Lookups / variable substitution

Contents of the properties are [automatically interpolated by Log4j2](https://logging.apache.org/log4j/log4j-2.2/manual/configuration.html#PropertySubstitution).
All environment, system etc. variable references will be replaced by their values during initialization of the appender.
The exception is context/MDC (`${ctx:foo}`) value lookup as it make sense only during runtime.

NOTE: This process could have been executed for every lookup type at runtime (for each log message). This approach was 
deemed too expensive. If you need a mechanism to replace a variable (other than context/MDC) after logging system 
initialization I would love to hear your use case - please file an issue.

## Details

Let's go through the example config above and analyze configuration options (**Note: Tags are case-insensitive**).

#### Host (required)

Network host address of Loki instance. Either IP address or host name. It will be passed to Netty and end up being resolved
by call to `InetSocketAddress.createUnresolved`.

#### Port (required)

Port used for connecting to running Loki. Tjahzi by default uses plain HTTP but if the port is `443` then it will
automatically switch to HTTPS.

#### Username

Username for HTTP basic auth.

#### Password

Password for HTTP basic auth.

#### Header (optional)

This tag can be used multiple times to specify additional headers that are passed to Loki instance. One example is to pass
a `X-Scope-OrgID` header when [running Loki in multi-tenant mode](https://grafana.com/docs/loki/latest/operations/authentication/).

#### Label (optional)

Specify additional labels attached to each log line sent via this appender instance. See also note about [label naming](https://github.com/tkowalcz/tjahzi/wiki/Label-naming).

#### LogLevelLabel (optional)

If defined then log level label of configured name will be added to each line sent to Loki. It will contain Log4j log level e.g. `INFO`, `WARN` etc. See also note about [label naming](https://github.com/tkowalcz/tjahzi/wiki/Label-naming).

#### bufferSizeMegabytes (optional, default = 32 MB)

Size of the `log buffer`. Must be power of two between 1MB and 1GB. See [log buffer sizing](https://github.com/tkowalcz/tjahzi/wiki/Log-buffer-sizing) for more explanations.

#### maxRetries (optional, default = 3)

Maximum number of retries to perform when delivering log message to Loki. Log buffer data is delivered in order, one batch after
the other, so too much retries will block delivery of subsequent log batches (on the other hand if we need to retry many times then
next batches will probably fail too).

#### connectTimeoutMillis (optional, default = 5s)

This configures socket connect timeout when connecting to Loki. After unsuccessful connection attempt it will continue to retry indefinitely
employing exponential backoff (initial backoff = 250ms, maximum backoff = 30s, multiplier = 3).

#### readTimeoutMillis (optional, default = 60s)

Sets socket read timeout on Loki connection.

#### useOffHeapBuffer (optional, default = true)

Whether Tjahzi should allocate native buffer for `Log buffer` component. We can go into a rabbit hole of divagations what are the
implications of this. Most important in our view is that having 10s or 100s of MB of space taken out of heap is not very
friendly to garbage collector which might have to occasionally copy it around.

#### batchSize (optional, default = 10_2400)

Like in [promtail configuration](https://grafana.com/docs/loki/latest/clients/promtail/configuration/) `maximum batch
size (in bytes) of logs to accumulate before sending the batch to Loki`.

#### batchWait = (optional, default = 5s)

Like in [promtail configuration](https://grafana.com/docs/loki/latest/clients/promtail/configuration/) `maximum amount
of time to wait before sending a batch, even if that batch isn't full`.
