package com.lmax.angler.monitoring.network.monitor.socket;

import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

public final class SocketIdentifier
{
    private SocketIdentifier() {}

    public static long fromInet4SocketAddress(final InetSocketAddress socketAddress)
    {
        validateAddressType(socketAddress);
        final long ipAddressOctets = Integer.toUnsignedLong(socketAddress.getAddress().hashCode());
        final long port = socketAddress.getPort();
        return port << 32 | ipAddressOctets;
    }

    public static long fromLinuxKernelHexEncodedAddressAndPort(final long decodedAddress, final long port)
    {
        return port << 32 | Long.reverseBytes(decodedAddress) >>> 32;
    }

    public static long fromLinuxKernelHexEncodedAddressAndPortAndInode(final long decodedAddress,
                                                                       final long port,
                                                                       final long inode)
    {
        return inode << 48 | port << 32 | Long.reverseBytes(decodedAddress) >>> 32;
    }

    public static long overlayInode(final long socketIdentifier, final long inode)
    {
        return  inode << 48 | socketIdentifier;
    }

    public static int extractPortNumber(final long socketIdentifier)
    {
        return (int) ((socketIdentifier >> 32) & 0xFFFF);
    }

    public static long extractInode(final long socketIdentifier)
    {
        return (socketIdentifier >> 48);
    }

    public static String extractHostIpAddress(final long socketIdentifier) throws UnknownHostException
    {
        final byte[] address = new byte[4];
        address[3] = (byte) (socketIdentifier & 0xFF);
        address[2] = (byte) (socketIdentifier >> 8 & 0xFF);
        address[1] = (byte) (socketIdentifier >> 16 & 0xFF);
        address[0] = (byte) (socketIdentifier >> 24 & 0xFF);
        return Inet4Address.getByAddress(address).getHostAddress();
    }

    private static void validateAddressType(final InetSocketAddress socketAddress)
    {
        if(!(socketAddress.getAddress() instanceof Inet4Address))
        {
            throw new IllegalArgumentException("Due to the nature of some awful hacks, " +
                    "I only work with Inet4Address-based sockets");
        }
    }
}
