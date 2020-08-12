package com.mh.smartroom;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MainActivity extends AppCompatActivity {

    static String URI = "tcp://m15.cloudmqtt.com:10506";
    static String USER = "ahcyltpg";
    static String PASSWORD = "1qlDBAKreMXr";
    final static String PUBLIC_TOPIC = "PHONE";
    final static String SUBSCRIBE_TOPIC = "ESP";
    MqttAndroidClient client;

    Context context;

    final int NUM_OF_RELAY = 4;

    SharedPreferences pref;

    TextView tvTerminal;
    EditText[] DeviceNames = new EditText[NUM_OF_RELAY];
    TextView[] OnTimes = new TextView[NUM_OF_RELAY];
    TextView[] OffTimes = new TextView[NUM_OF_RELAY];
    Button[] ButtonSetTimes = new Button[NUM_OF_RELAY];
    ConstraintLayout[] layouts = new ConstraintLayout[NUM_OF_RELAY];

    Relay[] Relays = new Relay[NUM_OF_RELAY];
    String RelayStatus = "0000";

    int index = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        InitWidgets();
        InitData();
        InitMQTTConnection();
        //InitActionForWidgets();
    }

    public void InitMQTTConnection()
    {
        String clientId = "MHPhone";
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
                    tvTerminal.setText("Connection : Successful");
                    Subscribe(SUBSCRIBE_TOPIC);
                    Publish("PhoneConnected");
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
                //ProcessMQTTData(topic, message);
                System.out.println(message.toString());
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }

    public void ProcessMQTTData(MqttMessage message) {
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
        } catch (MqttException e) {
            e.printStackTrace();
        }
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
    
    protected void InitData() {
        context = getApplicationContext();
        pref = context.getSharedPreferences("MyPref", MODE_PRIVATE);

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

    protected  void InitActionForWidgets() {
        tvTerminal.setMovementMethod(new ScrollingMovementMethod());
        for (index = 0; index < 4; index++)
        {
            ButtonSetTimes[index].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Relays[index].SetTime(OnTimes[index].getText().toString(), OffTimes[index].getText().toString());
                    Publish(Relays[index].Command);
                    prefEditor.putString(Relays[index].ON_TIME_KEY, OnTimes[index].getText().toString());
                    prefEditor.putString(Relays[index].OFF_TIME_KEY, OffTimes[index].getText().toString());
                    prefEditor.commit();
                }
            });

            DeviceNames[index].setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean b) {
                    String name = DeviceNames[index].getText().toString();
                    prefEditor.putString(Relays[index].DEVICE_NAME_KEY, name);
                    prefEditor.commit();
                }
            });


        }
    }
}