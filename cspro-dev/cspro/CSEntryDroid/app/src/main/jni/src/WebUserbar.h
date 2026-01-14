/**
 * WebUserbar.h
 * 
 * Web-specific userbar implementation
 * Replaces AndroidUserbar for WASM builds
 * 
 * Provides JavaScript callbacks for userbar display updates
 */

#pragma once

#include <zEngineF/PortableUserbar.h>

#ifdef __EMSCRIPTEN__
#include <emscripten.h>
#include <zToolsO/Utf8Convert.h>
#endif


class WebUserbar : public PortableUserbar
{
public:
    /**
     * Get the user function for a button at the given index
     * Same interface as AndroidUserbar::GetFunctionForButton
     */
    UserFunctionArgumentEvaluator* GetFunctionForButton(size_t userbar_index)
    {
        if (userbar_index >= m_items.size())
            return nullptr;
        
        const PortableUserbarItem& item = m_items[userbar_index];
        
        // Update last activated item ID (for GetLastActivatedItem)
        m_lastActivatedItemId = item.id;
        
        // Return the user function evaluator if this is a function action
        if (item.action.has_value() && 
            std::holds_alternative<std::unique_ptr<UserFunctionArgumentEvaluator>>(*item.action))
        {
            return std::get<std::unique_ptr<UserFunctionArgumentEvaluator>>(*item.action).get();
        }
        
        return nullptr;
    }
    
    /**
     * Get the control action for a button at the given index
     * Returns nullopt if not a control action
     */
    std::optional<ControlAction> GetControlActionForButton(size_t userbar_index)
    {
        if (userbar_index >= m_items.size())
            return std::nullopt;
        
        const PortableUserbarItem& item = m_items[userbar_index];
        
        if (item.action.has_value() && 
            std::holds_alternative<ControlAction>(*item.action))
        {
            return std::get<ControlAction>(*item.action);
        }
        
        return std::nullopt;
    }

protected:
    /**
     * Called when userbar display or items change
     * Sends update to JavaScript/Kotlin UI
     */
    void OnDisplayOrItemChange() override
    {
#ifdef __EMSCRIPTEN__
        // Build JSON array of button texts for JavaScript
        std::string json = "[";
        for (size_t i = 0; i < m_items.size(); ++i) {
            if (i > 0) json += ",";
            
            std::string text = UTF8Convert::WideToUTF8(m_items[i].text);
            // Escape quotes in text
            std::string escapedText;
            for (char c : text) {
                if (c == '"') escapedText += "\\\"";
                else if (c == '\\') escapedText += "\\\\";
                else escapedText += c;
            }
            json += "\"" + escapedText + "\"";
        }
        json += "]";
        
        EM_ASM({
            if (window.csproUserbarUpdate) {
                var shown = $0;
                var buttonsJson = UTF8ToString($1);
                var buttons = JSON.parse(buttonsJson);
                window.csproUserbarUpdate(shown, buttons);
            }
        }, m_shownState, json.c_str());
#endif
    }
};
