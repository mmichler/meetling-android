package app.meetling.ui.edit;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import java.io.IOError;

import app.meetling.R;
import app.meetling.io.Host;
import app.meetling.io.LocalStorage;
import app.meetling.io.Then;
import app.meetling.io.User;
import app.meetling.io.WebApi;

import static app.meetling.io.Host.EXTRA_HOST;
import static app.meetling.io.User.EXTRA_USER;

public class SetUserEmailDialog extends AppCompatDialogFragment {
    private WebApi mApi;
    private Listener mListener;

    public static SetUserEmailDialog newInstance(User user, Host host) {
        SetUserEmailDialog fragment = new SetUserEmailDialog();
        Bundle args = new Bundle();
        args.putParcelable(EXTRA_USER, user);
        args.putParcelable(EXTRA_HOST, host);
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
            mApi = new WebApi(args.getParcelable(EXTRA_HOST));
        } else {
            throw new IllegalArgumentException("Args may not be null");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View dialogLayout = inflater.inflate(R.layout.dialog_set_user_email, null);
        TextInputLayout inputLayoutEmail
                = (TextInputLayout) dialogLayout.findViewById(R.id.input_layout_email);
        TextInputEditText inputEmail =
                (TextInputEditText) dialogLayout.findViewById(R.id.input_email);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(dialogLayout)
                .setTitle(R.string.dialog_edit_user_email)
                .setPositiveButton(R.string.action_save, null)
                .setNegativeButton(R.string.action_cancel, null);

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(di -> {
            Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            inputEmail.addTextChangedListener(new TextWatcher() {

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable editable) {
                    String error;
                    String address = editable.toString();
                    if (address.trim().isEmpty()) {
                        error = getContext().getString(R.string.error_input_mandatory);
                    } else if (!address.contains("@") && !address.isEmpty()) {
                        error = getContext().getString(R.string.error_needs_at_sign);
                    } else if (address.startsWith("@")) {
                        error = getContext().getString(R.string.error_needs_part_before_at_sign);
                    } else if (address.endsWith("@")) {
                        error = getContext().getString(R.string.error_needs_part_after_at_sign);
                    } else {
                        error = null;
                    }
                    inputLayoutEmail.setError(error);
                    buttonPositive.setEnabled(error == null);
                }

            });
            buttonPositive.setOnClickListener(v -> {
                Then.Callback<Boolean> dismiss = new Then.Callback<Boolean>() {
                    @Override
                    public void call(Boolean result) {
                        if (result) {
                            mListener.onSetUserEmail();
                            dismiss();
                        } else {
                            throw new IOError(
                                    new Exception(
                                            "Could not write AuthRequestId to local storage"));
                        }
                    }
                };
                Then.Callback<String> storeAuthId = new Then.Callback<String>() {
                    @Override
                    public void call(String authId) {
                        LocalStorage localStorage = new LocalStorage(getContext());
                        localStorage.setAuthRequestId(authId).then(dismiss);
                    }
                };
                mApi.setEmail(inputEmail.getText().toString()).then(storeAuthId);
            });
            buttonPositive.setEnabled(false);
        });

        return dialog;
    }

    public interface Listener {
        void onSetUserEmail();
    }

}
