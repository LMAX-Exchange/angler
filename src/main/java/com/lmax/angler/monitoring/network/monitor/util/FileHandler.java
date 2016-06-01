package com.lmax.angler.monitoring.network.monitor.util;

import java.nio.ByteBuffer;

public interface FileHandler
{
    void handleData(final ByteBuffer src, final int startPosition, final int endPosition);

    void noFurtherData();
}
