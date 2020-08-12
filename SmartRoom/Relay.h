// Relay.h

#ifndef _RELAY_h
#define _RELAY_h

#if defined(ARDUINO) && ARDUINO >= 100
	#include "arduino.h"
#else
	#include "WProgram.h"
#endif

#define ON LOW
#define OFF HIGH
class Relay
{
public:
	Relay(uint8_t _pin);
	bool isOn();
	void On();
	void Off();
	void SetTime(uint16_t _on, uint16_t _off);

	uint8_t pin;
	bool stt = false;
	uint16_t onTime = 0;
	uint16_t offTime = 0;
};
#endif

