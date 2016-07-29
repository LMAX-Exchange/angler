package com.lmax.angler.monitoring.network.monitor.socket;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;

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
     * Pack IPv4 address and match-all socket flag into a long.
     * @param inetAddress the host address
     * @return the encoded value
     */
    public static long fromInet4Address(final InetAddress inetAddress)
    {
        ensureIsInet4Address(inetAddress);

        return Integer.toUnsignedLong(inetAddress.hashCode());
    }

    /**
     * Pack IPv4 address and socket into a long.
     * @param socketAddress the socket address
     * @param inode the socket inode
     * @return the encoded value
     */
    public static long fromInet4SocketAddressAndInode(final InetSocketAddress socketAddress, final int inode)
    {
        return overlayInode(fromInet4SocketAddress(socketAddress), inode);
    }

    /**
     * Pack IPv4 address and match-all socket flag into a long.
     * @param inetAddress the host address
     * @param inode the socket inode
     * @return the encoded value
     */
    public static long fromInet4AddressAndInode(final InetAddress inetAddress, final int inode)
    {
        return overlayInode(fromInet4Address(inetAddress), inode);
    }

    /**
     * Is this socketIdentifier a match for all ports on the encoded IP address.
     * @param socketIdentifier the encoded value
     * @return whether this socketIdentifier matches all ports
     */
    public static boolean isMatchAllSocketFlagSet(final long socketIdentifier)
    {
        return extractPortNumber(socketIdentifier) == 0L;
    }

    /**
     * Mast the port number out of this socket identifier.
     * @param socketIdentifier the encoded value
     * @return the socketIdentifier with the port number masked out
     */
    public static long asMatchAllSocketsSocketIdentifier(final long socketIdentifier)
    {
        return 0xFFFF0000FFFFFFFFL & socketIdentifier;
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
        return (int) ((socketIdentifier >> 32) & 0xFFFFL);
    }

    /**
     * Generates a vaguely human-readable format for a given socket identifier.
     * @param socketIdentifier the encoded socket identifier
     * @return human-readable form
     */
    public static String toDebugString(final long socketIdentifier)
    {
        final int ipBits = (int) socketIdentifier;
        final int port = extractPortNumber(socketIdentifier);
        final int inode = (int) (socketIdentifier >> 48);

        return Integer.toHexString(ipBits) + ":" + port + "/" + inode;
    }

    private static void validateAddressType(final InetSocketAddress socketAddress)
    {
        ensureIsInet4Address(socketAddress.getAddress());
    }

    private static void ensureIsInet4Address(final InetAddress address)
    {
        if(!(address instanceof Inet4Address))
        {
            throw new IllegalArgumentException("Due to the nature of some awful hacks, " +
                    "I only work with Inet4Address-based sockets, not: " + address.getClass().getSimpleName());
        }
    }
}