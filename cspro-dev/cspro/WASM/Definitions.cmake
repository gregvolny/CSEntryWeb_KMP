
# Only set CSPRO_DIRECTORY if not already set (parent CMakeLists may have set it)
if(NOT DEFINED CSPRO_DIRECTORY)
    get_filename_component(CSPRO_DIRECTORY ${CMAKE_CURRENT_LIST_DIR}/.. ABSOLUTE)
endif()

if(NOT DEFINED EXTERNAL_DIRECTORY)
    set(EXTERNAL_DIRECTORY ${CSPRO_DIRECTORY}/external)
endif()

# Include directories - only add if not already added by parent
# Note: CMAKE_CURRENT_SOURCE_DIR will be the calling project's directory
if(NOT WASM_INCLUDES_ADDED)
    set(WASM_INCLUDES_ADDED TRUE CACHE INTERNAL "Includes already added")
    include_directories(
        ${CSPRO_DIRECTORY}
        ${EXTERNAL_DIRECTORY}
        ${EXTERNAL_DIRECTORY}/jsoncons
    )
endif()

# Add project's own source directory for stdafx.h resolution
include_directories(${CMAKE_CURRENT_SOURCE_DIR})

# Only add compile definitions if not already added
if(NOT WASM_DEFINITIONS_ADDED)
    set(WASM_DEFINITIONS_ADDED TRUE CACHE INTERNAL "Definitions already added")
    add_compile_definitions(WASM)
    add_compile_definitions(UNICODE=1)
    add_compile_definitions(_UNICODE=1)
    add_compile_definitions(GENERATE_BINARY=1)
    add_compile_definitions(USE_BINARY=1)
    
    # Use native WASM exceptions for better performance
    # This is compatible with JSPI (unlike Asyncify which required JS exceptions)
    add_compile_options(-fwasm-exceptions)
endif()
