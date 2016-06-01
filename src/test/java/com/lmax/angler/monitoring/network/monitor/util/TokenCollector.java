package com.lmax.angler.monitoring.network.monitor.util;

import java.nio.ByteBuffer;
import java.util.List;

final class TokenCollector implements TokenHandler
{
    private final List<String> collectedTokens;

    TokenCollector(final List<String> collectedTokens)
    {
        this.collectedTokens = collectedTokens;
    }

    @Override
    public void handleToken(final ByteBuffer src, final int startPosition, final int endPosition)
    {
        final byte[] tmp = new byte[endPosition - startPosition];
        final int bufferPosition = src.position();
        final int bufferLimit = src.limit();
        src.position(startPosition).limit(endPosition);
        src.get(tmp, 0, endPosition - startPosition);
        src.position(bufferPosition).limit(bufferLimit);
        collectedTokens.add(new String(tmp));
    }

    @Override
    public void complete()
    {

    }

}
