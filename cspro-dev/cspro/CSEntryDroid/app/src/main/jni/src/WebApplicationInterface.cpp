/**
 * WebApplicationInterface.cpp
 * 
 * Implementation of Web platform APIs
 * These will be called from C++ but the actual implementation happens in JavaScript/Kotlin
 */

// Include StandardSystemIncludes.h first to get all necessary macros and types
#include <engine/StandardSystemIncludes.h>

#include "WebApplicationInterface.h"
#include "WebUserbar.h"
#include <emscripten.h>
#include <emscripten/val.h>
#include <zToolsO/PortableFunctions.h>
#include <zToolsO/Utf8Convert.h>

using namespace emscripten;

WebApplicationInterface::WebApplicationInterface(CoreEntryEngineInterface* core_interface)
    : BaseApplicationInterface()
    , m_coreInterface(core_interface)
    , m_progressDialogCancelled(false)
{
}

WebApplicationInterface::~WebApplicationInterface()
{
}

// ============================================================
// DISPLAY & UI
// ============================================================

ObjectTransporter* WebApplicationInterface::GetObjectTransporter() {
    // Not needed for web - Kotlin/JS handles object transport
    return nullptr;
}

void WebApplicationInterface::RefreshPage(RefreshPageContents contents) {
    // Call JavaScript to refresh the UI
    EM_ASM({
        if (window.csproRefreshPage) {
            window.csproRefreshPage($0);
        }
    }, static_cast<int>(contents));
}

void WebApplicationInterface::DisplayErrorMessage(const TCHAR* error_message) {
    // Call JavaScript alert or custom error dialog
    std::string msg = UTF8Convert::WideToUTF8(error_message);
    EM_ASM({
        if (window.csproShowError) {
            window.csproShowError(UTF8ToString($0));
        } else {
            console.error('CSPro Error:', UTF8ToString($0));
        }
    }, msg.c_str());
}

std::optional<std::wstring> WebApplicationInterface::DisplayCSHtmlDlg(
    const NavigationAddress& navigation_address,
    const std::wstring* action_invoker_access_token_override)
{
    // HTML dialogs will be handled by Kotlin/JS
    return std::nullopt;
}

std::optional<std::wstring> WebApplicationInterface::DisplayHtmlDialogFunctionDlg(
    const NavigationAddress& navigation_address,
    const std::wstring* action_invoker_access_token_override,
    const std::optional<std::wstring>& display_options_json)
{
    return std::nullopt;
}

int WebApplicationInterface::ShowModalDialog(NullTerminatedString title, NullTerminatedString message, int mbType) {
    // Use browser confirm/alert
    std::string sTitle = UTF8Convert::WideToUTF8(title);
    std::string sMessage = UTF8Convert::WideToUTF8(message);
    
    int result = EM_ASM_INT({
        var title = UTF8ToString($0);
        var message = UTF8ToString($1);
        var mbType = $2;
        
        if (window.csproShowModalDialog) {
            return window.csproShowModalDialog(title, message, mbType);
        } else {
            // Default: use browser confirm
            return confirm(title + "\\n\\n" + message) ? 1 : 0;
        }
    }, sTitle.c_str(), sMessage.c_str(), mbType);
    
    return result;
}

int WebApplicationInterface::ShowMessage(const CString& title, const CString& message, const std::vector<CString>& aButtons) {
    // Custom button dialog - delegate to Kotlin/JS
    return 0;
}

std::tuple<int, int> WebApplicationInterface::GetMaxDisplaySize() const {
    int width = EM_ASM_INT({ return window.innerWidth; });
    int height = EM_ASM_INT({ return window.innerHeight; });
    return std::make_tuple(width, height);
}

// ============================================================
// GPS (Web Geolocation API)
// ============================================================

bool WebApplicationInterface::GpsOpen() {
    // Web Geolocation API doesn't require explicit open
    return true;
}

bool WebApplicationInterface::GpsClose() {
    return true;
}

CString WebApplicationInterface::GpsRead(int waitTime, int accuracy, const CString& dialog_text) {
    // This should be called from Kotlin/JS using navigator.geolocation
    // For now, return empty - actual implementation happens in WasmPlatformServices.kt
    return CString("");
}

CString WebApplicationInterface::GpsReadLast() {
    return CString("");
}

CString WebApplicationInterface::GpsReadInteractive(bool read_interactive_mode, const BaseMapSelection& base_map_selection, const CString& message, double read_duration) {
    return CString("");
}

// ============================================================
// AUTHENTICATION & CREDENTIALS
// ============================================================

std::optional<BaseApplicationInterface::LoginCredentials> WebApplicationInterface::ShowLoginDialog(const CString& server, bool show_invalid_error) {
    // Delegate to Kotlin/JS form
    return std::nullopt;
}

std::optional<BluetoothDeviceInfo> WebApplicationInterface::ChooseBluetoothDevice(const GUID& service_uuid) {
    // Web Bluetooth API - delegate to Kotlin/JS
    return std::nullopt;
}

CString WebApplicationInterface::AuthorizeDropbox(const CString& clientId) {
    // OAuth flow - delegate to Kotlin/JS
    return CString("");
}

CString WebApplicationInterface::GetUsername() const {
    return m_username;
}

void WebApplicationInterface::SetUsername(const CString& username) {
    m_username = username;
}

void WebApplicationInterface::StoreCredential(const std::wstring& attribute, const std::wstring& secret_value) {
    // Use localStorage or IndexedDB via EM_ASM
    std::string attr = UTF8Convert::WideToUTF8(attribute);
    std::string value = UTF8Convert::WideToUTF8(secret_value);
    
    EM_ASM({
        if (window.localStorage) {
            localStorage.setItem(UTF8ToString($0), UTF8ToString($1));
        }
    }, attr.c_str(), value.c_str());
}

std::wstring WebApplicationInterface::RetrieveCredential(const std::wstring& attribute) {
    std::string attr = UTF8Convert::WideToUTF8(attribute);
    
    char* result = (char*)EM_ASM_INT({
        if (window.localStorage) {
            var value = localStorage.getItem(UTF8ToString($0));
            if (value) {
                var len = lengthBytesUTF8(value) + 1;
                var ptr = _malloc(len);
                stringToUTF8(value, ptr, len);
                return ptr;
            }
        }
        return 0;
    }, attr.c_str());
    
    if (result) {
        std::wstring wresult = CS2WS(CString(result));
        free(result);
        return wresult;
    }
    
    return L"";
}

std::optional<std::wstring> WebApplicationInterface::GetPassword(const std::wstring& title, const std::wstring& description, bool file_exists) {
    // Password dialog - delegate to Kotlin/JS
    return std::nullopt;
}

// ============================================================
// DEVICE INFO
// ============================================================

CString WebApplicationInterface::GetDeviceId() const {
    // Generate/retrieve browser fingerprint
    char* result = (char*)EM_ASM_INT({
        var deviceId = localStorage.getItem('csproDeviceId');
        if (!deviceId) {
            deviceId = 'web-' + Math.random().toString(36).substr(2, 9);
            localStorage.setItem('csproDeviceId', deviceId);
        }
        var len = lengthBytesUTF8(deviceId) + 1;
        var ptr = _malloc(len);
        stringToUTF8(deviceId, ptr, len);
        return ptr;
    });
    
    CString deviceId(result);
    free(result);
    return deviceId;
}

CString WebApplicationInterface::GetLocaleLanguage() const {
    char* result = (char*)EM_ASM_INT({
        var lang = navigator.language || 'en-US';
        var len = lengthBytesUTF8(lang) + 1;
        var ptr = _malloc(len);
        stringToUTF8(lang, ptr, len);
        return ptr;
    });
    
    CString lang(result);
    free(result);
    return lang;
}

// ============================================================
// SYSTEM OPERATIONS
// ============================================================

void WebApplicationInterface::EngineAbort() {
    // Terminate execution
    EM_ASM({ throw new Error('CSPro engine aborted'); });
}

bool WebApplicationInterface::ExecSystem(const std::wstring& command, bool wait) {
    // Most system commands don't make sense in web context
    // Allow specific web-safe commands via Kotlin/JS
    return false;
}

bool WebApplicationInterface::ExecPff(const std::wstring& pff_filename) {
    // Execute another PFF - delegate to Kotlin/JS
    return false;
}

// ============================================================
// PROPERTIES
// ============================================================

CString WebApplicationInterface::GetProperty(const CString& parameter) {
    // Store properties in localStorage
    std::string param = UTF8Convert::WideToUTF8(std::wstring_view(parameter.GetString(), parameter.GetLength()));
    
    char* result = (char*)EM_ASM_INT({
        var key = 'cspro_prop_' + UTF8ToString($0);
        var value = localStorage.getItem(key);
        if (value) {
            var len = lengthBytesUTF8(value) + 1;
            var ptr = _malloc(len);
            stringToUTF8(value, ptr, len);
            return ptr;
        }
        return 0;
    }, param.c_str());
    
    if (result) {
        CString value(result);
        free(result);
        return value;
    }
    
    return CString("");
}

void WebApplicationInterface::SetProperty(const CString& parameter, const CString& value) {
    std::string param = UTF8Convert::WideToUTF8(std::wstring_view(parameter.GetString(), parameter.GetLength()));
    std::string val = UTF8Convert::WideToUTF8(std::wstring_view(value.GetString(), value.GetLength()));
    
    EM_ASM({
        var key = 'cspro_prop_' + UTF8ToString($0);
        localStorage.setItem(key, UTF8ToString($1));
    }, param.c_str(), val.c_str());
}

// ============================================================
// PROGRESS DIALOGS
// ============================================================

void WebApplicationInterface::ShowProgressDialog(const CString& message) {
    m_progressDialogCancelled = false;
    std::string msg = UTF8Convert::WideToUTF8(std::wstring_view(message.GetString(), message.GetLength()));
    
    EM_ASM({
        if (window.csproShowProgress) {
            window.csproShowProgress(UTF8ToString($0));
        }
    }, msg.c_str());
}

void WebApplicationInterface::HideProgressDialog() {
    EM_ASM({
        if (window.csproHideProgress) {
            window.csproHideProgress();
        }
    });
}

bool WebApplicationInterface::UpdateProgressDialog(int progressPercent, const CString* message) {
    if (message) {
        std::string msg = UTF8Convert::WideToUTF8(std::wstring_view(message->GetString(), message->GetLength()));
        EM_ASM({
            if (window.csproUpdateProgress) {
                window.csproUpdateProgress($0, UTF8ToString($1));
            }
        }, progressPercent, msg.c_str());
    } else {
        EM_ASM({
            if (window.csproUpdateProgress) {
                window.csproUpdateProgress($0, null);
            }
        }, progressPercent);
    }
    
    return !m_progressDialogCancelled;
}

// ============================================================
// PARTIAL SAVE
// ============================================================

bool WebApplicationInterface::PartialSave(bool bPartialSaveClearSkipped, bool bFromLogic) {
    // Delegate to Kotlin/JS
    return false;
}

// ============================================================
// DIALOGS
// ============================================================

int WebApplicationInterface::ShowChoiceDialog(const CString& title, const std::vector<std::vector<CString>*>& data) {
    return -1; // Cancelled
}

int WebApplicationInterface::ShowShowDialog(
    const std::vector<CString>* column_titles,
    const std::vector<PortableColor>* row_text_colors,
    const std::vector<std::vector<CString>*>& data,
    const CString& heading)
{
    return -1;
}

int WebApplicationInterface::ShowSelcaseDialog(
    const std::vector<CString>* column_titles,
    const std::vector<std::vector<CString>*>& data,
    const CString& heading,
    std::vector<bool>* selections)
{
    return -1;
}

// ============================================================
// PARADATA
// ============================================================

void WebApplicationInterface::ParadataDriverManager(Paradata::PortableMessage msg, const Application* application) {
    // Paradata logging - delegate to Kotlin/JS IndexedDB
}

void WebApplicationInterface::ParadataDeviceInfoQuery(Paradata::ApplicationEvent::DeviceInfo& device_info) {
    // TODO: Check actual DeviceInfo struct members
    // device_info.deviceId = GetDeviceId();
    // device_info.osVersion = CString("Web");
    // device_info.appVersion = CString("1.0.0");
}

void WebApplicationInterface::ParadataDeviceStateQuery(Paradata::DeviceStateEvent::DeviceState& device_state) {
    // TODO: Check actual DeviceState struct members
    // device_state.batteryLevel = 100; // N/A for web
    // device_state.isCharging = false;
}

// ============================================================
// NETWORK
// ============================================================

double WebApplicationInterface::GetUpTime() {
    double uptime = EM_ASM_DOUBLE({ return performance.now() / 1000; });
    return uptime;
}

bool WebApplicationInterface::IsNetworkConnected(int connectionType) {
    bool connected = EM_ASM_INT({ return navigator.onLine ? 1 : 0; });
    return connected;
}

// ============================================================
// PLATFORM ADAPTERS (not yet implemented)
// ============================================================

IBluetoothAdapter* WebApplicationInterface::CreateAndroidBluetoothAdapter() {
    return nullptr; // TODO: Implement Web Bluetooth
}

IHttpConnection* WebApplicationInterface::CreateAndroidHttpConnection() {
    return nullptr; // Use fetch API via Kotlin/JS
}

IFtpConnection* WebApplicationInterface::CreateAndroidFtpConnection() {
    return nullptr; // FTP not commonly supported in browsers
}

// ============================================================
// MEDIA
// ============================================================

std::vector<std::wstring> WebApplicationInterface::GetMediaFilenames(MediaStore::MediaType media_type) const {
    return std::vector<std::wstring>();
}

std::wstring WebApplicationInterface::BarcodeRead(const std::wstring& message_text) {
    // Delegate to Kotlin/JS barcode scanner
    return L"";
}

bool WebApplicationInterface::AudioPlay(const std::wstring& filename, const std::wstring& message_text) {
    // Use HTML5 Audio API via Kotlin/JS
    return false;
}

bool WebApplicationInterface::AudioStartRecording(const std::wstring& filename, std::optional<double> seconds, std::optional<int> sampling_rate) {
    // Use MediaRecorder API via Kotlin/JS
    return false;
}

bool WebApplicationInterface::AudioStopRecording() {
    return false;
}

std::unique_ptr<TemporaryFile> WebApplicationInterface::AudioRecordInteractive(const std::wstring& message_text, std::optional<int> sampling_rate) {
    return nullptr;
}

// ============================================================
// GEOMETRY/MAPPING
// ============================================================

void WebApplicationInterface::CapturePolygonTrace(std::unique_ptr<Geometry::Polygon>& captured_polygon, const Geometry::Polygon* polygon, IMapUI* map) {
    // Delegate to Kotlin/JS mapping UI
}

void WebApplicationInterface::CapturePolygonWalk(std::unique_ptr<Geometry::Polygon>& captured_polygon, const Geometry::Polygon* polygon, IMapUI* map) {
    // Delegate to Kotlin/JS mapping UI
}

// ============================================================
// ENGINE UI PROCESSOR
// ============================================================

long WebApplicationInterface::RunEngineUIProcessor(WPARAM wParam, LPARAM lParam) {
    return 0;
}

bool WebApplicationInterface::CaptureImage(EngineUI::CaptureImageNode& capture_image_node) {
    // Use getUserMedia API via Kotlin/JS
    return false;
}

void WebApplicationInterface::CreateMapUI(std::unique_ptr<IMapUI>& map_ui) {
    // Create Leaflet/Mapbox UI via Kotlin/JS
}

void WebApplicationInterface::CreateUserbar(std::unique_ptr<Userbar>& userbar) {
    // Create web userbar - same pattern as AndroidApplicationInterface
    userbar = std::make_unique<WebUserbar>();
}

CString WebApplicationInterface::EditNote(const CString& note, const CString& title, bool case_note) {
    // Text area dialog via Kotlin/JS
    return note;
}

bool WebApplicationInterface::ExecSystemApp(EngineUI::ExecSystemAppNode& exec_system_app_node) {
    return false;
}

std::wstring WebApplicationInterface::GetHtmlDialogsDirectory() {
    return L"/html_dialogs";
}

void WebApplicationInterface::Prompt(EngineUI::PromptNode& options) {
    // Show prompt dialog via Kotlin/JS
}

bool WebApplicationInterface::RunPffExecutor(EngineUI::RunPffExecutorNode& run_pff_executor_node) {
    return false;
}

long WebApplicationInterface::View(const Viewer& viewer) {
    return 0;
}

// ============================================================
// FILE OPERATIONS
// ============================================================

void WebApplicationInterface::MediaScanFiles(const std::vector<CString>& paths) {
    // Not needed in web - files are already visible
}

std::wstring WebApplicationInterface::CreateSharableUri(const std::wstring& path, bool add_write_permission) {
    // Return file:// URL or blob: URL
    return path;
}

void WebApplicationInterface::FileCopySharableUri(const std::wstring& sharable_uri, const std::wstring& destination_path) {
    // File copy via Emscripten FS
}

// ============================================================
// ACTION INVOKER
// ============================================================

int WebApplicationInterface::ActionInvokerCreateWebController(const std::wstring* access_token_override) {
    return -1; // Not implemented
}

// ============================================================
// PARADATA
// ============================================================

void WebApplicationInterface::GetParadataCachedEvents() {
    // Get cached paradata events and send to Kotlin/JS
    EM_ASM({
        if (window.csproGetParadataCachedEvents) {
            window.csproGetParadataCachedEvents();
        }
    });
}

// ============================================================
// PROGRESS DIALOG CANCEL
// ============================================================

void WebApplicationInterface::OnProgressDialogCancel() {
    m_progressDialogCancelled = true;
}
