/**************************************************************************
Copyright (c) 2013, Rockwell Collins and the University of Minnesota.
Developed with the sponsorship of the Defense Advanced Research Projects Agency (DARPA).

Permission is hereby granted, free of charge, to any person obtaining a copy of this data,
including any software or models in source or binary form, as well as any drawings, specifications, 
and documentation (collectively "the Data"), to deal in the Data without restriction, including 
without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, 
and/or sell copies of the Data, and to permit persons to whom the Data is furnished to do so, 
subject to the following conditions: 

The above copyright notice and this permission notice shall be included in all copies or 
substantial portions of the Data.

THE DATA IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT 
LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
IN NO EVENT SHALL THE AUTHORS, SPONSORS, DEVELOPERS, CONTRIBUTORS, OR COPYRIGHT HOLDERS BE LIABLE 
FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, 
ARISING FROM, OUT OF OR IN CONNECTION WITH THE DATA OR THE USE OR OTHER DEALINGS IN THE DATA. 
 **************************************************************************/


/**************************************************************************
   File: C:\docs\svn_smaccm\architecture\Examples\event-data-port-test-2\gen\Test_Impl_Instance.c
   Created on: 2013/10/22 13:15:51
   using Dulcimer AADL system build tool suite 

   ***AUTOGENERATED CODE: DO NOT MODIFY***
 **************************************************************************/


/**************************************************************************
   This .c file contains the implementations of the communication functions for threads 
   and the top level entrypoint functions for the AADL components as defined in the 
   system implementation Test_Impl_Instance.
 **************************************************************************/


#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>
#include "rtos-kochab.h"
#include "Test_Impl_Instance.h"

void *memcpy(void *dst, const void *src, int count) { 
   uint32_t i;
   uint8_t *src_ptr = (uint8_t *)src;
   uint8_t *dst_ptr = (uint8_t *)dst;
   for (i=0; i < count; i++) { 
      *dst_ptr = *src_ptr;
      dst_ptr++; src_ptr++;
   }
   return dst;
}

/**************************************************************************
Initialize the systick signal based on the GCD of the thread periods
 **************************************************************************/
#define SYST_CSR_REG 0xE000E010         // Basic control of SysTick: enable, clock source, interrupt, or poll
#define SYST_RVR_REG 0xE000E014         // Value to load Current Value register when 0 is reached
#define SYST_CVR_REG 0xE000E018         // The current value of the count down
#define SYST_CAV_REG 0xE000E01C         // Calibration value for count down.

#define SYST_CSR_READ() (*((volatile uint32_t*)SYST_CSR_REG))
#define SYST_CSR_WRITE(x) (*((volatile uint32_t*)SYST_CSR_REG) = x)

#define SYST_RVR_READ() (*((volatile uint32_t*)SYST_RVR_REG))
#define SYST_RVR_WRITE(x) (*((volatile uint32_t*)SYST_RVR_REG) = x)

#define SYST_CVR_READ() (*((volatile uint32_t*)SYST_CVR_REG))
#define SYST_CVR_WRITE(x) (*((volatile uint32_t*)SYST_CVR_REG) = x)
#define SYST_CAV_READ() (*((volatile uint32_t*)SYST_CAV_REG))

void smaccm_initialize_px4_systick_interrupt() {
   /* The SysTick Calibration Value Register is a read-only register that contains
   the number of pulses for a period of 10ms, in the TENMS field, bits[23:0].
   This register also has a SKEW bit. Bit[30] == 1 indicates that the calibration
   for 10ms in the TENMS section is not exactly 10ms due to clock frequency.
   Bit[31] == 1 indicates that the reference clock is not provided.*/

   uint32_t cav_value = SYST_CAV_READ();
   uint32_t gcd_value = 250;                        // SMACCM thread rate GCD in milliseconds 
   uint32_t ten_ms_val = cav_value & 0x00ffffff ;   // number of cycles per 10ms
   uint32_t one_ms_val = cav_value / 10;            // number of cycles per 1ms

   uint32_t mult_of_ten_ms = gcd_value / 10;
   uint32_t remainder_of_ten_ms = gcd_value % 10;

   uint32_t desired_rate = (mult_of_ten_ms * ten_ms_val) + (remainder_of_ten_ms * one_ms_val) ; 
   SYST_RVR_WRITE(desired_rate);
   SYST_CVR_WRITE(0);
   SYST_CSR_WRITE((1 << 1) | 1);
};



/* Shared variables for port-to-port communication. */

/**************************************************************************
Shared data for thread instance port: signalCh_Instance_1
 **************************************************************************/
#define QS_SIGNALCH_INSTANCE_1 3
test__dt_rec var_signalCh_Instance_1 [4]; 
uint32_t var_signalCh_Instance_1_front ; 
uint32_t var_signalCh_Instance_1_back ; 

bool var_signalCh_Instance_1_is_full() {
   int mod_value = (var_signalCh_Instance_1_back + 1) % 4;
   if (mod_value == var_signalCh_Instance_1_front) {
      return true;
   } else {
      return false;
   }
}

bool var_signalCh_Instance_1_is_empty() {
   return (var_signalCh_Instance_1_back == var_signalCh_Instance_1_front);
}

bool var_signalCh_Instance_1_enqueue(const test__dt_rec * elem ) {
   if (var_signalCh_Instance_1_is_full()) {
      return false;
   } else {
      memcpy(&var_signalCh_Instance_1[var_signalCh_Instance_1_back], elem, sizeof(test__dt_rec  ));
      var_signalCh_Instance_1_back = (var_signalCh_Instance_1_back + 1) % 4;
      return true;
   }
}

bool var_signalCh_Instance_1_dequeue(test__dt_rec * elem ) {
   if (var_signalCh_Instance_1_is_empty()) {
      return false;
   } else {
      memcpy(elem, &var_signalCh_Instance_1[var_signalCh_Instance_1_front], sizeof(test__dt_rec  ));
      var_signalCh_Instance_1_front = (var_signalCh_Instance_1_front + 1) % 4;
      return true;
   }
}



/**************************************************************************
ISR Function for managing periodic thread dispatch.
 **************************************************************************/
int smaccm_calendar_counter = 0;

void smaccm_thread_calendar() {
   smaccm_calendar_counter = (smaccm_calendar_counter + 1) % 2;
   if ((smaccm_calendar_counter % 2) == 0) { 
      rtos_irq_event_raise(IRQ_EVENT_ID_SMACCM_PERIODIC_DISPATCH_THREAD_A);
   }
   if ((smaccm_calendar_counter % 1) == 0) { 
      rtos_irq_event_raise(IRQ_EVENT_ID_SMACCM_PERIODIC_DISPATCH_THREAD_B);
   }
}

/**************************************************************************
End dispatch.
 **************************************************************************/
bool thread_b_read_signalCh(/* THREAD_ID tid,  */ test__dt_rec * elem ) {

   bool result = true;

    /////////////////////////////////////////////////////////////////////////
    // here is where we would branch based on thread instance id.
    // For now we support only one thread instance per thread implementation.
    // In this case we would split on destination thread id: thread_b_instance_1
    /////////////////////////////////////////////////////////////////////////


   rtos_mutex_lock(MUTEX_ID_MUTEX_SIGNALCH_INSTANCE_1);
   result = var_signalCh_Instance_1_dequeue(elem);
   rtos_mutex_unlock(MUTEX_ID_MUTEX_SIGNALCH_INSTANCE_1);
   return result;
}

bool thread_b_is_empty_signalCh(/* THREAD_ID tid,  */ ) {

   bool result = false;

    /////////////////////////////////////////////////////////////////////////
    // here is where we would branch based on thread instance id.
    // For now we support only one thread instance per thread implementation.
    // In this case we would split on destination thread id: thread_b_instance_1
    /////////////////////////////////////////////////////////////////////////


   rtos_mutex_lock(MUTEX_ID_MUTEX_SIGNALCH_INSTANCE_1);
   result = var_signalCh_Instance_1_is_empty();
   rtos_mutex_unlock(MUTEX_ID_MUTEX_SIGNALCH_INSTANCE_1);
   return result;
}


 /* Writer functions for port-to-port communication.*/

bool thread_a_write_foo_event_data(/* THREAD_ID tid,  */ const test__dt_rec * elem ) {

   bool result = true;

    /////////////////////////////////////////////////////////////////////////
    // here is where we would branch based on thread instance id.
    // For now we support only one thread instance per thread implementation.
    // In this case we would split on destination thread id: thread_a_instance_0
    /////////////////////////////////////////////////////////////////////////


   rtos_mutex_lock(MUTEX_ID_MUTEX_SIGNALCH_INSTANCE_1);
   result = var_signalCh_Instance_1_enqueue(elem);
   rtos_signal_send_set(TASK_ID_THREAD_B_INSTANCE_1, 2/* signalCh */); 
   rtos_mutex_unlock(MUTEX_ID_MUTEX_SIGNALCH_INSTANCE_1);
   return result;
}

void Thread_A(int threadID) 
{
   init_A(/* threadID */);

   for (;;) {
      int current_sig = rtos_signal_wait_set(1/*periodic_dispatcher (500 ms)*/); 
      if (current_sig == 0/*periodic_dispatcher (500 ms)*/) {
         exec_periodic_thread_A(/*threadID*/);
      }
   }
}

void Thread_B(int threadID) 
{
   init_B(/* threadID */);

   for (;;) {
      int current_sig = rtos_signal_wait_set(1/*periodic_dispatcher (250 ms)*/ | 2/*signalCh*/); 
      if (current_sig == 0/*periodic_dispatcher (250 ms)*/) {
         exec_periodic_thread_B(/*threadID*/);
      }
      else if (current_sig == 1/*signalCh*/) {
         while (! thread_b_is_empty_signalCh()) {
            test__dt_rec elem ;
            thread_b_read_signalCh(&elem);
            exec_signalCh_thread_B(/*threadID, */ &elem);
         }
      }
   }
}

void Thread_A_Instance_0()
{
   Thread_A(TASK_ID_THREAD_A_INSTANCE_0);
}

void Thread_B_Instance_1()
{
   Thread_B(TASK_ID_THREAD_B_INSTANCE_1);
}

/**************************************************************************
   End of autogenerated file: C:\docs\svn_smaccm\architecture\Examples\event-data-port-test-2\gen\Test_Impl_Instance.c
 **************************************************************************/
