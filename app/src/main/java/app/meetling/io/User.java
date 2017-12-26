package app.meetling.io;

import android.os.Parcel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Meetling user. The local user has <code>AuthSecret</code> set for web API
 * authentication purposes.
 */
public class User extends Object implements Editable<String> {
    public static final String EXTRA_USER = User.class.getPackage() + ".EXTRA_USER";
    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };
    private List<String> mAuthorIds;
    private String mName;
    private String mEmail;
    private String mAuthSecret;

    User(String type, String id, Boolean trashed, List<String> authorIds, String name, String email,
            String authSecret) {
        super(type, id, trashed);

        mAuthorIds = authorIds;
        mName = name;
        mEmail = email;
        mAuthSecret = authSecret;
    }

    User(JSONObject obj) {
        super(obj);

        try {
            mAuthorIds = new ArrayList<>();
            JSONArray authorsArr = obj.getJSONArray("authors");
            for (int i = 0; i < authorsArr.length(); i++) {
                mAuthorIds.add(authorsArr.getString(i));
            }
            mName = obj.getString("name");
            if (!obj.isNull("email")) {
                mEmail = obj.optString("email");
            }
            mAuthSecret = obj.optString("auth_secret");
        } catch (JSONException e) {
            // unreachable
            throw new RuntimeException(e);
        }
    }

    private User(Parcel in) {
        super(in);

        mAuthorIds = new ArrayList<>();
        in.readStringList(mAuthorIds);
        mName = in.readString();
        mEmail = in.readString();
        mAuthSecret = in.readString();
    }

    @Override
    public JSONObject getJson() {
        JSONObject obj = super.getJson();

        try {
            JSONArray authorIdArr = new JSONArray();
            for (String id : mAuthorIds) {
                authorIdArr.put(id);
            }
            obj.put("authors", authorIdArr);
            obj.put("name", mName);
            obj.put("email", Util.toNullableJsonValue(mEmail));
            obj.put("auth_secret", mAuthSecret);
        } catch (JSONException e) {
            // unreachable
            throw new RuntimeException(e);
        }

        return obj;
    }

    public String getName() {
        return mName;
    }

    // TODO remove, name should only be set via API call
    public void setName(String name) {
        mName = name;
    }

    public String getEmail() {
        return mEmail;
    }

    public String getAuthSecret() {
        return mAuthSecret;
    }

    @Override
    public List<String> getAuthors() {
        return mAuthorIds;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);

        dest.writeStringList(mAuthorIds);
        dest.writeString(mName);
        dest.writeString(mEmail);
        dest.writeString(mAuthSecret);
    }
}
