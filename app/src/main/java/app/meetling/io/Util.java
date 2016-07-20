package app.meetling.io;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Utility class for data manipulation.
 */
class Util {
    private static final SimpleDateFormat meetlingDateFormat;

    static {
        meetlingDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        meetlingDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    static List<User> extractAuthors(JSONObject obj) throws JSONException {
        List<User> authors = new ArrayList<>();
        JSONArray authorsArr = obj.getJSONArray("authors");
        for (int i = 0; i < authorsArr.length(); i++) {
            authors.add(new User(authorsArr.getJSONObject(i)));
        }

        return authors;
    }

    static String toMeetlingTimeFormat(Date date) {
        return meetlingDateFormat.format(date);
    }

    static Date fromMeetlingTimeFormat(String time) {
        try {
            return meetlingDateFormat.parse(time);
        } catch (ParseException e) {
            // unreachable
            throw new RuntimeException(e);
        }
    }

    static java.lang.Object toNullableJsonValue(String string) {
        return (string == null || string.isEmpty()) ? JSONObject.NULL : string;
    }
}
