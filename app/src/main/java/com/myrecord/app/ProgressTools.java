package com.myrecord.app;

import java.util.Calendar;

final class ProgressTools {
    private ProgressTools() {}

    static final class Stats {
        int done;
        int total;

        Stats() {}

        Stats(int done, int total) {
            this.done = done;
            this.total = total;
        }

        void add(Stats other) {
            done += other.done;
            total += other.total;
        }

        int percent() {
            return total == 0 ? 0 : done * 100 / total;
        }
    }

    static Stats checklist(Models.AppData data, String date) {
        Stats stats = new Stats();
        Models.DailyEntry entry = findDaily(data, date);
        if (entry == null) return stats;
        for (Models.ChecklistItem item : entry.items) {
            if (item.text.trim().isEmpty()) continue;
            stats.total++;
            if (item.done) stats.done++;
        }
        return stats;
    }

    static Stats routines(Models.AppData data, Calendar day, String date) {
        Stats stats = new Stats();
        int weekday = day.get(Calendar.DAY_OF_WEEK);
        for (Models.Routine routine : data.routines) {
            if (!routine.text.trim().isEmpty() && routine.weekdays.contains(weekday)) {
                stats.total++;
                if (routine.doneDates.contains(date)) stats.done++;
            }
        }
        return stats;
    }

    static Stats goals(Models.AppData data, String type, String period) {
        Stats stats = new Stats();
        for (Models.Goal goal : data.goals) {
            if (type.equals(goal.type)
                    && period.equals(goal.period)
                    && !goal.text.trim().isEmpty()) {
                stats.total++;
                if (goal.done) stats.done++;
            }
        }
        return stats;
    }

    static Stats activityForDate(Models.AppData data, Calendar day) {
        String date = DateTools.dateKey(day);
        Stats stats = checklist(data, date);
        Calendar today = DateTools.today();
        if (!day.after(today)) stats.add(routines(data, day, date));
        return stats;
    }

    static Stats dashboardTotal(Models.AppData data, Calendar today) {
        String date = DateTools.dateKey(today);
        Stats result = checklist(data, date);
        result.add(routines(data, today, date));
        result.add(goals(data, Models.Goal.MONTH, DateTools.monthKey(today)));
        result.add(goals(data, Models.Goal.YEAR, DateTools.yearKey(today)));
        return result;
    }

    static Stats recentSevenDays(Models.AppData data, Calendar today) {
        Stats result = new Stats();
        Calendar day = DateTools.copy(today);
        day.add(Calendar.DAY_OF_MONTH, -6);
        for (int index = 0; index < 7; index++) {
            result.add(activityForDate(data, day));
            day.add(Calendar.DAY_OF_MONTH, 1);
        }
        return result;
    }

    static Models.DailyEntry findDaily(Models.AppData data, String date) {
        for (Models.DailyEntry entry : data.dailyEntries) {
            if (date.equals(entry.date)) return entry;
        }
        return null;
    }

    static boolean hasDailyMemo(Models.AppData data, String date) {
        Models.DailyEntry entry = findDaily(data, date);
        return entry != null && !entry.memo.trim().isEmpty();
    }
}
