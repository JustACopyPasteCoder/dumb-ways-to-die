package com.aldaypcs.dumbwaystodie;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import net.runelite.client.util.Filepath;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class SoundFilesTest
{
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Test
	public void copiesPlayableWav() throws Exception
	{
		File source = writeWav("clip.wav", 2.0);
		Filepath destination = rooted(folder.newFolder("plugin")).joinSegment("death.wav");

		SoundFiles.copyIfPlayable(rooted(source), destination);

		assertArrayEquals(Files.readAllBytes(source.toPath()), Files.readAllBytes(Filepath.Unchecked.getPath(destination)));
		assertEquals(2.0, SoundFiles.lengthSeconds(destination), 0.01);
	}

	@Test
	public void replacesExistingSound() throws Exception
	{
		Filepath destination = rooted(folder.newFolder("plugin")).joinSegment("death.wav");
		SoundFiles.copyIfPlayable(rooted(writeWav("first.wav", 1.0)), destination);

		SoundFiles.copyIfPlayable(rooted(writeWav("second.wav", 3.0)), destination);

		assertEquals(3.0, SoundFiles.lengthSeconds(destination), 0.01);
	}

	@Test
	public void rejectsFilesJavaCannotPlay() throws Exception
	{
		File notWav = folder.newFile("song.mp3");
		Files.write(notWav.toPath(), new byte[]{'I', 'D', '3', 4, 0, 0, 0, 0, 0, 0, 1, 2, 3, 4, 5, 6});
		Filepath destination = rooted(folder.newFolder("plugin")).joinSegment("death.wav");

		try
		{
			SoundFiles.copyIfPlayable(rooted(notWav), destination);
			fail("Expected the MP3 to be rejected");
		}
		catch (UnsupportedAudioFileException expected)
		{
			assertFalse(destination.exists());
		}
	}

	private static Filepath rooted(File file)
	{
		return Filepath.Unchecked.getRooted(file.toPath());
	}

	// One second of 16-bit 44.1 kHz stereo silence per second requested
	private File writeWav(String name, double seconds) throws IOException
	{
		AudioFormat format = new AudioFormat(44100, 16, 2, true, false);
		int frames = (int) (format.getFrameRate() * seconds);
		byte[] silence = new byte[frames * format.getFrameSize()];
		File file = folder.newFile(name);
		try (AudioInputStream in = new AudioInputStream(new ByteArrayInputStream(silence), format, frames))
		{
			AudioSystem.write(in, AudioFileFormat.Type.WAVE, file);
		}
		return file;
	}
}
