package app.meetling.ui;

import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.design.widget.NavigationView;
import android.support.v7.graphics.drawable.DrawerArrowDrawable;
import android.support.v7.widget.Toolbar;
import android.view.View;

import app.meetling.R;
import app.meetling.io.Meeting;
import app.meetling.io.User;
import app.meetling.ui.edit.EditMeetingDialog;
import app.meetling.ui.edit.EditUserNameDialog;
import app.meetling.ui.edit.SetUserEmailDialog;
import app.meetling.ui.edit.SubmitAuthCodeDialog;

/**
 * Base class for all data displaying Fragments that are accessed via the navigation drawer.
 */
public abstract class NavigationFragment<CallbackT extends NavigationFragment.Callback>
        extends MeetlingFragment<CallbackT> implements EditUserNameDialog.Listener,
        EditMeetingDialog.Listener, SetUserEmailDialog.Listener, SubmitAuthCodeDialog.Listener {
    NavigationView mNavigationView;
    MainActivity mMainActivity;

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

        mNavigationView = (NavigationView) getActivity().findViewById(R.id.navigation_view);
        if (mNavigationView == null) {
            throw new IllegalStateException("View navigation_view not found");
        }

        mMainActivity = (MainActivity) getActivity();
    }

    @Override
    public void onEditedUserName(User user) {
        mMainActivity.setUser(user);
        mMainActivity.showUser();
    }

    @Override
    public void onEdited(Meeting meeting) {
        mMainActivity.showMeeting(meeting);
        mMainActivity.addToHistory(meeting.getId());
        mMainActivity.showHistory();
    }

    @Override
    public void onSetUserEmail() {
        mMainActivity.showUser();
    }

    @Override
    public void onSubmittedAuthCode(User user) {
        mMainActivity.setUser(user);
        mMainActivity.showUser();
    }

    public interface Callback {
        void onSelectHome();
    }
}
