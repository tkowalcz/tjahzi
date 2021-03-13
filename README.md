# About
[Tjahzi](http://www.thorgal.com/personnages/tjahzi/) is a set of Java tools and appenders for logging to [Grafana Loki](https://grafana.com/oss/loki/). 

[![License: MIT](https://img.shields.io/github/license/tkowalcz/tjahzi?style=for-the-badge)](https://github.com/tkowalcz/tjahzi/blob/master/LICENSE)
[![Last Commit](https://img.shields.io/github/last-commit/tkowalcz/tjahzi?style=for-the-badge)](https://github.com/tkowalcz/tjahzi/commits/master)
[![CircleCI](https://img.shields.io/circleci/build/github/tkowalcz/tjahzi?style=for-the-badge)](https://app.circleci.com/pipelines/github/tkowalcz/tjahzi?branch=master)

Latest releases:

[![Maven Central](https://img.shields.io/maven-central/v/pl.tkowalcz.tjahzi/core.svg?label=Core&style=for-the-badge)](https://search.maven.org/search?q=g:pl.tkowalcz.tjahzi)
[![Maven Central](https://img.shields.io/maven-central/v/pl.tkowalcz.tjahzi/log4j2-appender.svg?label=Log4j2%20Appender&style=for-the-badge)](https://search.maven.org/search?q=g:pl.tkowalcz.tjahzi)
[![Maven Central](https://img.shields.io/maven-central/v/pl.tkowalcz.tjahzi/logback-appender.svg?label=Logback%20Appender&style=for-the-badge)](https://search.maven.org/search?q=g:pl.tkowalcz.tjahzi)

If you find the project useful (or not useful for some reason) please let me know to encourage further development. ⭐ Stars are also welcome ⭐ ;).

## Features

Tjahzi allows pushing log data to Loki. It uses protobuf format to push log lines with timestamps and labels to Loki. This
is implemented in the `core` component. On top of that we built appenders for `log4j2` and `Logback`. These feature:

1. Logging does not allocate objects nor take any locks.
1. Sending data to Loki in batches allocates as little as possible.
1. We also provide a no-dependency versions of these appenders. 
1. Includes in-house implementation of protobuf wire format for Loki to reduce dependencies and improve performance. 

Log4j2 appender is currently used to ship logs from tens of servers at about 10GB of logs per day.

## Getting started

For `log4j2` appender quick start guide and detailed discussions see this [README.md](log4j2-appender/README.md).
For `Logback` appender quick start guide and detailed discussions see this [README.md](logback-appender/README.md).

## Core design principles

Logging should be lightweight and not interfere with main business tasks of threads that happen to log a message. 
Asking the logging subsystem to log a message should be as CPU efficient as possible. 
That's a truism. Apart from computation itself there are many other causes of jitter (varying speed of code execution). 
Thread can be slowed down by excessive allocations, by initialization code running in constructors of unnecessarily allocated objects, 
by garbage collector activity that is triggered by it. There can by configuration refresh checks on logging path, inter thread signaling etc.

To avoid these effects we strive to adhere to the following principles (and document any violations):

1. Lock free and wait free API
2. Allocation free in steady state

You can compare this with [design principles of Aeron](https://github.com/real-logic/aeron/wiki/Design-Principles) which are close to our hearts.

## Architecture

```
                [via 10kB thread local buffer]
                           │                                          
┌─────────────┐    Log  ←──┘                                                
│ Application │----------------┐                                          
│  Thread 1   │                │                                          
└─────────────┘                │                                          
                               │                                          
       .                       ▼                                          
                          ┌────────────────────┐         ┌─────────┐         ┌─────────┐
       .      Log         │                    │         │ Reading │         │  I/O    │
          ---------------▶│     Log buffer     ├--→---→--┤ agent   ├--→---→--┤ thread  │      
       .                  │                    │         │ thread  │         │ (Netty) │    
                          └────────────────────┘         └─────────┘         └─────────┘    
       .                             ▲                                    
                                     │                                    
┌─────────────┐      Log             │                                    
│ Application │----------------------┘                                    
│  Thread N   │                                                           
└─────────────┘                                                           
```

For those interested wiki contains some information on [log buffer sizing](https://github.com/tkowalcz/tjahzi/wiki/Log-buffer-sizing).

# LICENSE

This work is released under MIT license. Feel free to use, copy and modify this work as long as you credit original authors. 
Pull and feature requests are welcome.
