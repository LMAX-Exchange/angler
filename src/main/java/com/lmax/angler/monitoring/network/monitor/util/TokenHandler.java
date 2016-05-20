package com.lmax.angler.monitoring.network.monitor.util;

import java.nio.ByteBuffer;

/**
 * A handler for a single token of delimited data, invoked from DelimitedDataParser.
 */
public interface TokenHandler
{
    /**
     * Handle a single token.
     * @param src data source
     * @param startPosition the start position of the token
     * @param endPosition the end position of the token
     */
    void handleToken(final ByteBuffer src, final int startPosition, final int endPosition);

    /**
     * Current token set is complete.
     */
    void complete();

    /**
     * Reset state in preparation for handling a new data set.
     */
    void reset();
}
