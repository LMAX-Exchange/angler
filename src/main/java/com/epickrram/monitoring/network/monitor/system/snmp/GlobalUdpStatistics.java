package com.epickrram.monitoring.network.monitor.system.snmp;

final class GlobalUdpStatistics
{
    private long inErrors;
    private long receiveBufferErrors;
    private long checksumErrors;

    long getInErrors()
    {
        return inErrors;
    }

    void setInErrors(final long inErrors)
    {
        this.inErrors = inErrors;
    }

    long getReceiveBufferErrors()
    {
        return receiveBufferErrors;
    }

    void setReceiveBufferErrors(final long receiveBufferErrors)
    {
        this.receiveBufferErrors = receiveBufferErrors;
    }

    long getChecksumErrors()
    {
        return checksumErrors;
    }

    void setChecksumErrors(final long checksumErrors)
    {
        this.checksumErrors = checksumErrors;
    }

    void reset()
    {
        inErrors = 0;
        receiveBufferErrors = 0;
        checksumErrors = 0;
    }
}
