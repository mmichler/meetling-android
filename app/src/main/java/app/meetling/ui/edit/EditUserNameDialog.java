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

import app.meetling.R;
import app.meetling.io.Host;
import app.meetling.io.Then;
import app.meetling.io.User;
import app.meetling.io.WebApi;

import static app.meetling.io.Host.EXTRA_HOST;
import static app.meetling.io.User.EXTRA_USER;

public class EditUserNameDialog extends AppCompatDialogFragment {
    private User mUser;
    private WebApi mApi;
    private Listener mListener;

    public static EditUserNameDialog newInstance(User user, Host host) {
        EditUserNameDialog fragment = new EditUserNameDialog();
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
        View dialogLayout = inflater.inflate(R.layout.dialog_edit_user_name, null);
        TextInputLayout inputLayoutName
                = (TextInputLayout) dialogLayout.findViewById(R.id.input_layout_name);
        TextInputEditText inputName =
                (TextInputEditText) dialogLayout.findViewById(R.id.input_name);
        inputName.setText(mUser.getName());

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(dialogLayout)
                .setTitle(R.string.dialog_edit_user_name)
                .setPositiveButton(R.string.action_save, null)
                .setNegativeButton(R.string.action_cancel, null);

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(di -> {
            Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            DialogUtil.monitorForEmpty(inputName, inputLayoutName, buttonPositive);
            buttonPositive.setOnClickListener(v -> {
                Then.Callback<User> returnUser = new Then.Callback<User>() {
                    @Override
                    public void call(User user) {
                        mListener.onEditedUserName(user);
                        dismiss();
                    }
                };
                mUser.setName(inputName.getText().toString());
                mApi.edit(mUser).then(returnUser);
            });
        });

        return dialog;
    }

    public interface Listener {
        void onEditedUserName(User user);
    }
}
