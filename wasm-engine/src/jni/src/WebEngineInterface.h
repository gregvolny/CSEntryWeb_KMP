/**
 * WebEngineInterface.h
 * 
 * Web-specific engine interface for WASM build
 * Replaces AndroidEngineInterface for web platform
 * 
 * Inherits from CoreEntryEngineInterface and provides
 * web-specific implementations without JNI dependencies
 */

#pragma once

#include <zPlatformO/PortableMFC.h>
#include <Zentryo/Runaple.h>
#include <Zentryo/CoreEntryPage.h>
#include <Zentryo/CoreEntryPageField.h>
#include <Zentryo/CoreEntryEngineInterface.h>
#include "WebApplicationInterface.h"

/**
 * Web platform engine interface
 * Provides all engine functionality for Kotlin/Wasm frontend
 */
class WebEngineInterface : public CoreEntryEngineInterface
{
public:
    // Constructor/Destructor
    WebEngineInterface();
    virtual ~WebEngineInterface();

private:
    WebApplicationInterface* m_pApplicationInterface;

public:
    // Field navigation
    CoreEntryPage* GoToField(int fieldSymbol, int index1, int index2, int index3);
    
    // Application lifecycle
    void EndApplication();
    
    // PFF properties
    CString GetOpIDFromPff();
    
    // Lock flags
    bool GetAddLockFlag();
    bool GetModifyLockFlag();
    bool GetDeleteLockFlag();
    bool GetViewLockFlag();
    bool GetCaseListingLockFlag();
    
    // Environment configuration
    void SetWebEnvironmentVariables(
        const CString& email,
        const CString& tempFolder,
        const CString& applicationFolder,
        const CString& versionNumber,
        const CString& assetsDirectory,
        const CString& csEntryFolder,
        const CString& opfsRootPath,
        const CString& downloadsDirectory
    );
    
    // Paradata
    void GetParadataCachedEvents();
    
    // Application info
    CString GetStartKeyString();
    CString GetApplicationDescription() const;
    
    // Userbar
    void RunUserbarFunction(int userbar_index);
    
    // Case tree - returns serializable data instead of JNI objects
    std::string GetCaseTreeJson();
    std::string UpdateCaseTreeJson();
    
    // Progress dialog
    void OnProgressDialogCancel();
    
    // Web-specific: Get application interface
    WebApplicationInterface* GetApplicationInterface() { return m_pApplicationInterface; }
};
