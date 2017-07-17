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

import java.util.HashMap;
import java.util.Map;

import app.meetling.R;
import app.meetling.io.Then;
import app.meetling.io.User;
import app.meetling.io.WebApi;

import static app.meetling.io.User.EXTRA_USER;
import static app.meetling.io.WebApi.EXTRA_API_HOST;

public class EditUserDialog extends AppCompatDialogFragment {
    private User mUser;
    private WebApi mApi;
    private Listener mListener;
    private TextInputEditText mInputName;

    public static EditUserDialog newInstance(User user, String host) {
        EditUserDialog fragment = new EditUserDialog();
        Bundle args = new Bundle();
        args.putParcelable(EXTRA_USER, user);
        args.putString(EXTRA_API_HOST, host);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            mListener = (Listener) getTargetFragment();
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
            mUser = args.getParcelable(EXTRA_USER);
            mApi = new WebApi(args.getString(EXTRA_API_HOST));
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

        return inflater.inflate(R.layout.fragment_edit_user, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            getDialog().dismiss();
        });

        Button saveButton = (Button) view.findViewById(R.id.saveUser);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = mInputName.getText().toString();
                if (name.isEmpty()) {
                    Snackbar.make(
                            view.findViewById(R.id.edit_user_layout),
                            R.string.toast_name_empty, Snackbar.LENGTH_LONG).show();
                } else {
                    // TODO show progressbar
                    Then.Callback<User> returnUser = new Then.Callback<User>() {
                        @Override
                        public void call(User user) {
                            mListener.onEdited(user);
                            ((DialogUtil.PromptOnChangesDialog) getDialog())
                                    .ignoreChanges().dismiss();
                        }
                    };

                    mUser.setName(name);
                    mApi.edit(mUser).then(returnUser);
                }
            }
        });

        final TextInputLayout inputLayoutName
                = (TextInputLayout) view.findViewById(R.id.input_layout_name);
        mInputName = (TextInputEditText) view.findViewById(R.id.input_name);
        mInputName.setText(mUser.getName());
        DialogUtil.monitorForEmpty(mInputName, inputLayoutName);
        ((DialogUtil.PromptOnChangesDialog) getDialog())
                .watchValue(mInputName, mInputName.getText().toString());
    }

    public interface Listener {
        void onEdited(User user);
    }
}
