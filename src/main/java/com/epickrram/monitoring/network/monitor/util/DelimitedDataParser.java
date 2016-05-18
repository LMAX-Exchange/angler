package com.epickrram.monitoring.network.monitor.util;

import java.nio.ByteBuffer;

public final class DelimitedDataParser implements TokenHandler
{
    private final TokenHandler tokenHandler;
    private final byte delimiter;
    private final boolean skipConsecutiveDelimiters;

    public DelimitedDataParser(
            final TokenHandler tokenHandler,
            final byte delimiter,
            final boolean skipConsecutiveDelimiters)
    {
        this.tokenHandler = tokenHandler;
        this.delimiter = delimiter;
        this.skipConsecutiveDelimiters = skipConsecutiveDelimiters;
    }

    public void handleToken(final ByteBuffer src, final int startPosition, final int endPosition)
    {
        int currentPosition = startPosition;

        while(currentPosition < src.capacity() && src.get(currentPosition) == delimiter)
        {
            currentPosition++;
        }

        int tokenStart = currentPosition;

        while(currentPosition < endPosition)
        {
            while(currentPosition < endPosition && src.get(currentPosition) != delimiter)
            {
                currentPosition++;
            }

            if(currentPosition == endPosition)
            {
                tokenHandler.handleToken(src, tokenStart, currentPosition);
                break;
            }

            tokenHandler.handleToken(src, tokenStart, currentPosition);
            currentPosition += 1;

            while(
                    skipConsecutiveDelimiters &&
                    currentPosition < endPosition &&
                    src.get(currentPosition) == delimiter)
            {
                currentPosition++;
            }

            tokenStart = currentPosition;
        }

        tokenHandler.complete();
    }

    public void reset()
    {
        tokenHandler.reset();
    }

    public void complete()
    {

    }
}