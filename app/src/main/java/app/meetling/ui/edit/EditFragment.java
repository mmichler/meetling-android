package app.meetling.ui.edit;

import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import app.meetling.R;
import app.meetling.ui.MeetlingFragment;

/**
 * Base class for all Fragments with editing capability. Incorporates a save/cancel toolbar menu.
 *
 * Fragments implementing this Class need an XML menu named <code>edit</code> with an action
 * id <code>action_cancel</code>.
 */
public abstract class EditFragment<CallbackT extends EditFragment.Callback>
        extends MeetlingFragment<CallbackT> {

    @Override
    @CallSuper
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.edit);
        toolbar.setNavigationOnClickListener(createHomeClickListener());
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.action_cancel) {
                    mCallback.onCancel();
                    return true;
                } else {
                    return false;
                }
            }
        });
    }

    protected abstract View.OnClickListener createHomeClickListener();

    public interface Callback {
        void onCancel();
    }
}
