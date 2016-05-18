package com.epickrram.monitoring.network.monitor.util;

import java.nio.ByteBuffer;

public final class BufferToString
{
    private BufferToString() {}

    public static String bufferToString(final ByteBuffer src, final int startPosition, final int endPosition)
    {
        final int srcPos = src.position();
        final int srcLimit = src.limit();
        final byte[] tmp = new byte[endPosition - startPosition];
        src.position(startPosition).limit(endPosition);
        src.get(tmp);
        src.position(srcPos).limit(srcLimit);
        return new String(tmp);
    }
}
