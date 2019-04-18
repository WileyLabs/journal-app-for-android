/*  Journal App for Android
 *  Copyright (C) 2019 John Wiley & Sons, Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.wiley.android.journalApp.fragment.access;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.inject.Inject;
import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.error.ErrorManager;
import com.wiley.android.journalApp.exception.TextFieldInfoNotFoundException;
import com.wiley.android.journalApp.fragment.settings.AccessCodeChecker;
import com.wiley.android.journalApp.utils.UIUtils;
import com.wiley.wol.client.android.data.service.AuthorizationService;
import com.wiley.wol.client.android.error.AppErrorCode;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

public class ScreenRegisterFragment extends AbstractScreenFragment {
    public static final String MOBILE_AFFILIATION = "MobileAffiliation";
    private static final String FIRST_NAME_FIELD_VALUE = "first_name_field_value";
    private static final String LAST_NAME_FIELD_VALUE = "last_name_field_value";
    private static final String EMAIL_FIELD_VALUE = "email_field_value";
    private static final String PASSWORD_FIELD_VALUE = "password_field_value";
    private static final String ACCESS_CODE_FIELD_VALUE = "access_code_field_value";

    @Inject
    private AuthorizationService authorizationService;

    @Inject
    private ErrorManager errorManager;

    protected AccessCodeChecker accessCodeChecker;
    protected AccessCodeChecker.Listener accessCodeCheckerListener = new AccessCodeChecker.Listener() {
        @Override
        public void onStateChanged() {
            updateUi();
        }
    };

    private boolean sponsoredSubscription = true;

    private TextView accessCodeText;
    private ImageView accessCodeStatus;
    private ProgressBar accessCodeProgress;
    private Button buttonSubmit;

    private List<TextFieldInfo> textFieldInfos = new ArrayList<>();

    private TextWatcher accessCodeTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            accessCodeChecker.requestAccessCodeCheck(s.toString());
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };

    private enum TextField {
        FirstName,
        LastName,
        Email,
        Email2,
        Password,
        Password2
    }

    private static class TextFieldInfo {
        public final TextField field;
        public final EditText edit;
        public final ImageView status;

        public boolean edited = false;
        public boolean valid = false;

        public TextFieldInfo(TextField field, EditText edit, ImageView status) {
            this.field = field;
            this.edit = edit;
            this.status = status;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        accessCodeChecker = new AccessCodeChecker(getActivity());
        accessCodeChecker.addListener(accessCodeCheckerListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        accessCodeChecker.removeListener(accessCodeCheckerListener);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frag_access_screen_register, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final Bundle args = getArguments();
        if (args != null && args.containsKey(MOBILE_AFFILIATION) && args.getBoolean(MOBILE_AFFILIATION)) {
            sponsoredSubscription = false;
        }

        initUi();
        validateTextFields();
        updateUi();
    }

    private TextFieldInfo getTextFieldInfo(TextField field) {
        for (TextFieldInfo info : textFieldInfos) {
            if (info.field == field) {
                return info;
            }
        }
        throw new TextFieldInfoNotFoundException(format("Unable to find text field info for field '%s'", field));
    }

    private void addTextFieldInfo(TextField field, int editId, int statusId, String argKey) {
        TextFieldInfo prevInfo = null;
        try {
            prevInfo = getTextFieldInfo(field);
        } catch (TextFieldInfoNotFoundException ignored) {
        }

        if (prevInfo != null) {
            textFieldInfos.remove(prevInfo);
        }

        EditText edit = findView(editId);
        ImageView status = findView(statusId);
        final TextFieldInfo info = new TextFieldInfo(field, edit, status);

        final Bundle arguments = getArguments();
        if (arguments != null && arguments.containsKey(argKey)) {
            edit.setText(arguments.getString(argKey));
            info.edited = true;
        } else {
            edit.setText("");
        }
        edit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                info.edited = true;
                validateTextFields();
                updateUi();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        edit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    info.edited = true;
                } else {
                    updateUi();
                }
            }
        });
        if (prevInfo != null) {
            info.valid = prevInfo.valid;
            info.edited = prevInfo.edited;
        }
        textFieldInfos.add(info);
    }

    private void initUi() {
        if (!sponsoredSubscription) {
            findView(R.id.access_code_layout).setVisibility(View.GONE);
        } else {
            accessCodeText = findView(R.id.access_code_text);
            accessCodeStatus = findView(R.id.access_code_status);
            accessCodeProgress = findView(R.id.access_code_progress);
            accessCodeText.addTextChangedListener(accessCodeTextWatcher);

            final Bundle arguments = getArguments();
            if (arguments != null && arguments.containsKey(ACCESS_CODE_FIELD_VALUE)) {
                accessCodeText.setText(arguments.getString(ACCESS_CODE_FIELD_VALUE));
            }
        }

        final TextView termsAndConditionsLink = findView(R.id.term_and_conditions_link);
        termsAndConditionsLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWebController.openUrlInternal("http://onlinelibrary.wiley.com/termsAndConditions");
            }
        });

        addTextFieldInfo(TextField.FirstName, R.id.first_name_text, R.id.first_name_status, FIRST_NAME_FIELD_VALUE);
        addTextFieldInfo(TextField.LastName, R.id.last_name_text, R.id.last_name_status, LAST_NAME_FIELD_VALUE);
        addTextFieldInfo(TextField.Email, R.id.email_text, R.id.email_status, EMAIL_FIELD_VALUE);
        addTextFieldInfo(TextField.Email2, R.id.email2_text, R.id.email2_status, EMAIL_FIELD_VALUE);
        addTextFieldInfo(TextField.Password, R.id.password_text, R.id.password_status, PASSWORD_FIELD_VALUE);
        addTextFieldInfo(TextField.Password2, R.id.password2_text, R.id.password2_status, PASSWORD_FIELD_VALUE);

        buttonSubmit = findView(R.id.button_submit);
        buttonSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(getActivity())
                        .setTitle(getActivity().getString(R.string.wol_terms))
                        .setMessage(getActivity().getString(R.string.wol_terms_confirmation))
                        .setCancelable(true)
                        .setNegativeButton(R.string.alert_button_no, null)
                        .setPositiveButton(R.string.alert_button_yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                registerNewUser();
                            }
                        })
                        .show();
            }
        });
    }

    @Override
    public void updateUi() {
        boolean isAccessCodeValid = true;
        if (sponsoredSubscription) {
            isAccessCodeValid = accessCodeChecker.getState() == AccessCodeChecker.State.Valid;
            boolean isAccessCodeProgress = accessCodeChecker.getState() == AccessCodeChecker.State.Progress;
            boolean isAccessCodeNone = accessCodeChecker.getState() == AccessCodeChecker.State.None;
            accessCodeText.setBackgroundResource(isAccessCodeValid || isAccessCodeNone ? R.drawable.grey_edit_text_selector : R.drawable.red_edit_text_selector);
            accessCodeText.setEnabled(true);
            accessCodeProgress.setVisibility(isAccessCodeProgress ? View.VISIBLE : View.GONE);
            if (isAccessCodeNone) {
                accessCodeStatus.setVisibility(View.INVISIBLE);
            } else {
                accessCodeStatus.setVisibility(isAccessCodeProgress ? View.GONE : View.VISIBLE);
                accessCodeStatus.setImageResource(isAccessCodeValid ? R.drawable.status_checkmark : R.drawable.status_crossmark);
            }
        }

        boolean allTextFieldsIsValid = true;

        for (TextFieldInfo info : textFieldInfos) {
            if (!info.valid) {
                allTextFieldsIsValid = false;
            }

            if (!isAccessCodeValid) {
                info.edit.setEnabled(false);
                info.edit.setBackgroundResource(R.drawable.grey_edit_text_selector);
                info.status.setVisibility(View.INVISIBLE);
            } else {
                info.edit.setEnabled(true);

                if (info.edited) {
                    if (info.edit.hasFocus()) {
                        info.status.setVisibility(View.INVISIBLE);
                    } else {
                        info.status.setVisibility(View.VISIBLE);
                        info.status.setImageResource(info.valid ? R.drawable.status_checkmark : R.drawable.status_crossmark);
                    }

                    info.edit.setBackgroundResource(info.valid || info.edit.hasFocus() ? R.drawable.grey_edit_text_selector : R.drawable.red_edit_text_selector);
                } else {
                    info.status.setVisibility(View.INVISIBLE);
                    info.edit.setBackgroundResource(R.drawable.grey_edit_text_selector);
                }
            }
        }

        boolean canSubmit = isAccessCodeValid && allTextFieldsIsValid;
        if (buttonSubmit != null) {
            buttonSubmit.setEnabled(canSubmit);
        }
    }

    private void validateTextFields() {
        validateTextFieldNotEmpty(TextField.FirstName);
        validateTextFieldNotEmpty(TextField.LastName);
        validateTextFieldRegexp(TextField.Email, ".+@([A-Za-z0-9]+\\.)+[A-Za-z]{2}[A-Za-z]*");
        validateTextFieldNotEmptyAndEqual(TextField.Email2, TextField.Email);
        validateTextFieldNotEmpty(TextField.Password);
        validateTextFieldNotEmptyAndEqual(TextField.Password2, TextField.Password);
    }

    private void validateTextFieldNotEmpty(TextField field) {
        getTextFieldInfo(field).valid = isTextFieldNotEmpty(field);
    }

    private void validateTextFieldRegexp(TextField field, String regexp) {
        getTextFieldInfo(field).valid = isTextFieldMatchRegexp(field, regexp);
    }

    private void validateTextFieldNotEmptyAndEqual(TextField field, TextField equalTo) {
        getTextFieldInfo(field).valid = isTextFieldNotEmpty(field) && isTextFieldsEqual(field, equalTo);
    }

    private boolean isTextFieldNotEmpty(TextField field) {
        return !TextUtils.isEmpty(getTextFieldInfo(field).edit.getText());
    }

    private boolean isTextFieldMatchRegexp(TextField field, String regexp) {
        String text = getTextFieldInfo(field).edit.getText().toString();
        return text.matches(regexp);
    }

    private boolean isTextFieldsEqual(TextField field1, TextField field2) {
        String value1 = getTextFieldInfo(field1).edit.getText().toString();
        String value2 = getTextFieldInfo(field2).edit.getText().toString();
        return TextUtils.equals(value1, value2);
    }

    private void registerNewUser() {
        boolean isAccessCodeValid = (!sponsoredSubscription) || (accessCodeChecker.getState() == AccessCodeChecker.State.Valid);
        validateTextFields();
        boolean allTextFieldsIsValid = true;
        for (TextFieldInfo info : textFieldInfos) {
            if (!info.valid) {
                allTextFieldsIsValid = false;
                break;
            }
        }

        boolean canSubmit = isAccessCodeValid && allTextFieldsIsValid;
        if (!canSubmit) {
            return;
        }

        final AuthorizationService.NewUserData newUserData = new AuthorizationService.NewUserData();
        newUserData.firstName = getTextFieldInfo(TextField.FirstName).edit.getText().toString();
        newUserData.lastName = getTextFieldInfo(TextField.LastName).edit.getText().toString();
        newUserData.email = getTextFieldInfo(TextField.Email).edit.getText().toString();
        newUserData.password = getTextFieldInfo(TextField.Password).edit.getText().toString();
        if (sponsoredSubscription) {
            newUserData.accessCode = accessCodeText.getText().toString();
        }

        AsyncTask<Void, Void, AuthorizationService.RegisterNewUserResult> task = new AsyncTask<Void, Void, AuthorizationService.RegisterNewUserResult>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                getAccessDialogFragment().showProgress();
            }

            @Override
            protected AuthorizationService.RegisterNewUserResult doInBackground(Void... params) {
                return authorizationService.registerNewUser(newUserData);
            }

            @Override
            protected void onPostExecute(AuthorizationService.RegisterNewUserResult registerNewUserResult) {
                super.onPostExecute(registerNewUserResult);
                if (getActivity() == null || getActivity().isFinishing()) {
                    return;
                }

                getAccessDialogFragment().hideProgress();

                switch (registerNewUserResult) {
                    case Success:
                        onRegisterUserSuccess();
                        break;
                    case NetworkError:
                        onRegisterUserNetworkError();
                        break;
                    case EmailError:
                        onRegisterUserEmailError();
                        break;
                    default:
                        break;
                }
            }
        };
        task.execute();
    }

    private void onRegisterUserSuccess() {
        getAccessDialogFragment().openRegisterConfirmScreen();
    }

    private void onRegisterUserNetworkError() {
        errorManager.alertWithErrorCode(getActivity(), AppErrorCode.SERVER_ERROR);
    }

    private void onRegisterUserEmailError() {
        final Bundle args = new Bundle();
        if (!sponsoredSubscription) {
            args.putBoolean(ScreenRegisterFragment.MOBILE_AFFILIATION, true);
        } else {
            args.putString(ACCESS_CODE_FIELD_VALUE, accessCodeText.getText().toString());
        }
        args.putString(FIRST_NAME_FIELD_VALUE, getTextFieldInfo(TextField.FirstName).edit.getText().toString());
        args.putString(LAST_NAME_FIELD_VALUE, getTextFieldInfo(TextField.LastName).edit.getText().toString());
        args.putString(EMAIL_FIELD_VALUE, getTextFieldInfo(TextField.Email).edit.getText().toString());
        args.putString(PASSWORD_FIELD_VALUE, getTextFieldInfo(TextField.Password).edit.getText().toString());

        getAccessDialogFragment().openRegisterErrorScreen(args);
    }

    @Override
    public void onStart() {
        super.onStart();
        getAccessDialogFragment().setDialogHeader(getActivity().getResources().getString(R.string.register_label));
    }

    @Override
    public void onStop() {
        super.onStop();
        UIUtils.hideSoftInput(getActivity());
        getAccessDialogFragment().setDialogHeader(getActivity().getResources().getString(R.string.get_access_label));
    }

    @Override
    public boolean canGoBack() {
        return true;
    }

    @Override
    protected String getGANHelperEvent() {
        return null;
    }

    @Override
    protected void openPreviousScreen() {
        if (sponsoredSubscription) {
            getAccessDialogFragment().openSubscriptionScreen();
        } else {
            getAccessDialogFragment().openScreenD();
        }
    }
}
