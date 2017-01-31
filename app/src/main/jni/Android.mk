LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := jsoncpp
LOCAL_CPPFLAGS := -fexceptions
LOCAL_SRC_FILES := jsoncpp.cpp
LOCAL_C_INCLUDES := $(LOCAL_PATH)/json
LOCAL_LDLIBS := -L$(call host-path, $(LOCAL_PATH)/../../libs/armeabi) -L$(call host-path, $(LOCAL_PATH)/../../libs/armeabi-v7a)
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
OpenCV_INSTALL_MODULES := on
OpenCV_CAMERA_MODULES := off
OPENCV_LIB_TYPE :=STATIC
ifeq ("$(wildcard $(OPENCV_MK_PATH))","")
include D:\Simon\Works\Android\Git\AverageFace\native\jni\OpenCV.mk
else
include $(OPENCV_MK_PATH)
endif

LOCAL_MODULE := JNI_APP
LOCAL_SRC_FILES =: jni_app.cpp \
                   md5.cpp
LOCAL_LDLIBS +=  -lm -llog

# include jsoncpp
LOCAL_C_INCLUDES += $(LOCAL_PATH)/json
LOCAL_SHARED_LIBRARIES := jsoncpp
include $(BUILD_SHARED_LIBRARY)