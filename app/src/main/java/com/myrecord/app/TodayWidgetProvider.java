package com.myrecord.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.widget.RemoteViews;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TodayWidgetProvider extends AppWidgetProvider {
    static final String PAGE_TODAY = "today";
    static final String PAGE_ROUTINE = "routine";
    static final String PAGE_MONTH = "month";
    static final String PAGE_YEAR = "year";
    static final String PAGE_NOTES = "notes";
    static final String PAGE_DASHBOARD = "dashboard";
    static final String PAGE_CALENDAR = "calendar";

    private static final String ACTION_TOGGLE = "com.myrecord.app.action.WIDGET_TOGGLE";
    private static final String EXTRA_KIND = "kind";
    private static final String EXTRA_ITEM_ID = "item_id";
    private static final String EXTRA_DATE = "date";
    private static final String KIND_DAILY = "daily";
    private static final String KIND_ROUTINE = "routine";
    private static final String KIND_GOAL = "goal";
    private static final String KIND_NOTE = "note";
    private static final String KIND_SUMMARY = "summary";
    private static final int[] ROW_IDS = {
            R.id.widget_row_1,
            R.id.widget_row_2,
            R.id.widget_row_3,
            R.id.widget_row_4
    };
    private static final int[] CALENDAR_DAY_IDS = {
            R.id.widget_day_1, R.id.widget_day_2, R.id.widget_day_3,
            R.id.widget_day_4, R.id.widget_day_5, R.id.widget_day_6,
            R.id.widget_day_7, R.id.widget_day_8, R.id.widget_day_9,
            R.id.widget_day_10, R.id.widget_day_11, R.id.widget_day_12,
            R.id.widget_day_13, R.id.widget_day_14, R.id.widget_day_15,
            R.id.widget_day_16, R.id.widget_day_17, R.id.widget_day_18,
            R.id.widget_day_19, R.id.widget_day_20, R.id.widget_day_21,
            R.id.widget_day_22, R.id.widget_day_23, R.id.widget_day_24,
            R.id.widget_day_25, R.id.widget_day_26, R.id.widget_day_27,
            R.id.widget_day_28, R.id.widget_day_29, R.id.widget_day_30,
            R.id.widget_day_31, R.id.widget_day_32, R.id.widget_day_33,
            R.id.widget_day_34, R.id.widget_day_35, R.id.widget_day_36,
            R.id.widget_day_37, R.id.widget_day_38, R.id.widget_day_39,
            R.id.widget_day_40, R.id.widget_day_41, R.id.widget_day_42
    };

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        updateWidgets(context, manager, appWidgetIds, PAGE_TODAY);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();
        if (ACTION_TOGGLE.equals(action)) {
            toggleItem(context, intent);
            return;
        }
        if (Intent.ACTION_DATE_CHANGED.equals(action)
                || Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
            updateAll(context);
        }
    }

    static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        updateComponent(context, manager, TodayWidgetProvider.class, PAGE_TODAY);
        updateComponent(context, manager, RoutineWidgetProvider.class, PAGE_ROUTINE);
        updateComponent(context, manager, MonthWidgetProvider.class, PAGE_MONTH);
        updateComponent(context, manager, YearWidgetProvider.class, PAGE_YEAR);
        updateComponent(context, manager, NotesWidgetProvider.class, PAGE_NOTES);
        updateComponent(context, manager, DashboardWidgetProvider.class, PAGE_DASHBOARD);
        int[] calendarIds = manager.getAppWidgetIds(
                new ComponentName(context, CalendarWidgetProvider.class));
        if (calendarIds != null && calendarIds.length > 0) {
            updateCalendarWidgets(context, manager, calendarIds);
        }
    }

    private static void updateComponent(
            Context context,
            AppWidgetManager manager,
            Class<?> provider,
            String page) {
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, provider));
        if (ids != null && ids.length > 0) updateWidgets(context, manager, ids, page);
    }

    static void updateWidgets(
            Context context,
            AppWidgetManager manager,
            int[] ids,
            String page) {
        Models.AppData data = new DataStore(context).load();
        Calendar today = DateTools.today();
        String date = DateTools.dateKey(today);
        List<WidgetRow> rows;
        String title;
        String period;
        String dots;
        ProgressTools.Stats dashboardStats = null;

        if (PAGE_ROUTINE.equals(page)) {
            title = "반복 루틴";
            period = DateTools.shortDate(today);
            dots = "○ ● ○ ○ ○ ○ ○";
            rows = collectRoutineRows(data, today, date);
        } else if (PAGE_MONTH.equals(page)) {
            title = "이번 달 목표";
            period = DateTools.monthTitle(today);
            dots = "○ ○ ● ○ ○ ○ ○";
            rows = collectGoalRows(data, Models.Goal.MONTH, DateTools.monthKey(today));
        } else if (PAGE_YEAR.equals(page)) {
            title = "올해 목표";
            period = DateTools.yearTitle(today);
            dots = "○ ○ ○ ● ○ ○ ○";
            rows = collectGoalRows(data, Models.Goal.YEAR, DateTools.yearKey(today));
        } else if (PAGE_NOTES.equals(page)) {
            title = "최근 메모";
            period = "고정 우선 · 최근 수정순";
            dots = "○ ○ ○ ○ ● ○ ○";
            rows = collectNoteRows(data);
        } else if (PAGE_DASHBOARD.equals(page)) {
            title = "달성 대시보드";
            period = DateTools.shortDate(today);
            dots = "○ ○ ○ ○ ○ ● ○";
            rows = collectDashboardRows(data, today, date);
            dashboardStats = ProgressTools.dashboardTotal(data, today);
        } else {
            title = "오늘 기록";
            period = DateTools.shortDate(today);
            dots = "● ○ ○ ○ ○ ○ ○";
            rows = collectRows(data, today, date);
        }

        int done = 0;
        for (WidgetRow row : rows) if (row.done) done++;
        PendingIntent openApp = openAppIntent(context, page);

        for (int widgetId : ids) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_page);
            views.setTextViewText(R.id.widget_title, title);
            views.setTextViewText(R.id.widget_date, period);
            if (PAGE_NOTES.equals(page)) {
                views.setTextViewText(R.id.widget_summary,
                        rows.isEmpty() ? "작성한 메모가 없습니다" : "최근 메모 " + rows.size() + "개");
            } else if (PAGE_DASHBOARD.equals(page)) {
                int total = dashboardStats == null ? 0 : dashboardStats.total;
                int complete = dashboardStats == null ? 0 : dashboardStats.done;
                int percent = dashboardStats == null ? 0 : dashboardStats.percent();
                views.setTextViewText(R.id.widget_summary,
                        total == 0 ? "집계할 항목이 없습니다" : percent + "% 달성 · " + complete + " / " + total);
            } else {
                views.setTextViewText(R.id.widget_summary,
                        rows.isEmpty() ? "등록된 항목이 없습니다" : done + " / " + rows.size() + " 완료");
            }
            views.setTextViewText(R.id.widget_page_index, dots);
            int progressMax = PAGE_DASHBOARD.equals(page) && dashboardStats != null
                    ? Math.max(1, dashboardStats.total) : Math.max(1, rows.size());
            int progressDone = PAGE_DASHBOARD.equals(page) && dashboardStats != null
                    ? dashboardStats.done : done;
            views.setProgressBar(R.id.widget_progress, progressMax, progressDone, false);
            views.setViewVisibility(R.id.widget_progress,
                    PAGE_NOTES.equals(page) ? View.INVISIBLE : View.VISIBLE);
            views.setOnClickPendingIntent(R.id.widget_page_root, openApp);
            views.setOnClickPendingIntent(R.id.widget_header, openApp);
            views.setOnClickPendingIntent(R.id.widget_hint, openApp);

            if (rows.isEmpty()) {
                views.setViewVisibility(R.id.widget_row_1, View.VISIBLE);
                views.setTextViewText(R.id.widget_row_1, "＋ " + title + " 추가하기");
                views.setTextColor(R.id.widget_row_1, Color.rgb(157, 163, 173));
                views.setOnClickPendingIntent(R.id.widget_row_1, openApp);
                for (int index = 1; index < ROW_IDS.length; index++) {
                    views.setViewVisibility(ROW_IDS[index], View.GONE);
                }
            } else {
                for (int index = 0; index < ROW_IDS.length; index++) {
                    int rowId = ROW_IDS[index];
                    if (index >= rows.size()) {
                        views.setViewVisibility(rowId, View.GONE);
                        continue;
                    }
                    WidgetRow row = rows.get(index);
                    views.setViewVisibility(rowId, View.VISIBLE);
                    views.setTextViewText(rowId, KIND_NOTE.equals(row.kind)
                            ? "▤ " + row.label + " · " + row.text
                            : KIND_SUMMARY.equals(row.kind)
                            ? "▥ " + row.label + " · " + row.text
                            : (row.done ? "☑ " : "☐ ") + row.label + " · " + row.text);
                    views.setTextColor(rowId, row.done
                            ? Color.rgb(127, 133, 143)
                            : Color.rgb(244, 245, 247));
                    views.setContentDescription(rowId, KIND_NOTE.equals(row.kind)
                            ? row.text + ", 누르면 메모 열기"
                            : KIND_SUMMARY.equals(row.kind)
                            ? row.label + " " + row.text + ", 누르면 대시보드 열기"
                            : (row.done ? "완료됨, " : "미완료, ") + row.text + ", 누르면 상태 변경");
                    views.setOnClickPendingIntent(rowId, KIND_NOTE.equals(row.kind)
                            ? openNoteIntent(context, row.id)
                            : KIND_SUMMARY.equals(row.kind)
                            ? openApp
                            : toggleIntent(context, row, date));
                }
            }

            int hidden = Math.max(0, rows.size() - ROW_IDS.length);
            views.setTextViewText(R.id.widget_hint, hidden > 0
                    ? "나머지 " + hidden + "개 앱에서 보기 · 좌우로 넘기기"
                    : (PAGE_NOTES.equals(page)
                    ? "메모를 눌러 열기 · 좌우로 넘기기"
                    : PAGE_DASHBOARD.equals(page)
                    ? "눌러서 자세히 보기 · 좌우로 넘기기"
                    : "항목을 눌러 체크 · 좌우로 넘기기"));
            manager.updateAppWidget(widgetId, views);
        }
    }

    static List<WidgetRow> collectRows(
            Models.AppData data,
            Calendar today,
            String date) {
        List<WidgetRow> rows = new ArrayList<>();
        Models.DailyEntry daily = data.daily(date);
        int order = 0;
        for (Models.ChecklistItem item : daily.items) {
            if (!item.text.trim().isEmpty()) {
                rows.add(new WidgetRow(
                        KIND_DAILY,
                        item.id,
                        "할 일",
                        item.text.trim(),
                        item.done,
                        order++));
            }
        }

        sortRows(rows);
        return rows;
    }

    static List<WidgetRow> collectRoutineRows(
            Models.AppData data,
            Calendar today,
            String date) {
        List<WidgetRow> rows = new ArrayList<>();
        int order = 0;
        int weekday = today.get(Calendar.DAY_OF_WEEK);
        for (Models.Routine routine : data.routines) {
            if (routine.weekdays.contains(weekday) && !routine.text.trim().isEmpty()) {
                rows.add(new WidgetRow(
                        KIND_ROUTINE,
                        routine.id,
                        "루틴",
                        routine.text.trim(),
                        routine.doneDates.contains(date),
                        order++));
            }
        }
        sortRows(rows);
        return rows;
    }

    static List<WidgetRow> collectNoteRows(Models.AppData data) {
        List<Models.Note> notes = new ArrayList<>(data.notes);
        Collections.sort(notes, new Comparator<Models.Note>() {
            @Override
            public int compare(Models.Note first, Models.Note second) {
                if (first.pinned != second.pinned) return first.pinned ? -1 : 1;
                return Long.compare(second.updatedAt, first.updatedAt);
            }
        });
        List<WidgetRow> rows = new ArrayList<>();
        int order = 0;
        for (Models.Note note : notes) {
            if (note.isBlank()) continue;
            String title = note.title.trim().isEmpty() ? "제목 없는 메모" : note.title.trim();
            String folder = note.folder.trim().isEmpty() ? "미분류" : note.folder.trim();
            rows.add(new WidgetRow(KIND_NOTE, note.id, folder, title, false, order++));
        }
        return rows;
    }

    static List<WidgetRow> collectGoalRows(
            Models.AppData data,
            String type,
            String period) {
        List<WidgetRow> rows = new ArrayList<>();
        int order = 0;
        for (Models.Goal goal : data.goals) {
            if (type.equals(goal.type)
                    && period.equals(goal.period)
                    && !goal.text.trim().isEmpty()) {
                rows.add(new WidgetRow(
                        KIND_GOAL,
                        goal.id,
                        "목표",
                        goal.text.trim(),
                        goal.done,
                        order++));
            }
        }
        sortRows(rows);
        return rows;
    }

    static List<WidgetRow> collectDashboardRows(
            Models.AppData data,
            Calendar today,
            String date) {
        List<WidgetRow> rows = new ArrayList<>();
        addDashboardRow(rows, "오늘 체크", ProgressTools.checklist(data, date), 0);
        addDashboardRow(rows, "반복 루틴", ProgressTools.routines(data, today, date), 1);
        addDashboardRow(rows, "한 달 목표", ProgressTools.goals(
                data, Models.Goal.MONTH, DateTools.monthKey(today)), 2);
        addDashboardRow(rows, "1년 목표", ProgressTools.goals(
                data, Models.Goal.YEAR, DateTools.yearKey(today)), 3);
        return rows;
    }

    private static void addDashboardRow(
            List<WidgetRow> rows,
            String label,
            ProgressTools.Stats stats,
            int order) {
        String value = stats.total == 0
                ? "항목 없음"
                : stats.percent() + "% · " + stats.done + "/" + stats.total;
        rows.add(new WidgetRow(
                KIND_SUMMARY,
                label,
                label,
                value,
                stats.total > 0 && stats.done == stats.total,
                order));
    }

    static void updateCalendarWidgets(
            Context context,
            AppWidgetManager manager,
            int[] ids) {
        Models.AppData data = new DataStore(context).load();
        Calendar today = DateTools.today();
        Calendar first = DateTools.copy(today);
        first.set(Calendar.DAY_OF_MONTH, 1);
        Calendar cursor = DateTools.copy(first);
        cursor.add(Calendar.DAY_OF_MONTH, -(first.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY));
        PendingIntent openCalendar = openAppIntent(context, PAGE_CALENDAR);

        for (int widgetId : ids) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_calendar);
            views.setTextViewText(R.id.widget_calendar_month, DateTools.monthTitle(today));
            views.setOnClickPendingIntent(R.id.widget_calendar_root, openCalendar);
            views.setOnClickPendingIntent(R.id.widget_calendar_header, openCalendar);

            Calendar day = DateTools.copy(cursor);
            for (int index = 0; index < CALENDAR_DAY_IDS.length; index++) {
                int cellId = CALENDAR_DAY_IDS[index];
                ProgressTools.Stats stats = ProgressTools.activityForDate(data, day);
                boolean hasMemo = ProgressTools.hasDailyMemo(data, DateTools.dateKey(day));
                boolean currentMonth = day.get(Calendar.MONTH) == today.get(Calendar.MONTH)
                        && day.get(Calendar.YEAR) == today.get(Calendar.YEAR);
                boolean isToday = DateTools.sameDay(day, today);
                String secondLine = stats.total > 0 ? stats.percent() + "%" : "";
                if (hasMemo) secondLine += "·";
                String text = day.get(Calendar.DAY_OF_MONTH)
                        + (secondLine.isEmpty() ? "" : "\n" + secondLine);
                views.setTextViewText(cellId, text);
                views.setTextColor(cellId, isToday
                        ? Color.rgb(16, 17, 20)
                        : currentMonth ? Color.rgb(244, 245, 247) : Color.rgb(93, 98, 108));
                views.setInt(cellId, "setBackgroundResource", isToday
                        ? R.drawable.widget_day_today : R.drawable.widget_day_clear);
                views.setContentDescription(cellId,
                        DateTools.fullDate(day)
                                + (stats.total > 0 ? ", 달성률 " + stats.percent() + "%" : ", 기록 없음")
                                + (hasMemo ? ", 당일 메모 있음" : "")
                                + ", 누르면 해당 날짜 열기");
                views.setOnClickPendingIntent(cellId, openDateIntent(context, day));
                day.add(Calendar.DAY_OF_MONTH, 1);
            }
            manager.updateAppWidget(widgetId, views);
        }
    }

    private static void sortRows(List<WidgetRow> rows) {
        Collections.sort(rows, new Comparator<WidgetRow>() {
            @Override
            public int compare(WidgetRow first, WidgetRow second) {
                if (first.done != second.done) return first.done ? 1 : -1;
                return Integer.compare(first.order, second.order);
            }
        });
    }

    private static PendingIntent openAppIntent(Context context, String tab) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_OPEN_TAB, tab);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(
                context,
                9500 + tab.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static PendingIntent openDateIntent(Context context, Calendar day) {
        String date = DateTools.dateKey(day);
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_OPEN_TAB, PAGE_TODAY);
        intent.putExtra(MainActivity.EXTRA_OPEN_DATE, date);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(
                context,
                9900 + date.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static PendingIntent toggleIntent(Context context, WidgetRow row, String date) {
        Intent intent = new Intent(context, TodayWidgetProvider.class);
        intent.setAction(ACTION_TOGGLE);
        intent.putExtra(EXTRA_KIND, row.kind);
        intent.putExtra(EXTRA_ITEM_ID, row.id);
        intent.putExtra(EXTRA_DATE, date);
        int requestCode = (row.kind + row.id + date).hashCode();
        return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static PendingIntent openNoteIntent(Context context, String noteId) {
        Intent intent = new Intent(context, NoteEditorActivity.class);
        intent.putExtra(NoteEditorActivity.EXTRA_NOTE_ID, noteId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(
                context,
                9700 + noteId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static void toggleItem(Context context, Intent intent) {
        String kind = intent.getStringExtra(EXTRA_KIND);
        String itemId = intent.getStringExtra(EXTRA_ITEM_ID);
        String date = intent.getStringExtra(EXTRA_DATE);
        if (kind == null || itemId == null) return;
        if ((KIND_DAILY.equals(kind) || KIND_ROUTINE.equals(kind)) && date == null) return;

        DataStore store = new DataStore(context);
        Models.AppData data = store.load();
        boolean changed = false;
        if (KIND_DAILY.equals(kind)) {
            Models.DailyEntry daily = data.daily(date);
            for (Models.ChecklistItem item : daily.items) {
                if (item.id.equals(itemId)) {
                    item.done = !item.done;
                    changed = true;
                    break;
                }
            }
        } else if (KIND_ROUTINE.equals(kind)) {
            for (Models.Routine routine : data.routines) {
                if (routine.id.equals(itemId)) {
                    if (routine.doneDates.contains(date)) routine.doneDates.remove(date);
                    else routine.doneDates.add(date);
                    changed = true;
                    break;
                }
            }
        } else if (KIND_GOAL.equals(kind)) {
            for (Models.Goal goal : data.goals) {
                if (goal.id.equals(itemId)) {
                    goal.done = !goal.done;
                    changed = true;
                    break;
                }
            }
        }
        if (changed) store.save(data);
        else updateAll(context);
    }

    static final class WidgetRow {
        final String kind;
        final String id;
        final String label;
        final String text;
        final boolean done;
        final int order;

        WidgetRow(String kind, String id, String label, String text, boolean done, int order) {
            this.kind = kind;
            this.id = id;
            this.label = label;
            this.text = text;
            this.done = done;
            this.order = order;
        }
    }
}
