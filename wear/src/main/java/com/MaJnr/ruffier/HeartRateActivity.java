package com.MaJnr.ruffier;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.MaJnr.testruffier.R;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.Wearable;

import java.util.Objects;


import static com.MaJnr.common.Constants.HEART_MEASURE_1;
import static com.MaJnr.common.Constants.HEART_MEASURE_2;
import static com.MaJnr.common.Constants.HEART_MEASURE_3;
import static com.MaJnr.common.Constants.MEASURE_DURATION;
import static com.MaJnr.common.Constants.MOBILE_QUIT_APP_PATH;
import static com.MaJnr.common.Constants.STOP_MEASURE_PATH;
import static com.MaJnr.common.Constants.measureNb;
import static com.MaJnr.common.Constants.measuresList1;
import static com.MaJnr.common.Constants.MEASURE_1_MSG;
import static com.MaJnr.common.Constants.MEASURE_2_MSG;
import static com.MaJnr.common.Constants.MEASURE_3_MSG;
import static com.MaJnr.common.Constants.measuresList2;
import static com.MaJnr.common.Constants.measuresList3;

public class HeartRateActivity extends WearableActivity implements DataClient.OnDataChangedListener {

    final String TAG = "HeartRateActivity";

    // view entities
    TextView title;
    TextView description;
    Button done;

    Vibrator vibrator;

    // handle the heart rate service
    Intent rateServiceIntent;
    BroadcastReceiver heartRateReceiver;
    DataClient mDataClient;

    // flags
    int mRate = 0;
    private static final int BOYDY_SENSOR_PERMISSION_CODE = 0;
    private boolean isTimerRunning = false;
    private boolean testEnded = false;

    // 1 min counter before first measure
    CountDownTimer timerBeforeMeasure = new CountDownTimer(60000, 1000) {
        @Override
        public void onTick(long l) {
            Log.d(TAG, "tick 1");
            String txt = "" + l / 1000 + " s";
            description.setText(txt);
        }

        @Override
        public void onFinish() {
            title.setText(R.string.measurement_in_progress);
            description.setText("");
            startService(rateServiceIntent);
        }
    };

    /**
     * Timer used at 3 moments, depending on the test step :
     * 1. At the start of the first measure, launches the heart rate sensor service
     * 2. At the start of the second measure, also launches the heart rate sensor service
     * 3. At the end of the test, finish this activity
     */
    CountDownTimer timerOnMeasureStep = new CountDownTimer(MEASURE_DURATION, 1000) {

        @Override
        public void onTick(long l) {
            Log.d(TAG, "tick 2");
        }

        @Override
        public void onFinish() {
            stopService(rateServiceIntent);
            description.setText("");
            title.setText("");
            if (measureNb == 1) {
                isTimerRunning = false;
                setButtonClickListener(2);
                done.setText(R.string.button_continue);
                done.setVisibility(View.VISIBLE);
                description.setText(R.string.instruct_vibration);
            } else if (measureNb == 2) {
                isTimerRunning = false;
                title.setText(R.string.instruct_lie);
            } else if (measureNb == 3) {
                title.setText(R.string.test_ended);
                description.setText(R.string.result_sent);
                new CountDownTimer(10000, 1000) {
                    @Override
                    public void onTick(long l) {
                        Log.d(TAG, "tick before finish");
                    }

                    @Override
                    public void onFinish() {
                        testEnded = true;
                        HeartRateActivity.this.finish();
                    }
                }.start();
            }
        }
    };

    // timer used to vibrate the wear to rhythm the squat step
    CountDownTimer timerOnSquatStep = new CountDownTimer(45000, 1500) {
        @Override
        public void onTick(long l) {
            Log.d(TAG, "onTick3");
            // vibrate
            vibrator.vibrate(100);
            String s = "" + l / 1500;
            title.setText(s);
        }

        @Override
        public void onFinish() {
            title.setText(R.string.instruct_lie);
            timerBeforeMeasure.start();
            startService(rateServiceIntent);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_heartrate);
        Log.d(TAG, "creation");

        title = findViewById(R.id.title);
        description = findViewById(R.id.description);

        mDataClient = Wearable.getDataClient(this);

        // send start of test msg
        SyncAsyncTaskWearRunningTest mSyncAsyncTaskWearRunningTest = new SyncAsyncTaskWearRunningTest(getApplicationContext(), 0);
        mSyncAsyncTaskWearRunningTest.execute(0);

        heartRateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // filter received intent to send different messages

                switch (Objects.requireNonNull(intent.getAction())) {
                    case MEASURE_1_MSG:
                        Log.d(TAG, "received msg 1");
                        mRate = intent.getIntExtra(HEART_MEASURE_1, 0);
                        String s1 = "" + mRate + " bpm";
                        description.setText(s1);

                        break;
                    case MEASURE_2_MSG:
                        Log.d(TAG, "received msg 2");
                        mRate = intent.getIntExtra(HEART_MEASURE_2, 0);
                        String s2 = "" + mRate + " bpm";
                        title.setText(s2);

                        break;
                    case MEASURE_3_MSG:
                        Log.d(TAG, "received msg 3");
                        mRate = intent.getIntExtra(HEART_MEASURE_3, 0);
                        String s3 = "" + mRate + " bpm";
                        description.setText(s3);

                        break;
                    default:
                        Log.e(TAG, "heart rate receiver error");
                }
                if (!isTimerRunning) {
                    timerOnMeasureStep.start();
                    isTimerRunning = true;
                }
            }
        };

        checkBodySensorPermissionisRequired();

        // enable always-on
        setAmbientEnabled();

        done = findViewById(R.id.action);
        setButtonClickListener(1);

        rateServiceIntent = new Intent(this, WearHeartRateService.class);

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        Wearable.getDataClient(this).addListener(this);
    }

    /**
     * Set the action and the text of the button to avoid using multiple ones
     * @param step : the step of the test
     */
    public void setButtonClickListener(int step) {
        if (step == 1) {
            done.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(TAG, "onClick");
                    done.setVisibility(View.GONE);
                    title.setText(R.string.start_of_measure_in);
                    timerBeforeMeasure.start();
                }
            });
        } else if (step == 2) {
            done.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(TAG, "onClick");
                    done.setVisibility(View.GONE);
                    description.setText("");
                    new CountDownTimer(5000, 1000) {
                        @Override
                        public void onTick(long l) {
                            Log.d(TAG, "beforeFlex");
                            String s = getString(R.string.start_in) + " " + l / 1000;
                            title.setText(s);
                        }

                        @Override
                        public void onFinish() {
                            timerOnSquatStep.start();
                        }
                    }.start();
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "resume");

        // set the filters for the broadcast receiver
        IntentFilter filter1 = new IntentFilter(MEASURE_1_MSG);
        IntentFilter filter2 = new IntentFilter(MEASURE_2_MSG);
        IntentFilter filter3 = new IntentFilter(MEASURE_3_MSG);
        registerReceiver(heartRateReceiver, filter1);
        registerReceiver(heartRateReceiver, filter2);
        registerReceiver(heartRateReceiver, filter3);

        // reset the measures number
        measureNb = 1;
        measuresList1.clear();
        measuresList2.clear();
        measuresList3.clear();
    }


    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        if (!testEnded) {
            // abort the test on wear and on mobile
            Log.d(TAG, "test not ended");
            SyncAsyncTaskWearRunningTest wearQuitTestTask = new SyncAsyncTaskWearRunningTest(this, 0);
            wearQuitTestTask.execute(1);
            finish();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        timerBeforeMeasure.cancel();
        timerOnMeasureStep.cancel();
        timerOnSquatStep.cancel();
        stopService(rateServiceIntent);
        unregisterReceiver(heartRateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        Wearable.getDataClient(this).removeListener(this);
    }

    public void checkBodySensorPermissionisRequired() {
        // Check if the BODY_SENSOR permission is already available.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS)
                != PackageManager.PERMISSION_GRANTED) {
            // body sensor has not been granted.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BODY_SENSORS}, BOYDY_SENSOR_PERMISSION_CODE);
        } else {
            // Body Sensor permissions is already available.
            Log.i("RateFragment", "BODY_SENSOR permission has already been granted.");
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == BOYDY_SENSOR_PERMISSION_CODE) {
            // BEGIN_INCLUDE(permission_result)
            // Received permission result for camera permission.
            Log.i("RateFragment", "Received response for BODY_SENSOR permission request.");

            // Check if the only required permission has been granted
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Camera permission has been granted, preview can be displayed
                Log.i("RateFragment", "BODY_SENSOR permission has now been granted. Showing preview.");

            } else {
                Log.i("RateFragment", "BODY_SENSOR permission was NOT granted.");
            }
            // END_INCLUDE(permission_result)
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    @Override
    public void onDataChanged(@NonNull DataEventBuffer dataEventBuffer) {
        Log.d(TAG, "OnDataChanged" + dataEventBuffer);

        for (DataEvent event : dataEventBuffer) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                // DataItem changed
                DataItem item = event.getDataItem();
                if (Objects.requireNonNull(item.getUri().getPath()).compareTo(STOP_MEASURE_PATH) == 0) {
                    // cancel button clicked on mobile
                    Log.d(TAG, "cancel called on mobile");
                    finish();
                } else if (Objects.requireNonNull(item.getUri().getPath()).compareTo(MOBILE_QUIT_APP_PATH) == 0) {
                    // app was closed by user
                    Log.d(TAG, "app closed on mobile");
                    drawInfoBox();
                }
            }
        }
    }

    public void drawInfoBox() {
        Intent wearAlertInfoIntent = new Intent(this, WearAlertInfoActivity.class);
        startActivity(wearAlertInfoIntent);
        finish();
    }

}
