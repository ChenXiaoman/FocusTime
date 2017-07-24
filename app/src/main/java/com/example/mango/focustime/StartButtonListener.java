package com.example.mango.focustime;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.example.mango.focustime.Activity.FocusModeActivity;
import com.example.mango.focustime.Activity.HowToUseActivity;
import com.example.mango.focustime.Activity.PunishmentActivity;
import com.example.mango.focustime.Activity.WhiteListActivity;
import com.example.mango.focustime.lineartimer.LinearTimerStates;
import com.example.mango.focustime.processutil.Features;
import com.example.mango.focustime.receiver.ScreenReceiver;
import com.example.mango.focustime.service.MyService;

import com.example.mango.focustime.lineartimer.LinearTimer;
import com.example.mango.focustime.util.PreferenceUtilities;

/**
 * Created by chenxiaoman on 23/6/17.
 */

public class StartButtonListener implements View.OnClickListener {

    protected static Context context;
    protected static Activity activity;
    public static CountDownTimer timer;
    private BroadcastReceiver mReceiver;
    private static boolean timerStarted = false;

    private final EditText second;
    private final EditText minute;
    private final Button s;
    private Intent intent;
    private Notification notification;
    private NotificationCompat.Builder mBuilder;
    private PendingIntent pendingIntent;

    private int totalSecond = 0;

    private LinearTimer linearTimer;

    private SharedPreferences sharedPreferences;
    private long secondLeft;
    private long totalSecondPassed;
    private boolean serviceStarted;
    private boolean alrClickForgot;


    public StartButtonListener(Context context, Activity activity, LinearTimer linearTimer, BroadcastReceiver receiver) {
        this.context = context;
        this.activity = activity;
        this.intent = activity.getIntent();
        this.linearTimer = linearTimer;
        this.mReceiver = receiver;
        alrClickForgot = false;

        second = (EditText) activity.findViewById(R.id.second);
        minute = (EditText) activity.findViewById(R.id.minute);
        s = (Button) activity.findViewById(R.id.start);

    }

    @Override
    public void onClick(View view) {

        if (s.getText().equals(context.getResources().getString(R.string.start_button))) {
            if (convertUserInputToTotalSecondsValid()) {
                if (isAccessibilitySettingsOn(context)) {
                    if (!alrClickForgot && PreferenceUtilities.getShowBlacklistDialog(context)) {
                        reminderBlacklist();
                    } else {
                        startTimer();
                    }
                } else {
                    openAccessibilityService();
                }
            }
        } else {
            showAlertForQuitting();
        }
    }

    private void reminderBlacklist() {
        final View content = activity.getLayoutInflater().inflate(
                R.layout.dialog_content, null);
        final CheckBox userCheck = (CheckBox) content //the checkbox from that view
                .findViewById(R.id.check_box1);
        //build the dialog
        new AlertDialog.Builder(context)
                .setTitle(R.string.blacklist)
                .setMessage(R.string.remember_blacklist)
                .setView(content)
                .setPositiveButton(R.string.i_did,
                        new DialogInterface.OnClickListener() {

                            public void onClick(
                                    DialogInterface dialog,
                                    int which) {
                                PreferenceUtilities.setShowBlacklistDialog(context, !userCheck.isChecked());
                                dialog.dismiss(); //end the dialog.

                                alrClickForgot = true;

                                //Start the timer normally
                                startTimer();
                            }
                        })
                .setNegativeButton(R.string.forgot,
                        new DialogInterface.OnClickListener() {
                            public void onClick(
                                    DialogInterface dialog,
                                    int which) {
                                PreferenceUtilities.setShowBlacklistDialog(context, !userCheck.isChecked());
                                dialog.dismiss();

                                alrClickForgot = true;

                                //Send users to set their blacklist
                                Intent intent = new Intent(context, WhiteListActivity.class);
                                context.startActivity(intent);
                            }
                        }).show();
    }

    public static boolean FocusModeStarted() {
        return timerStarted;
    }

    private boolean convertUserInputToTotalSecondsValid() {
        if (minute.getText().toString().equals("") && second.getText().toString().equals("")) {
            Toast.makeText(context, R.string.please_enter_time, Toast.LENGTH_LONG).show();
            return false;
        } else if (minute.getText().toString().equals("")) {
            if (Integer.parseInt(second.getText().toString()) <= 0) {
                Toast.makeText(context, R.string.invalid_number, Toast.LENGTH_SHORT).show();
                return false;
            } else {
                totalSecond = Integer.parseInt(second.getText().toString());
                return true;
            }
        } else if (second.getText().toString().equals("")) {
            if (Integer.parseInt(minute.getText().toString()) <= 0) {
                Toast.makeText(context, R.string.invalid_number, Toast.LENGTH_SHORT).show();
                return false;
            } else {
                totalSecond = Integer.parseInt(minute.getText().toString()) * 60;
                return true;
            }
        } else {
            if (Integer.parseInt(minute.getText().toString()) <= 0 && Integer.parseInt(second.getText().toString()) <= 0) {
                Toast.makeText(context, R.string.invalid_number, Toast.LENGTH_SHORT).show();
                return false;
            } else {
                totalSecond = Integer.parseInt(minute.getText().toString()) * 60 + Integer.parseInt(second.getText().toString());
                return true;
            }
        }
    }

    private void startTimer() {
        timer = new CountDownTimer(totalSecond * 1000, 1000) {

            public void onTick(long millisUntilFinished) {
                secondLeft = millisUntilFinished / 1000;
                long m = secondLeft / 60;
                long s = secondLeft - m * 60;

                String M = "" + m;
                String S = "" + s;

                //Format
                if(M.length() == 1) {
                    M = "0" + M;
                }
                if(S.length() == 1) {
                    S = "0" + S;
                }

                minute.setText(M);
                second.setText(S);
                if(!ScreenReceiver.wasScreenOn && serviceStarted) {
                    Log.v("StartButtonListener", "stopService");
                    stopService();
                } else if (ScreenReceiver.wasScreenOn && !serviceStarted) {
                    Log.v("StartButtonListener", "startService");
                    startService();
                }
            }

            public void onFinish() {
                Toast.makeText(context, R.string.relax, Toast.LENGTH_LONG).show();
                clearTimer();
            }
        };

        openAccessibilityService();

        //linearTimer.builder.

        timer.start();
        linearTimer.startTimer((totalSecond - 1) * 1000);
        timerStarted = true;

        startService();


        s.setText(R.string.cancel);

        minute.setEnabled(false);
        minute.setClickable(false);


        second.setEnabled(false);
        second.setClickable(false);

        Toast.makeText(context, R.string.mode_started, Toast.LENGTH_LONG).show();
    }

    private void startService() {
        serviceStarted = true;
        Features.showForeground = true;
        Intent intent = new Intent(context, MyService.class);
        context.startService(intent);
    }

    // Create an AlertDialog.Builder and set the message, and click listeners
    // for the postivie and negative buttons on the dialog.
    private void showAlertForQuitting() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.quit_mode);
        builder.setMessage(R.string.sure_to_quit);
        builder.setNegativeButton(R.string.confirm, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                clearTimer();
                Intent intent = new Intent(context, PunishmentActivity.class);
                context.startActivity(intent);
            }
        });
        builder.setPositiveButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the pet.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();

    }

    private void clearTimer() {

        timer.cancel();
        if (linearTimer.getState().equals(LinearTimerStates.ACTIVE)) {
            linearTimer.pauseTimer();
        } else if (!linearTimer.getState().equals(LinearTimerStates.INITIALIZED)) {
            linearTimer.resetTimer();
        }

        timerStarted = false;
        s.setText(R.string.start_button);

        minute.setEnabled(true);
        minute.setClickable(true);
        second.setEnabled(true);
        second.setClickable(true);

        minute.setText("");
        second.setText("");

        stopService();

        storeTotalSecondsPassed();

        alrClickForgot = false;
    }

    private void stopService() {
        // Stop detection service
        serviceStarted = false;
        Features.showForeground = false;
        Intent i = new Intent(context, MyService.class);
        context.stopService(i);
    }

    private void countTotalSecondsPassed() {
        totalSecondPassed = totalSecond - secondLeft;
    }

    private void storeTotalSecondsPassed() {
        countTotalSecondsPassed();
        Long totalSecondRecorded = PreferenceUtilities.getTotalSecondRecorded(context);
        totalSecondRecorded += totalSecondPassed + 1;
        PreferenceUtilities.setSecondRecorded(context, totalSecondRecorded);
    }

    final static String TAG = "AccessibilityUtil";

    // Check whether the accessibilitySetting is on
    public static boolean isAccessibilitySettingsOn(Context context) {
        int accessibilityEnabled = 0;
        try {
            accessibilityEnabled = Settings.Secure.getInt(context.getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            Log.i(TAG, e.getMessage());
        }

        if (accessibilityEnabled == 1) {
            String services = Settings.Secure.getString(context.getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (services != null) {
                return services.toLowerCase().contains(context.getPackageName().toLowerCase());
            }
        }

        return false;
    }

    private void openAccessibilityService() {
        // 判断辅助功能是否开启
        if (!isAccessibilitySettingsOn(context)) {

            AlertDialog.Builder noPermissionDialog;

            noPermissionDialog = new AlertDialog.Builder(context)
                    .setTitle(R.string.accessibility)
                    .setMessage(R.string.please_open_access)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Toast.makeText(context, R.string.click_access, Toast.LENGTH_LONG).show();
                            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            // 引导至辅助功能设置页面
                            activity.startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                        }
                    });
//                    .setNsegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//
//                        }
//                    });
            noPermissionDialog.show();
        } else {
            return;
        }
    }



}
