package app.meetling.io;

import android.os.Parcel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
/**
 * Item on a Meetling Agenda.
 */
public class AgendaItem extends Object implements Editable<User> {
    public static final String EXTRA_AGENDA_ITEM
            = AgendaItem.class.getPackage() + ".EXTRA_AGENDA_ITEM";
    public static final String CONTENT_TYPE = "meetling/agenda_item";

    public static final Creator<AgendaItem> CREATOR = new Creator<AgendaItem>() {
        @Override
        public AgendaItem createFromParcel(Parcel in) {
            return new AgendaItem(in);
        }

        @Override
        public AgendaItem[] newArray(int size) {
            return new AgendaItem[size];
        }
    };
    private List<User> mAuthors;
    private String mTitle;
    private Integer mDuration;
    private String mDescription;

    AgendaItem(String type, String id, Boolean trashed, List<User> authors, String title,
               Integer duration, String description) {
        super(type, id, trashed);

        mAuthors = authors;
        mTitle = title;
        mDuration = duration;
        mDescription = description;
    }

    AgendaItem(JSONObject obj) {
        super(obj);

        try {
            mAuthors = Util.extractAuthors(obj);
            mTitle = obj.getString("title");
            Integer duration = obj.optInt("duration");
            if (duration != 0) {
                mDuration = duration;
            }
            String description = obj.optString("description");
            if (!description.equals("null")) {
                mDescription = description;
            }
        } catch (JSONException e) {
            // unreachable
            throw new RuntimeException(e);
        }
    }

    private AgendaItem(Parcel in) {
        super(in);

        mAuthors = new ArrayList<>();
        in.readTypedList(mAuthors, User.CREATOR);
        mTitle = in.readString();
        int duration = in.readInt();
        mDuration = duration == 0 ? null : duration;
        mDescription = in.readString();
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
            obj.put("duration", mDuration == null ? JSONObject.NULL : mDuration);
            obj.put("description", Util.toNullableJsonValue(mDescription));
        } catch (JSONException e) {
            // unreachable
            throw new RuntimeException(e);
        }

        return obj;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public Integer getDuration() {
        return mDuration;
    }

    public void setDuration(Integer duration) {
        mDuration = duration;
    }

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String description) {
        mDescription = description;
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
        dest.writeInt(mDuration == null ? 0 : mDuration);
        dest.writeString(mDescription);
    }
}
