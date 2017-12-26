package app.meetling.ui;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.StringRes;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.util.Pair;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import app.meetling.R;
import app.meetling.io.AgendaItem;
import app.meetling.io.Host;
import app.meetling.io.LocalStorage;
import app.meetling.io.Meeting;
import app.meetling.io.Then;
import app.meetling.io.Then.Callback;
import app.meetling.io.User;
import app.meetling.io.WebApi;
import app.meetling.ui.edit.EditMeetingDialog;
import app.meetling.ui.edit.EditUserNameDialog;
import app.meetling.ui.edit.SetUserEmailDialog;
import app.meetling.ui.edit.SubmitAuthCodeDialog;
import app.meetling.ui.meeting.MeetingFragment;

import static app.meetling.io.Host.EXTRA_HOST;
import static app.meetling.io.User.EXTRA_USER;

/**
 * Hosts the main navigation and all (meeting-) data displaying Fragments.
 */
public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, MeetingFragment.Callback,
        GreeterFragment.Callback {
    private static final String HISTORY = "history";
    private WebApi mApi;
    private Host mHost;
    private User mUser;
    private LocalStorage mStorage;
    private List<String> mHistory;
    private ContentLoadingProgressBar mProgressBar;
    private DrawerLayout mDrawerLayout;
    private NavigationView mNavigationView;

    // TODO rewrite without committing fragment transactions in callbacks
    // see http://www.androiddesignpatterns.com/2013/08/fragment-transaction-commit-state-loss.html
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Restart in own task if started in browser task
        Intent intent = getIntent();
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            if (intent.getType() == null) {
                if ((intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) == 0) {
                    intent.setFlags(intent.getFlags() | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }
            }
        }

        setContentView(R.layout.activity_main);

        View view = findViewById(R.id.progress_bar);
        if (view == null) {
            throw new IllegalStateException("View progress_bar not found");
        }
        mProgressBar = (ContentLoadingProgressBar) view;

        view = findViewById(R.id.drawer_layout);
        if (view == null) {
            throw new IllegalStateException("View drawer_layout not found");
        }
        mDrawerLayout = (DrawerLayout) view;

        view = findViewById(R.id.navigation_view);
        if (view == null) {
            throw new IllegalStateException("View navigation_view not found");
        }
        mNavigationView = (NavigationView) view;
        mNavigationView.setNavigationItemSelectedListener(this);

        mStorage = new LocalStorage(this);
        mHistory = new ArrayList<>();

        if (!isNetworkAvailable()) {
            Toast.makeText(this, "Network is unavailable", Toast.LENGTH_LONG).show();
            return;
        }

        Callback<User> init = new Callback<User>() {
            @Override
            public void call(User user) {
                mUser = user;
                populateDrawerMenu();
                showUser();
                handleIntent();
            }
        };

        Callback<List<Host>> newApi = new Callback<List<Host>>() {
            @Override
            public void call(List<Host> hosts) {
                mHost = hosts.get(0);
                mApi = new WebApi(mHost);
                mApi.getUser(mStorage).then(init);

            }
        };

        if (savedInstanceState != null) {
            mUser = savedInstanceState.getParcelable(EXTRA_USER);
            mHistory = savedInstanceState.getStringArrayList(HISTORY);
            mHost = savedInstanceState.getParcelable(EXTRA_HOST);
            mApi = new WebApi(mHost);
            populateDrawerMenu();
        } else {
            mProgressBar.show();

            mStorage.getHosts().then(newApi);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        mStorage.setHistory(mHistory);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(EXTRA_USER, mUser);
        outState.putParcelable(EXTRA_HOST, mHost);
        outState.putStringArrayList(HISTORY, (ArrayList<String>) mHistory);
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        // TODO make sure meeting ids are not the same as ids for other items
        if (id < mHistory.size()) {
            showMeeting(mHistory.get(id));
        } else {
            switch (id) {
                case R.id.nav_view :
                    if (mHistory.isEmpty()) {
                        showGreeter();
                    } else {
                        showMeeting(mHistory.get(0));
                    }
                    break;
                case R.id.nav_about :
                    showAbout();
                    break;
                case R.id.nav_new_meeting :
                    onCreateMeeting();
                    break;
                case R.id.nav_example :
                    showExampleMeeting();
                    break;
                case R.id.nav_change_user_name :
                    // TODO proof of concept, better: use onDrawerClosed() callback of DrawerLayout
                    // .DrawerListener
                    new Handler().postDelayed(() -> showDialog(
                            EditUserNameDialog.newInstance(
                                    mUser, mHost), "dialog_edit_user_name"), 230);
                    break;
                case R.id.nav_set_user_email :
                    showDialog(
                            SetUserEmailDialog.newInstance(
                                    mUser, mHost), "dialog_set_user_email");
                    break;
                case R.id.nav_delete_user_email :
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage(R.string.dialog_confirm_delete_email_message);
                    builder.setPositiveButton(
                            R.string.dialog_confirm_delete_email_affirmative, null);
                    builder.setNegativeButton(R.string.action_cancel, null);
                    AlertDialog dialog = builder.create();
                    dialog.setOnShowListener(di -> {
                        Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                        buttonPositive.setOnClickListener(v ->
                            mApi.deleteEmail(mUser).then(new Callback<User>() {
                                @Override
                                public void call(User user) {
                                    mUser = user;
                                    showUser();
                                    dialog.dismiss();
                                }
                        }));
                    });
                    dialog.show();
                    break;
                case R.id.nav_submit_auth_code :
                    showDialog(
                            SubmitAuthCodeDialog.newInstance(mUser, mHost), "dialog_submit_auth_code");
                    break;
                case R.id.nav_clear_history:
                    clearHistory();
                    break;
            }
        }

        mDrawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    public void addToHistory(String meetingId) {
        mHistory.remove(meetingId);
        mHistory.add(0, meetingId);
    }

    public void showHistory() {
        mNavigationView.getMenu().findItem(R.id.nav_history).getSubMenu().clear();
        if (mHistory.isEmpty()) {
            return;
        }

        mNavigationView.getMenu().findItem(R.id.nav_clear_history).setVisible(true);

        Then.Callback<Meeting> addMeetingToHistoryMenu = new Then.Callback<Meeting>() {
            @Override
            public void call(Meeting meeting) {
                int index = mHistory.indexOf(meeting.getId());
                MenuItem historyMenu = mNavigationView.getMenu().findItem(R.id.nav_history);
                if (historyMenu.getSubMenu().findItem(index) == null) {
                    historyMenu.getSubMenu().add(
                            R.id.nav_history, index, index, meeting.getTitle());
                }
            }
        };

        // TODO traffic intensive in long histories; cache meeting titles?
        for (String id : mHistory) {
            mApi.getMeeting(id).then(addMeetingToHistoryMenu);
        }
    }

    public void showMeeting(Meeting meeting) {
        showFragment(MeetingFragment.newInstance(meeting, mHost), R.string.title_meeting);
    }

    public void showUser() {
        ((TextView) findViewById(R.id.user_name)).setText(mUser.getName());
        Menu menu = mNavigationView.getMenu();
        menu.findItem(R.id.nav_change_user_name).setEnabled(true);
        menu.findItem(R.id.nav_set_user_email).setEnabled(true);

        String email = mUser.getEmail();
        if (email != null) {
            ((TextView) findViewById(R.id.user_email)).setText(email);
            menu.findItem(R.id.nav_set_user_email).setVisible(false);
            menu.findItem(R.id.nav_delete_user_email).setVisible(true);
        } else {
            ((TextView) findViewById(R.id.user_email)).setText("");
            menu.findItem(R.id.nav_set_user_email).setVisible(true);
            menu.findItem(R.id.nav_delete_user_email).setVisible(false);
        }

        Then.Callback<Boolean> enableAuthMenu = new Then.Callback<Boolean>() {
            @Override
            public void call(Boolean has) {
                mNavigationView.getMenu().findItem(R.id.nav_submit_auth_code).setEnabled(has);
            }
        };
        LocalStorage localStorage = new LocalStorage(this);
        localStorage.hasAuthRequestId().then(enableAuthMenu);
    }

    /*
    Getters/Setters
    ---------------
     */

    public User getUser() {
        return mUser;
    }

    public void setUser(User user) {
        mUser = user;
    }

    /*
    Fragment Callbacks
    ------------------
     */

    @Override
    public void onRefresh(Meeting meeting) {
        showMeeting(meeting.getId());
    }

    @Override
    public void onViewExample() {
        showExampleMeeting();
    }

    @Override
    public void onCreateMeeting() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        EditMeetingDialog editMeetingDialog = EditMeetingDialog.newInstance(mHost);
        editMeetingDialog.setTargetFragment(fragmentManager.findFragmentById(R.id
                .fragment_placeholder_main), 300);
        editMeetingDialog.show(fragmentManager, "dialog_edit_meeting");
    }

    @Override
    public void onSelectHome() {
        if (mDrawerLayout.isDrawerVisible(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else {
            mDrawerLayout.openDrawer(GravityCompat.START);
        }
    }

    /*
     Private Methods
     ---------------
     */

    private Boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }

    private void populateDrawerMenu() {
        // add stored history
        mStorage.getHistory().then(new Callback<List<String>>() {
            @Override
            public void call(List<String> meetingIds) {
                for (String id : meetingIds) {
                    if (!mHistory.contains(id)) {
                        mHistory.add(id);
                    }
                }
                showHistory();
            }
        });
    }

    // TODO replace with multi-host-capable version or remove
    private void handleIntent() {
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) & "text/plain".equals(type)) {
            String url = intent.getStringExtra(Intent.EXTRA_TEXT);
            String id = url.replace("meetling.org/api/meetings/", "");
            showMeeting(id);
            // TODO handle error when url is not valid
        } else if (Intent.ACTION_VIEW.equals(action)) {
            if (type == null) {
                String url = intent.getData().getPath();
                String id = url.replace("/meetings/", "");
                showMeeting(id);
            }
        } else {
            mProgressBar.hide();
            showGreeter();
        }
    }

    private void showGreeter() {
        showFragment(GreeterFragment.newInstance(), null);
    }

    private void showExampleMeeting() {
        mProgressBar.show();

        final Callback<Meeting> getAgenda = new Callback<Meeting>() {
            @Override
            public void call(final Meeting meeting) {
                final int[] loaded = {0};
                final Meeting[] tmpMeeting = {meeting};
                final List<AgendaItem> items = new ArrayList<>();
                final List<AgendaItem> trash = new ArrayList<>();

                getAgenda(meeting.getId(), tmpMeeting, items, trash, loaded, 2);
            }
        };

        mApi.createExampleMeeting().then(getAgenda);
    }

    private void showMeeting(final String id) {
        mProgressBar.show();

        final int[] loaded = {0};
        final int allFinished = 3;
        final Meeting[] tmpMeeting = new Meeting[1];
        final List<AgendaItem> items = new ArrayList<>();
        final List<AgendaItem> trash = new ArrayList<>();

        Callback<Meeting> setMeeting = new Callback<Meeting>() {
            @Override
            public void call(final Meeting meeting) {
                tmpMeeting[0] = meeting;
                loaded[0]++;
                if (loaded[0] < allFinished) {
                    return;
                }
                showMeeting(meeting, items, trash);
            }
        };

        mApi.getMeeting(id).then(setMeeting);
        getAgenda(id, tmpMeeting, items, trash, loaded, allFinished);
    }

    private void getAgenda(
            String meetingId, final Meeting[] mutableMeeting, final List<AgendaItem> items,
            final List<AgendaItem> trash, final int[] loaded, final int allFinished) {

        final Callback<List<AgendaItem>> setItems = new Callback<List<AgendaItem>>() {
            @Override
            public void call(List<AgendaItem> agendaItems) {
                items.addAll(agendaItems);
                loaded[0]++;
                if (loaded[0] < allFinished) {
                    return;
                }
                showMeeting(mutableMeeting[0], agendaItems, trash);
            }
        };

        final Callback<List<AgendaItem>> setTrash = new Callback<List<AgendaItem>>() {
            @Override
            public void call(List<AgendaItem> trashedItems) {
                trash.addAll(trashedItems);
                loaded[0]++;
                if (loaded[0] < allFinished) {
                    return;
                }
                showMeeting(mutableMeeting[0], items, trashedItems);
            }
        };

        mApi.getAgendaItems(meetingId).then(setItems);
        mApi.getTrashedAgendaItems(meetingId).then(setTrash);
    }

    private void showMeeting(Meeting meeting, List<AgendaItem> items, List<AgendaItem> trash) {
        MeetingFragment meetingFragment
                = MeetingFragment.newInstance(meeting, items, trash, mHost);
        showFragment(meetingFragment, R.string.title_meeting);
        addToHistory(meeting.getId());
        showHistory();

        mNavigationView.getMenu().findItem(R.id.nav_view).setChecked(true);
        mProgressBar.hide();
    }

    private void showFragment(MeetlingFragment fragment, @StringRes Integer backStackNameRes) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_placeholder_main, fragment);
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        if (backStackNameRes != null) {
            String name = getString(backStackNameRes);
            transaction.addToBackStack(name);
        }
        if (!isFinishing()) {
            transaction.commitAllowingStateLoss();
        }
    }

    private void showAbout() {
        if (getSupportFragmentManager().findFragmentById(
                R.id.fragment_placeholder_main) instanceof AboutFragment) {
            return;
        }
        showFragment(AboutFragment.newInstance(), R.string.title_about);
    }

    private void showDialog(AppCompatDialogFragment dialogFragment, String tag) {
        Fragment currentFragment = getSupportFragmentManager()
                .findFragmentById(R.id.fragment_placeholder_main);
        dialogFragment.setTargetFragment(currentFragment, 300);
        FragmentManager fragmentManager = getSupportFragmentManager();
        dialogFragment.show(fragmentManager, tag);
    }

    private void clearHistory() {
        mHistory.clear();
        mNavigationView.getMenu().findItem(R.id.nav_clear_history).setVisible(false);

        showHistory();
    }
}
