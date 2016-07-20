package app.meetling.io;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.CallSuper;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Base class for all Meetling data objects.
 */
public abstract class Object implements Parcelable {
    private String mType;
    private String mId;
    private boolean mTrashed;

    Object(String type, String id, boolean trashed) {
        mType = type;
        mId = id;
        mTrashed = trashed;
    }

    Object(JSONObject obj) {
        try {
            mType = obj.getString("__type__");
            mId = obj.getString("id");
            mTrashed = obj.getBoolean("trashed");
        } catch (JSONException e) {
            // unreachable
            throw new RuntimeException(e);
        }
    }

    Object(Parcel in) {
        mType = in.readString();
        mId = in.readString();
        mTrashed = in.readInt() != 0;
    }

    @CallSuper
    JSONObject getJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("__type__", mType);
            obj.put("id", getId());
            obj.put("trashed", isTrashed());
        } catch (JSONException e) {
            // unreachable
            throw new RuntimeException(e);
        }

        return obj;
    }

    @Override
    @CallSuper
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mType);
        dest.writeString(mId);
        dest.writeInt(mTrashed ? 1 : 0);
    }

    public String getType() {
        return mType;
    }

    public String getId() {
        return mId;
    }

    public boolean isTrashed() {
        return mTrashed;
    }

    public boolean equals(Object o) {
        return o != null && this.getId().equals(o.getId());
    }
}
