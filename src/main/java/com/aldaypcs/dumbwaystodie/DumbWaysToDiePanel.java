package com.aldaypcs.dumbwaystodie;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.Filepath;

class DumbWaysToDiePanel extends PluginPanel
{
	private final JLabel statusLabel = new JLabel();

	DumbWaysToDiePanel(DumbWaysToDiePlugin plugin)
	{
		setBorder(new EmptyBorder(10, 10, 10, 10));
		setLayout(new BorderLayout());

		JPanel content = new JPanel(new DynamicGridLayout(0, 1, 0, 8));

		JLabel title = new JLabel("Dumb Ways to Die");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(Color.WHITE);
		content.add(title);

		content.add(wrappedLabel("Choose a WAV file to play when your character dies. "
			+ "The volume is in the plugin's settings.", ColorScheme.LIGHT_GRAY_COLOR));

		content.add(statusLabel);

		JButton chooseButton = new JButton("Choose WAV file...");
		chooseButton.addActionListener(e ->
		{
			List<Filepath> chosen = new Filepath.Chooser()
				.setIsOpen()
				.setAcceptsFiles()
				.setDialogTitle("Choose a death sound")
				.addExtensionFilter("WAV audio", "wav")
				.showDialog(this);
			if (!chosen.isEmpty())
			{
				plugin.importSound(chosen.get(0));
			}
		});
		content.add(chooseButton);

		JButton testButton = new JButton("Play test");
		testButton.addActionListener(e -> plugin.playSound());
		content.add(testButton);

		add(content, BorderLayout.NORTH);
	}

	void showStatus(String text)
	{
		setStatus(text, Color.WHITE);
	}

	void showError(String text)
	{
		setStatus(text, ColorScheme.PROGRESS_ERROR_COLOR);
	}

	private void setStatus(String text, Color color)
	{
		SwingUtilities.invokeLater(() ->
		{
			statusLabel.setText("<html>" + text + "</html>");
			statusLabel.setForeground(color);
		});
	}

	private static JLabel wrappedLabel(String text, Color color)
	{
		JLabel label = new JLabel("<html>" + text + "</html>");
		label.setForeground(color);
		return label;
	}
}
