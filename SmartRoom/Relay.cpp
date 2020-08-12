// 
// 
// 

#include "Relay.h"

Relay::Relay(uint8_t _pin)
{
	this->pin = _pin;
	pinMode(_pin, OUTPUT);
	digitalWrite(_pin, OFF);
}

bool Relay::isOn()
{
	return this->stt;
}

void Relay::On()
{
	digitalWrite(this->pin, ON);
	this->stt = true;
}

void Relay::Off()
{
	digitalWrite(this->pin, OFF);
	this->stt = false;
}

void Relay::SetTime(uint16_t _on, uint16_t _off)
{
	this->onTime = _on;
	this->offTime = _off;
}
