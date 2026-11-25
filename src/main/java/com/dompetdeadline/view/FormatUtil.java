package com.dompetdeadline.view;

import java.text.NumberFormat;
import java.util.Locale;

public class FormatUtil {
    private static final NumberFormat RUPIAH = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));

    static {
        RUPIAH.setMaximumFractionDigits(0);
    }

    public static String rupiah(long nominal) {
        return RUPIAH.format(nominal);
    }
}