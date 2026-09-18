package com.myrecord.app;

import android.appwidget.AppWidgetManager;
import android.content.Context;

public class CalendarWidgetProvider extends TodayWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        updateCalendarWidgets(context, manager, appWidgetIds);
    }
}
