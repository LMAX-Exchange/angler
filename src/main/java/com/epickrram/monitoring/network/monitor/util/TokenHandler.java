package com.epickrram.monitoring.network.monitor.util;

import java.nio.ByteBuffer;

public interface TokenHandler
{
    void handleToken(final ByteBuffer src, final int startPosition, final int endPosition);
    void complete();
    void reset();
}
