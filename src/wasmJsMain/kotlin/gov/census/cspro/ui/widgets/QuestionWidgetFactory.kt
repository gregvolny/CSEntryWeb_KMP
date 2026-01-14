package gov.census.cspro.ui.widgets

import gov.census.cspro.data.CDEField
import gov.census.cspro.data.CaptureType

/**
 * Factory for creating QuestionWidget instances based on field type
 * Web equivalent of Android's QuestionWidgetFactory
 * 
 * This factory creates the appropriate widget based on the field's
 * capture type and data type, matching the logic from Android.
 */
object QuestionWidgetFactory {
    
    /**
     * Create a widget for the given field based on its capture type and data type
     * Matches Android's QuestionWidgetFactory.createWidgetForField()
     * 
     * @param field Engine field that this question wraps
     * @param setFocus Whether or not this widget should get focus when first displayed
     * @param emitNextPage Whether or not this widget should call next page listener
     * @param showCodes Whether or not to display codes along with labels
     * @param editNotes Whether notes editing is enabled
     */
    fun createWidgetForField(
        field: CDEField,
        setFocus: Boolean = false,
        emitNextPage: Boolean = true,
        showCodes: Boolean = false,
        editNotes: Boolean = false
    ): QuestionWidget {
        
        // Support both string and integer capture types
        val captureTypeStr = field.captureType.lowercase()
        val captureTypeInt = field.captureTypeInt
        
        return when {
            // Handle integer capture types from native engine
            captureTypeInt == CaptureType.DATA_CAPTURE_TEXT_BOX || captureTypeStr == CaptureType.TEXT_BOX || captureTypeStr == "text" -> {
                if (field.isNumeric) {
                    QuestionWidgetTextBoxNumeric(field, setFocus, emitNextPage, showCodes)
                } else {
                    QuestionWidgetTextBoxAlpha(field, setFocus, emitNextPage, showCodes)
                }
            }
            
            captureTypeInt == CaptureType.DATA_CAPTURE_RADIO_BUTTON || captureTypeStr == CaptureType.RADIO_BUTTON || captureTypeStr == "radio" -> {
                QuestionWidgetRadioButtons(field, setFocus, emitNextPage, showCodes)
            }
            
            captureTypeInt == CaptureType.DATA_CAPTURE_CHECK_BOX || captureTypeStr == CaptureType.CHECK_BOX || captureTypeStr == "check" -> {
                QuestionWidgetCheckBoxes(field, setFocus, emitNextPage, showCodes)
            }
            
            captureTypeInt == CaptureType.DATA_CAPTURE_DROP_DOWN || captureTypeStr == CaptureType.DROP_DOWN || captureTypeStr == "select" -> {
                QuestionWidgetDropDown(field, setFocus, emitNextPage, showCodes)
            }
            
            captureTypeInt == CaptureType.DATA_CAPTURE_COMBO_BOX || captureTypeStr == CaptureType.COMBO_BOX -> {
                // Combo box is a searchable dropdown
                if (field.isNumeric) {
                    QuestionWidgetComboBoxNumeric(field, setFocus, emitNextPage, showCodes)
                } else {
                    QuestionWidgetComboBoxAlpha(field, setFocus, emitNextPage, showCodes)
                }
            }
            
            captureTypeInt == CaptureType.DATA_CAPTURE_DATE || captureTypeStr == CaptureType.DATE -> {
                QuestionWidgetDate(field, setFocus, emitNextPage, showCodes)
            }
            
            captureTypeInt == CaptureType.DATA_CAPTURE_SLIDER || captureTypeStr == CaptureType.SLIDER -> {
                QuestionWidgetSlider(field, setFocus, emitNextPage, showCodes)
            }
            
            captureTypeInt == CaptureType.DATA_CAPTURE_BARCODE || captureTypeStr == CaptureType.BARCODE -> {
                // Protected barcodes are just text boxes (matching Android)
                if (field.isReadOnly || field.isProtected) {
                    if (field.isNumeric) {
                        QuestionWidgetTextBoxNumeric(field, setFocus, emitNextPage, showCodes)
                    } else {
                        QuestionWidgetTextBoxAlpha(field, setFocus, emitNextPage, showCodes)
                    }
                } else {
                    if (field.isNumeric) {
                        QuestionWidgetBarcodeNumeric(field, setFocus, emitNextPage, showCodes)
                    } else {
                        QuestionWidgetBarcodeAlpha(field, setFocus, emitNextPage, showCodes)
                    }
                }
            }
            
            captureTypeInt == CaptureType.DATA_CAPTURE_TOGGLE_BUTTON || captureTypeStr == CaptureType.TOGGLE -> {
                QuestionWidgetToggleButton(field, setFocus, emitNextPage, showCodes)
            }
            
            captureTypeInt == CaptureType.DATA_CAPTURE_PHOTO || captureTypeStr == CaptureType.PHOTO -> {
                QuestionWidgetPhoto(field, setFocus, emitNextPage, showCodes)
            }
            
            captureTypeInt == CaptureType.DATA_CAPTURE_SIGNATURE || captureTypeStr == CaptureType.SIGNATURE -> {
                QuestionWidgetSignature(field, setFocus, emitNextPage, showCodes)
            }
            
            captureTypeInt == CaptureType.DATA_CAPTURE_AUDIO || captureTypeStr == CaptureType.AUDIO -> {
                QuestionWidgetAudio(field, setFocus, emitNextPage, showCodes)
            }
            
            else -> {
                // Default to text box
                println("[QuestionWidgetFactory] Unknown capture type: ${field.captureType} (int: $captureTypeInt), using text box")
                if (field.isNumeric) {
                    QuestionWidgetTextBoxNumeric(field, setFocus, emitNextPage, showCodes)
                } else {
                    QuestionWidgetTextBoxAlpha(field, setFocus, emitNextPage, showCodes)
                }
            }
        }
    }
    
    /**
     * Get display name for a capture type
     */
    fun getCaptureTypeName(captureType: String): String {
        return when (captureType.lowercase()) {
            CaptureType.TEXT_BOX, "text" -> "Text Box"
            CaptureType.RADIO_BUTTON, "radio" -> "Radio Buttons"
            CaptureType.CHECK_BOX, "check" -> "Check Boxes"
            CaptureType.DROP_DOWN, "select" -> "Drop Down"
            CaptureType.COMBO_BOX -> "Combo Box"
            CaptureType.DATE -> "Date Picker"
            CaptureType.SLIDER -> "Slider"
            CaptureType.BARCODE -> "Barcode"
            CaptureType.TOGGLE -> "Toggle Button"
            CaptureType.PHOTO -> "Photo"
            CaptureType.SIGNATURE -> "Signature"
            CaptureType.AUDIO -> "Audio"
            else -> "Unknown"
        }
    }
}
