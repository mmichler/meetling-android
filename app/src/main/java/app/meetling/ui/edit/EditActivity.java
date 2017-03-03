package app.meetling.ui.edit;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NavUtils;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v7.app.AppCompatActivity;
import android.widget.DatePicker;
import android.widget.TimePicker;

import java.util.Date;

import app.meetling.R;
import app.meetling.io.AgendaItem;
import app.meetling.io.Meeting;
import app.meetling.io.Then;
import app.meetling.io.User;
import app.meetling.io.WebApi;
import app.meetling.ui.MainActivity;

import static app.meetling.io.AgendaItem.EXTRA_AGENDA_ITEM;
import static app.meetling.io.Meeting.EXTRA_MEETING;
import static app.meetling.io.User.EXTRA_USER;

/**
 * Hosts the editing Fragments.
 */
public class EditActivity extends AppCompatActivity implements EditMeetingFragment.Callback,
        EditItemFragment.Callback, TimePickerDialog.OnTimeSetListener, EditUserFragment.Callback,
        DatePickerDialog.OnDateSetListener {
    private User mUser;
    private WebApi mApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        if (savedInstanceState != null) {
            mUser = savedInstanceState.getParcelable(EXTRA_USER);
        } else {
            mUser = intent.getParcelableExtra(EXTRA_USER);
        }

        mApi = new WebApi("https://meetling.org");

        setContentView(R.layout.activity_edit);

        if (getSupportFragmentManager().findFragmentById(R.id.fragment_placeholder_edit) != null) {
            return;
        }

        Meeting meeting = intent.getParcelableExtra(EXTRA_MEETING);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        switch (intent.getAction()) {
            case MainActivity.ACTION_EDIT_USER :
                transaction.replace(
                        R.id.fragment_placeholder_edit,
                        EditUserFragment.newInstance(mUser.getName()));
                break;
            case MainActivity.ACTION_CREATE_MEETING :
                transaction.replace(
                        R.id.fragment_placeholder_edit, EditMeetingFragment.newInstance());
                break;
            case MainActivity.ACTION_EDIT_MEETING :
                transaction.replace(
                        R.id.fragment_placeholder_edit, EditMeetingFragment.newInstance(meeting));
                break;
            case MainActivity.ACTION_ADD_AGENDA_ITEM :
                transaction.replace(
                        R.id.fragment_placeholder_edit, EditItemFragment.newInstance(meeting));
                break;
            case MainActivity.ACTION_EDIT_AGENDA_ITEM :
                AgendaItem item = intent.getParcelableExtra(EXTRA_AGENDA_ITEM);
                transaction.replace(
                        R.id.fragment_placeholder_edit,
                        EditItemFragment.newInstance(item, meeting));
                break;

        }
        transaction.commit();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(EXTRA_USER, mUser);
    }

    @Override
    public void onPickDate(int year, int month, int day) {
        DialogFragment fragment = DatePickerFragment.newInstance(year, month, day);
        fragment.show(getSupportFragmentManager(), "datePicker");
    }

    @Override
    public void onPickTime(int hour, int minute) {
        DialogFragment fragment = TimePickerFragment.newInstance(hour, minute);
        fragment.show(getSupportFragmentManager(), "timePicker");
    }

    @Override
    public void onCreateMeeting(String title, Date date, String location, String description) {

        Then.Callback<Meeting> returnMeeting = new Then.Callback<Meeting>() {
            @Override
            public void call(Meeting meeting) {
                Intent intent = NavUtils.getParentActivityIntent(EditActivity.this);
                intent.setAction(Intent.ACTION_VIEW);
                intent.setType(Meeting.CONTENT_TYPE);
                intent.putExtra(EXTRA_MEETING, meeting);
                setResult(RESULT_OK, intent);
                finish();
            }
        };

        mApi.createMeeting(title, date, location, description, mUser).then(returnMeeting);

        final ContentLoadingProgressBar progressBar
                = (ContentLoadingProgressBar) findViewById(R.id.progress_bar);
        progressBar.show();
    }

    @Override
    public void onEdited(Meeting meeting) {
        mApi.edit(meeting, mUser);
        Intent intent = NavUtils.getParentActivityIntent(this);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setType(Meeting.CONTENT_TYPE);
        intent.putExtra(EXTRA_MEETING, meeting);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onCreateAgendaItem(
            String title, Integer duration, String description, Meeting meeting) {

        Then.Callback<AgendaItem> addItem = new Then.Callback<AgendaItem>() {
            @Override
            public void call(AgendaItem item) {
                Intent intent = NavUtils.getParentActivityIntent(EditActivity.this);
                intent.setAction(Intent.ACTION_VIEW);
                intent.setType(AgendaItem.CONTENT_TYPE);
                intent.putExtra(EXTRA_AGENDA_ITEM, item);
                setResult(RESULT_OK, intent);
                finish();
            }
        };

        mApi.createAgendaItem(title, duration, description, meeting, mUser).then(addItem);
        final ContentLoadingProgressBar progressBar
                = (ContentLoadingProgressBar) findViewById(R.id.progress_bar);
        progressBar.show();
    }

    @Override
    public void onEdited(AgendaItem item, Meeting meeting) {
        mApi.edit(item, meeting, mUser);
        Intent intent = NavUtils.getParentActivityIntent(this);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setType(AgendaItem.CONTENT_TYPE);
        intent.putExtra(EXTRA_AGENDA_ITEM, item);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onCancel() {
        finish();
    }

    @Override
    public void onEdited(String userName) {
        mUser.setName(userName);
        mApi.edit(mUser);
        Intent intent = NavUtils.getParentActivityIntent(this);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setType(User.CONTENT_TYPE);
        intent.putExtra(EXTRA_USER, mUser);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        EditMeetingFragment fragment
                = (EditMeetingFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.fragment_placeholder_edit);
        fragment.setTime(hourOfDay, minute);
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        EditMeetingFragment fragment
                = (EditMeetingFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.fragment_placeholder_edit);
        fragment.setDate(year, monthOfYear, dayOfMonth);
    }
}
