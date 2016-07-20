package app.meetling.ui.edit;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import app.meetling.R;

/**
 * Fragment for editing the User's name.
 */
public class EditUserFragment extends EditFragment<EditUserFragment.Callback> {
    private static final String ARG_USER_NAME = "user_name";
    private TextInputEditText mInputName;
    private String mUserName;

    public static EditUserFragment newInstance(String userName) {
        EditUserFragment fragment = new EditUserFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USER_NAME, userName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        Bundle args = getArguments();
        if (args != null) {
            mUserName = args.getString(ARG_USER_NAME);
        } else {
            throw new IllegalArgumentException("Args may not be null");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        return inflater.inflate(R.layout.fragment_edit_user, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mInputName = (TextInputEditText) getActivity().findViewById(R.id.input_name);
        mInputName.setText(mUserName);
    }

    @Override
    protected View.OnClickListener createHomeClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = mInputName.getText().toString();
                if (name.isEmpty()) {
                    Snackbar.make(
                            getActivity().findViewById(R.id.edit_user_layout),
                            R.string.toast_name_empty, Snackbar.LENGTH_LONG).show();
                } else {
                    mCallback.onEdited(name);
                }
            }
        };
    }

    public interface Callback extends EditFragment.Callback {
        void onEdited(String userName);
    }
}
