package com.mh.smartroom;

import androidx.constraintlayout.widget.ConstraintLayout;

public class Relay {
    public static final boolean ON = true;
    public static final boolean OFF = false;

    public String deviceName;
    public boolean status;
    public String onTime;
    public String offTime;
    public String Command;

    public int hour, minute;

    public String ON_TIME_KEY;
    public String OFF_TIME_KEY;
    public String DEVICE_NAME_KEY;
    public ConstraintLayout backgroundLayout;

    public Relay() {
        deviceName = "Device ";
        status = false;
        onTime = "00:00";
        offTime = "00:00";
        DEVICE_NAME_KEY = "DEFAULT_NAME_KEY";
        ON_TIME_KEY = "DEFAULT_ON_TIME_KEY";
        OFF_TIME_KEY = "DEFAULT_OFF_TIME_KEY";
    }

    public void On() {
        status = ON;
        backgroundLayout.setBackgroundResource(R.drawable.relay_layout);
    }

    public void Off() {
        status = OFF;
        backgroundLayout.setBackgroundResource(R.drawable.relay_off_layout);
    }

    public void SetTime(String _onTime, String _offTime) {
        onTime = _onTime;
        offTime = _offTime;

    }
}
