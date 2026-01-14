/**
 * WebWASMBindings.cpp
 * 
 * Emscripten Embind bindings for AndroidEngineInterface
 * This replaces JNI bindings with JavaScript-callable functions
 * 
 * Port of: gov_census_cspro_engine_EngineInterface_jni.cpp (1015 lines)
 * Target: Kotlin/Wasm frontend via CSProWasmModule.kt
 */

#include <emscripten/bind.h>
#include <emscripten/val.h>
#include "AndroidEngineInterface.h"
#include "AndroidApplicationInterface.h"
#include <Zentryo/CoreEntryPage.h>
#include <Zentryo/CoreEntryPageField.h>
#include <zCaseO/CaseSummary.h>
#include <ZBRIDGEO/npff.h>
#include <zMapping/CoordinateConverter.h>

using namespace emscripten;

// Global engine interface instance (replaces JNI static reference)
static AndroidEngineInterface* g_engineInterface = nullptr;

// ============================================================
// INITIALIZATION
// ============================================================

/**
 * Initialize the native engine interface
 * Replaces: Java_gov_census_cspro_engine_EngineInterface_InitNativeEngineInterface
 */
val InitNativeEngineInterface() {
    if (!g_engineInterface) {
        g_engineInterface = new AndroidEngineInterface();
    }
    return val(reinterpret_cast<long>(g_engineInterface));
}

/**
 * Set web environment variables
 * Replaces: Java_gov_census_cspro_engine_EngineInterface_SetAndroidEnvironmentVariables
 */
void SetEnvironmentVariables(
    std::string email,
    std::string tempFolder,
    std::string applicationFolder,
    std::string versionNumber,
    std::string assetsDirectory,
    std::string csEntryDirectory,
    std::string externalMemoryCardDirectory,
    std::string internalStorageDirectory,
    std::string downloadsDirectory)
{
    if (!g_engineInterface) return;
    
    g_engineInterface->SetAndroidEnvironmentVariables(
        CString(email.c_str()),
        CString(tempFolder.c_str()),
        CString(applicationFolder.c_str()),
        CString(versionNumber.c_str()),
        CString(assetsDirectory.c_str()),
        CString(csEntryDirectory.c_str()),
        CString(externalMemoryCardDirectory.c_str()),
        CString(internalStorageDirectory.c_str()),
        CString(downloadsDirectory.c_str())
    );
}

// ============================================================
// APPLICATION LIFECYCLE
// ============================================================

/**
 * Initialize application from PFF file
 * Replaces: Java_gov_census_cspro_engine_EngineInterface_InitApplication
 */
bool InitApplication(std::string pffFilename) {
    if (!g_engineInterface) return false;
    
    CString csPffFilename(pffFilename.c_str());
    return g_engineInterface->InitApplication(csPffFilename);
}

/**
 * Close/End application
 * Replaces: Java_gov_census_cspro_engine_EngineInterface_EndApplication
 */
void EndApplication() {
    if (!g_engineInterface) return;
    g_engineInterface->EndApplication();
}

/**
 * Start data entry
 * Replaces: Java_gov_census_cspro_engine_EngineInterface_Start
 */
bool Start() {
    if (!g_engineInterface) return false;
    return g_engineInterface->Start();
}

/**
 * Stop data entry
 * Replaces: Java_gov_census_cspro_engine_EngineInterface_Stop
 */
void Stop() {
    if (!g_engineInterface) return;
    g_engineInterface->Stop();
}

/**
 * Get stop code
 * Replaces: Java_gov_census_cspro_engine_EngineInterface_StopCode
 */
int StopCode() {
    if (!g_engineInterface) return 0;
    return g_engineInterface->StopCode();
}

/**
 * Run user-triggered stop
 * Replaces: Java_gov_census_cspro_engine_EngineInterface_RunUserTriggeredStop
 */
void RunUserTriggeredStop() {
    if (!g_engineInterface) return;
    g_engineInterface->RunUserTriggeredStop();
}

/**
 * Get application description
 * Replaces: Java_gov_census_cspro_engine_EngineInterface_GetApplicationDescription
 */
std::string GetApplicationDescription() {
    if (!g_engineInterface) return "";
    CString desc = g_engineInterface->GetApplicationDescription();
    return std::string(desc);
}

// ============================================================
// FIELD NAVIGATION
// ============================================================

/**
 * Move to next field
 * Replaces: Java_gov_census_cspro_engine_EngineInterface_NextField
 */
void NextField() {
    if (!g_engineInterface) return;
    g_engineInterface->NextField();
}

/**
 * Move to previous field
 * Replaces: Java_gov_census_cspro_engine_EngineInterface_PrevField
 */
void PrevField() {
    if (!g_engineInterface) return;
    g_engineInterface->PreviousField();
}

/**
 * Check if form has persistent fields
 * Replaces: Java_gov_census_cspro_engine_EngineInterface_HasPersistentFields
 */
bool HasPersistentFields() {
    if (!g_engineInterface) return false;
    return g_engineInterface->HasPersistentFields();
}

/**
 * Move to previous persistent field
 * Replaces: Java_gov_census_cspro_engine_EngineInterface_PreviousPersistentField
 */
void PreviousPersistentField() {
    if (!g_engineInterface) return;
    g_engineInterface->PreviousPersistentField();
}

/**
 * Insert occurrence
 * Replaces: Java_gov_census_cspro_engine_EngineInterface_InsertOcc
 */
bool InsertOcc() {
    if (!g_engineInterface) return false;
    return g_engineInterface->InsertOcc();
}

/**
 * Insert occurrence after current
 * Replaces: Java_gov_census_cspro_engine_EngineInterface_InsertOccAfter
 */
bool InsertOccAfter() {
    if (!g_engineInterface) return false;
    return g_engineInterface->InsertOccAfter();
}

/**
 * Delete occurrence
 * Replaces: Java_gov_census_cspro_engine_EngineInterface_DeleteOcc
 */
bool DeleteOcc() {
    if (!g_engineInterface) return false;
    return g_engineInterface->DeleteOcc();
}

/**
 * Go to specific field
 * Replaces: Java_gov_census_cspro_engine_EngineInterface_GoToField
 */
void GoToField(int fieldSymbol, int index1, int index2, int index3) {
    if (!g_engineInterface) return;
    g_engineInterface->GoToField(fieldSymbol, index1, index2, index3);
}

/**
 * End current group
 * Replaces: Java_gov_census_cspro_engine_EngineInterface_EndGroup
 */
void EndGroup() {
    if (!g_engineInterface) return;
    g_engineInterface->EndGroup();
}

/**
 * Advance to end of form
 * Replaces: Java_gov_census_cspro_engine_EngineInterface_AdvanceToEnd
 */
void AdvanceToEnd() {
    if (!g_engineInterface) return;
    g_engineInterface->AdvanceToEnd();
}

/**
 * End current level
 * Replaces: Java_gov_census_cspro_engine_EngineInterface_EndLevel
 */
void EndLevel() {
    if (!g_engineInterface) return;
    g_engineInterface->EndLevel();
}

/**
 * End current level occurrence
 * Replaces: Java_gov_census_cspro_engine_EngineInterface_EndLevelOcc
 */
void EndLevelOcc() {
    if (!g_engineInterface) return;
    g_engineInterface->EndLevelOcc();
}

// ============================================================
// CASE MANAGEMENT
// ============================================================

/**
 * Insert new case
 * Replaces: Java_gov_census_cspro_engine_EngineInterface_InsertCase
 */
bool InsertCase(double casePosition) {
    if (!g_engineInterface) return false;
    return g_engineInterface->InsertCase(casePosition);
}

/**
 * Modify existing case
 * Replaces: Java_gov_census_cspro_engine_EngineInterface_ModifyCase
 */
bool ModifyCase(double casePosition) {
    if (!g_engineInterface) return false;
    return g_engineInterface->ModifyCase(casePosition);
}

/**
 * Delete case
 * Replaces: Java_gov_census_cspro_engine_EngineInterface_DeleteCase
 */
bool DeleteCase(double casePosition) {
    if (!g_engineInterface) return false;
    return g_engineInterface->DeleteCase(casePosition);
}

/**
 * Save partial case
 * Replaces: Java_gov_census_cspro_engine_EngineInterface_SavePartial
 */
void SavePartial() {
    if (!g_engineInterface) return;
    g_engineInterface->SavePartial();
}

/**
 * Check if partial save is allowed
 * Replaces: Java_gov_census_cspro_engine_EngineInterface_AllowsPartialSave
 */
bool AllowsPartialSave() {
    if (!g_engineInterface) return false;
    return g_engineInterface->AllowsPartialSave();
}

/**
 * Get sequential case IDs
 * Replaces: Java_gov_census_cspro_engine_EngineInterface_GetSequentialCaseIds
 */
val GetSequentialCaseIds() {
    val result = val::array();
    if (!g_engineInterface) return result;
    
    std::vector<CaseSummary> caseSummaries;
    bool success = g_engineInterface->GetSequentialCaseIds(caseSummaries);
    
    if (success) {
        for (const auto& summary : caseSummaries) {
            val caseObj = val::object();
            caseObj.set("caseId", std::string(summary.GetCaseIdString()));
            caseObj.set("label", std::string(summary.GetLabel()));
            caseObj.set("position", summary.GetCasePosition());
            result.call<void>("push", caseObj);
        }
    }
    
    return result;
}

// ============================================================
// FIELD VALUES
// ============================================================

/**
 * Get field value by name
 * Replaces: Java_gov_census_cspro_engine_EngineInterface_GetFieldValue
 */
std::string GetFieldValue(std::string fieldName) {
    if (!g_engineInterface) return "";
    
    CString csFieldName(fieldName.c_str());
    CString value = g_engineInterface->GetFieldValue(csFieldName);
    return std::string(value);
}

/**
 * Set field value by name
 * Replaces: Java_gov_census_cspro_engine_EngineInterface_SetFieldValue
 */
bool SetFieldValue(std::string fieldName, std::string value) {
    if (!g_engineInterface) return false;
    
    CString csFieldName(fieldName.c_str());
    CString csValue(value.c_str());
    return g_engineInterface->SetFieldValue(csFieldName, csValue);
}

// ============================================================
// OPERATOR ID
// ============================================================

/**
 * Get ask operator ID flag from PFF
 * Replaces: Java_gov_census_cspro_engine_EngineInterface_GetAskOpIDFlag
 */
bool GetAskOpIDFlag() {
    if (!g_engineInterface) return false;
    return g_engineInterface->GetPifFile()->GetAskOpIDFlag();
}

/**
 * Get operator ID from PFF
 * Replaces: Java_gov_census_cspro_engine_EngineInterface_GetOpIDFromPff
 */
std::string GetOpIDFromPff() {
    if (!g_engineInterface) return "";
    CString opID = g_engineInterface->GetOpIDFromPff();
    return std::string(opID);
}

/**
 * Set operator ID
 * Replaces: Java_gov_census_cspro_engine_EngineInterface_SetOperatorId
 */
void SetOperatorId(std::string operatorID) {
    if (!g_engineInterface) return;
    CString csOpID(operatorID.c_str());
    g_engineInterface->SetOperatorId(csOpID);
}

// ============================================================
// LOCK FLAGS
// ============================================================

bool GetAddLockFlag() {
    if (!g_engineInterface) return false;
    return g_engineInterface->GetAddLockFlag();
}

bool GetModifyLockFlag() {
    if (!g_engineInterface) return false;
    return g_engineInterface->GetModifyLockFlag();
}

bool GetDeleteLockFlag() {
    if (!g_engineInterface) return false;
    return g_engineInterface->GetDeleteLockFlag();
}

bool GetViewLockFlag() {
    if (!g_engineInterface) return false;
    return g_engineInterface->GetViewLockFlag();
}

bool GetCaseListingLockFlag() {
    if (!g_engineInterface) return false;
    return g_engineInterface->GetCaseListingLockFlag();
}

// ============================================================
// NOTES
// ============================================================

void EditCaseNote() {
    if (!g_engineInterface) return;
    g_engineInterface->EditCaseNote();
}

void ReviewNotes() {
    if (!g_engineInterface) return;
    g_engineInterface->ReviewNotes();
}

val GetAllNotes() {
    val result = val::array();
    if (!g_engineInterface) return result;
    
    // TODO: Implement notes retrieval from engine
    // This would require accessing the FieldNote structures
    
    return result;
}

void DeleteNote(int noteIndex) {
    if (!g_engineInterface) return;
    g_engineInterface->DeleteNote(noteIndex);
}

void GoToNoteField(int noteIndex) {
    if (!g_engineInterface) return;
    g_engineInterface->GoToNoteField(noteIndex);
}

// ============================================================
// LANGUAGE
// ============================================================

void ChangeLanguage() {
    if (!g_engineInterface) return;
    g_engineInterface->ChangeLanguage();
}

// ============================================================
// SYNC
// ============================================================

bool HasSync() {
    if (!g_engineInterface) return false;
    return g_engineInterface->HasSync();
}

bool SyncApp() {
    if (!g_engineInterface) return false;
    return g_engineInterface->SyncApp();
}

// ============================================================
// SYSTEM/UTILITY
// ============================================================

bool ExecSystem(std::string command) {
    if (!g_engineInterface) return false;
    CString csCommand(command.c_str());
    return g_engineInterface->ExecSystem(csCommand);
}

std::string GetStartKeyString() {
    if (!g_engineInterface) return "";
    CString key = g_engineInterface->GetStartKeyString();
    return std::string(key);
}

std::string GetStartPffKey() {
    if (!g_engineInterface) return "";
    CString key = g_engineInterface->GetStartPffKey();
    return std::string(key);
}

val QueryPffStartMode() {
    val result = val::object();
    if (!g_engineInterface) {
        result.set("action", 0);
        return result;
    }
    
    PffStartModeParameter startMode = g_engineInterface->QueryPffStartMode();
    result.set("action", startMode.action);
    return result;
}

// ============================================================
// ENTRY PAGE
// ============================================================

val GetCurrentPage(bool processPossibleRequests) {
    if (!g_engineInterface) return val::null();
    
    if (processPossibleRequests) {
        g_engineInterface->ProcessPossibleRequests();
    }
    
    CoreEntryPage* page = g_engineInterface->GetCurrentEntryPage();
    if (!page) return val::null();
    
    val pageObj = val::object();
    pageObj.set("pageNumber", page->GetPageNumber());
    // TODO: Add more page properties as needed
    
    return pageObj;
}

void OnProgressDialogCancel() {
    if (!g_engineInterface) return;
    g_engineInterface->OnProgressDialogCancel();
}

// ============================================================
// MAPPING
// ============================================================

std::string FormatCoordinates(double longitude, double latitude) {
    if (!g_engineInterface) return "";
    CString formatted = CoordinateConverter::FormatCoordinate(longitude, latitude);
    return std::string(formatted);
}

val GetMappingOptions() {
    val result = val::object();
    if (!g_engineInterface) return result;
    
    // TODO: Implement mapping options retrieval
    result.set("enabled", false);
    return result;
}

val GetBaseMapSelection() {
    val result = val::object();
    if (!g_engineInterface) return result;
    
    // TODO: Implement base map selection retrieval
    result.set("mapType", "");
    return result;
}

// ============================================================
// CASE TREE
// ============================================================

val UpdateCaseTree() {
    val result = val::array();
    if (!g_engineInterface) return result;
    
    // TODO: Implement case tree updates
    // This requires converting jobjectArray from Android version
    
    return result;
}

val GetCaseTree() {
    if (!g_engineInterface) return val::null();
    
    // TODO: Implement case tree retrieval
    // This requires converting jobject from Android version
    
    return val::null();
}

// ============================================================
// ACTION INVOKER (stub implementations for now)
// ============================================================

val RunActionInvoker(
    std::string callingPackage,
    std::string action,
    std::string accessToken,
    std::string refreshToken,
    bool abortOnException)
{
    val result = val::object();
    result.set("success", false);
    result.set("error", "ActionInvoker not yet implemented for web");
    return result;
}

int ActionInvokerCreateWebController(std::string actionInvokerAccessTokenOverride) {
    return -1; // Not implemented
}

void ActionInvokerCancelAndWaitOnActionsInProgress(int webControllerKey) {
    // Not implemented
}

std::string ActionInvokerProcessMessage(
    int webControllerKey,
    val listener,
    std::string message,
    bool async,
    bool calledByOldCSProObject)
{
    return ""; // Not implemented
}

// ============================================================
// EMSCRIPTEN BINDINGS
// ============================================================

EMSCRIPTEN_BINDINGS(cspro_android_engine) {
    // Initialization
    function("InitNativeEngineInterface", &InitNativeEngineInterface);
    function("SetEnvironmentVariables", &SetEnvironmentVariables);
    
    // Application lifecycle
    function("InitApplication", &InitApplication);
    function("EndApplication", &EndApplication);
    function("Start", &Start);
    function("Stop", &Stop);
    function("StopCode", &StopCode);
    function("RunUserTriggeredStop", &RunUserTriggeredStop);
    function("GetApplicationDescription", &GetApplicationDescription);
    
    // Field navigation
    function("NextField", &NextField);
    function("PrevField", &PrevField);
    function("HasPersistentFields", &HasPersistentFields);
    function("PreviousPersistentField", &PreviousPersistentField);
    function("InsertOcc", &InsertOcc);
    function("InsertOccAfter", &InsertOccAfter);
    function("DeleteOcc", &DeleteOcc);
    function("GoToField", &GoToField);
    function("EndGroup", &EndGroup);
    function("AdvanceToEnd", &AdvanceToEnd);
    function("EndLevel", &EndLevel);
    function("EndLevelOcc", &EndLevelOcc);
    
    // Case management
    function("InsertCase", &InsertCase);
    function("ModifyCase", &ModifyCase);
    function("DeleteCase", &DeleteCase);
    function("SavePartial", &SavePartial);
    function("AllowsPartialSave", &AllowsPartialSave);
    function("GetSequentialCaseIds", &GetSequentialCaseIds);
    
    // Field values
    function("GetFieldValue", &GetFieldValue);
    function("SetFieldValue", &SetFieldValue);
    
    // Operator ID
    function("GetAskOpIDFlag", &GetAskOpIDFlag);
    function("GetOpIDFromPff", &GetOpIDFromPff);
    function("SetOperatorId", &SetOperatorId);
    
    // Lock flags
    function("GetAddLockFlag", &GetAddLockFlag);
    function("GetModifyLockFlag", &GetModifyLockFlag);
    function("GetDeleteLockFlag", &GetDeleteLockFlag);
    function("GetViewLockFlag", &GetViewLockFlag);
    function("GetCaseListingLockFlag", &GetCaseListingLockFlag);
    
    // Notes
    function("EditCaseNote", &EditCaseNote);
    function("ReviewNotes", &ReviewNotes);
    function("GetAllNotes", &GetAllNotes);
    function("DeleteNote", &DeleteNote);
    function("GoToNoteField", &GoToNoteField);
    
    // Language
    function("ChangeLanguage", &ChangeLanguage);
    
    // Sync
    function("HasSync", &HasSync);
    function("SyncApp", &SyncApp);
    
    // System/Utility
    function("ExecSystem", &ExecSystem);
    function("GetStartKeyString", &GetStartKeyString);
    function("GetStartPffKey", &GetStartPffKey);
    function("QueryPffStartMode", &QueryPffStartMode);
    
    // Entry page
    function("GetCurrentPage", &GetCurrentPage);
    function("OnProgressDialogCancel", &OnProgressDialogCancel);
    
    // Mapping
    function("FormatCoordinates", &FormatCoordinates);
    function("GetMappingOptions", &GetMappingOptions);
    function("GetBaseMapSelection", &GetBaseMapSelection);
    
    // Case tree
    function("UpdateCaseTree", &UpdateCaseTree);
    function("GetCaseTree", &GetCaseTree);
    
    // Action Invoker (stubs)
    function("RunActionInvoker", &RunActionInvoker);
    function("ActionInvokerCreateWebController", &ActionInvokerCreateWebController);
    function("ActionInvokerCancelAndWaitOnActionsInProgress", &ActionInvokerCancelAndWaitOnActionsInProgress);
    function("ActionInvokerProcessMessage", &ActionInvokerProcessMessage);
}
