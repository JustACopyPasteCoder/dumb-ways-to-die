package com.aldaypcs.dumbwaystodie;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.StandardCopyOption;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import net.runelite.client.util.Filepath;

final class SoundFiles
{
	private SoundFiles()
	{
	}

	/**
	 * Copies source over destination, but only if Java can play it. Formats like MP3 throw
	 * UnsupportedAudioFileException and leave the current sound untouched.
	 */
	static void copyIfPlayable(Filepath source, Filepath destination) throws IOException, UnsupportedAudioFileException
	{
		lengthSeconds(source);
		source.copyTo(destination, StandardCopyOption.REPLACE_EXISTING);
	}

	static double lengthSeconds(Filepath file) throws IOException, UnsupportedAudioFileException
	{
		try (AudioInputStream in = AudioSystem.getAudioInputStream(new BufferedInputStream(file.openInputStream())))
		{
			return in.getFrameLength() / in.getFormat().getFrameRate();
		}
	}
}
