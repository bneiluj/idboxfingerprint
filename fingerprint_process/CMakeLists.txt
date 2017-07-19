
cmake_minimum_required(VERSION 3.6)
project( fingerprint_process  CXX )

SET(OpenCV_DIR ~/Projects/libs/OpenCV-android-sdk/sdk/native/jni)

find_package( OpenCV REQUIRED  )
add_executable( fingerprint_process fingerprint_process.cpp  )
target_link_libraries( fingerprint_process ${OpenCV_LIBS}  )
