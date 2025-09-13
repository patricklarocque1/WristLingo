# Minimum CMake version compatible with recent AGP
cmake_minimum_required(VERSION 3.22.1)

project(wristlingo_native)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# NDK-specific toggles for 16KB support (default OFF on r29+)
if (ANDROID)
    if (NOT DEFINED NEED_16K_LINKER_FLAGS)
        set(NEED_16K_LINKER_FLAGS OFF)  # Test with OFF; keep helper for legacy toolchains
    endif()
endif()

# --- 16KB page-size helper ---
function(apply_16K_page_size target)
    if (ANDROID AND NEED_16K_LINKER_FLAGS)
        target_link_options(${target} PRIVATE "-Wl,-z,max-page-size=16384")
    endif()
endfunction()

# Ensure 16 KB page-size compatibility on Android (PT_LOAD p_align = 16384)
if (ANDROID AND NEED_16K_LINKER_FLAGS)
    # Optional global application if explicitly enabled
    add_link_options("-Wl,-z,max-page-size=16384")
    set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,-z,max-page-size=16384")
    set(CMAKE_MODULE_LINKER_FLAGS "${CMAKE_MODULE_LINKER_FLAGS} -Wl,-z,max-page-size=16384")
    set(CMAKE_EXE_LINKER_FLAGS    "${CMAKE_EXE_LINKER_FLAGS} -Wl,-z,max-page-size=16384")
endif()

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

if (ANDROID)
    apply_16K_page_size(wristlingo_native)
endif()


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

if (ANDROID)
    apply_16K_page_size(whisper_bridge)
    foreach(_t IN ITEMS whisper ggml ggml-base)
        if (TARGET ${_t})
            apply_16K_page_size(${_t})
        endif()
    endforeach()
endif()

