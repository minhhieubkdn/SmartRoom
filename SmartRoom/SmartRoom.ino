/*
 Name:		SmartRoom.ino
 Created:	7/13/2020 10:10:00 PM
 Author:	Minh Hieu
*/

#include "Relay.h"
#include <PubSubClient.h>
#include <NTPClient.h>
#include <WiFiUdp.h>
#include <ESP8266WiFi.h>
#include <EEPROM.h>

#define RL1_PIN D3
#define RL2_PIN D2
#define RL3_PIN D1
#define RL4_PIN D0

#define MQTT_SERVER "m15.cloudmqtt.com"
#define MQTT_TOPIC_PUB "ESP"
#define MQTT_TOPIC_SUB "PHONE"
#define MQTT_USER "ahcyltpg"
#define MQTT_PASSWORD "1qlDBAKreMXr"
const uint16_t MQTT_PORT = 10506;

#define TIMER_DELAY 2000

#define UTC_OFFSET_IN_SECS 25200 //UTC +7 * 60 *60
WiFiUDP ntpUDP;
NTPClient TimeClient(ntpUDP, "asia.pool.ntp.org", UTC_OFFSET_IN_SECS);

WiFiClient espClient;
PubSubClient client(espClient);
String MqttMessage = "";
String InputString = "";
boolean StringComplete = false;
boolean isSendCommand = false;

char Ssid[32] = "";
char Password[32] = "";
bool isWifiConnected = false; \
int wifiMode = 0;

Relay Relay1(RL1_PIN);
Relay Relay2(RL2_PIN);
Relay Relay3(RL3_PIN);
Relay Relay4(RL4_PIN);
Relay RelayArray[] = { Relay1, Relay2, Relay3, Relay4 };

uint16_t TimeInMinutes = 0;
uint8_t Counter = 0;

int Mode = 0;
unsigned long LastMillis = 0;

void ICACHE_RAM_ATTR onTimerISR()
{
	timer1_write(625000);
	Counter++;
	if (Counter == 30)
	{
		Counter = 0;
		if (!isWifiConnected)
		{
			TimeInMinutes++;
			if (TimeInMinutes == 1440) //24h
			{
				TimeInMinutes = 0;
			}
		}
	}
}

void setup() {
	Init();
	timer1_attachInterrupt(onTimerISR);
	timer1_enable(TIM_DIV256, TIM_EDGE, TIM_SINGLE);
	timer1_write(625000); //2s
}

void loop() {
	SetWifiMode();
	ReadSerialCommand();
	if (isWifiConnected) {
		if (!client.connected()) {
			MQTTReconnect();
		}
		client.loop();
	}
	TaskExecute();
}

void Init()
{
	Serial.begin(115200);

	loadCredentials();
	if (ReconnectWifi()) {
		TimeClient.begin();
	}

	client.setServer(MQTT_SERVER, MQTT_PORT);
	client.setCallback(Callback);

	LastMillis = millis();
}

void UpdateRelayStatus()
{
	String RelayStt = "";
	for (int i = 0; i < 4; i++) 
	{
		RelayStt += String(RelayArray[i].stt);
	}
	Serial.println("RelayStt: " + RelayStt);
	PublishMqttMessage(RelayStt);
}

void loadCredentials() {
	EEPROM.begin(512);
	EEPROM.get(0, Ssid);
	EEPROM.get(0 + sizeof(Ssid), Password);
	char ok[2 + 1];
	EEPROM.get(0 + sizeof(Ssid) + sizeof(Password), ok);
	EEPROM.end();

	if (String(ok) != String("OK")) {
		Ssid[0] = 0;
		Password[0] = 0;
	}

	if (strlen(Ssid) > 0)
	{
	}
	else
	{
		Ssid[0] = 0;
		Password[0] = 0;
	}
}

void saveCredentials() {
	EEPROM.begin(512);
	EEPROM.put(0, Ssid);
	EEPROM.put(0 + sizeof(Ssid), Password);
	char ok[2 + 1] = "OK";
	EEPROM.put(0 + sizeof(Ssid) + sizeof(Password), ok);
	EEPROM.commit();
	EEPROM.end();
}

void clearCredentials() {
	char Ssid_d[32] = "";
	char Password_d[32] = "";
	EEPROM.begin(512);
	EEPROM.put(0, Ssid_d);
	EEPROM.put(0 + sizeof(Ssid_d), Password_d);
	char ok[2 + 1] = "";
	EEPROM.put(0 + sizeof(Ssid_d) + sizeof(Password_d), ok);
	EEPROM.commit();
	EEPROM.end();
	for (byte index = 0; index < 32; index++)
	{
		Ssid[index] = 0;
		Password[index] = 0;
	}
}

bool ReconnectWifi()
{
	WiFi.disconnect();
	WiFi.mode(WIFI_STA);
	WiFi.begin(Ssid, Password);
	return CheckWifiConnection();
}

bool CheckWifiConnection()
{
	if (WiFi.waitForConnectResult() != WL_CONNECTED) {
		isWifiConnected = false;
		return false;
	}
	else {
		isWifiConnected = true;
		return true;
	}
}

void ReadSerialCommand()
{
	while (Serial.available())
	{
		char inChar = (char)Serial.read();

		if (inChar == '\n') {
			StringComplete = true;
			break;
		}

		if (inChar != '\r' && inChar != '\n') {
			InputString += inChar;
		}
	}

	if (!StringComplete)
		return;

	if (isWifiConnected) {
		PublishMqttMessage(InputString);
	}

	if (InputString == "CRTM") {
		Serial.println("CurrentTime: " + TimeInMinutes);
		InputString = "";
		StringComplete = false;
		return;
	}
	String message = InputString.substring(0, 4);

	if (message == "SSID") {
		InputString.substring(5).toCharArray(Ssid, 32);
		wifiMode = 2;
		Serial.println("Ok");
	}
	else if (message == "PSWD") {
		InputString.substring(5).toCharArray(Password, 32);
		wifiMode = 4;
		Serial.println("Ok");
	}
	else if (message == "TIME") {
		TimeInMinutes = InputString.substring(5).toInt();
		Serial.print("CurrentTime: ");
		Serial.println(TimeInMinutes);
	}
	else if (message == "SAVEWIFI") {
		saveCredentials();
		Serial.println("Ok");
		ReconnectWifi();
	}

	InputString = "";
	StringComplete = false;
}

void MQTTReconnect()
{
	if (!client.connected()) {
		if (client.connect("ESP8266client", MQTT_USER, MQTT_PASSWORD)) {
			client.publish(MQTT_TOPIC_PUB, "ESP_reconnect");
			client.subscribe(MQTT_TOPIC_SUB);
		}
	}
}

void Callback(char* topic, byte* payload, unsigned int len)
{
	MqttMessage = "";
	for (int i = 0; i < len; i++)
	{
		MqttMessage += (char)payload[i];
	}

	Serial.println(MqttMessage);

	if (MqttMessage == "gStt") {
		UpdateRelayStatus();
	}

	if (MqttMessage.charAt(0) != 'R')
	{
		Serial.println("Unknown Command!");
		return;
	}

	int ind = MqttMessage.substring(1, 2).toInt();
	char stt = MqttMessage.charAt(3);
	uint16_t timeOn, timeOff;
	if (stt == '1')
	{
		RelayArray[ind].On();
	}
	else if (stt == '0')
	{
		RelayArray[ind].Off();
	}
	else if (stt == 'S')
	{
		timeOn = MqttMessage.substring(4, MqttMessage.indexOf(",")).toInt();
		timeOff = MqttMessage.substring(MqttMessage.indexOf(",") + 1).toInt();

		RelayArray[ind].SetTime(timeOn, timeOff);
	}

}

void PublishMqttMessage(String msg)
{
	client.publish(MQTT_TOPIC_PUB, msg.c_str());
}

void TaskExecute()
{
	if (isWifiConnected) {
		TimeClient.update();
		TimeInMinutes = TimeClient.getHours() * 60 + TimeClient.getMinutes();
	}

	for (int i = 0; i < 4; i++)
	{
		if (RelayArray[i].onTime == RelayArray[i].offTime)
			continue;

		if (TimeInMinutes == RelayArray[i].onTime && !isSendCommand) {
			isSendCommand = true;
			RelayArray[i].On();
			PublishMqttMessage("R" + String(i) + " 1");
		}
		else if (TimeInMinutes == RelayArray[i].offTime && !isSendCommand) {
			isSendCommand = true;
			RelayArray[i].Off();
			PublishMqttMessage("R" + String(i) + " 0");
		}
		else {
			isSendCommand = false;
		}
	}
}

bool WaitMillis()
{
	if (millis() - LastMillis > TIMER_DELAY) {
		LastMillis = millis();
		return true;
	}
	else {
		return false;
	}
}

void SetWifiMode()
{
	switch (wifiMode)
	{
	case 0:
		if (WaitMillis()) {
			if (CheckWifiConnection()) {
				Serial.println(WiFi.localIP());

				TimeClient.begin();
				MQTTReconnect();
			}
			else {
				Serial.println("SSID:?");
			}
			wifiMode = 1;
		}
		break;
	case 2:
		Serial.println("PSWD:?");
		wifiMode = 3;
		break;
	case 4:
		if (strlen(Ssid) > 0) {
			ReconnectWifi();
			wifiMode = 5;
		}
		break;
	case 5:
		if (WaitMillis()) {
			if (CheckWifiConnection()) {
				Serial.println("Wifi Connected!");
				Serial.println(WiFi.localIP());
				wifiMode = 6;
				saveCredentials();
				delay(10);
			}
		}
		break;
	case 6:
		if (isWifiConnected) {
			TimeClient.begin();
			MQTTReconnect();
			wifiMode = 7;
		}
		break;
	case 7:
		if (!CheckWifiConnection()) {
			if (ReconnectWifi()) {
				Serial.println("Wifi Connected!");
				TimeClient.begin();
				MQTTReconnect();
			}
		}
		break;

	default:
		break;
	}
}

