package com.epickrram.monitoring.network.monitor;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.lang.Thread.currentThread;
import static java.nio.file.Files.copy;

public final class ResourceUtil
{
    private ResourceUtil() {}

    public static void writeDataFile(final String resourceName, final Path sourcePath) throws IOException, URISyntaxException
    {
        copy(Paths.get(currentThread().getContextClassLoader().getResource(resourceName).toURI()),
                new FileOutputStream(sourcePath.toFile(), false));
    }
}
