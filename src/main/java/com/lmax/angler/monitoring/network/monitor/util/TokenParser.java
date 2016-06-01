package com.lmax.angler.monitoring.network.monitor.util;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

final class TokenParser implements FileHandler
{
    private final TokenHandler tokenHandler;
    private final byte delimiter;
    private final ByteBuffer buffer;

    TokenParser(
            final TokenHandler tokenHandler,
            final byte delimiter,
            int maximumTokenSize)
    {
        this.tokenHandler = tokenHandler;
        this.delimiter = delimiter;
        this.buffer = ByteBuffer.allocateDirect(maximumTokenSize);
    }

    public void handleData(final ByteBuffer src, final int startPosition, final int endPosition)
    {
        // Strategy to minimise copying
        //    1.) if the buffer has content, add to it until we see a delimiter, then dispatch that token
        //    2.) peruse the remaining data in source for tokens
        //    3.) park any leftover bytes in the internal buffer
        int currentPosition = startPosition;
        if (buffer.position() > 0)
        {
            while (currentPosition < endPosition && src.get(currentPosition) != delimiter)
            {
                putOne(src.get(currentPosition));
                currentPosition++;
            }

            final boolean tokenFound = currentPosition != endPosition;
            if (tokenFound)
            {
                putOne(src.get(currentPosition));
                currentPosition++;

                dispatchFromInternalBuffer();
            }
        }

        while (currentPosition < endPosition)
        {
            int tokenStart = currentPosition;
            while (currentPosition < endPosition && src.get(currentPosition) != delimiter)
            {
                currentPosition++;
            }

            final boolean tokenFound = currentPosition != endPosition;
            if (tokenFound)
            {
                currentPosition++;

                dispatchToTokenHandler(src, tokenStart, currentPosition);
            }
            else
            {
                copyToInternalBuffer(src, tokenStart, currentPosition);
            }
        }
    }

    private void putOne(final byte b)
    {
        try
        {
            buffer.put(b);
        }
        catch (BufferOverflowException boe)
        {
            throw new RuntimeException(
                    "Encountered input without delimiter larger than buffer size (" + buffer.capacity() + ")",
                    boe);
        }
    }

    @Override
    public void noFurtherData()
    {
        if (buffer.position() > 0)
        {
            buffer.flip();
            tokenHandler.handleToken(buffer, 0, buffer.remaining());
            buffer.clear();
        }
    }

    private void dispatchToTokenHandler(final ByteBuffer bb, final int startPosition, final int endPosition)
    {
        if (endPosition - startPosition > 1)
        {
            tokenHandler.handleToken(bb, startPosition, endPosition - 1);
        }
    }

    private void dispatchFromInternalBuffer()
    {
        buffer.flip();
        dispatchToTokenHandler(buffer, 0, buffer.remaining());
        buffer.clear();
    }

    private void copyToInternalBuffer(final ByteBuffer bb, final int startPosition, final int endPosition)
    {
        for (int i = startPosition; i < endPosition; i++)
        {
            putOne(bb.get(i));
        }
    }
}