package app.meetling.ui.edit;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

import app.meetling.R;
import app.meetling.io.Meeting;

import static app.meetling.io.Meeting.EXTRA_MEETING;

/**
 * Fragment for editing Meetings.
 */
public class EditMeetingFragment extends EditFragment<EditMeetingFragment.Callback> {
    private boolean mDateSet;
    private boolean mTimeSet;
    private Meeting mMeeting;
    private Calendar mCalendar;
    private TextInputEditText mInputTitle;
    private TextInputEditText mInputDate;
    private TextInputEditText mInputTime;
    private TextInputEditText mInputLocation;
    private TextInputEditText mInputDescription;

    public static EditMeetingFragment newInstance() {
        return new EditMeetingFragment();
    }

    public static EditMeetingFragment newInstance(Meeting meeting) {
        EditMeetingFragment fragment = new EditMeetingFragment();
        Bundle args = new Bundle();
        args.putParcelable(EXTRA_MEETING, meeting);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        Bundle args = getArguments();
        if (args != null) {
            mMeeting = args.getParcelable(EXTRA_MEETING);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        return inflater.inflate(R.layout.fragment_edit_meeting, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final TextInputLayout inputLayoutTitle
                = (TextInputLayout) getActivity().findViewById(R.id.input_layout_title);
        mInputTitle = (TextInputEditText) getActivity().findViewById(R.id.input_title);
        mInputTitle.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus && EditMeetingFragment.this.isResumed()) {
                    if (mInputTitle.getText().toString().trim().isEmpty()) {
                        inputLayoutTitle.setError(getString(R.string.error_input_mandatory));
                    } else {
                        inputLayoutTitle.setError(null);
                    }
                } else {
                    // otherwise the hint text of mInputTitle won't be shown
                    getActivity().findViewById(R.id.nested_scrollview).scrollTo(0, 0);
                }

            }
        });
        mInputTitle.addTextChangedListener(new NonEmptyTextWatcher(inputLayoutTitle));
        mInputDate = (TextInputEditText) getActivity().findViewById(R.id.input_date);
        mInputTime = (TextInputEditText) getActivity().findViewById(R.id.input_time);
        mInputTime.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                hideSoftKeyboard();
                return false;
            }
        });
        mInputTime.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                hideSoftKeyboard();
                return false;
            }
        });

        ImageButton actionPickDate = (ImageButton) getActivity().findViewById(R.id.action_pick_date);
        actionPickDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCalendar == null) {
                    mCalendar = Calendar.getInstance();
                }
                int year = mCalendar.get(Calendar.YEAR);
                int month = mCalendar.get(Calendar.MONTH);
                int day = mCalendar.get(Calendar.DAY_OF_MONTH);

                mCallback.onPickDate(year, month, day);

                hideSoftKeyboard();
                mInputDate.requestFocus();
            }
        });

        ImageButton actionPickTime = (ImageButton) getActivity().findViewById(R.id.action_pick_time);
        actionPickTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCalendar == null) {
                    mCalendar = Calendar.getInstance();
                }
                int hour = mCalendar.get(Calendar.HOUR_OF_DAY);
                int minute = mCalendar.get(Calendar.MINUTE);

                mCallback.onPickTime(hour, minute);

                hideSoftKeyboard();
                mInputTime.requestFocus();
            }
        });

        ImageButton actionClearDate = (ImageButton) getActivity().findViewById(R.id.action_clear_date);
        actionClearDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDateSet = false;
                if (!mTimeSet) {
                    mCalendar = null;
                }
                updateDateTimeFields();
                checkForIncompleteDateTime();
            }
        });

        ImageButton actionClearTime = (ImageButton) getActivity().findViewById(R.id.action_clear_time);
        actionClearTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTimeSet = false;
                if (!mDateSet) {
                    mCalendar = null;
                }
                updateDateTimeFields();
                checkForIncompleteDateTime();
            }
        });

        mInputLocation = (TextInputEditText) getActivity().findViewById(R.id.input_location);
        mInputDescription = (TextInputEditText) getActivity().findViewById(R.id.input_description);

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
    }

    @Override
    protected View.OnClickListener createHomeClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mInputTitle.getText().toString().isEmpty()) {
                    Snackbar.make(
                            getActivity().findViewById(R.id.edit_meeting_layout),
                            R.string.toast_title_empty, Snackbar.LENGTH_LONG).show();
                } else if (mDateSet == mTimeSet) {
                    String title = mInputTitle.getText().toString();
                    Date date = mCalendar == null ? null : mCalendar.getTime();
                    String location = mInputLocation.getText().toString();
                    String description = mInputDescription.getText().toString();
                    if (mMeeting == null) {
                        mCallback.onCreateMeeting(title, date, location, description);
                    } else {
                        mMeeting.setTitle(title);
                        mMeeting.setDate(date);
                        mMeeting.setLocation(location);
                        mMeeting.setDescription(description);
                        mCallback.onEdited(mMeeting);
                    }
                }
            }
        };
    }

    public void setDate(int year, int month, int day) {
        mCalendar.set(Calendar.YEAR, year);
        mCalendar.set(Calendar.MONTH, month);
        mCalendar.set(Calendar.DAY_OF_MONTH, day);
        mDateSet = true;
        updateDateTimeFields();
        checkForIncompleteDateTime();
    }

    public void setTime(int hour, int minute) {
        mCalendar.set(Calendar.HOUR_OF_DAY, hour);
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
        TextInputLayout inputLayoutDate
                = (TextInputLayout) getActivity().findViewById(R.id.input_layout_date);
        TextInputLayout inputLayoutTime
                = (TextInputLayout) getActivity().findViewById(R.id.input_layout_time);
        if (mDateSet == mTimeSet) {
            inputLayoutDate.setError(null);
            inputLayoutTime.setError(null);
            return;
        }

        if (mTimeSet) {
            inputLayoutDate.setError(getString(R.string.error_date_mandatory));
        }

        if (mDateSet) {
            inputLayoutTime.setError(getString(R.string.error_time_mandatory));
        }
    }

    public interface Callback extends EditFragment.Callback {
        void onPickDate(int year, int month, int day);
        void onPickTime(int hour, int minute);

        void onCreateMeeting(String title, Date date, String location, String description);
        void onEdited(Meeting meeting);
    }
}
