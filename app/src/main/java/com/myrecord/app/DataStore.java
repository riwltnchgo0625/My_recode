package com.myrecord.app;

import android.content.Context;
import android.util.AtomicFile;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

final class DataStore {
    private static final String FILE_NAME = "my_record_data.json";
    private final Context appContext;
    private final File dataFile;
    private final AtomicFile atomicFile;
    private String lastError = "";

    DataStore(Context context) {
        appContext = context.getApplicationContext();
        dataFile = new File(appContext.getFilesDir(), FILE_NAME);
        atomicFile = new AtomicFile(dataFile);
    }

    synchronized Models.AppData load() {
        lastError = "";
        if (!dataFile.exists()) return new Models.AppData();
        try {
            String json = readUtf8(atomicFile.openRead());
            return Models.AppData.fromJson(new JSONObject(json));
        } catch (Exception error) {
            preserveUnreadableFile();
            lastError = "저장된 데이터를 읽지 못해 손상 파일을 별도로 보존하고 새 기록을 시작합니다.";
            return new Models.AppData();
        }
    }

    synchronized boolean save(Models.AppData data) {
        lastError = "";
        FileOutputStream output = null;
        try {
            byte[] bytes = data.toJson().toString().getBytes(StandardCharsets.UTF_8);
            output = atomicFile.startWrite();
            output.write(bytes);
            output.flush();
            atomicFile.finishWrite(output);
            output = null;
            try {
                TodayWidgetProvider.updateAll(appContext);
            } catch (RuntimeException ignored) {
                // A launcher widget failure must not turn a successful data write into a failure.
            }
            return true;
        } catch (Exception error) {
            if (output != null) atomicFile.failWrite(output);
            lastError = "저장하지 못했습니다: " + error.getMessage();
            return false;
        }
    }

    String getLastError() {
        return lastError;
    }

    private static String readUtf8(FileInputStream input) throws IOException {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            input.close();
        }
    }

    private void preserveUnreadableFile() {
        if (!dataFile.exists()) return;
        File preserved = new File(dataFile.getParentFile(),
                "my_record_data_unreadable_" + System.currentTimeMillis() + ".json");
        if (!dataFile.renameTo(preserved)) return;
        File atomicBackup = new File(dataFile.getPath() + ".bak");
        if (atomicBackup.exists()) atomicBackup.delete();
    }
}
