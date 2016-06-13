/* This file has been autogenerated by Ivory
 * Compiler version  0.1.0.3
 */
#ifndef __USER_INPUT_TYPES_H__
#define __USER_INPUT_TYPES_H__
#ifdef __cplusplus
extern "C" {
#endif
#include "ivory.h"
#include "ivory_serialize.h"
typedef struct user_input { float throttle;
                            float roll;
                            float pitch;
                            float yaw;
} user_input;
void user_input_get_le(const uint8_t *n_var0, uint32_t n_var1, struct user_input *n_var2);
void user_input_get_be(const uint8_t *n_var0, uint32_t n_var1, struct user_input *n_var2);
void user_input_set_le(uint8_t *n_var0, uint32_t n_var1, const struct user_input *n_var2);
void user_input_set_be(uint8_t *n_var0, uint32_t n_var1, const struct user_input *n_var2);

#ifdef __cplusplus
}
#endif
#endif /* __USER_INPUT_TYPES_H__ */