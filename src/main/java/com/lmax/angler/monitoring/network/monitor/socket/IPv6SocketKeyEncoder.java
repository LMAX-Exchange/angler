package com.lmax.angler.monitoring.network.monitor.socket;


import java.net.Inet6Address;
import java.nio.ByteBuffer;
import java.util.function.BiConsumer;

public final class IPv6SocketKeyEncoder implements BiConsumer<InodeIdentifiedSocketDescriptor, ByteBuffer>
{
    public static final int KEY_LENGTH = 28;

    @Override
    public void accept(final InodeIdentifiedSocketDescriptor descriptor, final ByteBuffer buffer)
    {
        final Inet6Address address = (Inet6Address) descriptor.getSocketAddress().getAddress();
        // generates garbage :(
        buffer.put(address.getAddress());
        buffer.putInt(descriptor.getSocketAddress().getPort());
        buffer.putLong(descriptor.getInode());
    }
}
