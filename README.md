# Acadence
## Integrated Research Support Platform

Acadence is a Kotlin-based Android application designed to assist postgraduate students by providing them with consultants to manage their research efficiently. The prototype integrates with Firebase to offer secure authentication, file uploads, and customizable user settings.


## 1. Overview

- Secure user registration and Single Sign-On (SSO)
- Document upload and management
- Personalized article feed/ For You page
- Notifications
- Adjustable user settings (language, theme, notification)

This prototype showcases the essential functionalities required for Part 2 of the project brief.


## 2. Tech Stack

| Component       | Technology                                      |
|-----------------|------------------------------------------------|
| Language        | Kotlin                                         |
| IDE             | Android Studio                                 |
| Database        | Firebase Firestore                             |
| Authentication  | Firebase Authentication (SSO + Biometrics)     |
| File Storage    | Firebase Storage                               |
| Notifications   | Firebase Cloud Messaging                        |
| Target SDK      | API 36 (Android 14)                            |


## 3. Key Features

- **Register & Log In:** Secure Firebase SSO authentication  
- **Settings Page:** Language, theme, notifications  
- **Upload Documents:** PDF/DOC upload and cloud storage  
- **For You Page:** View and share articles from other students  
- **Notifications:** Firebase push alerts for activity updates  

**Planned for the final POE:**

- AI assistant & recommendation system  
- Consultant feedback and messaging  
- Offline mode with Room DB
- Biometrics


## 4. Setup

1. Clone the repository
2. Add your Firebase google-services.json file under /app.
3. Enable Firebase Authentication, Firestore, and Cloud Storage.
4. Run on a physical device or emulator (API 36+).

## 5. Development & Version Control
Repository hosted on GitHub with feature-based commits
GitHub Actions used for build verification and CI
Code structured for scalability and maintainability

