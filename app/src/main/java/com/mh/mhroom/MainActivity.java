package com.mh.mhroom;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.constraint.ConstraintLayout;

import android.content.Context;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.sql.Struct;
import java.util.Calendar;

public class MainActivity extends FragmentActivity{

    static String URI = "tcp://m15.cloudmqtt.com:10506";
    static String USER = "ahcyltpg";
    static String PASSWORD = "1qlDBAKreMXr";
    final static String PUBLIC_TOPIC = "PHONE";
    final static String SUBSCRIBE_TOPIC = "ESP";
    public MqttAndroidClient client;

    Context context;

    final int NUM_OF_RELAY = 4;

    public SharedPreferences pref;
    public SharedPreferences.Editor prefEditor;

    public TextView tvTerminal;
    public EditText[] DeviceNames = new EditText[NUM_OF_RELAY];
    public TextView[] OnTimes = new TextView[NUM_OF_RELAY];
    public TextView[] OffTimes = new TextView[NUM_OF_RELAY];
    public Button[] ButtonSetTimes = new Button[NUM_OF_RELAY];
    public ConstraintLayout[] layouts = new ConstraintLayout[NUM_OF_RELAY];

    public Relay[] Relays = new Relay[NUM_OF_RELAY];
    public String RelayStatus = "0000";

    public int hour, minute;
    public String hourString, minuteString;
    public String timeInMinutes;

    public int index = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        InitWidgets();
        InitMQTTConnection();
        InitData();
        InitActionForWidgets();
    }

    protected void InitWidgets() {
        tvTerminal = (TextView) findViewById(R.id.tv_terminal);
        DeviceNames[0] = (EditText) findViewById(R.id.tv_device_0_name);
        DeviceNames[1] = (EditText) findViewById(R.id.tv_device_1_name);
        DeviceNames[2]  = (EditText) findViewById(R.id.tv_device_2_name);
        DeviceNames[3]  = (EditText) findViewById(R.id.tv_device_3_name);
        OnTimes[0] = (TextView) findViewById(R.id.tv_on_time_0);
        OnTimes[1] = (TextView) findViewById(R.id.tv_on_time_1);
        OnTimes[2] = (TextView) findViewById(R.id.tv_on_time_2);
        OnTimes[3] = (TextView) findViewById(R.id.tv_on_time_3);
        OffTimes[0] = (TextView) findViewById(R.id.tv_off_time_0);
        OffTimes[1] = (TextView) findViewById(R.id.tv_off_time_1);
        OffTimes[2] = (TextView) findViewById(R.id.tv_off_time_2);
        OffTimes[3] = (TextView) findViewById(R.id.tv_off_time_3);
        ButtonSetTimes[0] = (Button) findViewById(R.id.bt_set_time_0);
        ButtonSetTimes[1] = (Button) findViewById(R.id.bt_set_time_1);
        ButtonSetTimes[2] = (Button) findViewById(R.id.bt_set_time_2);
        ButtonSetTimes[3] = (Button) findViewById(R.id.bt_set_time_3);
        layouts[0] = (ConstraintLayout) findViewById(R.id.cl_device_0);
        layouts[1] = (ConstraintLayout) findViewById(R.id.cl_device_1);
        layouts[2] = (ConstraintLayout) findViewById(R.id.cl_device_2);
        layouts[3] = (ConstraintLayout) findViewById(R.id.cl_device_3);
    }

    @SuppressLint("CommitPrefEdits")
    protected void InitData() {
        context = getApplicationContext();
        pref = context.getSharedPreferences("MyPref", MODE_PRIVATE);
        prefEditor = pref.edit();
        for ( index = 0; index < 4; index++) {

            Relays[index] = new Relay();

            if (RelayStatus.charAt(index) == '1') {
                Relays[index].status = Relay.ON;
            } else if (RelayStatus.charAt(index) == '0') {
                Relays[index].status = Relay.OFF;
            }

            Relays[index].DEVICE_NAME_KEY = "DEVICE_NAME_KEY_" + Integer.toString(index);
            Relays[index].ON_TIME_KEY = "ON_TIME_KEY_" + Integer.toString(index);
            Relays[index].OFF_TIME_KEY = "OFF_TIME_KEY_" + Integer.toString(index);

            Relays[index].deviceName = pref.getString(Relays[index].DEVICE_NAME_KEY, "Device " + index);
            Relays[index].onTime = pref.getString(Relays[index].ON_TIME_KEY, "00:00");
            Relays[index].offTime = pref.getString(Relays[index].OFF_TIME_KEY, "00:00");
            Relays[index].backgroundLayout = layouts[index];

            DeviceNames[index].setText(Relays[index].deviceName);
            OnTimes[index].setText(Relays[index].onTime);
            OffTimes[index].setText(Relays[index].offTime);
        }
    }

    public void InitActionForWidgets() {
        tvTerminal.setMovementMethod(new ScrollingMovementMethod());
        tvTerminal.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                tvTerminal.setText("");
                return false;
            }
        });
        OnTimes[0].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Calendar c = Calendar.getInstance();
                int mHour = c.get(Calendar.HOUR_OF_DAY);
                int mMinute = c.get(Calendar.MINUTE);

                TimePickerDialog timePickerDialog = new TimePickerDialog(MainActivity.this,
                        new TimePickerDialog.OnTimeSetListener() {

                            @Override
                            public void onTimeSet(TimePicker timePickerView, int selectedHour,
                                                  int selectedMinute) {
                                Relays[0].SetOnTime(selectedHour, selectedMinute);
                                if (selectedHour < 10) {
                                    OnTimes[0].setText("0" + Integer.toString(selectedHour) + ":" + Integer.toString(selectedMinute));
                                } else {
                                    OnTimes[0].setText(Integer.toString(selectedHour) + ":" + Integer.toString(selectedMinute));
                                }
                            }
                        }, mHour, mMinute, true);
                timePickerDialog.show();
            }
        });
        OnTimes[1].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Calendar c = Calendar.getInstance();
                int mHour = c.get(Calendar.HOUR_OF_DAY);
                int mMinute = c.get(Calendar.MINUTE);

                TimePickerDialog timePickerDialog = new TimePickerDialog(MainActivity.this,
                        new TimePickerDialog.OnTimeSetListener() {

                            @Override
                            public void onTimeSet(TimePicker timePickerView, int selectedHour,
                                                  int selectedMinute) {
                                Relays[1].SetOnTime(selectedHour, selectedMinute);
                                if (selectedHour < 10) {
                                    OnTimes[1].setText("0" + Integer.toString(selectedHour) + ":" + Integer.toString(selectedMinute));
                                } else {
                                    OnTimes[1].setText(Integer.toString(selectedHour) + ":" + Integer.toString(selectedMinute));
                                }
                            }
                        }, mHour, mMinute, true);
                timePickerDialog.show();

            }
        });
        OnTimes[2].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Calendar c = Calendar.getInstance();
                int mHour = c.get(Calendar.HOUR_OF_DAY);
                int mMinute = c.get(Calendar.MINUTE);

                TimePickerDialog timePickerDialog = new TimePickerDialog(MainActivity.this,
                        new TimePickerDialog.OnTimeSetListener() {

                            @Override
                            public void onTimeSet(TimePicker timePickerView, int selectedHour,
                                                  int selectedMinute) {
                                Relays[2].SetOnTime(selectedHour, selectedMinute);
                                if (selectedHour < 10) {
                                    OnTimes[2].setText("0" + Integer.toString(selectedHour) + ":" + Integer.toString(selectedMinute));
                                } else {
                                    OnTimes[2].setText(Integer.toString(selectedHour) + ":" + Integer.toString(selectedMinute));
                                }
                            }
                        }, mHour, mMinute, true);
                timePickerDialog.show();

            }
        });
        OnTimes[3].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Calendar c = Calendar.getInstance();
                int mHour = c.get(Calendar.HOUR_OF_DAY);
                int mMinute = c.get(Calendar.MINUTE);

                TimePickerDialog timePickerDialog = new TimePickerDialog(MainActivity.this,
                        new TimePickerDialog.OnTimeSetListener() {

                            @Override
                            public void onTimeSet(TimePicker timePickerView, int selectedHour,
                                                  int selectedMinute) {
                                Relays[3].SetOnTime(selectedHour, selectedMinute);
                                if (selectedHour < 10) {
                                    OnTimes[3].setText("0" + Integer.toString(selectedHour) + ":" + Integer.toString(selectedMinute));
                                } else {
                                    OnTimes[3].setText(Integer.toString(selectedHour) + ":" + Integer.toString(selectedMinute));
                                }
                            }
                        }, mHour, mMinute, true);
                timePickerDialog.show();

            }
        });

        OffTimes[0].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Calendar c = Calendar.getInstance();
                int mHour = c.get(Calendar.HOUR_OF_DAY);
                int mMinute = c.get(Calendar.MINUTE);

                TimePickerDialog timePickerDialog = new TimePickerDialog(MainActivity.this,
                        new TimePickerDialog.OnTimeSetListener() {

                            @Override
                            public void onTimeSet(TimePicker timePickerView, int selectedHour,
                                                  int selectedMinute) {
                                Relays[0].SetOffTime(selectedHour, selectedMinute);
                                if (selectedHour < 10) {
                                    OffTimes[0].setText("0" + Integer.toString(selectedHour) + ":" + Integer.toString(selectedMinute));
                                } else {
                                    OffTimes[0].setText(Integer.toString(selectedHour) + ":" + Integer.toString(selectedMinute));
                                }
                            }
                        }, mHour, mMinute, false);
                timePickerDialog.show();
            }
        });
        OffTimes[1].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Calendar c = Calendar.getInstance();
                int mHour = c.get(Calendar.HOUR_OF_DAY);
                int mMinute = c.get(Calendar.MINUTE);

                TimePickerDialog timePickerDialog = new TimePickerDialog(MainActivity.this,
                        new TimePickerDialog.OnTimeSetListener() {

                            @Override
                            public void onTimeSet(TimePicker timePickerView, int selectedHour,
                                                  int selectedMinute) {
                                Relays[1].SetOffTime(selectedHour, selectedMinute);
                                if (selectedHour < 10) {
                                    OffTimes[1].setText("0" + Integer.toString(selectedHour) + ":" + Integer.toString(selectedMinute));
                                } else {
                                    OffTimes[1].setText(Integer.toString(selectedHour) + ":" + Integer.toString(selectedMinute));
                                }
                            }
                        }, mHour, mMinute, false);
                timePickerDialog.show();
            }
        });
        OffTimes[2].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Calendar c = Calendar.getInstance();
                int mHour = c.get(Calendar.HOUR_OF_DAY);
                int mMinute = c.get(Calendar.MINUTE);

                TimePickerDialog timePickerDialog = new TimePickerDialog(MainActivity.this,
                        new TimePickerDialog.OnTimeSetListener() {

                            @Override
                            public void onTimeSet(TimePicker timePickerView, int selectedHour,
                                                  int selectedMinute) {
                                Relays[2].SetOffTime(selectedHour, selectedMinute);
                                if (selectedHour < 10) {
                                    OffTimes[2].setText("0" + Integer.toString(selectedHour) + ":" + Integer.toString(selectedMinute));
                                } else {
                                    OffTimes[2].setText(Integer.toString(selectedHour) + ":" + Integer.toString(selectedMinute));
                                }
                            }
                        }, mHour, mMinute, false);
                timePickerDialog.show();
            }
        });
        OffTimes[3].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Calendar c = Calendar.getInstance();
                int mHour = c.get(Calendar.HOUR_OF_DAY);
                int mMinute = c.get(Calendar.MINUTE);

                TimePickerDialog timePickerDialog = new TimePickerDialog(MainActivity.this,
                        new TimePickerDialog.OnTimeSetListener() {

                            @Override
                            public void onTimeSet(TimePicker timePickerView, int selectedHour,
                                                  int selectedMinute) {
                                Relays[3].SetOffTime(selectedHour, selectedMinute);
                                if (selectedHour < 10) {
                                    OffTimes[3].setText("0" + Integer.toString(selectedHour) + ":" + Integer.toString(selectedMinute));
                                } else {
                                    OffTimes[3].setText(Integer.toString(selectedHour) + ":" + Integer.toString(selectedMinute));
                                }
                            }
                        }, mHour, mMinute, false);
                timePickerDialog.show();
            }
        });

        ButtonSetTimes[0].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String mess = getCommand(0, OnTimes[0].getText().toString(), OffTimes[0].getText().toString());
                Publish(mess);
                prefEditor.putString(Relays[0].DEVICE_NAME_KEY, DeviceNames[0].getText().toString());
                prefEditor.putString(Relays[0].ON_TIME_KEY, OnTimes[0].getText().toString());
                prefEditor.putString(Relays[0].OFF_TIME_KEY, OffTimes[0].getText().toString());
                prefEditor.commit();
            }
        });
        ButtonSetTimes[1].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String mess = getCommand(1, OnTimes[1].getText().toString(), OffTimes[1].getText().toString());
                Publish(mess);
                prefEditor.putString(Relays[1].DEVICE_NAME_KEY, DeviceNames[1].getText().toString());
                prefEditor.putString(Relays[1].ON_TIME_KEY, OnTimes[1].getText().toString());
                prefEditor.putString(Relays[1].OFF_TIME_KEY, OffTimes[1].getText().toString());
                prefEditor.commit();
            }
        });
        ButtonSetTimes[2].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String mess = getCommand(2, OnTimes[2].getText().toString(), OffTimes[2].getText().toString());
                Publish(mess);
                prefEditor.putString(Relays[2].DEVICE_NAME_KEY, DeviceNames[2].getText().toString());
                prefEditor.putString(Relays[2].ON_TIME_KEY, OnTimes[2].getText().toString());
                prefEditor.putString(Relays[2].OFF_TIME_KEY, OffTimes[2].getText().toString());
                prefEditor.commit();
            }
        });
        ButtonSetTimes[3].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String mess = getCommand(3, OnTimes[3].getText().toString(), OffTimes[3].getText().toString());
                Publish(mess);
                prefEditor.putString(Relays[3].DEVICE_NAME_KEY, DeviceNames[3].getText().toString());
                prefEditor.putString(Relays[3].ON_TIME_KEY, OnTimes[3].getText().toString());
                prefEditor.putString(Relays[3].OFF_TIME_KEY, OffTimes[3].getText().toString());
                prefEditor.commit();
            }
        });

        layouts[0].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Relays[0].status) {
                    Publish("R0 0");
                } else {
                    Publish("R0 1");
                }
                Relays[0].ChangeStatus();
            }
        });
        layouts[1].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Relays[1].status) {
                    Publish("R1 0");
                } else {
                    Publish("R1 1");
                }
                Relays[1].ChangeStatus();
            }
        });
        layouts[2].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Relays[2].status) {
                    Publish("R2 0");
                } else {
                    Publish("R2 1");
                }
                Relays[2].ChangeStatus();
            }
        });
        layouts[3].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Relays[3].status) {
                    Publish("R3 0");
                } else {
                    Publish("R3 1");
                }
                Relays[3].ChangeStatus();
            }
        });

        for (index = 0; index < 4; index++)
        {
            DeviceNames[index].setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean b) {
                    if (!b) {
                        hideKeyboard(view);
                    }
                }
            });
        }
    }

    public String getCommand(int _relayIndex, String _onTime, String _offTime) {
        String command;
        int _onHour, _onMin, _offHour, _offMin;
        _onHour = Integer.parseInt(_onTime.substring(0, _onTime.indexOf(":")));
        _onMin = Integer.parseInt(_onTime.substring(_onTime.indexOf(":") + 1));
        _offHour = Integer.parseInt(_offTime.substring(0, _offTime.indexOf(":")));
        _offMin = Integer.parseInt(_offTime.substring(_offTime.indexOf(":") + 1));
        command = "R" + Integer.toString(_relayIndex) + " S"
                + Integer.toString(_onHour * 60 + _onMin) + ","
                +  Integer.toString(_offHour * 60 + _offMin);
        return command;

    }

    void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public void InitMQTTConnection()
    {
        String clientId = MqttClient.generateClientId();
        client = new MqttAndroidClient(this.getApplicationContext(), URI, clientId);

        MqttConnectOptions options = new MqttConnectOptions();
        options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1);
        options.setUserName(USER);
        options.setPassword(PASSWORD.toCharArray());

        try {
            IMqttToken token = client.connect(options);

            tvTerminal.setText("Connecting ...");

            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    tvTerminal.setText("Connection : Successful\n");
                    Subscribe(SUBSCRIBE_TOPIC);
                    Publish("gStt");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    tvTerminal.setText("Connection : Failed");

                }
            });
        } catch (MqttException e) {

            tvTerminal.setText("Error");
            e.printStackTrace();
        }

        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                ProcessMQTTData(topic, message);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }

    public void ProcessMQTTData(String topic, MqttMessage message)
    {
        String msg = message.toString();
        if (msg.startsWith("R")) {
            int ind = Integer.parseInt(msg.substring(1, 2));
            int stt = Integer.parseInt(msg.substring(3));
            if (stt == 0) {
                Relays[ind].Off();
            } else if (stt == 1) {
                Relays[ind].On();
            }
        } else if (msg.startsWith("1") || msg.startsWith("0")) {
            RelayStatus = msg;
            for ( int charInd = 0; charInd < 4; charInd++)
            {
                if (msg.charAt(charInd) == '0') {
                    Relays[charInd].Off();
                } else if (msg.charAt(charInd) == '1') {
                    Relays[charInd].On();
                }
            }
        }
    }

    public void Subscribe(String topic) {
        int qos = 1;
        try {
            IMqttToken subToken = client.subscribe(topic, qos);
            subToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {

                }
                @Override
                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {
                    // The subscription could not be performed, maybe the user was not
                    // authorized to subscribe on the specified topic e.g. using wildcards
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void Publish(String msg) {
        try {
            MqttMessage message = new MqttMessage();
            message.setPayload(msg.getBytes());
            client.publish(PUBLIC_TOPIC, message);
            tvTerminal.append(msg + "\n");
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


}