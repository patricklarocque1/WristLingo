# Minimum CMake version compatible with recent AGP
cmake_minimum_required(VERSION 3.22.1)

project(wristlingo_native)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(
    wristlingo_native
    SHARED
    native-lib.cpp
)

find_library(
    log-lib
    log
)

target_link_libraries(
    wristlingo_native
    ${log-lib}
)


# Whisper.cpp integration
add_subdirectory(whisper.cpp)

add_library(
    whisper_bridge
    SHARED
    whisper_bridge.cpp
)

target_include_directories(
    whisper_bridge
    PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/whisper.cpp
)

target_link_libraries(
    whisper_bridge
    PRIVATE
    whisper
    ${log-lib}
)

