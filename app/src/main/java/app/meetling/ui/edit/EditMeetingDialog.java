package app.meetling.ui.edit;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatDialogFragment;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.TimePicker;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import app.meetling.R;
import app.meetling.io.Meeting;
import app.meetling.io.Then;
import app.meetling.io.User;
import app.meetling.io.WebApi;

import static app.meetling.io.Meeting.EXTRA_MEETING;
import static app.meetling.io.User.EXTRA_USER;
import static app.meetling.io.WebApi.EXTRA_API_HOST;

public class EditMeetingDialog extends AppCompatDialogFragment
        implements TimePickerDialog.OnTimeSetListener, DatePickerDialog.OnDateSetListener {
    private User mUser;
    private WebApi mApi;

    private Listener mListener;
    private boolean mDateSet;
    private boolean mTimeSet;
    private Meeting mMeeting;
    private Calendar mCalendar;
    TextInputLayout mInputLayoutDate;
    TextInputLayout mInputLayoutTime;
    private TextInputEditText mInputTitle;
    private TextInputEditText mInputDate;
    private TextInputEditText mInputTime;
    private TextInputEditText mInputLocation;
    private TextInputEditText mInputDescription;

    public static EditMeetingDialog newInstance(User user, String host) {
        EditMeetingDialog dialog = new EditMeetingDialog();
        Bundle args = new Bundle();
        args.putParcelable(EXTRA_USER, user);
        args.putString(EXTRA_API_HOST, host);
        dialog.setArguments(args);
        return dialog;
    }

    public static EditMeetingDialog newInstance(Meeting meeting, User user, String host) {
        EditMeetingDialog dialog = new EditMeetingDialog();
        Bundle args = new Bundle();
        args.putParcelable(EXTRA_MEETING, meeting);
        args.putParcelable(EXTRA_USER, user);
        args.putString(EXTRA_API_HOST, host);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            mListener = (Listener) getTargetFragment();
        } catch (ClassCastException e) {
            throw new RuntimeException(
                    getTargetFragment().toString() + " must implement listener Interface of class "
                            + this.getClass());
        }
        if (mListener == null) {
            throw new RuntimeException("This dialog needs a target fragment");
        }

        Bundle args = getArguments();
        if (args != null) {
            mMeeting = args.getParcelable(EXTRA_MEETING);
            mUser = args.getParcelable(EXTRA_USER);
            mApi = new WebApi(args.getString(EXTRA_API_HOST));
        } else {
            throw new IllegalArgumentException("Args may not be null");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new DialogUtil.PromptOnChangesDialog(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        return inflater.inflate(R.layout.fragment_edit_meeting, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> getDialog().dismiss());

        Button saveButton = (Button) view.findViewById(R.id.saveMeeting);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mInputTitle.getText().toString().isEmpty()) {
                    Snackbar.make(
                            view.findViewById(R.id.edit_meeting_layout),
                            R.string.toast_title_empty, Snackbar.LENGTH_LONG).show();
                } else if (mDateSet != mTimeSet) {
                    Snackbar.make(
                            view.findViewById(R.id.edit_meeting_layout),
                            R.string.toast_date_time_not_set, Snackbar.LENGTH_LONG).show();
                } else {
                    String title = mInputTitle.getText().toString();
                    Date date = mCalendar == null ? null : mCalendar.getTime();
                    String location = mInputLocation.getText().toString();
                    String description = mInputDescription.getText().toString();

                    Then.Callback<Meeting> returnMeeting = new Then.Callback<Meeting>() {
                        @Override
                        public void call(Meeting meeting) {
                            mListener.onEdited(meeting);
                            ((DialogUtil.PromptOnChangesDialog) getDialog())
                                    .ignoreChanges().dismiss();
                        }
                    };
                    // TODO show progressbar

                    if (mMeeting == null) {
                        mApi.createMeeting(title, date, location, description, mUser)
                                .then(returnMeeting);
                    } else {
                        mMeeting.setTitle(title);
                        mMeeting.setDate(date);
                        mMeeting.setLocation(location);
                        mMeeting.setDescription(description);

                        mApi.edit(mMeeting, mUser).then(returnMeeting);
                    }
                }
            }
        });

        final TextInputLayout inputLayoutTitle
                = (TextInputLayout) view.findViewById(R.id.input_layout_title);
        mInputTitle = (TextInputEditText) view.findViewById(R.id.input_title);
        DialogUtil.monitorForEmpty(mInputTitle, inputLayoutTitle);
        mInputLayoutDate = (TextInputLayout) view.findViewById(R.id.input_layout_date);
        mInputLayoutTime = (TextInputLayout) view.findViewById(R.id.input_layout_time);
        mInputDate = (TextInputEditText) view.findViewById(R.id.input_date);
        mInputTime = (TextInputEditText) view.findViewById(R.id.input_time);
        mInputTime.setOnTouchListener((v, event) -> {
            hideSoftKeyboard();
            return false;
        });
        mInputTime.setOnTouchListener((v, event) -> {
            hideSoftKeyboard();
            return false;
        });

        ImageButton actionPickDate = (ImageButton) view.findViewById(R.id.action_pick_date);
        actionPickDate.setOnClickListener(v -> {
            if (mCalendar == null) {
                mCalendar = Calendar.getInstance();
            }
            int year = mCalendar.get(Calendar.YEAR);
            int month = mCalendar.get(Calendar.MONTH);
            int day = mCalendar.get(Calendar.DAY_OF_MONTH);

            new DatePickerDialog(
                    getContext(), 0, EditMeetingDialog.this, year, month,
                    day).show();

            hideSoftKeyboard();
            mInputDate.requestFocus();
        });

        ImageButton actionPickTime = (ImageButton) view.findViewById(R.id.action_pick_time);
        actionPickTime.setOnClickListener(v -> {
            if (mCalendar == null) {
                mCalendar = Calendar.getInstance();
            }
            int hour = mCalendar.get(Calendar.HOUR_OF_DAY);
            int minute = mCalendar.get(Calendar.MINUTE);

            new TimePickerDialog(
                    getContext(), 0, EditMeetingDialog.this, hour, minute,
                    android.text.format.DateFormat.is24HourFormat(getActivity())).show();

            hideSoftKeyboard();
            mInputTime.requestFocus();
        });

        ImageButton actionClearDate = (ImageButton) view.findViewById(R.id.action_clear_date);
        actionClearDate.setOnClickListener(v -> {
            mDateSet = false;
            if (!mTimeSet) {
                mCalendar = null;
            }
            updateDateTimeFields();
            checkForIncompleteDateTime();
        });

        ImageButton actionClearTime = (ImageButton) view.findViewById(R.id.action_clear_time);
        actionClearTime.setOnClickListener(v -> {
            mTimeSet = false;
            if (!mDateSet) {
                mCalendar = null;
            }
            updateDateTimeFields();
            checkForIncompleteDateTime();
        });

        mInputLocation = (TextInputEditText) view.findViewById(R.id.input_location);
        mInputDescription = (TextInputEditText) view.findViewById(R.id.input_description);

        if (mMeeting == null) {
            return;
        }
        if (mMeeting.getDate() != null) {
            mCalendar = Calendar.getInstance();
            mCalendar.setTime(mMeeting.getDate());
            mDateSet = true;
            mTimeSet = true;
            updateDateTimeFields();
        }
        mInputTitle.setText(mMeeting.getTitle());
        mInputLocation.setText(mMeeting.getLocation());
        mInputDescription.setText(mMeeting.getDescription());
        ((DialogUtil.PromptOnChangesDialog) getDialog())
                .watchValue(mInputTitle, mInputTitle.getText().toString())
                .watchValue(mInputDate, mInputDate.getText().toString())
                .watchValue(mInputTime, mInputTime.getText().toString())
                .watchValue(mInputLocation, mInputLocation.getText().toString())
                .watchValue(mInputDescription, mInputDescription.getText().toString());
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        mCalendar.set(Calendar.YEAR, year);
        mCalendar.set(Calendar.MONTH, month);
        mCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        mDateSet = true;
        updateDateTimeFields();
        checkForIncompleteDateTime();
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        mCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        mCalendar.set(Calendar.MINUTE, minute);
        mTimeSet = true;
        updateDateTimeFields();
        checkForIncompleteDateTime();
    }

    private void updateDateTimeFields() {
        DateFormat dateDisplayFormat = android.text.format.DateFormat.getDateFormat(getContext());
        DateFormat timeDisplayFormat = android.text.format.DateFormat.getTimeFormat(getContext());
        if (mDateSet) {
            mInputDate.setText(dateDisplayFormat.format(mCalendar.getTime()));
        } else {
            mInputDate.setText(null);
        }
        if (mTimeSet) {
            mInputTime.setText(timeDisplayFormat.format(mCalendar.getTime()));
        } else {
            mInputTime.setText(null);
        }
    }

    private void hideSoftKeyboard() {
        InputMethodManager imm
                = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mInputTitle.getWindowToken(), 0);
    }

    private void checkForIncompleteDateTime() {
        if (mDateSet == mTimeSet) {
            mInputLayoutDate.setError(null);
            mInputLayoutTime.setError(null);
            return;
        }

        if (mTimeSet) {
            mInputLayoutDate.setError(getString(R.string.error_date_mandatory));
        }

        if (mDateSet) {
            mInputLayoutTime.setError(getString(R.string.error_time_mandatory));
        }
    }

    public interface Listener {
        void onEdited(Meeting meeting);
    }
}
