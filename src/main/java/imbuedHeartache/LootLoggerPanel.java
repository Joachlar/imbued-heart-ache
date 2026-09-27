package imbuedHeartache;

import imbuedHeartache.service.SlayerKillCountService;
import net.runelite.api.Client;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.PluginErrorPanel;
import net.runelite.client.util.Filepath;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class LootLoggerPanel extends PluginPanel {

    public LootLoggerPanel(Client client, SlayerKillCountService slayerKillCountService) {


        final PluginErrorPanel errorPanel = new PluginErrorPanel();
        errorPanel.setBorder(new EmptyBorder(10, 25, 10, 25));
        errorPanel.setContent("Imbued Heartache", "Please select the folder named 'loots' in your .runelite directory");
        JButton findLootsFolderBtn = new JButton("Select loots folder");
        fetchFromFolder(client, slayerKillCountService, findLootsFolderBtn);
        errorPanel.add(findLootsFolderBtn);

        this.add(errorPanel, BorderLayout.NORTH);

        // Tooltip
        JLabel label = new JLabel();
        label.setFont(FontManager.getRunescapeFont());
        label.setText("Please select the folder named");
        JLabel label1 = new JLabel();
        label1.setFont(FontManager.getRunescapeFont());
        label1.setText("'loots', in .runelite directory.");
        JLabel label2 = new JLabel();
        label2.setFont(FontManager.getRunescapeFont());
        label2.setText("This will add Venator");
        JLabel label3 = new JLabel();
        label3.setFont(FontManager.getRunescapeFont());
        label3.setText("and Elder Custodian Stalker");
        JLabel label4 = new JLabel();
        label4.setFont(FontManager.getRunescapeFont());
        label4.setText("to the slayer calculation!");
        JLabel label5 = new JLabel();
        label5.setFont(FontManager.getRunescapeFont());
        label5.setText("This is just a bonus :)");

        // Help for direction to loots
        JLabel label6 = new JLabel();
        label6.setFont(FontManager.getRunescapeBoldFont());
        label6.setText("");
        JLabel label7 = new JLabel();
        label7.setFont(FontManager.getRunescapeBoldFont());
        label7.setText("How to get there");
        JLabel label8 = new JLabel();
        label8.setFont(FontManager.getRunescapeBoldFont());
        label8.setText(" -> Go to This PC");
        JLabel label9 = new JLabel();
        label9.setFont(FontManager.getRunescapeBoldFont());
        label9.setText(" -> Local Disk (C:)");
        JLabel label10 = new JLabel();
        label10.setFont(FontManager.getRunescapeBoldFont());
        label10.setText(" -> Users");
        JLabel label11 = new JLabel();
        label11.setFont(FontManager.getRunescapeBoldFont());
        label11.setText(" -> your username");
        JLabel label12 = new JLabel();
        label12.setFont(FontManager.getRunescapeBoldFont());
        label12.setText(" -> .runelite");
        JLabel label13 = new JLabel();
        label13.setFont(FontManager.getRunescapeBoldFont());
        label13.setText(" -> loots");

        this.add(label);
        this.add(label1);
        this.add(label2);
        this.add(label3);
        this.add(label4);
        this.add(label5);
        this.add(label6);
        this.add(label7);
        this.add(label8);
        this.add(label9);
        this.add(label10);
        this.add(label11);
        this.add(label12);
        this.add(label13);

        this.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        this.setOpaque(false);
        this.setPreferredSize(new Dimension(700, 400));

        this.revalidate();
        this.repaint();
    }

    private void fetchFromFolder(Client client, SlayerKillCountService slayerKillCountService, JButton findLootsFolderBtn) {
        findLootsFolderBtn.addActionListener(ev -> {
            Filepath.Chooser chooser = new Filepath.Chooser();
            chooser.setIsOpen()
                    .setAcceptsDirectories()
                    .setDialogTitle("Please find 'loots'-folder in .runelite");
            List<Filepath> files = chooser.showDialog(this);

            if (Objects.nonNull(files) && !files.isEmpty()) {
                Filepath lootsFile = files.get(0).join(String.valueOf(client.getAccountHash()));

                Filepath venator = lootsFile.join("npc", "venator.log");
                Filepath elderCustodianStalker = lootsFile.join("npc", "elder custodian stalker.log");
                int venatorCount;
                try {
                    venatorCount = Math.toIntExact(venator.openBufferedReader().lines().count());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                int elderCustodianStalkerCount;
                try {
                    elderCustodianStalkerCount = Math.toIntExact(elderCustodianStalker.openBufferedReader().lines().count());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                slayerKillCountService.upsertSlayerKc("venator", venatorCount);
                slayerKillCountService.upsertSlayerKc("elder custodian stalker", elderCustodianStalkerCount);

            }
        });
    }
}