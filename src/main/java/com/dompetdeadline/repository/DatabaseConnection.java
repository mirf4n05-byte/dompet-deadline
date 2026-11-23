package com.dompetdeadline.repository;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {
    private static final String DB_DIR = System.getProperty("user.home") + File.separator + ".dompet-deadline";
    private static final String DB_URL = "jdbc:sqlite:" + DB_DIR + File.separator + "dompet.db";

    static {
        File dir = new File(DB_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        initDatabase();
    }

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }

    private static void initDatabase() {
        String sqlKategori = "CREATE TABLE IF NOT EXISTS kategori (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nama TEXT NOT NULL UNIQUE, " +
                "jenis TEXT NOT NULL CHECK(jenis IN ('PEMASUKAN', 'PENGELUARAN'))" +
                ");";

        String sqlTugas = "CREATE TABLE IF NOT EXISTS tugas (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "judul TEXT NOT NULL, " +
                "mata_kuliah TEXT NOT NULL, " +
                "tenggat TEXT NOT NULL, " +
                "prioritas TEXT NOT NULL CHECK(prioritas IN ('TINGGI', 'SEDANG', 'RENDAH')), " +
                "selesai INTEGER NOT NULL DEFAULT 0" +
                ");";

        String sqlTransaksi = "CREATE TABLE IF NOT EXISTS transaksi (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "tanggal TEXT NOT NULL, " +
                "jumlah INTEGER NOT NULL, " +
                "kategori_id INTEGER NOT NULL, " +
                "tugas_id INTEGER NULL, " +
                "catatan TEXT, " +
                "FOREIGN KEY (kategori_id) REFERENCES kategori(id) ON DELETE RESTRICT, " +
                "FOREIGN KEY (tugas_id) REFERENCES tugas(id) ON DELETE SET NULL" +
                ");";

        String seedKategori = "INSERT OR IGNORE INTO kategori (nama, jenis) VALUES " +
                "('Uang Saku', 'PEMASUKAN'), " +
                "('Beasiswa / Proyek', 'PEMASUKAN'), " +
                "('Print & Fotokopi', 'PENGELUARAN'), " +
                "('Alat Tulis & Modul', 'PENGELUARAN'), " +
                "('Konsumsi & Kopi', 'PENGELUARAN'), " +
                "('Kuota & Langganan', 'PENGELUARAN');";

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sqlKategori);
            stmt.execute(sqlTugas);
            stmt.execute(sqlTransaksi);
            stmt.execute(seedKategori);
        } catch (SQLException e) {
            throw new RuntimeException("Gagal inisialisasi database SQLite: " + e.getMessage(), e);
        }
    }
}