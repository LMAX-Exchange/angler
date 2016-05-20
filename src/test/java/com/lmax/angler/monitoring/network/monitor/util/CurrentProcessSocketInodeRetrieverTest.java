package com.lmax.angler.monitoring.network.monitor.util;

import org.agrona.collections.LongHashSet;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class CurrentProcessSocketInodeRetrieverTest
{
    private final CurrentProcessSocketInodeRetriever retriever = new CurrentProcessSocketInodeRetriever();

    @Test
    public void shouldFindSocketInodeValue() throws Exception
    {
        try(final DatagramChannel channel = DatagramChannel.open().bind(new InetSocketAddress(55555)))
        {
            final LongHashSet target = new LongHashSet(4);

            retriever.accept(target);

            assertThat(target.size(), is(atLeast(1)));
        }
    }

    @Test
    public void shouldFindMultipleSocketInodeValues() throws Exception
    {
        try(final DatagramChannel channel1 = DatagramChannel.open().bind(new InetSocketAddress(55555));
            final DatagramChannel channel2 = DatagramChannel.open().bind(new InetSocketAddress(44444)))
        {
            final LongHashSet target = new LongHashSet(4);

            retriever.accept(target);

            assertThat(target.size(), is(atLeast(2)));
        }
    }

    private Matcher<Integer> atLeast(final int expected)
    {
        return new TypeSafeMatcher<Integer>()
        {
            @Override
            protected boolean matchesSafely(final Integer item)
            {
                return item >= expected;
            }

            @Override
            public void describeTo(final Description description)
            {
                description.appendText(" at least " + expected);
            }
        };
    }
}