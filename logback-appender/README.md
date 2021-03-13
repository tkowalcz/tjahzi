# Logback Appender

On top of the `Core` component that sends logs to Loki this project gives you Logback appender.

## Example configuration

This example sets up a root logger with Loki appender. The appender definition must point to class `pl.tkowalcz.tjahzi.logback.LokiAppender`.

```xml
<configuration>
    <appender name="Loki" class="pl.tkowalcz.tjahzi.logback.LokiAppender">
        <host>${loki.host}</host>
        <port>${loki.port}</port>

        <encoder>
            <pattern>%-4relative [%thread] %-5level %logger{35} - %msg%n</pattern>
        </encoder>
        
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

Contents of the properties are automatically interpolated by Logback (see [here](http://logback.qos.ch/manual/configuration.html#variableSubstitution)).
All environment, system etc. variable references will be replaced by their values during initialization of the appender.

## Details

Let's go through the example config above and analyze configuration options (**Note: Tags are case-insensitive**).

| Tag | Required | Default value | Description |
|-----|----------|---------------|-------------|
| Host | Y | | Network host address of Loki instance. Either IP address or host name. It will be passed to Netty and end up being resolved by call to `InetSocketAddress.createUnresolved`. |
| Port | Y | | Self-explanatory :) |
| Header | N | (empty list) | This tag can be used multiple times to specify additional headers that are passed to Loki instance. One example is to pass a `X-Scope-OrgID` header when [running Loki in multi-tenant mode](https://grafana.com/docs/loki/latest/operations/authentication/). |
| Label | N | (empty list) | Specify additional labels attached to each log line sent via this appender instance. See also note about [label naming](https://github.com/tkowalcz/tjahzi/wiki/Label-naming). |
| LogLevelLabel | N | null | If defined then log level label of configured name will be added to each line sent to Loki. It will contain Log4j log level e.g. `INFO`, `WARN` etc. See also note about [label naming](https://github.com/tkowalcz/tjahzi/wiki/Label-naming). |
| BufferSizeMegabytes | N | 32 MB | Size of the `log buffer`. Must be power of two between 1MB and 1GB. See [log buffer sizing](https://github.com/tkowalcz/tjahzi/wiki/Log-buffer-sizing) for more explanations. |
| MaxRetries | N | 3 | Maximum number of retries to perform when delivering log message to Loki. Log buffer data is delivered in order, one batch after the other, so too much retries will block delivery of subsequent log batches (on the other hand if we need to retry many times then next batches will probably fail too). |
| ConnectTimeoutMillis | N | 5s | This configures socket connect timeout when connecting to Loki. After unsuccessful connection attempt it will continue to retry indefinitely employing exponential backoff (initial backoff = 250ms, maximum backoff = 30s, multiplier = 3). |
| ReadTimeoutMillis | N | 60s | Sets socket read timeout on Loki connection. |
| UseOffHeapBuffer | N | true | Whether Tjahzi should allocate native buffer for `Log buffer` component. We can go into a rabbit hole of divagations what are the implications of this. Most important in our view is that having 10s or 100s of MB of space taken out of heap is not very friendly to garbage collector which might have to occasionally copy it around. |
| BatchSize | N | 100 KB | Like in [promtail configuration](https://grafana.com/docs/loki/latest/clients/promtail/configuration/) `maximum batch size (in bytes) of logs to accumulate before sending the batch to Loki`.|
| BatchWait | N | 5s | Like in [promtail configuration](https://grafana.com/docs/loki/latest/clients/promtail/configuration/) `maximum amount of time to wait before sending a batch, even if that batch isn't full`.|
