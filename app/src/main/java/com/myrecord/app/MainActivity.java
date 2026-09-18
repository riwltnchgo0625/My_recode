package com.myrecord.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity {
    static final String EXTRA_OPEN_TAB = "open_tab";
    static final String EXTRA_OPEN_DATE = "open_date";
    private static final String TAB_TODAY = "today";
    private static final String TAB_ROUTINE = "routine";
    private static final String TAB_MONTH = "month";
    private static final String TAB_YEAR = "year";
    private static final String TAB_NOTES = "notes";
    private static final String TAB_DASHBOARD = "dashboard";
    private static final String TAB_CALENDAR = "calendar";
    private static final String NOTE_SORT_UPDATED = "updated";
    private static final String NOTE_SORT_TITLE = "title";

    private DataStore store;
    private Models.AppData data;
    private FrameLayout contentFrame;
    private LinearLayout navBar;
    private String selectedTab = TAB_TODAY;
    private Calendar selectedDay = DateTools.today();
    private Calendar selectedMonth = DateTools.today();
    private Calendar selectedYear = DateTools.today();
    private Calendar calendarMonth = DateTools.today();
    private String noteQuery = "";
    private String selectedFolder = "전체";
    private String noteSortMode = NOTE_SORT_UPDATED;
    private int dashboardPageIndex;
    private final Handler saveHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingMemoSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(Ui.BG);
        getWindow().getDecorView().setSystemUiVisibility(0);

        store = new DataStore(this);
        data = store.load();
        applyRequestedTab(getIntent());
        buildShell();
        render();
        showLoadErrorIfNeeded();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        applyRequestedTab(intent);
        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (store != null && contentFrame != null) {
            data = store.load();
            render();
            showLoadErrorIfNeeded();
        }
    }

    @Override
    protected void onPause() {
        flushPendingMemo();
        if (store != null && data != null) store.save(data);
        super.onPause();
    }

    private void buildShell() {
        LinearLayout root = Ui.vertical(this);
        root.setBackgroundColor(Ui.BG);

        contentFrame = new FrameLayout(this);
        root.addView(contentFrame, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        navBar = Ui.horizontal(this);
        navBar.setGravity(Gravity.CENTER);
        navBar.setPadding(Ui.dp(this, 6), Ui.dp(this, 7), Ui.dp(this, 6), Ui.dp(this, 7));
        navBar.setBackgroundColor(Ui.SURFACE);
        root.addView(navBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 70)));
        setContentView(root);
    }

    private void render() {
        if (contentFrame == null) return;
        contentFrame.removeAllViews();
        View page;
        if (TAB_ROUTINE.equals(selectedTab)) {
            page = routinePage();
        } else if (TAB_MONTH.equals(selectedTab)) {
            page = goalsPage(true);
        } else if (TAB_YEAR.equals(selectedTab)) {
            page = goalsPage(false);
        } else if (TAB_NOTES.equals(selectedTab)) {
            page = notesPage();
        } else if (TAB_DASHBOARD.equals(selectedTab)) {
            page = dashboardPage();
        } else if (TAB_CALENDAR.equals(selectedTab)) {
            page = calendarPage();
        } else {
            page = todayPage();
        }
        contentFrame.addView(page, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        renderNav();
    }

    private void applyRequestedTab(Intent intent) {
        if (intent == null) return;
        String requested = intent.getStringExtra(EXTRA_OPEN_TAB);
        if (TAB_TODAY.equals(requested)
                || TAB_ROUTINE.equals(requested)
                || TAB_MONTH.equals(requested)
                || TAB_YEAR.equals(requested)
                || TAB_NOTES.equals(requested)
                || TAB_DASHBOARD.equals(requested)
                || TAB_CALENDAR.equals(requested)) {
            selectedTab = requested;
        }
        Calendar requestedDate = DateTools.parseDateKey(intent.getStringExtra(EXTRA_OPEN_DATE));
        if (requestedDate != null) {
            selectedDay = requestedDate;
            calendarMonth = DateTools.copy(requestedDate);
        } else if (TAB_TODAY.equals(requested)) {
            selectedDay = DateTools.today();
        }
    }

    private void renderNav() {
        navBar.removeAllViews();
        addNav("✓", "오늘", TAB_TODAY);
        addNav("↻", "루틴", TAB_ROUTINE);
        addNav("○", "월 목표", TAB_MONTH);
        addNav("◎", "연 목표", TAB_YEAR);
        addNav("▤", "메모", TAB_NOTES);
        addNav("▥", "현황", TAB_DASHBOARD);
        addNav("□", "달력", TAB_CALENDAR);
    }

    private void addNav(String icon, String label, final String tab) {
        boolean active = tab.equals(selectedTab);
        LinearLayout item = Ui.vertical(this);
        item.setGravity(Gravity.CENTER);
        item.setPadding(Ui.dp(this, 2), Ui.dp(this, 4), Ui.dp(this, 2), Ui.dp(this, 4));
        Ui.setClickableBackground(item, active ? Ui.PRIMARY_SOFT : Color.TRANSPARENT, 14);

        TextView iconView = Ui.text(this, icon, 18, active ? Ui.PRIMARY : Ui.MUTED);
        iconView.setGravity(Gravity.CENTER);
        item.addView(iconView, Ui.matchWrap());
        TextView labelView = Ui.text(this, label, 9.5f, active ? Ui.PRIMARY : Ui.MUTED);
        labelView.setGravity(Gravity.CENTER);
        if (active) labelView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        item.addView(labelView, Ui.margins(Ui.matchWrap(), this, 0, 3, 0, 0));

        item.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!tab.equals(selectedTab)) {
                    flushPendingMemo();
                    Ui.hideKeyboard(MainActivity.this);
                    selectedTab = tab;
                    render();
                }
            }
        });
        navBar.addView(item, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.MATCH_PARENT, 1));
    }

    private View todayPage() {
        ScrollView scroll = pageScroll();
        LinearLayout content = pageContent();
        scroll.addView(content);

        Calendar today = DateTools.today();
        boolean isToday = DateTools.sameDay(selectedDay, today);
        content.addView(pageHeading(
                isToday ? "오늘 기록" : "날짜별 기록",
                isToday ? "오늘 할 일과 생각을 한곳에 남겨보세요." : "지난 기록도 수정할 수 있습니다."));

        LinearLayout dateRow = Ui.horizontal(this);
        TextView previous = Ui.iconButton(this, "‹");
        previous.setContentDescription("이전 날짜");
        previous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                flushPendingMemo();
                selectedDay.add(Calendar.DAY_OF_MONTH, -1);
                render();
            }
        });
        dateRow.addView(previous, new LinearLayout.LayoutParams(
                Ui.dp(this, 48), Ui.dp(this, 48)));

        LinearLayout dateLabels = Ui.vertical(this);
        dateLabels.setGravity(Gravity.CENTER);
        TextView date = Ui.sectionTitle(this, DateTools.shortDate(selectedDay));
        date.setGravity(Gravity.CENTER);
        dateLabels.addView(date, Ui.matchWrap());
        TextView dateHint = Ui.caption(this, isToday ? "오늘" : DateTools.dateKey(selectedDay));
        dateHint.setGravity(Gravity.CENTER);
        dateLabels.addView(dateHint, Ui.margins(Ui.matchWrap(), this, 0, 3, 0, 0));
        dateRow.addView(dateLabels, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView next = Ui.iconButton(this, "›");
        next.setContentDescription("다음 날짜");
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                flushPendingMemo();
                selectedDay.add(Calendar.DAY_OF_MONTH, 1);
                render();
            }
        });
        dateRow.addView(next, new LinearLayout.LayoutParams(
                Ui.dp(this, 48), Ui.dp(this, 48)));
        Ui.setRoundedBackground(dateRow, Ui.SURFACE, 22);
        content.addView(dateRow, Ui.margins(Ui.matchWrap(), this, 0, 20, 0, 0));

        if (!isToday) {
            Button todayButton = Ui.softButton(this, "오늘로 돌아가기");
            todayButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    flushPendingMemo();
                    selectedDay = DateTools.today();
                    render();
                }
            });
            content.addView(todayButton, Ui.margins(Ui.matchWrap(), this, 0, 10, 0, 0));
        }

        final Models.DailyEntry entry = data.daily(DateTools.dateKey(selectedDay));
        int done = 0;
        for (Models.ChecklistItem item : entry.items) if (item.done) done++;
        content.addView(progressCard(done, entry.items.size(), "체크리스트"),
                Ui.margins(Ui.matchWrap(), this, 0, 18, 0, 0));

        LinearLayout listTitle = sectionHeader("체크리스트", "+ 항목 추가");
        listTitle.getChildAt(1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showTextDialog("체크할 항목", "", "예: 물 2L 마시기", new TextResult() {
                    @Override
                    public void accept(String value) {
                        Models.ChecklistItem item = new Models.ChecklistItem();
                        item.text = value;
                        entry.items.add(item);
                        saveAndRender();
                    }
                });
            }
        });
        content.addView(listTitle, Ui.margins(Ui.matchWrap(), this, 0, 24, 0, 10));

        if (entry.items.isEmpty()) {
            content.addView(emptyCard("아직 체크할 항목이 없습니다.", "오른쪽의 ‘항목 추가’를 눌러 시작하세요."));
        } else {
            for (final Models.ChecklistItem item : entry.items) {
                content.addView(checkItemRow(
                        item.text,
                        item.done,
                        true,
                        detailPreview(item.details),
                        new CheckedResult() {
                            @Override
                            public void accept(boolean checked) {
                                item.done = checked;
                                saveAndRender();
                            }
                        },
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                openItemDetail(
                                        ItemDetailActivity.KIND_DAILY,
                                        item.id,
                                        DateTools.dateKey(selectedDay));
                            }
                        },
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                showChecklistMenu(entry, item);
                            }
                        }), Ui.margins(Ui.matchWrap(), this, 0, 0, 0, 9));
            }
        }

        TextView memoTitle = Ui.sectionTitle(this, "당일 메모");
        content.addView(memoTitle, Ui.margins(Ui.matchWrap(), this, 0, 25, 0, 10));
        final EditText memo = Ui.input(this, "오늘 있었던 일이나 생각을 자유롭게 적어보세요.", true);
        memo.setMinHeight(Ui.dp(this, 150));
        memo.setText(entry.memo);
        content.addView(memo, Ui.matchWrap());
        final TextView saveState = Ui.caption(this, "입력하면 자동 저장됩니다.");
        saveState.setGravity(Gravity.END);
        content.addView(saveState, Ui.margins(Ui.matchWrap(), this, 0, 7, 2, 0));
        memo.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable editable) {
                entry.memo = editable.toString();
                scheduleMemoSave(saveState);
            }
        });

        return scroll;
    }

    private View routinePage() {
        ScrollView scroll = pageScroll();
        LinearLayout content = pageContent();
        scroll.addView(content);

        final Calendar today = DateTools.today();
        final String dateKey = DateTools.dateKey(today);
        final int weekday = today.get(Calendar.DAY_OF_WEEK);
        content.addView(pageHeading("반복 루틴", DateTools.fullDate(today)));

        int active = 0;
        int done = 0;
        for (Models.Routine routine : data.routines) {
            if (routine.weekdays.contains(weekday)) {
                active++;
                if (routine.doneDates.contains(dateKey)) done++;
            }
        }
        content.addView(progressCard(done, active, "오늘 루틴"),
                Ui.margins(Ui.matchWrap(), this, 0, 20, 0, 0));

        LinearLayout title = sectionHeader("나의 루틴", "+ 루틴 추가");
        title.getChildAt(1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showRoutineDialog(null);
            }
        });
        content.addView(title, Ui.margins(Ui.matchWrap(), this, 0, 24, 0, 10));

        if (data.routines.isEmpty()) {
            content.addView(emptyCard("등록된 루틴이 없습니다.", "요일을 정해 반복할 습관을 추가해 보세요."));
        } else {
            for (final Models.Routine routine : data.routines) {
                final boolean scheduled = routine.weekdays.contains(weekday);
                boolean checked = routine.doneDates.contains(dateKey);
                content.addView(checkItemRow(
                        routine.text,
                        checked,
                        scheduled,
                        routineSubtitle(routine, scheduled),
                        new CheckedResult() {
                            @Override
                            public void accept(boolean value) {
                                if (value) routine.doneDates.add(dateKey);
                                else routine.doneDates.remove(dateKey);
                                saveAndRender();
                            }
                        },
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                openItemDetail(ItemDetailActivity.KIND_ROUTINE, routine.id, null);
                            }
                        },
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                showRoutineMenu(routine);
                            }
                        }), Ui.margins(Ui.matchWrap(), this, 0, 0, 0, 9));
            }
        }
        return scroll;
    }

    private View goalsPage(final boolean monthly) {
        ScrollView scroll = pageScroll();
        LinearLayout content = pageContent();
        scroll.addView(content);

        final Calendar selected = monthly ? selectedMonth : selectedYear;
        final String type = monthly ? Models.Goal.MONTH : Models.Goal.YEAR;
        final String period = monthly ? DateTools.monthKey(selected) : DateTools.yearKey(selected);
        content.addView(pageHeading(monthly ? "한 달 목표" : "1년 목표",
                monthly ? "이번 달에 이루고 싶은 일을 체크하세요." : "올해의 큰 목표를 한눈에 관리하세요."));

        LinearLayout periodRow = Ui.horizontal(this);
        TextView previous = Ui.iconButton(this, "‹");
        previous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (monthly) {
                    selected.set(Calendar.DAY_OF_MONTH, 1);
                    selected.add(Calendar.MONTH, -1);
                } else {
                    selected.add(Calendar.YEAR, -1);
                }
                render();
            }
        });
        periodRow.addView(previous, new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 48)));
        TextView periodTitle = Ui.sectionTitle(this,
                monthly ? DateTools.monthTitle(selected) : DateTools.yearTitle(selected));
        periodTitle.setGravity(Gravity.CENTER);
        periodRow.addView(periodTitle, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView next = Ui.iconButton(this, "›");
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (monthly) {
                    selected.set(Calendar.DAY_OF_MONTH, 1);
                    selected.add(Calendar.MONTH, 1);
                } else {
                    selected.add(Calendar.YEAR, 1);
                }
                render();
            }
        });
        periodRow.addView(next, new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 48)));
        Ui.setRoundedBackground(periodRow, Ui.SURFACE, 22);
        content.addView(periodRow, Ui.margins(Ui.matchWrap(), this, 0, 20, 0, 0));

        final List<Models.Goal> goals = goalsFor(type, period);
        int done = 0;
        for (Models.Goal goal : goals) if (goal.done) done++;
        content.addView(progressCard(done, goals.size(), monthly ? "이번 달" : "올해"),
                Ui.margins(Ui.matchWrap(), this, 0, 18, 0, 0));

        LinearLayout title = sectionHeader("목표 목록", "+ 목표 추가");
        title.getChildAt(1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showTextDialog(monthly ? "한 달 목표 추가" : "1년 목표 추가", "",
                        "달성하고 싶은 목표", new TextResult() {
                            @Override
                            public void accept(String value) {
                                Models.Goal goal = new Models.Goal();
                                goal.type = type;
                                goal.period = period;
                                goal.text = value;
                                data.goals.add(goal);
                                saveAndRender();
                            }
                        });
            }
        });
        content.addView(title, Ui.margins(Ui.matchWrap(), this, 0, 24, 0, 10));

        if (goals.isEmpty()) {
            content.addView(emptyCard("아직 목표가 없습니다.", "체크할 수 있는 목표를 하나씩 추가해 보세요."));
        } else {
            for (final Models.Goal goal : goals) {
                content.addView(checkItemRow(
                        goal.text,
                        goal.done,
                        true,
                        detailPreview(goal.details),
                        new CheckedResult() {
                            @Override
                            public void accept(boolean checked) {
                                goal.done = checked;
                                saveAndRender();
                            }
                        },
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                openItemDetail(ItemDetailActivity.KIND_GOAL, goal.id, null);
                            }
                        },
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                showGoalMenu(goal);
                            }
                        }), Ui.margins(Ui.matchWrap(), this, 0, 0, 0, 9));
            }
        }
        return scroll;
    }

    private View dashboardPage() {
        ScrollView scroll = pageScroll();
        LinearLayout content = pageContent();
        scroll.addView(content);

        final Calendar today = DateTools.today();
        String date = DateTools.dateKey(today);
        ProgressTools.Stats checklist = ProgressTools.checklist(data, date);
        ProgressTools.Stats routines = ProgressTools.routines(data, today, date);
        ProgressTools.Stats monthGoals = ProgressTools.goals(
                data, Models.Goal.MONTH, DateTools.monthKey(today));
        ProgressTools.Stats yearGoals = ProgressTools.goals(
                data, Models.Goal.YEAR, DateTools.yearKey(today));
        content.addView(pageHeading("달성 대시보드", "오늘의 흐름과 장기 목표를 한눈에 확인하세요."));

        LinearLayout hero = Ui.vertical(this);
        hero.setGravity(Gravity.CENTER);
        hero.setPadding(Ui.dp(this, 18), Ui.dp(this, 14), Ui.dp(this, 18), Ui.dp(this, 14));
        hero.setBackground(Ui.outlined(Ui.SURFACE, Ui.LINE, 30, this));
        if (android.os.Build.VERSION.SDK_INT >= 21) hero.setElevation(Ui.dp(this, 2));

        final String[] pageLabels = {"오늘 체크", "반복 루틴", "한 달 목표", "1년 목표"};
        final ProgressTools.Stats[] pageStats = {checklist, routines, monthGoals, yearGoals};
        final int[] pageColors = {
                Color.rgb(244, 245, 247),
                Color.rgb(202, 206, 213),
                Color.rgb(151, 157, 167),
                Color.rgb(101, 108, 119)
        };

        final ViewFlipper pager = new ViewFlipper(this);
        for (int index = 0; index < pageLabels.length; index++) {
            pager.addView(dashboardSlide(pageLabels[index], pageStats[index], pageColors[index]));
        }
        pager.setDisplayedChild(dashboardPageIndex);
        hero.addView(pager, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 218)));

        final TextView pageDots = Ui.text(this, dashboardPageDots(), 12, Ui.MUTED);
        pageDots.setGravity(Gravity.CENTER);
        pageDots.setLetterSpacing(0.12f);
        hero.addView(pageDots, Ui.margins(Ui.matchWrap(), this, 0, 2, 0, 0));

        TextView heroCaption = Ui.caption(this, "좌우로 넘겨 항목별 달성률을 확인하세요.");
        heroCaption.setGravity(Gravity.CENTER);
        hero.addView(heroCaption, Ui.margins(Ui.matchWrap(), this, 0, 6, 0, 0));

        final float[] touchStartX = new float[1];
        final float[] touchStartY = new float[1];
        View.OnTouchListener swipeListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    touchStartX[0] = event.getX();
                    touchStartY[0] = event.getY();
                    view.getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                }
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    float horizontal = Math.abs(event.getX() - touchStartX[0]);
                    float vertical = Math.abs(event.getY() - touchStartY[0]);
                    if (vertical > horizontal && vertical > Ui.dp(MainActivity.this, 12)) {
                        view.getParent().requestDisallowInterceptTouchEvent(false);
                        return false;
                    }
                    return true;
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    view.getParent().requestDisallowInterceptTouchEvent(false);
                    float distance = event.getX() - touchStartX[0];
                    float threshold = Ui.dp(MainActivity.this, 42);
                    if (distance < -threshold) {
                        moveDashboardPage(pager, pageDots, true);
                    } else if (distance > threshold) {
                        moveDashboardPage(pager, pageDots, false);
                    }
                    return true;
                }
                if (event.getAction() == MotionEvent.ACTION_CANCEL) {
                    view.getParent().requestDisallowInterceptTouchEvent(false);
                }
                return true;
            }
        };
        pager.setOnTouchListener(swipeListener);
        content.addView(hero, Ui.margins(Ui.matchWrap(), this, 0, 20, 0, 0));

        TextView categories = Ui.sectionTitle(this, "항목별 달성률");
        content.addView(categories, Ui.margins(Ui.matchWrap(), this, 0, 24, 0, 10));

        LinearLayout firstRow = Ui.horizontal(this);
        firstRow.addView(metricCard("오늘 체크", "01", checklist, Color.rgb(244, 245, 247)),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        firstRow.addView(metricCard("반복 루틴", "02", routines, Color.rgb(202, 206, 213)),
                Ui.margins(new LinearLayout.LayoutParams(0,
                        ViewGroup.LayoutParams.WRAP_CONTENT, 1), this, 10, 0, 0, 0));
        content.addView(firstRow, Ui.matchWrap());

        LinearLayout secondRow = Ui.horizontal(this);
        secondRow.addView(metricCard("한 달 목표", "03", monthGoals, Color.rgb(151, 157, 167)),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        secondRow.addView(metricCard("1년 목표", "04", yearGoals, Color.rgb(101, 108, 119)),
                Ui.margins(new LinearLayout.LayoutParams(0,
                        ViewGroup.LayoutParams.WRAP_CONTENT, 1), this, 10, 0, 0, 0));
        content.addView(secondRow, Ui.margins(Ui.matchWrap(), this, 0, 10, 0, 0));

        ProgressTools.Stats recent = ProgressTools.recentSevenDays(data, today);
        LinearLayout recentHeader = sectionHeader("최근 7일", recent.done + " / " + recent.total + " 완료");
        content.addView(recentHeader, Ui.margins(Ui.matchWrap(), this, 0, 26, 0, 10));

        LinearLayout trendCard = Ui.card(this);
        trendCard.setBackground(Ui.outlined(Ui.SURFACE, Ui.LINE, 26, this));
        Calendar day = DateTools.copy(today);
        day.add(Calendar.DAY_OF_MONTH, -6);
        SimpleDateFormat weekdayFormat = new SimpleDateFormat("M/d E", Locale.KOREAN);
        for (int index = 0; index < 7; index++) {
            ProgressTools.Stats stats = ProgressTools.activityForDate(data, day);
            LinearLayout row = Ui.horizontal(this);
            TextView label = Ui.text(this, weekdayFormat.format(day.getTime()), 12, Ui.MUTED);
            row.addView(label, new LinearLayout.LayoutParams(Ui.dp(this, 58),
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            bar.setMax(100);
            bar.setProgress(stats.percent());
            bar.setProgressTintList(ColorStateList.valueOf(Ui.PRIMARY));
            bar.setProgressBackgroundTintList(ColorStateList.valueOf(Ui.LINE));
            row.addView(bar, new LinearLayout.LayoutParams(0, Ui.dp(this, 7), 1));
            TextView percent = Ui.text(this,
                    stats.total == 0 ? "–" : stats.percent() + "%", 12, Ui.TEXT);
            percent.setGravity(Gravity.END);
            row.addView(percent, new LinearLayout.LayoutParams(Ui.dp(this, 42),
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            trendCard.addView(row, Ui.margins(Ui.matchWrap(), this, 0,
                    index == 0 ? 0 : 12, 0, 0));
            day.add(Calendar.DAY_OF_MONTH, 1);
        }
        content.addView(trendCard, Ui.matchWrap());
        return scroll;
    }

    private LinearLayout dashboardSlide(
            String label,
            ProgressTools.Stats stats,
            int accent) {
        LinearLayout page = Ui.vertical(this);
        page.setGravity(Gravity.CENTER);

        DashboardRingView ring = new DashboardRingView(this);
        ring.setData(stats.percent(), label, accent);
        page.addView(ring, new LinearLayout.LayoutParams(
                Ui.dp(this, 158), Ui.dp(this, 158)));

        TextView count = Ui.text(this,
                stats.total == 0 ? "등록된 항목이 없습니다"
                        : stats.done + "개 완료  ·  전체 " + stats.total + "개",
                14, Ui.TEXT);
        count.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        count.setGravity(Gravity.CENTER);
        page.addView(count, Ui.margins(Ui.matchWrap(), this, 0, 8, 0, 0));
        return page;
    }

    private void moveDashboardPage(
            ViewFlipper pager,
            TextView dots,
            boolean forward) {
        int next = dashboardPageIndex + (forward ? 1 : -1);
        if (next < 0 || next > 3) return;

        float direction = forward ? 1f : -1f;
        TranslateAnimation incoming = new TranslateAnimation(
                Animation.RELATIVE_TO_PARENT, direction,
                Animation.RELATIVE_TO_PARENT, 0f,
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 0f);
        TranslateAnimation outgoing = new TranslateAnimation(
                Animation.RELATIVE_TO_PARENT, 0f,
                Animation.RELATIVE_TO_PARENT, -direction,
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 0f);
        incoming.setDuration(220);
        outgoing.setDuration(220);
        pager.setInAnimation(incoming);
        pager.setOutAnimation(outgoing);
        dashboardPageIndex = next;
        pager.setDisplayedChild(dashboardPageIndex);
        dots.setText(dashboardPageDots());
        pager.announceForAccessibility((dashboardPageIndex + 1) + "번째 달성률 페이지");
    }

    private String dashboardPageDots() {
        StringBuilder dots = new StringBuilder();
        for (int index = 0; index < 4; index++) {
            if (index > 0) dots.append("  ");
            dots.append(index == dashboardPageIndex ? "●" : "○");
        }
        return dots.toString();
    }

    private LinearLayout metricCard(
            String label,
            String number,
            ProgressTools.Stats stats,
            int accent) {
        LinearLayout card = Ui.vertical(this);
        card.setPadding(Ui.dp(this, 16), Ui.dp(this, 15), Ui.dp(this, 16), Ui.dp(this, 15));
        card.setBackground(Ui.outlined(Ui.SURFACE_RAISED, Ui.LINE, 24, this));

        LinearLayout header = Ui.horizontal(this);
        TextView marker = Ui.text(this, number, 10,
                "04".equals(number) ? Ui.TEXT : Ui.ON_PRIMARY);
        marker.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        marker.setGravity(Gravity.CENTER);
        marker.setBackground(Ui.rounded(accent, 12, this));
        header.addView(marker, new LinearLayout.LayoutParams(Ui.dp(this, 28), Ui.dp(this, 24)));
        TextView percent = Ui.text(this, stats.percent() + "%", 24, Ui.TEXT);
        percent.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        percent.setGravity(Gravity.END);
        header.addView(percent, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        card.addView(header, Ui.matchWrap());

        TextView labelView = Ui.text(this, label, 13, Ui.TEXT);
        labelView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        card.addView(labelView, Ui.margins(Ui.matchWrap(), this, 0, 14, 0, 0));
        card.addView(Ui.caption(this, stats.done + " / " + stats.total + " 완료"),
                Ui.margins(Ui.matchWrap(), this, 0, 6, 0, 0));

        ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(Math.max(1, stats.total));
        progress.setProgress(stats.done);
        progress.setProgressTintList(ColorStateList.valueOf(accent));
        progress.setProgressBackgroundTintList(ColorStateList.valueOf(Ui.LINE));
        card.addView(progress, Ui.margins(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 5)), this, 0, 11, 0, 0));
        return card;
    }

    private View calendarPage() {
        ScrollView scroll = pageScroll();
        LinearLayout content = pageContent();
        scroll.addView(content);
        content.addView(pageHeading("달력", "날짜별 완료율과 당일 메모를 확인하세요."));

        LinearLayout calendarPanel = Ui.vertical(this);
        calendarPanel.setPadding(Ui.dp(this, 12), Ui.dp(this, 12),
                Ui.dp(this, 12), Ui.dp(this, 18));
        calendarPanel.setBackground(Ui.outlined(Ui.SURFACE, Ui.LINE, 30, this));
        if (android.os.Build.VERSION.SDK_INT >= 21) calendarPanel.setElevation(Ui.dp(this, 2));

        LinearLayout monthRow = Ui.horizontal(this);
        TextView previous = Ui.iconButton(this, "‹");
        previous.setTextColor(Ui.TEXT);
        previous.setContentDescription("이전 달");
        previous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                calendarMonth.set(Calendar.DAY_OF_MONTH, 1);
                calendarMonth.add(Calendar.MONTH, -1);
                render();
            }
        });
        monthRow.addView(previous, new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 44)));

        TextView monthTitle = Ui.sectionTitle(this, DateTools.monthTitle(calendarMonth));
        monthTitle.setGravity(Gravity.CENTER);
        monthRow.addView(monthTitle, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView todayButton = Ui.text(this, "오늘", 11, Ui.ON_PRIMARY);
        todayButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        todayButton.setGravity(Gravity.CENTER);
        Ui.setClickableBackground(todayButton, Ui.PRIMARY, 13);
        todayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                calendarMonth = DateTools.today();
                render();
            }
        });
        monthRow.addView(todayButton, new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 30)));

        TextView next = Ui.iconButton(this, "›");
        next.setTextColor(Ui.TEXT);
        next.setContentDescription("다음 달");
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                calendarMonth.set(Calendar.DAY_OF_MONTH, 1);
                calendarMonth.add(Calendar.MONTH, 1);
                render();
            }
        });
        monthRow.addView(next, new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 44)));
        calendarPanel.addView(monthRow, Ui.matchWrap());

        LinearLayout weekdayRow = Ui.horizontal(this);
        String[] weekdays = {"일", "월", "화", "수", "목", "금", "토"};
        for (int column = 0; column < 7; column++) {
            TextView header = Ui.text(this, weekdays[column], 12,
                    column == 0 || column == 6 ? Ui.MUTED : Ui.TEXT);
            header.setGravity(Gravity.CENTER);
            header.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            weekdayRow.addView(header, new LinearLayout.LayoutParams(
                    0, Ui.dp(this, 38), 1));
        }
        calendarPanel.addView(weekdayRow, Ui.matchWrap());

        Calendar first = DateTools.copy(calendarMonth);
        first.set(Calendar.DAY_OF_MONTH, 1);
        int offset = first.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY;
        Calendar cellDay = DateTools.copy(first);
        cellDay.add(Calendar.DAY_OF_MONTH, -offset);
        Calendar today = DateTools.today();
        int shownMonth = calendarMonth.get(Calendar.MONTH);
        int shownYear = calendarMonth.get(Calendar.YEAR);

        for (int week = 0; week < 6; week++) {
            if (week > 0) {
                View divider = new View(this);
                divider.setBackgroundColor(Ui.LINE);
                calendarPanel.addView(divider, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 1)));
            }
            LinearLayout weekRow = Ui.horizontal(this);
            for (int column = 0; column < 7; column++) {
                final Calendar chosen = DateTools.copy(cellDay);
                boolean inMonth = chosen.get(Calendar.MONTH) == shownMonth
                        && chosen.get(Calendar.YEAR) == shownYear;
                boolean isToday = DateTools.sameDay(chosen, today);
                boolean isSelected = DateTools.sameDay(chosen, selectedDay);
                ProgressTools.Stats stats = ProgressTools.activityForDate(data, chosen);
                boolean hasMemo = ProgressTools.hasDailyMemo(data, DateTools.dateKey(chosen));

                LinearLayout cell = Ui.vertical(this);
                cell.setGravity(Gravity.CENTER);
                cell.setPadding(Ui.dp(this, 1), Ui.dp(this, 7),
                        Ui.dp(this, 1), Ui.dp(this, 5));
                cell.setAlpha(inMonth ? 1f : 0.26f);
                if (isSelected) {
                    cell.setBackground(Ui.outlined(Ui.SURFACE_RAISED, Ui.PRIMARY, 15, this));
                } else if (isToday) {
                    cell.setBackground(Ui.rounded(Ui.PRIMARY, 15, this));
                } else {
                    cell.setBackground(Ui.rounded(Color.TRANSPARENT, 15, this));
                }

                int numberColor = isToday && !isSelected ? Ui.ON_PRIMARY : Ui.TEXT;
                TextView number = Ui.text(this,
                        String.valueOf(chosen.get(Calendar.DAY_OF_MONTH)), 13, numberColor);
                number.setGravity(Gravity.CENTER);
                number.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                cell.addView(number, Ui.matchWrap());

                String meta = stats.total == 0 ? "" : stats.percent() + "%";
                if (hasMemo) meta += "·";
                TextView metaView = Ui.text(this, meta, 8.5f,
                        isToday && !isSelected ? Ui.ON_PRIMARY : Ui.MUTED);
                metaView.setGravity(Gravity.CENTER);
                cell.addView(metaView, Ui.margins(Ui.matchWrap(), this, 0, 5, 0, 0));
                cell.setContentDescription(DateTools.dateKey(chosen) + ", "
                        + (stats.total == 0 ? "기록 없음" : stats.percent() + "% 완료")
                        + (hasMemo ? ", 메모 있음" : ""));
                cell.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        selectedDay = DateTools.copy(chosen);
                        selectedTab = TAB_TODAY;
                        render();
                    }
                });
                weekRow.addView(cell, Ui.margins(new LinearLayout.LayoutParams(
                        0, Ui.dp(this, 66), 1), this, 2, 2, 2, 2));
                cellDay.add(Calendar.DAY_OF_MONTH, 1);
            }
            calendarPanel.addView(weekRow, Ui.matchWrap());
        }
        content.addView(calendarPanel, Ui.margins(Ui.matchWrap(), this, 0, 20, 0, 0));
        content.addView(Ui.caption(this, "완료율은 체크리스트와 루틴을 합산합니다.  ·  당일 메모"),
                Ui.margins(Ui.matchWrap(), this, 2, 12, 2, 0));
        return scroll;
    }

    private View notesPage() {
        FrameLayout frame = new FrameLayout(this);
        frame.setBackgroundColor(Ui.BG);
        ScrollView scroll = pageScroll();
        LinearLayout content = pageContent();
        content.setPadding(Ui.dp(this, 20), Ui.dp(this, 22), Ui.dp(this, 20), Ui.dp(this, 100));
        scroll.addView(content);
        frame.addView(scroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        content.addView(pageHeading("메모", "제목·내용·폴더·태그를 함께 검색할 수 있습니다."));
        final EditText search = Ui.input(this, "메모 검색", false);
        search.setText(noteQuery);
        search.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_search, 0, 0, 0);
        search.setCompoundDrawablePadding(Ui.dp(this, 9));
        content.addView(search, Ui.margins(Ui.matchWrap(), this, 0, 20, 0, 0));

        final LinearLayout folderArea = Ui.horizontal(this);
        populateFolderChips(folderArea);
        HorizontalScrollView folderScroll = new HorizontalScrollView(this);
        folderScroll.setHorizontalScrollBarEnabled(false);
        folderScroll.addView(folderArea);
        content.addView(folderScroll, Ui.margins(Ui.matchWrap(), this, 0, 12, 0, 0));

        LinearLayout sortRow = Ui.horizontal(this);
        sortRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView sortLabel = Ui.caption(this, "정렬");
        sortRow.addView(sortLabel, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        sortRow.addView(noteSortChip("마지막 수정순", NOTE_SORT_UPDATED));
        sortRow.addView(noteSortChip("제목순", NOTE_SORT_TITLE),
                Ui.margins(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT), this, 8, 0, 0, 0));
        content.addView(sortRow, Ui.margins(Ui.matchWrap(), this, 0, 10, 0, 0));

        final LinearLayout noteList = Ui.vertical(this);
        content.addView(noteList, Ui.margins(Ui.matchWrap(), this, 0, 20, 0, 0));
        populateNoteList(noteList);

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable editable) {
                noteQuery = editable.toString();
                populateNoteList(noteList);
            }
        });

        Button add = Ui.primaryButton(this, "+ 새 메모");
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, NoteEditorActivity.class);
                startActivity(intent);
            }
        });
        FrameLayout.LayoutParams addParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, Ui.dp(this, 52), Gravity.BOTTOM | Gravity.END);
        addParams.setMargins(Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 20));
        frame.addView(add, addParams);
        return frame;
    }


    private TextView noteSortChip(String label, final String mode) {
        final boolean active = mode.equals(noteSortMode);
        TextView chip = Ui.text(this, label, 12, active ? Ui.ON_PRIMARY : Ui.MUTED);
        chip.setGravity(Gravity.CENTER);
        chip.setTypeface(Typeface.DEFAULT, active ? Typeface.BOLD : Typeface.NORMAL);
        chip.setPadding(Ui.dp(this, 12), Ui.dp(this, 8), Ui.dp(this, 12), Ui.dp(this, 8));
        Ui.setClickableBackground(chip, active ? Ui.PRIMARY : Ui.SURFACE, 17);
        chip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mode.equals(noteSortMode)) {
                    noteSortMode = mode;
                    render();
                }
            }
        });
        return chip;
    }

    private void populateFolderChips(LinearLayout target) {
        target.removeAllViews();
        Set<String> unique = new HashSet<>();
        unique.add("전체");
        for (Models.Note note : data.notes) {
            String folder = note.folder == null || note.folder.trim().isEmpty() ? "미분류" : note.folder.trim();
            unique.add(folder);
        }
        List<String> folders = new ArrayList<>(unique);
        Collections.sort(folders, new Comparator<String>() {
            @Override
            public int compare(String left, String right) {
                if ("전체".equals(left)) return -1;
                if ("전체".equals(right)) return 1;
                return left.compareTo(right);
            }
        });
        for (final String folder : folders) {
            final boolean active = folder.equals(selectedFolder);
            TextView chip = Ui.text(this, folder, 13, active ? Ui.ON_PRIMARY : Ui.MUTED);
            chip.setGravity(Gravity.CENTER);
            chip.setTypeface(Typeface.DEFAULT, active ? Typeface.BOLD : Typeface.NORMAL);
            chip.setPadding(Ui.dp(this, 14), Ui.dp(this, 9), Ui.dp(this, 14), Ui.dp(this, 9));
            Ui.setClickableBackground(chip, active ? Ui.PRIMARY : Ui.SURFACE, 18);
            chip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    selectedFolder = folder;
                    render();
                }
            });
            target.addView(chip, Ui.margins(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT), this, 0, 0, 8, 0));
        }
    }

    private void populateNoteList(LinearLayout target) {
        target.removeAllViews();
        List<Models.Note> matches = new ArrayList<>();
        for (Models.Note note : data.notes) {
            if (note.matches(noteQuery, selectedFolder)) matches.add(note);
        }
        Collections.sort(matches, new Comparator<Models.Note>() {
            @Override
            public int compare(Models.Note first, Models.Note second) {
                if (first.pinned != second.pinned) return first.pinned ? -1 : 1;
                if (NOTE_SORT_TITLE.equals(noteSortMode)) {
                    String firstTitle = first.title.trim().isEmpty() ? "제목 없는 메모" : first.title.trim();
                    String secondTitle = second.title.trim().isEmpty() ? "제목 없는 메모" : second.title.trim();
                    int titleOrder = firstTitle.compareToIgnoreCase(secondTitle);
                    if (titleOrder != 0) return titleOrder;
                }
                return Long.compare(second.updatedAt, first.updatedAt);
            }
        });
        if (matches.isEmpty()) {
            target.addView(emptyCard(
                    data.notes.isEmpty() ? "아직 작성한 메모가 없습니다." : "검색 결과가 없습니다.",
                    data.notes.isEmpty() ? "아래의 ‘새 메모’를 눌러 작성하세요." : "검색어나 폴더를 바꿔 보세요."));
            return;
        }
        for (final Models.Note note : matches) {
            LinearLayout card = Ui.card(this);
            card.setClickable(true);
            card.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    openNote(note.id);
                }
            });

            LinearLayout top = Ui.horizontal(this);
            LinearLayout labels = Ui.vertical(this);
            String titleValue = note.title.trim().isEmpty() ? "제목 없는 메모" : note.title.trim();
            TextView title = Ui.sectionTitle(this, titleValue);
            title.setMaxLines(2);
            labels.addView(title, Ui.matchWrap());
            TextView updated = Ui.caption(this,
                    (note.pinned ? "고정됨  ·  " : "") + formatUpdated(note.updatedAt));
            labels.addView(updated, Ui.margins(Ui.matchWrap(), this, 0, 5, 0, 0));
            top.addView(labels, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            TextView pin = Ui.iconButton(this, note.pinned ? "📌" : "⌖");
            pin.setContentDescription(note.pinned ? "메모 고정 해제" : "메모 고정");
            pin.setTextColor(note.pinned ? Ui.PRIMARY : Ui.MUTED);
            pin.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    toggleNotePin(note);
                }
            });
            top.addView(pin, new LinearLayout.LayoutParams(Ui.dp(this, 42), Ui.dp(this, 42)));
            TextView menu = Ui.iconButton(this, "⋮");
            menu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showNoteMenu(note);
                }
            });
            top.addView(menu, new LinearLayout.LayoutParams(Ui.dp(this, 42), Ui.dp(this, 42)));
            card.addView(top, Ui.matchWrap());

            String preview = note.content.trim().replace('\n', ' ');
            if (!preview.isEmpty()) {
                TextView excerpt = Ui.text(this, preview, 14, Ui.MUTED);
                excerpt.setMaxLines(3);
                card.addView(excerpt, Ui.margins(Ui.matchWrap(), this, 0, 10, 0, 0));
            }

            StringBuilder meta = new StringBuilder();
            meta.append("▣ ").append(note.folder.trim().isEmpty() ? "미분류" : note.folder.trim());
            for (String tag : note.tags) meta.append("   #").append(tag);
            if (!note.photoPaths.isEmpty()) meta.append("   사진 ").append(note.photoPaths.size()).append("장");
            TextView metaView = Ui.text(this, meta.toString(), 12, Ui.PRIMARY);
            metaView.setMaxLines(2);
            card.addView(metaView, Ui.margins(Ui.matchWrap(), this, 0, 12, 0, 0));

            target.addView(card, Ui.margins(Ui.matchWrap(), this, 0, 0, 0, 11));
        }
    }

    private LinearLayout pageHeading(String title, String subtitle) {
        LinearLayout group = Ui.vertical(this);
        group.addView(Ui.title(this, title), Ui.matchWrap());
        group.addView(Ui.caption(this, subtitle), Ui.margins(Ui.matchWrap(), this, 0, 7, 0, 0));
        return group;
    }

    private ScrollView pageScroll() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setBackgroundColor(Ui.BG);
        return scroll;
    }

    private LinearLayout pageContent() {
        LinearLayout content = Ui.vertical(this);
        content.setPadding(Ui.dp(this, 20), Ui.dp(this, 22), Ui.dp(this, 20), Ui.dp(this, 32));
        return content;
    }

    private LinearLayout progressCard(int done, int total, String label) {
        LinearLayout card = Ui.card(this);
        LinearLayout row = Ui.horizontal(this);
        LinearLayout copy = Ui.vertical(this);
        TextView title = Ui.sectionTitle(this, total == 0 ? "0% 완료" : (done * 100 / total) + "% 완료");
        copy.addView(title, Ui.matchWrap());
        copy.addView(Ui.caption(this, label + "  " + done + " / " + total),
                Ui.margins(Ui.matchWrap(), this, 0, 5, 0, 0));
        row.addView(copy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView badge = Ui.text(this, done == total && total > 0 ? "완료" : "진행 중", 12,
                done == total && total > 0 ? Ui.SUCCESS : Ui.TEXT);
        badge.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(Ui.dp(this, 10), Ui.dp(this, 6), Ui.dp(this, 10), Ui.dp(this, 6));
        Ui.setRoundedBackground(badge, Ui.PRIMARY_SOFT, 13);
        row.addView(badge);
        card.addView(row, Ui.matchWrap());

        ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(Math.max(1, total));
        progress.setProgress(done);
        progress.setProgressTintList(ColorStateList.valueOf(Ui.PRIMARY));
        progress.setProgressBackgroundTintList(ColorStateList.valueOf(Ui.LINE));
        card.addView(progress, Ui.margins(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 8)), this, 0, 14, 0, 0));
        return card;
    }

    private LinearLayout sectionHeader(String title, String action) {
        LinearLayout row = Ui.horizontal(this);
        row.addView(Ui.sectionTitle(this, title), new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView actionView = Ui.text(this, action, 13, Ui.PRIMARY);
        actionView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        actionView.setGravity(Gravity.CENTER);
        actionView.setPadding(Ui.dp(this, 10), Ui.dp(this, 8), Ui.dp(this, 4), Ui.dp(this, 8));
        row.addView(actionView);
        return row;
    }

    private LinearLayout emptyCard(String title, String subtitle) {
        LinearLayout card = Ui.card(this);
        card.setGravity(Gravity.CENTER);
        TextView icon = Ui.text(this, "＋", 28, Ui.PRIMARY);
        icon.setGravity(Gravity.CENTER);
        card.addView(icon, Ui.matchWrap());
        TextView titleView = Ui.text(this, title, 15, Ui.TEXT);
        titleView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        titleView.setGravity(Gravity.CENTER);
        card.addView(titleView, Ui.margins(Ui.matchWrap(), this, 0, 9, 0, 0));
        TextView subtitleView = Ui.caption(this, subtitle);
        subtitleView.setGravity(Gravity.CENTER);
        card.addView(subtitleView, Ui.margins(Ui.matchWrap(), this, 0, 5, 0, 0));
        return card;
    }

    private LinearLayout checkItemRow(
            String value,
            boolean checked,
            boolean enabled,
            String subtitle,
            final CheckedResult checkedResult,
            View.OnClickListener detailListener,
            View.OnClickListener menuListener) {
        LinearLayout card = Ui.horizontal(this);
        card.setPadding(Ui.dp(this, 12), Ui.dp(this, 9), Ui.dp(this, 7), Ui.dp(this, 9));
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setBackground(Ui.outlined(Ui.SURFACE, Ui.LINE, 18, this));
        card.setAlpha(enabled ? 1f : 0.55f);
        card.setClickable(true);
        card.setOnClickListener(detailListener);

        CheckBox box = Ui.checkBox(this);
        box.setChecked(checked);
        box.setEnabled(enabled);
        box.setContentDescription(value);
        box.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (buttonView.isPressed()) checkedResult.accept(isChecked);
            }
        });
        card.addView(box, new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 48)));

        LinearLayout copy = Ui.vertical(this);
        TextView title = Ui.text(this, value, 15, checked ? Ui.MUTED : Ui.TEXT);
        if (checked) title.setPaintFlags(title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        copy.addView(title, Ui.matchWrap());
        if (subtitle != null && !subtitle.isEmpty()) {
            copy.addView(Ui.caption(this, subtitle), Ui.margins(Ui.matchWrap(), this, 0, 5, 0, 0));
        }
        card.addView(copy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView menu = Ui.iconButton(this, "⋮");
        menu.setOnClickListener(menuListener);
        card.addView(menu, new LinearLayout.LayoutParams(Ui.dp(this, 42), Ui.dp(this, 42)));
        return card;
    }

    private List<Models.Goal> goalsFor(String type, String period) {
        List<Models.Goal> result = new ArrayList<>();
        for (Models.Goal goal : data.goals) {
            if (type.equals(goal.type) && period.equals(goal.period)) result.add(goal);
        }
        Collections.sort(result, new Comparator<Models.Goal>() {
            @Override
            public int compare(Models.Goal first, Models.Goal second) {
                return Long.compare(first.createdAt, second.createdAt);
            }
        });
        return result;
    }

    private void showChecklistMenu(final Models.DailyEntry entry, final Models.ChecklistItem item) {
        new AlertDialog.Builder(this)
                .setItems(new String[] {"세부 메모", "이름 수정", "삭제"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            openItemDetail(
                                    ItemDetailActivity.KIND_DAILY,
                                    item.id,
                                    DateTools.dateKey(selectedDay));
                        } else if (which == 1) {
                            showTextDialog("항목 수정", item.text, "체크할 항목", new TextResult() {
                                @Override
                                public void accept(String value) {
                                    item.text = value;
                                    saveAndRender();
                                }
                            });
                        } else {
                            confirmDelete("이 항목을 삭제할까요?", new Runnable() {
                                @Override
                                public void run() {
                                    entry.items.remove(item);
                                    saveAndRender();
                                }
                            });
                        }
                    }
                }).show();
    }

    private void showGoalMenu(final Models.Goal goal) {
        new AlertDialog.Builder(this)
                .setItems(new String[] {"세부 메모", "이름 수정", "삭제"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            openItemDetail(ItemDetailActivity.KIND_GOAL, goal.id, null);
                        } else if (which == 1) {
                            showTextDialog("목표 수정", goal.text, "목표", new TextResult() {
                                @Override
                                public void accept(String value) {
                                    goal.text = value;
                                    saveAndRender();
                                }
                            });
                        } else {
                            confirmDelete("이 목표를 삭제할까요?", new Runnable() {
                                @Override
                                public void run() {
                                    data.goals.remove(goal);
                                    saveAndRender();
                                }
                            });
                        }
                    }
                }).show();
    }

    private void showRoutineMenu(final Models.Routine routine) {
        new AlertDialog.Builder(this)
                .setItems(new String[] {"세부 메모", "루틴 수정", "삭제"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            openItemDetail(ItemDetailActivity.KIND_ROUTINE, routine.id, null);
                        } else if (which == 1) showRoutineDialog(routine);
                        else confirmDelete("이 반복 루틴을 삭제할까요?", new Runnable() {
                            @Override
                            public void run() {
                                data.routines.remove(routine);
                                saveAndRender();
                            }
                        });
                    }
                }).show();
    }

    private void openItemDetail(String kind, String id, String date) {
        flushPendingMemo();
        store.save(data);
        Intent intent = new Intent(this, ItemDetailActivity.class);
        intent.putExtra(ItemDetailActivity.EXTRA_KIND, kind);
        intent.putExtra(ItemDetailActivity.EXTRA_ITEM_ID, id);
        if (date != null) intent.putExtra(ItemDetailActivity.EXTRA_DATE, date);
        startActivity(intent);
    }

    private static String detailPreview(String details) {
        if (details == null) return "";
        String value = details.trim().replace('\n', ' ');
        if (value.isEmpty()) return "";
        if (value.length() > 48) value = value.substring(0, 48) + "…";
        return "메모  ·  " + value;
    }

    private static String routineSubtitle(Models.Routine routine, boolean scheduled) {
        String value = DateTools.weekdayLabel(routine.weekdays);
        if (!scheduled) value += " · 오늘 일정 아님";
        String details = detailPreview(routine.details);
        if (!details.isEmpty()) value += "\n" + details;
        return value;
    }

    private void showRoutineDialog(final Models.Routine existing) {
        final boolean[] selected = new boolean[7];
        for (int index = 0; index < 7; index++) {
            int calendarDay = index + 1;
            selected[index] = existing == null || existing.weekdays.contains(calendarDay);
        }

        LinearLayout container = Ui.vertical(this);
        container.setPadding(Ui.dp(this, 20), Ui.dp(this, 4), Ui.dp(this, 20), Ui.dp(this, 4));
        final EditText name = Ui.input(this, "예: 영양제 먹기", false);
        if (existing != null) name.setText(existing.text);
        container.addView(name, Ui.matchWrap());
        TextView dayLabel = Ui.caption(this, "반복할 요일");
        container.addView(dayLabel, Ui.margins(Ui.matchWrap(), this, 2, 16, 0, 8));

        LinearLayout days = Ui.horizontal(this);
        String[] labels = {"일", "월", "화", "수", "목", "금", "토"};
        final List<TextView> dayViews = new ArrayList<>();
        for (int index = 0; index < labels.length; index++) {
            final int position = index;
            final TextView chip = Ui.text(this, labels[index], 13,
                    selected[index] ? Ui.ON_PRIMARY : Ui.MUTED);
            chip.setGravity(Gravity.CENTER);
            chip.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            Ui.setClickableBackground(chip, selected[index] ? Ui.PRIMARY : Ui.PRIMARY_SOFT, 16);
            chip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    selected[position] = !selected[position];
                    chip.setTextColor(selected[position] ? Ui.ON_PRIMARY : Ui.MUTED);
                    Ui.setClickableBackground(chip,
                            selected[position] ? Ui.PRIMARY : Ui.PRIMARY_SOFT, 16);
                }
            });
            dayViews.add(chip);
            days.addView(chip, Ui.margins(new LinearLayout.LayoutParams(
                    0, Ui.dp(this, 38), 1), this, index == 0 ? 0 : 3, 0, 0, 0));
        }
        container.addView(days, Ui.matchWrap());

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(existing == null ? "루틴 추가" : "루틴 수정")
                .setView(container)
                .setNegativeButton("취소", null)
                .setPositiveButton(existing == null ? "추가" : "저장", null)
                .create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface ignored) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String value = name.getText().toString().trim();
                        if (value.isEmpty()) {
                            name.setError("루틴 이름을 입력해 주세요.");
                            return;
                        }
                        boolean any = false;
                        for (boolean day : selected) if (day) any = true;
                        if (!any) {
                            Toast.makeText(MainActivity.this, "반복할 요일을 하나 이상 선택해 주세요.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Models.Routine routine = existing == null ? new Models.Routine() : existing;
                        routine.text = value;
                        routine.weekdays.clear();
                        for (int index = 0; index < selected.length; index++) {
                            if (selected[index]) routine.weekdays.add(index + 1);
                        }
                        if (existing == null) data.routines.add(routine);
                        saveAndRender();
                        dialog.dismiss();
                    }
                });
            }
        });
        dialog.getWindow();
        dialog.show();
    }

    private void showNoteMenu(final Models.Note note) {
        final String pinLabel = note.pinned ? "고정 해제" : "메모 고정";
        new AlertDialog.Builder(this)
                .setItems(new String[] {"열기", pinLabel, "삭제"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            openNote(note.id);
                        } else if (which == 1) {
                            toggleNotePin(note);
                        } else {
                            confirmDelete("이 메모와 첨부 사진을 삭제할까요?", new Runnable() {
                                @Override
                                public void run() {
                                    deleteNote(note);
                                    saveAndRender();
                                }
                            });
                        }
                    }
                }).show();
    }

    private void toggleNotePin(Models.Note note) {
        note.pinned = !note.pinned;
        if (store.save(data)) {
            TodayWidgetProvider.updateAll(this);
            render();
        } else {
            note.pinned = !note.pinned;
            Toast.makeText(this, "메모 고정 상태를 저장하지 못했습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void openNote(String noteId) {
        Intent intent = new Intent(this, NoteEditorActivity.class);
        intent.putExtra(NoteEditorActivity.EXTRA_NOTE_ID, noteId);
        startActivity(intent);
    }

    private void deleteNote(Models.Note note) {
        for (String path : note.photoPaths) PhotoStore.delete(path);
        data.notes.remove(note);
    }

    private void showTextDialog(
            String title,
            String initial,
            String hint,
            final TextResult result) {
        LinearLayout container = Ui.vertical(this);
        container.setPadding(Ui.dp(this, 20), Ui.dp(this, 4), Ui.dp(this, 20), Ui.dp(this, 4));
        final EditText input = Ui.input(this, hint, false);
        input.setText(initial);
        input.setSelection(input.length());
        container.addView(input, Ui.matchWrap());
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(container)
                .setNegativeButton("취소", null)
                .setPositiveButton("저장", null)
                .create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface ignored) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String value = input.getText().toString().trim();
                        if (value.isEmpty()) {
                            input.setError("내용을 입력해 주세요.");
                            return;
                        }
                        result.accept(value);
                        dialog.dismiss();
                    }
                });
            }
        });
        dialog.show();
    }

    private void confirmDelete(String message, final Runnable action) {
        new AlertDialog.Builder(this)
                .setTitle("삭제 확인")
                .setMessage(message)
                .setNegativeButton("취소", null)
                .setPositiveButton("삭제", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        action.run();
                    }
                }).show();
    }

    private void scheduleMemoSave(final TextView state) {
        if (pendingMemoSave != null) saveHandler.removeCallbacks(pendingMemoSave);
        state.setText("저장 중…");
        pendingMemoSave = new Runnable() {
            @Override
            public void run() {
                if (store.save(data)) state.setText("자동 저장됨");
                else state.setText("저장 실패");
                pendingMemoSave = null;
            }
        };
        saveHandler.postDelayed(pendingMemoSave, 650);
    }

    private void flushPendingMemo() {
        if (pendingMemoSave != null) {
            saveHandler.removeCallbacks(pendingMemoSave);
            pendingMemoSave = null;
            store.save(data);
        }
    }

    private void saveAndRender() {
        flushPendingMemo();
        if (!store.save(data)) {
            Toast.makeText(this, store.getLastError(), Toast.LENGTH_LONG).show();
        }
        render();
    }

    private void showLoadErrorIfNeeded() {
        if (store != null && !store.getLastError().isEmpty()) {
            Toast.makeText(this, store.getLastError(), Toast.LENGTH_LONG).show();
        }
    }

    private static String formatUpdated(long time) {
        return new SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREAN).format(time);
    }

    private interface TextResult {
        void accept(String value);
    }

    private interface CheckedResult {
        void accept(boolean checked);
    }
}
