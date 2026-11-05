# SafeZone – Personal & Campus Safety Application

## 1. Project Overview

SafeZone is an Android mobile application designed to improve personal and campus safety by providing users with access to emergency assistance and safety-related services.

The application provides two separate user experiences: **Personal** and **Campus**. It includes authentication, emergency assistance, incident reporting, safety alerts, location services, profile management and other safety-related functionality.

SafeZone was developed as part of the Android Mobile Application POE.

## 2. Main Features

### Personal User

- Personal registration and login
- Firebase Authentication
- Google Sign-In
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

## 3. User Interface Design

SafeZone uses a simple and user-friendly interface designed to provide quick access to important safety functions.

The interface includes:

- Navy blue and white as the primary colours
- Rounded input fields and buttons
- Clear navigation
- Separate Personal and Campus user panels
- User-friendly layouts
- Input validation
- Error handling to help prevent invalid inputs from causing application crashes

## 4. Technologies Used

### Android Application

- Kotlin
- Android Studio
- XML
- Android SDK
- Firebase Authentication
- Firebase Cloud Firestore
- Firebase Cloud Messaging (FCM)
- Google Sign-In / Credential Manager
- Retrofit
- Gson
- Room Database
- WorkManager
- AndroidX

### REST API

- C#
- ASP.NET Core
- Entity Framework Core
- Swagger / OpenAPI
- SQL Server

### Cloud Services

- Microsoft Azure App Service
- Azure SQL Database
- Firebase

### Version Control and Testing

- Git
- GitHub
- GitHub Actions
- Automated unit testing
- JUnit

## 5. Application Architecture

SafeZone follows a client-server architecture. The Android application communicates with the hosted REST API using Retrofit, while the API uses Entity Framework Core to interact with the Azure SQL Database.

Firebase provides authentication and cloud-based services for the Android application.

                  SAFEZONE ANDROID APPLICATION
                              |
             +----------------+----------------+
             |                |                |
             v                v                v
          Retrofit        Firebase          Room DB
             |          Authentication      Offline
             |          and Firestore       Storage
             |                |
             v                |
       HOSTED REST API         |
       ASP.NET CORE / C#       |
             |                |
             v                |
    Entity Framework Core      |
             |                |
             v                |
       AZURE SQL DATABASE      |
             |                |
             +----------------+

## 6. RESTful API

SafeZone connects to a hosted RESTful API developed using ASP.NET Core and C#.

The API manages safety alert information and communicates with the Azure SQL Database.

**Hosted API and Swagger Documentation:**

[Open SafeZone REST API – Swagger](https://safezone-api-2026-anehabhmdegdfqcm.southafricanorth-01.azurewebsites.net/swagger/index.html)

Part 3
(https://drive.google.com/file/d/1YmP3ydDjoKJ16z6JPz_OetupP1KK2IDI/view?usp=drive_link)

The API provides endpoints for:

- Retrieving all active safety alerts
- Retrieving a safety alert by ID
- Retrieving alerts by campus
- Retrieving alerts by location
- Retrieving alerts by alert type

The Android application uses Retrofit to communicate with these endpoints.

## 7. Firebase Authentication and Firestore

Firebase Authentication is used to manage user registration and login.

The application supports email and password authentication, as well as Google Sign-In.

Cloud Firestore stores user profile information, including:

- Full name
- Email address
- Account type
- Institution
- Campus
- User ID
- Account creation information

Passwords are managed by Firebase Authentication rather than being stored as plaintext in Firestore user profile records.

Firebase Cloud Messaging is also included for push notification functionality.

## 8. Azure SQL Database

The SafeZone REST API connects to a hosted Azure SQL Database named `SafeZoneDB`.

The database stores safety alert records that are accessed through the REST API.

Database and API functionality was tested to verify:

- Database connectivity
- Retrieval of safety alerts
- Creation of safety alert records
- Updating of safety alert records
- Deletion of safety alert records
- Campus filtering
- Location filtering
- Alert type filtering

## 9. Retrofit and External Libraries

Retrofit is used as the HTTP client for communication between the Android application and the SafeZone REST API.

Gson converts JSON responses from the API into Kotlin data objects.

Other Android libraries support authentication, local database storage, background synchronisation and application functionality.

## 10. Testing

Automated unit testing was performed during the development of SafeZone.

**Local test results: 18 automated tests passed.**

Testing activities included:

- User registration validation
- Login validation
- Invalid input handling
- Firebase authentication
- REST API communication
- Safety alert retrieval
- Database connectivity
- Application functionality
- Navigation
- Error handling

The application was also tested on a physical Android device to confirm that the prototype runs correctly.

### Continuous Integration Testing

GitHub Actions was configured to automate the Android project's build and unit testing processes.

The workflow runs when changes are pushed to the `main` branch or when a pull request targets `main`.

The workflow performs the following tasks:

1. Checks out the repository.
2. Sets up the Java development environment.
3. Restores Firebase configuration using GitHub Actions Secrets.
4. Runs automated unit tests.
5. Builds the Android application.

**Final GitHub Actions verification: Both the unit testing and Android build workflow completed successfully.**

## 11. Application Logging

Logging was implemented in important authentication and alert functionality to assist with debugging and demonstrate application processes.

Logging was implemented in:

- `PersonalLoginActivity`
- `PersonalRegistrationActivity`
- `CampusLoginActivity`
- `CampusRegistrationActivity`
- `CampusAlertsActivity`

The logs demonstrate processes such as:

- Screen loading
- Input validation
- Firebase authentication
- Firestore profile retrieval
- API-related processing
- Successful navigation

Sensitive information such as user passwords is not written to application logs.

## 12. Screenshots and Evidence

A collection of screenshots demonstrating the development and testing of the SafeZone application is provided in the project evidence PDF.

The evidence includes:

- Personal Login logging
- Personal Registration logging
- Campus Login logging
- Campus Registration logging
- Campus Alerts and REST API communication
- REST API and Azure SQL connectivity
- Azure SQL safety alert records

**[View OPSC6312 SafeZone Screenshots and Evidence (PDF)](OPSC6312%20SafeZone_Screenshots.pdf)**

## 13. Demonstration Videos

### SafeZone Android Application Demonstration

The video demonstrates the SafeZone Android application running on a physical mobile device, including its main application functionality.

[Watch SafeZone Android Application Demo](https://drive.google.com/file/d/1xLhI_-Hp1TpwSapte7lKqVxBD3edgUJk/view)

Part 3 video 
(https://drive.google.com/file/d/1idYWufphIWlbmG24rIdea807PSb66_7H/view?usp=drive_link)

### SafeZone REST API and Azure Demonstration

The video demonstrates the hosted SafeZone REST API, Swagger, Azure SQL Database connectivity and safety alert data.

[Watch SafeZone REST API and Azure Demonstration](https://drive.google.com/file/d/1WabGZOF5A5OA51ZXzmyWwWe1Tt_EG8yi/view)

Part 3 Video 
(https://drive.google.com/file/d/1YmP3ydDjoKJ16z6JPz_OetupP1KK2IDI/view?usp=drive_link)

## 14. GitHub and Version Control

GitHub was used to manage the SafeZone source code and maintain version control throughout development.

The repository contains:

- Android application source code
- SafeZone REST API
- Database-related code
- Project documentation
- README documentation
- Testing-related files
- GitHub Actions workflow configuration

Git was used to commit, merge and push changes to the GitHub repository throughout development.

The `.gitignore` file prevents sensitive and unnecessary files from being committed, including:

- Local Gradle files
- Android Studio files
- Build files
- APK files
- Keystore files
- Firebase configuration files
- Visual Studio build files
- Publish profiles
- Temporary log files

The Firebase configuration is supplied to GitHub Actions through repository secrets rather than being committed to the repository.

### Final Submission Tag

The final project version is identified by the Git tag:

`final-poe`

## 15. GitHub Actions and Continuous Integration

GitHub Actions supports continuous integration by automatically running unit tests and building the Android application.

The workflow is configured in:

`.github/workflows/build.yml`

The workflow helps identify compilation and testing issues when changes are pushed to the repository.

The final submitted version was successfully verified through GitHub Actions, with both the unit testing and Android application build steps passing.

## 16. Project Structure

```text
SafeZone/
│
├── .github/
│   └── workflows/
│       └── build.yml
│
├── app/
│   └── Android application source code
│
├── SafeZoneAPI/
│   ├── Controllers/
│   ├── Data/
│   ├── Models/
│   ├── Migrations/
│   └── Program.cs
│
├── OPSC6312 SafeZone_Screenshots.pdf
├── README.md
└── .gitignore
```

## 17. Setting Up the Project

### Requirements

- Android Studio
- JDK 17 or a compatible configured Android Gradle JDK
- Android SDK
- Git
- Firebase project configuration

### Installation

1. Clone the repository:

git clone https://github.com/ST10449303/SafeZone-Android-POE.git


2. Open the cloned project in Android Studio.

3. Allow Gradle to synchronise the project dependencies.

4. Configure Firebase by adding the appropriate `google-services.json` file to the `app/` directory.

5. Ensure that Google Sign-In is configured with the correct OAuth client credentials.

6. Configure the required API connection settings.

7. Connect an Android device or start an emulator.

8. Build and run the application using Android Studio.

**Important:** Firebase configuration files and other sensitive credentials are excluded from version control. Developers must supply their own authorised configuration when setting up the project.

### Running the REST API

The `SafeZoneAPI` project can be opened separately in Visual Studio.

Configure the required database connection settings and Azure SQL access before running the API.

Swagger can be used to inspect and test the available API endpoints.

## 18. AI Tools

AI tools were used as development support during the project for activities such as:

- Debugging programming errors
- Understanding development concepts
- Improving code structure
- Troubleshooting Android Studio issues
- Troubleshooting REST API and database issues
- Generating development suggestions
- Improving project documentation

Generated suggestions were reviewed, adapted and tested during the development of the SafeZone prototype.

## 19. Conclusion

SafeZone provides an Android prototype focused on personal and campus safety.

The project demonstrates the integration of:

- Kotlin Android development
- Firebase Authentication
- Firebase Firestore
- Firebase Cloud Messaging
- Google Sign-In
- RESTful web services
- Retrofit
- Room Database
- ASP.NET Core
- Entity Framework Core
- Azure App Service
- Azure SQL Database
- Automated unit testing
- GitHub version control
- GitHub Actions
- Application logging

The application was tested on a physical Android device, while the hosted REST API was tested against the Azure SQL Database.

The project demonstrates how an Android application can communicate with cloud-based services and a hosted database to provide safety-related functionality.
