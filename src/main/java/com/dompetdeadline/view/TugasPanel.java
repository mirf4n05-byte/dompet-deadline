package com.dompetdeadline.view;

import com.dompetdeadline.model.Tugas;
import com.dompetdeadline.repository.TugasRepository;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class TugasPanel extends JPanel {
    private final TugasRepository tugasRepo;
    private final Runnable onDataChanged;

    private final JTextField txtJudul = new JTextField(18);
    private final JTextField txtMatkul = new JTextField(15);
    private final JTextField txtTenggat = new JTextField(14);
    private final JComboBox<String> cbPrioritas = new JComboBox<>(new String[]{"TINGGI", "SEDANG", "RENDAH"});
    private final JButton btnSimpan = new JButton("Simpan Tugas");
    private final JButton btnBatal = new JButton("Batal");

    private final DefaultTableModel tableModel;
    private final JTable table;
    private List<Tugas> daftarTugas = new ArrayList<>();
    private Integer editingId = null;

    public TugasPanel(TugasRepository tugasRepo, Runnable onDataChanged) {
        this.tugasRepo = tugasRepo;
        this.onDataChanged = onDataChanged;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Form Tugas Kuliah"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 6, 5, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        txtTenggat.setText(LocalDateTime.now().plusDays(1).format(TugasRepository.FORMATTER));
        txtTenggat.setToolTipText("Format: yyyy-MM-dd HH:mm (Contoh: 2026-11-28 23:59)");

        gbc.gridx = 0; gbc.gridy = 0; formPanel.add(new JLabel("Judul Tugas:"), gbc);
        gbc.gridx = 1; formPanel.add(txtJudul, gbc);
        gbc.gridx = 2; formPanel.add(new JLabel("Mata Kuliah:"), gbc);
        gbc.gridx = 3; formPanel.add(txtMatkul, gbc);

        gbc.gridx = 0; gbc.gridy = 1; formPanel.add(new JLabel("Tenggat (yyyy-MM-dd HH:mm):"), gbc);
        gbc.gridx = 1; formPanel.add(txtTenggat, gbc);
        gbc.gridx = 2; formPanel.add(new JLabel("Prioritas:"), gbc);
        gbc.gridx = 3; formPanel.add(cbPrioritas, gbc);

        JPanel btnFormPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnBatal.setVisible(false);
        btnFormPanel.add(btnBatal);
        btnFormPanel.add(btnSimpan);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 4;
        formPanel.add(btnFormPanel, gbc);
        add(formPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(
                new String[]{"ID", "Judul", "Mata Kuliah", "Tenggat", "Prioritas", "Status", "Total Biaya"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(tableModel);
        table.setRowHeight(25);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowSorter(new TableRowSorter<>(tableModel));
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnSelesai = new JButton("Tandai Selesai / Aktif");
        JButton btnUbah = new JButton("Ubah");
        JButton btnHapus = new JButton("Hapus");
        actionPanel.add(btnSelesai);
        actionPanel.add(btnUbah);
        actionPanel.add(btnHapus);
        add(actionPanel, BorderLayout.SOUTH);

        btnSimpan.addActionListener(e -> simpanTugas());
        btnBatal.addActionListener(e -> resetForm());
        btnSelesai.addActionListener(e -> toggleSelesai());
        btnUbah.addActionListener(e -> muatKeForm());
        btnHapus.addActionListener(e -> hapusTugas());
    }

    public JButton getDefaultButton() {
        return btnSimpan;
    }

    public void setDaftarTugas(List<Tugas> list) {
        this.daftarTugas = list;
        tableModel.setRowCount(0);
        for (Tugas t : list) {
            tableModel.addRow(new Object[]{
                    t.getId(),
                    t.getJudul(),
                    t.getMataKuliah(),
                    t.getTenggat().format(TugasRepository.FORMATTER),
                    t.getPrioritas(),
                    t.isSelesai() ? "Selesai" : "Aktif",
                    FormatUtil.rupiah(t.getTotalBiaya())
            });
        }
    }

    private void simpanTugas() {
        String judul = txtJudul.getText().trim();
        String matkul = txtMatkul.getText().trim();
        String tenggatStr = txtTenggat.getText().trim();

        if (judul.isEmpty() || matkul.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Judul tugas dan mata kuliah wajib diisi. Silakan lengkapi kolom yang kosong.",
                    "Validasi Input", JOptionPane.WARNING_MESSAGE);
            return;
        }

        LocalDateTime tenggat;
        try {
            tenggat = LocalDateTime.parse(tenggatStr, TugasRepository.FORMATTER);
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this,
                    "Format tenggat tidak valid. Gunakan format yyyy-MM-dd HH:mm (contoh: 2026-11-28 23:59).",
                    "Validasi Tanggal", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Tugas t = new Tugas(
                    editingId == null ? 0 : editingId,
                    judul,
                    matkul,
                    tenggat,
                    (String) cbPrioritas.getSelectedItem(),
                    false
            );
            if (editingId == null) {
                tugasRepo.insert(t);
                JOptionPane.showMessageDialog(this, "Tugas berhasil ditambahkan.");
            } else {
                Tugas lama = cariById(editingId);
                if (lama != null) t.setSelesai(lama.isSelesai());
                tugasRepo.update(t);
                JOptionPane.showMessageDialog(this, "Tugas berhasil diperbarui.");
            }
            resetForm();
            onDataChanged.run();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Gagal menyimpan tugas: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void toggleSelesai() {
        Tugas terpilih = getSelectedTugas();
        if (terpilih == null) {
            JOptionPane.showMessageDialog(this, "Pilih satu baris tugas pada tabel terlebih dahulu.");
            return;
        }
        try {
            tugasRepo.setSelesai(terpilih.getId(), !terpilih.isSelesai());
            onDataChanged.run();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Gagal mengubah status tugas: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void muatKeForm() {
        Tugas terpilih = getSelectedTugas();
        if (terpilih == null) {
            JOptionPane.showMessageDialog(this, "Pilih baris tugas yang ingin diubah.");
            return;
        }
        editingId = terpilih.getId();
        txtJudul.setText(terpilih.getJudul());
        txtMatkul.setText(terpilih.getMataKuliah());
        txtTenggat.setText(terpilih.getTenggat().format(TugasRepository.FORMATTER));
        cbPrioritas.setSelectedItem(terpilih.getPrioritas());
        btnSimpan.setText("Perbarui Tugas");
        btnBatal.setVisible(true);
    }

    private void hapusTugas() {
        Tugas terpilih = getSelectedTugas();
        if (terpilih == null) {
            JOptionPane.showMessageDialog(this, "Pilih baris tugas yang ingin dihapus.");
            return;
        }
        int konfirmasi = JOptionPane.showConfirmDialog(this,
                "Hapus tugas \"" + terpilih.getJudul() + "\"? Transaksi terkait tetap aman.",
                "Konfirmasi Hapus", JOptionPane.YES_NO_OPTION);
        if (konfirmasi == JOptionPane.YES_OPTION) {
            try {
                tugasRepo.delete(terpilih.getId());
                if (editingId != null && editingId == terpilih.getId()) resetForm();
                onDataChanged.run();
                JOptionPane.showMessageDialog(this, "Tugas berhasil dihapus.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Gagal menghapus tugas: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private Tugas getSelectedTugas() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) return null;
        int modelRow = table.convertRowIndexToModel(viewRow);
        int id = (int) tableModel.getValueAt(modelRow, 0);
        return cariById(id);
    }

    private Tugas cariById(int id) {
        return daftarTugas.stream().filter(t -> t.getId() == id).findFirst().orElse(null);
    }

    private void resetForm() {
        editingId = null;
        txtJudul.setText("");
        txtMatkul.setText("");
        txtTenggat.setText(LocalDateTime.now().plusDays(1).format(TugasRepository.FORMATTER));
        cbPrioritas.setSelectedIndex(0);
        btnSimpan.setText("Simpan Tugas");
        btnBatal.setVisible(false);
    }
}