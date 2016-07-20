package app.meetling.ui.edit;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.format.DateFormat;

import app.meetling.R;

/**
 * Host the time picker dialog.
 */
public class TimePickerFragment extends DialogFragment {
    private static final String HOUR = "hour";
    private static final String MINUTE = "minute";
    private TimePickerDialog.OnTimeSetListener mListener;
    private int mHour;
    private int mMinute;

    public static TimePickerFragment newInstance(int hour, int minute) {
        TimePickerFragment fragment = new TimePickerFragment();
        Bundle args = new Bundle();
        args.putInt(HOUR, hour);
        args.putInt(MINUTE, minute);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof TimePickerDialog.OnTimeSetListener) {
            mListener = (TimePickerDialog.OnTimeSetListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnTimeSetListener");
        }

        Bundle args = getArguments();
        if (args != null) {
            mHour = args.getInt(HOUR);
            mMinute = args.getInt(MINUTE);
        } else {
            throw new IllegalArgumentException("Args may not be null");
        }
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new TimePickerDialog(
                getContext(), R.style.MeetlingDialog, mListener, mHour, mMinute, DateFormat
                        .is24HourFormat(getActivity()));
    }

    @Override
    public void onDetach() {
        super.onDetach();

        mListener = null;
    }
}
