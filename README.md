# About
[Tjahzi](http://www.thorgal.com/personnages/tjahzi/) is a Log4j2 Appender for [Grafana Loki](https://grafana.com/oss/loki/). 

[![License: MIT](https://img.shields.io/github/license/tkowalcz/tjahzi?style=for-the-badge)](https://github.com/tkowalcz/tjahzi/blob/master/LICENSE)
[![Last Commit](https://img.shields.io/github/last-commit/tkowalcz/tjahzi?style=for-the-badge)](https://github.com/tkowalcz/tjahzi/commits/master)
[![CircleCI](https://img.shields.io/circleci/build/github/tkowalcz/tjahzi?style=for-the-badge)](https://app.circleci.com/pipelines/github/tkowalcz/tjahzi?branch=master)

Latest releases:

[![Maven Central](https://img.shields.io/maven-central/v/pl.tkowalcz/core.svg?label=Core&style=for-the-badge)](https://search.maven.org/search?q=g:pl.tkowalcz)
[![Maven Central](https://img.shields.io/maven-central/v/pl.tkowalcz/log4j2-appender.svg?label=Log4j2%20Appender&style=for-the-badge)](https://search.maven.org/search?q=g:pl.tkowalcz)

# About

Tjahzi allows pushing log data to Loki. It uses protobuf format to push log lines with timestamps and labels to Loki. This 
is implemented in the `core` component. On top of that we built the log4j2 appender, others might follow.

## Features

1. Logging does not allocate objects nor take any locks.
1. Sending data to Loki in batches allocates as little as possible.
1. We also provide a no-dependency version of log4j2 appender. 
1. Includes in-house implementation of protobuf wire format for Loki to reduce dependencies and improve performance 

## Getting started

1. Grab the no-dependency version of the appender:
   
   ```xml
   <dependency>
     <groupId>pl.tkowalcz</groupId>
     <artifactId>log4j2-appender-nodep</artifactId>
     <version>0.9.1</version>
   </dependency>
   ```
   
   If you already use a compatible version of Netty in your project then to reduce the size of your dependencies include 
   regular appender distribution:
   
   ```xml
   <dependency>
     <groupId>pl.tkowalcz</groupId>
     <artifactId>log4j2-appender</artifactId>
     <version>0.9.1</version>
   </dependency>
   ```
   
1. Add `packages="pl.tkowalcz.tjahzi.log4j2"` attribute to your log4j2 configuration xml.
1. Include minimal appender configuration

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

Detailed discussion of how to configure the appender with examples and description of all possible tags check 
[README.md](loki-log4j2-appender/README.md).

## Design principles

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

For those interested wiki contains some information on [log buffer sizing](https://github.com/tkowalcz/tjahzi/wiki/Log-buffer-sizing).

# LICENSE

This work is released under MIT license. Feel free to use, copy and modify this work as long as you credit original authors. 
Pull and feature requests are welcome.

This repository contains code copied from Javolution (http://javolution.org/) library. Javolution is BSD licensed and 
copied file(s) contain copyright notice as per the license requirements. 
