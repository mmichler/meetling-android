package app.meetling.ui.edit;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import app.meetling.R;
import app.meetling.io.AgendaItem;
import app.meetling.io.Meeting;

import static app.meetling.io.AgendaItem.EXTRA_AGENDA_ITEM;
import static app.meetling.io.Meeting.EXTRA_MEETING;

/**
 * Fragment for editing AgendaItems.
 */
public class EditItemFragment extends EditFragment<EditItemFragment.Callback> {
    private static final String NEW_ITEM = "new_item";
    private boolean mNewItem;
    private AgendaItem mItem;
    private Meeting mMeeting;
    private TextInputEditText mInputTitle;
    private TextInputEditText mInputDuration;
    private TextInputEditText mInputDescription;

    public static EditItemFragment newInstance(@NonNull Meeting meeting) {
        EditItemFragment fragment = new EditItemFragment();
        fragment.setArguments(setBundle(null, meeting));
        return fragment;
    }

    public static EditItemFragment newInstance(@NonNull AgendaItem item, @NonNull Meeting meeting) {
        EditItemFragment fragment = new EditItemFragment();
        fragment.setArguments(setBundle(item, meeting));
        return fragment;
    }

    private static Bundle setBundle(AgendaItem item, Meeting meeting) {
        Bundle args = new Bundle();
        if (item == null) {
            args.putBoolean(NEW_ITEM, true);
        } else {
            args.putParcelable(EXTRA_AGENDA_ITEM, item);
        }
        args.putParcelable(EXTRA_MEETING, meeting);
        return args;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        Bundle args = getArguments();
        if (args != null) {
            mItem = args.getParcelable(EXTRA_AGENDA_ITEM);
            mMeeting = args.getParcelable(EXTRA_MEETING);
            mNewItem = args.getBoolean(NEW_ITEM, false);
        } else {
            throw new IllegalArgumentException("Args may not be null");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        return inflater.inflate(R.layout.fragment_edit_item, container, false);
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
                if (!hasFocus) {
                    if (mInputTitle.getText().toString().trim().isEmpty()) {
                        inputLayoutTitle.setError(getString(R.string.error_input_mandatory));
                    } else {
                        inputLayoutTitle.setError(null);
                    }
                    inputLayoutTitle.setErrorEnabled(
                            mInputTitle.getText().toString().trim().isEmpty());
                } else {
                    // otherwise the hint text of mInputTitle won't be shown
                    getActivity().findViewById(R.id.nested_scrollview).scrollTo(0, 0);
                }
            }
        });
        mInputTitle.addTextChangedListener(new NonEmptyTextWatcher(inputLayoutTitle));

        mInputTitle = (TextInputEditText) getActivity().findViewById(R.id.input_title);
        mInputDuration = (TextInputEditText) getActivity().findViewById(R.id.input_duration);
        mInputDescription = (TextInputEditText) getActivity().findViewById(R.id.input_description);
        if (mNewItem) {
            return;
        }
        mInputTitle.setText(mItem.getTitle());
        Integer duration = mItem.getDuration();
        mInputDuration.setText(String.valueOf(duration == null ? "" : duration));
        mInputDescription.setText(mItem.getDescription());
    }

    @Override
    protected View.OnClickListener createHomeClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = mInputTitle.getText().toString();
                if (title.isEmpty()) {
                    Snackbar.make(
                            getActivity().findViewById(R.id.edit_item_layout),
                            R.string.toast_title_empty, Snackbar.LENGTH_LONG).show();
                } else {
                    String durationInput = mInputDuration.getText().toString();
                    Integer duration
                            = durationInput.isEmpty() ? null : Integer.parseInt(durationInput);
                    String description = mInputDescription.getText().toString();
                    if (mNewItem) {
                        mCallback.onCreateAgendaItem(title, duration, description, mMeeting);
                        return;
                    }
                    mItem.setTitle(title);
                    mItem.setDuration(duration);
                    mItem.setDescription(description);
                    mCallback.onEdited(mItem, mMeeting);
                }
            }
        };
    }

    public interface Callback extends EditFragment.Callback {
        void onCreateAgendaItem(String title, Integer duration, String description, Meeting meeting);
        void onEdited(AgendaItem item, Meeting meeting);
    }
}
