#include <p18f2520.h>
#include <delays.h>
#include <timers.h>
#include <pwm.h>

//#pragma config OSC = HS	// Oscillator Selection:HS, 4mHz external oscillator required, see data sheet
#pragma config OSC = INTIO67
#pragma config WDT = OFF	// Watchdog Timer, must be set off, or PIC will continuously reset
#pragma config LVP = OFF	// Low Voltage ICSP(PIN 6):Disabled

void main(void){
    OSCCON = 0b1000000;

    PORTCbits.RC2=0;
    TRISC=0;

    TRISA = 0b00000000; //Declares all of Port A to be output
    PORTA = 0b00000000; //Clears Port A
    TRISB = 0b00111100; //Every bit with a 1 corresponds to a Pin on Port B which is now an input pin
    PORTB = 0b00111100; //Every bit with a 1 corresponds to a Pin on Port B which now expects input
    ADCON1 = 0b00001111; //Sets input pins to be Digital input

    OpenTimer2(T2_PS_1_1 & T2_POST_1_1);
    OpenPWM1(0x9A); // Period
    OpenPWM2(0x9A);
	while(1){

            if(PORTBbits.RB2 == 0 && PORTBbits.RB3 == 0 && PORTBbits.RB4 == 0){
                //This is really reading tone "D"
                //STOP
                SetDCPWM1(310);
                SetDCPWM2(310);
            }
            else if(PORTBbits.RB2 == 1 && PORTBbits.RB3 == 0 && PORTBbits.RB4 == 0){
                //DTMF 1
                //FORWARD
                SetDCPWM1(25);
                SetDCPWM2(615);
            }
            else if(PORTBbits.RB2 == 0 && PORTBbits.RB3 == 1 && PORTBbits.RB4 == 0){
                //DTMF 2
                //REVERSE
                SetDCPWM1(615);
                SetDCPWM2(25);
            }
            else if(PORTBbits.RB2 == 1 && PORTBbits.RB3 == 1 && PORTBbits.RB4 == 0){
                //DTMF 3
                //COUNTER-CLOCKWISE
                SetDCPWM1(615);
                SetDCPWM2(615);
            }
            else if(PORTBbits.RB2 == 0 && PORTBbits.RB3 == 0 && PORTBbits.RB4 == 1){
                //DTMF 4
                //CLOCKWISE
                SetDCPWM1(25);
                SetDCPWM2(25);
            }
            else if(PORTBbits.RB2 == 1 && PORTBbits.RB3 == 0 && PORTBbits.RB4 == 1){
                //DTMF 5
                //POINT TURN LEFT
                SetDCPWM1(310);
                SetDCPWM2(615);
            }
            else if(PORTBbits.RB2 == 0 && PORTBbits.RB3 == 1 && PORTBbits.RB4 == 1){
                //DTMF 6
                //POINT TURN RIGHT
                SetDCPWM1(615);
                SetDCPWM2(310);
            }
	}
}