package app.meetling.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import app.meetling.R;

/**
 * Fragment to be shown at the "Meeting" navigation level when no meeting has been selected.
 */
public class GreeterFragment extends NavigationFragment<GreeterFragment.Callback> {

    public static GreeterFragment newInstance() {
        return new GreeterFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        return inflater.inflate(R.layout.fragment_greeter, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Button actionNewMeeting = (Button) getActivity().findViewById(R.id.action_new_meeting);
        actionNewMeeting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallback.onCreateMeeting();
            }
        });
        Button actionViewExample = (Button) getActivity().findViewById(R.id.action_view_example);
        actionViewExample.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallback.onViewExample();
            }
        });
    }

    public interface Callback extends NavigationFragment.Callback {
        void onViewExample();
        void onCreateMeeting();
    }
}
