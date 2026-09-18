package com.myrecord.app;

import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Locale;
import java.util.Set;

final class DateTools {
    private static final Locale KO = Locale.KOREAN;

    private DateTools() {}

    static Calendar today() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    static Calendar copy(Calendar source) {
        return (Calendar) source.clone();
    }

    static String dateKey(Calendar calendar) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.getTime());
    }

    static String monthKey(Calendar calendar) {
        return new SimpleDateFormat("yyyy-MM", Locale.US).format(calendar.getTime());
    }

    static String yearKey(Calendar calendar) {
        return new SimpleDateFormat("yyyy", Locale.US).format(calendar.getTime());
    }

    static String fullDate(Calendar calendar) {
        return new SimpleDateFormat("yyyy년 M월 d일 EEEE", KO).format(calendar.getTime());
    }

    static String shortDate(Calendar calendar) {
        return new SimpleDateFormat("M월 d일 E", KO).format(calendar.getTime());
    }

    static String monthTitle(Calendar calendar) {
        return new SimpleDateFormat("yyyy년 M월", KO).format(calendar.getTime());
    }

    static String yearTitle(Calendar calendar) {
        return new SimpleDateFormat("yyyy년", KO).format(calendar.getTime());
    }

    static Calendar parseDateKey(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        format.setLenient(false);
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(format.parse(value));
            calendar.set(Calendar.HOUR_OF_DAY, 12);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            return calendar;
        } catch (ParseException ignored) {
            return null;
        }
    }

    static String weekdayLabel(Set<Integer> weekdays) {
        if (weekdays.size() == 7) return "매일";
        String[] labels = {"", "일", "월", "화", "수", "목", "금", "토"};
        StringBuilder builder = new StringBuilder();
        for (int day = Calendar.SUNDAY; day <= Calendar.SATURDAY; day++) {
            if (weekdays.contains(day)) {
                if (builder.length() > 0) builder.append(" · ");
                builder.append(labels[day]);
            }
        }
        return builder.length() == 0 ? "요일 미설정" : builder.toString();
    }

    static boolean sameDay(Calendar first, Calendar second) {
        return first.get(Calendar.YEAR) == second.get(Calendar.YEAR)
                && first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR);
    }
}
