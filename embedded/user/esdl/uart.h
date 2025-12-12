#ifndef ESDL_UART_H
#define ESDL_UART_H

#include <stdint.h> // uint8_t 사용을 위해 추가

void USART1_Init(void);
void USART2_init(void);
void USART1_SendString(const char *s);
void NVIC_Configure(void);


// [추가] 게임 로직에서 사용할 수신 함수
int UART1_Available(void);
uint8_t UART1_Read(void);

#endif
