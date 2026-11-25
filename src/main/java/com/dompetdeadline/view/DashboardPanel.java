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

public class DashboardPanel extends JPanel {
    private final DompetService service;
    private final JLabel lblSisaUang = new JLabel("Rp 0");
    private final JLabel lblTotalBiayaAktif = new JLabel("Rp 0");
    private final JComboBox<YearMonth> cbBulan = new JComboBox<>();
    private final DefaultTableModel modelTenggat;
    private final GrafikBatangPanel grafikPanel = new GrafikBatangPanel();

    private List<Transaksi> cacheTransaksi = List.of();
    private List<Tugas> cacheTugas = List.of();

    public DashboardPanel(DompetService service) {
        this.service = service;
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        // Isi pilihan 12 bulan terakhir
        YearMonth now = YearMonth.now();
        for (int i = 0; i < 12; i++) {
            cbBulan.addItem(now.minusMonths(i));
        }
        cbBulan.addActionListener(e -> perbaruiGrafikDanSaldo());

        // Panel Atas: Filter Bulan & 2 Kartu Ringkasan
        JPanel topWrapper = new JPanel(new BorderLayout(8, 8));
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterBar.add(new JLabel("Periode Bulan:"));
        filterBar.add(cbBulan);
        topWrapper.add(filterBar, BorderLayout.NORTH);

        JPanel cardsPanel = new JPanel(new GridLayout(1, 2, 12, 0));
        cardsPanel.add(
                buatKartu("Sisa Uang Bulan Dipilih", lblSisaUang, new Color(232, 245, 233), new Color(27, 94, 32)));
        cardsPanel.add(buatKartu("Total Biaya Tugas Aktif", lblTotalBiayaAktif, new Color(227, 242, 253),
                new Color(13, 71, 161)));
        topWrapper.add(cardsPanel, BorderLayout.CENTER);
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
        scrollTenggat.setPreferredSize(new Dimension(600, 115));
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
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));
        JLabel lblTitle = new JLabel(judul);
        lblTitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lblNilai.setFont(new Font("SansSerif", Font.BOLD, 20));
        lblNilai.setForeground(fg);
        card.add(lblTitle, BorderLayout.NORTH);
        card.add(lblNilai, BorderLayout.CENTER);
        return card;
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
        grafikPanel.setData(service.hitungPengeluaranPerKategori(cacheTransaksi, bulan));
    }
}