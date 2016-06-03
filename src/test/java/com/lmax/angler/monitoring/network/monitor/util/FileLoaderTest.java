package com.lmax.angler.monitoring.network.monitor.util;

import org.junit.Test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.file.Files.copy;
import static java.nio.file.Files.createTempFile;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class FileLoaderTest
{
    private static final String LONG_FILE = "src/test/resources/long_file_loader_file.txt";

    private final Path inputPath;

    public FileLoaderTest() throws IOException
    {
        inputPath = createTempFile("file-loader-file", "txt");
    }

    @Test
    public void loadReadsEntireFile() throws Exception
    {
        writeData(LONG_FILE);

        final FileLoader fileLoader = new FileLoader(inputPath, 128);
        fileLoader.load();

        assertThat(fileLoader.getBuffer().remaining(), is((int)(Files.size(inputPath))));
    }

    private void writeData(String filename) throws IOException
    {
        copy(Paths.get(filename), new FileOutputStream(inputPath.toFile(), false));
    }
}