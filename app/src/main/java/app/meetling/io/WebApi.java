package app.meetling.io;

import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.Object;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

/**
 * Encapsulates all communication with the Meetling Web API.
 */
// TODO decide whether methods take objects or ids/authSecrets
public class WebApi {
    public static final String EXTRA_API_HOST = "api_host";

    private String mHost;

    public WebApi(String host) {
        mHost = host;
    }

    /**
     *
     * @param user User to be connected to meetling.org
     *
     * @throws ValueError if the login fails
     */
    public Then<User> login(User user) {
        final Then<User> then = new Then<>();
        String code = null;
        if (user != null) {
            code = user.getAuthSecret();
        }

        class RequestTask extends AsyncTask<String, Void, User> {

            @Override
            protected User doInBackground(String... params) {
                User result;

                JSONObject authObj = new JSONObject();
                try {
                    authObj.put("code", params[0]);
                } catch (JSONException e) {
                    // unreachable
                    throw new RuntimeException(e);
                }

                JSONObject returnObj = (JSONObject) httpPost("/api/login", authObj);

                try {
                    if (returnObj.getString("__type__").equals("ValueError")) {
                        throw new ValueError(returnObj.getString("code"));
                    }
                    result = new User(returnObj);
                } catch (JSONException e) {
                    // unreachable
                    throw new RuntimeException(e);
                }

                return result;
            }

            @Override
            protected void onPostExecute(User result) {
                super.onPostExecute(result);

                then.compute(result);
            }
        }

        new RequestTask().execute(code);

        return then;
    }

    public Then<User> getUser(String id, String authSecret) {
        final Then<User> then = new Then<>();

        class RequestTask extends AsyncTask<String, Void, User> {

            @Override
            protected User doInBackground(String... params) {
                User result;

                JSONObject returnObj
                        = (JSONObject) httpGet("/api/users/" + params[0], params[1]);

                checkForErrors(returnObj);
                result = new User(returnObj);

                return result;
            }

            @Override
            protected void onPostExecute(User result) {
                super.onPostExecute(result);

                then.compute(result);
            }
        }

        new RequestTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, id, authSecret);

        return then;
    }

    public Then<String> setEmail(String email, User user) {
        Then<String> then = new Then<>();

        class RequestTask extends AsyncTask<Void, Void, String> {

            @Override
            protected String doInBackground(Void... params) {
                String endpoint = String.format("/api/users/%s/set-email", user.getId());
                JSONObject content = new JSONObject();
                try {
                    content.put("email", email);
                } catch (JSONException e) {
                    // unreachable
                    throw new RuntimeException(e);
                }
                JSONObject returnObj
                        = (JSONObject) httpPost(endpoint, content, user.getAuthSecret());
                checkForErrors(returnObj);

                String authRequestId;
                try {
                    authRequestId = (String) returnObj.get("id");
                } catch (JSONException e) {
                    // unreachable
                    throw new RuntimeException(e);
                }

                return authRequestId;
            }

            @Override
            protected void onPostExecute(String authId) {
                super.onPostExecute(authId);

                then.compute(authId);
            }
        }

        new RequestTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        return then;
    }

    public Then<User> finishSetEmail(String authRequestId, String authCode, User user) {
        Then<User> then = new Then<>();

        class RequestTask extends AsyncTask<Void, Void, User> {
            private Pair<String, String> error;

            @Override
            protected User doInBackground(Void... params) {
                String endpoint = String.format("/api/users/%s/finish-set-email", user.getId());
                JSONObject content = new JSONObject();
                try {
                    content.put("auth_request_id", authRequestId);
                    content.put("auth", authCode);
                } catch (JSONException e) {
                    // unreachable
                    throw new RuntimeException(e);
                }
                JSONObject returnObj
                        = (JSONObject) httpPost(endpoint, content, user.getAuthSecret());

                try {
                    checkForErrors(returnObj);
                } catch (ValueError valueError) {
                    error = new Pair<>("ValueError", valueError.getMessage());
                    return null;
                }

                return new User(returnObj);
            }

            @Override
            protected void onPostExecute(User result) {
                super.onPostExecute(result);

                if (error != null) {
                    then.setError(error);
                }
                then.compute(result);

            }
        }

        new RequestTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        return then;
    }

    public Then<User> deleteEmail(User user) {
        Then<User> then = new Then<>();

        class RequestTask extends AsyncTask<Void, Void, User> {

            @Override
            protected User doInBackground(Void... params) {
                User result;

                String endpoint = String.format("/api/users/%s/remove-email", user.getId());
                JSONObject returnObj
                        = (JSONObject) httpPost(endpoint, user.getJson(), user.getAuthSecret());

                checkForErrors(returnObj);
                result = new User(returnObj);

                return result;
            }
            @Override
            protected void onPostExecute(User result) {
                super.onPostExecute(result);

                then.compute(result);
            }
        }

        new RequestTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        return then;
    }

    public Then<User> edit(User user) {
        final Then<User> then = new Then<>();

        class RequestTask extends AsyncTask<User, Void, User> {

            @Override
            protected User doInBackground(User... params) {
                User user = params[0];
                User result;

                String endpoint = String.format("/api/users/%s", user.getId());
                JSONObject returnObj
                        = (JSONObject) httpPost(endpoint, user.getJson(), user.getAuthSecret());

                checkForErrors(returnObj);
                result = new User(returnObj);

                return result;
            }

            @Override
            protected void onPostExecute(User result) {
                super.onPostExecute(result);

                then.compute(result);
            }
        }

        new RequestTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, user);

        return then;
    }

    public Then<Meeting> edit(Meeting meeting, User user) {
        final Then<Meeting> then = new Then<>();

        class RequestTask extends AsyncTask<app.meetling.io.Object, Void, Meeting> {

            @Override
            protected Meeting doInBackground(app.meetling.io.Object... params) {
                Meeting result;
                Meeting meeting = (Meeting) params[0];
                User user = (User) params[1];

                String endpoint = String.format("/api/meetings/%s", meeting.getId());
                JSONObject returnObj
                        = (JSONObject) httpPost(endpoint, meeting.getJson(), user.getAuthSecret());

                checkForErrors(returnObj);
                result = new Meeting(returnObj);

                return result;
            }

            @Override
            protected void onPostExecute(Meeting result) {
                super.onPostExecute(result);

                then.compute(result);
            }
        }

        new RequestTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, meeting, user);

        return then;
    }

    public Then<AgendaItem> edit(AgendaItem item, Meeting meeting, User user) {
        final Then<AgendaItem> then = new Then<>();

        class RequestTask extends AsyncTask<app.meetling.io.Object, Void, AgendaItem> {

            @Override
            protected AgendaItem doInBackground(app.meetling.io.Object... params) {
                AgendaItem item = (AgendaItem) params[0];
                Meeting meeting = (Meeting) params[1];
                User user = (User) params[2];
                AgendaItem result;
                String endpoint
                        = String.format("/api/meetings/%s/items/%s", meeting.getId(), item.getId());
                JSONObject returnObj
                        = (JSONObject) httpPost(endpoint, item.getJson(), user.getAuthSecret());

                checkForErrors(returnObj);
                result = new AgendaItem(returnObj);

                return result;
            }

            @Override
            protected void onPostExecute(AgendaItem result) {
                super.onPostExecute(result);

                then.compute(result);
            }
        }

        new RequestTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, item, meeting, user);

        return then;
    }

    public Then<Meeting> createMeeting(
            String title, Date time, String location, String description, User user) {
        final Then<Meeting> then = new Then<>();

        class RequestTask extends AsyncTask<Object, Void, Meeting> {

            @Override
            protected Meeting doInBackground(Object... params) {
                String title = (String) params[0];
                Date time = (Date) params[1];
                String location = (String) params[2];
                String description = (String) params[3];
                User user = (User) params[4];
                JSONObject content;

                try {
                    content = new JSONObject(
                            String.format(
                                    "{\"title\": \"%s\", \"time\": \"%s\", \"location\": \"%s\", \"description\": \"%s\"}",
                                    title, time == null? "" : Util.toMeetlingTimeFormat(time),
                                    location, description));
                } catch (JSONException e) {
                    // unreachable
                    throw new RuntimeException(e);
                }
                Meeting result;

                JSONObject returnObj
                        = (JSONObject) httpPost("/api/meetings", content, user.getAuthSecret());

                try {
                    if (returnObj.getString("__type__").equals("ValueError")) {
                        if (returnObj.getString("code").equals("input_invalid")) {
                            throw new InputError(returnObj.getJSONObject("errors"));
                        }
                        throw new ValueError(returnObj.getString("code"));
                    }
                    result = new Meeting(returnObj);
                } catch (JSONException e) {
                    // unreachable
                    throw new RuntimeException(e);
                }

                return result;
            }

            @Override
            protected void onPostExecute(Meeting result) {
                super.onPostExecute(result);

                then.compute(result);
            }
        }

        new RequestTask()
                .executeOnExecutor(
                        AsyncTask.THREAD_POOL_EXECUTOR, title, time, location, description, user);

        return then;
    }

    public Then<Meeting> createExampleMeeting(User user) {
        final Then<Meeting> then = new Then<>();

        class RequestTask extends AsyncTask<User, Void, Meeting> {

            @Override
            protected Meeting doInBackground(User... params) {
                Meeting result;

                JSONObject returnObj
                        = (JSONObject) httpPost(
                                "/api/create-example-meeting", params[0].getAuthSecret());

                result = new Meeting(returnObj);

                return result;
            }

            @Override
            protected void onPostExecute(Meeting result) {
                super.onPostExecute(result);

                then.compute(result);
            }
        }

        new RequestTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, user);

        return then;
    }

    public Then<Meeting> getMeeting(String id, User user) {
        final Then<Meeting> then = new Then<>();

        class RequestTask extends AsyncTask<Object, Void, Meeting> {
            @Override
            protected Meeting doInBackground(Object... params) {
                String id = (String) params[0];
                User user = (User) params[1];
                Meeting result;

                JSONObject returnObj
                        = (JSONObject) httpGet("/api/meetings/" + id, user.getAuthSecret());

                checkForErrors(returnObj);
                result = new Meeting(returnObj);

                return result;
            }

            @Override
            protected void onPostExecute(Meeting result) {
                super.onPostExecute(result);

                then.compute(result);
            }
        }

        new RequestTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, id, user);

        return then;
    }

    public Then<AgendaItem> createAgendaItem(
            String title, Integer duration, String description, Meeting meeting, User user) {
        final Then<AgendaItem> then = new Then<>();

        class RequestTask extends AsyncTask<Object, Void, AgendaItem> {

            @Override
            protected AgendaItem doInBackground(Object... params) {
                String title = (String) params[0];
                Integer duration = (Integer) params[1];
                String description = (String) params[2];
                Meeting meeting = (Meeting) params[3];
                User user = (User) params[4];
                JSONObject content;

                try {
                    content = new JSONObject(
                            String.format(Locale.US,
                                    "{\"title\" : \"%s\", \"duration\": %d, \"description\": \"%s\"}",
                                    title, duration, description));
                } catch (JSONException e) {
                    // unreachable
                    throw new RuntimeException(e);
                }
                AgendaItem result;

                JSONObject returnObj
                        = (JSONObject) httpPost(String.format("/api/meetings/%s/items",
                                meeting.getId()), content, user.getAuthSecret());

                checkForErrors(returnObj);
                result = new AgendaItem(returnObj);

                return result;
            }

            @Override
            protected void onPostExecute(AgendaItem result) {
                super.onPostExecute(result);

                then.compute(result);
            }
        }

        new RequestTask()
                .executeOnExecutor(
                        AsyncTask.THREAD_POOL_EXECUTOR, title, duration, description, meeting,
                        user);

        return then;
    }

    public Then<List<AgendaItem>> getAgendaItems(String meetingId, User user) {
        final Then<List<AgendaItem>> then = new Then<>();

        class RequestTask extends AsyncTask<Object, Void, List<AgendaItem>> {

            @Override
            protected List<AgendaItem> doInBackground(Object... params) {
                String meetingId = (String) params[0];
                User user = (User) params[1];
                List<AgendaItem> result = new ArrayList<>();

                JSONArray returnArr
                        = (JSONArray) httpGet(String.format("/api/meetings/%s/items",
                                meetingId), user.getAuthSecret());
                try {
                    for (int i = 0; i < returnArr.length(); i++) {
                        result.add(new AgendaItem(returnArr.getJSONObject(i)));
                    }
                } catch (JSONException e) {
                    // unreachable
                    throw new RuntimeException(e);
                }

                return result;
            }

            @Override
            protected void onPostExecute(List<AgendaItem> result) {
                super.onPostExecute(result);

                then.compute(result);
            }
        }

        new RequestTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, meetingId, user);

        return then;
    }

    public Then<List<AgendaItem>> getTrashedAgendaItems(String meetingId, User user) {

        final Then<List<AgendaItem>> then = new Then<>();

        class RequestTask extends AsyncTask<Object, Void, List<AgendaItem>> {

            @Override
            protected List<AgendaItem> doInBackground(Object... params) {
                String meetingId = (String) params[0];
                User user = (User) params[1];
                List<AgendaItem> result = new ArrayList<>();

                JSONArray returnArr
                        = (JSONArray) httpGet(String.format("/api/meetings/%s/items/trashed",
                                meetingId), user.getAuthSecret());
                try {
                    for (int i = 0; i < returnArr.length(); i++) {
                        result.add(new AgendaItem(returnArr.getJSONObject(i)));
                    }
                } catch (JSONException e) {
                    // unreachable
                    throw new RuntimeException(e);
                }

                return result;
            }

            @Override
            protected void onPostExecute(List<AgendaItem> result) {
                super.onPostExecute(result);

                then.compute(result);
            }
        }

        new RequestTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, meetingId, user);

        return then;
    }

    public void trashAgendaItem(AgendaItem item, Meeting meeting, User user) {

        class RequestTask extends AsyncTask<Object, Void, Void> {

            @Override
            protected Void doInBackground(Object... params) {
                AgendaItem item = (AgendaItem) params[0];
                Meeting meeting = (Meeting) params[1];
                User user = (User) params[2];

                JSONObject content;

                try {
                    content = new JSONObject(String.format("{\"item_id\" : \"%s\"}", item.getId()));
                } catch (JSONException e) {
                    // unreachable
                    throw new RuntimeException(e);
                }

                checkForErrors(
                        httpPost(
                                String.format("/api/meetings/%s/trash-agenda-item",
                                meeting.getId()), content, user.getAuthSecret()));

                return null;
            }
        }

        new RequestTask().execute(item, meeting, user);
    }

    public void restoreAgendaItem(AgendaItem item, Meeting meeting, User user) {

        class RequestTask extends AsyncTask<app.meetling.io.Object, Void, Void> {

            @Override
            protected Void doInBackground(app.meetling.io.Object... params) {
                AgendaItem item = (AgendaItem) params[0];
                Meeting meeting = (Meeting) params[1];
                User user = (User) params[2];
                JSONObject content;

                try {
                    content = new JSONObject(String.format("{\"item_id\" : \"%s\"}", item.getId()));
                } catch (JSONException e) {
                    // unreachable
                    throw new RuntimeException(e);
                }

                checkForErrors(
                        httpPost(
                                String.format("/api/meetings/%s/restore-agenda-item",
                                meeting.getId()), content, user.getAuthSecret()));

                return null;
            }
        }

        new RequestTask().execute(item, meeting, user);
    }

    public void moveAgendaItem(AgendaItem item, AgendaItem after, Meeting meeting, User user) {

        class RequestTask extends AsyncTask<app.meetling.io.Object, Void, Void> {

            @Override
            protected Void doInBackground(app.meetling.io.Object... params) {
                AgendaItem item = (AgendaItem) params[0];
                AgendaItem after = (AgendaItem) params[1];
                Meeting meeting = (Meeting) params[2];
                User user = (User) params[3];
                JSONObject content;

                try {
                    content = new JSONObject(
                            String.format("{\"item_id\" : \"%s\", \"to_id\" : %s}",
                                    item.getId(),
                                    after == null ? null : String.format("\"%s\"", after.getId())));
                } catch (JSONException e) {
                    // unreachable
                    throw new RuntimeException(e);
                }

                checkForErrors(
                        httpPost(
                                String.format("/api/meetings/%s/move-agenda-item", meeting.getId()),
                                content, user.getAuthSecret()));

                return null;
            }
        }

        new RequestTask().execute(item, after, meeting, user);
    }

    private void checkForErrors(Object returnObj) {
        if (returnObj != JSONObject.NULL) {
            checkForErrors((JSONObject) returnObj);
        }
    }

    private void checkForErrors(JSONObject returnObj) {
        try {
            if (returnObj.getString("__type__").equals("ValueError")) {
                if (returnObj.getString("code").equals("input_invalid")) {
                    throw new InputError(returnObj.getJSONObject("errors"));
                }
                throw new ValueError(returnObj.getString("code"));
            } else if (returnObj.getString("__type__").equals("NotFoundError")) {
                throw new NotFoundError();
            }
        } catch (JSONException e) {
            // unreachable
            throw new RuntimeException(e);
        }
    }

    private Object httpPost(String endpoint, JSONObject content) {
        return httpPost(endpoint, content, null);
    }

    private Object httpPost(String endpoint, String authSecret) {
        return httpPost(endpoint, null, authSecret);
    }

    private Object httpPost(String endpoint, JSONObject content, String authSecret) {
        Object response;
        HttpsURLConnection urlConnection = null;
        try { // replace with try-with-resources once minSdkVersion is 19
            URL url = new URL(mHost + endpoint);
            urlConnection = (HttpsURLConnection) url.openConnection();

            urlConnection.setDoOutput(true);
            if (authSecret != null) {
                urlConnection.setRequestProperty("Cookie", "auth_secret=" + authSecret);
            }

            urlConnection.connect();

            if (content != null) {
                Log.v("HTTPS POST", String.format(
                        "%s %s, auth_secret=%s", endpoint, content.toString(), authSecret));
                OutputStream out = urlConnection.getOutputStream();
                byte[] postData = content.toString().getBytes();
                out.write(postData);
                out.close();
            }

            response = readStream(urlConnection);

        } catch (MalformedURLException e) {
            // unreachable
            throw new RuntimeException(e);
        } catch (IOException e) {
            // TODO handle availability problems
            throw new IOError(e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        return response;
    }

    private Object httpGet(String endpoint, String authSecret) {
        Object response;
        HttpsURLConnection urlConnection = null;
        try { // replace with try-with-resources once minSdkVersion is 19
            URL url = new URL(mHost + endpoint);
            urlConnection = (HttpsURLConnection) url.openConnection();

            urlConnection.setDoInput(true);
            if (authSecret != null) {
                Log.v("HTTPS GET", endpoint + ", auth_secret=" + authSecret);
                urlConnection.setRequestProperty("Cookie", "auth_secret=" + authSecret);
            }

            urlConnection.connect();

            response = readStream(urlConnection);

        } catch (MalformedURLException e) {
            // unreachable
            throw new RuntimeException(e);
        } catch (IOException e) {
            // TODO handle availability problems
            throw new IOError(e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        return response;
    }

    public String getHost() {
        return mHost;
    }

    private Object readStream(HttpsURLConnection urlConnection) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();

        InputStream in = null;
        BufferedReader reader = null;
        try { // replace with try-with-resources once minSdkVersion is 19
            if (urlConnection.getResponseCode() != 200) {
                in = new BufferedInputStream(urlConnection.getErrorStream());
            } else {
                in = new BufferedInputStream(urlConnection.getInputStream());
            }
            reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
        } finally {
            if (in != null) {
                in.close();
            }
            if (reader != null) {
                reader.close();
            }
        }

        try {
            String content = stringBuilder.toString();
            Log.v("HTTPS response body", content);
            return new JSONTokener(stringBuilder.toString()).nextValue();
        } catch (JSONException e) {
            // unreachable
            throw new RuntimeException(e);
        }
    }

    private static class NotFoundError extends RuntimeException {
    }

    public static class ValueError extends RuntimeException {

        public ValueError(String code) {
            super(code);
        }
    }

    public static class InputError extends ValueError {
        private Map<String, String> mErrors;

        public InputError(JSONObject errors) {
            super("input_invalid");

            mErrors = new HashMap<>();

            Iterator<String> iter = errors.keys();
            while (iter.hasNext()) {
                String key = iter.next();
                try {
                    mErrors.put(key, errors.getString(key));
                } catch (JSONException e) {
                    // unreachable
                    throw new RuntimeException(e);
                }
            }
        }

        public Map<String, String> getErrors() {
            return mErrors;
        }
    }
}
