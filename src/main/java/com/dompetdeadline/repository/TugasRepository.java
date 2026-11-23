package com.dompetdeadline.repository;

import com.dompetdeadline.model.Tugas;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TugasRepository {
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public List<Tugas> findAllWithTotalBiaya() throws SQLException {
        List<Tugas> list = new ArrayList<>();
        String sql = "SELECT t.id, t.judul, t.mata_kuliah, t.tenggat, t.prioritas, t.selesai, " +
                "COALESCE(SUM(CASE WHEN k.jenis = 'PENGELUARAN' THEN tr.jumlah ELSE 0 END), 0) AS total_biaya " +
                "FROM tugas t " +
                "LEFT JOIN transaksi tr ON tr.tugas_id = t.id " +
                "LEFT JOIN kategori k ON tr.kategori_id = k.id " +
                "GROUP BY t.id " +
                "ORDER BY t.selesai ASC, t.tenggat ASC";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Tugas t = new Tugas(
                        rs.getInt("id"),
                        rs.getString("judul"),
                        rs.getString("mata_kuliah"),
                        LocalDateTime.parse(rs.getString("tenggat"), FORMATTER),
                        rs.getString("prioritas"),
                        rs.getInt("selesai") == 1);
                t.setTotalBiaya(rs.getLong("total_biaya"));
                list.add(t);
            }
        }
        return list;
    }

    public void insert(Tugas t) throws SQLException {
        String sql = "INSERT INTO tugas (judul, mata_kuliah, tenggat, prioritas, selesai) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, t.getJudul());
            ps.setString(2, t.getMataKuliah());
            ps.setString(3, t.getTenggat().format(FORMATTER));
            ps.setString(4, t.getPrioritas());
            ps.setInt(5, t.isSelesai() ? 1 : 0);
            ps.executeUpdate();
        }
    }

    public void update(Tugas t) throws SQLException {
        String sql = "UPDATE tugas SET judul = ?, mata_kuliah = ?, tenggat = ?, prioritas = ?, selesai = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, t.getJudul());
            ps.setString(2, t.getMataKuliah());
            ps.setString(3, t.getTenggat().format(FORMATTER));
            ps.setString(4, t.getPrioritas());
            ps.setInt(5, t.isSelesai() ? 1 : 0);
            ps.setInt(6, t.getId());
            ps.executeUpdate();
        }
    }

    public void setSelesai(int id, boolean selesai) throws SQLException {
        String sql = "UPDATE tugas SET selesai = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, selesai ? 1 : 0);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM tugas WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}