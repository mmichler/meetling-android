package app.meetling.ui.edit;

import android.annotation.SuppressLint;
import android.app.Dialog;
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
import app.meetling.io.LocalStorage;
import app.meetling.io.Then;

/**
 * Created by markus on 04.01.18.
 */

public class AddHostDialog extends AppCompatDialogFragment {

    public static AddHostDialog newInstance() {
        return new AddHostDialog();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        @SuppressLint("InflateParams")
        View dialogLayout = inflater.inflate(R.layout.dialog_add_host, null);
        TextInputLayout inputLayoutUrl = dialogLayout.findViewById(R.id.input_layout_url);
        TextInputEditText inputUrl = dialogLayout.findViewById(R.id.input_url);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(dialogLayout)
                .setTitle(R.string.dialog_add_host)
                .setPositiveButton(R.string.action_save, null)
                .setNegativeButton(R.string.action_cancel, null);

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(di -> {
            Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            buttonPositive.setEnabled(false);
            DialogUtil.monitorForEmpty(inputUrl, inputLayoutUrl, buttonPositive);

            Then.Callback<Host> returnHost =  new Then.Callback<Host>() {
                @Override
                public void call(Host host) {
                    dismiss();
                }
            };

            buttonPositive.setOnClickListener(v -> {
                new LocalStorage(getContext()).addHost(inputUrl.getText().toString())
                        .then(returnHost);

            });
        });

        return dialog;
    }
}
