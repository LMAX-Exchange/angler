# Angler

Angler is a utility for monitoring the performance of inbound UDP message-processing in the Linux kernel.

### Maintainer

[Mark Price](https://github.com/epickrram)


## What can it tell me?

Angler will notify registered listeners of buffer-depths, packet drops and receive errors of individual sockets.

System-wide UDP receive metrics will also be supplied if requested.


Current metric sources are:

### `/proc/net/udp`

1.   socket queue depth
2.   socket drop count

### `/proc/net/snmp`

1.   UDP queue errors
2.   UDP buffer overflows
3.   UDP checksum errors

### `/proc/net/softnet_stat`

1.   network events processed
2.   time squeeze events
3.   drop events


Monitoring these metrics can be useful when trying to track down the source of packet-loss in
high-throughput traffic scenarios.


## How do I use it?

The simplest use-case for Angler is to monitor socket receive-queue depth and drop counts:

```java
// begin monitoring
private final UdpSocketMonitor udpSocketMonitor = new UdpSocketMonitor(monitoringCallback);

udpSocketMonitor.beginMonitoringOf(new InetSocketAddress("127.0.0.1", 19889));

Executors.newSingleThreadScheduledExecutor().
        scheduleAtFixedRate(() -> udpSocketMonitor.poll(loggingStatsHandler),
                0L, 1L, TimeUnit.SECONDS)
```

Implement a handler for the socket statistics:

```java
private static class LoggingUdpSocketStatisticsHandler implements UdpSocketStatisticsHandler
{
    @Override
    public void onStatisticsUpdated(final InetAddress inetAddress, final int port,
                                    final long socketIdentifier, final long inode,
                                    final long receiveQueueDepth, final long drops)
    {
        if(drops != 0)
        {
            log("Socket [%s:%d], queued: %d, drops: %d",
                    inetAddress.toString(), port, receiveQueueDepth, drops);
        }
    }
}
```

The handler will be notified if the socket's queue-depth or drop-count changes between invocations of the `poll` method.


See the
[ExampleApplication](https://github.com/epickrram/angler/blob/master/src/test/java/com/lmax/angler/monitoring/network/monitor/example/ExampleApplication.java)
for further details.


To test functionality, check out the project and run `./gradlew runExample`.


## Performance notes

Angler will not generate garbage once in a steady-state.

Adding and removing sockets from the monitored set will cause allocation.
Large changes in the number of active UDP sockets on the system will cause one-time allocation of a larger read-buffer for `/proc/net/udp`.


## Change log

### 1.0.2

   * Upgrade to Agrona 0.5.1