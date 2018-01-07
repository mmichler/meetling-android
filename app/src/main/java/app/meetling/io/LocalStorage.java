package app.meetling.io;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

/**
 * Handles data persistence on the device.
 */
@SuppressLint({"StaticFieldLeak", "DefaultLocale"})
public class LocalStorage {
    private static final int DATA_MODEL_VERSION = 1;
    private static final String DB_NAME = "meetling";
    private Context mContext;
    private DbHelper mDbHelper;
    private boolean mInMemory;

    public LocalStorage(Context context) {
        this(context, false);
    }

    public LocalStorage(Context context, boolean inMemory) {
        mContext = context;
        mInMemory = inMemory;
        mDbHelper = new DbHelper(context, inMemory ? null : DB_NAME, null, DATA_MODEL_VERSION);
    }

    /**
     *
     * @param url A host URL in the format <code>[http/https]://host.tld</code>
     * @return
     */
    public Then<Host> addHost(String url) {
        // FIXME basic validation
        Then<Host> then = new Then<>();

        class Task extends AsyncTask<Void, Void, Host> {

            @Override
            protected Host doInBackground(Void... params) {
                int id;
                try {
                    id = addHostSync(url, null);
                } catch (SQLiteException e) {
                    then.setError(e);
                    return null;
                }
                return getHostSync(id);
            }

            @Override
            protected void onPostExecute(Host result) {
                super.onPostExecute(result);

                then.compute(result);
            }
        }

        new Task().execute();

        return then;
    }

    private int addHostSync(String url, SQLiteDatabase db) {
        int id = new Random().nextInt();
        boolean doClose = true;
        if (db == null) {
            db = mDbHelper.getWritableDatabase();
        } else {
            doClose = false;
        }

        ContentValues values = new ContentValues();
        values.put("id", id);
        values.put("url", url);

        try {
            db.insertOrThrow("hosts", null, values);
        } finally {
            if (doClose) {
                close(db);
            }
        }

        return id;
    }

    public Then<Void> removeHost(int id) {
        Then<Void> then = new Then<>();

        class Task extends AsyncTask<Void, Void, Void> {

            @Override
            protected Void doInBackground(Void... params) {
                SQLiteDatabase db = mDbHelper.getWritableDatabase();

                int rows;
                rows = db.delete("hosts", String.format("id = %d", id), null);
                if (rows == 0) {
                    then.setError(new IllegalArgumentException("No host object with this ID in DB."));
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                super.onPostExecute(result);

                then.compute(result);
            }
        }

        new Task().execute();

        return then;
    }

    public Then<Host> getHost(int id) {
        Then<Host> then = new Then<>();

        class Task extends AsyncTask<Void, Void, Host> {

            @Override
            protected Host doInBackground(Void... params) {
                try {
                    return getHostSync(id);
                } catch (IllegalArgumentException e) {
                    then.setError(e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Host result) {
                super.onPostExecute(result);

                then.compute(result);
            }
        }

        new Task().execute();

        return then;
    }

    private Host getHostSync(int id) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        Cursor cursor  = db.rawQuery(String.format("SELECT * FROM hosts where id = %d", id), null);

        Host host;

        if (cursor != null && cursor.moveToFirst()) {
            host = new Host(id, cursor.getString(1), cursor.getString(2), cursor.getString(3));
            cursor.close();
        } else {
            throw new IllegalArgumentException("No host object with this ID in DB.");
        }

        close(db);

        return host;
    }

    public Then<List<Host>> getHosts() {
        Then<List<Host>> then = new Then<>();

        class Task extends AsyncTask<Void, Void, List<Host>> {

            @Override
            protected List<Host> doInBackground(Void... params) {
                SQLiteDatabase db = mDbHelper.getWritableDatabase();

                Cursor cursor  = db.rawQuery("SELECT * FROM hosts", null);

                List<Host> hosts = new ArrayList<>();

                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        hosts.add(new Host(cursor.getInt(0), cursor.getString(1), cursor.getString(2),
                                cursor.getString(3)));
                    }
                    cursor.close();
                }

                close(db);

                return hosts;
            }

            @Override
            protected void onPostExecute(List<Host> result) {
                super.onPostExecute(result);

                then.compute(result);
            }
        }

        new Task().execute();

        return then;
    }

    public Then<Host> setCredentials(Host host, String userId, String authSecret) {
        final Then<Host> then = new Then<>();

        class SetCredentialsTask extends AsyncTask<Void, Void, Host> {

            @Override
            protected Host doInBackground(Void... params) {
                return setCredentialsSync(host, userId, authSecret);
            }

            @Override
            protected void onPostExecute(Host result) {
                super.onPostExecute(result);

                then.compute(result);
            }
        }

        new SetCredentialsTask().execute();

        return then;
    }

    Host setCredentialsSync(Host host, String userId, String authSecret) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        // TODO store auth_secret securely
        ContentValues values = new ContentValues();
        values.put("user_id", userId);
        values.put("auth_secret", authSecret);

        int rows;
        rows = db.update("hosts", values, "id = ?", new
                String[]{String.valueOf(host.getId())});
        if (rows == 0) {
            throw new IllegalArgumentException("No host object with this ID in DB.");
        }

        close(db);

        return getHostSync(host.getId());
    }

    public Then<Boolean> setAuthRequestId(String requestId) {
        Then<Boolean> then = new Then<>();

        class SetAuthRequestIdTask extends AsyncTask<Void, Void, Boolean> {

            @Override
            protected Boolean doInBackground(Void... params) {
                SharedPreferences prefs =
                        mContext.getSharedPreferences("meetling", Context.MODE_PRIVATE);
                return prefs.edit().putString("auth_request_id", requestId).commit();
            }

            @Override
            protected void onPostExecute(Boolean result) {
                super.onPostExecute(result);

                then.compute(result);
            }
        }

        new SetAuthRequestIdTask().execute();

        return then;
    }

    public Then<String> getAuthRequestId() {
        Then<String> then = new Then<>();

        class SetAuthRequestIdTask extends AsyncTask<Void, Void, String> {

            @Override
            protected String doInBackground(Void... params) {
                SharedPreferences prefs =
                        mContext.getSharedPreferences("meetling", Context.MODE_PRIVATE);
                return prefs.getString("auth_request_id", null);
            }

            @Override
            protected void onPostExecute(String result) {
                super.onPostExecute(result);

                then.compute(result);
            }
        }

        new SetAuthRequestIdTask().execute();

        return then;
    }

    public Then<Boolean> deleteAuthRequestId() {
        Then<Boolean> then = new Then<>();

        class SetAuthRequestIdTask extends AsyncTask<Void, Void, Boolean> {

            @Override
            protected Boolean doInBackground(Void... params) {
                SharedPreferences prefs =
                        mContext.getSharedPreferences("meetling", Context.MODE_PRIVATE);
                return prefs.edit().remove("auth_request_id").commit();
            }

            @Override
            protected void onPostExecute(Boolean result) {
                super.onPostExecute(result);

                then.compute(result);
            }
        }

        new SetAuthRequestIdTask().execute();

        return then;
    }

    public Then<Boolean> hasAuthRequestId() {
        Then<Boolean> then = new Then<>();

        class SetAuthRequestIdTask extends AsyncTask<Void, Void, Boolean> {

            @Override
            protected Boolean doInBackground(Void... params) {
                SharedPreferences prefs =
                        mContext.getSharedPreferences("meetling", Context.MODE_PRIVATE);
                return prefs.contains("auth_request_id");
            }

            @Override
            protected void onPostExecute(Boolean result) {
                super.onPostExecute(result);

                then.compute(result);
            }
        }

        new SetAuthRequestIdTask().execute();

        return then;
    }

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

        DbHelper(
                Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
            super(context, name, factory, version);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            createDb(db);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            dropAll(db);
            createDb(db);
        }

        @Override
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            dropAll(db);
            createDb(db);
        }

        private void createDb(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE hosts (" +
                            "id INTEGER PRIMARY KEY NOT NULL," +
                            "url TEXT UNIQUE NOT NULL," +
                            "user_id TEXT," +
                            "auth_secret TEXT" +
                    ")");
            db.execSQL(
                    "CREATE TABLE history (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "meeting_id TEXT" +
                    ")");

            addHostSync("https://meetling.org", db);
        }

        private void dropAll(SQLiteDatabase db) {
            db.execSQL("DROP TABLE IF EXISTS history");
            db.execSQL("DROP TABLE IF EXISTS hosts");
        }
    }
}
