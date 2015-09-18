LOCAL_PATH := $(call my-dir)  
  
include $(CLEAR_VARS)  
LOCAL_MODULE := MainActivity   
LOCAL_SRC_FILES := LEDControl.c  
LOCAL_LDLIBS := -llog  
LOCAL_C_INCLUDES := $(MY_ANDROID_SOURCE)/frameworks/base/core/jni/android/graphics \  
include $(BUILD_SHARED_LIBRARY)