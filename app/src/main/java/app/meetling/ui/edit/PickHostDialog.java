package app.meetling.ui.edit;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import app.meetling.R;
import app.meetling.io.Host;

import static app.meetling.io.Host.EXTRA_HOST;

/**
 * Created by markus on 14.01.18.
 */

public class PickHostDialog extends AppCompatDialogFragment {
    private Listener mListener;
    private List<Host> mHosts;

    public static PickHostDialog newInstance(ArrayList<Host> hosts) {
        PickHostDialog fragment =  new PickHostDialog();
        Bundle args = new Bundle();
        args.putParcelableArrayList(EXTRA_HOST, hosts);
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
            mHosts = args.getParcelable(EXTRA_HOST);
        } else {
            throw new IllegalArgumentException("Args may not be null");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.dialog_pick_host)
                .setPositiveButton(R.string.action_save, null)
                .setNegativeButton(R.string.action_cancel, null)
                .setSingleChoiceItems((CharSequence[]) mHosts.stream().map(Host::getUrl).collect
                        (Collectors.toList()).toArray(), 0,
                        (dialogInterface, i) -> mListener.onPickedHost(mHosts.get(i)));

        return builder.create();
    }


    public interface Listener {
        void onPickedHost(Host host);
    }
}
