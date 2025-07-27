import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.imageio.ImageIO;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class YouTubeDownloaderGUI extends JFrame {
    private JTextField urlField;
    private JButton downloadButton, goToYouTubeButton;
    private JTextArea outputArea;
    private JProgressBar downloadProgressBar;
    private JToggleButton themeSwitchButton;
    private boolean darkMode = true;

    private DefaultListModel<String> historyModel;
    private JList<String> historyList;
    private List<String> downloadPaths;

    // Theme Colors
    private final Color darkBg = new Color(30, 30, 40);
    private final Color lightBg = new Color(245, 245, 248);
    private final Color darkText = Color.WHITE;
    private final Color lightText = Color.BLACK;

    public YouTubeDownloaderGUI() {
        setTitle("YouTube Video Downloader");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(700, 600);
        setMinimumSize(new Dimension(480, 400));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Load icon
        try {
            Image icon = ImageIO.read(getClass().getResource("yt_icon.png"));
            setIconImage(icon);
        } catch (Exception ignored) {}

        // Top Panel
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(16, 16, 0, 16));

        JLabel urlLabel = new JLabel("YouTube URL:");
        urlLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));

        urlField = new JTextField();
        urlField.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        urlField.setMargin(new Insets(10, 10, 10, 10));

        JPanel urlPanel = new JPanel(new BorderLayout(8, 8));
        urlPanel.add(urlLabel, BorderLayout.WEST);
        urlPanel.add(urlField, BorderLayout.CENTER);

        topPanel.add(urlPanel, BorderLayout.CENTER);

        // Theme Switch
        themeSwitchButton = new JToggleButton();
        themeSwitchButton.setPreferredSize(new Dimension(60, 30));
        themeSwitchButton.setUI(new ModernSwitchUI());
        themeSwitchButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        themeSwitchButton.addActionListener(e -> switchTheme());
        topPanel.add(themeSwitchButton, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // Center Panel with buttons
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 50, 20, 50));

        downloadButton = new JButton("Download");
        styleButton(downloadButton, new Color(255, 0, 43));
        downloadButton.setFont(new Font("Segoe UI", Font.BOLD, 20));
        downloadButton.addActionListener(e -> downloadVideo());
        centerPanel.add(downloadButton);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        goToYouTubeButton = new JButton("Go to YouTube");
        styleButton(goToYouTubeButton, new Color(230, 33, 23));
        goToYouTubeButton.setFont(new Font("Segoe UI", Font.BOLD, 18));
        goToYouTubeButton.addActionListener(ev -> openYouTube());
        centerPanel.add(goToYouTubeButton);

        add(centerPanel, BorderLayout.CENTER);

        // Bottom Panel with log, progress, and history
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 16, 10));

        outputArea = new JTextArea(5, 50);
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(outputArea);
        bottomPanel.add(scrollPane, BorderLayout.CENTER);

        downloadProgressBar = new JProgressBar(0, 100);
        downloadProgressBar.setStringPainted(true);
        downloadProgressBar.setVisible(false);
        downloadProgressBar.setForeground(new Color(255, 69, 58));
        bottomPanel.add(downloadProgressBar, BorderLayout.SOUTH);

        add(bottomPanel, BorderLayout.SOUTH);

        // Download History
        historyModel = new DefaultListModel<>();
        historyList = new JList<>(historyModel);
        historyList.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        JScrollPane historyScroll = new JScrollPane(historyList);
        historyScroll.setPreferredSize(new Dimension(200, 120));
        JPanel historyPanel = new JPanel(new BorderLayout());
        historyPanel.setBorder(BorderFactory.createTitledBorder("Download History"));
        historyPanel.add(historyScroll, BorderLayout.CENTER);

        add(historyPanel, BorderLayout.EAST);

        downloadPaths = new ArrayList<>();

        applyTheme();
        setVisible(true);
    }

    private void styleButton(JButton button, Color bgColor) {
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(180, 50));
        button.setMaximumSize(new Dimension(220, 60));
        button.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    private void openYouTube() {
        try {
            Desktop.getDesktop().browse(new URI("https://www.youtube.com/"));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Unable to open browser.\n" + e.getMessage());
        }
    }

    private void downloadVideo() {
        String url = urlField.getText().trim();
        if (url.isEmpty()) {
            outputArea.setText("Please enter a YouTube URL.");
            return;
        }

        outputArea.setText("Downloading, please wait...");
        downloadProgressBar.setVisible(true);
        downloadProgressBar.setIndeterminate(true);
        downloadButton.setEnabled(false);

        new Thread(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder("python", "downloader.py", url);
                pb.redirectErrorStream(true);
                Process process = pb.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                SwingUtilities.invokeLater(() -> outputArea.setText(""));
                while ((line = reader.readLine()) != null) {
                    final String outputLine = line + "\n";
                    SwingUtilities.invokeLater(() -> outputArea.append(outputLine));
                    Integer pct = parseProgressPercent(line);
                    if (pct != null) {
                        SwingUtilities.invokeLater(() -> {
                            downloadProgressBar.setIndeterminate(false);
                            downloadProgressBar.setValue(pct);
                            downloadProgressBar.setString(pct + "%");
                        });
                    }
                }
                process.waitFor();

                SwingUtilities.invokeLater(() -> {
                    downloadProgressBar.setValue(100);
                    downloadProgressBar.setString("100%");
                    outputArea.append("\nDownload completed.");
                    addHistory(url, "Completed");
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    outputArea.setText("Error: " + ex.getMessage());
                    addHistory(url, "Error");
                });
            } finally {
                SwingUtilities.invokeLater(() -> {
                    downloadButton.setEnabled(true);
                    new Timer(2500, ev -> downloadProgressBar.setVisible(false)).start();
                });
            }
        }).start();
    }

    private void addHistory(String url, String status) {
        historyModel.addElement(status + " - " + url);
    }

    private Integer parseProgressPercent(String line) {
        int percentIdx = line.indexOf('%');
        if (percentIdx > 0) {
            int i = percentIdx - 1;
            while (i >= 0 && (Character.isDigit(line.charAt(i)) || line.charAt(i) == '.')) i--;
            i++;
            try {
                String num = line.substring(i, percentIdx);
                float f = Float.parseFloat(num);
                return Math.round(f);
            } catch (Exception ignored) {}
        }
        return null;
    }

    private void switchTheme() {
        darkMode = !darkMode;
        applyTheme();
    }

    private void applyTheme() {
        Color bg = darkMode ? darkBg : lightBg;
        Color fg = darkMode ? darkText : lightText;
        getContentPane().setBackground(bg);
        urlField.setBackground(darkMode ? new Color(40, 40, 55) : Color.WHITE);
        urlField.setForeground(fg);
        outputArea.setBackground(darkMode ? new Color(40, 40, 55) : Color.WHITE);
        outputArea.setForeground(fg);
        historyList.setBackground(darkMode ? new Color(40, 40, 55) : Color.WHITE);
        historyList.setForeground(fg);
    }

    // Custom toggle switch UI
    private static class ModernSwitchUI extends javax.swing.plaf.basic.BasicButtonUI {
        @Override
        public void paint(Graphics g, JComponent c) {
            AbstractButton b = (AbstractButton) c;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = b.getWidth(), h = b.getHeight();
            g2.setColor(b.isSelected() ? new Color(255, 69, 58) : new Color(180, 180, 180));
            g2.fillRoundRect(0, 0, w, h, h, h);

            g2.setColor(Color.WHITE);
            int knobX = b.isSelected() ? w - h + 4 : 4;
            g2.fillOval(knobX, 4, h - 8, h - 8);

            g2.dispose();
        }

        @Override
        public Dimension getPreferredSize(JComponent c) {
            return new Dimension(60, 30);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(YouTubeDownloaderGUI::new);
    }
}

// Note: The downloader.py script should be implemented separately to handle the actual downloading of YouTube videos.
// This GUI is designed to work with that script, which should be in the same directory as this Java class.
// Ensure you have the necessary Python environment and libraries installed to run the downloader script.