# CMakeLists.txt for CSEntry KMP WASM Engine
# Self-contained build from local src/ folder
cmake_minimum_required(VERSION 3.13)

project(csentryKMP VERSION 1.0.0 LANGUAGES C CXX)

# ============================================================================
# Configuration
# ============================================================================

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_C_STANDARD 11)

# Source root (local copy)
set(SRC_ROOT ${CMAKE_CURRENT_SOURCE_DIR}/src)

message(STATUS "=======================================")
message(STATUS "Building CSEntry KMP WASM Engine")
message(STATUS "=======================================")
message(STATUS "Source root: ${SRC_ROOT}")

# ============================================================================
# Compile Definitions
# ============================================================================

add_compile_definitions(WASM)
add_compile_definitions(UNICODE=1)
add_compile_definitions(_UNICODE=1)
add_compile_definitions(GENERATE_BINARY=1)
add_compile_definitions(USE_BINARY=1)

# Use JS exceptions (not WASM exceptions) for Asyncify compatibility
# WASM exceptions (-fwasm-exceptions) are NOT compatible with Asyncify
# add_compile_options(-fwasm-exceptions)

# Enable C++ exception handling via JavaScript (required for try-catch to work)
# This must be set for BOTH compile and link options
add_compile_options(-sDISABLE_EXCEPTION_CATCHING=0)

# ============================================================================
# Include Directories (order matters!)
# ============================================================================

# Each library's own directory comes first (for stdafx.h)
# Then global includes
include_directories(
    ${SRC_ROOT}
    ${SRC_ROOT}/external
    ${SRC_ROOT}/external/jsoncons
    ${SRC_ROOT}/external/easylogging
    ${SRC_ROOT}/external/variant/include
    ${SRC_ROOT}/external/geometry.hpp/include
    ${SRC_ROOT}/external/rxcpp
    ${SRC_ROOT}/external/yaml-cpp/include
    ${SRC_ROOT}/external/libxlsxwriter/include
    ${SRC_ROOT}/external/mp4v2/include
    ${SRC_ROOT}/external/gumbo
    ${SRC_ROOT}/external/qrcodegen
    ${SRC_ROOT}/WASM
    ${SRC_ROOT}/jni/build-wasm
    ${SRC_ROOT}/jni/src
    # Library directories for stdafx.h resolution in subdirectories
    ${SRC_ROOT}/zEngineO
    ${SRC_ROOT}/zLogicO
    ${SRC_ROOT}/zToolsO
    ${SRC_ROOT}/zUtilO
    ${SRC_ROOT}/zAppO
    ${SRC_ROOT}/zFormO
    ${SRC_ROOT}/zDictO
    ${SRC_ROOT}/zDataO
    ${SRC_ROOT}/zCaseO
    ${SRC_ROOT}/zEntryO
    ${SRC_ROOT}/engine
)

# ============================================================================
# Collect Source Files
# ============================================================================

# Helper function to collect source files from a library
function(collect_sources RESULT_VAR LIB_DIR)
    file(GLOB_RECURSE _SOURCES 
        "${SRC_ROOT}/${LIB_DIR}/*.cpp"
        "${SRC_ROOT}/${LIB_DIR}/*.c"
    )
    # Exclude test files, Windows-specific files, MFC files
    list(FILTER _SOURCES EXCLUDE REGEX ".*[Tt]est.*\\.cpp$")
    list(FILTER _SOURCES EXCLUDE REGEX ".*_win\\.cpp$")
    list(FILTER _SOURCES EXCLUDE REGEX ".*_windows\\.cpp$")
    list(FILTER _SOURCES EXCLUDE REGEX ".*Mfc.*\\.cpp$")
    list(FILTER _SOURCES EXCLUDE REGEX ".*/Android.*\\.cpp$")
    set(${RESULT_VAR} ${_SOURCES} PARENT_SCOPE)
endfunction()

# External libraries
file(GLOB ZLIB_SOURCES "${SRC_ROOT}/external/zlib/*.c")
# Only include sqlite3.c, not sqlite3_original.c (they have duplicate symbols)
set(SQLITE_SOURCES "${SRC_ROOT}/external/SQLite/sqlite3.c")
file(GLOB_RECURSE YAML_SOURCES "${SRC_ROOT}/external/yaml-cpp/src/*.cpp")
# libxlsxwriter for Excel export
file(GLOB XLSXWRITER_SOURCES "${SRC_ROOT}/external/libxlsxwriter/src/*.c")
# easylogging++ for logging
set(EASYLOGGING_SOURCES "${SRC_ROOT}/external/easylogging/easylogging++.cc")

# rtf2html - use specific files (exclude Windows DLL wrapper)
set(RTF2HTML_SOURCES
    ${SRC_ROOT}/rtf2html_dll/fmt_opts.cpp
    ${SRC_ROOT}/rtf2html_dll/rtf_keyword.cpp
    ${SRC_ROOT}/rtf2html_dll/rtf_table.cpp
    ${SRC_ROOT}/rtf2html_dll/rtf2html.cpp
)

# SQLite wrapper
file(GLOB SQLITE_WRAPPER_SOURCES "${SRC_ROOT}/SQLite/*.cpp")

# Engine core - explicit list based on Android Engine.mk (excludes Windows-only files)
set(ENGINE_SOURCES
    ${SRC_ROOT}/engine/Applload.cpp
    ${SRC_ROOT}/engine/citer.cpp
    ${SRC_ROOT}/engine/Common.cpp
    ${SRC_ROOT}/engine/Decorr.cpp
    ${SRC_ROOT}/engine/engdrv.cpp
    ${SRC_ROOT}/engine/EngineExecutor.cpp
    ${SRC_ROOT}/engine/Exappl.cpp
    ${SRC_ROOT}/engine/Exapplva.cpp
    ${SRC_ROOT}/engine/Exopfile.cpp
    ${SRC_ROOT}/engine/Form.cpp
    ${SRC_ROOT}/engine/gpsfuncs.cpp
    ${SRC_ROOT}/engine/Int_set.cpp
    ${SRC_ROOT}/engine/IntActionInvoker.cpp
    ${SRC_ROOT}/engine/IntArray.cpp
    ${SRC_ROOT}/engine/IntAudio.cpp
    ${SRC_ROOT}/engine/IntBarcode.cpp
    ${SRC_ROOT}/engine/intcapi.cpp
    ${SRC_ROOT}/engine/IntCase.cpp
    ${SRC_ROOT}/engine/Intchars.cpp
    ${SRC_ROOT}/engine/IntCompression.cpp
    ${SRC_ROOT}/engine/IntDataAccess.cpp
    ${SRC_ROOT}/engine/IntDate.cpp
    ${SRC_ROOT}/engine/Intdentr.cpp
    ${SRC_ROOT}/engine/IntDiagnostics.cpp
    ${SRC_ROOT}/engine/IntDocument.cpp
    ${SRC_ROOT}/engine/Intdrive.cpp
    ${SRC_ROOT}/engine/IntDynamicLogicEvaluation.cpp
    ${SRC_ROOT}/engine/intenter.cpp
    ${SRC_ROOT}/engine/InterpreterAccessor.cpp
    ${SRC_ROOT}/engine/Intexpr.cpp
    ${SRC_ROOT}/engine/Intextdi.cpp
    ${SRC_ROOT}/engine/IntFreq.cpp
    ${SRC_ROOT}/engine/Intfuncs.cpp
    ${SRC_ROOT}/engine/IntGeometry.cpp
    ${SRC_ROOT}/engine/IntHashMap.cpp
    ${SRC_ROOT}/engine/IntHtmlDialog.cpp
    ${SRC_ROOT}/engine/IntImage.cpp
    ${SRC_ROOT}/engine/IntImpute.cpp
    ${SRC_ROOT}/engine/Intinstr.cpp
    ${SRC_ROOT}/engine/IntItem.cpp
    ${SRC_ROOT}/engine/IntJson.cpp
    ${SRC_ROOT}/engine/IntLanguage.cpp
    ${SRC_ROOT}/engine/IntList.cpp
    ${SRC_ROOT}/engine/IntLocalhost.cpp
    ${SRC_ROOT}/engine/IntMap.cpp
    ${SRC_ROOT}/engine/IntMath.cpp
    ${SRC_ROOT}/engine/IntMessages.cpp
    ${SRC_ROOT}/engine/IntOccurrences.cpp
    ${SRC_ROOT}/engine/IntPath.cpp
    ${SRC_ROOT}/engine/IntPff.cpp
    ${SRC_ROOT}/engine/IntPre77Report.cpp
    ${SRC_ROOT}/engine/IntProgramControl.cpp
    ${SRC_ROOT}/engine/IntProperties.cpp
    ${SRC_ROOT}/engine/IntQuery.cpp
    ${SRC_ROOT}/engine/IntReport.cpp
    ${SRC_ROOT}/engine/IntString.cpp
    ${SRC_ROOT}/engine/IntSwitch.cpp
    ${SRC_ROOT}/engine/IntSymbol.cpp
    ${SRC_ROOT}/engine/IntSync.cpp
    ${SRC_ROOT}/engine/IntSystemApp.cpp
    ${SRC_ROOT}/engine/IntUserbar.cpp
    ${SRC_ROOT}/engine/IntUserFunction.cpp
    ${SRC_ROOT}/engine/IntUserInterface.cpp
    ${SRC_ROOT}/engine/IntValues.cpp
    ${SRC_ROOT}/engine/IntVariable.cpp
    ${SRC_ROOT}/engine/Issafunc.cpp
    ${SRC_ROOT}/engine/Listing.cpp
    ${SRC_ROOT}/engine/LoadSaveBinary.cpp
    ${SRC_ROOT}/engine/Messages.cpp
    ${SRC_ROOT}/engine/Nextnode.cpp
    ${SRC_ROOT}/engine/ParadataDriver.cpp
    ${SRC_ROOT}/engine/ParameterManager.cpp
    ${SRC_ROOT}/engine/Pre80Routines.cpp
    ${SRC_ROOT}/engine/QuestionTextParamCache.cpp
    ${SRC_ROOT}/engine/relt.cpp
    ${SRC_ROOT}/engine/Settings.cpp
    ${SRC_ROOT}/engine/subtable.cpp
    ${SRC_ROOT}/engine/SyncListener.cpp
    ${SRC_ROOT}/engine/Tables.cpp
    ${SRC_ROOT}/engine/Tablesen.cpp
    ${SRC_ROOT}/engine/VariableInOut.cpp
)

# CEXEntry sources (from Android Engine.mk)
set(CEXENTRY_SOURCES
    ${SRC_ROOT}/CEXEntry/DeWrite.cpp
    ${SRC_ROOT}/CEXEntry/Dicx.cpp
    ${SRC_ROOT}/CEXEntry/Entifaz.cpp
    ${SRC_ROOT}/CEXEntry/Relation.cpp
    ${SRC_ROOT}/CEXEntry/Secx.cpp
    ${SRC_ROOT}/CEXEntry/varx.cpp
)

# Zissalib sources (from Android Engine.mk - excludes Windows-only files like KeyboardLoader.cpp)
set(ZISSALIB_SOURCES
    ${SRC_ROOT}/Zissalib/Attr.cpp
    ${SRC_ROOT}/Zissalib/CFlAdmin.cpp
    ${SRC_ROOT}/Zissalib/CFlow.cpp
    ${SRC_ROOT}/Zissalib/CsDriver.cpp
    ${SRC_ROOT}/Zissalib/CsDriver1.cpp
    ${SRC_ROOT}/Zissalib/CsKernel.cpp
    ${SRC_ROOT}/Zissalib/Defuncs.cpp
    ${SRC_ROOT}/Zissalib/Dict.cpp
    ${SRC_ROOT}/Zissalib/Dictload.cpp
    ${SRC_ROOT}/Zissalib/EngineAccessor.cpp
    ${SRC_ROOT}/Zissalib/EngineLoader.cpp
    ${SRC_ROOT}/Zissalib/Exallocd.cpp
    ${SRC_ROOT}/Zissalib/Exapplfu.cpp
    ${SRC_ROOT}/Zissalib/Exapplin.cpp
    ${SRC_ROOT}/Zissalib/ExNotes.cpp
    ${SRC_ROOT}/Zissalib/FlowCore.cpp
    ${SRC_ROOT}/Zissalib/FlowLoad.cpp
    ${SRC_ROOT}/Zissalib/Form2.cpp
    ${SRC_ROOT}/Zissalib/GroupT.cpp
    ${SRC_ROOT}/Zissalib/GroupT2.cpp
    ${SRC_ROOT}/Zissalib/GroupVisitor.cpp
    ${SRC_ROOT}/Zissalib/Labsalo.cpp
    ${SRC_ROOT}/Zissalib/OccurrenceInfo.cpp
    ${SRC_ROOT}/Zissalib/OccurrenceInfoSet.cpp
    ${SRC_ROOT}/Zissalib/OccurrenceVisitor.cpp
    ${SRC_ROOT}/Zissalib/SecT.cpp
    ${SRC_ROOT}/Zissalib/VarT.cpp
)

# All z* library sources - explicit lists from Android .mk files
# (excludes Windows-only files like PortableRunnerWindows.cpp)

# zAction sources (Android zAction.mk)
set(ZACTION_SOURCES
    ${SRC_ROOT}/zAction/ActionFunctionMapping.cpp
    ${SRC_ROOT}/zAction/ActionInvoker.cpp
    ${SRC_ROOT}/zAction/Application.cpp
    ${SRC_ROOT}/zAction/Caller.cpp
    ${SRC_ROOT}/zAction/Clipboard.cpp
    ${SRC_ROOT}/zAction/Data.cpp
    ${SRC_ROOT}/zAction/Dictionary.cpp
    ${SRC_ROOT}/zAction/Encoding.cpp
    ${SRC_ROOT}/zAction/File.cpp
    ${SRC_ROOT}/zAction/Hash.cpp
    ${SRC_ROOT}/zAction/JsonExecutor.cpp
    ${SRC_ROOT}/zAction/Localhost.cpp
    ${SRC_ROOT}/zAction/Logic.cpp
    ${SRC_ROOT}/zAction/Message.cpp
    ${SRC_ROOT}/zAction/Path.cpp
    ${SRC_ROOT}/zAction/Settings.cpp
    ${SRC_ROOT}/zAction/Sqlite.cpp
    ${SRC_ROOT}/zAction/System.cpp
    ${SRC_ROOT}/zAction/UserInterface.cpp
)

# zAppO sources (Android zAppO.mk)
set(ZAPPO_SOURCES
    ${SRC_ROOT}/zAppO/AppFileType.cpp
    ${SRC_ROOT}/zAppO/Application.cpp
    ${SRC_ROOT}/zAppO/AppSyncParameters.cpp
    ${SRC_ROOT}/zAppO/CodeFile.cpp
    ${SRC_ROOT}/zAppO/DictionaryDescription.cpp
    ${SRC_ROOT}/zAppO/EnumJsonSerializers.cpp
    ${SRC_ROOT}/zAppO/LabelSet.cpp
    ${SRC_ROOT}/zAppO/Language.cpp
    ${SRC_ROOT}/zAppO/LogicSettings.cpp
    ${SRC_ROOT}/zAppO/MappingDefines.cpp
    ${SRC_ROOT}/zAppO/OccurrenceLabels.cpp
    ${SRC_ROOT}/zAppO/PFF.cpp
    ${SRC_ROOT}/zAppO/Pre80SpecFile.cpp
    ${SRC_ROOT}/zAppO/Properties/ApplicationProperties.cpp
    ${SRC_ROOT}/zAppO/Properties/JsonProperties.cpp
    ${SRC_ROOT}/zAppO/Properties/MappingProperties.cpp
    ${SRC_ROOT}/zAppO/Properties/MappingTileProviderProperties.cpp
    ${SRC_ROOT}/zAppO/Properties/ParadataProperties.cpp
)

# zBridgeO sources (Android zBridgeO.mk)
set(ZBRIDGEO_SOURCES
    ${SRC_ROOT}/zBridgeO/NPFF.cpp
    ${SRC_ROOT}/zBridgeO/RunApl.cpp
)

# zCaseO sources (Android zCaseO.mk)
set(ZCASEO_SOURCES
    ${SRC_ROOT}/zCaseO/BinaryCaseItem.cpp
    ${SRC_ROOT}/zCaseO/Case.cpp
    ${SRC_ROOT}/zCaseO/CaseAccess.cpp
    ${SRC_ROOT}/zCaseO/CaseIO.cpp
    ${SRC_ROOT}/zCaseO/CaseItem.cpp
    ${SRC_ROOT}/zCaseO/CaseItemIndex.cpp
    ${SRC_ROOT}/zCaseO/CaseItemPrinter.cpp
    ${SRC_ROOT}/zCaseO/CaseItemReference.cpp
    ${SRC_ROOT}/zCaseO/CaseJsonSerializer.cpp
    ${SRC_ROOT}/zCaseO/CaseLevel.cpp
    ${SRC_ROOT}/zCaseO/CaseRecord.cpp
    ${SRC_ROOT}/zCaseO/FixedWidthNumericCaseItem.cpp
    ${SRC_ROOT}/zCaseO/FixedWidthNumericWithStringBufferCaseItem.cpp
    ${SRC_ROOT}/zCaseO/FixedWidthStringCaseItem.cpp
    ${SRC_ROOT}/zCaseO/ItemSubitemSyncTask.cpp
    ${SRC_ROOT}/zCaseO/NumericCaseItem.cpp
    ${SRC_ROOT}/zCaseO/Pre74_Case.cpp
    ${SRC_ROOT}/zCaseO/StringBasedCaseConstructionReporter.cpp
    ${SRC_ROOT}/zCaseO/StringCaseItem.cpp
    ${SRC_ROOT}/zCaseO/TextToCaseConverter.cpp
    ${SRC_ROOT}/zCaseO/VectorClock.cpp
)

# zConcatO sources (Android zConcatO.mk)
set(ZCONCATO_SOURCES
    ${SRC_ROOT}/zConcatO/CaseConcatenator.cpp
    ${SRC_ROOT}/zConcatO/Concatenator.cpp
    ${SRC_ROOT}/zConcatO/CSConcatReporter.cpp
    ${SRC_ROOT}/zConcatO/DuplicateCaseChecker.cpp
    ${SRC_ROOT}/zConcatO/TextConcatenator.cpp
)

# zDataO sources (Android zDataO.mk)
set(ZDATAO_SOURCES
    ${SRC_ROOT}/zDataO/CacheableCaseWrapperRepository.cpp
    ${SRC_ROOT}/zDataO/DataRepository.cpp
    ${SRC_ROOT}/zDataO/DataRepositoryHelpers.cpp
    ${SRC_ROOT}/zDataO/EncryptedSQLiteRepository.cpp
    ${SRC_ROOT}/zDataO/EncryptedSQLiteRepositoryPasswordManager.cpp
    ${SRC_ROOT}/zDataO/ExportWriterRepository.cpp
    ${SRC_ROOT}/zDataO/IndexableTextRepository.cpp
    ${SRC_ROOT}/zDataO/JsonRepository.cpp
    ${SRC_ROOT}/zDataO/MemoryRepository.cpp
    ${SRC_ROOT}/zDataO/NullRepository.cpp
    ${SRC_ROOT}/zDataO/ParadataWrapperRepository.cpp
    ${SRC_ROOT}/zDataO/SQLiteBinaryItemSerializer.cpp
    ${SRC_ROOT}/zDataO/SQLiteBlobQuestionnaireSerializer.cpp
    ${SRC_ROOT}/zDataO/SQLiteDictionarySchemaGenerator.cpp
    ${SRC_ROOT}/zDataO/SQLiteDictionarySchemaReconciler.cpp
    ${SRC_ROOT}/zDataO/SQLiteQuestionnaireSerializer.cpp
    ${SRC_ROOT}/zDataO/SQLiteRepository.cpp
    ${SRC_ROOT}/zDataO/SQLiteRepositoryCaseLoader.cpp
    ${SRC_ROOT}/zDataO/SQLiteRepositoryIterators.cpp
    ${SRC_ROOT}/zDataO/TextRepository.cpp
    ${SRC_ROOT}/zDataO/TextRepositoryNotesFile.cpp
    ${SRC_ROOT}/zDataO/TextRepositoryStatusFile.cpp
)

# zDictO sources (Android zDictO.mk)
set(ZDICTO_SOURCES
    ${SRC_ROOT}/zDictO/CaptureInfo.cpp
    ${SRC_ROOT}/zDictO/DDClass.cpp
    ${SRC_ROOT}/zDictO/Definitions.cpp
    ${SRC_ROOT}/zDictO/DictBase.cpp
    ${SRC_ROOT}/zDictO/DictionaryComparer.cpp
    ${SRC_ROOT}/zDictO/DictionaryValidator.cpp
    ${SRC_ROOT}/zDictO/DictItem.cpp
    ${SRC_ROOT}/zDictO/DictLevel.cpp
    ${SRC_ROOT}/zDictO/DictNamedBase.cpp
    ${SRC_ROOT}/zDictO/DictRecord.cpp
    ${SRC_ROOT}/zDictO/DictRelation.cpp
    ${SRC_ROOT}/zDictO/DictValue.cpp
    ${SRC_ROOT}/zDictO/DictValuePair.cpp
    ${SRC_ROOT}/zDictO/DictValueSet.cpp
    ${SRC_ROOT}/zDictO/ItemIndexHelper.cpp
    ${SRC_ROOT}/zDictO/Pre80SpecFile.cpp
    ${SRC_ROOT}/zDictO/ValueProcessor.cpp
    ${SRC_ROOT}/zDictO/ValueSetFixer.cpp
    ${SRC_ROOT}/zDictO/ValueSetResponse.cpp
)

# zDiffO sources (Android zDiffO.mk)
set(ZDIFFO_SOURCES
    ${SRC_ROOT}/zDiffO/Differ.cpp
    ${SRC_ROOT}/zDiffO/DiffSpec.cpp
)

# zEngineF sources (Android zEngineF.mk)
set(ZENGINEF_SOURCES
    ${SRC_ROOT}/zEngineF/EngineUI.cpp
    ${SRC_ROOT}/zEngineF/EngineUIPortable.cpp
    ${SRC_ROOT}/zEngineF/ErrmsgDlg.cpp
    ${SRC_ROOT}/zEngineF/PortableUserbar.cpp
    ${SRC_ROOT}/zEngineF/ReviewNotesDlg.cpp
    ${SRC_ROOT}/zEngineF/TraceHandler.cpp
)

# zEngineO sources (Android zEngineO.mk)
set(ZENGINEO_SOURCES
    ${SRC_ROOT}/zEngineO/Audio.cpp
    ${SRC_ROOT}/zEngineO/Array.cpp
    ${SRC_ROOT}/zEngineO/BinarySymbol.cpp
    ${SRC_ROOT}/zEngineO/BinarySymbolData.cpp
    ${SRC_ROOT}/zEngineO/Block.cpp
    ${SRC_ROOT}/zEngineO/Document.cpp
    ${SRC_ROOT}/zEngineO/EngineCase.cpp
    ${SRC_ROOT}/zEngineO/EngineData.cpp
    ${SRC_ROOT}/zEngineO/EngineDataRepository.cpp
    ${SRC_ROOT}/zEngineO/EngineDictionary.cpp
    ${SRC_ROOT}/zEngineO/EngineItem.cpp
    ${SRC_ROOT}/zEngineO/EngineRecord.cpp
    ${SRC_ROOT}/zEngineO/File.cpp
    ${SRC_ROOT}/zEngineO/Flow.cpp
    ${SRC_ROOT}/zEngineO/Geometry.cpp
    ${SRC_ROOT}/zEngineO/HashMap.cpp
    ${SRC_ROOT}/zEngineO/Image.cpp
    ${SRC_ROOT}/zEngineO/Imputation.cpp
    ${SRC_ROOT}/zEngineO/List.cpp
    ${SRC_ROOT}/zEngineO/LogicByteCode.cpp
    ${SRC_ROOT}/zEngineO/LoopStack.cpp
    ${SRC_ROOT}/zEngineO/Map.cpp
    ${SRC_ROOT}/zEngineO/NamedFrequency.cpp
    ${SRC_ROOT}/zEngineO/PenReaderApplicationLoader.cpp
    ${SRC_ROOT}/zEngineO/PersistentVariableProcessor.cpp
    ${SRC_ROOT}/zEngineO/Pff.cpp
    ${SRC_ROOT}/zEngineO/PffExecutor.cpp
    ${SRC_ROOT}/zEngineO/PreinitializedVariable.cpp
    ${SRC_ROOT}/zEngineO/ProcType.cpp
    ${SRC_ROOT}/zEngineO/Report.cpp
    ${SRC_ROOT}/zEngineO/ReportTokenizer.cpp
    ${SRC_ROOT}/zEngineO/ResponseProcessor.cpp
    ${SRC_ROOT}/zEngineO/RunnableSymbol.cpp
    ${SRC_ROOT}/zEngineO/RuntimeEvent.cpp
    ${SRC_ROOT}/zEngineO/SaveArrayFile.cpp
    ${SRC_ROOT}/zEngineO/SymbolCalculator.cpp
    ${SRC_ROOT}/zEngineO/SystemApp.cpp
    ${SRC_ROOT}/zEngineO/UserFunction.cpp
    ${SRC_ROOT}/zEngineO/UserFunctionArgumentChecker.cpp
    ${SRC_ROOT}/zEngineO/UserFunctionLocalSymbolsManager.cpp
    ${SRC_ROOT}/zEngineO/ValueSet.cpp
    ${SRC_ROOT}/zEngineO/Versioning.cpp
    ${SRC_ROOT}/zEngineO/WorkString.cpp
    ${SRC_ROOT}/zEngineO/WorkVariable.cpp
    ${SRC_ROOT}/zEngineO/Compiler/ActionInvokerCC.cpp
    ${SRC_ROOT}/zEngineO/Compiler/ArgumentSpecificationCompiler.cpp
    ${SRC_ROOT}/zEngineO/Compiler/ArrayCC.cpp
    ${SRC_ROOT}/zEngineO/Compiler/AudioCC.cpp
    ${SRC_ROOT}/zEngineO/Compiler/BarcodeCC.cpp
    ${SRC_ROOT}/zEngineO/Compiler/CaseCC.cpp
    ${SRC_ROOT}/zEngineO/Compiler/CompilerHelper.cpp
    ${SRC_ROOT}/zEngineO/Compiler/ControlFlowCC.cpp
    ${SRC_ROOT}/zEngineO/Compiler/DataAccessCC.cpp
    ${SRC_ROOT}/zEngineO/Compiler/DictionaryCC.cpp
    ${SRC_ROOT}/zEngineO/Compiler/DocumentCC.cpp
    ${SRC_ROOT}/zEngineO/Compiler/EngineDictionaryCC.cpp
    ${SRC_ROOT}/zEngineO/Compiler/ExpressionsCC.cpp
    ${SRC_ROOT}/zEngineO/Compiler/FileCC.cpp
    ${SRC_ROOT}/zEngineO/Compiler/FreqCC.cpp
    ${SRC_ROOT}/zEngineO/Compiler/FunctionsGenericCC.cpp
    ${SRC_ROOT}/zEngineO/Compiler/FunctionsVariousCC.cpp
    ${SRC_ROOT}/zEngineO/Compiler/GeometryCC.cpp
    ${SRC_ROOT}/zEngineO/Compiler/GpsCC.cpp
    ${SRC_ROOT}/zEngineO/Compiler/HashMapCC.cpp
    ${SRC_ROOT}/zEngineO/Compiler/ImageCC.cpp
    ${SRC_ROOT}/zEngineO/Compiler/ImputeCC.cpp
    ${SRC_ROOT}/zEngineO/Compiler/ItemCC.cpp
    ${SRC_ROOT}/zEngineO/Compiler/JsonCC.cpp
    ${SRC_ROOT}/zEngineO/Compiler/ListCC.cpp
    ${SRC_ROOT}/zEngineO/Compiler/LogicCompiler.cpp
    ${SRC_ROOT}/zEngineO/Compiler/MapCC.cpp
    ${SRC_ROOT}/zEngineO/Compiler/MessagesCC.cpp
    ${SRC_ROOT}/zEngineO/Compiler/NextTokenCC.cpp
    ${SRC_ROOT}/zEngineO/Compiler/NodeCreationCC.cpp
    ${SRC_ROOT}/zEngineO/Compiler/NumbersCC.cpp
    ${SRC_ROOT}/zEngineO/Compiler/OptionalNamedArgumentsCompiler.cpp
    ${SRC_ROOT}/zEngineO/Compiler/PathCC.cpp
    ${SRC_ROOT}/zEngineO/Compiler/PffCC.cpp
    ${SRC_ROOT}/zEngineO/Compiler/QueryCC.cpp
    ${SRC_ROOT}/zEngineO/Compiler/ReportCC.cpp
    ${SRC_ROOT}/zEngineO/Compiler/StringsCC.cpp
    ${SRC_ROOT}/zEngineO/Compiler/SwitchCC.cpp
    ${SRC_ROOT}/zEngineO/Compiler/SymbolsCC.cpp
    ${SRC_ROOT}/zEngineO/Compiler/SyncCC.cpp
    ${SRC_ROOT}/zEngineO/Compiler/SystemAppCC.cpp
    ${SRC_ROOT}/zEngineO/Compiler/TokenCC.cpp
    ${SRC_ROOT}/zEngineO/Compiler/TraceCC.cpp
    ${SRC_ROOT}/zEngineO/Compiler/UserbarCC.cpp
    ${SRC_ROOT}/zEngineO/Compiler/UserFunctionCC.cpp
    ${SRC_ROOT}/zEngineO/Compiler/UserInterfaceCC.cpp
    ${SRC_ROOT}/zEngineO/Compiler/ValueSetCC.cpp
    ${SRC_ROOT}/zEngineO/Compiler/VariableCC.cpp
)

# zEntryO sources (from Android Engine.mk)
set(ZENTRYO_SOURCES
    ${SRC_ROOT}/zEntryO/CapiContentVirtualFileMapping.cpp
    ${SRC_ROOT}/zEntryO/CaseTreeBuilder.cpp
    ${SRC_ROOT}/zEntryO/CaseTreeNode.cpp
    ${SRC_ROOT}/zEntryO/CaseTreeUpdate.cpp
    ${SRC_ROOT}/zEntryO/CoreEntryEngineInterface.cpp
    ${SRC_ROOT}/zEntryO/CoreEntryPage.cpp
    ${SRC_ROOT}/zEntryO/CoreEntryPageField.cpp
    ${SRC_ROOT}/zEntryO/DeploymentPackageDownloader.cpp
    ${SRC_ROOT}/zEntryO/EntDrv.cpp
    ${SRC_ROOT}/zEntryO/LevelNodeProcessing.cpp
    ${SRC_ROOT}/zEntryO/Runaple.cpp
    ${SRC_ROOT}/zEntryO/Wexentry.cpp
)

# zExcelO sources (Android zExcelO.mk - excluding libxlsxwriter)
set(ZEXCELO_SOURCES
    ${SRC_ROOT}/zExcelO/ExcelWriter.cpp
)

# zExportO sources (Android zExportO.mk)
set(ZEXPORTO_SOURCES
    ${SRC_ROOT}/zExportO/CSProExportWriter.cpp
    ${SRC_ROOT}/zExportO/DelimitedTextExportWriter.cpp
    ${SRC_ROOT}/zExportO/EncodedTextWriter.cpp
    ${SRC_ROOT}/zExportO/ExcelExportWriter.cpp
    ${SRC_ROOT}/zExportO/ExportDefinitions.cpp
    ${SRC_ROOT}/zExportO/ExportWriterBase.cpp
    ${SRC_ROOT}/zExportO/ReadStatExportWriterBase.cpp
    ${SRC_ROOT}/zExportO/RExportWriter.cpp
    ${SRC_ROOT}/zExportO/SasExportWriter.cpp
    ${SRC_ROOT}/zExportO/SasTransportWriter.cpp
    ${SRC_ROOT}/zExportO/SingleRecordExportWriterBase.cpp
    ${SRC_ROOT}/zExportO/SpssExportWriter.cpp
    ${SRC_ROOT}/zExportO/StataExportWriter.cpp
)

# zFormatterO sources (Android zFormatterO.mk)
set(ZFORMATTERO_SOURCES
    ${SRC_ROOT}/zFormatterO/QuestionnaireContentCreator.cpp
    ${SRC_ROOT}/zFormatterO/QuestionnaireViewer.cpp
)

# zFormO sources (Android zFormO.mk)
set(ZFORMO_SOURCES
    ${SRC_ROOT}/zFormO/Block.cpp
    ${SRC_ROOT}/zFormO/Box.cpp
    ${SRC_ROOT}/zFormO/Definitions.cpp
    ${SRC_ROOT}/zFormO/DragOptions.cpp
    ${SRC_ROOT}/zFormO/Field.cpp
    ${SRC_ROOT}/zFormO/FieldColors.cpp
    ${SRC_ROOT}/zFormO/Form.cpp
    ${SRC_ROOT}/zFormO/FormBase.cpp
    ${SRC_ROOT}/zFormO/FormFile.cpp
    ${SRC_ROOT}/zFormO/Group.cpp
    ${SRC_ROOT}/zFormO/ItemBase.cpp
    ${SRC_ROOT}/zFormO/Level.cpp
    ${SRC_ROOT}/zFormO/RepeatedItemSerializer.cpp
    ${SRC_ROOT}/zFormO/Roster.cpp
    ${SRC_ROOT}/zFormO/RosterCells.cpp
    ${SRC_ROOT}/zFormO/RosterColumn.cpp
    ${SRC_ROOT}/zFormO/Text.cpp
)

# zFreqO sources (Android zFreqO.mk)
set(ZFREQO_SOURCES
    ${SRC_ROOT}/zFreqO/ExcelFrequencyPrinter.cpp
    ${SRC_ROOT}/zFreqO/Frequency.cpp
    ${SRC_ROOT}/zFreqO/FrequencyCounter.cpp
    ${SRC_ROOT}/zFreqO/FrequencyPrinterEntry.cpp
    ${SRC_ROOT}/zFreqO/FrequencyPrinterHelpers.cpp
    ${SRC_ROOT}/zFreqO/FrequencyPrinterOptions.cpp
    ${SRC_ROOT}/zFreqO/HtmlFrequencyPrinter.cpp
    ${SRC_ROOT}/zFreqO/JsonFileFrequencyPrinter.cpp
    ${SRC_ROOT}/zFreqO/JsonFrequencyPrinter.cpp
    ${SRC_ROOT}/zFreqO/JsonStringFrequencyPrinter.cpp
    ${SRC_ROOT}/zFreqO/TextFrequencyPrinter.cpp
)

# zHtml sources (Android zHtml.mk)
set(ZHTML_SOURCES
    ${SRC_ROOT}/zHtml/AccessUrlSerializer.cpp
    ${SRC_ROOT}/zHtml/CSHtmlDlgRunner.cpp
    ${SRC_ROOT}/zHtml/FileSystemVirtualFileMappingHandler.cpp
    ${SRC_ROOT}/zHtml/HtmlDlgBaseRunner.cpp
    ${SRC_ROOT}/zHtml/LocalhostUrl.cpp
    ${SRC_ROOT}/zHtml/VirtualFileMappingHandlers.cpp
    ${SRC_ROOT}/zHtml/WebViewSyncOperationMarker.cpp
)

# zIndexO sources (Android zIndexO.mk)
set(ZINDEXO_SOURCES
    ${SRC_ROOT}/zIndexO/Indexer.cpp
)

# zJavaScript sources (Android zJavaScript.mk)
set(ZJAVASCRIPT_SOURCES
    ${SRC_ROOT}/zJavaScript/ActionInvokerJS.cpp
    ${SRC_ROOT}/zJavaScript/Executor.cpp
    ${SRC_ROOT}/zJavaScript/QuickJSAccess.cpp
    ${SRC_ROOT}/zJavaScript/Value.cpp
)

# zJson sources (Android zJson.mk)
set(ZJSON_SOURCES
    ${SRC_ROOT}/zJson/Json.cpp
    ${SRC_ROOT}/zJson/JsonFormattingOptions.cpp
    ${SRC_ROOT}/zJson/JsonNode.cpp
    ${SRC_ROOT}/zJson/JsonObjectCreator.cpp
    ${SRC_ROOT}/zJson/JsonSpecFile.cpp
    ${SRC_ROOT}/zJson/JsonStream.cpp
)

# zListingO sources (Android zListingO.mk)
set(ZLISTINGO_SOURCES
    ${SRC_ROOT}/zListingO/CsvLister.cpp
    ${SRC_ROOT}/zListingO/DataFileLister.cpp
    ${SRC_ROOT}/zListingO/ErrorLister.cpp
    ${SRC_ROOT}/zListingO/ExcelLister.cpp
    ${SRC_ROOT}/zListingO/HtmlLister.cpp
    ${SRC_ROOT}/zListingO/JsonLister.cpp
    ${SRC_ROOT}/zListingO/Lister.cpp
    ${SRC_ROOT}/zListingO/ListingHelpers.cpp
    ${SRC_ROOT}/zListingO/TextLister.cpp
    ${SRC_ROOT}/zListingO/TextWriteFile.cpp
)

# zLogicO sources (Android zLogicO.mk)
set(ZLOGICO_SOURCES
    ${SRC_ROOT}/zLogicO/ActionInvoker.cpp
    ${SRC_ROOT}/zLogicO/BaseCompiler.cpp
    ${SRC_ROOT}/zLogicO/BasicTokenCompiler.cpp
    ${SRC_ROOT}/zLogicO/FunctionTable.cpp
    ${SRC_ROOT}/zLogicO/GeneralizedFunction.cpp
    ${SRC_ROOT}/zLogicO/KeywordTable.cpp
    ${SRC_ROOT}/zLogicO/Preprocessor.cpp
    ${SRC_ROOT}/zLogicO/ReservedWords.cpp
    ${SRC_ROOT}/zLogicO/SourceBuffer.cpp
    ${SRC_ROOT}/zLogicO/Symbol.cpp
    ${SRC_ROOT}/zLogicO/SymbolTable.cpp
    ${SRC_ROOT}/zLogicO/SymbolType.cpp
)

# zMapping sources (Android zMapping.mk)
set(ZMAPPING_SOURCES
    ${SRC_ROOT}/zMapping/CoordinateConverter.cpp
    ${SRC_ROOT}/zMapping/GeoJson.cpp
)

# zMessageO sources (Android zMessageO.mk)
set(ZMESSAGEO_SOURCES
    ${SRC_ROOT}/zMessageO/MessageEvaluator.cpp
    ${SRC_ROOT}/zMessageO/MessageFile.cpp
    ${SRC_ROOT}/zMessageO/MessageManager.cpp
    ${SRC_ROOT}/zMessageO/Messages.cpp
    ${SRC_ROOT}/zMessageO/SystemMessageIssuer.cpp
    ${SRC_ROOT}/zMessageO/SystemMessages.cpp
)

# zMultimediaO sources (Android zMultimediaO.mk - excluding external)
set(ZMULTIMEDIAO_SOURCES
    ${SRC_ROOT}/zMultimediaO/BmpFile.cpp
    ${SRC_ROOT}/zMultimediaO/Image.cpp
    ${SRC_ROOT}/zMultimediaO/Mp4Reader.cpp
    ${SRC_ROOT}/zMultimediaO/Mp4Writer.cpp
    ${SRC_ROOT}/zMultimediaO/QRCode.cpp
)

# zNetwork sources (Android zNetwork.mk)
set(ZNETWORK_SOURCES
    ${SRC_ROOT}/zNetwork/FileInfo.cpp
    ${SRC_ROOT}/zNetwork/HeaderList.cpp
)

# zPackO sources (Android zPackO.mk)
set(ZPACKO_SOURCES
    ${SRC_ROOT}/zPackO/PackEntry.cpp
    ${SRC_ROOT}/zPackO/Packer.cpp
    ${SRC_ROOT}/zPackO/PackSpec.cpp
)

# zParadataO sources (Android zParadataO.mk)
set(ZPARADATAO_SOURCES
    ${SRC_ROOT}/zParadataO/ApplicationEvent.cpp
    ${SRC_ROOT}/zParadataO/CaseEvent.cpp
    ${SRC_ROOT}/zParadataO/Concatenator.cpp
    ${SRC_ROOT}/zParadataO/DataRepositoryEvent.cpp
    ${SRC_ROOT}/zParadataO/DeviceStateEvent.cpp
    ${SRC_ROOT}/zParadataO/ExternalApplicationEvent.cpp
    ${SRC_ROOT}/zParadataO/FieldEntryEvent.cpp
    ${SRC_ROOT}/zParadataO/FieldInfo.cpp
    ${SRC_ROOT}/zParadataO/FieldMovementEvent.cpp
    ${SRC_ROOT}/zParadataO/FieldValidationEvent.cpp
    ${SRC_ROOT}/zParadataO/GpsEvent.cpp
    ${SRC_ROOT}/zParadataO/GuiConcatenator.cpp
    ${SRC_ROOT}/zParadataO/ImputeEvent.cpp
    ${SRC_ROOT}/zParadataO/KeyingInstance.cpp
    ${SRC_ROOT}/zParadataO/LanguageChangeEvent.cpp
    ${SRC_ROOT}/zParadataO/Log.cpp
    ${SRC_ROOT}/zParadataO/Logger.cpp
    ${SRC_ROOT}/zParadataO/MessageEvent.cpp
    ${SRC_ROOT}/zParadataO/NoteEvent.cpp
    ${SRC_ROOT}/zParadataO/OperatorSelectionEvent.cpp
    ${SRC_ROOT}/zParadataO/PropertyEvent.cpp
    ${SRC_ROOT}/zParadataO/SessionEvent.cpp
    ${SRC_ROOT}/zParadataO/Syncer.cpp
    ${SRC_ROOT}/zParadataO/Table.cpp
    ${SRC_ROOT}/zParadataO/TableDefinitions.cpp
)

# zPlatformO sources (Android zPlatformO.mk)
set(ZPLATFORMO_SOURCES
    ${SRC_ROOT}/zPlatformO/PlatformInterface.cpp
    ${SRC_ROOT}/zPlatformO/CSProStdioFile.cpp
    ${SRC_ROOT}/zPlatformO/MobileStringConversion.cpp
    ${SRC_ROOT}/zPlatformO/util_snprintf.cpp
    ${SRC_ROOT}/zPlatformO/PortableFStream.cpp
)

# zReformatO sources (Android zReformatO.mk)
set(ZREFORMATO_SOURCES
    ${SRC_ROOT}/zReformatO/Reformatter.cpp
    ${SRC_ROOT}/zReformatO/ToolReformatter.cpp
)

# zReportO sources (Android zReportO.mk - excluding gumbo)
set(ZREPORTO_SOURCES
    ${SRC_ROOT}/zReportO/Pre77Report.cpp
    ${SRC_ROOT}/zReportO/Pre77ReportManager.cpp
    ${SRC_ROOT}/zReportO/Pre77ReportNodes.cpp
)

# zSortO sources (Android zSortO.mk)
set(ZSORTO_SOURCES
    ${SRC_ROOT}/zSortO/SortableKeyDatabase.cpp
    ${SRC_ROOT}/zSortO/Sorter.cpp
    ${SRC_ROOT}/zSortO/SortSpec.cpp
)

# zSyncO sources (Android zSyncO.mk - excluding easylogging)
set(ZSYNCO_SOURCES
    ${SRC_ROOT}/zSyncO/AppSyncParamRunner.cpp
    ${SRC_ROOT}/zSyncO/ApplicationPackage.cpp
    ${SRC_ROOT}/zSyncO/ApplicationPackageManager.cpp
    ${SRC_ROOT}/zSyncO/BluetoothChunk.cpp
    ${SRC_ROOT}/zSyncO/BluetoothSyncServerConnection.cpp
    ${SRC_ROOT}/zSyncO/BluetoothObexConnection.cpp
    ${SRC_ROOT}/zSyncO/BluetoothObexServer.cpp
    ${SRC_ROOT}/zSyncO/ConnectResponse.cpp
    ${SRC_ROOT}/zSyncO/CSWebDataChunk.cpp
    ${SRC_ROOT}/zSyncO/CSWebSyncServerConnection.cpp
    ${SRC_ROOT}/zSyncO/DefaultChunk.cpp
    ${SRC_ROOT}/zSyncO/DropboxSyncServerConnection.cpp
    ${SRC_ROOT}/zSyncO/DropboxDB.cpp
    ${SRC_ROOT}/zSyncO/ExponentialBackoff.cpp
    ${SRC_ROOT}/zSyncO/FtpSyncServerConnection.cpp
    ${SRC_ROOT}/zSyncO/JsonConverter.cpp
    ${SRC_ROOT}/zSyncO/LocalFileSyncServerConnection.cpp
    ${SRC_ROOT}/zSyncO/ObexClient.cpp
    ${SRC_ROOT}/zSyncO/ObexConstants.cpp
    ${SRC_ROOT}/zSyncO/ObexHeader.cpp
    ${SRC_ROOT}/zSyncO/ObexPacket.cpp
    ${SRC_ROOT}/zSyncO/ObexPacketSerializer.cpp
    ${SRC_ROOT}/zSyncO/ObexServer.cpp
    ${SRC_ROOT}/zSyncO/OauthTokenRequest.cpp
    ${SRC_ROOT}/zSyncO/OauthTokenResponse.cpp
    ${SRC_ROOT}/zSyncO/SyncClient.cpp
    ${SRC_ROOT}/zSyncO/SyncCustomHeaders.cpp
    ${SRC_ROOT}/zSyncO/SyncObexHandler.cpp
    ${SRC_ROOT}/zSyncO/SyncErrorResponse.cpp
    ${SRC_ROOT}/zSyncO/SyncGetResponse.cpp
    ${SRC_ROOT}/zSyncO/SyncPutResponse.cpp
    ${SRC_ROOT}/zSyncO/SyncRequest.cpp
    ${SRC_ROOT}/zSyncO/SyncServerConnectionFactory.cpp
)

# zToolsO sources (Android zToolsO.mk - including crypto/compression C files)
set(ZTOOLSO_SOURCES
    ${SRC_ROOT}/zToolsO/base64.cpp
    ${SRC_ROOT}/zToolsO/BinaryGen.cpp
    ${SRC_ROOT}/zToolsO/CSProException.cpp
    ${SRC_ROOT}/zToolsO/DateTime.cpp
    ${SRC_ROOT}/zToolsO/DelimitedTextCreator.cpp
    ${SRC_ROOT}/zToolsO/DirectoryLister.cpp
    ${SRC_ROOT}/zToolsO/Encoders.cpp
    ${SRC_ROOT}/zToolsO/Encryption.cpp
    ${SRC_ROOT}/zToolsO/ErrorMessageDisplayer.cpp
    ${SRC_ROOT}/zToolsO/FileIO.cpp
    ${SRC_ROOT}/zToolsO/FuzzyWuzzy.cpp
    ${SRC_ROOT}/zToolsO/Hash.cpp
    ${SRC_ROOT}/zToolsO/ImsaStrMem.cpp
    ${SRC_ROOT}/zToolsO/Memctrl.cpp
    ${SRC_ROOT}/zToolsO/NumberToString.cpp
    ${SRC_ROOT}/zToolsO/ObjectTransporter.cpp
    ${SRC_ROOT}/zToolsO/PortableFunctions.cpp
    ${SRC_ROOT}/zToolsO/Screen.cpp
    ${SRC_ROOT}/zToolsO/serializer.cpp
    ${SRC_ROOT}/zToolsO/Special.cpp
    ${SRC_ROOT}/zToolsO/StringOperations.cpp
    ${SRC_ROOT}/zToolsO/TextConverterAnsi.cpp
    ${SRC_ROOT}/zToolsO/Tools.cpp
    ${SRC_ROOT}/zToolsO/Utf8Convert.cpp
    ${SRC_ROOT}/zToolsO/Utf8FileStream.cpp
    ${SRC_ROOT}/zToolsO/uuid.cpp
    ${SRC_ROOT}/zToolsO/VarFuncs.cpp
    # Crypto and compression C files
    ${SRC_ROOT}/zToolsO/bzlib.c
    ${SRC_ROOT}/zToolsO/md5.c
    ${SRC_ROOT}/zToolsO/rijndael/rijndael-alg-fst.c
    ${SRC_ROOT}/zToolsO/scrypt/sha256.c
    ${SRC_ROOT}/zToolsO/scrypt/insecure_memzero.c
)

# zUtilF sources (Android zUtilF.mk)
set(ZUTILF_SOURCES
    ${SRC_ROOT}/zUtilF/ChoiceDlg.cpp
    ${SRC_ROOT}/zUtilF/HtmlDialogFunctionRunner.cpp
    ${SRC_ROOT}/zUtilF/ImageCaptureDlg.cpp
    ${SRC_ROOT}/zUtilF/ImageViewDlg.cpp
    ${SRC_ROOT}/zUtilF/NoteEditDlg.cpp
    ${SRC_ROOT}/zUtilF/ProcessSummaryDlg.cpp
    ${SRC_ROOT}/zUtilF/ProgressDlgFactory.cpp
    ${SRC_ROOT}/zUtilF/SelectDlg.cpp
    ${SRC_ROOT}/zUtilF/SelectFileDlg.cpp
    ${SRC_ROOT}/zUtilF/TextInputDlg.cpp
)

# zUtilO sources (Android zUtilO.mk)
set(ZUTILO_SOURCES
    ${SRC_ROOT}/zUtilO/BasicLogger.cpp
    ${SRC_ROOT}/zUtilO/BinaryDataAccessor.cpp
    ${SRC_ROOT}/zUtilO/BinaryDataMetadata.cpp
    ${SRC_ROOT}/zUtilO/CommonStore.cpp
    ${SRC_ROOT}/zUtilO/ConnectionString.cpp
    ${SRC_ROOT}/zUtilO/CredentialStore.cpp
    ${SRC_ROOT}/zUtilO/CSProExecutables.cpp
    ${SRC_ROOT}/zUtilO/DataTypes.cpp
    ${SRC_ROOT}/zUtilO/ExecutionStack.cpp
    ${SRC_ROOT}/zUtilO/FileExtensions.cpp
    ${SRC_ROOT}/zUtilO/imsaStr.cpp
    ${SRC_ROOT}/zUtilO/Interapp.cpp
    ${SRC_ROOT}/zUtilO/MediaStore.cpp
    ${SRC_ROOT}/zUtilO/MimeType.cpp
    ${SRC_ROOT}/zUtilO/MimeTypeMap.cpp
    ${SRC_ROOT}/zUtilO/NameShortener.cpp
    ${SRC_ROOT}/zUtilO/PathHelpers.cpp
    ${SRC_ROOT}/zUtilO/PortableColor.cpp
    ${SRC_ROOT}/zUtilO/PortableFileSystem.cpp
    ${SRC_ROOT}/zUtilO/PortableFont.cpp
    ${SRC_ROOT}/zUtilO/ProcessSummary.cpp
    ${SRC_ROOT}/zUtilO/PropertyString.cpp
    ${SRC_ROOT}/zUtilO/Randomizer.cpp
    ${SRC_ROOT}/zUtilO/SimpleDbMap.cpp
    ${SRC_ROOT}/zUtilO/Specfile.cpp
    ${SRC_ROOT}/zUtilO/SpecialDirectoryLister.cpp
    ${SRC_ROOT}/zUtilO/SQLiteSchema.cpp
    ${SRC_ROOT}/zUtilO/SqlLogicFunctions.cpp
    ${SRC_ROOT}/zUtilO/StdioFileUnicode.cpp
    ${SRC_ROOT}/zUtilO/TemporaryFile.cpp
    ${SRC_ROOT}/zUtilO/TextSource.cpp
    ${SRC_ROOT}/zUtilO/TextSourceEditable.cpp
    ${SRC_ROOT}/zUtilO/TextSourceExternal.cpp
    ${SRC_ROOT}/zUtilO/TransactionManager.cpp
    ${SRC_ROOT}/zUtilO/Versioning.cpp
    ${SRC_ROOT}/zUtilO/Viewers.cpp
    ${SRC_ROOT}/zUtilO/VirtualDirectoryLister.cpp
)

# zZipO sources (Android zZipO.mk)
set(ZZIPO_SOURCES
    ${SRC_ROOT}/zZipO/IZip.cpp
    ${SRC_ROOT}/zZipO/miniz_inclusion.cpp
    ${SRC_ROOT}/zZipO/ZipUtility.cpp
    ${SRC_ROOT}/zZipO/ZipZL.cpp
)

# zCapiO sources (from Android Engine.mk)
set(ZCAPIO_SOURCES
    ${SRC_ROOT}/zCapiO/Capi.cpp
    ${SRC_ROOT}/zCapiO/CapiCondition.cpp
    ${SRC_ROOT}/zCapiO/CapiFill.cpp
    ${SRC_ROOT}/zCapiO/CapiQuestion.cpp
    ${SRC_ROOT}/zCapiO/CapiQuestionFilePre76.cpp
    ${SRC_ROOT}/zCapiO/CapiQuestionManager.cpp
    ${SRC_ROOT}/zCapiO/CapiQuestionYaml.cpp
    ${SRC_ROOT}/zCapiO/CapiStyle.cpp
    ${SRC_ROOT}/zCapiO/CapiText.cpp
)

# Combine all z* sources
set(ALL_Z_SOURCES
    ${ZACTION_SOURCES}
    ${ZAPPO_SOURCES}
    ${ZBRIDGEO_SOURCES}
    ${ZCASEO_SOURCES}
    ${ZCONCATO_SOURCES}
    ${ZDATAO_SOURCES}
    ${ZDICTO_SOURCES}
    ${ZDIFFO_SOURCES}
    ${ZENGINEF_SOURCES}
    ${ZENGINEO_SOURCES}
    ${ZENTRYO_SOURCES}
    ${ZEXCELO_SOURCES}
    ${ZEXPORTO_SOURCES}
    ${ZFORMATTERO_SOURCES}
    ${ZFORMO_SOURCES}
    ${ZFREQO_SOURCES}
    ${ZHTML_SOURCES}
    ${ZINDEXO_SOURCES}
    ${ZJAVASCRIPT_SOURCES}
    ${ZJSON_SOURCES}
    ${ZLISTINGO_SOURCES}
    ${ZLOGICO_SOURCES}
    ${ZMAPPING_SOURCES}
    ${ZMESSAGEO_SOURCES}
    ${ZMULTIMEDIAO_SOURCES}
    ${ZNETWORK_SOURCES}
    ${ZPACKO_SOURCES}
    ${ZPARADATAO_SOURCES}
    ${ZPLATFORMO_SOURCES}
    ${ZREFORMATO_SOURCES}
    ${ZREPORTO_SOURCES}
    ${ZSORTO_SOURCES}
    ${ZSYNCO_SOURCES}
    ${ZTOOLSO_SOURCES}
    ${ZUTILF_SOURCES}
    ${ZUTILO_SOURCES}
    ${ZZIPO_SOURCES}
    ${ZCAPIO_SOURCES}
)
# WASM-specific sources (includes WASMBindings.cpp and WasmApplicationInterface.cpp)
file(GLOB WASM_SOURCES 
    "${SRC_ROOT}/WASM/*.cpp"
)

# Note: jni/src/Web*.cpp contains Android-specific bindings, not used for KMP WASM build
# The WASM folder contains proper web/Emscripten bindings)

# ============================================================================
# Build Libraries
# ============================================================================

# External static libraries
add_library(zlib_static STATIC ${ZLIB_SOURCES})
add_library(sqlite_static STATIC ${SQLITE_SOURCES} ${SQLITE_WRAPPER_SOURCES})
add_library(yaml_static STATIC ${YAML_SOURCES})
target_include_directories(yaml_static PRIVATE 
    ${SRC_ROOT}/external/yaml-cpp/include
    ${SRC_ROOT}/external/yaml-cpp/src
)
add_library(rtf2html_static STATIC ${RTF2HTML_SOURCES})
target_compile_definitions(rtf2html_static PRIVATE RTF2HTML_DLL_EXPORTS)

# libxlsxwriter for Excel export (with minizip for ZIP support)
set(MINIZIP_SOURCES
    ${SRC_ROOT}/external/libxlsxwriter/third_party/minizip/ioapi.c
    ${SRC_ROOT}/external/libxlsxwriter/third_party/minizip/zip.c
)
add_library(xlsxwriter_static STATIC ${XLSXWRITER_SOURCES} ${MINIZIP_SOURCES})
target_include_directories(xlsxwriter_static PRIVATE 
    ${SRC_ROOT}/external/libxlsxwriter/include
    ${SRC_ROOT}/external/libxlsxwriter/third_party/minizip
    ${SRC_ROOT}/external/zlib
)
target_compile_definitions(xlsxwriter_static PRIVATE USE_STANDARD_TMPFILE)

# librdata for R data file export
file(GLOB LIBRDATA_SOURCES "${SRC_ROOT}/external/librdata/*.c")
add_library(librdata_static STATIC ${LIBRDATA_SOURCES})
target_include_directories(librdata_static PRIVATE ${SRC_ROOT}/external/librdata)

# ReadStat for SAS/SPSS/Stata export (includes librdata+ReadStat helpers)
file(GLOB READSTAT_SOURCES 
    "${SRC_ROOT}/external/ReadStat/*.c"
    "${SRC_ROOT}/external/ReadStat/sas/*.c"
    "${SRC_ROOT}/external/ReadStat/spss/*.c"
    "${SRC_ROOT}/external/ReadStat/stata/*.c"
    "${SRC_ROOT}/external/librdata+ReadStat/*.c"
)
add_library(readstat_static STATIC ${READSTAT_SOURCES})
target_include_directories(readstat_static PRIVATE 
    ${SRC_ROOT}/external/ReadStat
    ${SRC_ROOT}/external/zlib
)

# easylogging++ for logging
add_library(easylogging_static STATIC ${EASYLOGGING_SOURCES})
target_include_directories(easylogging_static PRIVATE ${SRC_ROOT}/external/easylogging)
target_compile_definitions(easylogging_static PRIVATE ELPP_NO_DEFAULT_LOG_FILE)

# Gumbo HTML parser for Pre77Report
file(GLOB GUMBO_SOURCES "${SRC_ROOT}/external/gumbo/*.c")
add_library(gumbo_static STATIC ${GUMBO_SOURCES})
target_include_directories(gumbo_static PUBLIC 
    ${SRC_ROOT}/external/gumbo
    ${SRC_ROOT}/external/gumbo/include
)

# QR Code generator
add_library(qrcodegen_static STATIC ${SRC_ROOT}/external/qrcodegen/qrcodegen.cpp)
target_include_directories(qrcodegen_static PUBLIC ${SRC_ROOT}/external/qrcodegen)

# Engine library (includes CEXEntry and Zissalib)
add_library(engine_static STATIC ${ENGINE_SOURCES} ${CEXENTRY_SOURCES} ${ZISSALIB_SOURCES})
target_include_directories(engine_static PRIVATE ${SRC_ROOT})

# CSPro core library (all z* modules)
add_library(cspro_core STATIC ${ALL_Z_SOURCES})
target_include_directories(cspro_core PRIVATE 
    ${SRC_ROOT}
    # Add each z* library directory for relative includes
    ${SRC_ROOT}/zAction
    ${SRC_ROOT}/zAppO
    ${SRC_ROOT}/zBridgeO
    ${SRC_ROOT}/zCaseO
    ${SRC_ROOT}/zConcatO
    ${SRC_ROOT}/zDataO
    ${SRC_ROOT}/zDictO
    ${SRC_ROOT}/zDiffO
    ${SRC_ROOT}/zEngineF
    ${SRC_ROOT}/zEngineO
    ${SRC_ROOT}/zEntryO
    ${SRC_ROOT}/zExcelO
    ${SRC_ROOT}/zExportO
    ${SRC_ROOT}/zFormatterO
    ${SRC_ROOT}/zFormO
    ${SRC_ROOT}/zFreqO
    ${SRC_ROOT}/zHtml
    ${SRC_ROOT}/zIndexO
    ${SRC_ROOT}/zJavaScript
    ${SRC_ROOT}/zJson
    ${SRC_ROOT}/zListingO
    ${SRC_ROOT}/zLogicO
    ${SRC_ROOT}/zMapping
    ${SRC_ROOT}/zMessageO
    ${SRC_ROOT}/zMultimediaO
    ${SRC_ROOT}/zNetwork
    ${SRC_ROOT}/zPackO
    ${SRC_ROOT}/zParadataO
    ${SRC_ROOT}/zPlatformO
    ${SRC_ROOT}/zReformatO
    ${SRC_ROOT}/zReportO
    ${SRC_ROOT}/zSortO
    ${SRC_ROOT}/zSyncO
    ${SRC_ROOT}/zToolsO
    ${SRC_ROOT}/zUtilF
    ${SRC_ROOT}/zUtilO
    ${SRC_ROOT}/zZipO
    ${SRC_ROOT}/zCapiO
)

# ============================================================================
# Main Executable
# ============================================================================

add_executable(${PROJECT_NAME}
    ${WASM_SOURCES}
)

# Link all libraries
target_link_libraries(${PROJECT_NAME}
    cspro_core
    engine_static
    xlsxwriter_static
    librdata_static
    readstat_static
    easylogging_static
    gumbo_static
    qrcodegen_static
    rtf2html_static
    yaml_static
    -Wl,--whole-archive sqlite_static -Wl,--no-whole-archive
    zlib_static
)

# ============================================================================
# Emscripten Link Options
# ============================================================================

# Memory
target_link_options(${PROJECT_NAME} PUBLIC -sALLOW_MEMORY_GROWTH)
target_link_options(${PROJECT_NAME} PUBLIC -sINITIAL_MEMORY=134217728)  # 128MB

# JS exceptions (for Asyncify compatibility - WASM exceptions not compatible)
# Use -sDISABLE_EXCEPTION_CATCHING=0 for C++ exceptions via JS
target_link_options(${PROJECT_NAME} PUBLIC -sDISABLE_EXCEPTION_CATCHING=0)

# Embind for JavaScript bindings
target_link_options(${PROJECT_NAME} PUBLIC --bind)

# ES6 module export
target_link_options(${PROJECT_NAME} PUBLIC -sEXPORT_ES6=1)
target_link_options(${PROJECT_NAME} PUBLIC -sMODULARIZE=1)
target_link_options(${PROJECT_NAME} PUBLIC -sEXPORT_NAME=createCSEntryKMPModule)

# Environment support
target_link_options(${PROJECT_NAME} PUBLIC -sENVIRONMENT=web,node)

# Asyncify for async operations (widely supported alternative to JSPI)
# JSPI (-sASYNCIFY=2) requires Chrome 129+ with flag - not widely supported
# Asyncify (-sASYNCIFY=1) works in all browsers but is slower/larger
# Use Asyncify for maximum browser compatibility
target_link_options(${PROJECT_NAME} PUBLIC -sASYNCIFY=1)
target_link_options(${PROJECT_NAME} PUBLIC -sASYNCIFY_STACK_SIZE=65536)
# List functions that may call async JS (enables stack unwinding)
target_link_options(${PROJECT_NAME} PUBLIC "-sASYNCIFY_IMPORTS=['jspi_showDialog','jspi_showHtmlDialog','jspi_showModalDialog','jspi_getInputData','emscripten_sleep']")

# Export filesystem API
target_link_options(${PROJECT_NAME} PUBLIC -sEXPORTED_RUNTIME_METHODS=['FS','ccall','cwrap','UTF8ToString','stringToUTF8'])

# Optimization
target_link_options(${PROJECT_NAME} PUBLIC "SHELL:-O2")

# Preload assets
if(EXISTS "${SRC_ROOT}/WASM/Assets")
    target_link_options(${PROJECT_NAME} PUBLIC --preload-file ${SRC_ROOT}/WASM/Assets@Assets/)
endif()

# ============================================================================
# Install
# ============================================================================

set(INSTALL_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../public/wasm)
install(FILES 
    ${CMAKE_BINARY_DIR}/${PROJECT_NAME}.js
    ${CMAKE_BINARY_DIR}/${PROJECT_NAME}.wasm
    DESTINATION ${INSTALL_DIR}
)

if(EXISTS ${CMAKE_BINARY_DIR}/${PROJECT_NAME}.data)
    install(FILES ${CMAKE_BINARY_DIR}/${PROJECT_NAME}.data DESTINATION ${INSTALL_DIR})
endif()

message(STATUS "=======================================")
message(STATUS "Build Configuration Complete")
message(STATUS "  Output: ${PROJECT_NAME}.js, ${PROJECT_NAME}.wasm")
message(STATUS "  Install: ${INSTALL_DIR}")
message(STATUS "=======================================")
