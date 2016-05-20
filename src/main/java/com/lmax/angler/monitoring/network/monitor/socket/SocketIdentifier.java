package com.lmax.angler.monitoring.network.monitor.socket;

import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

/**
 * Utility class for packing IPv4 socket endpoint identifiers into a 64-bit long.
 */
public final class SocketIdentifier
{
    private SocketIdentifier() {}

    /**
     * Pack IPv4 address and socket into a long.
     * @param socketAddress the socket address
     * @return the encoded value
     */
    public static long fromInet4SocketAddress(final InetSocketAddress socketAddress)
    {
        validateAddressType(socketAddress);
        final long ipAddressOctets = Integer.toUnsignedLong(socketAddress.getAddress().hashCode());
        final long port = socketAddress.getPort();
        return port << 32 | ipAddressOctets;
    }

    /**
     * Pack a hex-encoded IPv4:port socket address into a long.
     * @param decodedAddress 32-bit IPv4 address decoded from hex
     * @param port the port number
     * @return the encoded value
     */
    public static long fromLinuxKernelHexEncodedAddressAndPort(final long decodedAddress, final long port)
    {
        return port << 32 | Long.reverseBytes(decodedAddress) >>> 32;
    }

    /**
     * Pack inode value into socketIdentifier to differentiate between difference sockets listening to the same port.
     * @param socketIdentifier the socketIdentifier
     * @param inode the inode
     * @return the encoded value
     */
    public static long overlayInode(final long socketIdentifier, final long inode)
    {
        return  inode << 48 | socketIdentifier;
    }

    /**
     * Extract port number from a socketIdentifier.
     * @param socketIdentifier the socketIdentifier
     * @return the port number
     */
    public static int extractPortNumber(final long socketIdentifier)
    {
        return (int) ((socketIdentifier >> 32) & 0xFFFF);
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
