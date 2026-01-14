project(zAppO)

include(../WASM/Definitions.cmake)

# Include this project's directory first so its stdafx.h is found
include_directories(
    ${CMAKE_CURRENT_SOURCE_DIR}
)

add_library(${PROJECT_NAME}
    AppFileType.cpp
    Application.cpp
    AppSyncParameters.cpp
    CodeFile.cpp
    DictionaryDescription.cpp
    EnumJsonSerializers.cpp
    LabelSet.cpp
    Language.cpp
    LogicSettings.cpp
    MappingDefines.cpp
    OccurrenceLabels.cpp
    PFF.cpp
    Pre80SpecFile.cpp

    Properties/ApplicationProperties.cpp
    Properties/JsonProperties.cpp
    Properties/MappingProperties.cpp
    Properties/MappingTileProviderProperties.cpp
    Properties/ParadataProperties.cpp
)
