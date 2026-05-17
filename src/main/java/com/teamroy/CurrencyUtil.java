package com.teamroy;
import java.text.NumberFormat;
import java.util.Locale;
public final class CurrencyUtil {
    private static final NumberFormat FMT =
            NumberFormat.getCurrencyInstance(Locale.forLanguageTag("fil-PH"));
    static {
        FMT.setMaximumFractionDigits(2);
        FMT.setMinimumFractionDigits(2);
    }
    private CurrencyUtil() {}
    public static String format(double amount) {
        String s = FMT.format(amount);
        return s.replace("PHP", "\u20b1").replace("Php", "\u20b1").replace("PH\u20b1", "\u20b1");
    }
}
