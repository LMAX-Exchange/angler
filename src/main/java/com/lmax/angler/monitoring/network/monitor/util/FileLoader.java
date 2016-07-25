package com.lmax.angler.monitoring.network.monitor.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Utility for repeated reads of a file using a ByteBuffer to avoid allocating unnecessary objects.
 */
public final class FileLoader
{
    private final Path path;
    private final ByteBuffer tmp = ByteBuffer.allocateDirect(4096);
    private ByteBuffer buffer;
    private FileChannel fileChannel;

    public FileLoader(final Path path, final int initialBufferCapacity)
    {
        this.path = path;
        buffer = ByteBuffer.allocateDirect(initialBufferCapacity);
    }

    /**
     * Opens a channel to the specified path if it does not already exist.
     * Allocates a larger ByteBuffer if file size &gt; current buffer size.
     * Reads file data into ByteBuffer.
     */
    public void load()
    {
        try
        {
            if (fileChannel == null)
            {
                fileChannel = FileChannel.open(path, StandardOpenOption.READ);
            }

            fileChannel.position(0L);
            buffer.clear();
            tmp.clear();
            while(fileChannel.read(tmp) > 0)
            {
                tmp.flip();
                if(tmp.remaining() > buffer.capacity() - buffer.position())
                {
                    final ByteBuffer next = ByteBuffer.allocateDirect(Math.max(buffer.capacity() * 2, tmp.remaining()));
                    buffer.flip();
                    next.put(buffer);
                    buffer = next;
                }
                buffer.put(tmp);
                tmp.clear();
            }

            buffer.flip();
        }
        catch(final IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    public ByteBuffer getBuffer()
    {
        return buffer;
    }
}
