package com.aldaypcs.dumbwaystodie;

import com.google.inject.Provides;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.audio.AudioPlayer;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.Filepath;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
	name = "Dumb Ways to Die",
	description = "Plays a sound of your choice when your character dies",
	tags = {"death", "sound", "audio"},
	internalName = DumbWaysToDiePlugin.INTERNAL_NAME
)
public class DumbWaysToDiePlugin extends Plugin
{
	static final String INTERNAL_NAME = "dumb-ways-to-die";
	static final String SOUND_FILE_NAME = "death.wav";
	private static final String TEST_COMMAND = "deathsound";

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private DumbWaysToDieConfig config;

	@Inject
	private AudioPlayer audioPlayer;

	@Inject
	private ScheduledExecutorService executor;

	private Filepath soundFile;
	private DumbWaysToDiePanel panel;
	private NavigationButton navigationButton;
	private boolean remindedAboutMissingFile;

	@Override
	protected void startUp() throws Exception
	{
		Filepath directory = getPluginDirectory();
		directory.createDirectories();
		soundFile = directory.joinSegment(SOUND_FILE_NAME);
		remindedAboutMissingFile = false;

		panel = new DumbWaysToDiePanel(this);
		navigationButton = NavigationButton.builder()
			.tooltip("Dumb Ways to Die")
			.icon(ImageUtil.loadImageResource(getClass(), "panel_icon.png"))
			.priority(10)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navigationButton);

		executor.execute(this::updateStatus);
	}

	@Override
	protected void shutDown() throws Exception
	{
		clientToolbar.removeNavigation(navigationButton);
		navigationButton = null;
		panel = null;
		soundFile = null;
	}

	@Subscribe
	public void onActorDeath(ActorDeath actorDeath)
	{
		if (actorDeath.getActor() == client.getLocalPlayer())
		{
			playSound();
		}
	}

	// Typing ::deathsound in the chatbox plays the sound without having to die
	@Subscribe
	public void onCommandExecuted(CommandExecuted commandExecuted)
	{
		if (TEST_COMMAND.equalsIgnoreCase(commandExecuted.getCommand()))
		{
			playSound();
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		Filepath file = soundFile;
		if (gameStateChanged.getGameState() != GameState.LOGGED_IN || remindedAboutMissingFile || file == null)
		{
			return;
		}

		remindedAboutMissingFile = true;
		executor.execute(() ->
		{
			if (!file.exists())
			{
				sendMissingFileMessage();
			}
		});
	}

	void playSound()
	{
		Filepath file = soundFile;
		int volume = config.volume();
		if (file == null || volume <= 0)
		{
			return;
		}

		// Loading the clip reads from disk, so keep it off the client thread
		executor.execute(() ->
		{
			if (!file.exists())
			{
				sendMissingFileMessage();
				return;
			}

			try
			{
				audioPlayer.play(file, toGain(volume));
			}
			catch (IOException | UnsupportedAudioFileException | LineUnavailableException e)
			{
				log.warn("Couldn't play the death sound", e);
				sendChatMessage("Dumb Ways to Die couldn't play its sound: " + e.getMessage());
			}
		});
	}

	// Called from the panel after the user picks a file
	void importSound(Filepath chosen)
	{
		Filepath destination = soundFile;
		DumbWaysToDiePanel currentPanel = panel;
		if (destination == null || currentPanel == null)
		{
			return;
		}

		executor.execute(() ->
		{
			try
			{
				SoundFiles.copyIfPlayable(chosen, destination);
				updateStatus();
			}
			catch (UnsupportedAudioFileException e)
			{
				currentPanel.showError("That file can't be played. Only WAV files work; 16-bit PCM is safest.");
			}
			catch (IOException e)
			{
				log.warn("Couldn't import the death sound", e);
				currentPanel.showError("Couldn't copy that file: " + e.getMessage());
			}
		});
	}

	// Reads the sound file, so only call this from the executor
	private void updateStatus()
	{
		Filepath file = soundFile;
		DumbWaysToDiePanel currentPanel = panel;
		if (file == null || currentPanel == null)
		{
			return;
		}

		if (!file.exists())
		{
			currentPanel.showStatus("No sound chosen yet.");
			return;
		}

		try
		{
			currentPanel.showStatus(String.format("Sound set (%.1f seconds).", SoundFiles.lengthSeconds(file)));
		}
		catch (IOException | UnsupportedAudioFileException e)
		{
			currentPanel.showError("The saved sound can't be played. Choose another WAV file.");
		}
	}

	// AudioPlayer takes gain in decibels; 100% volume is 0 dB
	private static float toGain(int volumePercent)
	{
		return (float) (20 * Math.log10(volumePercent / 100.0));
	}

	private void sendMissingFileMessage()
	{
		sendChatMessage("Dumb Ways to Die has no sound yet. Choose a WAV file in its sidebar panel.");
	}

	private void sendChatMessage(String message)
	{
		clientThread.invokeLater(() -> client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null));
	}

	@Provides
	DumbWaysToDieConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(DumbWaysToDieConfig.class);
	}
}
