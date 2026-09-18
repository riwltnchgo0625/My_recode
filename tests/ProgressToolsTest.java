package com.myrecord.app;

import java.util.Calendar;

public final class ProgressToolsTest {
    public static void main(String[] args) {
        Calendar day = Calendar.getInstance();
        day.set(2026, Calendar.AUGUST, 30, 12, 0, 0);
        day.set(Calendar.MILLISECOND, 0);
        String date = DateTools.dateKey(day);

        Models.AppData data = new Models.AppData();
        Models.DailyEntry daily = data.daily(date);
        daily.memo = "오늘 메모";
        daily.items.add(checklist("완료", true));
        daily.items.add(checklist("미완료", false));

        Models.Routine routine = new Models.Routine();
        routine.text = "스트레칭";
        data.routines.add(routine);

        data.goals.add(goal(Models.Goal.MONTH, "2026-08", "월 목표 1", true));
        data.goals.add(goal(Models.Goal.MONTH, "2026-08", "월 목표 2", false));
        data.goals.add(goal(Models.Goal.YEAR, "2026", "연 목표", true));

        ProgressTools.Stats checklist = ProgressTools.checklist(data, date);
        require(checklist.done == 1 && checklist.total == 2, "checklist stats");
        require(checklist.percent() == 50, "checklist percent");

        ProgressTools.Stats total = ProgressTools.dashboardTotal(data, day);
        require(total.done == 3 && total.total == 6, "dashboard total");
        require(total.percent() == 50, "dashboard percent");
        require(ProgressTools.hasDailyMemo(data, date), "daily memo marker");
        require(ProgressTools.findDaily(data, "2026-08-29") == null,
                "missing daily entry");
        System.out.println("ProgressToolsTest: PASS");
    }

    private static Models.ChecklistItem checklist(String text, boolean done) {
        Models.ChecklistItem item = new Models.ChecklistItem();
        item.text = text;
        item.done = done;
        return item;
    }

    private static Models.Goal goal(
            String type,
            String period,
            String text,
            boolean done) {
        Models.Goal goal = new Models.Goal();
        goal.type = type;
        goal.period = period;
        goal.text = text;
        goal.done = done;
        return goal;
    }

    private static void require(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }
}
