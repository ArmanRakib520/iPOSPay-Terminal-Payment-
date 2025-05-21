# iPOSPay Terminal Payment

An Android application for integrating with Dejavoo payment terminals using the DVPayLite API.

## Overview
This application demonstrates integration with Dejavoo payment terminals via URI-based payment processing. It allows users to register terminals using Terminal Provider Numbers (TPN) and initiate payment transactions.

## Features
- Terminal registration using TPN (12-digit identifier)
- URI-based payment processing
- Integration with DVPayLite API (v1.1.9.7)

## Technical Details
- Minimum SDK: 29 (Android 10)
- Target SDK: 34 (Android 14)
- Written in Kotlin
- Uses AndroidX libraries

## Requirements
- Android 10 or higher
- Internet connection for terminal communication
- Dejavoo payment terminal with valid TPN

## Dependencies
- DVPayLite API: `com.denovo:invoke-dvpay-lite:1.1.9.7`
- AndroidX Core, AppCompat, Material Design, and ConstraintLayout libraries

## Building and Installation
The application can be built using Android Studio with Gradle. For signing configurations, a valid keystore will need to be provided.

## License
Proprietary - All rights reserved