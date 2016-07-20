package app.meetling.ui.meeting;

import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import app.meetling.R;
import app.meetling.io.AgendaItem;
import app.meetling.io.Meeting;
import app.meetling.io.User;
import app.meetling.io.WebApi;
import app.meetling.ui.NavigationFragment;

import static app.meetling.io.Meeting.EXTRA_MEETING;
import static app.meetling.io.User.EXTRA_USER;

/**
 * Fragment that displays the a meeting's data.
 */
public class MeetingFragment extends NavigationFragment<MeetingFragment.Callback> {
    private static final String ITEMS = "items";
    private static final String TRASHED_ITEMS = "trashed_items";
    private User mUser;
    private Meeting mMeeting;
    private Adapter.AgendaItemAdapter mItemAdapter;
    private Adapter.TrashedItemAdapter mTrashedItemAdapter;
    private RecyclerView mRecyclerView;

    public static MeetingFragment newInstance(Meeting meeting, User user) {
        return newInstance(meeting, new ArrayList<AgendaItem>(), new ArrayList<AgendaItem>(), user);
    }

    public static MeetingFragment newInstance(
            Meeting meeting, List<AgendaItem> items, List<AgendaItem> trashedItems, User user) {
        MeetingFragment fragment = new MeetingFragment();
        Bundle args = new Bundle();
        args.putParcelable(EXTRA_MEETING, meeting);
        args.putParcelableArrayList(ITEMS, new ArrayList<Parcelable>(items));
        args.putParcelableArrayList(TRASHED_ITEMS, new ArrayList<Parcelable>(trashedItems));
        args.putParcelable(EXTRA_USER, user);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        Bundle args = getArguments();
        if (args != null) {
            mMeeting = args.getParcelable(EXTRA_MEETING);
            mItemAdapter
                    = new Adapter.AgendaItemAdapter(args.<AgendaItem>getParcelableArrayList(ITEMS), this);
            mTrashedItemAdapter
                    = new Adapter.TrashedItemAdapter(
                            args.<AgendaItem>getParcelableArrayList(TRASHED_ITEMS), this);
            mUser = args.getParcelable(EXTRA_USER);
        } else {
            throw new IllegalArgumentException("Args may not be null");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        return inflater.inflate(R.layout.fragment_meeting, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.meeting);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_share :
                        ClipboardManager clipboard
                                = (ClipboardManager) getActivity()
                                .getSystemService(Service.CLIPBOARD_SERVICE);
                        String url = "https://meetling.org/meetings/" + mMeeting.getId();
                        ClipData clip = ClipData.newPlainText("Meeting URL", url);
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(
                                getContext(), "Meeting URL saved to clipboard",
                                Toast.LENGTH_LONG).show();
                        return true;
                    case R.id.action_refresh :
                        mCallback.onRefresh(mMeeting);
                        return true;
                }

                return false;
            }
        });

        displayMeetingHeader();

        mRecyclerView = (RecyclerView) getActivity().findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(MeetingFragment.this.getContext()));
        mRecyclerView.addItemDecoration(new DividerItemDecoration());
        // Remove flickering animation when calling RecyclerView.notifyItemChanged()
        ((SimpleItemAnimator) mRecyclerView.getItemAnimator()).setSupportsChangeAnimations(false);

        TabLayout tabLayout = (TabLayout) getActivity().findViewById(R.id.tab_layout);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    showItems();
                } else {
                    showTrash();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) { }

            @Override
            public void onTabReselected(TabLayout.Tab tab) { }
        });

        showItems();
    }

    private void displayMeetingHeader() {
        ((TextView) getActivity().findViewById(R.id.meeting_title)).setText(mMeeting.getTitle());

        Date date = mMeeting.getDate();
        DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(getContext());
        DateFormat longDateFormat = android.text.format.DateFormat.getLongDateFormat(getContext());

        if (date != null) {
            ((TextView) getActivity().findViewById(R.id.meeting_date))
                    .setText(String.format("%s, %s", longDateFormat.format(date),
                            timeFormat.format(date)));
        }
        ((TextView) getActivity().findViewById(R.id.meeting_location))
                .setText(mMeeting.getLocation());
        ((TextView) getActivity().findViewById(R.id.meeting_description))
                .setText(mMeeting.getDescription());
        String lastAuthor = mMeeting.getAuthors().get(mMeeting.getAuthors().size() - 1).getName();
        ((TextView) getActivity().findViewById(R.id.meeting_last_author)).setText(lastAuthor);
    }

    private void showItems() {
        mItemTouchHelper.attachToRecyclerView(mRecyclerView);
        mRecyclerView.setAdapter(mItemAdapter);
    }

    private void showTrash() {
        mItemTouchHelper.attachToRecyclerView(null);
        mRecyclerView.setAdapter(mTrashedItemAdapter);
    }

    public interface Callback extends NavigationFragment.Callback {
        void onRefresh(Meeting meeting);
    }
}
