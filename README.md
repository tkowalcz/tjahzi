# Thjazi

Thiazi is a Java client for Grafana Loki. Development philosophy is that logging should be lightweight and not interfere with main tasks of the business threads that in the process happen to log messages. We strive to adhere to following principles (and document any violations):

1. Lock free and wait free API
2. Allocation free in steady state

You can compare this with [design principles of Aeron](https://github.com/real-logic/aeron/wiki/Design-Principles) which are close to our hearts.
