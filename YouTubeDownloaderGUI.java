import java.awt.*;
import java.io.*;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class YouTubeDownloaderGUI extends JFrame {
    private JTextField urlField;
    private JButton downloadButton, goToYouTubeButton;
    private JTextArea outputArea;
    private JProgressBar downloadProgressBar;
    private JToggleButton themeSwitchButton;
    private boolean darkMode = true;
    private JLabel statusLabel;

    private DefaultListModel<String> historyModel;
    private JList<String> historyList;
    private List<String> downloadPaths;

    // History file path
    private static final String HISTORY_FILE = "history.txt";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Theme Colors
    private final Color darkBg = new Color(30, 30, 40);
    private final Color lightBg = new Color(245, 245, 248);
    private final Color darkText = Color.WHITE;
    private final Color lightText = Color.BLACK;

    public YouTubeDownloaderGUI() {
        // Try to use Nimbus L&F for a modern look
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {}

        setTitle("YouTube Video Downloader");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(700, 600);
        setMinimumSize(new Dimension(480, 400));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Load icon
        try {
            // Load icon from working directory (next to the class/jar)
            File iconFile = new File("yt_icon.png");
            if (iconFile.exists()) {
                Image icon = ImageIO.read(iconFile);
                setIconImage(icon);
            }
        } catch (Exception ignored) {}

        // Header with logo and URL field
        JPanel header = new JPanel(new BorderLayout(12, 12));
        header.setBorder(new EmptyBorder(14, 14, 8, 14));

        // App title + logo
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        titlePanel.setOpaque(false);
        try {
            File iconFile = new File("yt_icon.png");
            if (iconFile.exists()) {
                Image img = ImageIO.read(iconFile).getScaledInstance(36, 36, Image.SCALE_SMOOTH);
                JLabel logo = new JLabel(new ImageIcon(img));
                titlePanel.add(logo);
            }
        } catch (Exception ignored) {}
        JLabel title = new JLabel("YouTube Downloader");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titlePanel.add(title);

        header.add(titlePanel, BorderLayout.WEST);

        // URL input
        JPanel urlPanel = new JPanel(new BorderLayout(8, 8));
        urlPanel.setOpaque(false);
        JLabel urlLabel = new JLabel("YouTube URL:");
        urlLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        urlField = new JTextField();
        urlField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        urlField.setMargin(new Insets(10, 10, 10, 10));
        urlPanel.add(urlLabel, BorderLayout.WEST);
        urlPanel.add(urlField, BorderLayout.CENTER);

        // Theme Switch
        themeSwitchButton = new JToggleButton();
        themeSwitchButton.setPreferredSize(new Dimension(60, 30));
        themeSwitchButton.setUI(new ModernSwitchUI());
        themeSwitchButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        themeSwitchButton.addActionListener(e -> switchTheme());

        JPanel rightHeader = new JPanel(new BorderLayout());
        rightHeader.setOpaque(false);
        rightHeader.add(themeSwitchButton, BorderLayout.EAST);

        header.add(urlPanel, BorderLayout.CENTER);
        header.add(rightHeader, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        // Main center panel with controls
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(12, 20, 12, 20));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 12));
        controls.setOpaque(false);

        downloadButton = new JButton("Download");
        styleButton(downloadButton, new Color(220, 20, 60));
        downloadButton.setFont(new Font("Segoe UI", Font.BOLD, 18));
        downloadButton.addActionListener(e -> downloadVideo());

        goToYouTubeButton = new JButton("Go to YouTube");
        styleButton(goToYouTubeButton, new Color(200, 30, 30));
        goToYouTubeButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        goToYouTubeButton.addActionListener(ev -> openYouTube());

        controls.add(downloadButton);
        controls.add(goToYouTubeButton);

        mainPanel.add(controls, BorderLayout.NORTH);

        add(mainPanel, BorderLayout.CENTER);

        // Bottom Panel with log and progress
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setBorder(new EmptyBorder(10, 16, 16, 16));

        outputArea = new JTextArea(6, 50);
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);
        outputArea.setOpaque(true);
        outputArea.setBorder(BorderFactory.createLineBorder(new Color(200,200,200),1,true));

        JScrollPane scrollPane = new JScrollPane(outputArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        bottomPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel progressPanel = new JPanel(new BorderLayout(8,8));
        progressPanel.setOpaque(false);
        downloadProgressBar = new JProgressBar(0, 100);
        downloadProgressBar.setStringPainted(true);
        downloadProgressBar.setVisible(false);
        downloadProgressBar.setForeground(new Color(220,20,60));

        statusLabel = new JLabel("Ready");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        progressPanel.add(statusLabel, BorderLayout.WEST);
        progressPanel.add(downloadProgressBar, BorderLayout.CENTER);
        bottomPanel.add(progressPanel, BorderLayout.SOUTH);

        add(bottomPanel, BorderLayout.SOUTH);

        // Download History (right side)
        historyModel = new DefaultListModel<>();
        historyList = new JList<>(historyModel);
        historyList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        JScrollPane historyScroll = new JScrollPane(historyList);
        historyScroll.setPreferredSize(new Dimension(240, 200));

        JPanel historyPanel = new JPanel(new BorderLayout(8,8));
        historyPanel.setBorder(BorderFactory.createTitledBorder("Download History"));
        historyPanel.add(historyScroll, BorderLayout.CENTER);

        JPanel historyButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 6));
        JButton openDownloadsBtn = new JButton("Open Downloads");
        styleButton(openDownloadsBtn, new Color(70,130,180));
        openDownloadsBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        openDownloadsBtn.addActionListener(e -> openDownloads());

        JButton clearHistoryBtn = new JButton("Clear");
        styleButton(clearHistoryBtn, new Color(150,150,150));
        clearHistoryBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        clearHistoryBtn.addActionListener(e -> {
            historyModel.clear();
            saveHistory();
        });

        historyButtons.add(openDownloadsBtn);
        historyButtons.add(clearHistoryBtn);
        historyPanel.add(historyButtons, BorderLayout.SOUTH);

        add(historyPanel, BorderLayout.EAST);

        downloadPaths = new ArrayList<>();

        applyTheme();
        loadHistory();
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
                            statusLabel.setText(pct + "%");
                        });
                    }
                }
                process.waitFor();

                SwingUtilities.invokeLater(() -> {
                    downloadProgressBar.setValue(100);
                    downloadProgressBar.setString("100%");
                    outputArea.append("\nDownload completed.");
                    statusLabel.setText("Completed");
                    addHistory(url, "Completed");
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    outputArea.setText("Error: " + ex.getMessage());
                    statusLabel.setText("Error");
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

    private void openDownloads() {
        try {
            File downloadsDir = new File(System.getProperty("user.dir"), "downloads");
            if (!downloadsDir.exists()) downloadsDir.mkdirs();
            Desktop.getDesktop().open(downloadsDir);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Unable to open downloads folder:\n" + e.getMessage());
        }
    }

    private void addHistory(String url, String status) {
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
        String historyEntry = "[" + timestamp + "] " + status + " - " + url;
        historyModel.addElement(historyEntry);
        saveHistory();
    }

    private void loadHistory() {
        File historyFile = new File(HISTORY_FILE);
        if (!historyFile.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(historyFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    historyModel.addElement(line);
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading history: " + e.getMessage());
        }
    }

    private void saveHistory() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(HISTORY_FILE))) {
            for (int i = 0; i < historyModel.getSize(); i++) {
                writer.write(historyModel.getElementAt(i));
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error saving history: " + e.getMessage());
        }
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