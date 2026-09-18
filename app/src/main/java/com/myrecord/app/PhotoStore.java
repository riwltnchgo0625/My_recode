package com.myrecord.app;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

final class PhotoStore {
    private PhotoStore() {}

    static String importPhoto(Context context, Uri source) throws Exception {
        File directory = new File(context.getFilesDir(), "photos");
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IllegalStateException("사진 저장 폴더를 만들 수 없습니다.");
        }
        File destination = new File(directory, Models.id() + ".img");
        ContentResolver resolver = context.getContentResolver();
        InputStream input = resolver.openInputStream(source);
        if (input == null) throw new IllegalStateException("사진을 열 수 없습니다.");
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(destination);
            byte[] buffer = new byte[16 * 1024];
            int count;
            long total = 0;
            while ((count = input.read(buffer)) != -1) {
                total += count;
                if (total > 30L * 1024L * 1024L) {
                    throw new IllegalArgumentException("30MB 이하 사진만 첨부할 수 있습니다.");
                }
                output.write(buffer, 0, count);
            }
            output.flush();
        } catch (Exception error) {
            destination.delete();
            throw error;
        } finally {
            try {
                input.close();
            } finally {
                if (output != null) output.close();
            }
        }
        return destination.getAbsolutePath();
    }

    static Bitmap thumbnail(String path, int maxPixels) {
        File file = new File(path);
        if (!file.exists()) return null;
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, bounds);
        int sample = 1;
        while (bounds.outWidth / sample > maxPixels * 2
                || bounds.outHeight / sample > maxPixels * 2) {
            sample *= 2;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = Math.max(1, sample);
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        return BitmapFactory.decodeFile(path, options);
    }

    static void delete(String path) {
        if (path == null || path.isEmpty()) return;
        File file = new File(path);
        if (file.exists()) file.delete();
    }
}
