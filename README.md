# About
[Tjahzi](http://www.thorgal.com/personnages/tjahzi/) is a Java client for [Grafana Loki](https://grafana.com/oss/loki/).

[![License: MIT](https://img.shields.io/github/license/tkowalcz/tjahzi?style=for-the-badge)](https://github.com/tkowalcz/tjahzi/blob/master/LICENSE)
[![Last Commit](https://img.shields.io/github/last-commit/tkowalcz/tjahzi?style=for-the-badge)](https://github.com/tkowalcz/tjahzi/commits/master)
[![CircleCI](https://img.shields.io/circleci/build/github/tkowalcz/tjahzi?style=for-the-badge)](https://app.circleci.com/pipelines/github/tkowalcz/tjahzi?branch=master)

Latest releases:

[![Maven Central](https://img.shields.io/maven-central/v/pl.tkowalcz/core.svg?label=Core&style=for-the-badge)](https://search.maven.org/search?q=g:pl.tkowalcz)
[![Maven Central](https://img.shields.io/maven-central/v/pl.tkowalcz/log4j2-appender.svg?label=Log4j2%20Appender&style=for-the-badge)](https://search.maven.org/search?q=g:pl.tkowalcz)

# Design principles

Logging should be lightweight and not interfere with main business tasks of threads that happen to log a message. 
Asking the logging subsystem to log a message should be as CPU efficient as possible. 
That's a truism. Apart from computation itself there are many other causes of jitter (varying speed of code execution). 
Thread can be slowed down by excessive allocations, by initialization code running in constructors of unnecessarily allocated objects, 
by garbage collector activity that is triggered by it. There can by configuration refresh checks on logging path, inter thread signaling etc.

To avoid these effects we strive to adhere to the following principles (and document any violations):

1. Lock free and wait free API
2. Allocation free in steady state

You can compare this with [design principles of Aeron](https://github.com/real-logic/aeron/wiki/Design-Principles) which are close to our hearts.

### Architecture

```
                [via 10kB thread local buffer]
                           │                                          
+-------------+    Log  <──┘                                                
│ Application │----------------+                                          
│  Thread 1   │                │                                          
+-------------+                │                                          
                               │                                          
       .                      \│/                                          
                          +----+---------------+         +---------+         +---------+
       .      Log         │                    │         │ Reading │         │  I/O    │
          --------------->┤     Log buffer     ├-->--->--┤ agent   ├-->--->--┤ thread  │      
       .                  │                    │         │ thread  │         │ (Netty) │    
                          +----------+---------+         +---------+         +---------+    
       .                            /│\                                    
                                     │                                    
+-------------+      Log             │                                    
│ Application │----------------------+                                    
│  Thread N   │                                                           
+-------------+                                                           
```

# Log4j2 Appender

On top of a `Core` component  with rather simplistic API we intend to build several layers that make it truly useful. Log4j2 
appender seemed like a good first.

## Example Log4j2 configuration

This example sets up a root logger with a Loki appender.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration status="OFF" shutdownHook="disable" packages="pl.tkowalcz.tjahzi.log4j2">
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

Contents of the properties are automatically interpolated by Log4j2 (see [here](https://logging.apache.org/log4j/log4j-2.2/manual/lookups.html)).
All environment, system etc. variable references will be replaced by their values during initialization of the appender. 
Alternatively this process could have been executed for every log message. The latter approach was deemed too expensive. If you need a mechanism
to replace a variable after logging system initialization I would lvie to hear your use case - please file an issue. 
      
## Details

Let's go through the example config above and analyze configuration options (**Note: Tags are case-insensitive**).

#### Host (required)

Network host address of Loki instance. Either IP address or host name. It will be passed to Netty and end up being resolved
 by call to `InetSocketAddress.createUnresolved`. 

#### Port (required)

Self-explanatory.

#### Header (optional)

This tag can be used multiple times to specify additional headers that are passed to Loki instance. One example is to pass
a `X-Scope-OrgID` header when [running Loki in multi-tenant mode](https://grafana.com/docs/loki/latest/operations/authentication/).

#### Label (optional)

Specify additional labels attached to each log line sent via this appender instance. See also note about [label naming](https://github.com/tkowalcz/tjahzi/wiki/Label-naming).

#### LogLevelLabel (optional)

If defined then log level label of configured name will be added to each line sent to Loki. It will contain Log4j log level e.g. `INFO`, `WARN` etc. See also note about [label naming](https://github.com/tkowalcz/tjahzi/wiki/Label-naming).

#### bufferSizeMegabytes (optional, default = 1MB)

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

# LICENSE

This work is released under MIT license. Feel free to use, copy and modify this work as long as you credit original authors. 
Pull and feature requests are welcome.

This repository contains code copied from Javolution (http://javolution.org/) library. Javolution is BSD licensed and 
copied file(s) contain copyright notice as per the license requirements. 
