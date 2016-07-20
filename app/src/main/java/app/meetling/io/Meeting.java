package app.meetling.io;

import android.os.Parcel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A meeting containing meta data (title, location etc.), an agenda, and an item trash.
 */
public class Meeting extends Object implements Editable<User> {
    public static final String EXTRA_MEETING = Meeting.class.getPackage() + ".EXTRA_MEETING";
    public static final String CONTENT_TYPE = "meetling/meeting";

    public static final Creator<Meeting> CREATOR = new Creator<Meeting>() {
        @Override
        public Meeting createFromParcel(Parcel in) {
            return new Meeting(in);
        }

        @Override
        public Meeting[] newArray(int size) {
            return new Meeting[size];
        }
    };
    private List<User> mAuthors;
    private String mTitle;
    private Date mDate;
    private String mLocation;
    private String mDescription;

    Meeting(String type, String id, Boolean trashed, List<User> authors, String title, Date date,
            String location, String description) {
        super(type, id, trashed);

        mAuthors = authors;
        mTitle = title;
        mDate = date;
        mLocation = location;
        mDescription = description;
    }

    Meeting(JSONObject obj) {
        super(obj);

        try {
            mAuthors = Util.extractAuthors(obj);
            mTitle = obj.getString("title");
            mDate = obj.get("time") == JSONObject.NULL ?
                    null : Util.fromMeetlingTimeFormat(obj.getString("time"));
            mLocation = obj.get("location") == JSONObject.NULL ? null : obj.getString("location");
            mDescription
                    = obj.get("description") == JSONObject.NULL ?
                            null : obj.getString("description");
        } catch (JSONException e) {
            // unreachable
            throw new RuntimeException(e);
        }
    }

    private Meeting(Parcel in) {
        super(in);

        mAuthors = new ArrayList<>();
        in.readTypedList(mAuthors, User.CREATOR);
        mTitle = in.readString();
        long time = in.readLong();
        mDate = time == 0 ? null : new Date(time);
        mLocation = in.readString();
        mDescription = in.readString();
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public Date getDate() {
        return mDate;
    }

    public void setDate(Date date) {
        mDate = date;
    }

    public String getLocation() {
        return mLocation;
    }

    public void setLocation(String location) {
        mLocation = location;
    }

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String description) {
        mDescription = description;
    }

    @Override
    public List<User> getAuthors() {
        return mAuthors;
    }

    @Override
    public JSONObject getJson() {
        JSONObject obj = super.getJson();

        try {
            if (mAuthors != null) {
                JSONArray authorArr = new JSONArray();
                for (User author : mAuthors) {
                    authorArr.put(author.getJson());
                }
                obj.put("authors", authorArr);
            }
            obj.put("title", mTitle);
            obj.put("time", mDate == null ? JSONObject.NULL : Util.toMeetlingTimeFormat(mDate));
            obj.put("location", Util.toNullableJsonValue(mLocation));
            obj.put("description", Util.toNullableJsonValue(mDescription));
        } catch (JSONException e) {
            // unreachable
            throw new RuntimeException(e);
        }

        return obj;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);

        dest.writeTypedList(mAuthors);
        dest.writeString(mTitle);
        dest.writeLong(mDate == null ? 0 : mDate.getTime());
        dest.writeString(mLocation);
        dest.writeString(mDescription);
    }
}
