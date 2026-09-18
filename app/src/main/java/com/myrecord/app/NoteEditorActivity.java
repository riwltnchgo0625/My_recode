package com.myrecord.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NoteEditorActivity extends Activity {
    static final String EXTRA_NOTE_ID = "note_id";
    private static final int REQUEST_PHOTO = 410;

    private DataStore store;
    private Models.AppData data;
    private Models.Note note;
    private boolean newNote;
    private boolean deleted;
    private EditText titleInput;
    private EditText folderInput;
    private EditText tagsInput;
    private EditText contentInput;
    private TextView saveState;
    private LinearLayout photoStrip;
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
        String noteId = getIntent().getStringExtra(EXTRA_NOTE_ID);
        note = data.note(noteId);
        if (note == null) {
            note = new Models.Note();
            newNote = true;
        }
        buildScreen();
        populateFields();
        attachAutoSave();

        if (!store.getLastError().isEmpty()) {
            Toast.makeText(this, store.getLastError(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onPause() {
        flushSave();
        if (!deleted) saveNow(false);
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        closeEditor();
    }

    private void buildScreen() {
        LinearLayout root = Ui.vertical(this);
        root.setBackgroundColor(Ui.BG);

        LinearLayout toolbar = Ui.horizontal(this);
        toolbar.setPadding(Ui.dp(this, 10), Ui.dp(this, 8), Ui.dp(this, 10), Ui.dp(this, 8));
        toolbar.setBackgroundColor(Ui.SURFACE);

        TextView back = Ui.iconButton(this, "‹");
        back.setTextSize(31);
        back.setContentDescription("뒤로가기");
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeEditor();
            }
        });
        toolbar.addView(back, new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 48)));

        LinearLayout toolbarCopy = Ui.vertical(this);
        toolbarCopy.setGravity(Gravity.CENTER_VERTICAL);
        TextView heading = Ui.sectionTitle(this, newNote ? "새 메모" : "메모 편집");
        toolbarCopy.addView(heading, Ui.matchWrap());
        saveState = Ui.caption(this, "입력하면 자동 저장됩니다.");
        toolbarCopy.addView(saveState, Ui.margins(Ui.matchWrap(), this, 0, 4, 0, 0));
        toolbar.addView(toolbarCopy, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.MATCH_PARENT, 1));

        TextView delete = Ui.iconButton(this, "⌫");
        delete.setTextColor(Ui.DANGER);
        delete.setContentDescription("메모 삭제");
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                confirmDelete();
            }
        });
        toolbar.addView(delete, new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 48)));
        root.addView(toolbar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 66)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout content = Ui.vertical(this);
        content.setPadding(Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 38));

        content.addView(fieldLabel("제목"));
        titleInput = Ui.input(this, "메모 제목", false);
        titleInput.setTextSize(19);
        titleInput.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        content.addView(titleInput, Ui.margins(Ui.matchWrap(), this, 0, 8, 0, 0));

        LinearLayout metaRow = Ui.horizontal(this);
        LinearLayout folderGroup = Ui.vertical(this);
        folderGroup.addView(fieldLabel("폴더"));
        folderInput = Ui.input(this, "예: 업무", false);
        folderGroup.addView(folderInput, Ui.margins(Ui.matchWrap(), this, 0, 8, 0, 0));
        metaRow.addView(folderGroup, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        LinearLayout tagsGroup = Ui.vertical(this);
        tagsGroup.addView(fieldLabel("태그"));
        tagsInput = Ui.input(this, "예: 아이디어, 중요", false);
        tagsGroup.addView(tagsInput, Ui.margins(Ui.matchWrap(), this, 0, 8, 0, 0));
        metaRow.addView(tagsGroup, Ui.margins(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1), this, 10, 0, 0, 0));
        content.addView(metaRow, Ui.margins(Ui.matchWrap(), this, 0, 18, 0, 0));

        content.addView(fieldLabel("내용"), Ui.margins(Ui.matchWrap(), this, 0, 20, 0, 0));
        contentInput = Ui.input(this, "내용을 입력하세요.", true);
        contentInput.setMinHeight(Ui.dp(this, 310));
        contentInput.setTextSize(16);
        content.addView(contentInput, Ui.margins(Ui.matchWrap(), this, 0, 8, 0, 0));

        LinearLayout photoTitle = Ui.horizontal(this);
        photoTitle.addView(Ui.sectionTitle(this, "첨부 사진"), new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Button addPhoto = Ui.softButton(this, "+ 사진 추가");
        addPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                choosePhoto();
            }
        });
        photoTitle.addView(addPhoto);
        content.addView(photoTitle, Ui.margins(Ui.matchWrap(), this, 0, 24, 0, 10));

        photoStrip = Ui.horizontal(this);
        HorizontalScrollView photos = new HorizontalScrollView(this);
        photos.setHorizontalScrollBarEnabled(false);
        photos.addView(photoStrip);
        content.addView(photos, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 126)));

        TextView privacy = Ui.caption(this, "메모와 사진은 이 휴대폰의 앱 내부에만 저장됩니다.");
        privacy.setGravity(Gravity.CENTER);
        content.addView(privacy, Ui.margins(Ui.matchWrap(), this, 0, 22, 0, 0));

        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(root);
    }

    private TextView fieldLabel(String label) {
        TextView view = Ui.text(this, label, 13, Ui.MUTED);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private void populateFields() {
        titleInput.setText(note.title);
        folderInput.setText("미분류".equals(note.folder) ? "" : note.folder);
        StringBuilder tags = new StringBuilder();
        for (String tag : note.tags) {
            if (tags.length() > 0) tags.append(", ");
            tags.append(tag);
        }
        tagsInput.setText(tags.toString());
        contentInput.setText(note.content);
        renderPhotos();
    }

    private void attachAutoSave() {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable editable) {
                updateNoteFromFields();
                scheduleSave();
            }
        };
        titleInput.addTextChangedListener(watcher);
        folderInput.addTextChangedListener(watcher);
        tagsInput.addTextChangedListener(watcher);
        contentInput.addTextChangedListener(watcher);
    }

    private void updateNoteFromFields() {
        note.title = titleInput.getText().toString();
        note.content = contentInput.getText().toString();
        String folder = folderInput.getText().toString().trim();
        note.folder = folder.isEmpty() ? "미분류" : folder;
        note.tags.clear();
        Set<String> unique = new HashSet<>();
        String raw = tagsInput.getText().toString().replace('#', ',');
        String[] pieces = raw.split(",");
        for (String piece : pieces) {
            String value = piece.trim();
            if (!value.isEmpty() && unique.add(value)) note.tags.add(value);
        }
        note.updatedAt = System.currentTimeMillis();
    }

    private void scheduleSave() {
        if (pendingSave != null) handler.removeCallbacks(pendingSave);
        saveState.setText("저장 중…");
        pendingSave = new Runnable() {
            @Override
            public void run() {
                boolean saved = saveNow(false);
                saveState.setText(saved ? "자동 저장됨" : "저장 실패");
                pendingSave = null;
            }
        };
        handler.postDelayed(pendingSave, 650);
    }

    private void flushSave() {
        if (pendingSave != null) {
            handler.removeCallbacks(pendingSave);
            pendingSave = null;
            saveNow(false);
        }
    }

    private boolean saveNow(boolean allowBlank) {
        updateNoteFromFields();
        if (note.isBlank() && newNote) {
            boolean wasStored = data.notes.remove(note);
            return !wasStored || store.save(data);
        }
        if (!data.notes.contains(note)) data.notes.add(note);
        boolean saved = store.save(data);
        if (!saved) saveState.setText("저장 실패");
        return saved;
    }

    private void choosePhoto() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_PHOTO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent result) {
        super.onActivityResult(requestCode, resultCode, result);
        if (requestCode != REQUEST_PHOTO || resultCode != RESULT_OK || result == null) return;
        final Uri uri = result.getData();
        if (uri == null) return;
        saveState.setText("사진 저장 중…");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String path = PhotoStore.importPhoto(NoteEditorActivity.this, uri);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            note.photoPaths.add(path);
                            note.updatedAt = System.currentTimeMillis();
                            saveNow(true);
                            renderPhotos();
                            saveState.setText("사진 저장됨");
                        }
                    });
                } catch (final Exception error) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            saveState.setText("사진 저장 실패");
                            Toast.makeText(NoteEditorActivity.this,
                                    error.getMessage() == null ? "사진을 첨부하지 못했습니다." : error.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void renderPhotos() {
        photoStrip.removeAllViews();
        if (note.photoPaths.isEmpty()) {
            LinearLayout empty = Ui.card(this);
            empty.setGravity(Gravity.CENTER);
            TextView text = Ui.caption(this, "첨부된 사진이 없습니다.");
            text.setGravity(Gravity.CENTER);
            empty.addView(text, Ui.matchWrap());
            photoStrip.addView(empty, new LinearLayout.LayoutParams(
                    Ui.dp(this, 220), Ui.dp(this, 112)));
            return;
        }
        final List<String> paths = new ArrayList<>(note.photoPaths);
        for (final String path : paths) {
            FrameLayout frame = new FrameLayout(this);
            Ui.setRoundedBackground(frame, Ui.SURFACE, 15);
            ImageView image = new ImageView(this);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Bitmap bitmap = PhotoStore.thumbnail(path, Ui.dp(this, 120));
            if (bitmap != null) image.setImageBitmap(bitmap);
            else image.setImageResource(android.R.drawable.ic_menu_report_image);
            frame.addView(image, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));

            TextView remove = Ui.text(this, "×", 20, Color.WHITE);
            remove.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            remove.setGravity(Gravity.CENTER);
            Ui.setClickableBackground(remove, Ui.DANGER, 15);
            remove.setContentDescription("사진 삭제");
            remove.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    confirmPhotoDelete(path);
                }
            });
            FrameLayout.LayoutParams removeParams = new FrameLayout.LayoutParams(
                    Ui.dp(this, 32), Ui.dp(this, 32), Gravity.TOP | Gravity.END);
            removeParams.setMargins(0, Ui.dp(this, 5), Ui.dp(this, 5), 0);
            frame.addView(remove, removeParams);

            photoStrip.addView(frame, Ui.margins(new LinearLayout.LayoutParams(
                    Ui.dp(this, 112), Ui.dp(this, 112)), this, 0, 0, 10, 0));
        }
    }

    private void confirmPhotoDelete(final String path) {
        new AlertDialog.Builder(this)
                .setTitle("사진 삭제")
                .setMessage("이 메모에서 사진을 삭제할까요?")
                .setNegativeButton("취소", null)
                .setPositiveButton("삭제", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        note.photoPaths.remove(path);
                        PhotoStore.delete(path);
                        note.updatedAt = System.currentTimeMillis();
                        saveNow(true);
                        renderPhotos();
                    }
                }).show();
    }

    private void confirmDelete() {
        if (newNote && note.isBlank()) {
            data.notes.remove(note);
            store.save(data);
            deleted = true;
            finish();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("메모 삭제")
                .setMessage("메모와 첨부한 사진을 모두 삭제할까요?")
                .setNegativeButton("취소", null)
                .setPositiveButton("삭제", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        flushSave();
                        for (String path : note.photoPaths) PhotoStore.delete(path);
                        data.notes.remove(note);
                        store.save(data);
                        deleted = true;
                        finish();
                    }
                }).show();
    }

    private void closeEditor() {
        flushSave();
        if (newNote && note.isBlank()) {
            data.notes.remove(note);
            store.save(data);
        } else {
            saveNow(true);
        }
        finish();
    }
}
