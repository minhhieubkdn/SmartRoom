package com.mh.mhroom;

import android.support.constraint.ConstraintLayout;

public class Relay {
    public static final boolean ON = true;
    public static final boolean OFF = false;

    public String deviceName;
    public boolean status = ON;
    public String onTime;
    public String offTime;
    public String Command;

    public int hour, minute;
    public int onTimeInMinutes;
    public int offTimeInMinutes;

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

    public void ChangeStatus() {
        if(status == ON) {
            Off();
        } else {
            On();
        }
    }
    public void SetTime(String _onTime, String _offTime) {
        onTime = _onTime;
        offTime = _offTime;

    }

    public void SetOnTime(int _hour, int _min) {
        onTimeInMinutes = _hour * 60 + _min;
    }

    public void SetOffTime(int _hour, int _min) {
        offTimeInMinutes = _hour * 60 + _min;
    }
}
