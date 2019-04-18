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
package com.wiley.android.journalApp.error;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.widget.TextView;

import com.wiley.android.journalApp.R;
import com.wiley.android.journalApp.utils.UIUtils;
import com.wiley.wol.client.android.data.utils.AANHelper;
import com.wiley.wol.client.android.data.utils.AppErrorUtils;
import com.wiley.wol.client.android.error.AppErrorCode;
import com.wiley.wol.client.android.journalApp.theme.Theme;
import com.wiley.wol.client.android.log.Logger;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import static android.app.AlertDialog.Builder;
import static android.content.DialogInterface.OnClickListener;
import static com.wiley.android.journalApp.error.ErrorMessage.withMessage;
import static com.wiley.android.journalApp.error.ErrorMessage.withTitle;
import static com.wiley.android.journalApp.error.ErrorMessage.withTitleAndMessage;
import static com.wiley.android.journalApp.error.ErrorMessage.withTitleAndSpannableMessage;
import static com.wiley.wol.client.android.error.AppErrorCode.ACCESS_FORBIDDEN_ARTICLE;
import static com.wiley.wol.client.android.error.AppErrorCode.ACCESS_FORBIDDEN_ISSUE;
import static com.wiley.wol.client.android.error.AppErrorCode.XML_PARSE_ERROR;
import static java.lang.String.format;

public class ErrorManagerImpl implements ErrorManager {
    private static final String TAG = ErrorManagerImpl.class.getSimpleName();
    private final String journalName;
    private final Set<ErrorMessage> runningDialogs = new HashSet<>();

    @Inject
    private AANHelper aanHelper;

    @Inject
    public ErrorManagerImpl(final Theme mTheme) {
        this.journalName = mTheme.getJournalName();
    }

    @Override
    public void alertWithErrorMessage(final Activity activity, final ErrorMessage errorMessage, final ErrorButton... errorButtons) {
        AlertDialog alertDialog = createAlertDialog(activity, errorMessage, errorButtons);
        if (alertDialog != null) {
            alertDialog.show();
        }
    }

    @Override
    public void alertWithErrorCode(final Activity activity, final AppErrorCode errorCode, final ErrorButton... errorButtons) {
        alertWithErrorCode(activity, errorCode, null, errorButtons);
    }

    private void alertWithErrorCode(final Activity activity, final AppErrorCode errorCode, final Throwable throwable, final ErrorButton... errorButtons) {
        {
            if (ACCESS_FORBIDDEN_ARTICLE == errorCode) {
                aanHelper.trackLoggedInNotAccessToArticleOverlay();
            } else if (ACCESS_FORBIDDEN_ISSUE == errorCode) {
                aanHelper.trackLoggedInNotAccessToIssueOverlay();
            }
        }

        ErrorMessage errorMessage = getErrorMessageForErrorCode(activity, errorCode);
        if (throwable != null && throwable.getMessage() != null) {
            errorMessage = withTitleAndMessage(errorMessage.getTitle(),
                    (errorMessage.getMessage() != null ? errorMessage.getMessage() : "") + " " + throwable.getMessage());
        }

        alertWithErrorMessage(activity, errorMessage, errorButtons);
    }

    @Override
    public void alertWithException(final Activity activity, final Throwable throwable, final ErrorButton... errorButtons) {
        final AppErrorCode errorCode = AppErrorUtils.getAppErrorCode(throwable);
        switch (errorCode) {
            case ACCESS_FORBIDDEN:
            case XML_PARSE_ERROR:
                alertWithErrorCode(activity, XML_PARSE_ERROR, errorButtons);
                break;
            default:
                Logger.s(TAG, throwable);
                break;
        }
    }

    private AlertDialog createAlertDialog(final Activity activity, final ErrorMessage errorMessage, final ErrorButton... errorButtons) {
        if (runningDialogs.contains(errorMessage)) {
            return null;
        } else {
            runningDialogs.add(errorMessage);
        }

        final TextView headerView = new TextView(activity);
        headerView.setGravity(Gravity.CENTER_VERTICAL | Gravity.FILL_HORIZONTAL);
        headerView.setTextSize(UIUtils.pxToDp(activity, (int) activity.getResources().getDimension(R.dimen.alert_dialog_title_text_size)));
        headerView.setTextColor(activity.getResources().getColor(R.color.blue_light));
        headerView.setText(errorMessage.hasTitle() ? errorMessage.getTitle() : activity.getString(R.string.error));

        float padding = activity.getResources().getDimension(R.dimen.alert_dialog_title_padding);
        headerView.setPadding((int) padding, (int) padding, (int) padding, (int) padding);

        boolean cancelable = true;
        if (errorButtons != null && errorButtons.length > 0) {
            cancelable = false;
        }

        final Builder builder = new Builder(activity)
                .setMessage(null == errorMessage.getMessage() ? errorMessage.getSpannableMessage() : errorMessage.getMessage())
                .setCustomTitle(headerView)
                .setCancelable(cancelable);

        if (null == errorButtons || errorButtons.length == 0) {
            builder.setPositiveButton(android.R.string.ok, null);
        } else {
            if (errorButtons.length >= 1) {
                builder.setPositiveButton(errorButtons[0].getTitle(), createListenerFor(errorButtons[0]));
            }
            if (errorButtons.length >= 2) {
                builder.setNegativeButton(errorButtons[1].getTitle(), createListenerFor(errorButtons[1]));
            }
            if (errorButtons.length >= 3) {
                builder.setNeutralButton(errorButtons[2].getTitle(), createListenerFor(errorButtons[2]));
            }
        }
        final AlertDialog alertDialog = builder.create();
        alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                runningDialogs.remove(errorMessage);
            }
        });
        return alertDialog;
    }

    private OnClickListener createListenerFor(final ErrorButton errorButton) {
        return new OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, final int which) {
                if (errorButton.getOnClickListener() != null) {
                    errorButton.getOnClickListener().onClick();
                }
            }
        };
    }

    @Override
    public ErrorMessage getErrorMessageForErrorCode(final Activity activity, final AppErrorCode code) {
        switch (code) {
            case XML_PARSE_ERROR:
            case WRONG_DOI:
            case FAIL_TO_OPEN_ARCHIVE:
            case EMPTY_ARCHIVE:
            case SERVER_ERROR:
            case FILENAME_IS_NULL:
                return withTitle(activity.getString(R.string.error_content_error_title));
            case ACCESS_FORBIDDEN:
                return withTitle(activity.getString(R.string.error_access_forbidden_title));
            case ACCESS_FORBIDDEN_ISSUE:
                return withTitleAndMessage(
                        activity.getString(R.string.error_access_forbidden_issue_title),
                        format(activity.getString(R.string.error_access_forbidden_issue_message), journalName)
                );
            case ACCESS_FORBIDDEN_ARTICLE:
                return withTitleAndMessage(
                        activity.getString(R.string.error_access_forbidden_article_title),
                        format(activity.getString(R.string.error_access_forbidden_article_message_format), journalName)
                );
            case ACCESS_FORBIDDEN_APP:
                final String text = format(activity.getString(R.string.error_access_forbidden_app_message_format), journalName);
                int indexStart = text.indexOf(journalName);
                Spannable  spannableText = new SpannableString(text);
                spannableText.setSpan(new StyleSpan(Typeface.ITALIC), indexStart, indexStart + journalName.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                return withTitleAndSpannableMessage(
                        activity.getString(R.string.error_access_forbidden_app_title),
                        spannableText
                );
            case NO_CONNECTION_AVAILABLE:
                return withTitleAndMessage(
                        activity.getString(R.string.error_no_connection_available_title),
                        activity.getString(R.string.error_no_connection_available_message)
                );
            case NO_CONNECTION_AVAILABLE_FOR_FIRST_LAUNCH:
                return withTitleAndMessage(
                        activity.getString(R.string.error_no_connection_available_for_first_launch_title),
                        activity.getString(R.string.error_no_connection_available_for_first_launch_message)
                );
            case NO_CONNECTION_AVAILABLE_TO_SAVE_ISSUE:
                return withTitleAndMessage(
                        activity.getString(R.string.error_no_connection_available_to_save_issue_title),
                        activity.getString(R.string.error_no_connection_available_to_save_issue_message)
                );
            case NO_CONNECTION_AVAILABLE_TO_SAVE_ARTICLE:
                return withTitleAndMessage(
                        activity.getString(R.string.error_no_connection_available_to_save_article_title),
                        activity.getString(R.string.error_no_connection_available_to_save_article_message)
                );
            case SERVER_ERROR_MESSAGE:
                return withTitle(activity.getString(R.string.error_server_error_title));
            case FAIL_TO_GET_PRODUCT:
                return withTitleAndMessage(
                        activity.getString(R.string.error_fail_to_get_product_title),
                        activity.getString(R.string.error_fail_to_get_product_message)
                );
            case TRANSACTION_CANCELED:
                return withTitle(activity.getString(R.string.error_transaction_canceled_title));
            case NO_PREVIOUS_SUBSCRIPTION:
                return withTitleAndMessage(
                        activity.getString(R.string.error_no_previous_subscription_title),
                        activity.getString(R.string.error_no_previous_subscription_message)
                );
            case PURCHASE_TURNED_OFF:
                return withTitleAndMessage(
                        activity.getString(R.string.error_purchase_turned_off_title),
                        activity.getString(R.string.error_purchase_turned_off_message)
                );
            case AUTHORIZATION_FAILED:
                return withTitleAndMessage(
                        activity.getString(R.string.error_authorisation_failed_title),
                        activity.getString(R.string.error_authorisation_failed_message)
                );
            case CONTENT_LOCKED_FOR_WOL:
                return withTitleAndMessage(
                        activity.getString(R.string.error_content_locked_for_wol_title),
                        activity.getString(R.string.error_content_locked_for_wol_message)
                );
            case CONTENT_LOCKED_FOR_ANDROID:
                return withTitleAndMessage(
                        activity.getString(R.string.error_content_locked_for_android_title),
                        activity.getString(R.string.error_content_locked_for_android_message)
                );
            case FAIL_TO_AUTHORISE_DOCUMENT:
                return withMessage(activity.getString(R.string.error_fail_to_authorize_doc_title));
            case FAIL_TO_GET_DOCUMENT:
                return withMessage(activity.getString(R.string.error_fail_to_get_doc_title));
            case TPS_NO_RESPONSE:
                return withMessage(activity.getString(R.string.tps_no_response_message));
            case TPS_UNKNOWN_RESPONSE:
                return withMessage(activity.getString(R.string.tps_unknown_response));
            case TPS_WRONG_PASSWORD:
                return withTitleAndMessage(
                        activity.getString(R.string.we_were_unable_to_log_you_in),
                        activity.getString(R.string.tps_login_pass_incorrect_message));
            case TPS_OAUTH_FAILED:
                return withMessage(activity.getString(R.string.we_are_having_trouble_accessing_content_message));
            case TPS_NO_ACCESS:
                return withMessage(activity.getString(R.string.tps_no_access_message));
            case ISSUE_IS_NOT_AVAILABLE_OFFLINE:
                return withTitleAndMessage(
                        activity.getString(R.string.issue_is_not_available_offline_title),
                        activity.getString(R.string.issue_is_not_available_offline_message));
            case NO_FEED_AVAILABLE:
                return withTitleAndMessage(activity.getString(R.string.no_feed_available_title),
                        activity.getString(R.string.no_feed_available_message));

            case UNDEFINED:
            default:
                return withTitle(activity.getString(R.string.error_unknown_error_title));
        }
    }

}