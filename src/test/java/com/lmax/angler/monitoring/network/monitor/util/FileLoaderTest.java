package com.lmax.angler.monitoring.network.monitor.util;

import org.junit.Test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static java.nio.file.Files.copy;
import static java.nio.file.Files.createTempFile;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class FileLoaderTest
{
    private static final String LONG_FILE = "src/test/resources/long_file_loader_file.txt";
    private static final String SHORT_FILE = "src/test/resources/short_file_loader_file.txt";

    private final Path inputPath;
    private final FileLoader fileLoader;

    public FileLoaderTest() throws IOException
    {
        inputPath = createTempFile("file-loader-file", "txt");
        fileLoader = new FileLoader(inputPath, 128);
    }

    @Test
    public void loadReadsEntireFile() throws Exception
    {
        writeData(LONG_FILE);

        final FileLoader fileLoader = new FileLoader(inputPath, 128);
        fileLoader.load();

        assertThat(fileLoader.getBuffer().remaining(), is((int)(Files.size(inputPath))));
    }

    @Test
    public void fileLongerThanBufferSizeIsChunked() throws IOException, URISyntaxException
    {
        writeData(LONG_FILE);

        final FileLoader fileLoader = new FileLoader(inputPath, 128);
        final CapturingDataHandler ch = new CapturingDataHandler();
        fileLoader.run(ch);

        final String fileContent = fileContent(LONG_FILE);
        assertThat(ch.seenStrings.get(0), is(fileContent.substring(0, 128)));
        assertThat(ch.seenStrings.get(1), is(fileContent.substring(128, 256)));
        assertThat(ch.seenStrings.get(2), is(fileContent.substring(256, 384)));
        assertTrue(ch.dataEnded);
    }

    @Test
    public void updatedFileContentIsSeenOnSecondRun() throws IOException, URISyntaxException
    {
        writeData(LONG_FILE);

        fileLoader.run(new CapturingDataHandler());

        writeData(SHORT_FILE);

        final CapturingDataHandler ch = new CapturingDataHandler();
        fileLoader.run(ch);

        final String fileContent = fileContent(SHORT_FILE);
        assertThat(ch.seenStrings.get(0), is(fileContent.substring(0, 128)));
        assertThat(ch.seenStrings.get(1), is(fileContent.substring(128, 231)));
        assertTrue(ch.dataEnded);
    }

    private void writeData(String filename) throws IOException
    {
        copy(Paths.get(filename), new FileOutputStream(inputPath.toFile(), false));
    }

    private String fileContent(String filename) throws IOException
    {
        return new String(Files.readAllBytes(Paths.get(filename)), StandardCharsets.US_ASCII);
    }

    private class CapturingDataHandler implements FileHandler
    {
        private final List<String> seenStrings = new ArrayList<>();
        private boolean dataEnded = false;

        @Override
        public void handleData(ByteBuffer src, int startPosition, int endPosition)
        {
            final byte[] bytes = new byte[endPosition - startPosition];
            for (int i = 0; i < bytes.length; i++)
            {
                bytes[i] = src.get(startPosition + i);
            }
            seenStrings.add(new String(bytes, StandardCharsets.US_ASCII));
        }

        @Override
        public void noFurtherData()
        {
            dataEnded = true;
        }
    }
}