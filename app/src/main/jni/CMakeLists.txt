# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html
PROJECT(native-lib C)

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

#add_subdirectory(
#        mbedtls
#)

add_library(
        native-lib SHARED
        native-lib.c
)
#target_include_directories(native-lib PRIVATE
#        #${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/include
#        C:/Users/Administrator/Desktop/Resources/SourceCode/SecretGarden/app/src/main/jni/mbedtls/include
#        )
target_link_libraries( # Specifies the target library.
        native-lib
#        mbedtls
        log)
