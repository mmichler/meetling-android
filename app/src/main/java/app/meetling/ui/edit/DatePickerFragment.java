package app.meetling.ui.edit;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import app.meetling.R;

/**
 * Hosts the datePicker dialog.
 */
public class DatePickerFragment extends DialogFragment {
    private static final String YEAR = "hour";
    private static final String MONTH = "month";
    private static final String DAY = "day";
    private DatePickerDialog.OnDateSetListener mListener;
    private int mYear;
    private int mMonth;
    private int mDay;

    public static DatePickerFragment newInstance(int year, int month, int day) {
        DatePickerFragment fragment = new DatePickerFragment();
        Bundle args = new Bundle();
        args.putInt(YEAR, year);
        args.putInt(MONTH, month);
        args.putInt(DAY, day);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof TimePickerDialog.OnTimeSetListener) {
            mListener = (DatePickerDialog.OnDateSetListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnDateSetListener");
        }

        Bundle args = getArguments();
        if (args != null) {
            mYear = args.getInt(YEAR);
            mMonth = args.getInt(MONTH);
            mDay = args.getInt(DAY);
        } else {
            throw new IllegalArgumentException("Args may not be null");
        }
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new DatePickerDialog(
                getContext(), R.style.MeetlingDialog, mListener, mYear, mMonth, mDay);
    }

    @Override
    public void onDetach() {
        super.onDetach();

        mListener = null;
    }
}
