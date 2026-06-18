import java.awt.*;
import java.io.*;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class YouTubeDownloaderGUI extends JFrame {
    private JTextField urlField;
    private JButton downloadButton, goToYouTubeButton;
    private JButton pauseButton, cancelButton;
    private JTextArea outputArea;
    private JProgressBar downloadProgressBar;
    private JToggleButton themeSwitchButton;
    private JComboBox<String> formatComboBox;  // Format/Quality selector
    private boolean darkMode = true;
    private JLabel statusLabel;

    private DefaultListModel<String> historyModel;
    private JList<String> historyList;
    private List<String> downloadPaths;
    private volatile Process currentProcess = null;
    private volatile boolean isPaused = false;
    private volatile boolean isDownloading = false;

    // History file path
    private static final String HISTORY_FILE = "history.txt";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Theme Colors
    private final Color darkBg = new Color(30, 30, 40);
    private final Color lightBg = new Color(245, 245, 248);
    private final Color darkText = Color.WHITE;
    private final Color lightText = Color.BLACK;

    private static final Pattern YOUTUBE_URL_PATTERN = Pattern.compile(
            "^(https?://)?(www\\.|m\\.)?(youtube\\.com|youtu\\.be|youtube-nocookie\\.com)/"
                    + "(watch\\?v=[^&\\s]+|embed/[^\\s]+|shorts/[^\\s]+|playlist\\?list=[^&\\s]+|[^\\s]+)$",
            Pattern.CASE_INSENSITIVE);

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

        // Format/Quality Selection Panel
        JPanel formatPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        formatPanel.setOpaque(false);
        JLabel formatLabel = new JLabel("Quality/Format:");
        formatLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        
        String[] formatOptions = {
            "Best Quality (Recommended)",
            "Worst Quality (Smallest File)",
            "Video Only (No Audio)",
            "Audio Only (MP3)",
            "1080p (Full HD)",
            "720p (HD)",
            "480p (SD)",
            "360p (Low)",
            "240p (Very Low)",
            "144p (Minimum)"
        };
        
        formatComboBox = new JComboBox<>(formatOptions);
        formatComboBox.setSelectedIndex(0);  // Default to best quality
        formatComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        formatComboBox.setPreferredSize(new Dimension(240, 32));
        
        formatPanel.add(formatLabel);
        formatPanel.add(formatComboBox);

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

        // Pause / Resume button
        pauseButton = new JButton("Pause");
        styleButton(pauseButton, new Color(180, 180, 180));
        pauseButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        pauseButton.addActionListener(ev -> pauseOrResume());
        pauseButton.setEnabled(false);

        // Cancel button
        cancelButton = new JButton("Cancel");
        styleButton(cancelButton, new Color(120, 120, 120));
        cancelButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        cancelButton.addActionListener(ev -> cancelDownload());
        cancelButton.setEnabled(false);

        controls.add(downloadButton);
        controls.add(pauseButton);
        controls.add(cancelButton);
        controls.add(goToYouTubeButton);

        mainPanel.add(formatPanel, BorderLayout.NORTH);
        mainPanel.add(controls, BorderLayout.CENTER);

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

    private String mapFormatUIToParam(String formatUI) {
        /**
         * Maps the UI format selection to the parameter passed to downloader.py
         */
        switch (formatUI) {
            case "Best Quality (Recommended)":
                return "best";
            case "Worst Quality (Smallest File)":
                return "worst";
            case "Video Only (No Audio)":
                return "video-only";
            case "Audio Only (MP3)":
                return "audio-only";
            case "1080p (Full HD)":
                return "1080p";
            case "720p (HD)":
                return "720p";
            case "480p (SD)":
                return "480p";
            case "360p (Low)":
                return "360p";
            case "240p (Very Low)":
                return "240p";
            case "144p (Minimum)":
                return "144p";
            default:
                return "best";
        }
    }

    private void downloadVideo() {
        String url = urlField.getText().trim();
        if (url.isEmpty()) {
            outputArea.setText("Please enter a YouTube URL.");
            return;
        }

        if (!isValidYouTubeURL(url)) {
            outputArea.setText("Invalid YouTube URL. Please enter a valid URL like:\n" +
                    "  • https://www.youtube.com/watch?v=VIDEO_ID\n" +
                    "  • https://youtu.be/VIDEO_ID\n" +
                    "  • https://www.youtube.com/playlist?list=PLAYLIST_ID");
            statusLabel.setText("Invalid URL");
            return;
        }

        // Get the selected format
        String selectedFormatUI = (String) formatComboBox.getSelectedItem();
        String formatParam = mapFormatUIToParam(selectedFormatUI);

        outputArea.setText("Downloading " + selectedFormatUI.toLowerCase() + ", please wait...");
        downloadProgressBar.setVisible(true);
        downloadProgressBar.setIndeterminate(true);
        downloadButton.setEnabled(false);
        formatComboBox.setEnabled(false);

        // If we were paused, this acts as a resume (yt_dlp will continue due to continuedl=True)
        isPaused = false;

        new Thread(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder("python", "downloader.py", url, formatParam);
                pb.redirectErrorStream(true);
                Process process = pb.start();

                // Track the running process so pause/cancel can control it
                currentProcess = process;
                isDownloading = true;

                SwingUtilities.invokeLater(() -> {
                    pauseButton.setEnabled(true);
                    cancelButton.setEnabled(true);
                    downloadButton.setEnabled(false);
                    downloadButton.setText("Downloading...");
                });

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

                // If download finished normally and wasn't cancelled
                if (!isPaused) {
                    SwingUtilities.invokeLater(() -> {
                        downloadProgressBar.setValue(100);
                        downloadProgressBar.setString("100%");
                        outputArea.append("\nDownload completed.");
                        statusLabel.setText("Completed");
                        addHistory(url, "Completed - " + selectedFormatUI);
                    });
                }
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    outputArea.setText("Error: " + ex.getMessage());
                    statusLabel.setText("Error");
                    addHistory(url, "Error");
                });
            } finally {
                // Clear process state and reset UI
                currentProcess = null;
                isDownloading = false;
                isPaused = false;
                SwingUtilities.invokeLater(() -> {
                    downloadButton.setEnabled(true);
                    downloadButton.setText("Download");
                    formatComboBox.setEnabled(true);
                    pauseButton.setEnabled(false);
                    cancelButton.setEnabled(false);
                    new Timer(2500, ev -> downloadProgressBar.setVisible(false)).start();
                });
            }
        }).start();
    }

    private void pauseOrResume() {
        if (isDownloading && currentProcess != null) {
            // Pause: kill the process. yt_dlp supports resuming via continuedl=True
            pauseDownload();
        } else if (!isDownloading && !isPaused) {
            // Nothing to resume
        } else if (isPaused) {
            // Resume by triggering downloadVideo() which will start a new process and resume
            isPaused = false;
            downloadVideo();
        }
    }

    private void pauseDownload() {
        if (currentProcess != null) {
            try {
                currentProcess.destroy();
            } catch (Exception ignored) {}
            isPaused = true;
            isDownloading = false;
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("Paused");
                pauseButton.setEnabled(false);
                downloadButton.setEnabled(true);
                downloadButton.setText("Resume");
                cancelButton.setEnabled(true);
            });
        }
    }

    private void cancelDownload() {
        // Kill running process if any
        if (currentProcess != null) {
            try {
                currentProcess.destroyForcibly();
            } catch (Exception ignored) {}
        }
        isPaused = false;
        isDownloading = false;

        // Remove partial download files (.part) in downloads directory
        try {
            File downloadsDir = new File(System.getProperty("user.dir"), "downloads");
            if (downloadsDir.exists() && downloadsDir.isDirectory()) {
                File[] parts = downloadsDir.listFiles((dir, name) -> name.endsWith(".part") || name.endsWith(".part.~best~"));
                if (parts != null) {
                    for (File f : parts) {
                        try { f.delete(); } catch (Exception ignored) {}
                    }
                }
            }
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            outputArea.append("\nDownload cancelled.");
            statusLabel.setText("Cancelled");
            addHistory(urlField.getText().trim(), "Cancelled");
            downloadButton.setEnabled(true);
            downloadButton.setText("Download");
            pauseButton.setEnabled(false);
            cancelButton.setEnabled(false);
            new Timer(1500, ev -> downloadProgressBar.setVisible(false)).start();
        });
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

    private boolean isValidYouTubeURL(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }

        try {
            Matcher matcher = YOUTUBE_URL_PATTERN.matcher(url.trim());
            return matcher.matches();
        } catch (Exception e) {
            return false;
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
        
        // Apply theme to format combobox
        formatComboBox.setBackground(darkMode ? new Color(40, 40, 55) : Color.WHITE);
        formatComboBox.setForeground(fg);
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