/**
 * WebApplicationInterface.cpp - Stub Implementation
 * 
 * Minimal stub implementation that compiles
 * All real functionality delegated to Kotlin/JS via WebWASMBindings
 */

#include "WebApplicationInterface.h"
#include <emscripten.h>
#include <emscripten/val.h>
#include <codecvt>
#include <locale>

using namespace emscripten;

// Helper to convert wstring to UTF-8 string
static std::string wstring_to_utf8(const std::wstring& wstr) {
    std::wstring_convert<std::codecvt_utf8<wchar_t>> converter;
    return converter.to_bytes(wstr);
}

// Helper to convert CString to UTF-8 string  
static std::string cstring_to_utf8(const CString& cstr) {
    const wchar_t* ptr = cstr;
    if (!ptr) return "";
    return wstring_to_utf8(std::wstring(ptr));
}

WebApplicationInterface::WebApplicationInterface(CoreEntryEngineInterface* core_interface)
    : m_coreInterface(core_interface)
    , m_progressDialogCancelled(false)
{
}

WebApplicationInterface::~WebApplicationInterface()
{
}

ObjectTransporter* WebApplicationInterface::GetObjectTransporter() { return nullptr; }

void WebApplicationInterface::RefreshPage(RefreshPageContents contents) {
    EM_ASM({ if (window.csproRefreshPage) window.csproRefreshPage($0); }, static_cast<int>(contents));
}

void WebApplicationInterface::DisplayErrorMessage(const TCHAR* error_message) {
    std::string msg = wstring_to_utf8(error_message);
    EM_ASM({ console.error('CSPro:', UTF8ToString($0)); }, msg.c_str());
}

std::optional<std::wstring> WebApplicationInterface::DisplayCSHtmlDlg(
    const NavigationAddress& navigation_address,
    const std::wstring* action_invoker_access_token_override) {
    return std::nullopt;
}

std::optional<std::wstring> WebApplicationInterface::DisplayHtmlDialogFunctionDlg(
    const NavigationAddress& navigation_address,
    const std::wstring* action_invoker_access_token_override,
    const std::optional<std::wstring>& display_options_json) {
    return std::nullopt;
}

int WebApplicationInterface::ShowModalDialog(NullTerminatedString title, NullTerminatedString message, int mbType) {
    return 0;
}

int WebApplicationInterface::ShowMessage(const CString& title, const CString& message, const std::vector<CString>& aButtons) {
    return 0;
}

std::tuple<int, int> WebApplicationInterface::GetMaxDisplaySize() const {
    return std::make_tuple(1024, 768);
}

bool WebApplicationInterface::GpsOpen() { return false; }
bool WebApplicationInterface::GpsClose() { return false; }
CString WebApplicationInterface::GpsRead(int waitTime, int accuracy, const CString& dialog_text) { return CString(L""); }
CString WebApplicationInterface::GpsReadLast() { return CString(L""); }
CString WebApplicationInterface::GpsReadInteractive(bool read_interactive_mode, const BaseMapSelection& base_map_selection, const CString& message, double read_duration) { return CString(L""); }

std::optional<BaseApplicationInterface::LoginCredentials> WebApplicationInterface::ShowLoginDialog(const CString& server, bool show_invalid_error) {
    return std::nullopt;
}

std::optional<BluetoothDeviceInfo> WebApplicationInterface::ChooseBluetoothDevice(const GUID& service_uuid) {
    return std::nullopt;
}

CString WebApplicationInterface::AuthorizeDropbox(const CString& clientId) { return CString(L""); }

CString WebApplicationInterface::GetUsername() const { return m_username; }
void WebApplicationInterface::SetUsername(const CString& username) { m_username = username; }

void WebApplicationInterface::StoreCredential(const std::wstring& attribute, const std::wstring& secret_value) {
}

std::wstring WebApplicationInterface::RetrieveCredential(const std::wstring& attribute) {
    return L"";
}

std::optional<std::wstring> WebApplicationInterface::GetPassword(const std::wstring& title, const std::wstring& description, bool file_exists) {
    return std::nullopt;
}

CString WebApplicationInterface::GetDeviceId() const {
    return CString(L"web-device-001");
}

CString WebApplicationInterface::GetLocaleLanguage() const {
    return CString(L"en-US");
}

void WebApplicationInterface::EngineAbort() {
    EM_ASM({ throw new Error('CSPro aborted'); });
}

bool WebApplicationInterface::ExecSystem(const std::wstring& command, bool wait) { return false; }
bool WebApplicationInterface::ExecPff(const std::wstring& pff_filename) { return false; }

CString WebApplicationInterface::GetProperty(const CString& parameter) { return CString(L""); }
void WebApplicationInterface::SetProperty(const CString& parameter, const CString& value) {}

void WebApplicationInterface::ShowProgressDialog(const CString& message) {
    m_progressDialogCancelled = false;
}

void WebApplicationInterface::HideProgressDialog() {}

bool WebApplicationInterface::UpdateProgressDialog(int progressPercent, const CString* message) {
    return !m_progressDialogCancelled;
}

bool WebApplicationInterface::PartialSave(bool bPartialSaveClearSkipped, bool bFromLogic) { return false; }

int WebApplicationInterface::ShowChoiceDialog(const CString& title, const std::vector<std::vector<CString>*>& data) {
    return -1;
}

int WebApplicationInterface::ShowShowDialog(
    const std::vector<CString>* column_titles,
    const std::vector<PortableColor>* row_text_colors,
    const std::vector<std::vector<CString>*>& data,
    const CString& heading) {
    return -1;
}

int WebApplicationInterface::ShowSelcaseDialog(
    const std::vector<CString>* column_titles,
    const std::vector<std::vector<CString>*>& data,
    const CString& heading,
    std::vector<bool>* selections) {
    return -1;
}

void WebApplicationInterface::ParadataDriverManager(Paradata::PortableMessage msg, const Application* application) {}
void WebApplicationInterface::ParadataDeviceInfoQuery(Paradata::ApplicationEvent::DeviceInfo& device_info) {}
void WebApplicationInterface::ParadataDeviceStateQuery(Paradata::DeviceStateEvent::DeviceState& device_state) {}

double WebApplicationInterface::GetUpTime() { return 0.0; }
bool WebApplicationInterface::IsNetworkConnected(int connectionType) { return true; }

IBluetoothAdapter* WebApplicationInterface::CreateAndroidBluetoothAdapter() { return nullptr; }
IHttpConnection* WebApplicationInterface::CreateAndroidHttpConnection() { return nullptr; }
IFtpConnection* WebApplicationInterface::CreateAndroidFtpConnection() { return nullptr; }

std::vector<std::wstring> WebApplicationInterface::GetMediaFilenames(MediaStore::MediaType media_type) const {
    return std::vector<std::wstring>();
}

std::wstring WebApplicationInterface::BarcodeRead(const std::wstring& message_text) { return L""; }
bool WebApplicationInterface::AudioPlay(const std::wstring& filename, const std::wstring& message_text) { return false; }
bool WebApplicationInterface::AudioStartRecording(const std::wstring& filename, std::optional<double> seconds, std::optional<int> sampling_rate) { return false; }
bool WebApplicationInterface::AudioStopRecording() { return false; }
std::unique_ptr<TemporaryFile> WebApplicationInterface::AudioRecordInteractive(const std::wstring& message_text, std::optional<int> sampling_rate) { return nullptr; }

void WebApplicationInterface::CapturePolygonTrace(std::unique_ptr<Geometry::Polygon>& captured_polygon, const Geometry::Polygon* polygon, IMapUI* map) {}
void WebApplicationInterface::CapturePolygonWalk(std::unique_ptr<Geometry::Polygon>& captured_polygon, const Geometry::Polygon* polygon, IMapUI* map) {}

long WebApplicationInterface::RunEngineUIProcessor(WPARAM wParam, LPARAM lParam) { return 0; }
bool WebApplicationInterface::CaptureImage(EngineUI::CaptureImageNode& capture_image_node) { return false; }
void WebApplicationInterface::CreateMapUI(std::unique_ptr<IMapUI>& map_ui) {}
void WebApplicationInterface::CreateUserbar(std::unique_ptr<Userbar>& userbar) {}
CString WebApplicationInterface::EditNote(const CString& note, const CString& title, bool case_note) { return CString(L""); }
bool WebApplicationInterface::ExecSystemApp(EngineUI::ExecSystemAppNode& exec_system_app_node) { return false; }
std::wstring WebApplicationInterface::GetHtmlDialogsDirectory() { return L"/Assets/html/dialogs"; }
void WebApplicationInterface::Prompt(EngineUI::PromptNode& options) {}
bool WebApplicationInterface::RunPffExecutor(EngineUI::RunPffExecutorNode& run_pff_executor_node) { return false; }
long WebApplicationInterface::View(const Viewer& viewer) { return 0; }

void WebApplicationInterface::MediaScanFiles(const std::vector<CString>& paths) {}
std::wstring WebApplicationInterface::CreateSharableUri(const std::wstring& path, bool add_write_permission) { return path; }
void WebApplicationInterface::FileCopySharableUri(const std::wstring& sharable_uri, const std::wstring& destination_path) {}

int WebApplicationInterface::ActionInvokerCreateWebController(const std::wstring* access_token_override) {
    return -1;
}
