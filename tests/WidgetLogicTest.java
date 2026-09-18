package com.myrecord.app;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.List;

public final class WidgetLogicTest {
    public static void main(String[] args) throws Exception {
        Calendar day = Calendar.getInstance();
        day.set(2026, Calendar.AUGUST, 30, 12, 0, 0);
        day.set(Calendar.MILLISECOND, 0);
        String date = DateTools.dateKey(day);

        Models.AppData data = new Models.AppData();
        Models.DailyEntry daily = data.daily(date);

        Models.ChecklistItem pending = new Models.ChecklistItem();
        pending.text = "미완료 할 일";
        daily.items.add(pending);

        Models.ChecklistItem completed = new Models.ChecklistItem();
        completed.text = "완료한 할 일";
        completed.done = true;
        daily.items.add(completed);

        Models.Routine routine = new Models.Routine();
        routine.text = "매일 루틴";
        data.routines.add(routine);

        Method collect = TodayWidgetProvider.class.getDeclaredMethod(
                "collectRows", Models.AppData.class, Calendar.class, String.class);
        collect.setAccessible(true);
        List<?> rows = (List<?>) collect.invoke(null, data, day, date);

        require(rows.size() == 2, "today row count");
        require("미완료 할 일".equals(field(rows.get(0), "text")), "pending first");
        require("완료한 할 일".equals(field(rows.get(1), "text")), "completed last");
        require(Boolean.TRUE.equals(field(rows.get(1), "done")), "completed state");

        Method collectRoutines = TodayWidgetProvider.class.getDeclaredMethod(
                "collectRoutineRows", Models.AppData.class, Calendar.class, String.class);
        collectRoutines.setAccessible(true);
        List<?> routines = (List<?>) collectRoutines.invoke(null, data, day, date);
        require(routines.size() == 1, "routine row count");
        require("매일 루틴".equals(field(routines.get(0), "text")), "routine text");

        Models.Goal monthlyPending = new Models.Goal();
        monthlyPending.type = Models.Goal.MONTH;
        monthlyPending.period = "2026-08";
        monthlyPending.text = "이번 달 미완료 목표";
        data.goals.add(monthlyPending);

        Models.Goal monthlyDone = new Models.Goal();
        monthlyDone.type = Models.Goal.MONTH;
        monthlyDone.period = "2026-08";
        monthlyDone.text = "이번 달 완료 목표";
        monthlyDone.done = true;
        data.goals.add(monthlyDone);

        Models.Goal anotherMonth = new Models.Goal();
        anotherMonth.type = Models.Goal.MONTH;
        anotherMonth.period = "2026-09";
        anotherMonth.text = "다른 달 목표";
        data.goals.add(anotherMonth);

        Method collectGoals = TodayWidgetProvider.class.getDeclaredMethod(
                "collectGoalRows", Models.AppData.class, String.class, String.class);
        collectGoals.setAccessible(true);
        List<?> goals = (List<?>) collectGoals.invoke(
                null, data, Models.Goal.MONTH, "2026-08");
        require(goals.size() == 2, "monthly goal row count");
        require("이번 달 미완료 목표".equals(field(goals.get(0), "text")),
                "monthly pending first");
        require(Boolean.TRUE.equals(field(goals.get(1), "done")),
                "monthly completed state");

        Models.Note older = new Models.Note();
        older.title = "이전 메모";
        older.folder = "개인";
        older.updatedAt = 100;
        data.notes.add(older);

        Models.Note recent = new Models.Note();
        recent.title = "최근 메모";
        recent.folder = "업무";
        recent.updatedAt = 200;
        data.notes.add(recent);

        Method collectNotes = TodayWidgetProvider.class.getDeclaredMethod(
                "collectNoteRows", Models.AppData.class);
        collectNotes.setAccessible(true);
        List<?> notes = (List<?>) collectNotes.invoke(null, data);
        require(notes.size() == 2, "note row count");
        require("최근 메모".equals(field(notes.get(0), "text")), "recent note first");
        require("업무".equals(field(notes.get(0), "label")), "note folder label");

        Method collectDashboard = TodayWidgetProvider.class.getDeclaredMethod(
                "collectDashboardRows", Models.AppData.class, Calendar.class, String.class);
        collectDashboard.setAccessible(true);
        List<?> dashboard = (List<?>) collectDashboard.invoke(null, data, day, date);
        require(dashboard.size() == 4, "dashboard category count");
        require("오늘 체크".equals(field(dashboard.get(0), "label")),
                "dashboard today label");
        require("50% · 1/2".equals(field(dashboard.get(0), "text")),
                "dashboard today percent");
        System.out.println("WidgetLogicTest: PASS");
    }

    private static Object field(Object source, String name) throws Exception {
        Field field = source.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(source);
    }

    private static void require(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }
}
