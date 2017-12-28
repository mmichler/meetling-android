package app.meetling.io;

import android.util.Pair;

/**
 * Callback class for interactions with the web API and local storage.
 */
public class Then<Result> {
    private Callback<Result> mCallback;

    void setError(RuntimeException error) {
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
        private RuntimeException mError;

        void setError(RuntimeException error) {
            mError = error;
        }

        protected RuntimeException getError() {
            return mError;
        }

        /**
         * Convenience method for rethrowing errors on the main thread.
         */
        protected void rethrowError() {
            if (mError != null) {
                throw mError;
            }
        }

        public abstract void call(Result result);
    }
}
