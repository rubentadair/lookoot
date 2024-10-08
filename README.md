
 
# Lookoot: Local Business Discovery Platform

## Overview

Lookoot is a mobile application designed to bridge the gap between consumers and local businesses. In an era dominated by e-commerce giants, Lookoot aims to empower local economies by enhancing the discoverability and accessibility of local stores and their products.

### Key Features

- User Registration and Authentication
- Store and Product Search with Location-Based Services
- Detailed Store and Product Information
- Review and Rating System
- Store Profile Management for Business Owners
- Product Inventory Management
- Map Integration for Store Discovery
- Real-time Updates using Firebase

## Technology Stack

- **Frontend**: Android (Kotlin)
- **UI Framework**: Jetpack Compose
- **Backend**: Firebase (Authentication, Firestore, Cloud Functions)
- **Architecture**: MVVM (Model-View-ViewModel)
- **Dependency Injection**: Hilt
- **Asynchronous Programming**: Coroutines and Flow
- **Maps and Location**: Google Maps API

## Getting Started

### Prerequisites

- Android Studio Arctic Fox or later
- Kotlin 1.5.0 or later
- Google Maps API Key
- Firebase Project Setup

### Installation

1. Clone the repository:
   ```
   git clone https://github.com/your-username/lookoot.git
   ```
2. Open the project in Android Studio.
3. Set up your Google Maps API key in the `local.properties` file:
   ```
   MAPS_API_KEY=your_api_key_here
   ```
4. Connect the app to your Firebase project:
   - Add your `google-services.json` file to the app module.
   - Ensure Firebase dependencies are correctly set up in your gradle files.

5. Build and run the project on an emulator or physical device.

## Project Structure

- `app/src/main/java/com/example/lookoot/`
  - `data/`: Data models and repository implementations
  - `di/`: Dependency injection modules
  - `ui/`: UI components and ViewModels
  - `utils/`: Utility classes and extensions
- `app/src/main/res/`: Resources including layouts, strings, and drawables
