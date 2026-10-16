package com.aldaypcs.dumbwaystodie;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import net.runelite.client.util.Filepath;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class SoundFilesTest
{
	private static final int PCM = 1;
	private static final int MP3 = 0x55;

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Test
	public void copiesPlayableWav() throws Exception
	{
		File source = writeWav("clip.wav", PCM, 2.0, true);
		Filepath destination = rooted(folder.newFolder("plugin")).joinSegment("death.wav");

		SoundFiles.copyIfPlayable(rooted(source), destination);

		assertArrayEquals(Files.readAllBytes(source.toPath()), Files.readAllBytes(Filepath.Unchecked.getPath(destination)));
		assertEquals(2.0, SoundFiles.lengthSeconds(destination), 0.01);
	}

	@Test
	public void replacesExistingSound() throws Exception
	{
		Filepath destination = rooted(folder.newFolder("plugin")).joinSegment("death.wav");
		SoundFiles.copyIfPlayable(rooted(writeWav("first.wav", PCM, 1.0, false)), destination);

		SoundFiles.copyIfPlayable(rooted(writeWav("second.wav", PCM, 3.0, false)), destination);

		assertEquals(3.0, SoundFiles.lengthSeconds(destination), 0.01);
	}

	@Test
	public void rejectsMp3() throws Exception
	{
		File mp3 = folder.newFile("song.mp3");
		Files.write(mp3.toPath(), new byte[]{'I', 'D', '3', 4, 0, 0, 0, 0, 0, 0, 1, 2, 3, 4, 5, 6});

		assertRejected(mp3);
	}

	@Test
	public void rejectsCompressedWav() throws Exception
	{
		assertRejected(writeWav("compressed.wav", MP3, 1.0, false));
	}

	@Test
	public void rejectsWavWithNoAudio() throws Exception
	{
		File full = writeWav("full.wav", PCM, 1.0, false);
		File truncated = folder.newFile("truncated.wav");
		Files.write(truncated.toPath(), Arrays.copyOf(Files.readAllBytes(full.toPath()), 30));

		assertRejected(truncated);
	}

	private void assertRejected(File file) throws Exception
	{
		Filepath destination = rooted(folder.newFolder()).joinSegment("death.wav");
		try
		{
			SoundFiles.copyIfPlayable(rooted(file), destination);
			fail("Expected " + file.getName() + " to be rejected");
		}
		catch (SoundFiles.NotPlayableException expected)
		{
			assertFalse(destination.exists());
		}
	}

	private static Filepath rooted(File file)
	{
		return Filepath.Unchecked.getRooted(file.toPath());
	}

	// Silent 16-bit 44.1 kHz stereo WAV, optionally with a LIST chunk like ffmpeg writes
	private File writeWav(String name, int formatCode, double seconds, boolean withListChunk) throws IOException
	{
		int sampleRate = 44100;
		int blockAlign = 4;
		int dataSize = (int) (sampleRate * seconds) * blockAlign;
		byte[] list = "INFOISFT\5\0\0\0test\0\0".getBytes(StandardCharsets.US_ASCII);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.write(ascii("RIFF"));
		out.write(intLE(0));
		out.write(ascii("WAVE"));
		out.write(ascii("fmt "));
		out.write(intLE(16));
		out.write(shortLE(formatCode));
		out.write(shortLE(2));
		out.write(intLE(sampleRate));
		out.write(intLE(sampleRate * blockAlign));
		out.write(shortLE(blockAlign));
		out.write(shortLE(16));
		if (withListChunk)
		{
			out.write(ascii("LIST"));
			out.write(intLE(list.length));
			out.write(list);
		}
		out.write(ascii("data"));
		out.write(intLE(dataSize));
		out.write(new byte[dataSize]);

		File file = folder.newFile(name);
		Files.write(file.toPath(), out.toByteArray());
		return file;
	}

	private static byte[] ascii(String id)
	{
		return id.getBytes(StandardCharsets.US_ASCII);
	}

	private static byte[] intLE(int value)
	{
		return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array();
	}

	private static byte[] shortLE(int value)
	{
		return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort((short) value).array();
	}
}
