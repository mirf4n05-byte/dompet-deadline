package com.dompetdeadline.view;

import com.dompetdeadline.model.Transaksi;
import com.dompetdeadline.model.Tugas;
import com.dompetdeadline.repository.TugasRepository;
import com.dompetdeadline.service.DompetService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.YearMonth;
import java.util.List;
import java.util.prefs.Preferences;

public class DashboardPanel extends JPanel {
    private final DompetService service;
    private final Preferences prefs = Preferences.userNodeForPackage(DashboardPanel.class);

    private final JLabel lblSisaUang = new JLabel("Rp 0");
    private final JLabel lblTotalBiayaAktif = new JLabel("Rp 0");
    private final JLabel lblSisaAnggaran = new JLabel("Belum diatur");
    private final JLabel lblDetailAnggaran = new JLabel("Klik 'Atur Dana Bulanan' untuk menetapkan batas pengeluaran.");
    private final JProgressBar barAnggaran = new JProgressBar(0, 100);

    private final JComboBox<YearMonth> cbBulan = new JComboBox<>();
    private final DefaultTableModel modelTenggat;
    private final GrafikBatangPanel grafikPanel = new GrafikBatangPanel();

    private List<Transaksi> cacheTransaksi = List.of();
    private List<Tugas> cacheTugas = List.of();

    public DashboardPanel(DompetService service) {
        this.service = service;
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        // Pilihan 12 bulan terakhir
        YearMonth now = YearMonth.now();
        for (int i = 0; i < 12; i++) {
            cbBulan.addItem(now.minusMonths(i));
        }
        cbBulan.addActionListener(e -> perbaruiGrafikDanSaldo());

        // Panel Atas: Filter Bulan, Tombol Atur Dana, & Kartu Ringkasan
        JPanel topWrapper = new JPanel(new BorderLayout(8, 8));

        JPanel filterBar = new JPanel(new BorderLayout());
        JPanel leftFilter = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftFilter.add(new JLabel("Periode Bulan:"));
        leftFilter.add(cbBulan);

        JPanel rightFilter = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnAturDana = new JButton("Atur Dana Bulanan");
        btnAturDana.addActionListener(e -> dialogAturDanaBulanan());
        rightFilter.add(btnAturDana);

        filterBar.add(leftFilter, BorderLayout.WEST);
        filterBar.add(rightFilter, BorderLayout.EAST);
        topWrapper.add(filterBar, BorderLayout.NORTH);

        JPanel cardsPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        cardsPanel.add(
                buatKartu("Sisa Uang (Masuk - Keluar)", lblSisaUang, new Color(232, 245, 233), new Color(27, 94, 32)));
        cardsPanel
                .add(buatKartu("Sisa Dana Bulanan", lblSisaAnggaran, new Color(255, 248, 225), new Color(230, 81, 0)));
        cardsPanel.add(buatKartu("Total Biaya Tugas Aktif", lblTotalBiayaAktif, new Color(227, 242, 253),
                new Color(13, 71, 161)));
        topWrapper.add(cardsPanel, BorderLayout.CENTER);

        // Bar Pemakaian Dana Bulanan
        JPanel budgetPanel = new JPanel(new BorderLayout(6, 4));
        budgetPanel.setBorder(BorderFactory.createEmptyBorder(4, 2, 0, 2));
        barAnggaran.setStringPainted(true);
        barAnggaran.setPreferredSize(new Dimension(600, 20));
        lblDetailAnggaran.setFont(new Font("SansSerif", Font.PLAIN, 12));
        budgetPanel.add(lblDetailAnggaran, BorderLayout.NORTH);
        budgetPanel.add(barAnggaran, BorderLayout.CENTER);
        topWrapper.add(budgetPanel, BorderLayout.SOUTH);

        add(topWrapper, BorderLayout.NORTH);

        // Panel Tengah: 3 Tenggat Terdekat
        JPanel tengahPanel = new JPanel(new BorderLayout(6, 6));
        tengahPanel.setBorder(BorderFactory.createTitledBorder("3 Tenggat Terdekat (Belum Selesai)"));
        modelTenggat = new DefaultTableModel(
                new String[] { "Judul Tugas", "Mata Kuliah", "Tenggat", "Prioritas", "Biaya Terkait" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable tblTenggat = new JTable(modelTenggat);
        tblTenggat.setRowHeight(24);
        JScrollPane scrollTenggat = new JScrollPane(tblTenggat);
        scrollTenggat.setPreferredSize(new Dimension(600, 105));
        tengahPanel.add(scrollTenggat, BorderLayout.CENTER);
        add(tengahPanel, BorderLayout.CENTER);

        // Panel Bawah: Grafik Batang Pengeluaran per Kategori
        JPanel bawahPanel = new JPanel(new BorderLayout(6, 6));
        bawahPanel.setBorder(BorderFactory.createTitledBorder("Grafik Pengeluaran per Kategori"));
        bawahPanel.add(grafikPanel, BorderLayout.CENTER);
        add(bawahPanel, BorderLayout.SOUTH);
    }

    private JPanel buatKartu(String judul, JLabel lblNilai, Color bg, Color fg) {
        JPanel card = new JPanel(new BorderLayout(4, 6));
        card.setBackground(bg);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(fg.brighter(), 1),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        JLabel lblTitle = new JLabel(judul);
        lblTitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblNilai.setFont(new Font("SansSerif", Font.BOLD, 18));
        lblNilai.setForeground(fg);
        card.add(lblTitle, BorderLayout.NORTH);
        card.add(lblNilai, BorderLayout.CENTER);
        return card;
    }

    private void dialogAturDanaBulanan() {
        YearMonth bulan = (YearMonth) cbBulan.getSelectedItem();
        if (bulan == null)
            return;

        long saatIni = prefs.getLong("dana_" + bulan, 0L);
        String input = JOptionPane.showInputDialog(this,
                "Masukkan target dana/anggaran bulan " + bulan
                        + " (Rupiah):\n(Isi 0 untuk menonaktifkan batas anggaran)",
                saatIni > 0 ? String.valueOf(saatIni) : "1000000");

        if (input == null)
            return;
        try {
            long nominal = Long.parseLong(input.trim().replace(".", "").replace(",", ""));
            if (nominal < 0)
                throw new NumberFormatException();
            prefs.putLong("dana_" + bulan, nominal);
            perbaruiGrafikDanSaldo();
            JOptionPane.showMessageDialog(this, "Dana bulanan untuk " + bulan + " berhasil disimpan.");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Nominal tidak valid. Masukkan angka bulat positif (contoh: 1000000).",
                    "Validasi Dana Bulanan", JOptionPane.WARNING_MESSAGE);
        }
    }

    public void updateData(List<Transaksi> transaksiList, List<Tugas> tugasList) {
        this.cacheTransaksi = transaksiList;
        this.cacheTugas = tugasList;

        lblTotalBiayaAktif.setText(FormatUtil.rupiah(service.hitungTotalBiayaTugasAktif(tugasList)));

        modelTenggat.setRowCount(0);
        for (Tugas t : service.getTigaTenggatTerdekat(tugasList)) {
            modelTenggat.addRow(new Object[] {
                    t.getJudul(),
                    t.getMataKuliah(),
                    t.getTenggat().format(TugasRepository.FORMATTER),
                    t.getPrioritas(),
                    FormatUtil.rupiah(t.getTotalBiaya())
            });
        }
        perbaruiGrafikDanSaldo();
    }

    private void perbaruiGrafikDanSaldo() {
        YearMonth bulan = (YearMonth) cbBulan.getSelectedItem();
        if (bulan == null)
            return;

        long sisa = service.hitungSisaUangBulan(cacheTransaksi, bulan);
        lblSisaUang.setText(FormatUtil.rupiah(sisa));
        lblSisaUang.setForeground(sisa < 0 ? new Color(183, 28, 28) : new Color(27, 94, 32));

        long danaBulanan = prefs.getLong("dana_" + bulan, 0L);
        long terpakai = service.hitungTotalPengeluaranBulan(cacheTransaksi, bulan);

        if (danaBulanan <= 0) {
            lblSisaAnggaran.setText("Belum diatur");
            lblSisaAnggaran.setForeground(new Color(100, 105, 115));
            lblDetailAnggaran.setText(
                    "Dana bulanan belum diatur (Total pengeluaran bulan ini: " + FormatUtil.rupiah(terpakai) + ")");
            barAnggaran.setValue(0);
            barAnggaran.setString("Belum diatur");
            barAnggaran.setForeground(new Color(120, 130, 140));
        } else {
            long sisaDana = danaBulanan - terpakai;
            int persen = (int) Math.min(100, Math.round((double) terpakai / danaBulanan * 100));
            lblSisaAnggaran.setText(FormatUtil.rupiah(sisaDana));
            lblSisaAnggaran.setForeground(sisaDana < 0 ? new Color(183, 28, 28) : new Color(230, 81, 0));

            lblDetailAnggaran.setText("Pemakaian Dana Bulanan: " + FormatUtil.rupiah(terpakai)
                    + " dari " + FormatUtil.rupiah(danaBulanan));
            barAnggaran.setValue(persen);
            barAnggaran.setString(persen + "% terpakai" + (sisaDana < 0 ? " (Melebihi anggaran!)" : ""));

            if (terpakai > danaBulanan) {
                barAnggaran.setForeground(new Color(211, 47, 47));
            } else if (persen >= 75) {
                barAnggaran.setForeground(new Color(245, 124, 0));
            } else {
                barAnggaran.setForeground(new Color(46, 125, 50));
            }
        }

        grafikPanel.setData(service.hitungPengeluaranPerKategori(cacheTransaksi, bulan));
    }
}