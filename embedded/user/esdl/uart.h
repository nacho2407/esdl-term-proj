#ifndef ESDL_UART_H
#define ESDL_UART_H

void USART1_Init(void);
void USART2_init(void);
void USART1_SendString(const char *s);
void NVIC_Configure(void);

#endif
