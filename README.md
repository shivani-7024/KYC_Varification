# KYC Verification App

A mobile app that automates the document verification process using Firestore and email-based OTP (One-Time Password) authentication. This app allows users to enter a document number, verifies it against the database in Firestore, and sends an OTP via email for secure verification.

## Features

- **Document Verification:** Enter document numbers for validation with Firestore.
- **OTP Authentication:** Secure email-based OTP system to verify the user.
- **Firestore Integration:** Real-time data fetching from Firestore to validate documents.
- **Email Integration:** Uses JavaMail API to send OTPs to users for verification.
- **User Feedback:** Provides dialogs for user notifications during the verification process.

## Screenshots

_Include app screenshots here if available._

## Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/shivani-7024/KYC_Varification
   ```
2. Open the project in Android Studio.
3. Set up Firestore:
   - Create a Firebase project and enable Firestore.
   - Add your `google-services.json` to the project.
4. Configure the email sender:
   - Replace the sender email and password in `GmailSender` class with your credentials.

## Prerequisites

- Android Studio
- Firebase account with Firestore enabled
- JavaMail API setup

## How It Works

1. The user enters a document number in the app.
2. The app checks the document in Firestore.
3. If the document is found, an OTP is generated and sent to the user's email.
4. The user enters the OTP for final verification.
5. Upon successful OTP verification, the document is considered authenticated.

## Technologies Used

- **Android (Java/Kotlin)**
- **Firestore (Firebase)**: For storing and validating document information.
- **JavaMail API**: For sending OTP emails.

## Project Structure

```
app/
│
├── Models/
│   └── GmailSender.java        # Handles email sending via Gmail SMTP
│
├── Activities/
│   └── KycRegistrationActivity.java   # Main activity handling registration and OTP verification
│
├── layout/
│   └── activity_kyc_registration.xml  # UI layout for KYC registration
│
└── Firebase Firestore setup
```

## Usage

1. Launch the app and enter the document number.
2. The app will verify the document with Firestore.
3. If found, you will receive an OTP on the registered email.
4. Enter the OTP to complete the verification process.

## Unit Testing

The app includes unit tests for OTP generation and Firestore validation (details in the `/test` directory). Run the tests using Android Studio's built-in testing tools.

## Potential Benefits

- **Efficiency:** Automates the KYC process, reducing manual verification efforts.
- **Security:** Ensures data privacy with OTP-based authentication.
- **Scalability:** Easy to scale with Firebase Firestore backend.



