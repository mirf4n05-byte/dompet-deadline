package com.dompetdeadline.view;

import com.dompetdeadline.model.Kategori;
import com.dompetdeadline.model.Transaksi;
import com.dompetdeadline.model.Tugas;
import com.dompetdeadline.repository.KategoriRepository;
import com.dompetdeadline.repository.TransaksiRepository;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class TransaksiPanel extends JPanel {
    private final TransaksiRepository transaksiRepo;
    private final KategoriRepository kategoriRepo;
    private final Runnable onDataChanged;
    private final Runnable onEksporCsv;

    private final JTextField txtTanggal = new JTextField(12);
    private final JTextField txtJumlah = new JTextField(12);
    private final JComboBox<Kategori> cbKategori = new JComboBox<>();
    private final JComboBox<TugasOption> cbTugas = new JComboBox<>();
    private final JTextField txtCatatan = new JTextField(20);
    private final JButton btnSimpan = new JButton("Simpan Transaksi");
    private final JButton btnBatal = new JButton("Batal");

    private final DefaultTableModel tableModel;
    private final JTable table;
    private List<Transaksi> daftarTransaksi = new ArrayList<>();
    private Integer editingId = null;

    private static class TugasOption {
        final Tugas tugas;

        TugasOption(Tugas tugas) {
            this.tugas = tugas;
        }

        @Override
        public String toString() {
            return tugas == null ? "-- Tidak Terkait Tugas --" : tugas.getJudul() + " (" + tugas.getMataKuliah() + ")";
        }
    }

    public TransaksiPanel(TransaksiRepository transaksiRepo, KategoriRepository kategoriRepo,
            Runnable onDataChanged, Runnable onEksporCsv) {
        this.transaksiRepo = transaksiRepo;
        this.kategoriRepo = kategoriRepo;
        this.onDataChanged = onDataChanged;
        this.onEksporCsv = onEksporCsv;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Form Catatan Keuangan"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 6, 5, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        txtTanggal.setText(LocalDate.now().toString());
        JButton btnTambahKategori = new JButton("+ Kategori");

        JPanel katPanel = new JPanel(new BorderLayout(4, 0));
        katPanel.add(cbKategori, BorderLayout.CENTER);
        katPanel.add(btnTambahKategori, BorderLayout.EAST);

        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("Tanggal (yyyy-MM-dd):"), gbc);
        gbc.gridx = 1;
        formPanel.add(txtTanggal, gbc);
        gbc.gridx = 2;
        formPanel.add(new JLabel("Kategori:"), gbc);
        gbc.gridx = 3;
        formPanel.add(katPanel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(new JLabel("Jumlah (Rp):"), gbc);
        gbc.gridx = 1;
        formPanel.add(txtJumlah, gbc);
        gbc.gridx = 2;
        formPanel.add(new JLabel("Tugas Terkait (Opsional):"), gbc);
        gbc.gridx = 3;
        formPanel.add(cbTugas, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        formPanel.add(new JLabel("Catatan:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 3;
        formPanel.add(txtCatatan, gbc);

        JPanel btnFormPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnBatal.setVisible(false);
        btnFormPanel.add(btnBatal);
        btnFormPanel.add(btnSimpan);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 4;
        formPanel.add(btnFormPanel, gbc);
        add(formPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(
                new String[] { "ID", "Tanggal", "Kategori", "Jenis", "Jumlah", "Tugas Terkait", "Catatan" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setRowHeight(25);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowSorter(new TableRowSorter<>(tableModel));
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel actionPanel = new JPanel(new BorderLayout());
        JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnUbah = new JButton("Ubah");
        JButton btnHapus = new JButton("Hapus");
        leftActions.add(btnUbah);
        leftActions.add(btnHapus);

        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnCsv = new JButton("Ekspor CSV");
        rightActions.add(btnCsv);

        actionPanel.add(leftActions, BorderLayout.WEST);
        actionPanel.add(rightActions, BorderLayout.EAST);
        add(actionPanel, BorderLayout.SOUTH);

        btnSimpan.addActionListener(e -> simpanTransaksi());
        btnBatal.addActionListener(e -> resetForm());
        btnUbah.addActionListener(e -> muatKeForm());
        btnHapus.addActionListener(e -> hapusTransaksi());
        btnTambahKategori.addActionListener(e -> dialogTambahKategori());
        btnCsv.addActionListener(e -> onEksporCsv.run());
    }

    public JButton getDefaultButton() {
        return btnSimpan;
    }

    public void updateData(List<Transaksi> transaksiList, List<Kategori> kategoriList, List<Tugas> tugasList) {
        this.daftarTransaksi = transaksiList;

        cbKategori.removeAllItems();
        for (Kategori k : kategoriList)
            cbKategori.addItem(k);

        cbTugas.removeAllItems();
        cbTugas.addItem(new TugasOption(null));
        for (Tugas t : tugasList)
            cbTugas.addItem(new TugasOption(t));

        tableModel.setRowCount(0);
        for (Transaksi tr : transaksiList) {
            tableModel.addRow(new Object[] {
                    tr.getId(),
                    tr.getTanggal().toString(),
                    tr.getNamaKategori(),
                    tr.getJenisKategori(),
                    FormatUtil.rupiah(tr.getJumlah()),
                    tr.getJudulTugas() == null ? "-" : tr.getJudulTugas(),
                    tr.getCatatan() == null ? "" : tr.getCatatan()
            });
        }
    }

    private void simpanTransaksi() {
        LocalDate tanggal;
        try {
            tanggal = LocalDate.parse(txtTanggal.getText().trim());
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this,
                    "Format tanggal salah. Gunakan yyyy-MM-dd (contoh: 2026-11-26).",
                    "Validasi Tanggal", JOptionPane.WARNING_MESSAGE);
            return;
        }

        long jumlah;
        try {
            jumlah = Long.parseLong(txtJumlah.getText().trim().replace(".", "").replace(",", ""));
            if (jumlah <= 0)
                throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Jumlah harus berupa angka bulat positif dalam Rupiah (contoh: 15000).",
                    "Validasi Nominal", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Kategori kat = (Kategori) cbKategori.getSelectedItem();
        if (kat == null) {
            JOptionPane.showMessageDialog(this, "Pilih atau tambahkan kategori terlebih dahulu.");
            return;
        }

        TugasOption tugasOpt = (TugasOption) cbTugas.getSelectedItem();
        Integer tugasId = (tugasOpt != null && tugasOpt.tugas != null) ? tugasOpt.tugas.getId() : null;

        try {
            Transaksi tr = new Transaksi(
                    editingId == null ? 0 : editingId,
                    tanggal,
                    jumlah,
                    kat.getId(),
                    tugasId,
                    txtCatatan.getText().trim());
            if (editingId == null) {
                transaksiRepo.insert(tr);
                JOptionPane.showMessageDialog(this, "Transaksi berhasil disimpan.");
            } else {
                transaksiRepo.update(tr);
                JOptionPane.showMessageDialog(this, "Transaksi berhasil diperbarui.");
            }
            resetForm();
            onDataChanged.run();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Gagal menyimpan transaksi: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void dialogTambahKategori() {
        JTextField txtNama = new JTextField(15);
        JComboBox<String> cbJenis = new JComboBox<>(new String[] { "PENGELUARAN", "PEMASUKAN" });
        JPanel p = new JPanel(new GridLayout(2, 2, 6, 6));
        p.add(new JLabel("Nama Kategori:"));
        p.add(txtNama);
        p.add(new JLabel("Jenis:"));
        p.add(cbJenis);

        int res = JOptionPane.showConfirmDialog(this, p, "Tambah Kategori Baru", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            String nama = txtNama.getText().trim();
            if (nama.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nama kategori tidak boleh kosong.");
                return;
            }
            try {
                kategoriRepo.insert(new Kategori(0, nama, (String) cbJenis.getSelectedItem()));
                onDataChanged.run();
                JOptionPane.showMessageDialog(this, "Kategori \"" + nama + "\" ditambahkan.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Gagal menambah kategori (mungkin sudah ada): " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void muatKeForm() {
        Transaksi tr = getSelectedTransaksi();
        if (tr == null) {
            JOptionPane.showMessageDialog(this, "Pilih baris transaksi yang ingin diubah.");
            return;
        }
        editingId = tr.getId();
        txtTanggal.setText(tr.getTanggal().toString());
        txtJumlah.setText(String.valueOf(tr.getJumlah()));
        txtCatatan.setText(tr.getCatatan() == null ? "" : tr.getCatatan());

        for (int i = 0; i < cbKategori.getItemCount(); i++) {
            if (cbKategori.getItemAt(i).getId() == tr.getKategoriId()) {
                cbKategori.setSelectedIndex(i);
                break;
            }
        }
        for (int i = 0; i < cbTugas.getItemCount(); i++) {
            Tugas t = cbTugas.getItemAt(i).tugas;
            if ((t == null && tr.getTugasId() == null)
                    || (t != null && tr.getTugasId() != null && t.getId() == tr.getTugasId())) {
                cbTugas.setSelectedIndex(i);
                break;
            }
        }
        btnSimpan.setText("Perbarui Transaksi");
        btnBatal.setVisible(true);
    }

    private void hapusTransaksi() {
        Transaksi tr = getSelectedTransaksi();
        if (tr == null) {
            JOptionPane.showMessageDialog(this, "Pilih baris transaksi yang ingin dihapus.");
            return;
        }
        int konfirmasi = JOptionPane.showConfirmDialog(this,
                "Yakin ingin menghapus transaksi sebesar " + FormatUtil.rupiah(tr.getJumlah()) + "?",
                "Konfirmasi Hapus", JOptionPane.YES_NO_OPTION);
        if (konfirmasi == JOptionPane.YES_OPTION) {
            try {
                transaksiRepo.delete(tr.getId());
                if (editingId != null && editingId == tr.getId())
                    resetForm();
                onDataChanged.run();
                JOptionPane.showMessageDialog(this, "Transaksi berhasil dihapus.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Gagal menghapus transaksi: " + ex.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private Transaksi getSelectedTransaksi() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0)
            return null;
        int modelRow = table.convertRowIndexToModel(viewRow);
        int id = (int) tableModel.getValueAt(modelRow, 0);
        return daftarTransaksi.stream().filter(t -> t.getId() == id).findFirst().orElse(null);
    }

    private void resetForm() {
        editingId = null;
        txtTanggal.setText(LocalDate.now().toString());
        txtJumlah.setText("");
        txtCatatan.setText("");
        if (cbTugas.getItemCount() > 0)
            cbTugas.setSelectedIndex(0);
        btnSimpan.setText("Simpan Transaksi");
        btnBatal.setVisible(false);
    }
}