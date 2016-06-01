package com.lmax.angler.monitoring.network.monitor.util;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public final class DelimitedDataParserTest
{
    private final List<String> collectedTokens = new ArrayList<>();
    private final TokenCollector collector = new TokenCollector(collectedTokens);
    private final DelimitedDataParser parser = new DelimitedDataParser(collector, (byte) '\n');

    @Test
    public void shouldFindZeroTokensWhenDataIsEmpty() throws Exception
    {
        final ByteBuffer src = bufferForData("");
        parser.handleToken(src, src.position(), src.limit());

        assertTrue(collectedTokens.isEmpty());
    }

    @Test
    public void shouldFindZeroTokensWhenDataContainsOnlyDelimiters() throws Exception
    {
        final ByteBuffer src = bufferForData("\n\n");
        parser.handleToken(src, src.position(), src.limit());

        assertTrue(collectedTokens.isEmpty());
    }

    @Test
    public void shouldFindSingleToken() throws Exception
    {
        final String line = "1st line";
        final ByteBuffer src = bufferForData(line);
        parser.handleToken(src, src.position(), src.limit());

        assertThat(collectedTokens.size(), is(1));
        assertThat(collectedTokens.get(0), is(line));
    }

    @Test
    public void shouldFindMultipleTokens() throws Exception
    {
        final ByteBuffer src = bufferForData("1st line\n2nd line\n3rd line");
        parser.handleToken(src, src.position(), src.limit());

        assertThat(collectedTokens.size(), is(3));
        assertThat(collectedTokens.get(0), is("1st line"));
        assertThat(collectedTokens.get(1), is("2nd line"));
        assertThat(collectedTokens.get(2), is("3rd line"));
    }

    @Test
    public void shouldFindMultipleTokensWhenDataEndsInDelimiter() throws Exception
    {
        final ByteBuffer src = bufferForData("1st line\n2nd line\n3rd line\n");
        parser.handleToken(src, src.position(), src.limit());

        assertThat(collectedTokens.size(), is(3));
        assertThat(collectedTokens.get(0), is("1st line"));
        assertThat(collectedTokens.get(1), is("2nd line"));
        assertThat(collectedTokens.get(2), is("3rd line"));
    }

    @Test
    public void shouldConsumeConsecutiveDelimiters() throws Exception
    {
        final ByteBuffer src = bufferForData("\n\n1st line\n2nd line\n\n3rd line");
        parser.handleToken(src, src.position(), src.limit());

        assertThat(collectedTokens.size(), is(3));
        assertThat(collectedTokens.get(0), is("1st line"));
        assertThat(collectedTokens.get(1), is("2nd line"));
        assertThat(collectedTokens.get(2), is("3rd line"));
    }

    @Test
    public void shouldNest() throws Exception
    {
        final ByteBuffer src = bufferForData("\n\n 1st    line \n 2nd   line \n\n  3rd    line");
        final DelimitedDataParser inner = new DelimitedDataParser(collector, (byte)'\n');
        final DelimitedDataParser outer = new DelimitedDataParser(inner, (byte)' ');
        outer.handleToken(src, src.position(), src.limit());

        assertThat(collectedTokens.size(), is(6));
        System.out.println(collectedTokens);
    }

    private static ByteBuffer bufferForData(final String data)
    {
        return ByteBuffer.wrap(data.getBytes(StandardCharsets.UTF_8));
    }
}