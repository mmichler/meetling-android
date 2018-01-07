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
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Encapsulates all communication with the Meetling Web API.
 */
// TODO decide whether methods take objects or ids/authSecrets
public class WebApi {

    private Host mHost;

    // TODO move verification to localStorage - no web calls
    /**
     * Tests whether the a Meetling API is hosted at the input URL.
     *
     * @param url A URL including the protocol
     * @return A sanitized hostname with either an http or https protocol prefix or <code>null</code> if
     * the host does not exist or there is no Meetling API provided at the host
     */
    public static String verifyHost(String url) {
        URL parsedUrl;
        try {
            parsedUrl = new URL(url);
        } catch (MalformedURLException e) {
            return null;
        }
        String protocol = parsedUrl.getProtocol();
        String host = parsedUrl.getHost();

        return protocol + "://" + host;
    }

    public WebApi(Host host) {
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

    /**
     * Retrieves the user object for the credentials stored in the host object or creates a new user and
     * updates the host with the user's credentials
     *
     * @param localStorage used to update the host if there are no credentials stored in the host object
     *
     * @return the user that the app uses to edit meetings on the host
     */
    public Then<User> getUser(LocalStorage localStorage) {
        Then<User> then = new Then<>();

        class Task extends AsyncTask<Void, Void, User> {

            @Override
            protected User doInBackground(Void... params) {
                User result;

                JSONObject returnObj;
                if (mHost.hasCredentials()) {
                    // get existing user
                    returnObj
                            = (JSONObject) httpGet("/api/users/" + mHost.getUserId(), mHost.getAuthSecret());
                } else {
                    // create new user
                    returnObj = (JSONObject) httpPost("/api/login");
                }

                checkForErrors(returnObj);

                result = new User(returnObj);
                mHost = localStorage.setCredentialsSync(mHost, result.getId(), result.getAuthSecret());

                return result;
            }

            @Override
            protected void onPostExecute(User result) {
                super.onPostExecute(result);

                then.compute(result);
            }
        }

        new Task().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        return then;
    }

    public Then<String> setEmail(String email) {
        Then<String> then = new Then<>();

        class RequestTask extends AsyncTask<Void, Void, String> {

            @Override
            protected String doInBackground(Void... params) {
                String endpoint = String.format("/api/users/%s/set-email", mHost.getUserId());
                JSONObject content = new JSONObject();
                try {
                    content.put("email", email);
                } catch (JSONException e) {
                    // unreachable
                    throw new RuntimeException(e);
                }
                JSONObject returnObj
                        = (JSONObject) httpPost(endpoint, content, mHost.getAuthSecret());
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

    public Then<User> finishSetEmail(String authRequestId, String authCode) {
        Then<User> then = new Then<>();

        class RequestTask extends AsyncTask<Void, Void, User> {
            private ValueError error;

            @Override
            protected User doInBackground(Void... params) {
                String endpoint = String.format("/api/users/%s/finish-set-email", mHost.getUserId());
                JSONObject content = new JSONObject();
                try {
                    content.put("auth_request_id", authRequestId);
                    content.put("auth", authCode);
                } catch (JSONException e) {
                    // unreachable
                    throw new RuntimeException(e);
                }
                JSONObject returnObj
                        = (JSONObject) httpPost(endpoint, content, mHost.getAuthSecret());

                try {
                    checkForErrors(returnObj);
                } catch (ValueError valueError) {
                    error = valueError;
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

                String endpoint = String.format("/api/users/%s/remove-email", mHost.getUserId());
                JSONObject returnObj
                        = (JSONObject) httpPost(endpoint, user.getJson(), mHost.getAuthSecret());

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
        Then<User> then = new Then<>();

        class RequestTask extends AsyncTask<Void, Void, User> {

            @Override
            protected User doInBackground(Void... params) {
                User result;

                String endpoint = String.format("/api/users/%s", mHost.getUserId());
                JSONObject returnObj
                        = (JSONObject) httpPost(endpoint, user.getJson(), mHost.getAuthSecret());

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

    public Then<Meeting> edit(Meeting meeting) {
        Then<Meeting> then = new Then<>();

        class RequestTask extends AsyncTask<Void, Void, Meeting> {

            @Override
            protected Meeting doInBackground(Void... params) {
                Meeting result;

                String endpoint = String.format("/api/meetings/%s", meeting.getId());
                JSONObject returnObj
                        = (JSONObject) httpPost(endpoint, meeting.getJson(), mHost.getAuthSecret());

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

        new RequestTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        return then;
    }

    public Then<AgendaItem> edit(AgendaItem item, Meeting meeting) {
        Then<AgendaItem> then = new Then<>();

        class RequestTask extends AsyncTask<Void, Void, AgendaItem> {

            @Override
            protected AgendaItem doInBackground(Void... params) {
                AgendaItem result;
                String endpoint
                        = String.format("/api/meetings/%s/items/%s", meeting.getId(), item.getId());
                JSONObject returnObj = (JSONObject) httpPost(endpoint, item.getJson(), mHost.getAuthSecret());

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

        new RequestTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        return then;
    }

    public Then<Meeting> createMeeting(String title, Date time, String location, String description) {
        Then<Meeting> then = new Then<>();

        class RequestTask extends AsyncTask<Void, Void, Meeting> {

            @Override
            protected Meeting doInBackground(Void... params) {
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
                        = (JSONObject) httpPost("/api/meetings", content, mHost.getAuthSecret());

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

        new RequestTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        return then;
    }

    public Then<Meeting> createExampleMeeting() {
        Then<Meeting> then = new Then<>();

        class RequestTask extends AsyncTask<Void, Void, Meeting> {

            @Override
            protected Meeting doInBackground(Void... params) {
                Meeting result;

                JSONObject returnObj
                        = (JSONObject) httpPost(
                                "/api/create-example-meeting", mHost.getAuthSecret());

                result = new Meeting(returnObj);

                return result;
            }

            @Override
            protected void onPostExecute(Meeting result) {
                super.onPostExecute(result);

                then.compute(result);
            }
        }

        new RequestTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        return then;
    }

    public Then<Meeting> getMeeting(String id) {
        Then<Meeting> then = new Then<>();

        class RequestTask extends AsyncTask<Void, Void, Meeting> {
            @Override
            protected Meeting doInBackground(Void... params) {
                Meeting result;

                JSONObject returnObj = (JSONObject) httpGet("/api/meetings/" + id, mHost.getAuthSecret());

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

        new RequestTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        return then;
    }

    public Then<AgendaItem> createAgendaItem(
            String title, Integer duration, String description, Meeting meeting) {
        Then<AgendaItem> then = new Then<>();

        class RequestTask extends AsyncTask<Void, Void, AgendaItem> {

            @Override
            protected AgendaItem doInBackground(Void... params) {
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
                                meeting.getId()), content, mHost.getAuthSecret());

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

        new RequestTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        return then;
    }

    public Then<List<AgendaItem>> getAgendaItems(String meetingId) {
        Then<List<AgendaItem>> then = new Then<>();

        class RequestTask extends AsyncTask<Void, Void, List<AgendaItem>> {

            @Override
            protected List<AgendaItem> doInBackground(Void... params) {
                List<AgendaItem> result = new ArrayList<>();

                JSONArray returnArr
                        = (JSONArray) httpGet(String.format("/api/meetings/%s/items",
                                meetingId), mHost.getAuthSecret());
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

        new RequestTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        return then;
    }

    public Then<List<AgendaItem>> getTrashedAgendaItems(String meetingId) {
        Then<List<AgendaItem>> then = new Then<>();

        class RequestTask extends AsyncTask<Void, Void, List<AgendaItem>> {

            @Override
            protected List<AgendaItem> doInBackground(Void... params) {
                List<AgendaItem> result = new ArrayList<>();

                JSONArray returnArr
                        = (JSONArray) httpGet(String.format("/api/meetings/%s/items/trashed",
                                meetingId), mHost.getAuthSecret());
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

        new RequestTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        return then;
    }

    public void trashAgendaItem(AgendaItem item, Meeting meeting) {

        class RequestTask extends AsyncTask<Void, Void, Void> {

            @Override
            protected Void doInBackground(Void... params) {
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
                                meeting.getId()), content, mHost.getAuthSecret()));

                return null;
            }
        }

        new RequestTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void restoreAgendaItem(AgendaItem item, Meeting meeting) {

        class RequestTask extends AsyncTask<Void, Void, Void> {

            @Override
            protected Void doInBackground(Void... params) {
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
                                meeting.getId()), content, mHost.getAuthSecret()));

                return null;
            }
        }

        new RequestTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void moveAgendaItem(AgendaItem item, AgendaItem after, Meeting meeting) {

        class RequestTask extends AsyncTask<Void, Void, Void> {

            @Override
            protected Void doInBackground(Void... params) {
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
                                content, mHost.getAuthSecret()));

                return null;
            }
        }

        new RequestTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public Host getHost() {
        return mHost;
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

    private Object httpPost(String endpoint) {
        return httpPost(endpoint, null, null);
    }

    private Object httpPost(String endpoint, JSONObject content) {
        return httpPost(endpoint, content, null);
    }

    private Object httpPost(String endpoint, String authSecret) {
        return httpPost(endpoint, null, authSecret);
    }

    private Object httpPost(String endpoint, JSONObject content, String authSecret) {
        Object response;
        HttpURLConnection urlConnection = null;
        try { // TODO replace with try-with-resources
            URL url = new URL(mHost.getUrl() + endpoint);
            urlConnection = (HttpURLConnection) url.openConnection();

            urlConnection.setDoOutput(true);
            if (authSecret != null) {
                urlConnection.setRequestProperty("Cookie", "auth_secret=" + authSecret);
            }

            urlConnection.connect();

            if (content != null) {
                Log.v("HTTP POST", String.format(
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
        HttpURLConnection urlConnection = null;
        try { // TODO replace with try-with-resources once minSdkVersion is 19
            URL url = new URL(mHost.getUrl() + endpoint);
            urlConnection = (HttpURLConnection) url.openConnection();

            urlConnection.setDoInput(true);
            if (authSecret != null) {
                Log.v("HTTP GET", endpoint + ", auth_secret=" + authSecret);
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

    private Object readStream(HttpURLConnection urlConnection) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();

        InputStream in = null;
        BufferedReader reader = null;
        try { // TODO replace with try-with-resources
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
            Log.v("HTTP response body", content);
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
