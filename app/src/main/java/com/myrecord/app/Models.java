package com.myrecord.app;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

final class Models {
    private Models() {}

    static String id() {
        return UUID.randomUUID().toString();
    }

    static final class AppData {
        int version = 1;
        final List<DailyEntry> dailyEntries = new ArrayList<>();
        final List<Routine> routines = new ArrayList<>();
        final List<Goal> goals = new ArrayList<>();
        final List<Note> notes = new ArrayList<>();

        DailyEntry daily(String dateKey) {
            for (DailyEntry entry : dailyEntries) {
                if (entry.date.equals(dateKey)) return entry;
            }
            DailyEntry created = new DailyEntry();
            created.date = dateKey;
            dailyEntries.add(created);
            return created;
        }

        Note note(String noteId) {
            if (noteId == null) return null;
            for (Note note : notes) {
                if (note.id.equals(noteId)) return note;
            }
            return null;
        }

        JSONObject toJson() throws JSONException {
            JSONObject root = new JSONObject();
            root.put("version", version);
            root.put("dailyEntries", arrayOfDaily(dailyEntries));
            root.put("routines", arrayOfRoutines(routines));
            root.put("goals", arrayOfGoals(goals));
            root.put("notes", arrayOfNotes(notes));
            return root;
        }

        static AppData fromJson(JSONObject root) throws JSONException {
            AppData data = new AppData();
            data.version = root.optInt("version", 1);

            JSONArray daily = root.optJSONArray("dailyEntries");
            if (daily != null) {
                for (int i = 0; i < daily.length(); i++) {
                    JSONObject item = daily.optJSONObject(i);
                    if (item != null) data.dailyEntries.add(DailyEntry.fromJson(item));
                }
            }

            JSONArray routinesArray = root.optJSONArray("routines");
            if (routinesArray != null) {
                for (int i = 0; i < routinesArray.length(); i++) {
                    JSONObject item = routinesArray.optJSONObject(i);
                    if (item != null) data.routines.add(Routine.fromJson(item));
                }
            }

            JSONArray goalsArray = root.optJSONArray("goals");
            if (goalsArray != null) {
                for (int i = 0; i < goalsArray.length(); i++) {
                    JSONObject item = goalsArray.optJSONObject(i);
                    if (item != null) data.goals.add(Goal.fromJson(item));
                }
            }

            JSONArray notesArray = root.optJSONArray("notes");
            if (notesArray != null) {
                for (int i = 0; i < notesArray.length(); i++) {
                    JSONObject item = notesArray.optJSONObject(i);
                    if (item != null) data.notes.add(Note.fromJson(item));
                }
            }
            return data;
        }
    }

    static final class ChecklistItem {
        String id = Models.id();
        String text = "";
        String details = "";
        boolean done;
        long createdAt = System.currentTimeMillis();

        JSONObject toJson() throws JSONException {
            JSONObject object = new JSONObject();
            object.put("id", id);
            object.put("text", text);
            object.put("details", details);
            object.put("done", done);
            object.put("createdAt", createdAt);
            return object;
        }

        static ChecklistItem fromJson(JSONObject object) {
            ChecklistItem item = new ChecklistItem();
            item.id = object.optString("id", Models.id());
            item.text = object.optString("text", "");
            item.details = object.optString("details", "");
            item.done = object.optBoolean("done", false);
            item.createdAt = object.optLong("createdAt", System.currentTimeMillis());
            return item;
        }
    }

    static final class DailyEntry {
        String date = "";
        String memo = "";
        final List<ChecklistItem> items = new ArrayList<>();

        JSONObject toJson() throws JSONException {
            JSONObject object = new JSONObject();
            object.put("date", date);
            JSONArray array = new JSONArray();
            for (ChecklistItem item : items) array.put(item.toJson());
            object.put("items", array);
            object.put("memo", memo);
            return object;
        }

        static DailyEntry fromJson(JSONObject object) throws JSONException {
            DailyEntry entry = new DailyEntry();
            entry.date = object.optString("date", "");
            entry.memo = object.optString("memo", "");
            JSONArray array = object.optJSONArray("items");
            if (array != null) {
                for (int i = 0; i < array.length(); i++) {
                    JSONObject child = array.optJSONObject(i);
                    if (child != null) entry.items.add(ChecklistItem.fromJson(child));
                }
            }
            return entry;
        }
    }

    static final class Routine {
        String id = Models.id();
        String text = "";
        String details = "";
        final Set<Integer> weekdays = new HashSet<>();
        final Set<String> doneDates = new HashSet<>();
        long createdAt = System.currentTimeMillis();

        Routine() {
            for (int day = Calendar.SUNDAY; day <= Calendar.SATURDAY; day++) {
                weekdays.add(day);
            }
        }

        JSONObject toJson() throws JSONException {
            JSONObject object = new JSONObject();
            object.put("id", id);
            object.put("text", text);
            object.put("details", details);
            object.put("createdAt", createdAt);
            JSONArray days = new JSONArray();
            for (int day = Calendar.SUNDAY; day <= Calendar.SATURDAY; day++) {
                if (weekdays.contains(day)) days.put(day);
            }
            object.put("weekdays", days);
            JSONArray done = new JSONArray();
            for (String date : doneDates) done.put(date);
            object.put("doneDates", done);
            return object;
        }

        static Routine fromJson(JSONObject object) {
            Routine routine = new Routine();
            routine.id = object.optString("id", Models.id());
            routine.text = object.optString("text", "");
            routine.details = object.optString("details", "");
            routine.createdAt = object.optLong("createdAt", System.currentTimeMillis());
            routine.weekdays.clear();
            JSONArray days = object.optJSONArray("weekdays");
            if (days != null) {
                for (int i = 0; i < days.length(); i++) routine.weekdays.add(days.optInt(i));
            }
            if (routine.weekdays.isEmpty()) {
                for (int day = Calendar.SUNDAY; day <= Calendar.SATURDAY; day++) routine.weekdays.add(day);
            }
            JSONArray done = object.optJSONArray("doneDates");
            if (done != null) {
                for (int i = 0; i < done.length(); i++) {
                    String value = done.optString(i, "");
                    if (!value.isEmpty()) routine.doneDates.add(value);
                }
            }
            return routine;
        }
    }

    static final class Goal {
        static final String MONTH = "month";
        static final String YEAR = "year";

        String id = Models.id();
        String type = MONTH;
        String period = "";
        String text = "";
        String details = "";
        boolean done;
        long createdAt = System.currentTimeMillis();

        JSONObject toJson() throws JSONException {
            JSONObject object = new JSONObject();
            object.put("id", id);
            object.put("type", type);
            object.put("period", period);
            object.put("text", text);
            object.put("details", details);
            object.put("done", done);
            object.put("createdAt", createdAt);
            return object;
        }

        static Goal fromJson(JSONObject object) {
            Goal goal = new Goal();
            goal.id = object.optString("id", Models.id());
            goal.type = object.optString("type", MONTH);
            goal.period = object.optString("period", "");
            goal.text = object.optString("text", "");
            goal.details = object.optString("details", "");
            goal.done = object.optBoolean("done", false);
            goal.createdAt = object.optLong("createdAt", System.currentTimeMillis());
            return goal;
        }
    }

    static final class Note {
        String id = Models.id();
        String title = "";
        String content = "";
        String folder = "미분류";
        final List<String> tags = new ArrayList<>();
        final List<String> photoPaths = new ArrayList<>();
        long createdAt = System.currentTimeMillis();
        long updatedAt = System.currentTimeMillis();
        boolean pinned;

        boolean isBlank() {
            return title.trim().isEmpty()
                    && content.trim().isEmpty()
                    && tags.isEmpty()
                    && photoPaths.isEmpty()
                    && (folder.trim().isEmpty() || "미분류".equals(folder.trim()));
        }

        boolean matches(String query, String selectedFolder) {
            if (selectedFolder != null
                    && !selectedFolder.isEmpty()
                    && !"전체".equals(selectedFolder)
                    && !selectedFolder.equals(folder)) return false;
            String normalized = query == null ? "" : query.trim().toLowerCase();
            if (normalized.isEmpty()) return true;
            if (title.toLowerCase().contains(normalized)) return true;
            if (content.toLowerCase().contains(normalized)) return true;
            if (folder.toLowerCase().contains(normalized)) return true;
            for (String tag : tags) {
                if (tag.toLowerCase().contains(normalized)) return true;
            }
            return false;
        }

        JSONObject toJson() throws JSONException {
            JSONObject object = new JSONObject();
            object.put("id", id);
            object.put("title", title);
            object.put("content", content);
            object.put("folder", folder);
            object.put("createdAt", createdAt);
            object.put("updatedAt", updatedAt);
            object.put("pinned", pinned);
            JSONArray tagsArray = new JSONArray();
            for (String tag : tags) tagsArray.put(tag);
            object.put("tags", tagsArray);
            JSONArray photos = new JSONArray();
            for (String path : photoPaths) photos.put(path);
            object.put("photoPaths", photos);
            return object;
        }

        static Note fromJson(JSONObject object) {
            Note note = new Note();
            note.id = object.optString("id", Models.id());
            note.title = object.optString("title", "");
            note.content = object.optString("content", "");
            note.folder = object.optString("folder", "미분류");
            note.createdAt = object.optLong("createdAt", System.currentTimeMillis());
            note.updatedAt = object.optLong("updatedAt", note.createdAt);
            note.pinned = object.optBoolean("pinned", false);
            JSONArray tagsArray = object.optJSONArray("tags");
            if (tagsArray != null) {
                for (int i = 0; i < tagsArray.length(); i++) {
                    String value = tagsArray.optString(i, "").trim();
                    if (!value.isEmpty()) note.tags.add(value);
                }
            }
            JSONArray photos = object.optJSONArray("photoPaths");
            if (photos != null) {
                for (int i = 0; i < photos.length(); i++) {
                    String path = photos.optString(i, "");
                    if (!path.isEmpty()) note.photoPaths.add(path);
                }
            }
            return note;
        }
    }

    private static JSONArray arrayOfDaily(List<DailyEntry> source) throws JSONException {
        JSONArray array = new JSONArray();
        for (DailyEntry entry : source) array.put(entry.toJson());
        return array;
    }

    private static JSONArray arrayOfRoutines(List<Routine> source) throws JSONException {
        JSONArray array = new JSONArray();
        for (Routine routine : source) array.put(routine.toJson());
        return array;
    }

    private static JSONArray arrayOfGoals(List<Goal> source) throws JSONException {
        JSONArray array = new JSONArray();
        for (Goal goal : source) array.put(goal.toJson());
        return array;
    }

    private static JSONArray arrayOfNotes(List<Note> source) throws JSONException {
        JSONArray array = new JSONArray();
        for (Note note : source) array.put(note.toJson());
        return array;
    }
}
