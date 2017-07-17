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
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import app.meetling.R;
import app.meetling.io.AgendaItem;
import app.meetling.io.LocalStorage;
import app.meetling.io.Meeting;
import app.meetling.io.Then;
import app.meetling.io.Then.Callback;
import app.meetling.io.User;
import app.meetling.io.WebApi;
import app.meetling.ui.edit.EditMeetingDialog;
import app.meetling.ui.edit.EditUserDialog;
import app.meetling.ui.meeting.MeetingFragment;

import static app.meetling.io.User.EXTRA_USER;

/**
 * Hosts the main navigation and all (meeting-) data displaying Fragments.
 */
public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, MeetingFragment.Callback,
        GreeterFragment.Callback {
    private static final String HISTORY = "history";
    private WebApi mApi;
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

        mApi = new WebApi("https://meetling.org");
        mStorage = new LocalStorage(this);
        mHistory = new ArrayList<>();

        if (!isNetworkAvailable()) {
            Toast.makeText(this, "Network is unavailable", Toast.LENGTH_LONG).show();
            return;
        }

        if (savedInstanceState != null) {
            mUser = savedInstanceState.getParcelable(EXTRA_USER);
            mHistory = savedInstanceState.getStringArrayList(HISTORY);
            populateDrawerMenu();
        } else {

            final Callback<User> showUserAndHandleIntent = new Callback<User>() {
                @Override
                public void call(User user) {
                    mUser = user;
                    populateDrawerMenu();
                    showUser();
                    handleIntent();
                }
            };

            final Callback<User> storeUser = new Callback<User>() {
                @Override
                public void call(User user) {
                    mStorage.setCredentials(user.getId(), user.getAuthSecret());
                    showUserAndHandleIntent.call(user);
                }
            };

            Callback<Pair<String, String>> fetchUser = new Callback<Pair<String, String>>() {
                @Override
                public void call(Pair<String, String> credentials) {
                    if (credentials == null) {
                        mApi.login(null).then(storeUser);
                    } else {
                        mApi.getUser(
                                credentials.first, credentials.second)
                                        .then(showUserAndHandleIntent);
                    }
                }
            };

            mProgressBar.show();
            mStorage.getCredentials().then(fetchUser);
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
                case R.id.nav_edit_user :
                    // FIXME proof of concept, better: use onDrawerClosed() callback of DrawerLayout.DrawerListener
                    new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        editUser();
                    }
                }, 230);

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
            mApi.getMeeting(id, mUser).then(addMeetingToHistoryMenu);
        }
    }

    public void showMeeting(Meeting meeting) {
        showFragment(
                MeetingFragment.newInstance(
                        meeting, mUser, mApi.getHost()), R.string.title_meeting);
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
        EditMeetingDialog editMeetingDialog =
                EditMeetingDialog.newInstance(mUser, mApi.getHost());
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

    private void showUser() {
        // user
        ((TextView) findViewById(R.id.user_name)).setText(mUser.getName());
        mNavigationView.getMenu().findItem(R.id.nav_edit_user).setEnabled(true);
    }

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

        mApi.createExampleMeeting(mUser).then(getAgenda);
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

        mApi.getMeeting(id, mUser).then(setMeeting);
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

        mApi.getAgendaItems(meetingId, mUser).then(setItems);
        mApi.getTrashedAgendaItems(meetingId, mUser).then(setTrash);
    }

    private void showMeeting(Meeting meeting, List<AgendaItem> items, List<AgendaItem> trash) {
        MeetingFragment meetingFragment
                = MeetingFragment.newInstance(meeting, items, trash, mUser, mApi.getHost());
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

    private void editUser() {
        Fragment currentFragment = getSupportFragmentManager()
                .findFragmentById(R.id.fragment_placeholder_main);
        EditUserDialog editUserDialog = EditUserDialog.newInstance(mUser, mApi.getHost());
        editUserDialog.setTargetFragment(currentFragment, 300);
        FragmentManager fragmentManager = getSupportFragmentManager();
        editUserDialog.show(fragmentManager, "dialog_edit_user");
    }

    private void clearHistory() {
        mHistory.clear();
        mNavigationView.getMenu().findItem(R.id.nav_clear_history).setVisible(false);

        showHistory();
    }
}
