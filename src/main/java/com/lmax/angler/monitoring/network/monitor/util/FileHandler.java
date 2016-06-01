package com.lmax.angler.monitoring.network.monitor.util;

import java.nio.ByteBuffer;

public interface FileHandler
{
    void handleData(ByteBuffer src, int startPosition, int endPosition);

    void noFurtherData();
}
