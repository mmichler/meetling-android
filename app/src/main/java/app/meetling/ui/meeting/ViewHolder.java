package app.meetling.ui.meeting;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import app.meetling.R;

import static android.support.v7.widget.RecyclerView.NO_POSITION;

/**
 * Base class for holding an agenda item's data. Used in the RecyclerView.
 */
abstract class ViewHolder extends RecyclerView.ViewHolder {
    private Adapter mAdapter;
    TextView titleTextView;
    TextView durationTextView;
    TextView descriptionTextView;
    TextView authorTextView;

    ViewHolder(View itemView, Adapter adapter) {
        super(itemView);

        mAdapter = adapter;
        titleTextView = (TextView) itemView.findViewById(R.id.item_title);
        durationTextView = (TextView) itemView.findViewById(R.id.item_duration);
        descriptionTextView = (TextView) itemView.findViewById(R.id.item_description);
        authorTextView = (TextView) itemView.findViewById(R.id.item_last_author);

        itemView.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int clickedItemPos = getAdapterPosition();
                        if (clickedItemPos == mAdapter.getActiveItemPos()) {
                            mAdapter.setActiveItemPos(NO_POSITION);
                        } else {
                            mAdapter.setActiveItemPos(clickedItemPos);
                        }
                    }
                }
        );
    }

    /**
     * ViewHolder for an agenda item.
     */
    static class AgendaItemHolder extends ViewHolder {
        View swipeablePart;
        TextView numberTextView;

        AgendaItemHolder(View itemView, Adapter.AgendaItemAdapter adapter) {
            super(itemView, adapter);

            swipeablePart = itemView.findViewById(R.id.swipeable);
            numberTextView = (TextView) itemView.findViewById(R.id.item_number);
        }
    }

    /**
     * ViewHolder for a trashed agenda item.
     */
    static class TrashedItemHolder extends ViewHolder {

        TrashedItemHolder(View itemView, Adapter.TrashedItemAdapter adapter) {
            super(itemView, adapter);

            itemView.findViewById(R.id.item_number).getLayoutParams().width = 0;
        }
    }
}
