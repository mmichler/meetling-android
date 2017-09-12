package app.meetling.ui.edit;

import android.app.Dialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

import app.meetling.R;

class DialogUtil {

    static void monitorForEmpty(TextInputEditText editText, TextInputLayout layout) {
        editText.addTextChangedListener(new DialogUtil.EditTextWatcher(layout, null));
    }

    static void monitorForEmpty(TextInputEditText editText, TextInputLayout layout, View toDisable) {
        editText.addTextChangedListener(new DialogUtil.EditTextWatcher(layout, toDisable));
    }

    /**
     * Used for monitoring form fields for changes.
     */
    private static class EditTextWatcher implements TextWatcher {
        private TextInputLayout mInputLayout;
        private View mToDisable;

        EditTextWatcher(TextInputLayout inputLayout, View toDisable) {
            mInputLayout = inputLayout;
            mToDisable = toDisable;
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void afterTextChanged(android.text.Editable editable) {
            String error = mInputLayout.getContext().getString(R.string.error_input_mandatory);
            if (editable.toString().trim().isEmpty()) {
                mInputLayout.setError(error);
                if (mToDisable != null) {
                    mToDisable.setEnabled(false);
                }
            } else {
                if (mToDisable != null) {
                    mToDisable.setEnabled(true);
                }
                mInputLayout.setError(null);
            }
        }

    }

    static class PromptOnChangesDialog extends Dialog {
        private Map<TextView, String> mInitialValues;

        public PromptOnChangesDialog(@NonNull Context context) {
            super(context, R.style.MeetlingDialog);

            mInitialValues = new HashMap<>();
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        public PromptOnChangesDialog watchValue(TextView input, String initialVal) {
            mInitialValues.put(input, initialVal);
            return this;
        }

        public PromptOnChangesDialog ignoreChanges() {
            mInitialValues.clear();
            return this;
        }

        @Override
        public void dismiss() {
            boolean formDirty =
                    mInitialValues.entrySet().stream().anyMatch(
                            e -> !e.getKey().getText().toString().equals(e.getValue()));
            if (formDirty) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setMessage(R.string.dialog_discard_message);
                builder.setPositiveButton(
                        R.string.dialog_discard_affirmative,
                        (dialog, id) -> {
                            mInitialValues.clear();
                            super.dismiss();
                        });
                builder.setNegativeButton(
                        R.string.dialog_discard_negative, (dialog, id) -> dialog.dismiss());
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            } else {
                super.dismiss();
            }
        }
    }
}
