# መላ (Mela) - Ethiopian Block Management App

A bilingual (English/Amharic) Android application for managing electricity billing and payment calculations for residential blocks in Ethiopia.

## 📱 Features

- **Bilingual Support**: Switch seamlessly between English and Amharic languages
- **Block Management**: Manage blocks numbered from 355/01 to 355/66
- **Payment Calculation**: Automatic calculation of electricity bills including:
  - Base tariff
  - VAT percentage
  - Additional payments
  - Usage-based calculations
- **History Tracking**: View complete payment history with search functionality
- **Ethiopian Calendar Integration**: Built-in Ethiopian date converter
- **Data Persistence**: SQLite database for reliable data storage
- **PDF Export**: Export payment records as PDF documents
- **Search Functionality**: Quick search by block/house number

## 🛠️ Technical Details

### Built With
- **Language**: Java
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 35 (Android 15)
- **Compile SDK**: 35
- **Java Version**: 11

### Dependencies
- AndroidX AppCompat
- Material Design Components
- ConstraintLayout
- AndroidX Activity

### Key Components

#### Activities
- **MainActivity**: Main entry point for block data entry and payment calculation
- **MainActivity2**: History viewing and data management with search and export features
- **BaseActivity**: Base class for common functionality

#### Utilities
- **DatabaseHelper**: SQLite database management for storing block records
- **EthiopianDateConverter**: Converts between Gregorian and Ethiopian calendar dates
- **LocaleHelper**: Manages language switching between English and Amharic
- **SearchResultFragment**: Fragment for displaying search results

## 📋 Permissions

The app requires the following permissions:
- `READ_EXTERNAL_STORAGE` (Android 12 and below)
- `READ_MEDIA_IMAGES` (Android 13+)
- `READ_MEDIA_VIDEO` (Android 13+)
- `READ_MEDIA_AUDIO` (Android 13+)
- `WRITE_EXTERNAL_STORAGE` (Android 9 and below - for PDF export)

## 🚀 Installation

### Prerequisites
- Android Studio (Arctic Fox or later)
- JDK 11 or higher
- Android SDK with API level 35

### Steps
1. Clone this repository:
   ```bash
   git clone <your-repository-url>
   ```

2. Open the project in Android Studio

3. Sync Gradle files

4. Run the app on an emulator or physical device

## 💡 Usage

### Adding a New Record
1. Enter block/house number (must start with 355/ and be between 01-66)
2. Enter current electricity count
3. Enter tariff rate
4. Select VAT percentage
5. Enter additional payments (if any)
6. Click **Submit** to calculate and save the payment

### Viewing History
1. Tap the menu icon in the toolbar
2. Select **History**
3. Use the search function to find specific blocks
4. Export data as PDF using the download icon

### Switching Language
1. Tap the menu icon in the toolbar
2. Select **Switch Language**
3. App will restart with the selected language

## 📊 Database Schema

The app uses SQLite to store:
- Block/House number
- Electricity count
- VAT percentage
- Additional payments
- Final payment amount
- Tariff rate
- Date and time of entry

## 🌍 Localization

Supported Languages:
- **English (en)**: Default language
- **Amharic (am)**: አማርኛ

All UI strings are fully localized in both languages.

## 🔒 Security

- Device-specific validation using Android ID
- Secure data storage using SQLite
- No network permissions required - fully offline app

## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 👨‍💻 Developer

Developed for managing electricity billing in Ethiopian residential blocks.

## 🐛 Known Issues

None currently reported.

## 📧 Contact

For questions or support, please open an issue in this repository.

## 🎯 Future Enhancements

- Cloud backup integration
- Multi-user support
- Advanced analytics and reporting
- SMS notification integration
- Payment reminder system

---

**App Version**: 1.0  
**Last Updated**: 2026-02-09
