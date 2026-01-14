project(zJson)

include(../WASM/Definitions.cmake)

# Include directories for this target - external directory needed for <external/jsoncons/...> includes
include_directories(
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CSPRO_DIRECTORY}
    ${EXTERNAL_DIRECTORY}
    ${EXTERNAL_DIRECTORY}/jsoncons
)

add_library(${PROJECT_NAME} 
	Json.cpp
	JsonFormattingOptions.cpp
	JsonNode.cpp
	JsonObjectCreator.cpp
	JsonSpecFile.cpp
	JsonStream.cpp
)
