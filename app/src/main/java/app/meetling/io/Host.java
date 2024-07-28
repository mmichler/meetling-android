package app.meetling.io;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.util.Objects;

/**
 * Created by markus on 26.09.17.
 */

public class Host implements Parcelable {
    public static final String EXTRA_HOST = Meeting.class.getPackage() + ".EXTRA_HOST";

    private int mId;
    @NonNull
    private String mUrl;
    private String mUserId;
    private String mAuthSecret;

    Host(int id, String url) {
        this(id, url, null, null);
    }

    Host(int id, String url, String userId, String authSecret) {
        if (userId == null ^ authSecret == null) {
            throw new IllegalArgumentException("One of userId or authSecret is null and the other is not.");
        }
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("URL may not be null or empty");
        }

        mId = id;
        mUrl = url;
        mUserId = userId;
        mAuthSecret = authSecret;
    }

    private Host(Parcel in) {
        mId = in.readInt();
        mUrl = in.readString();
        mUserId = in.readString();
        mAuthSecret = in.readString();
    }

    public static final Creator<Host> CREATOR = new Creator<Host>() {
        @Override
        public Host createFromParcel(Parcel in) {
            return new Host(in);
        }

        @Override
        public Host[] newArray(int size) {
            return new Host[size];
        }
    };

    public int getId() {
        return mId;
    }

    public String getUrl() {
        return mUrl;
    }

    public String getUserId() {
        return mUserId;
    }

    public String getAuthSecret() {
        return mAuthSecret;
    }

    public boolean hasCredentials() {
        return mUserId != null;
    }

    @Override
    public boolean equals(java.lang.Object other) {
        return other.getClass() == Host.class && ((Host) other).getId() == mId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(String.valueOf(mId), mUrl, mUserId, mAuthSecret);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mId);
        dest.writeString(mUrl);
        dest.writeString(mUserId);
        dest.writeString(mAuthSecret);
    }
}
