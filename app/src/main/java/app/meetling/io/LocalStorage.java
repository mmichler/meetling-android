package app.meetling.io;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles data persistence on the device.
 */
public class LocalStorage {
    private static final int DATA_MODEL_VERSION = 1;
    private static final String DB_NAME = "meetling";
    private DbHelper mDbHelper;
    private boolean mInMemory;

    public LocalStorage(Context context) {
        this(context, false);
    }

    public LocalStorage(Context context, boolean inMemory) {
        mInMemory = inMemory;
        mDbHelper = new DbHelper(context, inMemory ? null : DB_NAME, null, DATA_MODEL_VERSION);
    }

    public Then<Void> setCredentials(String userId, String authSecret) {
        final Then<Void> then = new Then<>();

        class SetCredentialsTask extends AsyncTask<String, Void, Void> {

            @Override
            protected Void doInBackground(String... params) {
                SQLiteDatabase db = mDbHelper.getWritableDatabase();
                String userId = params[0];
                // TODO store auth_secret securely
                String authSecret = params[1];
                String sql
                        = String.format(
                        "INSERT OR REPLACE INTO credentials" +
                                "(user_id, auth_secret)" +
                                "VALUES ('%s', '%s')", userId, authSecret);
                db.execSQL(sql);
                close(db);
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                super.onPostExecute(result);

                then.compute(result);
            }
        }

        new SetCredentialsTask().execute(userId, authSecret);

        return then;
    }

    public Then<Pair<String, String>> getCredentials() {
        final Then<Pair<String, String>> then = new Then<>();

        class GetCredentialsTask extends AsyncTask<Void, Void, Pair<String, String>> {

            @Override
            protected Pair<String, String> doInBackground(Void... params) {
                SQLiteDatabase db = mDbHelper.getReadableDatabase();
                Cursor credentialResult = db.rawQuery("SELECT * FROM credentials", null);

                if (!credentialResult.moveToNext()) {
                    return null;
                }

                String userId = credentialResult.getString(0);
                String authSecret = credentialResult.getString(1);
                credentialResult.close();
                close(db);

                return new Pair<>(userId, authSecret);
            }

            @Override
            protected void onPostExecute(Pair<String, String> result) {
                super.onPostExecute(result);

                then.compute(result);
            }
        }

        new GetCredentialsTask().execute();

        return then;
    }

    // TODO Remove Then<Void>? AsyncTasks are by default executed sequentially anyway
    public Then<Void> setHistory(@NonNull List<String> history) {
        final Then<Void> then = new Then<>();

        class SetHistoryTask extends AsyncTask<List<String> , Void, Void> {

            @Override
            protected Void doInBackground(List<String>... params) {
                SQLiteDatabase db = mDbHelper.getWritableDatabase();
                db.delete("history", null, null);
                Cursor result =
                        db.rawQuery("SELECT * FROM sqlite_master WHERE type='table' AND name='sqlite_sequence'",
                                null);
                if (result.moveToNext()) {
                    db.execSQL("DELETE FROM history");
                    db.execSQL("DELETE FROM sqlite_sequence WHERE name = 'history'");
                }
                result.close();

                List<String> meetingIds = params[0];
                if (meetingIds.size() == 0) {
                    return null;
                }
                StringBuilder builder = new StringBuilder();
                builder.append("INSERT INTO history (meeting_id) VALUES ");
                for (String id : meetingIds) {
                    builder.append("('").append(id).append("')");
                    if (!meetingIds.get(meetingIds.size() - 1).equals(id)) {
                        builder.append(",");
                    }
                }
                db.execSQL(builder.toString());
                close(db);
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                super.onPostExecute(result);

                then.compute(result);
            }
        }

        new SetHistoryTask().execute(history);

        return then;
    }

    public Then<List<String>> getHistory() {
        final Then<List<String>> then = new Then<>();

        class GetHistoryTask extends AsyncTask<Void, Void, List<String>> {

            @Override
            protected List<String> doInBackground(Void... params) {
                SQLiteDatabase db = mDbHelper.getWritableDatabase();
                Cursor historyResult = db.rawQuery("SELECT * FROM history ORDER BY id", null);

                List<String> meetingIds = new ArrayList<>();
                while (historyResult.moveToNext()) {
                    meetingIds.add(historyResult.getString(1));
                }

                historyResult.close();
                close(db);
                return meetingIds;
            }

            @Override
            protected void onPostExecute(List<String> result) {
                super.onPostExecute(result);

                then.compute(result);
            }
        }

        new GetHistoryTask().execute();

        return then;
    }

    private void close(SQLiteDatabase db) {
        if (!mInMemory) {
            db.close();
        }
    }

    private class DbHelper extends SQLiteOpenHelper {

        public DbHelper(
                Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
            super(context, name, factory, version);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            createDb(db);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS credentials");
            db.execSQL("DROP TABLE IF EXISTS history");
            createDb(db);
        }

        @Override
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS credentials");
            db.execSQL("DROP TABLE IF EXISTS history");
            createDb(db);
        }

        private void createDb(SQLiteDatabase db) {
            db.execSQL(
                    "CREATE TABLE credentials (" +
                            "user_id TEXT PRIMARY KEY," +
                            "auth_secret TEXT" +
                            ")");
            db.execSQL(
                    "CREATE TABLE history (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "meeting_id TEXT" +
                            ")");
        }
    }
}
