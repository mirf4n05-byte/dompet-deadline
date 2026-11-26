# Dompet Deadline

Aplikasi desktop berbasis **Java Swing** dan **SQLite** yang membantu mahasiswa mengatur keuangan sekaligus memantau tenggat tugas kuliah dalam satu tempat. Setiap pengeluaran dapat dihubungkan langsung ke tugas tertentu (misalnya biaya cetak laporan atau pembelian alat praktik) sehingga total biaya riil tiap tugas dapat terpantau.

## Demo Aplikasi

![Demo Dompet Deadline](docs/demo.png)

## Fitur Utama

- **Dashboard Satu Layar:** Menampilkan sisa uang bulan berjalan, total biaya dari tugas yang masih aktif, dan 3 tenggat tugas terdekat yang belum selesai.
- **Manajemen Transaksi Keuangan:** Tambah, ubah, dan hapus pemasukan maupun pengeluaran dengan kategori kustom serta tabel yang dapat diurutkan.
- **Manajemen Tugas Kuliah:** Catat tugas beserta mata kuliah, tenggat waktu, tingkat prioritas (`TINGGI`, `SEDANG`, `RENDAH`), status penyelesaian, dan akumulasi biaya terkait.
- **Grafik Batang Kustom:** Visualisasi pengeluaran per kategori pada bulan terpilih yang digambar langsung menggunakan `paintComponent`.
- **Pengingat Tenggat Otomatis:** Pengecekan berkala menggunakan `Timer` untuk memunculkan notifikasi melalui *System Tray* (atau dialog cadangan) bagi tugas dengan tenggat kurang dari 24 jam.
- **Ekspor CSV:** Mengekspor seluruh riwayat transaksi ke berkas `.csv`.
- **Penyimpanan Permanen:** Menggunakan SQLite melalui JDBC yang tersimpan otomatis di direktori pengguna (`~/.dompet-deadline/dompet.db`).

## Struktur Proyek

```text
src/
├── main/java/com/dompetdeadline/
│   ├── Main.java                 # Titik masuk utama aplikasi
│   ├── model/                    # Kelas entitas (Kategori, Tugas, Transaksi)
│   ├── repository/               # Koneksi SQLite & query JDBC (PreparedStatement)
│   ├── service/                  # Logika bisnis, kalkulasi saldo, & ekspor CSV
│   └── view/                     # Antarmuka pengguna Java Swing & grafik kustom
└── test/java/com/dompetdeadline/
    └── service/                  # Unit test JUnit 5 untuk logika bisnis