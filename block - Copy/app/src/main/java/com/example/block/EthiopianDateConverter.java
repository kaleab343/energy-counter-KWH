package com.example.block;

import java.util.Calendar;

public class EthiopianDateConverter {

    // Convert Gregorian date to Ethiopian date
    public static String toEthiopian(int year, int month, int day) {
        int[] ethDate = convertToEthiopian(year, month, day);
        return ethDate[0] + "-" + ethDate[1] + "-" + ethDate[2];
    }

    private static int[] convertToEthiopian(int year, int month, int day) {
        // Algorithm reference: Ethiopian calendar rules
        int gregorianEpoch = 1723856;
        int ethiopianEpoch = 1724221;

        int jd = (1461 * (year + 4800 + (month - 14) / 12)) / 4
                + (367 * (month - 2 - 12 * ((month - 14) / 12))) / 12
                - (3 * ((year + 4900 + (month - 14) / 12) / 100)) / 4
                + day - 32075;

        int r = jd - ethiopianEpoch;
        int n = (4 * r + 1463) / 1461;
        int y = n + 1;
        int d = r - (365 * n + n / 4);

        if (d == 0) {
            y -= 1;
            d = isLeapYear(y) ? 366 : 365;
        }

        int m = d / 30 + 1;
        int dayOfMonth = d % 30;

        return new int[]{y, m, dayOfMonth};
    }

    private static boolean isLeapYear(int year) {
        return year % 4 == 3;
    }
}
