# Thjazi

Thiazi is a Java client for Grafana Loki. 

Logging should be lightweight and not interfere with main business tasks of threads that happen to log a message. Asking the logging subsysytem to log a message should be as CPU efficient as possible. But that is a truism. Apart from computaiton itself there are many other causes of jitter (varying speed of code execution). Thread can be slowed down by excessive allocations, by initialization code running in constructors of unnecesarily allocated objects, by garbage collector activity that is triggered by it. There can by configuration refresh checks on logging path, inter thread signaling etc.

To avoid these effects we strive to adhere to the following principles (and document any violations):

1. Lock free and wait free API
2. Allocation free in steady state

You can compare this with [design principles of Aeron](https://github.com/real-logic/aeron/wiki/Design-Principles) which are close to our hearts.


LICENSE

This work is released under MIT license. Feel free to use, copy and modify this work as long as you credit original authors. Pull and feature requests as welcome.
