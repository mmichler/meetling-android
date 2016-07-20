package app.meetling.ui.meeting;

import android.content.Context;
import android.support.annotation.CallSuper;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;

import app.meetling.R;
import app.meetling.io.AgendaItem;

import static android.support.v7.widget.RecyclerView.NO_POSITION;

/**
 * Base class for populating the agenda and trash lists with items.
 */
abstract class Adapter<VH extends ViewHolder> extends RecyclerView.Adapter<VH> {
    protected MeetingFragment mMeetingFragment;
    private List<AgendaItem> mItems;
    private int mActiveItemPos = NO_POSITION;

    Adapter(List<AgendaItem> items, MeetingFragment meetingFragment) {
        mItems = items;
        mMeetingFragment = meetingFragment;
    }

    @Override
    public abstract VH onCreateViewHolder(ViewGroup parent, int viewType);

    @Override
    @CallSuper
    public void onBindViewHolder(VH holder, int position) {
        AgendaItem item = mItems.get(position);
        TextView titleText = holder.titleTextView;
        titleText.setText(item.getTitle());
        Integer duration = item.getDuration();
        if (duration == null || duration == 0) {
            holder.durationTextView.setVisibility(View.GONE);
        } else {
            holder.durationTextView.setVisibility(View.VISIBLE);
            holder.durationTextView.setText(String.format(Locale.getDefault(), "%dm", duration));
        }
        holder.descriptionTextView.setText(item.getDescription());
        int lastAuthorPos = item.getAuthors().size() - 1;
        holder.authorTextView.setText(item.getAuthors().get(lastAuthorPos).getName());
        holder.itemView.setSelected(mActiveItemPos == position);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public AgendaItem getItemAt(int pos) {
        return mItems.get(pos);
    }

    public int put(AgendaItem item) {
        int index;
        ListIterator<AgendaItem> itr = mItems.listIterator();
        while (itr.hasNext()) {
            AgendaItem agendaItem = itr.next();
            if (item.equals(agendaItem)) {
                itr.set(item);
                index = itr.previousIndex();
                notifyItemChanged(index);
                return index;
            }
        }

        mItems.add(item);
        index = mItems.size() - 1;
        notifyItemInserted(index);
        return index;
    }

    public void remove(AgendaItem item) {
        if (item.equals(getActiveItem())) {
            mActiveItemPos = NO_POSITION;
        }
        notifyItemRemoved(mItems.indexOf(item));
        mItems.remove(item);

        onItemChanged();
    }

    public void swap(int itemPos, int withPos) {
        Collections.swap(mItems, itemPos, withPos);
        notifyItemMoved(itemPos, withPos);
        if (mActiveItemPos == itemPos) {
            mActiveItemPos = withPos;
        }
    }

    public boolean hasActiveItem() {
        return mActiveItemPos != NO_POSITION;
    }

    public AgendaItem getActiveItem() {
        return mActiveItemPos == NO_POSITION ? null : mItems.get(mActiveItemPos);
    }

    public int getActiveItemPos() {
        return mActiveItemPos;
    }

    public void setActiveItemPos(int pos) {
        if (pos == mActiveItemPos) {
            return;
        }
        int oldActiveItemPos = mActiveItemPos;
        mActiveItemPos = pos;
        if (oldActiveItemPos != NO_POSITION) {
            notifyItemChanged(oldActiveItemPos);
        }
        if (pos != NO_POSITION) {
            notifyItemChanged(pos);
        }

        onItemChanged();
    }

    abstract void onItemChanged();

    /**
     * Adapter for an ordered list of agenda items.
     */
    static class AgendaItemAdapter extends Adapter<ViewHolder.AgendaItemHolder> {

        public AgendaItemAdapter(List<AgendaItem> items, MeetingFragment meetingFragment) {
            super(items, meetingFragment);
        }

        @Override
        public ViewHolder.AgendaItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            Context context = parent.getContext();
            LayoutInflater inflater = LayoutInflater.from(context);

            View itemView = inflater.inflate(R.layout.item_agenda, parent, false);

            return new ViewHolder.AgendaItemHolder(itemView, this);
        }

        @Override
        public void onBindViewHolder(ViewHolder.AgendaItemHolder holder, int position) {
            super.onBindViewHolder(holder, position);

            holder.numberTextView.setText(String.format(Locale.getDefault(), "%d.", position + 1));
        }

        @Override
        void onItemChanged() {
            mMeetingFragment.updateEditFabVisibility();
        }
    }

    /**
     * Adapter for trashed agenda items.
     */
    static class TrashedItemAdapter extends Adapter<ViewHolder.TrashedItemHolder> {

        public TrashedItemAdapter(List<AgendaItem> items, MeetingFragment meetingFragment) {
            super(items, meetingFragment);
        }

        @Override
        public ViewHolder.TrashedItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            Context context = parent.getContext();
            LayoutInflater inflater = LayoutInflater.from(context);

            View itemView = inflater.inflate(R.layout.item_agenda, parent, false);

            return new ViewHolder.TrashedItemHolder(itemView, this);
        }

        @Override
        void onItemChanged() {
            mMeetingFragment.updateRestoreFabVisibility();
        }
    }
}
