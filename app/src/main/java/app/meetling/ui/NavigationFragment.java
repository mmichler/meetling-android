package app.meetling.ui;

import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.design.widget.NavigationView;
import android.support.v7.graphics.drawable.DrawerArrowDrawable;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import app.meetling.R;
import app.meetling.io.Meeting;
import app.meetling.io.User;
import app.meetling.ui.edit.EditMeetingDialog;
import app.meetling.ui.edit.EditUserDialog;

/**
 * Base class for all data displaying Fragments that are accessed via the navigation drawer.
 */
public abstract class NavigationFragment<CallbackT extends NavigationFragment.Callback>
        extends MeetlingFragment<CallbackT> implements EditUserDialog.Listener, EditMeetingDialog.Listener {

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

    @Override
    public void onEdited(User user) {
        NavigationView view = (NavigationView) getActivity().findViewById(R.id.navigation_view);
        if (view == null) {
            throw new IllegalStateException("View navigation_view not found");
        }
        ((TextView) view.getHeaderView(0).findViewById(R.id.user_name)).setText(user.getName());
    }

    @Override
    public void onEdited(Meeting meeting) {
        ((MainActivity) getActivity()).showMeeting(meeting);

        MainActivity mainActivity = (MainActivity) getActivity();
        mainActivity.addToHistory(meeting.getId());
        mainActivity.showHistory();
    }

    public interface Callback {
        void onSelectHome();
    }
}
