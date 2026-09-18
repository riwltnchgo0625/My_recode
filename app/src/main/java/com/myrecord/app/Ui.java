package com.myrecord.app;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

final class Ui {
    static final int BG = Color.rgb(10, 11, 13);
    static final int SURFACE = Color.rgb(20, 22, 26);
    static final int SURFACE_RAISED = Color.rgb(27, 30, 35);
    static final int PRIMARY = Color.rgb(232, 234, 238);
    static final int ON_PRIMARY = Color.rgb(16, 17, 20);
    static final int PRIMARY_SOFT = Color.rgb(38, 41, 47);
    static final int TEXT = Color.rgb(244, 245, 247);
    static final int MUTED = Color.rgb(157, 163, 173);
    static final int LINE = Color.rgb(47, 51, 58);
    static final int SUCCESS = Color.rgb(210, 213, 219);
    static final int DANGER = Color.rgb(105, 108, 114);
    static final int WARM = Color.rgb(31, 34, 39);

    private Ui() {}

    static int dp(Context context, float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    static int sp(Context context, float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().scaledDensity);
    }

    static LinearLayout vertical(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    static LinearLayout horizontal(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        return layout;
    }

    static TextView text(Context context, String value, float sizeSp, int color) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        view.setIncludeFontPadding(false);
        view.setLineSpacing(0, 1.08f);
        view.setFontFeatureSettings("kern");
        return view;
    }

    static TextView title(Context context, String value) {
        TextView view = text(context, value, 25, TEXT);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setLetterSpacing(-0.015f);
        return view;
    }

    static TextView sectionTitle(Context context, String value) {
        TextView view = text(context, value, 17, TEXT);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setLetterSpacing(-0.01f);
        return view;
    }

    static TextView caption(Context context, String value) {
        return text(context, value, 13, MUTED);
    }

    static GradientDrawable rounded(int color, float radiusDp, Context context) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(context, radiusDp));
        return drawable;
    }

    static GradientDrawable outlined(int fill, int stroke, float radiusDp, Context context) {
        GradientDrawable drawable = rounded(fill, radiusDp, context);
        drawable.setStroke(dp(context, 1), stroke);
        return drawable;
    }

    static void setRoundedBackground(View view, int color, float radiusDp) {
        view.setBackground(rounded(color, radiusDp, view.getContext()));
    }

    static void setClickableBackground(View view, int color, float radiusDp) {
        GradientDrawable content = rounded(color, radiusDp, view.getContext());
        if (Build.VERSION.SDK_INT >= 21) {
            int rippleColor = Color.argb(44, 235, 237, 240);
            view.setBackground(new RippleDrawable(ColorStateList.valueOf(rippleColor), content, null));
        } else {
            view.setBackground(content);
        }
    }

    static LinearLayout card(Context context) {
        LinearLayout card = vertical(context);
        card.setPadding(dp(context, 18), dp(context, 17), dp(context, 18), dp(context, 17));
        card.setBackground(outlined(SURFACE, LINE, 24, context));
        if (Build.VERSION.SDK_INT >= 21) {
            card.setElevation(dp(context, 1));
        }
        return card;
    }

    static Button primaryButton(Context context, String label) {
        Button button = new Button(context);
        button.setText(label);
        button.setTextSize(14);
        button.setTextColor(ON_PRIMARY);
        button.setAllCaps(false);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(context, 16), dp(context, 8), dp(context, 16), dp(context, 8));
        button.setMinHeight(0);
        button.setMinWidth(0);
        setClickableBackground(button, PRIMARY, 14);
        return button;
    }

    static Button softButton(Context context, String label) {
        Button button = new Button(context);
        button.setText(label);
        button.setTextSize(14);
        button.setTextColor(PRIMARY);
        button.setAllCaps(false);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(context, 14), dp(context, 7), dp(context, 14), dp(context, 7));
        button.setMinHeight(0);
        button.setMinWidth(0);
        setClickableBackground(button, PRIMARY_SOFT, 13);
        return button;
    }

    static TextView iconButton(Context context, String icon) {
        TextView view = text(context, icon, 20, MUTED);
        view.setGravity(Gravity.CENTER);
        view.setMinWidth(dp(context, 40));
        view.setMinHeight(dp(context, 40));
        view.setPadding(dp(context, 8), dp(context, 8), dp(context, 8), dp(context, 8));
        setClickableBackground(view, Color.TRANSPARENT, 20);
        return view;
    }

    static EditText input(Context context, String hint, boolean multiline) {
        EditText edit = new EditText(context);
        edit.setHint(hint);
        edit.setHintTextColor(Color.rgb(105, 110, 120));
        edit.setTextColor(TEXT);
        edit.setTextSize(15);
        edit.setPadding(dp(context, 14), dp(context, 12), dp(context, 14), dp(context, 12));
        edit.setSingleLine(!multiline);
        if (multiline) {
            edit.setGravity(Gravity.TOP | Gravity.START);
            edit.setMinLines(4);
        }
        edit.setBackground(outlined(SURFACE, LINE, 14, context));
        return edit;
    }

    static CheckBox checkBox(Context context) {
        CheckBox box = new CheckBox(context);
        box.setButtonTintList(new ColorStateList(
                new int[][] {new int[] {android.R.attr.state_checked}, new int[] {}},
                new int[] {PRIMARY, Color.rgb(105, 110, 120)}));
        box.setMinWidth(dp(context, 42));
        box.setGravity(Gravity.CENTER);
        return box;
    }

    static LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    static LinearLayout.LayoutParams weight(float value) {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, value);
    }

    static LinearLayout.LayoutParams weightMatch(float value) {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, value);
    }

    static LinearLayout.LayoutParams margins(
            LinearLayout.LayoutParams params,
            Context context,
            int left,
            int top,
            int right,
            int bottom) {
        params.setMargins(dp(context, left), dp(context, top), dp(context, right), dp(context, bottom));
        return params;
    }

    static void hideKeyboard(Activity activity) {
        View focused = activity.getCurrentFocus();
        if (focused != null) {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(focused.getWindowToken(), 0);
            }
            focused.clearFocus();
        }
    }
}
