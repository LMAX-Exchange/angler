package com.epickrram.monitoring.network;

import net.openhft.affinity.Affinity;

public final class AffinityWrapper implements Runnable
{
    public static int UNSET_AFFINITY = -1;

    private final Runnable delegate;
    private final int affinity;

    public AffinityWrapper(final Runnable delegate, final int affinity)
    {
        this.delegate = delegate;
        this.affinity = affinity;
    }

    @Override
    public void run()
    {
        if(affinity != UNSET_AFFINITY)
        {
            Affinity.setAffinity(affinity);
        }

        delegate.run();
    }

    public static Runnable runOnThread(final int cpuId, final Runnable delegate)
    {
        return new AffinityWrapper(delegate, cpuId);
    }
}
