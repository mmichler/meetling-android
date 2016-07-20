package app.meetling.ui;

import android.content.Context;
import android.support.annotation.CallSuper;
import android.support.v4.app.Fragment;

/**
 * Base class for all Fragments in the Meetling app that are not derived from an Android specialized
 * Fragment class (like dialogs).
 *
 * Classes extending this Fragment must have a Toolbar with the id <code>"@+id/toolbar"</code>
 * implemented in their layout XML.
 */
public abstract class MeetlingFragment<CallbackT> extends Fragment {
    protected CallbackT mCallback;

    @Override
    @CallSuper
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            mCallback = (CallbackT) context;
        } catch (ClassCastException e) {
            throw new RuntimeException(
                    context.toString() + " must implement callback Interface of class "
                            + this.getClass());
        }
    }

    @Override
    @CallSuper
    public void onDetach() {
        super.onDetach();

        mCallback = null;
    }
}
