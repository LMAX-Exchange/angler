package com.lmax.angler.monitoring.network.monitor.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class FileLoader
{
    private final Path path;
    private ByteBuffer buffer;
    private FileChannel fileChannel;

    public FileLoader(final Path path, final int initialBufferCapacity)
    {
        this.path = path;
        buffer = ByteBuffer.allocateDirect(initialBufferCapacity);
    }

    public void load()
    {
        try
        {
            if (fileChannel == null)
            {
                fileChannel = FileChannel.open(path, StandardOpenOption.READ);
            }
            final long fileSize = Files.size(path);

            if (fileSize > buffer.capacity())
            {
                buffer = ByteBuffer.allocateDirect((int) Math.max(buffer.capacity() * 2, fileSize));
            }

            buffer.clear();
            fileChannel.read(buffer, 0);
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
