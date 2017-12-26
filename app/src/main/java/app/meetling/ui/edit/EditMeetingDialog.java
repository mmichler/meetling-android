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
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Switch;
import android.widget.TimePicker;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

import app.meetling.R;
import app.meetling.io.Host;
import app.meetling.io.Meeting;
import app.meetling.io.Then;
import app.meetling.io.User;
import app.meetling.io.WebApi;

import static app.meetling.io.Host.EXTRA_HOST;
import static app.meetling.io.Meeting.EXTRA_MEETING;
import static app.meetling.io.User.EXTRA_USER;

public class EditMeetingDialog extends AppCompatDialogFragment
        implements TimePickerDialog.OnTimeSetListener, DatePickerDialog.OnDateSetListener {
    private WebApi mApi;

    private Listener mListener;
    private Meeting mMeeting;
    private Calendar mCalendar;
    private TextInputLayout mInputLayoutTitle;
    private TextInputEditText mInputTitle;
    private Switch mSwitchDate;
    private Button mButtonDate;
    private Button mButtonTime;
    private TextInputEditText mInputLocation;
    private TextInputEditText mInputDescription;
    private DateFormat mDateDisplayFormat;
    private DateFormat mTimeDisplayFormat;

    public static EditMeetingDialog newInstance(Host host) {
        return newInstance(null, host);
    }

    public static EditMeetingDialog newInstance(Meeting meeting, Host host) {
        EditMeetingDialog dialog = new EditMeetingDialog();
        Bundle args = new Bundle();
        args.putParcelable(EXTRA_MEETING, meeting);
        args.putParcelable(EXTRA_HOST, host);
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
            mApi = new WebApi(args.getParcelable(EXTRA_HOST));
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
        saveButton.setOnClickListener(v -> {
            if (mInputTitle.getText().toString().isEmpty()) {
                Snackbar.make(
                        view.findViewById(R.id.edit_meeting_layout),
                        R.string.toast_title_empty, Snackbar.LENGTH_LONG).show();
            } else {
                String title = mInputTitle.getText().toString();
                Date date = mSwitchDate.isChecked() ? mCalendar.getTime() : null;
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
                    mApi.createMeeting(title, date, location, description)
                            .then(returnMeeting);
                } else {
                    mMeeting.setTitle(title);
                    mMeeting.setDate(date);
                    mMeeting.setLocation(location);
                    mMeeting.setDescription(description);

                    mApi.edit(mMeeting).then(returnMeeting);
                }
            }
        });

        mDateDisplayFormat = android.text.format.DateFormat.getDateFormat(getContext());
        mTimeDisplayFormat = android.text.format.DateFormat.getTimeFormat(getContext());

        mInputLayoutTitle
                = (TextInputLayout) view.findViewById(R.id.input_layout_title);
        mInputTitle = (TextInputEditText) view.findViewById(R.id.input_title);
        DialogUtil.monitorForEmpty(mInputTitle, mInputLayoutTitle);

        mSwitchDate = (Switch) view.findViewById(R.id.date_switch);
        mSwitchDate.setOnCheckedChangeListener((buttonView, isChecked) -> {
            hideSoftKeyboard();
            mButtonDate.setEnabled(isChecked);
            mButtonTime.setEnabled(isChecked);
            mButtonDate.setText(isChecked ? mDateDisplayFormat.format(mCalendar.getTime()) : "");
            mButtonTime.setText(isChecked ? mTimeDisplayFormat.format(mCalendar.getTime()) : "");
        });
        mButtonDate = (Button) view.findViewById(R.id.date_button);
        mButtonDate.setOnClickListener(v -> {
            hideSoftKeyboard();
            int year = mCalendar.get(Calendar.YEAR);
            int month = mCalendar.get(Calendar.MONTH);
            int day = mCalendar.get(Calendar.DAY_OF_MONTH);
            new DatePickerDialog(
                    getContext(), 0, EditMeetingDialog.this, year, month,
                    day).show();
        });
        mButtonTime = (Button) view.findViewById(R.id.time_button);
        mButtonTime.setOnClickListener(v -> {
            hideSoftKeyboard();
            int hour = mCalendar.get(Calendar.HOUR_OF_DAY);
            int minute = mCalendar.get(Calendar.MINUTE);
            new TimePickerDialog(
                    getContext(), 0, EditMeetingDialog.this, hour, minute,
                    android.text.format.DateFormat.is24HourFormat(getActivity())).show();
        });

        mInputLocation = (TextInputEditText) view.findViewById(R.id.input_location);
        mInputDescription = (TextInputEditText) view.findViewById(R.id.input_description);

        mCalendar = Calendar.getInstance();
        Date meetingDate = null;
        if (mMeeting != null) {
            mInputTitle.setText(mMeeting.getTitle());
            mInputLocation.setText(mMeeting.getLocation());
            mInputDescription.setText(mMeeting.getDescription());
            meetingDate = mMeeting.getDate();
        }

        if (meetingDate != null) {
            mCalendar.setTime(mMeeting.getDate());
            mButtonDate.setText(mDateDisplayFormat.format(mCalendar.getTime()));
            mButtonTime.setText(mTimeDisplayFormat.format(mCalendar.getTime()));
        }

        ((DialogUtil.PromptOnChangesDialog) getDialog())
                .watchValue(mInputTitle, mInputTitle.getText().toString())
                .watchValue(mButtonDate, mButtonDate.getText().toString())
                .watchValue(mButtonTime, mButtonTime.getText().toString())
                .watchValue(mInputLocation, mInputLocation.getText().toString())
                .watchValue(mInputDescription, mInputDescription.getText().toString());

        mSwitchDate.setChecked(meetingDate != null);
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        mCalendar.set(Calendar.YEAR, year);
        mCalendar.set(Calendar.MONTH, month);
        mCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        mButtonDate.setText(mDateDisplayFormat.format(mCalendar.getTime()));
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        mCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        mCalendar.set(Calendar.MINUTE, minute);
        mButtonTime.setText(mTimeDisplayFormat.format(mCalendar.getTime()));
    }

    private void hideSoftKeyboard() {
        InputMethodManager imm
                = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mInputTitle.getWindowToken(), 0);
        mInputLayoutTitle.requestFocus();
    }

    public interface Listener {
        void onEdited(Meeting meeting);
    }
}
