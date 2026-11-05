# SafeZone – Personal & Campus Safety Application

## 1. About SafeZone

SafeZone is an Android safety application designed to provide users with personal and campus safety features.

The application provides separate **Personal** and **Campus** user experiences and includes authentication, emergency assistance, incident reporting, safety alerts, location services, profile management and other safety-related functionality.

The project was developed as part of the Android Mobile Application POE.

---

## 2. Main Features

### Personal User

- Personal registration and login
- Firebase Authentication
- Personal safety dashboard
- Emergency assistance
- Incident reporting
- My Reports
- Safety alerts
- Safety map
- Profile management
- Settings
- Input validation

### Campus User

- Campus registration and login
- Institution and campus selection
- Campus safety dashboard
- Campus incident reporting
- My Campus Reports
- Campus safety alerts
- Campus safety map
- Emergency assistance
- Profile management
- Settings
- Input validation

---

## 3. User Interface Design

SafeZone uses a simple and user-friendly interface designed to provide quick access to important safety functions.

The application uses:

- Navy blue and white as the primary colours
- Rounded input fields and buttons
- Clear navigation
- Separate Personal and Campus user panels
- User-friendly layouts
- Input validation
- Error handling to prevent invalid inputs from causing application crashes

---

## 4. Technologies Used

### Android Application

- Kotlin
- Android Studio
- XML
- Android SDK
- Firebase Authentication
- Firebase Firestore
- Retrofit
- Gson
- AndroidX

### REST API

- C#
- ASP.NET Core
- Entity Framework Core
- Swagger
- SQL Server

### Cloud Services

- Microsoft Azure App Service
- Azure SQL Database
- Firebase

### Version Control and Testing

- Git
- GitHub
- GitHub Actions
- Automated Unit Testing

---

## 5. Application Architecture

SafeZone follows a client-server architecture.

```text
                    SAFEZONE ANDROID APPLICATION
                              |
                              |
                         Retrofit
                              |
                              v
                    HOSTED RESTFUL API
                    ASP.NET CORE / C#
                              |
                              |
                     Entity Framework Core
                              |
                              v
                     AZURE SQL DATABASE
                         SafeZoneDB


       Firebase Authentication        Firebase Firestore
                 |                            |
                 |                            |
                 +---------- Android ----------+


## 6. RESTful API

SafeZone connects to a hosted RESTful API developed using ASP.NET Core.

The API manages safety alert information and communicates with the Azure SQL database.

Hosted API
Open SafeZone REST API
https://safezone-api-2026-anehabhmdegdfqcm.southafricanorth-01.azurewebsites.net/swagger/index.html

Swagger API Documentation
Open SafeZone Swagger Documentation
https://safezone-api-2026-anehabhmdegdfqcm.southafricanorth-01.azurewebsites.net/swagger/index.html

The API provides endpoints for:

Retrieving all active safety alerts
Retrieving a safety alert by ID
Retrieving alerts by campus
Retrieving alerts by location
Retrieving alerts by alert type

The Android application uses Retrofit to communicate with these endpoints.

## 7. Firebase Authentication and Firestore

Firebase Authentication is used to manage user registration and login.

The application uses Firebase Authentication for email and password authentication.

User profile information is stored in Cloud Firestore, including information such as:

Full name
Email address
Account type
Institution
Campus
User ID
Account creation information

Passwords are handled by Firebase Authentication rather than being stored as plaintext passwords in the application's Firestore user profile data.

## 8. Azure SQL Database

The SafeZone REST API is connected to a hosted Azure SQL Database named SafeZoneDB.

The database stores safety alert records that are accessed through the REST API.

The API and database were tested to confirm:

Successful database connectivity
Retrieval of safety alerts
Creation of safety alert records
Updating of safety alert records
Deletion of safety alert records
Campus filtering
Location filtering
Alert type filtering

## 9. Retrofit and External Libraries

The Android application uses Retrofit as the HTTP client for communication with the SafeZone REST API.

Gson is used to convert JSON responses from the API into Kotlin data objects.

The API integration allows the application to retrieve safety alert information from the hosted Azure environment.

10. Testing

Automated unit testing was performed on the main functionality of the application.

Test Results

18 automated tests successfully passed.

Testing included:

User registration validation
Login validation
Invalid input handling
Firebase authentication
REST API communication
Safety alert retrieval
Database connectivity
Application functionality
Navigation
Error handling

The application was also tested on a physical Android device to confirm that the prototype runs correctly.

## 11. Application Logging

Logging was implemented in important authentication and alert functionality to assist with debugging and demonstrate application processes.

Logging was implemented in:

PersonalLoginActivity
PersonalRegistrationActivity
CampusLoginActivity
CampusRegistrationActivity
CampusAlertsActivity

The logs demonstrate processes such as:

Screen loading
Input validation
Firebase authentication
Firestore profile retrieval
API-related processing
Successful navigation

Sensitive information such as user passwords is not written to the application logs.

## 12. Screenshots and Evidence

A collection of screenshots demonstrating the development and testing of the SafeZone application is provided in the PDF below.

The PDF contains evidence of:

Personal Login logging
Personal Registration logging
Campus Login logging
Campus Registration logging
Campus Alerts and REST API communication
REST API and Azure SQL connectivity
Azure SQL safety alert records

Screenshots PDF
View OPSC6312 SafeZone_Screenshots.pdf

## 13. Demo Videos
SafeZone Android Application Demo
The video demonstrates the SafeZone Android application running on a physical mobile device, including the main application functionality.
https://drive.google.com/file/d/1xLhI_-Hp1TpwSapte7lKqVxBD3edgUJk/view

SafeZone REST API and Azure Demonstration
The video demonstrates the hosted SafeZone REST API, Swagger, Azure SQL Database connectivity and safety alert data.
https://drive.google.com/file/d/1WabGZOF5A5OA51ZXzmyWwWe1Tt_EG8yi/view


## 14. GitHub and Version Control

GitHub was used to manage the SafeZone source code and maintain version control throughout development.

The repository contains:

Android application source code
SafeZone REST API
Database-related code
Project documentation
README documentation
Testing-related files

Git was used to commit and push changes to the GitHub repository throughout development.

The .gitignore file was configured to prevent sensitive and unnecessary files from being committed, including:

Local Gradle files
Android Studio files
Build files
APK files
Keystore files
Firebase configuration files
Visual Studio build files
Publish profiles
Temporary log files

## 15. GitHub Actions

GitHub Actions is intended to support automated build and testing of the Android project.

The workflow can be used to automatically build the application and run automated tests when changes are pushed to the repository.

This supports continuous integration and helps identify development issues before the application is released.

## 16. Project Structure
SafeZone/
│
├── app/
│   └── Android application
│
├── SafeZoneAPI/
│   ├── Controllers/
│   ├── Data/
│   ├── Models/
│   ├── Migrations/
│   └── Program.cs
│
├── README.md
└── .gitignore

## 17. AI Tools

AI tools were used as development support during the project for activities such as:

Debugging programming errors
Understanding development concepts
Improving code structure
Troubleshooting Android Studio issues
Troubleshooting REST API and database issues
Generating development suggestions
Improving documentation

The generated suggestions were reviewed, adapted and tested during the development of the SafeZone prototype.

## 18. Conclusion

SafeZone provides a functional Android prototype focused on personal and campus safety.

The project demonstrates the integration of:

Kotlin Android development
Firebase Authentication
Firebase Firestore
RESTful web services
Retrofit
ASP.NET Core
Entity Framework Core
Azure App Service
Azure SQL Database
Automated unit testing
GitHub version control
Application logging

The prototype was tested on a physical Android device and the hosted REST API was tested against the Azure SQL database.

The project demonstrates how an Android application can communicate with cloud-based services and a hosted database to provide safety-related functionality.
