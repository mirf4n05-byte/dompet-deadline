package com.dompetdeadline.service;

import com.dompetdeadline.model.Transaksi;
import com.dompetdeadline.model.Tugas;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DompetService {

    public long hitungSisaUangBulan(List<Transaksi> daftarTransaksi, YearMonth bulan) {
        long pemasukan = 0;
        long pengeluaran = 0;
        for (Transaksi t : daftarTransaksi) {
            if (YearMonth.from(t.getTanggal()).equals(bulan)) {
                if ("PEMASUKAN".equalsIgnoreCase(t.getJenisKategori())) {
                    pemasukan += t.getJumlah();
                } else if ("PENGELUARAN".equalsIgnoreCase(t.getJenisKategori())) {
                    pengeluaran += t.getJumlah();
                }
            }
        }
        return pemasukan - pengeluaran;
    }

    public long hitungTotalPengeluaranBulan(List<Transaksi> daftarTransaksi, YearMonth bulan) {
        long pengeluaran = 0;
        for (Transaksi t : daftarTransaksi) {
            if (YearMonth.from(t.getTanggal()).equals(bulan)
                    && "PENGELUARAN".equalsIgnoreCase(t.getJenisKategori())) {
                pengeluaran += t.getJumlah();
            }
        }
        return pengeluaran;
    }

    public long hitungTotalBiayaTugasAktif(List<Tugas> daftarTugas) {
        return daftarTugas.stream()
                .filter(t -> !t.isSelesai())
                .mapToLong(Tugas::getTotalBiaya)
                .sum();
    }

    public List<Tugas> getTigaTenggatTerdekat(List<Tugas> daftarTugas) {
        return daftarTugas.stream()
                .filter(t -> !t.isSelesai())
                .sorted(Comparator.comparing(Tugas::getTenggat))
                .limit(3)
                .collect(Collectors.toList());
    }

    public boolean isTugasMendesak(Tugas tugas, LocalDateTime sekarang) {
        if (tugas.isSelesai()) {
            return false;
        }
        Duration selisih = Duration.between(sekarang, tugas.getTenggat());
        long menit = selisih.toMinutes();
        return menit >= 0 && menit <= 24 * 60;
    }

    public Map<String, Long> hitungPengeluaranPerKategori(List<Transaksi> daftarTransaksi, YearMonth bulan) {
        Map<String, Long> hasil = new LinkedHashMap<>();
        for (Transaksi t : daftarTransaksi) {
            if (YearMonth.from(t.getTanggal()).equals(bulan)
                    && "PENGELUARAN".equalsIgnoreCase(t.getJenisKategori())) {
                String kategori = t.getNamaKategori() != null ? t.getNamaKategori() : "Lainnya";
                hasil.put(kategori, hasil.getOrDefault(kategori, 0L) + t.getJumlah());
            }
        }
        return hasil;
    }

    public void eksporCsv(List<Transaksi> daftarTransaksi, File targetFile) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(targetFile), StandardCharsets.UTF_8))) {
            writer.write("tanggal,kategori,jenis,jumlah,tugas_terkait,catatan");
            writer.newLine();
            for (Transaksi t : daftarTransaksi) {
                String tugasStr = (t.getJudulTugas() == null || t.getJudulTugas().isEmpty()) ? "-" : t.getJudulTugas();
                String catatanStr = (t.getCatatan() == null) ? "" : t.getCatatan();
                writer.write(String.format("%s,%s,%s,%d,%s,%s",
                        escapeCsv(t.getTanggal().toString()),
                        escapeCsv(t.getNamaKategori()),
                        escapeCsv(t.getJenisKategori()),
                        t.getJumlah(),
                        escapeCsv(tugasStr),
                        escapeCsv(catatanStr)));
                writer.newLine();
            }
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}