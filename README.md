


# Lookoot: Empowering Local Business Discovery

Lookoot is an innovative Android application that bridges the digital divide between consumers and local businesses. Developed as part of my MSc Software Development dissertation at the University of Glasgow, this platform leverages modern mobile technology and location-based services to make local shopping as convenient as major e-commerce platforms while supporting community economies.

## Project Overview

In response to the growing dominance of e-commerce giants, Lookoot provides a sophisticated mobile platform that enhances the discoverability and accessibility of local stores and their products. The application implements real-time location services, secure user authentication, and seamless store-customer interactions through a modern, intuitive interface built with Jetpack Compose.

### Key Features
- Location-based store and product discovery
- Real-time inventory tracking and updates
- Secure user authentication and profile management
- Interactive map integration with store locations
- Store profile and inventory management for business owners
- Customer review and rating system
- Direct messaging between customers and stores
- Personalized product recommendations

## Technical Architecture

### Frontend Development
- **Primary Language**: Kotlin
- **UI Framework**: Jetpack Compose for modern, declarative UI
- **Architecture Pattern**: MVVM (Model-View-ViewModel)
- **Dependency Injection**: Hilt
- **Asynchronous Operations**: Coroutines and Flow
- **Navigation**: Jetpack Navigation Component

### Backend Services
- **Firebase Integration**:
  - Authentication for secure user management
  - Cloud Firestore for real-time data storage
  - Cloud Functions for serverless operations
  - Cloud Storage for media management
- **Location Services**: Google Maps SDK and Places API
- **Analytics**: Firebase Analytics for user behavior tracking

### Key Technologies
- Android Studio
- Firebase Suite
- Google Maps Platform
- Kotlin Coroutines
- Jetpack Libraries
- Material Design 3

## Implementation Highlights

### Advanced Search Functionality
- Real-time search with geolocation filtering
- Product and store categorization
- Custom search algorithms for relevant results
- Search history and suggestions

### Data Management
- Efficient data caching for offline access
- Real-time data synchronization
- Secure data encryption
- Optimized database queries

### User Interface
- Material Design 3 components
- Responsive layouts for different screen sizes
- Custom animations and transitions
- Intuitive navigation patterns

### Security Features
- Multi-factor authentication
- Secure data transmission
- Privacy-focused user data handling
- Role-based access control

## Project Structure
com.example.lookoot/
├── data/
│   ├── models/        # Data entities
│   ├── repository/    # Data access layer
│   └── source/        # Data sources and APIs
├── di/                # Dependency injection modules
├── ui/
│   ├── components/    # Reusable UI components
│   ├── screens/       # Application screens
│   └── theme/         # UI theme and styling
└── utils/             # Utility classes and extensions


![image](https://github.com/user-attachments/assets/79795b6d-8e17-4dbf-960b-9de0f02f22a4)
![image](https://github.com/user-attachments/assets/0da6933b-daa7-4ae4-a7ce-34e0b5e6991e)
![image](https://github.com/user-attachments/assets/50db9fc2-df88-471f-8893-edb0287cfb7f)
![image](https://github.com/user-attachments/assets/68d9c764-2070-4dc2-8590-da73ccb5e382)


## Getting Started

### Prerequisites
- Android Studio Arctic Fox or later
- Kotlin 1.5.0+
- Google Maps API Key
- Firebase Project Setup

# Lookoot Installation Guide

This guide provides detailed instructions for setting up the Lookoot development environment and running the application locally.

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Development Environment Setup](#development-environment-setup)
3. [Project Configuration](#project-configuration)
4. [Firebase Setup](#firebase-setup)
5. [Google Maps Configuration](#google-maps-configuration)
6. [Building and Running](#building-and-running)
7. [Troubleshooting](#troubleshooting)

## Prerequisites

### Required Software
- Android Studio Arctic Fox (2020.3.1) or later
- JDK 11 or later
- Git
- Firebase CLI tools
- Node.js (for Firebase Functions)

### Hardware Requirements
- 8GB RAM minimum (16GB recommended)
- 10GB free disk space
- Intel i5/AMD Ryzen 5 or better processor

### Development Accounts
- Google Cloud Platform account
- Firebase account
- GitHub account

## Development Environment Setup

### 1. Android Studio Installation
1. Download Android Studio from [developer.android.com](https://developer.android.com/studio)
2. Install with these components:
   - Android SDK
   - Android SDK Platform
   - Android Virtual Device
   - Performance (Intel HAXM)
   ```bash
   # For Linux users
   sudo apt-get install qemu-kvm
2. SDK Configuration

Open Android Studio SDK Manager
Install:

Android 12 (API 31) SDK Platform
Google Play Services
Android Build Tools
NDK
CMake



Project Configuration
1. Clone Repository
bashCopygit clone https://github.com/rubentadair/lookoot.git
cd lookoot
2. Gradle Setup

Create local.properties in project root:

propertiesCopysdk.dir=/path/to/your/Android/Sdk
MAPS_API_KEY=your_google_maps_api_key
FIREBASE_API_KEY=your_firebase_api_key

Sync Gradle files:

bashCopy./gradlew clean
./gradlew build
Firebase Setup
1. Create Firebase Project

Go to Firebase Console
Create new project named "Lookoot"
Enable required services:

Authentication
Cloud Firestore
Cloud Functions
Cloud Storage



2. Configure Firebase in App

Download google-services.json
Place in app/ directory
Add Firebase dependencies in build.gradle:

gradleCopybuildscript {
    dependencies {
        classpath 'com.google.gms:google-services:4.3.15'
    }
}
3. Initialize Firebase Services

Enable Authentication methods:

Email/Password
Google Sign-In
Phone Number


Set up Firestore Database:

bashCopyfirebase init firestore

Deploy Firebase Functions:

bashCopycd functions
npm install
firebase deploy --only functions
Google Maps Configuration
1. Create API Key

Go to Google Cloud Console
Create new project or select existing
Enable APIs:

Maps SDK for Android
Places API
Geocoding API



2. Configure API Key in Project

Add to local.properties:

propertiesCopyMAPS_API_KEY=your_api_key

Update AndroidManifest.xml:

xmlCopy<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="${MAPS_API_KEY}"/>
Building and Running
1. Create Virtual Device

Open AVD Manager in Android Studio
Create Virtual Device:

Pixel 4 XL
API 31
x86 System Image



2. Build Project
bashCopy./gradlew assembleDebug
3. Run Application

Click 'Run' in Android Studio, or:

bashCopy./gradlew installDebug
Troubleshooting
Common Issues

Gradle Sync Failed

bashCopy./gradlew --refresh-dependencies

Missing SDK Components


Open SDK Manager
Install missing components
Sync project


Firebase Connection Issues


Verify google-services.json
Check Firebase console for correct package name
Ensure all Firebase services are enabled


Maps Not Loading


Verify API key in local.properties
Check Google Cloud Console for API activation
Enable billing in Google Cloud Console

Support Contacts

Technical Issues: rubentadair@gmail.com
Firebase Support: Firebase Support
Google Maps Platform: Maps Support

Additional Resources

Android Developer Guides
Firebase Documentation
Google Maps Platform Documentation
Kotlin Documentation


Contact
GitHub: rubentadair
