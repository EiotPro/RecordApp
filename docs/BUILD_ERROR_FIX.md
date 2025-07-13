# Build Error Fix: AreaBreakType Reference

## Issue
The application was failing to build with the following error:
```
Unresolved reference 'AreaBreakType'
```

This error occurred in the `PdfUtils.kt` file when trying to create page breaks in PDF documents.

## Root Cause
The error was caused by a mismatch between the iText PDF library version and the code implementation. The application is using iText PDF library version 7.2.5, but the code was written using an API that might be from a different version or was incorrectly implemented.

Specifically:
1. The code was trying to use `AreaBreakType.NEXT_PAGE` as a parameter to the `AreaBreak` constructor
2. In the version of iText PDF being used (7.2.5), the `AreaBreak` constructor doesn't require this parameter for a simple page break

## Solution
The solution involved two changes:

1. Removed the unnecessary import:
```kotlin
import com.itextpdf.layout.element.AreaBreakType
```

2. Modified the code that was using `AreaBreakType`:
```kotlin
// Before
document.add(AreaBreak(AreaBreakType.NEXT_PAGE))

// After
document.add(AreaBreak())
```

In iText PDF 7.2.5, the default constructor for `AreaBreak` creates a page break without needing to specify the type.

## Verification
After making these changes, the application should build successfully without any reference errors. The PDF export functionality will continue to work as expected, with page breaks between different folder sections in the generated PDF documents. 