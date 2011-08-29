LOCAL_PATH := $(call my-dir)
ifeq ($(LOCAL_JNI_SHARED_LIBRARIES),libNativeSSHd)
include $(CLEAR_VARS)

LOCAL_MODULE := libNativeSSHd
LOCAL_MODULE_TAGS := eng

LOCAL_SHARED_LIBRARIES := libc
LOCAL_SHARED_LIBRARIES += libcutils

#LOCAL_LDLIBS += -llog

# No static libraries.
LOCAL_STATIC_LIBRARIES :=

LOCAL_C_INCLUDES := $(ANDROID_BUILD_TOP)/system/core/include

# Also need the JNI headers.
LOCAL_C_INCLUDES += $(JNI_H_INCLUDE)

LOCAL_SRC_FILES := NativeSSHd.c

# Don't prelink this library.  For more efficient code, you may want
# to add this library to the prelink map and set this to true. However,
# it's difficult to do this for applications that are not supplied as
# part of a system image.

LOCAL_PRELINK_MODULE := false

include $(BUILD_SHARED_LIBRARY)
endif
