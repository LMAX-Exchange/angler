package com.lmax.angler.monitoring.network.monitor.util;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class TokenParserTest
{
    private final List<String> collectedTokens = new ArrayList<>();
    private final TokenParser parser = new TokenParser(new TokenCollector(collectedTokens), (byte) '\n', 64);

    @Test
    public void emptyInputYieldsNoTokens()
    {
        handleData("");
        parser.noFurtherData();

        assertThat(collectedTokens.size(), is(0));
    }

    @Test
    public void inputEntirelyOfDelimitersYieldsNoTokens()
    {
        handleData("\n\n\n\n\n\n");
        handleData("\n\n\n");
        parser.noFurtherData();

        assertThat(collectedTokens.size(), is(0));
    }

    @Test
    public void finalTokenIsDeliveredOnNotificationOfNoFurtherData()
    {
        handleData("foo\nbar");

        assertThat(collectedTokens, is(singletonList("foo")));

        parser.noFurtherData();

        assertThat(collectedTokens, is(asList("foo", "bar")));
    }

    @Test
    public void parsesNonEmptyTokensFromChunkedInput()
    {
        handleData("hello\nthis is some input with");
        handleData(" line breaks\n\n\n\nsplit across several ");
        handleData("invocations");
        handleData("\n\n\n\n");
        handleData("\nmoo\n\n\ni\n\nsay");
        parser.noFurtherData();

        assertThat(
                collectedTokens,
                is(asList(
                        "hello",
                        "this is some input with line breaks",
                        "split across several invocations",
                        "moo",
                        "i",
                        "say")));
    }

    @Test
    public void throwHelpfulErrorWhenBufferOverflows()
    {
        try
        {
            final byte[] chars = new byte[65];
            Arrays.fill(chars, (byte) 'a');
            handleData(new String(chars, StandardCharsets.US_ASCII));
        }
        catch (RuntimeException re)
        {
            assertThat(re.getMessage(), is("Encountered input without delimiter larger than buffer size (64)"));
        }
    }

    private void handleData(String pieceOne)
    {
        parser.handleData(bufferForData(pieceOne), 0, pieceOne.length());
    }

    private static ByteBuffer bufferForData(final String data)
    {
        return ByteBuffer.wrap(data.getBytes(StandardCharsets.UTF_8));
    }
}