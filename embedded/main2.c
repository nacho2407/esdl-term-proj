#include "stm32f10x.h"
#include "motor.h"
#include "trigger.h"

void Init(void);

void Init(void)
{
    SystemInit();

    // SysTick 1ms 설정 → millis() 사용 가능
    SysTick_Config(SystemCoreClock / 1000);

    Motor_Init();
    Trigger_Init();
    Trigger_Init();
}

int main(void)
{
    Init();

    while (1)
    {

    }
}
