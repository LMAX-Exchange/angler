package com.epickrram.monitoring.network;

import java.nio.ByteBuffer;

public final class MessageFactory
{
    private final ByteBuffer payload = ByteBuffer.allocateDirect(128);

    public ByteBuffer prepare()
    {
        payload.clear();
        payload.putLong(System.nanoTime());
        payload.flip();

        return payload;
    }
}
