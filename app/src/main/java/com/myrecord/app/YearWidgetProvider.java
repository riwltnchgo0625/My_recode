package com.myrecord.app;

import android.appwidget.AppWidgetManager;
import android.content.Context;

public class YearWidgetProvider extends TodayWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        updateWidgets(context, manager, appWidgetIds, PAGE_YEAR);
    }
}
