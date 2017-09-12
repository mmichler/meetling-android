package app.meetling.io;

import android.util.Pair;

/**
 * Callback class for interactions with the web API and local storage.
 */
public class Then<Result> {
    private Callback<Result> mCallback;

    void setError(Pair<String, String> error) {
        if (mCallback != null) {
           mCallback.setError(error);
        }
    }

    public void then(Callback<Result> callback) {
        mCallback = callback;
    }

    void compute(Result result) {
        if (mCallback != null) {
            mCallback.call(result);
        }
    }

    public static abstract class Callback<Result> {
        private Pair<String, String> mError;

        void setError(Pair<String, String> error) {
            mError = error;
        }

        protected Pair<String, String> getError() {
            return mError;
        }

        public abstract void call(Result result);
    }
}
