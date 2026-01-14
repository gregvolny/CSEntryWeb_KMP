/**
 * WebEngineInterface.cpp
 * 
 * Web-specific engine interface implementation
 * Based on AndroidEngineInterface.cpp but without JNI dependencies
 * 
 * Uses PlatformInterface for environment configuration (same as Android)
 * Uses CoreEntryEngineInterface base class for engine operations
 */

// Include StandardSystemIncludes.h first to get all necessary macros and types
#include <engine/StandardSystemIncludes.h>

#include "WebEngineInterface.h"
#include "WebApplicationInterface.h"
#include "WebUserbar.h"
#include <zPlatformO/PlatformInterface.h>
#include <ZBRIDGEO/npff.h>
#include <Zentryo/CaseTreeNode.h>
#include <Zentryo/CaseTreeUpdate.h>
#include <zToolsO/Utf8Convert.h>
#include <sstream>
#include <iomanip>

#ifdef __EMSCRIPTEN__
#include <emscripten.h>
#endif

// ============================================================
// Constructor / Destructor
// ============================================================

WebEngineInterface::WebEngineInterface()
    : m_pApplicationInterface(new WebApplicationInterface(this))
{
    // Initialize CSPro environment (same as Android)
    InitializeCSProEnvironment();
    
    // Register with platform interface (same pattern as Android)
    PlatformInterface::GetInstance()->SetApplicationInterface(m_pApplicationInterface);
}

WebEngineInterface::~WebEngineInterface()
{
    // Unregister from platform interface
    PlatformInterface::GetInstance()->SetApplicationInterface(nullptr);
    
    delete m_pApplicationInterface;
    m_pApplicationInterface = nullptr;
}

// ============================================================
// Field Navigation
// ============================================================

CoreEntryPage* WebEngineInterface::GoToField(int fieldSymbol, int index1, int index2, int index3)
{
    int index[3] = { index1, index2, index3 };
    return CoreEntryEngineInterface::GoToField(fieldSymbol, index);
}

// ============================================================
// Application Lifecycle
// ============================================================

void WebEngineInterface::EndApplication()
{
    OnStop();
    Cleanup();
}

// ============================================================
// PFF Properties (identical to Android implementation)
// ============================================================

CString WebEngineInterface::GetOpIDFromPff()
{
    return GetPifFile()->GetOpID();
}

bool WebEngineInterface::GetAddLockFlag()
{
    return GetPifFile()->GetAddLockFlag();
}

bool WebEngineInterface::GetModifyLockFlag()
{
    return GetPifFile()->GetModifyLockFlag();
}

bool WebEngineInterface::GetDeleteLockFlag()
{
    return GetPifFile()->GetDeleteLockFlag();
}

bool WebEngineInterface::GetViewLockFlag()
{
    return GetPifFile()->GetViewLockFlag();
}

bool WebEngineInterface::GetCaseListingLockFlag()
{
    return GetPifFile()->GetCaseListingLockFlag();
}

// ============================================================
// Environment Configuration
// ============================================================

void WebEngineInterface::SetWebEnvironmentVariables(
    const CString& email,
    const CString& tempFolder,
    const CString& applicationFolder,
    const CString& versionNumber,
    const CString& assetsDirectory,
    const CString& csEntryFolder,
    const CString& opfsRootPath,
    const CString& downloadsDirectory)
{
    // Set username on application interface
    m_pApplicationInterface->SetUsername(email);
    
    // Configure paths via PlatformInterface (same as Android)
    if (!tempFolder.IsEmpty())
        PlatformInterface::GetInstance()->SetTempDirectory(PortableFunctions::PathEnsureTrailingSlash(CS2WS(tempFolder)));
    
    PlatformInterface::GetInstance()->SetApplicationDirectory(CS2WS(applicationFolder));
    PlatformInterface::GetInstance()->SetCSEntryDirectory(CS2WS(csEntryFolder));
    
    // Use opfsRootPath as internal storage for web (analogous to Android's internal storage)
    PlatformInterface::GetInstance()->SetInternalStorageDirectory(CS2WS(opfsRootPath));
    
    PlatformInterface::GetInstance()->SetAssetsDirectory(CS2WS(assetsDirectory));
    PlatformInterface::GetInstance()->SetDownloadsDirectory(CS2WS(downloadsDirectory));
    PlatformInterface::GetInstance()->SetVersionNumber(CS2WS(versionNumber));
}

// ============================================================
// Application Info (identical to Android implementation)
// ============================================================

CString WebEngineInterface::GetStartKeyString()
{
    return GetPifFile()->GetStartKeyString();
}

CString WebEngineInterface::GetApplicationDescription() const
{
    return GetPifFile()->GetEvaluatedAppDescription();
}

// ============================================================
// Userbar
// ============================================================

void WebEngineInterface::RunUserbarFunction(int userbar_index)
{
    // Get userbar from the running application
    Userbar* userbar = GetRunAplEntry()->GetUserbar();
    if (userbar == nullptr)
        return;
    
    // Cast to WebUserbar (same pattern as Android)
    WebUserbar* webUserbar = dynamic_cast<WebUserbar*>(userbar);
    if (webUserbar == nullptr)
        return;
    
    // Get the function for this button and execute it
    UserFunctionArgumentEvaluator* user_function_argument_evaluator = 
        webUserbar->GetFunctionForButton(userbar_index);
    
    if (user_function_argument_evaluator != nullptr)
        ExecuteCallbackUserFunction(*user_function_argument_evaluator);
}

// ============================================================
// Paradata
// ============================================================

void WebEngineInterface::GetParadataCachedEvents()
{
    m_pApplicationInterface->GetParadataCachedEvents();
}

// ============================================================
// Progress Dialog
// ============================================================

void WebEngineInterface::OnProgressDialogCancel()
{
    m_pApplicationInterface->OnProgressDialogCancel();
}

// ============================================================
// Case Tree - JSON serialization for web (replaces JNI jobject)
// ============================================================

// JSON helper function
static std::string EscapeJson(const std::string& s) {
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
            default:   o << c;
        }
    }
    return o.str();
}

static std::string CStringToJson(const CString& cs) {
    std::string utf8 = UTF8Convert::WideToUTF8(cs);
    return "\"" + EscapeJson(utf8) + "\"";
}

static std::string WStringToJson(const std::wstring& ws) {
    std::string utf8 = UTF8Convert::WideToUTF8(ws);
    return "\"" + EscapeJson(utf8) + "\"";
}

static std::string CaseTreeNodeToJsonString(const std::shared_ptr<CaseTreeNode>& node);
static std::string CaseTreeToJsonString(const std::shared_ptr<CaseTreeNode>& node);

static std::string CaseTreeNodeToJsonString(const std::shared_ptr<CaseTreeNode>& node)
{
    std::ostringstream json;
    json << "{";
    json << "\"id\":" << node->getId() << ",";
    json << "\"name\":" << CStringToJson(node->getName()) << ",";
    json << "\"label\":" << CStringToJson(node->getLabel()) << ",";
    json << "\"value\":" << CStringToJson(node->getValue()) << ",";
    json << "\"type\":" << static_cast<int>(node->getType()) << ",";
    json << "\"fieldSymbol\":" << node->getFieldSymbol() << ",";
    json << "\"visible\":" << (node->getVisible() ? "true" : "false") << ",";
    
    // Color mapping (same as Android)
    int color;
    switch (node->getColor()) {
        case CaseTreeNode::Color::PARENT:       color = -1; break;
        case CaseTreeNode::Color::NEVERVISITED: color = 0;  break;
        case CaseTreeNode::Color::SKIPPED:      color = 1;  break;
        case CaseTreeNode::Color::VISITED:      color = 2;  break;
        case CaseTreeNode::Color::CURRENT:      color = 3;  break;
        case CaseTreeNode::Color::PROTECTED:    color = 4;  break;
        default:                                color = 0;  break;
    }
    json << "\"color\":" << color << ",";
    
    // Index array
    json << "\"index\":[";
    bool first = true;
    for (int idx : node->getIndex()) {
        if (!first) json << ",";
        first = false;
        json << idx;
    }
    json << "]";
    
    json << "}";
    return json.str();
}

static std::string CaseTreeToJsonString(const std::shared_ptr<CaseTreeNode>& node)
{
    std::ostringstream json;
    json << "{";
    json << "\"id\":" << node->getId() << ",";
    json << "\"name\":" << CStringToJson(node->getName()) << ",";
    json << "\"label\":" << CStringToJson(node->getLabel()) << ",";
    json << "\"value\":" << CStringToJson(node->getValue()) << ",";
    json << "\"type\":" << static_cast<int>(node->getType()) << ",";
    json << "\"fieldSymbol\":" << node->getFieldSymbol() << ",";
    json << "\"visible\":" << (node->getVisible() ? "true" : "false") << ",";
    
    int color;
    switch (node->getColor()) {
        case CaseTreeNode::Color::PARENT:       color = -1; break;
        case CaseTreeNode::Color::NEVERVISITED: color = 0;  break;
        case CaseTreeNode::Color::SKIPPED:      color = 1;  break;
        case CaseTreeNode::Color::VISITED:      color = 2;  break;
        case CaseTreeNode::Color::CURRENT:      color = 3;  break;
        case CaseTreeNode::Color::PROTECTED:    color = 4;  break;
        default:                                color = 0;  break;
    }
    json << "\"color\":" << color << ",";
    
    json << "\"index\":[";
    bool first = true;
    for (int idx : node->getIndex()) {
        if (!first) json << ",";
        first = false;
        json << idx;
    }
    json << "],";
    
    // Recursively add children
    json << "\"children\":[";
    first = true;
    for (const auto& childNode : node->getChildren()) {
        if (!first) json << ",";
        first = false;
        json << CaseTreeToJsonString(childNode);
    }
    json << "]";
    
    json << "}";
    return json.str();
}

std::string WebEngineInterface::GetCaseTreeJson()
{
    std::shared_ptr<CaseTreeNode> pTree = GetCaseTree();
    if (!pTree) {
        return "null";
    }
    
    return CaseTreeToJsonString(pTree);
}

std::string WebEngineInterface::UpdateCaseTreeJson()
{
    std::vector<CaseTreeUpdate> updates = UpdateCaseTree();
    if (updates.empty()) {
        return "[]";
    }
    
    std::ostringstream json;
    json << "[";
    bool first = true;
    
    for (const auto& update : updates) {
        if (!first) json << ",";
        first = false;
        
        json << "{";
        
        switch (update.getType()) {
            case CaseTreeUpdate::UpdateType::NODE_MODIFIED:
                json << "\"type\":1,";  // NODE_MODIFIED
                json << "\"parentId\":0,";
                json << "\"childIndex\":-1,";
                json << "\"node\":" << CaseTreeNodeToJsonString(update.getNode());
                break;
                
            case CaseTreeUpdate::UpdateType::NODE_ADDED:
                json << "\"type\":2,";  // NODE_ADDED
                json << "\"parentId\":" << update.getNode()->getId() << ",";
                json << "\"childIndex\":" << update.getChildIndex() << ",";
                json << "\"node\":" << CaseTreeToJsonString(update.getNode()->getChildren()[update.getChildIndex()]);
                break;
                
            case CaseTreeUpdate::UpdateType::NODE_REMOVED:
                json << "\"type\":3,";  // NODE_REMOVED
                json << "\"parentId\":" << update.getNode()->getId() << ",";
                json << "\"childIndex\":" << update.getChildIndex();
                break;
        }
        
        json << "}";
    }
    
    json << "]";
    return json.str();
}
