cmake_minimum_required(VERSION 3.20)

project(CSProAndroid)

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Emscripten-specific settings
if(EMSCRIPTEN)
    message(STATUS "Building for WebAssembly with Emscripten")
    
    # Define WASM and UNICODE
    add_compile_definitions(WASM)
    add_compile_definitions(UNICODE=1)
    add_compile_definitions(_UNICODE=1)
    add_compile_definitions(GENERATE_BINARY=1)
    add_compile_definitions(USE_BINARY=1)
    
    # Use native WASM exceptions (better performance)
    add_compile_options(-fwasm-exceptions)
    
else()
    message(FATAL_ERROR "This CMakeLists.txt is for Emscripten builds only. Use Android.mk for Android builds.")
endif()

# Set paths
set(CSPRO_ROOT ${CMAKE_CURRENT_SOURCE_DIR}/../../../../..)
set(EXTERNAL_DIR ${CSPRO_ROOT}/external)
set(JNI_SRC ${CMAKE_CURRENT_SOURCE_DIR}/src)

# Include directories
include_directories(
    ${CSPRO_ROOT}
    ${EXTERNAL_DIR}
    ${EXTERNAL_DIR}/easylogging
    ${EXTERNAL_DIR}/rxcpp
    ${EXTERNAL_DIR}/geometry.hpp/include
    ${EXTERNAL_DIR}/variant/include
)

# Add subdirectories for all the z* modules
add_subdirectory(${CSPRO_ROOT}/rtf2html_dll ${CMAKE_BINARY_DIR}/rtf2html_dll)
add_subdirectory(${CSPRO_ROOT}/SQLite ${CMAKE_BINARY_DIR}/SQLite)
add_subdirectory(${CSPRO_ROOT}/yaml-cpp ${CMAKE_BINARY_DIR}/yaml-cpp)
add_subdirectory(${CSPRO_ROOT}/WASM/zlib ${CMAKE_BINARY_DIR}/zlib)
add_subdirectory(${CSPRO_ROOT}/engine ${CMAKE_BINARY_DIR}/engine)
add_subdirectory(${CSPRO_ROOT}/zAction ${CMAKE_BINARY_DIR}/zAction)
add_subdirectory(${CSPRO_ROOT}/zAppO ${CMAKE_BINARY_DIR}/zAppO)
add_subdirectory(${CSPRO_ROOT}/ZBRIDGEO ${CMAKE_BINARY_DIR}/zBridgeO)
add_subdirectory(${CSPRO_ROOT}/zCaseO ${CMAKE_BINARY_DIR}/zCaseO)
add_subdirectory(${CSPRO_ROOT}/zConcatO ${CMAKE_BINARY_DIR}/zConcatO)
add_subdirectory(${CSPRO_ROOT}/zDataO ${CMAKE_BINARY_DIR}/zDataO)
add_subdirectory(${CSPRO_ROOT}/zDictO ${CMAKE_BINARY_DIR}/zDictO)
add_subdirectory(${CSPRO_ROOT}/zDiffO ${CMAKE_BINARY_DIR}/zDiffO)
add_subdirectory(${CSPRO_ROOT}/zEngineF ${CMAKE_BINARY_DIR}/zEngineF)
add_subdirectory(${CSPRO_ROOT}/zEngineO ${CMAKE_BINARY_DIR}/zEngineO)
add_subdirectory(${CSPRO_ROOT}/zExcelO ${CMAKE_BINARY_DIR}/zExcelO)
add_subdirectory(${CSPRO_ROOT}/zExportO ${CMAKE_BINARY_DIR}/zExportO)
add_subdirectory(${CSPRO_ROOT}/zFormatterO ${CMAKE_BINARY_DIR}/zFormatterO)
add_subdirectory(${CSPRO_ROOT}/zFormO ${CMAKE_BINARY_DIR}/zFormO)
add_subdirectory(${CSPRO_ROOT}/zFreqO ${CMAKE_BINARY_DIR}/zFreqO)
add_subdirectory(${CSPRO_ROOT}/zHtml ${CMAKE_BINARY_DIR}/zHtml)
add_subdirectory(${CSPRO_ROOT}/zIndexO ${CMAKE_BINARY_DIR}/zIndexO)
add_subdirectory(${CSPRO_ROOT}/zJavaScript ${CMAKE_BINARY_DIR}/zJavaScript)
add_subdirectory(${CSPRO_ROOT}/zJson ${CMAKE_BINARY_DIR}/zJson)
add_subdirectory(${CSPRO_ROOT}/zListingO ${CMAKE_BINARY_DIR}/zListingO)
add_subdirectory(${CSPRO_ROOT}/zLogicO ${CMAKE_BINARY_DIR}/zLogicO)
add_subdirectory(${CSPRO_ROOT}/zMapping ${CMAKE_BINARY_DIR}/zMapping)
add_subdirectory(${CSPRO_ROOT}/zMessageO ${CMAKE_BINARY_DIR}/zMessageO)
add_subdirectory(${CSPRO_ROOT}/zMultimediaO ${CMAKE_BINARY_DIR}/zMultimediaO)
add_subdirectory(${CSPRO_ROOT}/zNetwork ${CMAKE_BINARY_DIR}/zNetwork)
add_subdirectory(${CSPRO_ROOT}/zPackO ${CMAKE_BINARY_DIR}/zPackO)
add_subdirectory(${CSPRO_ROOT}/zParadataO ${CMAKE_BINARY_DIR}/zParadataO)
add_subdirectory(${CSPRO_ROOT}/zPlatformO ${CMAKE_BINARY_DIR}/zPlatformO)
add_subdirectory(${CSPRO_ROOT}/zReformatO ${CMAKE_BINARY_DIR}/zReformatO)
add_subdirectory(${CSPRO_ROOT}/zReportO ${CMAKE_BINARY_DIR}/zReportO)
add_subdirectory(${CSPRO_ROOT}/zSortO ${CMAKE_BINARY_DIR}/zSortO)
add_subdirectory(${CSPRO_ROOT}/zSyncO ${CMAKE_BINARY_DIR}/zSyncO)
add_subdirectory(${CSPRO_ROOT}/zToolsO ${CMAKE_BINARY_DIR}/zToolsO)
add_subdirectory(${CSPRO_ROOT}/zUtilF ${CMAKE_BINARY_DIR}/zUtilF)
add_subdirectory(${CSPRO_ROOT}/zUtilO ${CMAKE_BINARY_DIR}/zUtilO)
add_subdirectory(${CSPRO_ROOT}/zZipO ${CMAKE_BINARY_DIR}/zZipO)

# Add zEntryO for CoreEntryEngineInterface
include_directories(${CSPRO_ROOT}/Zentryo)

# Source files for CSProAndroid (Web version)
# Uses WebEngineInterface which has no JNI dependencies
set(CSENTRY_SOURCES
    ${JNI_SRC}/WebEngineInterface.cpp
    ${JNI_SRC}/WebApplicationInterface.cpp
    ${JNI_SRC}/WebWASMBindings_full.cpp
)

# Excluded from WASM build (all require JNI):
# - ActionInvoker.cpp (uses JNI)
# - ActionInvokerPortableRunner.cpp (uses JNI)
# - AndroidEngineInterface.cpp (uses JNI and AndroidApplicationInterface)
# - PortableLocalhostAndroid.cpp (uses JNI)
# - AndroidApplicationInterface.cpp (uses JNI)
# - All gov_census_cspro_*_jni.cpp files
# - AndroidBluetoothAdapter.cpp (uses JNI)
# - AndroidHttpConnection.cpp (uses JNI)
# - AndroidMapUI.cpp (uses JNI)
# - AndroidFtpConnection.cpp (uses JNI)
# - AndroidBluetoothObexTransport.cpp (uses JNI)
# - JNIHelpers.cpp (not needed)
# - GeometryJni.cpp (uses JNI)

# Create executable
add_executable(${PROJECT_NAME} ${CSENTRY_SOURCES})

# Emscripten link flags
target_link_options(${PROJECT_NAME} PUBLIC
    -fwasm-exceptions                    # Native WASM exceptions
    -sALLOW_MEMORY_GROWTH                # Allow memory to grow
    --bind                               # Embind for JS bindings
    -sEXPORT_ES6=1                      # Export as ES6 module
    -sMODULARIZE=1                      # Modularize output
    -sEXPORT_NAME=createCSProModule     # Module factory name
    -sENVIRONMENT=web                   # Browser-only (no Node.js)
    -sASYNCIFY=2                        # JSPI for async operations
    -sJSPI                              # JavaScript Promise Integration
    -sEXPORTED_RUNTIME_METHODS=['FS','ccall','cwrap']  # Export FS
    "SHELL:-O2"                         # Optimization level
)

# Bundle assets if they exist
if(EXISTS ${CMAKE_CURRENT_SOURCE_DIR}/../../assets)
    target_link_options(${PROJECT_NAME} PUBLIC
        --preload-file ${CMAKE_CURRENT_SOURCE_DIR}/../../assets@Assets/
    )
endif()

# Link libraries
target_link_libraries(${PROJECT_NAME}
    rtf2html_dll
    -Wl,--whole-archive SQLite -Wl,--no-whole-archive
    yaml-cpp
    zlib
    engine
    zAction
    zAppO
    zBridgeO
    zCaseO
    zConcatO
    zDataO
    zDictO
    zDiffO
    zEngineF
    zEngineO
    zExcelO
    zExportO
    zFormatterO
    zFormO
    zFreqO
    zHtml
    zIndexO
    -Wl,--whole-archive zJavaScript -Wl,--no-whole-archive
    zJson
    zListingO
    zLogicO
    zMapping
    zMessageO
    zMultimediaO
    zNetwork
    zPackO
    zParadataO
    zPlatformO
    zReformatO
    zReportO
    zSortO
    zSyncO
    zToolsO
    zUtilF
    zUtilO
    zZipO
)

# Install outputs
install(TARGETS ${PROJECT_NAME}
    RUNTIME DESTINATION ${CMAKE_CURRENT_SOURCE_DIR}/../../../../../build/wasm
)

install(FILES
    ${CMAKE_BINARY_DIR}/CSProAndroid.js
    ${CMAKE_BINARY_DIR}/CSProAndroid.wasm
    DESTINATION ${CMAKE_CURRENT_SOURCE_DIR}/../../../../../build/wasm
    OPTIONAL
)
