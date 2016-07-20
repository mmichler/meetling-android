package app.meetling.ui.meeting;

import android.graphics.Canvas;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;

import app.meetling.R;
import app.meetling.ui.meeting.MeetingFragment.Direction;

/**
 * Callback for touch events coming from the main RecyclerView's items.
 */
class ItemTouchCallback extends ItemTouchHelper.Callback {
    private boolean mInDragMode = false;
    private MeetingFragment mMeetingFragment;

    ItemTouchCallback(MeetingFragment meetingFragment) {
        mMeetingFragment = meetingFragment;
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return true;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return true;
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        int swipeFlags = ItemTouchHelper.END;
        return makeMovementFlags(dragFlags, swipeFlags);
    }

    @Override
    public boolean onMove(
            RecyclerView recyclerView, RecyclerView.ViewHolder source,
            RecyclerView.ViewHolder target) {
        int fromPosition = source.getAdapterPosition();
        int toPosition = target.getAdapterPosition();
        Direction direction;
        if (toPosition > fromPosition) {
            direction = Direction.UP;
        } else {
            direction = Direction.DOWN;
        }
        mMeetingFragment.moveAgendaItem(fromPosition, direction);

        return true;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        mMeetingFragment.trashAgendaItem(viewHolder.getAdapterPosition());
    }

    @Override
    public void clearView(
            RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        if (mInDragMode) {
            ViewCompat.setElevation(viewHolder.itemView, 0);
            int dropPos = viewHolder.getAdapterPosition();

            mMeetingFragment.writeAgendaItemMove(dropPos);

            mInDragMode = false;
        }
    }

    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
        super.onSelectedChanged(viewHolder, actionState);

        if (viewHolder == null) {
            return;
        }
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            getDefaultUIUtil()
                    .onSelected(
                            ((ViewHolder.AgendaItemHolder) viewHolder).swipeablePart);
        } else { // == ACTION_STATE_DRAG
            mInDragMode = true;
        }
    }

    @Override
    public void onChildDraw(
            Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
            float dX, float dY, int actionState, boolean isCurrentlyActive) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            getDefaultUIUtil()
                    .onDraw(
                            c, recyclerView,
                            ((ViewHolder.AgendaItemHolder) viewHolder).swipeablePart,
                            dX, dY, actionState, isCurrentlyActive);
        } else {
            super.onChildDraw(
                    c, recyclerView, viewHolder, dX, dY, actionState, false);
        }
    }

    @Override
    public void onChildDrawOver(
            Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
            float dX, float dY, int actionState, boolean isCurrentlyActive) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            getDefaultUIUtil()
                    .onDrawOver(
                            c, recyclerView,
                            ((ViewHolder.AgendaItemHolder) viewHolder).swipeablePart,
                            dX, dY, actionState, isCurrentlyActive);
        } else { // == ACTION_STATE_DRAG
            super.onChildDrawOver(
                    c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

            ViewCompat.setElevation(
                    viewHolder.itemView,
                    mMeetingFragment.getResources().getDimension(R.dimen.drag_elevation));
        }
    }
}
