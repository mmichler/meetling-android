package app.meetling.ui.edit;

import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.TextWatcher;

import app.meetling.R;

/**
 * Used for monitoring form fields for emptiness (illegal state).
 */
class NonEmptyTextWatcher implements TextWatcher {
    private TextInputLayout mInputLayout;

    public NonEmptyTextWatcher(TextInputLayout inputLayout) {
        this.mInputLayout = inputLayout;
    }

    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
    }

    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
    }

    public void afterTextChanged(Editable editable) {
        if (editable.toString().trim().isEmpty()) {
            String error = mInputLayout.getContext().getString(R.string.error_input_mandatory);
            mInputLayout.setError(error);
        } else {
            mInputLayout.setError(null);
        }
    }
}
