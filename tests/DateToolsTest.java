package com.myrecord.app;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

public final class DateToolsTest {
    public static void main(String[] args) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2026, Calendar.AUGUST, 30, 12, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        require("2026-08-30".equals(DateTools.dateKey(calendar)), "dateKey");
        require("2026-08".equals(DateTools.monthKey(calendar)), "monthKey");
        require("2026".equals(DateTools.yearKey(calendar)), "yearKey");
        Calendar parsed = DateTools.parseDateKey("2026-08-30");
        require(parsed != null && "2026-08-30".equals(DateTools.dateKey(parsed)),
                "parseDateKey valid");
        require(DateTools.parseDateKey("2026-02-30") == null, "parseDateKey invalid");
        require(DateTools.parseDateKey("") == null, "parseDateKey empty");

        Set<Integer> all = new HashSet<>();
        for (int day = Calendar.SUNDAY; day <= Calendar.SATURDAY; day++) all.add(day);
        require("매일".equals(DateTools.weekdayLabel(all)), "weekdayLabel all");

        Set<Integer> weekdays = new HashSet<>();
        weekdays.add(Calendar.MONDAY);
        weekdays.add(Calendar.WEDNESDAY);
        weekdays.add(Calendar.FRIDAY);
        require("월 · 수 · 금".equals(DateTools.weekdayLabel(weekdays)), "weekdayLabel selected");

        Calendar same = (Calendar) calendar.clone();
        require(DateTools.sameDay(calendar, same), "sameDay true");
        same.add(Calendar.DAY_OF_MONTH, 1);
        require(!DateTools.sameDay(calendar, same), "sameDay false");
        System.out.println("DateToolsTest: PASS");
    }

    private static void require(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }
}
