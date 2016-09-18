package com.nhancv.kurentoandroid;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Created by nhancao on 9/18/16.
 */
public class NDialog {
    private static final String TAG = NDialog.class.getSimpleName();

    /**
     * Show progress dialog
     *
     * @param uiContext
     * @param title
     * @return
     */
    public static ProgressDialog showProgressDialog(@NonNull Context uiContext, @Nullable CharSequence title) {
        ProgressDialog waitDialog = new ProgressDialog(uiContext);
        waitDialog.setTitle((title == null) ? "" : title);
        waitDialog.setCanceledOnTouchOutside(false);
        return waitDialog;
    }

    /**
     * Show Confirm dialog
     *
     * @param uiContext
     * @param title
     * @param message
     * @param callback
     * @return
     */
    public static AlertDialog showConfirmDialog(@NonNull Context uiContext,
                                                @Nullable CharSequence title,
                                                @Nullable CharSequence message,
                                                @Nullable ICollections.IDialogConfirmButtonImpl callback) {
        return showConfirmDialog(uiContext, title, message, -1, -1, callback);
    }

    /**
     * Show confirm dialog
     *
     * @param uiContext
     * @param title
     * @param message
     * @param callback
     * @return
     */
    public static AlertDialog showErrorConfirmDialog(@NonNull Context uiContext,
                                                     @Nullable CharSequence title,
                                                     @Nullable CharSequence message,
                                                     @Nullable ICollections.IDialogConfirmButtonImpl callback) {
        return showConfirmDialog(uiContext, title, message, R.string.dialog_tryagain, R.string.dialog_close, callback);
    }

    /**
     * Show confirm dialog
     *
     * @param uiContext
     * @param title
     * @param message
     * @param positiveBtn
     * @param negativeBtn
     * @param callback
     * @return
     */
    public static AlertDialog showConfirmDialog(@NonNull Context uiContext,
                                                @Nullable CharSequence title,
                                                @Nullable CharSequence message,
                                                @Nullable @StringRes Integer positiveBtn,
                                                @Nullable @StringRes Integer negativeBtn,
                                                @Nullable ICollections.IDialogConfirmButtonImpl callback) {
        return showConfirmDialog(uiContext, title, R.layout.dialog_alert, positiveBtn, negativeBtn, view -> {
            TextView tvMessage = (TextView) view.findViewById(R.id.tvMessage);
            if (message != null) {
                tvMessage.setVisibility(View.VISIBLE);
                tvMessage.setText(message);
            } else {
                tvMessage.setVisibility(View.GONE);
            }
        }, callback);
    }

    /**
     * Show Confirm dialog
     *
     * @param uiContext
     * @param title
     * @param message
     * @param positiveBtn
     * @param neutralBtn
     * @param negativeBtn
     * @param callback
     * @return
     */
    public static AlertDialog showConfirmDialog(@NonNull Context uiContext,
                                                @Nullable CharSequence title,
                                                @Nullable CharSequence message,
                                                @Nullable @StringRes Integer positiveBtn,
                                                @Nullable @StringRes Integer neutralBtn,
                                                @Nullable @StringRes Integer negativeBtn,
                                                @Nullable ICollections.IDialogButton callback) {
        return showConfirmDialog(uiContext, title, R.layout.dialog_alert, positiveBtn, neutralBtn, negativeBtn, new ICollections.ObjectCallBack<View>() {
            @Override
            public void callback(View view) {
                TextView tvMessage = (TextView) view.findViewById(R.id.tvMessage);
                if (message != null) {
                    tvMessage.setVisibility(View.VISIBLE);
                    tvMessage.setText(message);
                } else {
                    tvMessage.setVisibility(View.GONE);
                }
            }
        }, callback);
    }

    /**
     * Show alert dialog
     *
     * @param uiContext
     * @param title
     * @param message
     * @return
     */
    public static AlertDialog showAlertDialog(@NonNull Context uiContext,
                                              @Nullable CharSequence title,
                                              @Nullable CharSequence message) {
        return showAlertDialog(uiContext, title, message, null);
    }

    /**
     * Show alert dialog
     *
     * @param uiContext
     * @param title
     * @param message
     * @param callback
     * @return
     */
    public static AlertDialog showAlertDialog(@NonNull Context uiContext,
                                              @Nullable CharSequence title,
                                              @Nullable CharSequence message,
                                              @Nullable ICollections.IDialogAlertButtonImpl callback) {
        AlertDialog d = showDialog(new ContextThemeWrapper(uiContext,
                android.R.style.Theme_Holo_Light_Dialog), title, message, null, -1, null, null, null, (callback == null) ? new ICollections.IDialogAlertButtonImpl() {
            @Override
            public void positive(DialogInterface dialog, View button, View rootView) {
                dialog.cancel();
            }
        } : callback);
        d.setCancelable(false);
        d.setCanceledOnTouchOutside(false);
        return d;
    }

    /**
     * Show simple edit dialog
     *
     * @param uiContext
     * @param title
     * @param editTextHint
     * @param callback
     * @return
     */
    public static AlertDialog showSimpleEditDialog(@NonNull Context uiContext,
                                                   @Nullable CharSequence title,
                                                   @Nullable CharSequence editTextHint,
                                                   @Nullable ICollections.IDialogButton callback) {
        return showSimpleEditDialog(uiContext, title, view -> {
            if (editTextHint != null) {
                EditText etInput = (EditText) view.findViewById(R.id.etInput);
                etInput.setText(editTextHint);
            }
        }, callback);
    }

    /**
     * Show simple edit dialog
     *
     * @param uiContext
     * @param title
     * @param callback
     * @return
     */
    public static AlertDialog showSimpleEditDialog(@NonNull Context uiContext,
                                                   @Nullable CharSequence title,
                                                   @Nullable ICollections.ObjectCallBack<View> initLayout,
                                                   @Nullable ICollections.IDialogButton callback) {
        return showDialog(uiContext, title, null, -1, -1, null, -1, initLayout, callback);
    }

    /**
     * Show confirm dialog
     *
     * @param uiContext
     * @param title
     * @param layout
     * @param positiveBtn
     * @param negativeBtn
     * @param initLayout
     * @param callback
     * @return
     */
    public static AlertDialog showConfirmDialog(@NonNull Context uiContext,
                                                @Nullable CharSequence title,
                                                @Nullable @LayoutRes Integer layout,
                                                @Nullable @StringRes Integer positiveBtn,
                                                @Nullable @StringRes Integer negativeBtn,
                                                @Nullable ICollections.ObjectCallBack<View> initLayout,
                                                @Nullable ICollections.IDialogConfirmButtonImpl callback) {
        return showDialog(uiContext, title, null, layout, positiveBtn, null, negativeBtn, initLayout, callback);
    }

    /**
     * Show confirm dialog
     *
     * @param uiContext
     * @param title
     * @param layout
     * @param positiveBtn
     * @param neutralBtn
     * @param negativeBtn
     * @param initLayout
     * @param callback
     * @return
     */
    public static AlertDialog showConfirmDialog(@NonNull Context uiContext,
                                                @Nullable CharSequence title,
                                                @Nullable @LayoutRes Integer layout,
                                                @Nullable @StringRes Integer positiveBtn,
                                                @Nullable @StringRes Integer neutralBtn,
                                                @Nullable @StringRes Integer negativeBtn,
                                                @Nullable ICollections.ObjectCallBack<View> initLayout,
                                                @Nullable ICollections.IDialogButton callback) {
        return showDialog(uiContext, title, null, layout, positiveBtn, neutralBtn, negativeBtn, initLayout, callback);
    }

    /**
     * Show dialog
     *
     * @param uiContext
     * @param layout
     * @param initLayout
     * @return
     */
    public static AlertDialog showDialog(Context uiContext,
                                         @Nullable @LayoutRes Integer layout,
                                         @Nullable ICollections.ObjectCallBack<View> initLayout) {
        return showDialog(uiContext, null, null, layout, null, null, null, initLayout, null);
    }

    /**
     * Show dialog
     *
     * @param uiContext
     * @param title
     * @param message
     * @param layout
     * @param positiveBtn
     * @param neutralBtn
     * @param negativeBtn
     * @param initLayout
     * @param callback
     * @return
     */
    public static AlertDialog showDialog(Context uiContext,
                                         @Nullable CharSequence title,
                                         @Nullable CharSequence message,
                                         @Nullable @LayoutRes Integer layout,
                                         @Nullable @StringRes Integer positiveBtn,
                                         @Nullable @StringRes Integer neutralBtn,
                                         @Nullable @StringRes Integer negativeBtn,
                                         @Nullable ICollections.ObjectCallBack<View> initLayout,
                                         @Nullable ICollections.IDialogButton callback) {
        final AlertDialog.Builder dBuilder = new AlertDialog.Builder(uiContext);
        final View view;
        if (layout != null) {
            view = LayoutInflater.from(uiContext).inflate((layout == -1) ? R.layout.dialog_simple_edit_text : layout, null, false);
            if (initLayout != null) initLayout.callback(view);
            dBuilder.setView(view);
        } else {
            view = null;
        }
        if (positiveBtn != null) {
            dBuilder.setPositiveButton((positiveBtn == -1) ? android.R.string.ok : positiveBtn, null);
        }
        if (neutralBtn != null) {
            dBuilder.setNeutralButton((neutralBtn == -1) ? R.string.dialog_later : neutralBtn, null);
        }
        if (negativeBtn != null) {
            dBuilder.setNegativeButton((negativeBtn == -1) ? android.R.string.cancel : negativeBtn, null);
        }
        if (message != null) {
            dBuilder.setMessage(message);
        }
        if (title != null) {
            dBuilder.setTitle(title);
        }
        AlertDialog d = dBuilder.create();
        if (callback != null) {
            d.setOnShowListener(dialog -> {
                Button btOk = d.getButton(AlertDialog.BUTTON_POSITIVE);
                if (btOk != null) {
                    btOk.setOnClickListener(view1 -> callback.positive(dialog, view1, view));
                }
                Button btNeutral = d.getButton(AlertDialog.BUTTON_NEUTRAL);
                if (btNeutral != null) {
                    btNeutral.setOnClickListener(view1 -> callback.neutral(dialog, view1, view));
                }
                Button btCancel = d.getButton(AlertDialog.BUTTON_NEGATIVE);
                if (btCancel != null) {
                    btCancel.setOnClickListener(view1 -> callback.negative(dialog, view1, view));
                }
            });
        }
        return d;
    }

}
