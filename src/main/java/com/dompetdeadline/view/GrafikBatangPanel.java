package com.dompetdeadline.view;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class GrafikBatangPanel extends JPanel {
    private Map<String, Long> data = new LinkedHashMap<>();

    public GrafikBatangPanel() {
        setPreferredSize(new Dimension(600, 260));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 215, 220)),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
    }

    public void setData(Map<String, Long> data) {
        this.data = (data != null) ? data : new LinkedHashMap<>();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        int padLeft = 95;
        int padRight = 25;
        int padTop = 35;
        int padBottom = 45;

        int chartW = width - padLeft - padRight;
        int chartH = height - padTop - padBottom;

        if (chartW <= 0 || chartH <= 0) {
            g2.dispose();
            return;
        }

        if (data.isEmpty()) {
            g2.setColor(Color.GRAY);
            g2.setFont(new Font("SansSerif", Font.ITALIC, 13));
            String msg = "Belum ada data pengeluaran pada bulan yang dipilih.";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(msg, (width - fm.stringWidth(msg)) / 2, height / 2);
            g2.dispose();
            return;
        }

        long maxVal = data.values().stream().mapToLong(Long::longValue).max().orElse(1L);
        if (maxVal <= 0)
            maxVal = 1L;

        // Gambar garis grid horizontal & label sumbu Y
        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        FontMetrics fm = g2.getFontMetrics();
        int gridLines = 4;
        for (int i = 0; i <= gridLines; i++) {
            int y = padTop + chartH - (int) ((double) i / gridLines * chartH);
            long val = (long) ((double) i / gridLines * maxVal);

            g2.setColor(new Color(235, 238, 242));
            g2.drawLine(padLeft, y, padLeft + chartW, y);

            g2.setColor(new Color(90, 95, 105));
            String yLabel = FormatUtil.rupiah(val);
            g2.drawString(yLabel, padLeft - fm.stringWidth(yLabel) - 8, y + 4);
        }

        // Sumbu X dan Y utama
        g2.setColor(new Color(120, 130, 140));
        g2.drawLine(padLeft, padTop, padLeft, padTop + chartH);
        g2.drawLine(padLeft, padTop + chartH, padLeft + chartW, padTop + chartH);

        // Gambar batang
        int count = data.size();
        int slotW = chartW / count;
        int barW = Math.min(65, Math.max(20, (int) (slotW * 0.55)));
        Color[] palette = {
                new Color(52, 120, 246),
                new Color(16, 185, 129),
                new Color(245, 158, 11),
                new Color(239, 68, 68),
                new Color(139, 92, 246)
        };

        int idx = 0;
        for (Map.Entry<String, Long> entry : data.entrySet()) {
            long val = entry.getValue();
            int barH = (int) ((double) val / maxVal * chartH);
            int x = padLeft + idx * slotW + (slotW - barW) / 2;
            int y = padTop + chartH - barH;

            g2.setColor(palette[idx % palette.length]);
            g2.fillRoundRect(x, y, barW, barH, 8, 8);

            // Label angka di atas batang
            g2.setColor(new Color(30, 35, 45));
            g2.setFont(new Font("SansSerif", Font.BOLD, 11));
            FontMetrics fmVal = g2.getFontMetrics();
            String valStr = FormatUtil.rupiah(val);
            int valX = x + (barW - fmVal.stringWidth(valStr)) / 2;
            int valY = Math.max(padTop - 6, y - 6);
            g2.drawString(valStr, valX, valY);

            // Label kategori sumbu X
            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
            FontMetrics fmCat = g2.getFontMetrics();
            String cat = potongTeks(entry.getKey(), fmCat, slotW - 6);
            int catX = x + (barW - fmCat.stringWidth(cat)) / 2;
            g2.drawString(cat, catX, padTop + chartH + 18);

            idx++;
        }

        g2.dispose();
    }

    private String potongTeks(String text, FontMetrics fm, int maxWidth) {
        if (fm.stringWidth(text) <= maxWidth)
            return text;
        String suffix = "..";
        int end = text.length();
        while (end > 1 && fm.stringWidth(text.substring(0, end) + suffix) > maxWidth) {
            end--;
        }
        return text.substring(0, end) + suffix;
    }
}