package com.dompetdeadline.service;

import com.dompetdeadline.model.Transaksi;
import com.dompetdeadline.model.Tugas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DompetServiceTest {

    private DompetService service;

    @BeforeEach
    void setUp() {
        service = new DompetService();
    }

    @Test
    void testHitungSisaUangBulan() {
        Transaksi t1 = new Transaksi(1, LocalDate.of(2026, 11, 2), 1500000, 1, null, "Uang saku");
        t1.setJenisKategori("PEMASUKAN");
        Transaksi t2 = new Transaksi(2, LocalDate.of(2026, 11, 5), 45000, 3, 1, "Print laporan");
        t2.setJenisKategori("PENGELUARAN");
        Transaksi t3 = new Transaksi(3, LocalDate.of(2026, 10, 20), 500000, 3, null, "Bulan lalu");
        t3.setJenisKategori("PENGELUARAN");

        long sisa = service.hitungSisaUangBulan(Arrays.asList(t1, t2, t3), YearMonth.of(2026, 11));
        assertEquals(1455000L, sisa);
    }

    @Test
    void testHitungTotalBiayaTugasAktif() {
        Tugas aktif1 = new Tugas(1, "Laporan PBO", "PBO", LocalDateTime.now().plusDays(2), "TINGGI", false);
        aktif1.setTotalBiaya(35000L);
        Tugas aktif2 = new Tugas(2, "Makalah IMK", "IMK", LocalDateTime.now().plusDays(4), "SEDANG", false);
        aktif2.setTotalBiaya(15000L);
        Tugas selesai = new Tugas(3, "Kuis Basis Data", "Basdat", LocalDateTime.now().minusDays(1), "RENDAH", true);
        selesai.setTotalBiaya(50000L);

        long totalAktif = service.hitungTotalBiayaTugasAktif(Arrays.asList(aktif1, aktif2, selesai));
        assertEquals(50000L, totalAktif);
    }

    @Test
    void testIsTugasMendesak() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 20, 10, 0);
        Tugas mendesak = new Tugas(1, "Demo GUI", "PBO", now.plusHours(12), "TINGGI", false);
        Tugas aman = new Tugas(2, "UAS", "PBO", now.plusHours(30), "TINGGI", false);
        Tugas sudahSelesai = new Tugas(3, "Tugas 1", "PBO", now.plusHours(5), "TINGGI", true);

        assertTrue(service.isTugasMendesak(mendesak, now));
        assertFalse(service.isTugasMendesak(aman, now));
        assertFalse(service.isTugasMendesak(sudahSelesai, now));
    }

    @Test
    void testHitungPengeluaranPerKategori() {
        Transaksi t1 = new Transaksi(1, LocalDate.of(2026, 11, 10), 20000, 3, null, "");
        t1.setNamaKategori("Print");
        t1.setJenisKategori("PENGELUARAN");

        Transaksi t2 = new Transaksi(2, LocalDate.of(2026, 11, 12), 15000, 3, null, "");
        t2.setNamaKategori("Print");
        t2.setJenisKategori("PENGELUARAN");

        Map<String, Long> map = service.hitungPengeluaranPerKategori(Arrays.asList(t1, t2), YearMonth.of(2026, 11));
        assertEquals(35000L, map.get("Print"));
    }
}