package me.ramazanenescik04.swd;

import javax.net.ssl.HttpsURLConnection;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.URL;
import java.nio.file.*;

public class SWDownloader extends JFrame {
	private static final long serialVersionUID = 1L;
	
	private JTextField urlField;
    private JButton downloadButton;
    private JLabel statusLabel;
    private JProgressBar progressBar;

    public SWDownloader() {
        super("SitWatch Video İndirme Aracı - V1.1 - By: @ramazanenescik04");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(500, 140);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        urlField = new JTextField();
        downloadButton = new JButton("İndir");
        statusLabel = new JLabel("URL gir ve indir butonuna bas.", SwingConstants.CENTER);

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);

        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.add(new JLabel("Video URL:"), BorderLayout.WEST);
        topPanel.add(urlField, BorderLayout.CENTER);
        topPanel.add(downloadButton, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);
        add(progressBar, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        downloadButton.addActionListener(e -> startDownload());
    }

    private void startDownload() {
        String urlString = urlField.getText().trim();
        if (urlString.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Lütfen bir URL girin.", "Hata", JOptionPane.ERROR_MESSAGE);
            return;
        }

        downloadButton.setEnabled(false);
        statusLabel.setText("İndiriliyor...");
        progressBar.setValue(0);

        SwingWorker<Void, Integer> worker = new SwingWorker<Void, Integer>() {
        	int contentLength = -1;
            @Override
            protected Void doInBackground() {
                try {
                    URL url = SWApi.getVideoURI(SWApi.getVideoID(urlString)).toURL();
                    HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                    connection.setRequestProperty("User-Agent", "SimpleVideoDownloader");
                    connection.setRequestMethod("HEAD"); // Sadece header al
                    connection.connect();

                    int responseCode = connection.getResponseCode();
                    if (responseCode / 100 != 2) {
                        throw new IOException("HTTP Hatası: " + responseCode);
                    }

                    contentLength = connection.getContentLength();
                    connection.disconnect();

                    String sizeText;
                    if (contentLength > 0) {
                        sizeText = String.format("Dosya boyutu: %.2f MB", contentLength / 1024.0 / 1024.0);
                    } else {
                        sizeText = "Dosya boyutu alınamadı.";
                    }

                    SwingUtilities.invokeLater(() -> statusLabel.setText(sizeText + " İndiriliyor..."));

                    // Şimdi GET ile asıl indirme yapacağız
                    connection = (HttpsURLConnection) url.openConnection();
                    connection.setRequestProperty("User-Agent", "SimpleVideoDownloader");
                    connection.connect();

                    int responseCode2 = connection.getResponseCode();
                    if (responseCode2 / 100 != 2) {
                        throw new IOException("HTTP Hatası: " + responseCode2);
                    }

                    String fileName = "";
                    String disposition = connection.getHeaderField("Content-Disposition");
                    if (disposition != null && disposition.indexOf("filename=") != -1) {
                        int index = disposition.indexOf("filename=") + 9;
                        fileName = disposition.substring(index).replace("\"", "");
                    } else {
                        fileName = Paths.get(url.getPath()).getFileName().toString();
                    }
                    if (fileName.isEmpty()) fileName = "indirilen_video";
                    
                    fileName += ".mp4"; // Varsayılan uzantı olarak mp4 ekleyelim

                    Path outputPath = Paths.get(fileName);

                    try (InputStream in = connection.getInputStream();
                         OutputStream out = Files.newOutputStream(outputPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        long totalRead = 0;

                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                            totalRead += bytesRead;

                            if (contentLength > 0) {
                                int progress = (int) ((totalRead * 100) / contentLength);
                                publish(progress);
                                setProgress(progress);
                            }

                            String mbDownloaded = String.format("%.2f MB", totalRead / 1024.0 / 1024.0);
                            SwingUtilities.invokeLater(() -> statusLabel.setText("İndiriliyor... " + mbDownloaded + " / " + sizeText));
                        }
                    }

                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(SWDownloader.this,
                                "İndirme hatası:\n" + ex.getMessage(),
                                "Hata", JOptionPane.ERROR_MESSAGE);
                        statusLabel.setText("Hata oluştu.");
                        downloadButton.setEnabled(true);
                        progressBar.setValue(0);
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
                statusLabel.setText("İndirme tamamlandı.");
                progressBar.setValue(100);
                downloadButton.setEnabled(true);
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
