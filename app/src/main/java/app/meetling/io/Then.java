package app.meetling.io;

/**
 * Callback class for interactions with the web API and local storage.
 */
public class Then<Result> {
    private Callback<Result> mCallback;

    public void then(Callback<Result> callback) {
        mCallback = callback;
    }

    void compute(Result result) {
        if (mCallback != null) {
            mCallback.call(result);
        }
    }

    public static abstract class Callback<Result> {
        public abstract void call(Result result);
    }
}
