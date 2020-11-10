# Tjahzi
[![CircleCI](https://circleci.com/gh/tkowalcz/tjahzi.svg?style=shield)](https://app.circleci.com/pipelines/github/tkowalcz/tjahzi?branch=master)
[![Maven Central](https://img.shields.io/maven-central/v/pl.tkowalcz/core.svg?label=Core)](https://search.maven.org/search?q=g:pl.tkowalcz.core)
[![Maven Central](https://img.shields.io/maven-central/v/pl.tkowalcz/log4j2-appender.svg?label=Log4j2%20Appender)](https://search.maven.org/search?q=g:pl.tkowalcz.log4j2-appender)

[Tjahzi](http://www.thorgal.com/personnages/tjahzi/) is a Java client for [Grafana Loki](https://grafana.com/oss/loki/). 

### Design principles

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
+-------------+    Log                                                    
| Application |----------------+                                          
|  Thread 1   |                |                                          
+-------------+                |                                          
                               |                                          
       .                      \|/                                          
                          +----+---------------+         +---------+      
       .      Log         |                    |         |  I/O    |      
          --------------->+     Log buffer     +-->--->--+ thread  |      
       .                  |                    |         | (Netty) |      
                          +----------+---------+         +---------+      
       .                            /|\                                    
                                     |                                    
+-------------+      Log             |                                    
| Application |----------------------+                                    
|  Thread N   |                                                           
+-------------+                                                           
```                                                                                                                                                    
### LICENSE

This work is released under MIT license. Feel free to use, copy and modify this work as long as you credit original authors. Pull and feature requests as welcome.
