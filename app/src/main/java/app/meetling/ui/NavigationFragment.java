package app.meetling.ui;

import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.v7.graphics.drawable.DrawerArrowDrawable;
import android.support.v7.widget.Toolbar;
import android.view.View;

import app.meetling.R;

/**
 * Base class for all data displaying Fragments that are accessed via the navigation drawer.
 */
public abstract class NavigationFragment<CallbackT extends NavigationFragment.Callback>
        extends MeetlingFragment<CallbackT> {

    @Override
    @CallSuper
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallback.onSelectHome();
            }
        });
        toolbar.setNavigationIcon(new DrawerArrowDrawable(toolbar.getContext()));
        toolbar.setNavigationContentDescription(R.string.navigation_drawer_open);
    }

    public interface Callback {
        void onSelectHome();
    }
}
