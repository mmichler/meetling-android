package app.meetling.ui.edit;


import android.app.Dialog;
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
import android.widget.Button;

import app.meetling.R;
import app.meetling.io.AgendaItem;
import app.meetling.io.Host;
import app.meetling.io.Meeting;
import app.meetling.io.Then;
import app.meetling.io.WebApi;

import static app.meetling.io.AgendaItem.EXTRA_AGENDA_ITEM;
import static app.meetling.io.Host.EXTRA_HOST;
import static app.meetling.io.Meeting.EXTRA_MEETING;

/**
 * Created by mmichler on 27.06.2017.
 */

public class EditItemDialog extends AppCompatDialogFragment {
    private WebApi mApi;
    private EditItemDialog.Listener mListener;
    private AgendaItem mItem;
    private Meeting mMeeting;
    private TextInputEditText mInputTitle;
    private TextInputEditText mInputDuration;
    private TextInputEditText mInputDescription;

    public static EditItemDialog newInstance(Meeting meeting, Host host) {
        return newInstance(null, meeting, host);
    }

    public static EditItemDialog newInstance(AgendaItem item, Meeting meeting, Host host) {
        EditItemDialog fragment = new EditItemDialog();
        Bundle args = new Bundle();
        args.putParcelable(EXTRA_AGENDA_ITEM, item);
        args.putParcelable(EXTRA_MEETING, meeting);
        args.putParcelable(EXTRA_HOST, host);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            mListener = (EditItemDialog.Listener) getTargetFragment();
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
            mItem = args.getParcelable(EXTRA_AGENDA_ITEM);
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

        return inflater.inflate(R.layout.fragment_edit_item, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDialog().dismiss();
            }
        });

        Button saveButton = (Button) view.findViewById(R.id.saveItem);
        saveButton.setOnClickListener(v -> {
            String title = mInputTitle.getText().toString();
            if (title.isEmpty()) {
                Snackbar.make(
                        view.findViewById(R.id.edit_item_layout),
                        R.string.toast_title_empty, Snackbar.LENGTH_LONG).show();
            } else {
                String durationInput = mInputDuration.getText().toString();
                Integer duration
                        = durationInput.isEmpty() ? null : Integer.parseInt(durationInput);
                String description = mInputDescription.getText().toString();

                Then.Callback<AgendaItem> returnItem = new Then.Callback<AgendaItem>() {
                    @Override
                    public void call(AgendaItem item) {
                        mListener.onEdited(item);
                        ((DialogUtil.PromptOnChangesDialog) getDialog())
                                .ignoreChanges().dismiss();
                    }
                };
                // TODO show progressbar
                if (mItem == null) {
                    mApi.createAgendaItem(title, duration, description, mMeeting).then(returnItem);

                    return;
                }
                mItem.setTitle(title);
                mItem.setDuration(duration);
                mItem.setDescription(description);

                mApi.edit(mItem, mMeeting).then(returnItem);
            }
        });

        final TextInputLayout inputLayoutTitle
                = (TextInputLayout) view.findViewById(R.id.input_layout_title);
        mInputTitle = (TextInputEditText) view.findViewById(R.id.input_title);
        DialogUtil.monitorForEmpty(mInputTitle, inputLayoutTitle);
        mInputDuration = (TextInputEditText) view.findViewById(R.id.input_duration);
        mInputDescription = (TextInputEditText) view.findViewById(R.id.input_description);
        if (mItem == null) {
            return;
        }
        mInputTitle.setText(mItem.getTitle());
        Integer duration = mItem.getDuration();
        mInputDuration.setText(String.valueOf(duration == null ? "" : duration));
        mInputDescription.setText(mItem.getDescription());
        ((DialogUtil.PromptOnChangesDialog) getDialog())
                .watchValue(mInputTitle, mInputTitle.getText().toString())
                .watchValue(mInputDuration, mInputDuration.getText().toString())
                .watchValue(mInputDescription, mInputDescription.getText().toString());
    }

    public interface Listener {
        void onEdited(AgendaItem item);
    }
}
