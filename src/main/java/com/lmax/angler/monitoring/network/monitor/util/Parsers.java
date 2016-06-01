package com.lmax.angler.monitoring.network.monitor.util;

public final class Parsers {
    private static final byte COLUMN_DELIMITER = (byte) ' ';
    private static final byte ROW_DELIMITER = (byte) '\n';

    public static TokenHandler rowColumnParser(
            TokenHandler tokenHandler) {
        return new DelimitedDataParser(
                new DelimitedDataParser(
                        tokenHandler,
                        COLUMN_DELIMITER,
                        true),
                ROW_DELIMITER,
                true);
    }

    private Parsers() {}
}
