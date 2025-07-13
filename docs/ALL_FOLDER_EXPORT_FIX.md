# All Folder PDF Export Fix

## Issue
When exporting expenses from the "All" folder to PDF, the application was failing to generate PDFs properly. This happened after recent changes to the PDF generation system that unified the layout options across different folder exports.

## Root Cause
The issue was in the `generateFolderGridPdf` method which was filtering expenses by folder name:

```kotlin
// Filter expenses for the specific folder
val folderExpenses = expenses.filter { it.folderName == folderName }
if (folderExpenses.isEmpty()) {
    Log.e(TAG, "No expenses found in folder: $folderName")
    return null
}
```

When we passed "All Expenses" as the folder name for the "All" folder, this filter returned an empty list because no expense actually has a folder named "All Expenses".

## Solution
We implemented a specialized PDF generation method for the "All" folder called `generateAllExpensesGridPdf`. This method skips the folder filtering step and directly processes all expenses passed to it.

Key changes:
1. Added a new method `generateAllExpensesGridPdf` in `PdfUtils.kt` that works similarly to `generateFolderGridPdf` but doesn't filter by folder name
2. Updated `generatePdfByFolder` in `ExpenseViewModel.kt` to use this specialized method for the "All" folder

## Benefits
- The "All" folder can now be exported to PDF using all the same grid layouts as specific folders
- The user experience is consistent across all exports
- All expenses are properly included in the "All" folder PDF export

## Testing
To verify this fix:
1. Navigate to the Expenses screen
2. Select the "All" folder
3. Export to PDF
4. Verify that the PDF is generated successfully and includes all expenses 