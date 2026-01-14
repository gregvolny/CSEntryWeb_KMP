/**
 * WebWASMBindings_full.cpp
 * 
 * Full Emscripten Embind bindings for WebEngineInterface
 * This replaces JNI bindings with JavaScript-callable functions
 * 
 * Uses WebEngineInterface which has no JNI dependencies
 * Target: Kotlin/Wasm frontend via CSProWasmModule.kt
 */

// Include StandardSystemIncludes.h first to get all necessary macros and types
#include <engine/StandardSystemIncludes.h>

#include <emscripten/bind.h>
#include <emscripten/val.h>
#include "WebEngineInterface.h"
#include "WebApplicationInterface.h"
#include <Zentryo/CoreEntryPage.h>
#include <Zentryo/CoreEntryPageField.h>
#include <zCaseO/CaseSummary.h>
#include <ZBRIDGEO/npff.h>
#include <zToolsO/Utf8Convert.h>
#include <sstream>
#include <iomanip>

using namespace emscripten;

// Global engine interface instance
static WebEngineInterface* g_engineInterface = nullptr;

// ============================================================
// JSON Helpers (simple implementation without jsoncpp)
// ============================================================

static std::string EscapeJsonString(const std::string& s) {
    std::ostringstream o;
    for (char c : s) {
        switch (c) {
            case '"':  o << "\\\""; break;
            case '\\': o << "\\\\"; break;
            case '\b': o << "\\b";  break;
            case '\f': o << "\\f";  break;
            case '\n': o << "\\n";  break;
            case '\r': o << "\\r";  break;
            case '\t': o << "\\t";  break;
            default:
                if ('\x00' <= c && c <= '\x1f') {
                    o << "\\u" << std::hex << std::setw(4) << std::setfill('0') << (int)c;
                } else {
                    o << c;
                }
        }
    }
    return o.str();
}

static std::string CStringToJsonString(const CString& cs) {
    std::string utf8 = UTF8Convert::WideToUTF8(cs);
    return "\"" + EscapeJsonString(utf8) + "\"";
}

// ============================================================
// INITIALIZATION
// ============================================================

/**
 * Initialize the native engine interface
 */
val InitNativeEngineInterface() {
    if (!g_engineInterface) {
        g_engineInterface = new WebEngineInterface();
    }
    return val(reinterpret_cast<long>(g_engineInterface));
}

/**
 * Set web environment variables
 */
void SetEnvironmentVariables(
    std::string email,
    std::string tempFolder,
    std::string applicationFolder,
    std::string versionNumber,
    std::string assetsDirectory,
    std::string csEntryDirectory,
    std::string opfsRootPath,
    std::string downloadsDirectory)
{
    if (!g_engineInterface) return;
    
    g_engineInterface->SetWebEnvironmentVariables(
        CString(email.c_str()),
        CString(tempFolder.c_str()),
        CString(applicationFolder.c_str()),
        CString(versionNumber.c_str()),
        CString(assetsDirectory.c_str()),
        CString(csEntryDirectory.c_str()),
        CString(opfsRootPath.c_str()),
        CString(downloadsDirectory.c_str())
    );
}

// ============================================================
// APPLICATION LIFECYCLE
// ============================================================

bool InitApplication(std::string pffFilename) {
    if (!g_engineInterface) return false;
    return g_engineInterface->InitApplication(CString(pffFilename.c_str()));
}

void EndApplication() {
    if (!g_engineInterface) return;
    g_engineInterface->EndApplication();
}

bool Start() {
    if (!g_engineInterface) return false;
    return g_engineInterface->Start();
}

bool ModifyCase(double positionInRepository) {
    if (!g_engineInterface) return false;
    return g_engineInterface->ModifyCase(positionInRepository);
}

bool InsertCase(double insertBeforePosition) {
    if (!g_engineInterface) return false;
    return g_engineInterface->InsertCase(insertBeforePosition);
}

bool DeleteCase(double positionInRepository) {
    if (!g_engineInterface) return false;
    return g_engineInterface->DeleteCase(positionInRepository);
}

// ============================================================
// FIELD NAVIGATION - Using real CoreEntryPage API
// ============================================================

std::string GetCurrentEntryPageJson() {
    if (!g_engineInterface) return "{}";
    
    CoreEntryPage* page = g_engineInterface->GetCurrentEntryPage();
    if (!page) return "{}";
    
    std::ostringstream json;
    json << "{";
    
    // Block info
    json << "\"blockName\":" << CStringToJsonString(page->GetBlockName()) << ",";
    json << "\"blockLabel\":" << CStringToJsonString(page->GetBlockLabel()) << ",";
    json << "\"occurrenceLabel\":" << CStringToJsonString(page->GetOccurrenceLabel()) << ",";
    
    // Question/Help URLs
    auto questionUrl = page->GetBlockQuestionTextUrl();
    if (questionUrl) {
        json << "\"questionTextUrl\":\"" << EscapeJsonString(UTF8Convert::WideToUTF8(*questionUrl)) << "\",";
    }
    
    // Fields array
    const auto& pageFields = page->GetPageFields();
    json << "\"fieldCount\":" << pageFields.size() << ",";
    json << "\"fields\":[";
    
    bool first = true;
    for (const auto& field : pageFields) {
        if (!first) json << ",";
        first = false;
        
        json << "{";
        json << "\"name\":" << CStringToJsonString(field.GetName()) << ",";
        json << "\"label\":" << CStringToJsonString(field.GetLabel()) << ",";
        json << "\"symbol\":" << field.GetSymbol() << ",";
        json << "\"isReadOnly\":" << (field.IsReadOnly() ? "true" : "false") << ",";
        json << "\"isMirror\":" << (field.IsMirror() ? "true" : "false") << ",";
        json << "\"isNumeric\":" << (field.IsNumeric() ? "true" : "false") << ",";
        
        if (field.IsNumeric()) {
            json << "\"integerLength\":" << field.GetIntegerPartLength() << ",";
            json << "\"decimalLength\":" << field.GetFractionalPartLength() << ",";
            json << "\"numericValue\":" << field.GetNumericValue();
        } else {
            json << "\"alphaLength\":" << field.GetAlphaLength() << ",";
            json << "\"alphaValue\":" << CStringToJsonString(field.GetAlphaValue());
        }
        
        // Capture type info
        const CaptureInfo& captureInfo = field.GetEvaluatedCaptureInfo();
        json << ",\"captureType\":" << static_cast<int>(captureInfo.GetCaptureType());
        
        // Note
        CString note = field.GetNote();
        if (!note.IsEmpty()) {
            json << ",\"note\":" << CStringToJsonString(note);
        }
        
        json << "}";
    }
    json << "]";
    
    json << "}";
    return json.str();
}

std::string NextField() {
    if (!g_engineInterface) return "{}";
    g_engineInterface->NextField();
    return GetCurrentEntryPageJson();
}

std::string PreviousField() {
    if (!g_engineInterface) return "{}";
    g_engineInterface->PreviousField();
    return GetCurrentEntryPageJson();
}

std::string GoToField(std::string fieldName) {
    if (!g_engineInterface) return "{}";
    // TODO: Implement field lookup by name
    return GetCurrentEntryPageJson();
}

std::string SetFieldValueAndAdvance(std::string value) {
    if (!g_engineInterface) return "{}";
    CoreEntryPage* page = g_engineInterface->SetFieldValueAndAdvance(CString(value.c_str()));
    return GetCurrentEntryPageJson();
}

std::string EndGroup() {
    if (!g_engineInterface) return "{}";
    CoreEntryPage* page = g_engineInterface->EndGroup();
    return GetCurrentEntryPageJson();
}

std::string EndLevel() {
    if (!g_engineInterface) return "{}";
    CoreEntryPage* page = g_engineInterface->EndLevel();
    return GetCurrentEntryPageJson();
}

std::string AdvanceToEnd() {
    if (!g_engineInterface) return "{}";
    CoreEntryPage* page = g_engineInterface->AdvanceToEnd();
    return GetCurrentEntryPageJson();
}

// ============================================================
// OCCURRENCE MANAGEMENT
// ============================================================

bool InsertOcc() {
    if (!g_engineInterface) return false;
    return g_engineInterface->InsertOcc();
}

bool InsertOccAfter() {
    if (!g_engineInterface) return false;
    return g_engineInterface->InsertOccAfter();
}

bool DeleteOcc() {
    if (!g_engineInterface) return false;
    return g_engineInterface->DeleteOcc();
}

// ============================================================
// CASE MANAGEMENT
// ============================================================

std::string GetCaseListJson(bool sortAscending) {
    if (!g_engineInterface) return "[]";
    
    auto cases = g_engineInterface->GetSequentialCaseIds(sortAscending);
    
    std::ostringstream json;
    json << "[";
    bool first = true;
    for (const auto& cs : cases) {
        if (!first) json << ",";
        first = false;
        
        json << "{";
        json << "\"id\":" << CStringToJsonString(cs.case_summary.GetKey()) << ",";
        json << "\"label\":" << CStringToJsonString(cs.case_summary.GetCaseLabelOrKey()) << ",";
        json << "\"position\":" << cs.case_summary.GetPositionInRepository() << ",";
        json << "\"isPartial\":" << (cs.case_summary.IsPartial() ? "true" : "false") << ",";
        json << "\"latitude\":" << cs.latitude << ",";
        json << "\"longitude\":" << cs.longitude;
        json << "}";
    }
    json << "]";
    
    return json.str();
}

void PartialSave() {
    if (!g_engineInterface) return;
    g_engineInterface->PartialSave();
}

void Save() {
    if (!g_engineInterface) return;
    // Note: There's no direct Save() method - use PartialSave or OnStop
    g_engineInterface->PartialSave(false, false);
}

void Cancel() {
    if (!g_engineInterface) return;
    g_engineInterface->RunUserTriggedStop();
}

// ============================================================
// CASE TREE
// ============================================================

std::string GetCaseTree() {
    if (!g_engineInterface) return "[]";
    return g_engineInterface->GetCaseTreeJson();
}

std::string UpdateCaseTree() {
    if (!g_engineInterface) return "[]";
    return g_engineInterface->UpdateCaseTreeJson();
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
// APPLICATION INFO
// ============================================================

std::string GetApplicationDescription() {
    if (!g_engineInterface) return "";
    CString desc = g_engineInterface->GetApplicationDescription();
    return UTF8Convert::WideToUTF8(desc);
}

std::string GetOperatorIdFromPff() {
    if (!g_engineInterface) return "";
    CString opId = g_engineInterface->GetOpIDFromPff();
    return UTF8Convert::WideToUTF8(opId);
}

std::string GetStartKey() {
    if (!g_engineInterface) return "";
    CString key = g_engineInterface->GetStartKeyString();
    return UTF8Convert::WideToUTF8(key);
}

bool GetAskOperatorIdFlag() {
    if (!g_engineInterface) return false;
    return g_engineInterface->GetAskOpIDFlag();
}

void SetOperatorId(std::string operatorId) {
    if (!g_engineInterface) return;
    g_engineInterface->SetOperatorId(CString(operatorId.c_str()));
}

bool IsSystemControlled() {
    if (!g_engineInterface) return true;
    return g_engineInterface->IsSystemControlled();
}

bool ContainsMultipleLanguages() {
    if (!g_engineInterface) return false;
    return g_engineInterface->ContainsMultipleLanguages();
}

void ChangeLanguage() {
    if (!g_engineInterface) return;
    g_engineInterface->ChangeLanguage();
}

// ============================================================
// ENGINE CONTROL
// ============================================================

int GetStopCode() {
    if (!g_engineInterface) return 0;
    return g_engineInterface->GetStopCode();
}

void OnStop() {
    if (!g_engineInterface) return;
    g_engineInterface->OnStop();
}

void RunUserbarFunction(int userbarIndex) {
    if (!g_engineInterface) return;
    g_engineInterface->RunUserbarFunction(userbarIndex);
}

void OnProgressDialogCancel() {
    if (!g_engineInterface) return;
    g_engineInterface->OnProgressDialogCancel();
}

// ============================================================
// PARADATA
// ============================================================

void GetParadataCachedEvents() {
    if (!g_engineInterface) return;
    g_engineInterface->GetParadataCachedEvents();
}

// ============================================================
// EMSCRIPTEN BINDINGS
// ============================================================

EMSCRIPTEN_BINDINGS(CSProModule) {
    // Initialization
    function("InitNativeEngineInterface", &InitNativeEngineInterface);
    function("SetEnvironmentVariables", &SetEnvironmentVariables);
    
    // Application Lifecycle
    function("InitApplication", &InitApplication);
    function("EndApplication", &EndApplication);
    function("Start", &Start);
    function("ModifyCase", &ModifyCase);
    function("InsertCase", &InsertCase);
    function("DeleteCase", &DeleteCase);
    
    // Field Navigation
    function("GetCurrentEntryPageJson", &GetCurrentEntryPageJson);
    function("NextField", &NextField);
    function("PreviousField", &PreviousField);
    function("GoToField", &GoToField);
    function("SetFieldValueAndAdvance", &SetFieldValueAndAdvance);
    function("EndGroup", &EndGroup);
    function("EndLevel", &EndLevel);
    function("AdvanceToEnd", &AdvanceToEnd);
    
    // Occurrence Management
    function("InsertOcc", &InsertOcc);
    function("InsertOccAfter", &InsertOccAfter);
    function("DeleteOcc", &DeleteOcc);
    
    // Case Management
    function("GetCaseListJson", &GetCaseListJson);
    function("PartialSave", &PartialSave);
    function("Save", &Save);
    function("Cancel", &Cancel);
    
    // Case Tree
    function("GetCaseTree", &GetCaseTree);
    function("UpdateCaseTree", &UpdateCaseTree);
    
    // Lock Flags
    function("GetAddLockFlag", &GetAddLockFlag);
    function("GetModifyLockFlag", &GetModifyLockFlag);
    function("GetDeleteLockFlag", &GetDeleteLockFlag);
    function("GetViewLockFlag", &GetViewLockFlag);
    function("GetCaseListingLockFlag", &GetCaseListingLockFlag);
    
    // Application Info
    function("GetApplicationDescription", &GetApplicationDescription);
    function("GetOperatorIdFromPff", &GetOperatorIdFromPff);
    function("GetStartKey", &GetStartKey);
    function("GetAskOperatorIdFlag", &GetAskOperatorIdFlag);
    function("SetOperatorId", &SetOperatorId);
    function("IsSystemControlled", &IsSystemControlled);
    function("ContainsMultipleLanguages", &ContainsMultipleLanguages);
    function("ChangeLanguage", &ChangeLanguage);
    
    // Engine Control
    function("GetStopCode", &GetStopCode);
    function("OnStop", &OnStop);
    function("RunUserbarFunction", &RunUserbarFunction);
    function("OnProgressDialogCancel", &OnProgressDialogCancel);
    
    // Paradata
    function("GetParadataCachedEvents", &GetParadataCachedEvents);
}
