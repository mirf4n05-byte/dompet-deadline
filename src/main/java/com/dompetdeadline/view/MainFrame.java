package com.dompetdeadline.view;

import com.dompetdeadline.model.Kategori;
import com.dompetdeadline.model.Transaksi;
import com.dompetdeadline.model.Tugas;
import com.dompetdeadline.repository.KategoriRepository;
import com.dompetdeadline.repository.TransaksiRepository;
import com.dompetdeadline.repository.TugasRepository;
import com.dompetdeadline.service.DompetService;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainFrame extends JFrame {
    private final KategoriRepository kategoriRepo = new KategoriRepository();
    private final TugasRepository tugasRepo = new TugasRepository();
    private final TransaksiRepository transaksiRepo = new TransaksiRepository();
    private final DompetService service = new DompetService();

    private final DashboardPanel dashboardPanel;
    private final TransaksiPanel transaksiPanel;
    private final TugasPanel tugasPanel;
    private final JTabbedPane tabbedPane = new JTabbedPane();

    private List<Transaksi> currentTransaksi = List.of();
    private List<Tugas> currentTugas = List.of();
    private final Set<Integer> tugasSudahDinotifikasi = new HashSet<>();
    private TrayIcon trayIcon;

    public MainFrame() {
        setTitle("Dompet Deadline - Pengatur Keuangan & Tenggat Tugas");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(960, 680);
        setMinimumSize(new Dimension(800, 580));
        setLocationRelativeTo(null);

        dashboardPanel = new DashboardPanel(service);
        transaksiPanel = new TransaksiPanel(transaksiRepo, kategoriRepo, this::muatUlangDataAsync,
                this::prosesEksporCsv);
        tugasPanel = new TugasPanel(tugasRepo, this::muatUlangDataAsync);

        tabbedPane.addTab("Dashboard", dashboardPanel);
        tabbedPane.addTab("Transaksi Keuangan", transaksiPanel);
        tabbedPane.addTab("Daftar Tugas", tugasPanel);
        add(tabbedPane, BorderLayout.CENTER);

        tabbedPane.addChangeListener(e -> perbaruiDefaultButton());
        perbaruiDefaultButton();

        setupMenuBar();
        setupSystemTray();
        mulaiTimerPengingat();
        muatUlangDataAsync();
    }

    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu menuFile = new JMenu("File");
        JMenuItem itemEkspor = new JMenuItem("Ekspor Transaksi ke CSV...");
        JMenuItem itemRefresh = new JMenuItem("Muat Ulang Data");
        JMenuItem itemKeluar = new JMenuItem("Keluar");

        itemEkspor.addActionListener(e -> prosesEksporCsv());
        itemRefresh.addActionListener(e -> muatUlangDataAsync());
        itemKeluar.addActionListener(e -> dispose());

        menuFile.add(itemEkspor);
        menuFile.add(itemRefresh);
        menuFile.addSeparator();
        menuFile.add(itemKeluar);

        JMenu menuBantuan = new JMenu("Bantuan");
        JMenuItem itemTentang = new JMenuItem("Tentang Aplikasi");
        itemTentang.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Dompet Deadline v1.0.0\nAplikasi Manajemen Keuangan & Tenggat Tugas Mahasiswa.",
                "Tentang Dompet Deadline", JOptionPane.INFORMATION_MESSAGE));
        menuBantuan.add(itemTentang);

        menuBar.add(menuFile);
        menuBar.add(menuBantuan);
        setJMenuBar(menuBar);
    }

    private void perbaruiDefaultButton() {
        int idx = tabbedPane.getSelectedIndex();
        if (idx == 1) {
            getRootPane().setDefaultButton(transaksiPanel.getDefaultButton());
        } else if (idx == 2) {
            getRootPane().setDefaultButton(tugasPanel.getDefaultButton());
        } else {
            getRootPane().setDefaultButton(null);
        }
    }

    public void muatUlangDataAsync() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private List<Kategori> kategoriList;
            private List<Tugas> tugasList;
            private List<Transaksi> transaksiList;

            @Override
            protected Void doInBackground() throws Exception {
                kategoriList = kategoriRepo.findAll();
                tugasList = tugasRepo.findAllWithTotalBiaya();
                transaksiList = transaksiRepo.findAll();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    currentTugas = tugasList;
                    currentTransaksi = transaksiList;
                    dashboardPanel.updateData(transaksiList, tugasList);
                    transaksiPanel.updateData(transaksiList, kategoriList, tugasList);
                    tugasPanel.setDaftarTugas(tugasList);
                    cekPengingatTenggat();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(MainFrame.this,
                            "Gagal memuat data database: " + ex.getMessage(),
                            "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void setupSystemTray() {
        if (!SystemTray.isSupported())
            return;
        try {
            SystemTray tray = SystemTray.getSystemTray();
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = img.createGraphics();
            g2.setColor(new Color(52, 120, 246));
            g2.fillOval(1, 1, 14, 14);
            g2.dispose();

            trayIcon = new TrayIcon(img, "Dompet Deadline");
            trayIcon.setImageAutoSize(true);
            tray.add(trayIcon);
        } catch (Exception ignored) {
            trayIcon = null;
        }
    }

    private void mulaiTimerPengingat() {
        Timer timer = new Timer(60_000, e -> cekPengingatTenggat());
        timer.setInitialDelay(3_000);
        timer.start();
    }

    private void cekPengingatTenggat() {
        LocalDateTime sekarang = LocalDateTime.now();
        for (Tugas t : currentTugas) {
            if (!tugasSudahDinotifikasi.contains(t.getId()) && service.isTugasMendesak(t, sekarang)) {
                tugasSudahDinotifikasi.add(t.getId());
                String pesan = "Tugas \"" + t.getJudul() + "\" (" + t.getMataKuliah() + ") jatuh tempo pada "
                        + t.getTenggat().format(TugasRepository.FORMATTER) + "!";
                if (trayIcon != null) {
                    trayIcon.displayMessage("Pengingat Tenggat < 24 Jam", pesan, TrayIcon.MessageType.WARNING);
                } else {
                    JOptionPane.showMessageDialog(this, pesan, "Pengingat Tenggat < 24 Jam",
                            JOptionPane.WARNING_MESSAGE);
                }
            }
        }
    }

    private void prosesEksporCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Simpan Laporan Transaksi CSV");
        chooser.setSelectedFile(new File("transaksi_dompet_deadline.csv"));
        chooser.setFileFilter(new FileNameExtensionFilter("Berkas CSV (*.csv)", "csv"));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".csv")) {
                file = new File(file.getAbsolutePath() + ".csv");
            }
            try {
                service.eksporCsv(currentTransaksi, file);
                JOptionPane.showMessageDialog(this, "Data berhasil diekspor ke:\n" + file.getAbsolutePath());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Gagal ekspor CSV: " + ex.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}