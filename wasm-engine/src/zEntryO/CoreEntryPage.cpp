#include "StdAfx.h"
#include "CoreEntryPage.h"
#include "CoreEntryEngineInterface.h"
#include "CoreEntryPageField.h"
#include "Runaple.h"
#include <engine/IntDrive.h>


CoreEntryPage::CoreEntryPage(CoreEntryEngineInterface* core_entry_engine_interface, CDEField* pField)
    :   m_pField(pField)
{
    ASSERT(!m_pField->IsProtected());
    ASSERT(!m_pField->IsMirror());

    CRunAplEntry* pRunAplEntry = core_entry_engine_interface->GetRunAplEntry();
    CEntryDriver* m_pEngineDriver = pRunAplEntry->GetEntryDriver();
    CEngineArea* m_pEngineArea = m_pEngineDriver->getEngineAreaPtr();
    CIntDriver* m_pIntDriver = m_pEngineDriver->m_pIntDriver.get();

    auto GetSymbolTable = [&]() -> const Logic::SymbolTable& { return m_pEngineArea->GetSymbolTable(); };

    int symbol_index = m_pField->GetSymbol();
    const VART* field_vart = VPT(symbol_index);

    // construct a combined occurrence label (the function will return record and item occurrence labels)
    std::vector<std::wstring> occurrence_labels = m_pEngineDriver->GetOccurrenceLabels(field_vart);
    occurrence_labels.erase(std::remove(occurrence_labels.begin(), occurrence_labels.end(), SO::EmptyString), occurrence_labels.end());
    m_occurrenceLabel = SO::CreateSingleString(occurrence_labels, _T(" - "));

    // get the block information (if applicable)
    const EngineBlock* engine_block = field_vart->GetEngineBlock();

    if( engine_block != nullptr )
    {
        // setup the block details
        const CDEBlock& form_block = engine_block->GetFormBlock();
        m_blockName = form_block.GetName();
        m_blockLabel = form_block.GetLabel();

        // evaluate the block's question/help text
        const CCapi* capi = pRunAplEntry->GetCapi();
        CapiContent block_capi_content;
        capi->GetCapiContent(&block_capi_content, engine_block->GetSymbolIndex(), CCapi::CapiContentType::All);

        m_blockCapiContentVirtualFileMapping.SetCapiContent(std::move(block_capi_content), *m_pEngineDriver);
        
        // For WASM: Support DisplayTogether blocks like Android does
        // Add all fields in the block if display together is enabled
        if( form_block.GetDisplayTogether() )
        {
            for( CDEField* this_field : engine_block->GetFields() )
            {
                if( !this_field->IsProtected() && !this_field->IsMirror() )
                {
                    CoreEntryPageField& page_field = m_pageFields.emplace_back(core_entry_engine_interface, this_field);

                    // Track the current field
                    if( this_field == pField )
                        m_currentPageField = &page_field;
                }
            }
            
            // If we added multiple fields, don't need to add the single field below
            if( !m_pageFields.empty() )
            {
                // Make sure we have a current page field
                if( m_currentPageField == nullptr )
                    m_currentPageField = &m_pageFields.front();
                return;
            }
        }
    }

    // Single field display (no block or block not displayed together)
    m_currentPageField = &m_pageFields.emplace_back(core_entry_engine_interface, m_pField);

    if( m_pageFields.size() == 1 )
        m_pageFields.front().SetCaptureTypeForSingleFieldDisplay();
}


CoreEntryPageField* CoreEntryPage::GetPageField(const std::wstring& field_name)
{
    auto lookup = std::find_if(m_pageFields.begin(), m_pageFields.end(),
                               [&](const CoreEntryPageField& page_field) { return SO::EqualsNoCase(field_name, page_field.GetName()); });

    return ( lookup != m_pageFields.end() ) ? &(*lookup) :
                                              nullptr;
}


bool CoreEntryPage::AreAnyFieldsFilled() const
{
    for( const CoreEntryPageField& page_field : m_pageFields )
    {
        if( page_field.IsFieldFilled() )
            return true;
    }

    return false;
}
