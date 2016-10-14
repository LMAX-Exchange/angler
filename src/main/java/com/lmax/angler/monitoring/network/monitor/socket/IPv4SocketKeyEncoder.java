package com.lmax.angler.monitoring.network.monitor.socket;


import java.net.Inet4Address;
import java.nio.ByteBuffer;
import java.util.function.BiConsumer;

public final class IPv4SocketKeyEncoder implements BiConsumer<InodeIdentifiedSocketDescriptor, ByteBuffer>
{
    public static final int KEY_LENGTH = 16;

    @Override
    public void accept(final InodeIdentifiedSocketDescriptor descriptor, final ByteBuffer buffer)
    {
        final Inet4Address address = (Inet4Address) descriptor.getSocketAddress().getAddress();
        buffer.putInt(address.hashCode());
        buffer.putInt(descriptor.getSocketAddress().getPort());
        buffer.putLong(descriptor.getInode());
    }
}
