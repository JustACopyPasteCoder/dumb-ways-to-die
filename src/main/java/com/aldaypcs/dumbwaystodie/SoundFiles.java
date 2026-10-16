package com.aldaypcs.dumbwaystodie;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardCopyOption;
import net.runelite.client.util.Filepath;

/**
 * Checks WAV files by reading their RIFF header directly. Hub plugins have to play sound through
 * AudioPlayer rather than javax.sound, and AudioPlayer can't inspect a file without playing it.
 */
final class SoundFiles
{
	private static final int FORMAT_PCM = 1;
	private static final int FORMAT_EXTENSIBLE = 0xFFFE;

	static final class NotPlayableException extends Exception
	{
		NotPlayableException(String message)
		{
			super(message);
		}
	}

	private SoundFiles()
	{
	}

	/**
	 * Copies source over destination, but only if it's an uncompressed WAV. Anything else, like an
	 * MP3, throws NotPlayableException and leaves the current sound untouched.
	 */
	static void copyIfPlayable(Filepath source, Filepath destination) throws IOException, NotPlayableException
	{
		lengthSeconds(source);
		source.copyTo(destination, StandardCopyOption.REPLACE_EXISTING);
	}

	static double lengthSeconds(Filepath file) throws IOException, NotPlayableException
	{
		try (DataInputStream in = new DataInputStream(new BufferedInputStream(file.openInputStream())))
		{
			if (!"RIFF".equals(readId(in)))
			{
				throw new NotPlayableException("not a WAV file");
			}
			readIntLE(in);
			if (!"WAVE".equals(readId(in)))
			{
				throw new NotPlayableException("not a WAV file");
			}

			int format = -1;
			long byteRate = 0;
			while (true)
			{
				String chunkId = readId(in);
				long chunkSize = readIntLE(in) & 0xFFFFFFFFL;

				if ("fmt ".equals(chunkId))
				{
					format = readShortLE(in);
					in.skipBytes(6); // channels and sample rate
					byteRate = readIntLE(in) & 0xFFFFFFFFL;
					skipFully(in, chunkSize - 12 + (chunkSize & 1));
				}
				else if ("data".equals(chunkId))
				{
					if (format != FORMAT_PCM && format != FORMAT_EXTENSIBLE)
					{
						throw new NotPlayableException("only uncompressed (PCM) WAV files work");
					}
					if (byteRate == 0)
					{
						throw new NotPlayableException("the WAV header is damaged");
					}
					return chunkSize / (double) byteRate;
				}
				else
				{
					// Chunks are padded to an even length
					skipFully(in, chunkSize + (chunkSize & 1));
				}
			}
		}
		catch (EOFException e)
		{
			throw new NotPlayableException("the file ends before any audio");
		}
	}

	private static String readId(DataInputStream in) throws IOException
	{
		byte[] id = new byte[4];
		in.readFully(id);
		return new String(id, StandardCharsets.US_ASCII);
	}

	private static int readIntLE(DataInputStream in) throws IOException
	{
		return Integer.reverseBytes(in.readInt());
	}

	private static int readShortLE(DataInputStream in) throws IOException
	{
		return Short.reverseBytes(in.readShort()) & 0xFFFF;
	}

	private static void skipFully(DataInputStream in, long count) throws IOException
	{
		while (count > 0)
		{
			long skipped = in.skip(count);
			if (skipped <= 0)
			{
				// skip() can return 0 before EOF; read a byte to tell the two apart
				in.readByte();
				skipped = 1;
			}
			count -= skipped;
		}
	}
}
