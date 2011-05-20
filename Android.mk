TOP_LOCAL_PATH:= $(call my-dir)

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := eng optional

LOCAL_SRC_FILES := $(call all-java-files-under, src)

# LOCAL_STATIC_JAVA_LIBRARIES := br.com.bott.droidsshd

LOCAL_PACKAGE_NAME := DroidSSHd

# LOCAL_SDK_VERSION := current

ifeq ($(TARGET_ARCH),arm)

LOCAL_JNI_SHARED_LIBRARIES := libNativeTask
LOCAL_REQUIRED_MODULES := libNativeTask

endif

include $(BUILD_PACKAGE)

# Use the folloing include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))
