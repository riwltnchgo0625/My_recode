package com.myrecord.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class ItemDetailActivity extends Activity {
    static final String EXTRA_KIND = "detail_kind";
    static final String EXTRA_ITEM_ID = "detail_item_id";
    static final String EXTRA_DATE = "detail_date";
    static final String KIND_DAILY = "daily";
    static final String KIND_ROUTINE = "routine";
    static final String KIND_GOAL = "goal";

    private DataStore store;
    private Models.AppData data;
    private String kind;
    private String itemId;
    private String date;
    private Models.ChecklistItem dailyItem;
    private Models.Routine routine;
    private Models.Goal goal;
    private EditText titleInput;
    private EditText detailsInput;
    private CheckBox doneInput;
    private TextView saveState;
    private boolean loading = true;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pendingSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(Ui.BG);
        getWindow().getDecorView().setSystemUiVisibility(0);

        store = new DataStore(this);
        data = store.load();
        Intent intent = getIntent();
        kind = intent.getStringExtra(EXTRA_KIND);
        itemId = intent.getStringExtra(EXTRA_ITEM_ID);
        date = intent.getStringExtra(EXTRA_DATE);
        if (!resolveItem()) {
            Toast.makeText(this, "항목을 찾지 못했습니다.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        buildScreen();
        populate();
        loading = false;
    }

    @Override
    protected void onPause() {
        flushSave();
        saveNow();
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        flushSave();
        saveNow();
        finish();
    }

    private boolean resolveItem() {
        if (kind == null || itemId == null) return false;
        if (KIND_DAILY.equals(kind)) {
            if (date == null) return false;
            for (Models.DailyEntry entry : data.dailyEntries) {
                if (!date.equals(entry.date)) continue;
                for (Models.ChecklistItem item : entry.items) {
                    if (itemId.equals(item.id)) {
                        dailyItem = item;
                        return true;
                    }
                }
            }
        } else if (KIND_ROUTINE.equals(kind)) {
            for (Models.Routine item : data.routines) {
                if (itemId.equals(item.id)) {
                    routine = item;
                    return true;
                }
            }
        } else if (KIND_GOAL.equals(kind)) {
            for (Models.Goal item : data.goals) {
                if (itemId.equals(item.id)) {
                    goal = item;
                    return true;
                }
            }
        }
        return false;
    }

    private void buildScreen() {
        LinearLayout root = Ui.vertical(this);
        root.setBackgroundColor(Ui.BG);

        LinearLayout toolbar = Ui.horizontal(this);
        toolbar.setPadding(Ui.dp(this, 10), Ui.dp(this, 8), Ui.dp(this, 18), Ui.dp(this, 8));
        toolbar.setBackgroundColor(Ui.SURFACE);
        TextView back = Ui.iconButton(this, "‹");
        back.setTextSize(31);
        back.setContentDescription("뒤로가기");
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        toolbar.addView(back, new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 48)));

        LinearLayout heading = Ui.vertical(this);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = Ui.sectionTitle(this, "항목 상세");
        heading.addView(title, Ui.matchWrap());
        saveState = Ui.caption(this, "세부 메모는 자동 저장됩니다.");
        heading.addView(saveState, Ui.margins(Ui.matchWrap(), this, 0, 4, 0, 0));
        toolbar.addView(heading, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
        root.addView(toolbar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 66)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout content = Ui.vertical(this);
        content.setPadding(Ui.dp(this, 20), Ui.dp(this, 24), Ui.dp(this, 20), Ui.dp(this, 40));

        TextView context = Ui.text(this, contextLabel(), 13, Ui.MUTED);
        context.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        context.setLetterSpacing(0.02f);
        content.addView(context, Ui.matchWrap());

        LinearLayout doneCard = Ui.card(this);
        doneCard.setOrientation(LinearLayout.HORIZONTAL);
        doneCard.setGravity(Gravity.CENTER_VERTICAL);
        doneInput = Ui.checkBox(this);
        doneInput.setContentDescription("완료 상태");
        doneCard.addView(doneInput, new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 48)));
        LinearLayout doneCopy = Ui.vertical(this);
        TextView doneTitle = Ui.sectionTitle(this, "완료 상태");
        doneCopy.addView(doneTitle, Ui.matchWrap());
        doneCopy.addView(Ui.caption(this, "체크하면 목록과 위젯에도 바로 반영됩니다."),
                Ui.margins(Ui.matchWrap(), this, 0, 5, 0, 0));
        doneCard.addView(doneCopy, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        content.addView(doneCard, Ui.margins(Ui.matchWrap(), this, 0, 14, 0, 0));

        content.addView(fieldLabel("항목 이름"), Ui.margins(Ui.matchWrap(), this, 0, 26, 0, 0));
        titleInput = Ui.input(this, "항목 이름", false);
        titleInput.setTextSize(18);
        titleInput.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        content.addView(titleInput, Ui.margins(Ui.matchWrap(), this, 0, 9, 0, 0));

        content.addView(fieldLabel("세부 메모"), Ui.margins(Ui.matchWrap(), this, 0, 24, 0, 0));
        detailsInput = Ui.input(this,
                "예: 다이소에서 살 물건, 준비할 것, 참고할 내용을 적어보세요.", true);
        detailsInput.setMinHeight(Ui.dp(this, 280));
        detailsInput.setTextSize(16);
        content.addView(detailsInput, Ui.margins(Ui.matchWrap(), this, 0, 9, 0, 0));

        TextView helper = Ui.caption(this, "내용을 입력하고 잠시 기다리거나 뒤로 가면 자동 저장됩니다.");
        helper.setGravity(Gravity.END);
        content.addView(helper, Ui.margins(Ui.matchWrap(), this, 0, 9, 2, 0));

        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(root);

        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable editable) {
                if (!loading) scheduleSave();
            }
        };
        titleInput.addTextChangedListener(watcher);
        detailsInput.addTextChangedListener(watcher);
        doneInput.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton button, boolean checked) {
                if (!loading && button.isPressed()) {
                    setDone(checked);
                    if (store.save(data)) saveState.setText("자동 저장됨");
                    else saveState.setText("저장 실패");
                }
            }
        });
    }

    private TextView fieldLabel(String label) {
        TextView view = Ui.text(this, label, 13, Ui.MUTED);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setLetterSpacing(0.02f);
        return view;
    }

    private void populate() {
        titleInput.setText(getItemTitle());
        detailsInput.setText(getDetails());
        doneInput.setChecked(isDone());
    }

    private void scheduleSave() {
        if (pendingSave != null) handler.removeCallbacks(pendingSave);
        saveState.setText("저장 중…");
        pendingSave = new Runnable() {
            @Override
            public void run() {
                saveState.setText(saveNow() ? "자동 저장됨" : "저장 실패");
                pendingSave = null;
            }
        };
        handler.postDelayed(pendingSave, 600);
    }

    private void flushSave() {
        if (pendingSave != null) {
            handler.removeCallbacks(pendingSave);
            pendingSave = null;
        }
    }

    private boolean saveNow() {
        if (titleInput == null || detailsInput == null) return false;
        String title = titleInput.getText().toString().trim();
        if (!title.isEmpty()) setItemTitle(title);
        setDetails(detailsInput.getText().toString());
        boolean saved = store.save(data);
        if (!saved && saveState != null) saveState.setText("저장 실패");
        return saved;
    }

    private String getItemTitle() {
        if (dailyItem != null) return dailyItem.text;
        if (routine != null) return routine.text;
        return goal == null ? "" : goal.text;
    }

    private void setItemTitle(String value) {
        if (dailyItem != null) dailyItem.text = value;
        else if (routine != null) routine.text = value;
        else if (goal != null) goal.text = value;
    }

    private String getDetails() {
        if (dailyItem != null) return dailyItem.details;
        if (routine != null) return routine.details;
        return goal == null ? "" : goal.details;
    }

    private void setDetails(String value) {
        if (dailyItem != null) dailyItem.details = value;
        else if (routine != null) routine.details = value;
        else if (goal != null) goal.details = value;
    }

    private boolean isDone() {
        if (dailyItem != null) return dailyItem.done;
        if (routine != null) return routine.doneDates.contains(DateTools.dateKey(DateTools.today()));
        return goal != null && goal.done;
    }

    private void setDone(boolean value) {
        if (dailyItem != null) dailyItem.done = value;
        else if (routine != null) {
            String today = DateTools.dateKey(DateTools.today());
            if (value) routine.doneDates.add(today);
            else routine.doneDates.remove(today);
        } else if (goal != null) goal.done = value;
    }

    private String contextLabel() {
        if (dailyItem != null) return dateLabel(date) + " · 당일 체크리스트";
        if (routine != null) return "반복 루틴 · " + DateTools.weekdayLabel(routine.weekdays);
        if (goal != null && Models.Goal.MONTH.equals(goal.type)) {
            String[] parts = goal.period.split("-");
            if (parts.length == 2) {
                try {
                    return parts[0] + "년 " + Integer.parseInt(parts[1]) + "월 · 한 달 목표";
                } catch (NumberFormatException ignored) {}
            }
            return goal.period + " · 한 달 목표";
        }
        return (goal == null ? "" : goal.period) + "년 · 1년 목표";
    }

    private static String dateLabel(String key) {
        if (key == null) return "날짜 미지정";
        String[] parts = key.split("-");
        if (parts.length == 3) {
            try {
                return parts[0] + "년 " + Integer.parseInt(parts[1]) + "월 "
                        + Integer.parseInt(parts[2]) + "일";
            } catch (NumberFormatException ignored) {}
        }
        return key;
    }
}
