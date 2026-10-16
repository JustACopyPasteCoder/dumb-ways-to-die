# Dumb Ways to Die

A RuneLite plugin that plays a sound of your choice when your character dies.

No sounds are included. You pick a WAV file you have the rights to use, and it stays on your computer.

## Setup

1. Open the **Dumb Ways to Die** panel from the RuneLite sidebar (the skull icon).
2. Click **Choose WAV file...** and pick your sound.
3. Click **Play test** to hear it. You can also type `::deathsound` in the chatbox.

The volume is in the plugin's settings.

## Audio format

Only WAV files work, because Java can't play MP3 or other compressed formats.
16-bit PCM WAV at 44.1 or 48 kHz is the safest choice. To convert a file, use a free editor such as
Audacity (File > Export > Export as WAV), or ffmpeg:

    ffmpeg -i clip.mp3 -ac 2 -ar 44100 -sample_fmt s16 clip.wav

## Notes

- The sound plays on every death of your own character, including safe deaths in minigames.
- Your chosen sound is copied to `.runelite/plugin-data/dumb-ways-to-die/death.wav`.
- The name refers to the well-known rail safety campaign. This plugin isn't affiliated with it and includes none of its audio.

## Development

`./gradlew run` launches a developer RuneLite client with the plugin loaded. To log in with a Jagex Account,
follow https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts
