package com.lmax.angler.monitoring.network.monitor.util;

public final class Parsers
{
    private static final byte COLUMN_DELIMITER = (byte) ' ';
    private static final byte ROW_DELIMITER = (byte) '\n';
    private static final int MAXIMUM_LINE_LENGTH = 1024;

    public static FileHandler rowColumnHandler(
            final TokenHandler tokenHandler)
    {
        return new TokenParser(
                new DelimitedDataParser(
                        tokenHandler,
                        COLUMN_DELIMITER
                ),
                ROW_DELIMITER,
                MAXIMUM_LINE_LENGTH);
    }

    private Parsers() {}
}
