package me.ramazanenescik04.swd;

import javax.net.ssl.HttpsURLConnection;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.URL;
import java.nio.file.*;

public class SWDownloader extends JFrame {
	private static final long serialVersionUID = 1L;
	
	public static final String VERSION = "1.2";
	
	private JTextField urlField;
    private JButton downloadButton;
    private JButton cancelButton;
    private JLabel statusLabel;
    private JProgressBar progressBar;
    
    private boolean promptFileName = true;
    private boolean useFixedDirectory = false;
    private File fixedDirectory = new File(System.getProperty("user.home"), "İndirilenler");

    private SwingWorker<Void, Integer> worker;

    public SWDownloader() {
    	super("SitWatch Video İndirme Aracı - V" + VERSION + " - By: @ramazanenescik04");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(500, 200);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        urlField = new JTextField();
        downloadButton = new JButton("İndir");
        cancelButton = new JButton("İptal Et");
        cancelButton.setEnabled(false);

        statusLabel = new JLabel("URL gir ve indir butonuna bas.", SwingConstants.CENTER);
        statusLabel.setPreferredSize(new Dimension(500, 30)); // Görünürlüğü garantile

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);

        // Status + progress ayrı bir panelde
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.add(progressBar, BorderLayout.CENTER);
        centerPanel.add(statusLabel, BorderLayout.SOUTH);

        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.add(new JLabel("Video URL:"), BorderLayout.WEST);
        topPanel.add(urlField, BorderLayout.CENTER);
        topPanel.add(downloadButton, BorderLayout.EAST);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(cancelButton);
        
        JMenuBar menuBar = new JMenuBar();
        JMenu settingsMenu = new JMenu("Ayarlar");

        JCheckBoxMenuItem askNameItem = new JCheckBoxMenuItem("İndirme Öncesi Dosya Adı Sor", true);
        JCheckBoxMenuItem useFixedDirItem = new JCheckBoxMenuItem("Sabit Kayıt Klasörü Kullan", true);
        JMenuItem chooseDirItem = new JMenuItem("Kayıt Klasörünü Seç...");

        settingsMenu.add(askNameItem);
        settingsMenu.add(useFixedDirItem);
        settingsMenu.addSeparator();
        settingsMenu.add(chooseDirItem);
        
        JMenuItem aboutItem = new JMenuItem("Hakkında");
        
        menuBar.add(settingsMenu);
        menuBar.add(aboutItem);
        setJMenuBar(menuBar);

        add(topPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        downloadButton.addActionListener(e -> startDownload());
        cancelButton.addActionListener(e -> {
            if (worker != null && !worker.isDone()) {
                worker.cancel(true);
                statusLabel.setText("İndirme iptal edildi.");
            }
        });
        chooseDirItem.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Kayıt Klasörü Seç");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int result = chooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                fixedDirectory = chooser.getSelectedFile();
            }
        });
        aboutItem.addActionListener(e -> {
			JOptionPane.showMessageDialog(this,
					"SitWatch Video İndirme Aracı - V" + VERSION + "\n" +
					"Yapımcı: @ramazanenescik04\n" +
					"Bu araç, SitWatch videolarını indirmenizi sağlar.",
					"Hakkında", JOptionPane.INFORMATION_MESSAGE);
		});
        
        askNameItem.addActionListener(e -> promptFileName = askNameItem.isSelected());
		useFixedDirItem.addActionListener(e -> useFixedDirectory = useFixedDirItem.isSelected());
    }

    private void startDownload() {
        String urlString = urlField.getText().trim();
        if (urlString.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Lütfen bir URL girin.", "Hata", JOptionPane.ERROR_MESSAGE);
            return;
        }

        downloadButton.setEnabled(false);
        cancelButton.setEnabled(true);
        progressBar.setValue(0);
        statusLabel.setText("Dosya boyutu kontrol ediliyor...");

        worker = new SwingWorker<Void, Integer>() {
            private Path outputPath;

            @Override
            protected Void doInBackground() {
                try {
                    URL url = SWApi.getVideoURI(SWApi.getVideoID(urlString)).toURL();
                    HttpsURLConnection headConn = (HttpsURLConnection) url.openConnection();
                    headConn.setRequestProperty("User-Agent", "SimpleVideoDownloader");
                    headConn.setRequestMethod("HEAD");
                    headConn.connect();

                    int contentLength = headConn.getContentLength();
                    headConn.disconnect();

                    String sizeText = (contentLength > 0)
                            ? String.format("Dosya boyutu: %.2f MB", contentLength / 1024.0 / 1024.0)
                            : "Dosya boyutu alınamadı.";
                    SwingUtilities.invokeLater(() -> statusLabel.setText(sizeText + " İndiriliyor..."));

                    HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                    conn.setRequestProperty("User-Agent", "SimpleVideoDownloader");
                    conn.connect();

                    String fileName = "";
                    String disposition = conn.getHeaderField("Content-Disposition");
                    if (disposition != null && disposition.contains("filename=")) {
                        int index = disposition.indexOf("filename=") + 9;
                        fileName = disposition.substring(index).replace("\"", "");
                    } else {
                        fileName = Paths.get(url.getPath()).getFileName().toString();
                    }
                    if (fileName.isEmpty()) fileName = "indirilen_video";
                    
                    fileName += ".mp4"; // Varsayılan olarak mp4 uzantısı ekle
                    
                    // Dosya yolu oluşturulurken:
                    if (promptFileName) {
                        String name = JOptionPane.showInputDialog("Dosya adını girin:", fileName);
                        if (name != null && !name.trim().isEmpty()) fileName = name.trim();
                    }

                    outputPath = useFixedDirectory
                        ? new File(fixedDirectory, fileName).toPath()
                        : Paths.get(fileName);

                    try (InputStream in = conn.getInputStream();
                    	OutputStream out = Files.newOutputStream(outputPath, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        long totalRead = 0;

                        while ((bytesRead = in.read(buffer)) != -1) {
                            if (isCancelled()) throw new InterruptedException("İptal edildi");

                            out.write(buffer, 0, bytesRead);
                            totalRead += bytesRead;

                            if (contentLength > 0) {
                                int progress = (int) ((totalRead * 100) / contentLength);
                                publish(progress);
                                setProgress(progress);
                            }

                            String mbDownloaded = String.format("%.2f MB", totalRead / 1024.0 / 1024.0);
                            SwingUtilities.invokeLater(() -> statusLabel.setText("İndiriliyor... " + mbDownloaded + " / "  + sizeText));
                        }
                    }

                } catch (InterruptedException e) {
                    // iptal edildiğinde dosyayı sil
                    if (outputPath != null && Files.exists(outputPath)) {
                        try {
                            Files.delete(outputPath);
                        } catch (IOException ignored) {}
                    }
                    SwingUtilities.invokeLater(() -> statusLabel.setText("İndirme iptal edildi."));
                } catch (Exception ex) {
                	ex.printStackTrace();
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(SWDownloader.this,
                                "İndirme hatası:\n" + ex.getMessage(),
                                "Hata", JOptionPane.ERROR_MESSAGE);
                        statusLabel.setText("Hata oluştu.");
                    });
                }
                return null;
            }

            @Override
            protected void process(java.util.List<Integer> chunks) {
                int lastProgress = chunks.get(chunks.size() - 1);
                progressBar.setValue(lastProgress);
            }

            @Override
            protected void done() {
                downloadButton.setEnabled(true);
                cancelButton.setEnabled(false);

                if (!isCancelled()) {
                    progressBar.setValue(100);
                    statusLabel.setText("İndirme tamamlandı.");
                }
            }
        };

        worker.execute();
    }


	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			SWDownloader frame = new SWDownloader();
			frame.setVisible(true);
		});
	}

}
