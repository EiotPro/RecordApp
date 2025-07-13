# RecordApp User Guide

This guide will help you navigate and use all the features of RecordApp effectively.

## Getting Started

### Installation
1. Download RecordApp from Google Play Store or install the APK directly
2. Launch the app and create your account or log in
3. Grant required permissions when prompted (camera and storage)

### Home Screen
The home screen displays:
- Overview of your expenses
- Quick action buttons for common tasks
- Recent transactions
- Folder navigation options

## Core Features

### Capturing Expenses

#### Using the Camera
1. Tap the camera icon on the dashboard
2. Position the camera to capture the entire receipt
3. Tap the capture button
4. Review the captured image and confirm

#### Using Existing Images
1. Tap the gallery icon on the dashboard
2. Select an image from your gallery
3. Confirm your selection

### Managing Images

#### Reordering Images
1. Open a folder containing images
2. Tap the "Reorder" icon in the top-right corner
3. Click on each image in the order you want them to appear
   - Each image will be assigned a number (1, 2, 3, etc.) as you click
   - Click an already-numbered image to remove its number and reorder
4. After clicking all images, the new order will be saved automatically
5. You'll see a green success message when ordering is complete
6. Use the "Reset Order" button to start over if needed

#### Adding Images to a Specific Position
1. Upload a new image to the folder
2. Enter reordering mode as described above
3. Click images in your desired sequence, including the new image
4. The new image will be inserted exactly where you clicked it in the sequence

### OCR Receipt Processing
After capturing or selecting an image:
1. The app will automatically process the image to extract text
2. Review the extracted information (date, amount, description)
3. Make any necessary corrections
4. Tap "Save" to create the expense record

### Managing Folders
1. To change folders, tap the folder dropdown on the dashboard
2. Select an existing folder or create a new one
3. All new expenses will be saved to the selected folder

### Viewing Expenses
1. Tap "View All" to see all your expenses
2. Use the filters to narrow down by date, amount, or folder
3. Tap on any expense to view details

### Exporting Data

#### PDF Export
1. Go to Settings > Export
2. Select "Export to PDF"
3. Choose your layout option:
   - Individual (one receipt per page)
   - Grid 2×2 (four receipts per page)
   - Grid 2×3 (six receipts per page)
4. Select date range
5. Tap "Export" and choose where to save

#### CSV Export
1. Go to Settings > Export
2. Select "Export to CSV"
3. Select date range
4. Tap "Export" and choose where to save

## Admin Features

### Accessing the Admin Panel
1. Log in with admin credentials (default: admin/admin12345)
2. Navigate to Settings > Profile
3. Tap on "Admin Panel" button (only visible to admin users)

### Admin Dashboard
The dashboard tab provides at-a-glance metrics about your system:
1. Total user count
2. Number of admin users
3. Admin-to-user ratio percentage
4. Regular user count
5. User role distribution chart

### User Management
The Users tab allows you to manage all system users:
1. View all registered users with their roles
2. Filter users by name, email, or role
3. Change user roles:
   - ADMIN: Full system access
   - MODERATOR: Content moderation privileges
   - VIEWER: Read-only access
   - USER: Standard app access
4. Suspend problematic users (suspended users cannot log in)
5. Reactivate suspended users when needed

### Reporting
The Reports tab provides data analysis and export features:
1. Generate reports by user activity:
   - Login activity
   - Content creation
   - System usage
2. Select custom date ranges for your reports
3. Export data in multiple formats:
   - PDF: For formal reports with formatting
   - CSV: For data analysis in spreadsheet apps
   - JSON: For integration with other systems

### Security Settings
The Security tab allows admin credential management:
1. Update admin username and password:
   - Enter current credentials for verification
   - Enter and confirm new credentials
   - Follow password strength guidelines
2. View security best practices
3. Audit trail of admin actions is automatically maintained

## User Roles

RecordApp implements role-based access control with these roles:

### Admin
- Full access to all system features
- User management capabilities
- Report generation and data export
- Security settings management

### Moderator
- Content review capabilities
- Limited administrative functions
- Cannot modify admin users
- Access to reports and dashboard

### Viewer
- Read-only access to content
- Cannot modify data
- Cannot access security settings
- Can view reports

### User
- Standard app functionality
- Personal data management
- No administrative capabilities
- Cannot access admin panel

## Customization

### Theme Settings
1. Go to Settings > Appearance
2. Choose your preferred theme:
   - Light
   - Dark
   - System default

### Currency Settings
1. Go to Settings > General
2. Select your preferred currency from the list

## Receipt Types

The app can now automatically detect different receipt types:

### Physical Receipts
Standard paper receipts captured with your camera. These are displayed with a receipt icon.

### Digital Payments
Receipts from online transactions. These are displayed with a credit card icon.

### UPI Transactions
Receipts from UPI payment systems. These are displayed with a QR code icon.

## Troubleshooting

### Camera Issues
- Ensure the app has camera permissions
- Check that your device has sufficient storage
- Restart the app if the camera fails to initialize

### OCR Accuracy
For best OCR results:
- Ensure good lighting
- Avoid shadows on the receipt
- Position the receipt to be flat and unwrinkled
- Make sure the text is clearly visible

### Login Issues
- If you can't log in as admin, try the default credentials (admin/admin12345)
- If your account is suspended, contact another administrator
- Reset your password if you've forgotten it

### Admin Panel Access
- Only users with ADMIN role can access the admin panel
- The admin panel button appears only for users with admin privileges
- Check your user role in the profile screen

### User Role Changes
- Role changes take effect immediately
- Users must log out and log back in to see changes in their permissions
- When changing a user from ADMIN to another role, ensure at least one ADMIN remains

### Export Problems
- Verify storage permissions are granted
- Ensure your device has sufficient storage space
- Check that you have a compatible PDF reader installed

## Privacy and Data

All data is stored locally on your device. No information is shared without your consent.

To backup your data:
1. Go to Settings > Backup
2. Select "Create Backup"
3. Choose a location to save your backup file

To restore from backup:
1. Go to Settings > Backup
2. Select "Restore from Backup"
3. Select your backup file

## App Updates

RecordApp is regularly updated with new features and improvements. Always keep your app updated to the latest version for the best experience.

### Current Version
Version 1.2 - August 2024
- Improved image management with click-based ordering system
- Enhanced visual feedback for image ordering
- Added reset functionality for image ordering
- Fixed stability issues with image ordering

### Previous Versions
Version 1.1 - July 2024
- Fixed authentication issues with user roles
- Enhanced admin features
- Improved security handling 