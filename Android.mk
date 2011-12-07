TOP_LOCAL_PATH:= $(call my-dir)

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_PACKAGE_NAME := DroidSSHd

LOCAL_MODULE_TAGS := optional

LOCAL_CERTIFICATE := platform

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_JAVACFLAGS += -Xlint:unchecked

LOCAL_JNI_SHARED_LIBRARIES := libNativeSSHd
LOCAL_REQUIRED_MODULES := libNativeSSHd

LOCAL_REQUIRED_MODULES += dropbear dropbearkey sftp-server scp ssh

include $(BUILD_PACKAGE)

# Use the folloing include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))
