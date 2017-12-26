package app.meetling.ui.edit;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import app.meetling.R;
import app.meetling.io.Host;
import app.meetling.io.LocalStorage;
import app.meetling.io.Then;
import app.meetling.io.User;
import app.meetling.io.WebApi;

import static app.meetling.io.Host.EXTRA_HOST;
import static app.meetling.io.User.EXTRA_USER;

public class SubmitAuthCodeDialog extends AppCompatDialogFragment {
    private static final String AUTH_CODE = "auth_code";
    private User mUser;
    private WebApi mApi;
    private Listener mListener;


    public static SubmitAuthCodeDialog newInstance(User user, Host host) {
        // TODO check if authCode has already been submitted, could be the case when device was rotated
        // while API calls were done
        SubmitAuthCodeDialog fragment = new SubmitAuthCodeDialog();
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
            mUser = args.getParcelable(EXTRA_USER);
            mApi = new WebApi(args.getParcelable(EXTRA_HOST));
        } else {
            throw new IllegalArgumentException("Args may not be null");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View dialogLayout = inflater.inflate(R.layout.dialog_submit_auth_code, null);
        TextInputLayout inputLayoutAuthCode
                = (TextInputLayout) dialogLayout.findViewById(R.id.input_layout_auth_code);
        TextInputEditText inputAuthCode =
                (TextInputEditText) dialogLayout.findViewById(R.id.input_auth_code);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(dialogLayout)
                .setTitle(R.string.dialog_submit_auth_code)
                .setPositiveButton(R.string.action_submit, null)
                .setNegativeButton(R.string.action_cancel, null);

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(di -> {
            Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            buttonPositive.setEnabled(false);
            DialogUtil.monitorForEmpty(inputAuthCode, inputLayoutAuthCode, buttonPositive);
            buttonPositive.setOnClickListener(v -> {
                LocalStorage localStorage = new LocalStorage(getContext());

                Then.Callback<Boolean> returnUser = new Then.Callback<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {
                        mListener.onSubmittedAuthCode(mUser);
                        dismiss();
                    }
                };

                Then.Callback<User> deleteAuthReq = new Then.Callback<User>() {
                    @Override
                    public void call(User user) {
                        if (getError() == null) {
                            mUser = user;
                            localStorage.deleteAuthRequestId().then(returnUser);
                            return;
                        }

                        String uiMessage;
                        switch (getError().second) {
                            case "auth_request_not_found" :
                                uiMessage = getString(R.string.toast_auth_request_not_found);
                                break;
                            case "auth_invalid" :
                                uiMessage = getString(R.string.toast_auth_invalid);
                                break;
                            case "email_duplicate" :
                                uiMessage = getString(R.string.toast_email_duplicate);
                                break;
                            default:
                                throw new RuntimeException("Unknown error while authorizing user: "
                                        + getError().second);
                        }
                        Toast.makeText(
                                getContext(), uiMessage,
                                Toast.LENGTH_LONG).show();
                        dismiss();
                    }
                };

                Then.Callback<String> authorize = new Then.Callback<String>() {
                    @Override
                    public void call(String reqId) {
                        mApi.finishSetEmail(reqId, inputAuthCode.getText().toString()).then(deleteAuthReq);

                    }
                };

                localStorage.getAuthRequestId().then(authorize);
            });
        });

        return dialog;
    }

    public interface Listener {
        void onSubmittedAuthCode(User user);
    }

}
