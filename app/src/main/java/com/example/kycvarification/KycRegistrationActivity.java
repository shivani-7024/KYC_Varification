package com.example.kycvarification;

import static android.content.ContentValues.TAG;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.kycvarification.Models.FirestoreHelper;
import com.example.kycvarification.Models.GmailSender;
import com.example.kycvarification.Models.KycData;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class KycRegistrationActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private EditText etFullName, etDocumentNumber, etOtp;
    private Button btnVerifyDocument, btnVerifyOtp;
    private TextView tvKycStatus;
    private String generatedOtp;
    private String selectedDocumentType;
    private CardView cardView;

    private TextView tvUserDetails;
    private FirebaseFirestore firestore;

    Spinner documentTypeSpinner;
    FirestoreHelper firestoreHelper;

    private ImageView logout;
    private int check = 1; // Initially set to 1

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_kyc_registration);

        db = FirebaseFirestore.getInstance();
        documentTypeSpinner = findViewById(R.id.document_type_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.document_types_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        documentTypeSpinner.setAdapter(adapter);
        documentTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedDocumentType = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedDocumentType = null; // Handle case when nothing is selected
            }
        });
        // Initialize UI elements
        etFullName = findViewById(R.id.etFullName);
        etDocumentNumber = findViewById(R.id.etDocumentNumber);
        etOtp = findViewById(R.id.etOtp);
        btnVerifyDocument = findViewById(R.id.btnVerifyDocument);
        btnVerifyOtp = findViewById(R.id.btnVerifyOtp);
        tvKycStatus = findViewById(R.id.tvKycStatus);

        // Document Verification Button Click Listener
        btnVerifyDocument.setOnClickListener(view -> verifyDocumentNumber());

        // OTP Verification Button Click Listener
        btnVerifyOtp.setOnClickListener(view -> verifyOtp());

        firestoreHelper = new FirestoreHelper();

        adddata();

        //Logout
        logout = findViewById(R.id.logout);

        SharedPreferences sharedPreferences = getSharedPreferences("LOGIN", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("check", check);
        editor.apply();

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLogoutConfirmationDialog();
            }
        });

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // Initialize UI element
        tvUserDetails = findViewById(R.id.tvUserDetails);
        cardView = findViewById(R.id.cardView);

    }

    private void showLogoutConfirmationDialog() {
        // Create an AlertDialog to confirm logout
        AlertDialog.Builder builder = new AlertDialog.Builder(KycRegistrationActivity.this);
        builder.setTitle("Logout Confirmation");
        builder.setMessage("Are you sure you want to logout?");

        // "Yes" button
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // User confirmed logout, set check to 0 and perform logout
                check = 0;
                SharedPreferences sharedPreferences = getSharedPreferences("LOGIN", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("check", check);
                editor.apply();
                startActivity(new Intent(KycRegistrationActivity.this, LoginActivity.class));
                finish();

                Toast.makeText(KycRegistrationActivity.this, "Logged out", Toast.LENGTH_SHORT).show();
                // Perform any logout operations here, like redirecting to login activity or clearing session
            }
        });

        // "No" button
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Dismiss the dialog
                dialog.dismiss();
            }
        });

        // Show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }


    private void verifyDocumentNumber() {
        String documentNumber = etDocumentNumber.getText().toString();
        String enteredFullName = etFullName.getText().toString().trim(); // Assuming you have an input field for the full name

        if (documentNumber.isEmpty()) {
            Log.d("DocumentVerification", "No document number entered by user");
            showDialog("Error", "Please enter a document number");
            return;
        }

        if (enteredFullName.isEmpty()) {
            Log.d("DocumentVerification", "No full name entered by user");
            showDialog("Error", "Please enter your full name");
            return;
        }

        Log.d("DocumentVerification", "Entered document number: " + documentNumber);
        Log.d("DocumentVerification", "Entered full name: " + enteredFullName);

        // Check if document number exists in Firestore
        db.collection("KYC")
                .whereEqualTo("documentNumber", documentNumber).get().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        // Document found, retrieve details
                        for (DocumentSnapshot document : task.getResult()) {
                            String documentName = document.getString("documentName");
                            String ownerName = document.getString("ownerName");
                            String userEmail = document.getString("email"); // Assuming this field exists in your KYC data

                            Log.d("DocumentVerification", "Document found: " + documentName + " owned by " + ownerName);

                            // Compare entered name and owner name, ignoring case
                            if (enteredFullName.equalsIgnoreCase(ownerName.trim())) {
                                showDialog("Success", "Document verified: " + documentName + " owned by " + ownerName);

                                // Generate OTP
                                generatedOtp = generateOtp();
                                Log.d("OTPGeneration", "Generated OTP: " + generatedOtp);

                                // Send OTP via email
                                sendOtpViaEmail(userEmail, generatedOtp);

                                // Display OTP input fields for user to enter the OTP
                                etOtp.setVisibility(View.VISIBLE);
                                btnVerifyOtp.setVisibility(View.VISIBLE);
                            } else {
                                Log.d("DocumentVerification", "Owner name mismatch: Entered " + enteredFullName + " vs Document " + ownerName);
                                showDialog("Error", "Full name does not match the document owner.");
                            }
                        }
                    } else {
                        Log.d("DocumentVerification", "Document not found in Firestore for document number: " + documentNumber);
                        showDialog("Error", "Document not found.");
                    }
                }).addOnFailureListener(e -> {
                    Log.e("FirestoreError", "Error retrieving document: " + e.getMessage());
                    showDialog("Error", "An error occurred while retrieving the document.");
                });
    }


    private void showDialog(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(KycRegistrationActivity.this);
        builder.setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // Function to send OTP via GmailSender
    private void sendOtpViaEmail(String email, String otp) {
        // Validate the email before attempting to send
        if (email == null || email.trim().isEmpty()) {
            Log.e("EmailSender", "Recipient email is empty or invalid.");
            runOnUiThread(() -> {
                Toast.makeText(KycRegistrationActivity.this, "Recipient email is empty or invalid.", Toast.LENGTH_SHORT).show();
            });
            return; // Stop execution if email is invalid
        }

        String senderEmail = "vanigupta2428@gmail.com"; // Replace with your Gmail
        String senderPassword = "vwqz qlqq xmbx qqhw"; // Replace with your Gmail password

        Log.d("EmailSender", "Preparing to send OTP to: " + email);
        GmailSender gmailSender = new GmailSender(senderEmail, senderPassword);
        new Thread(() -> {
            try {
                Log.d("EmailSender", "Attempting to send email...");
                gmailSender.sendEmail(email, "Your OTP for Document Verification", "Dear user, your OTP is: " + otp);
                Log.d("EmailSender", "Email sent successfully to: " + email);
                runOnUiThread(() -> {
                    Toast.makeText(KycRegistrationActivity.this, "OTP sent to email: " + email, Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                Log.e("EmailSender", "Error sending email: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    Toast.makeText(KycRegistrationActivity.this, "Failed to send OTP email.", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }


    // Function to generate a random 6-digit OTP
    private String generateOtp() {
        int otp = (int) (Math.random() * 900000) + 100000;
        return String.valueOf(otp);
    }

    private String documentNumber;
    private void verifyOtp() {
        String otp = etOtp.getText().toString();

        if (otp.isEmpty()) {
            Log.d("OTPVerification", "No OTP entered by user");
            Toast.makeText(KycRegistrationActivity.this, "Please enter OTP", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.i("OTPVerification", "Entered OTP: " + otp);
        Log.d("OTPVerification", "Generated OTP to match: " + generatedOtp);

        // Perform OTP verification
        if (otp.equals(generatedOtp)) {
            tvKycStatus.setVisibility(View.VISIBLE);
            tvKycStatus.setText("KYC Verified");
            Log.d("OTPVerification", "KYC verification successful");
            // Assuming you have the selected document type and number from the spinner
            selectedDocumentType = documentTypeSpinner.getSelectedItem().toString();
            documentNumber = etDocumentNumber.getText().toString(); // Fetch from an EditText or similar
            fetchKycData(documentNumber);
            Toast.makeText(KycRegistrationActivity.this, "KYC Verification Complete", Toast.LENGTH_SHORT).show();
        } else {
            Log.d("OTPVerification", "Entered OTP does not match generated OTP");
            Toast.makeText(KycRegistrationActivity.this, "Invalid OTP", Toast.LENGTH_SHORT).show();
        }
    }


    String email;

    private void adddata() {
        // Add KYC data for different document types

        SharedPreferences srdf = getSharedPreferences("Email_passing_Login_Page", MODE_PRIVATE);
        email = srdf.getString("email", "");

        if(email.isEmpty()) {
            SharedPreferences Srdf = getSharedPreferences("Email_passing", MODE_PRIVATE);
            email = Srdf.getString("email", "");
        }

        Log.i("Email : ", email);
        addAadhaarData();
        addNationalIdData();
        addPassportData();
        addDriverLicenseData();
        addVoterIdData();
    }

    private void addAadhaarData() {

        // Aadhaar Entry 1
        Map<String, Object> aadhaarData1 = new HashMap<>();
        aadhaarData1.put("fullName", "Rajesh Sharma");
        aadhaarData1.put("address", "Sector 10, Noida, Uttar Pradesh");
        aadhaarData1.put("gender", "Male");
        aadhaarData1.put("dob", "1995-04-15");
        aadhaarData1.put("fingerprints", "Fingerprint data for Rajesh");
        aadhaarData1.put("irisScans", "Iris scan data for Rajesh");
        aadhaarData1.put("photoUrl", "http://example.com/photo_rajesh.jpg");
        aadhaarData1.put("phoneNumber", "9876543210");
        aadhaarData1.put("email", email);
        aadhaarData1.put("documentNumber", "110222054403");
        aadhaarData1.put("documentName", "Aadhaar Card");
        aadhaarData1.put("userPhoneNumber", "+919876543210");
        firestoreHelper.addOrUpdateKycData("Aadhaar_1", aadhaarData1);

        // Aadhaar Entry 2
        Map<String, Object> aadhaarData2 = new HashMap<>();
        aadhaarData2.put("fullName", "Pooja Gupta");
        aadhaarData2.put("address", "MG Road, Bangalore, Karnataka");
        aadhaarData2.put("gender", "Female");
        aadhaarData2.put("dob", "1998-08-20");
        aadhaarData2.put("fingerprints", "Fingerprint data for Pooja");
        aadhaarData2.put("irisScans", "Iris scan data for Pooja");
        aadhaarData2.put("photoUrl", "http://example.com/photo_pooja.jpg");
        aadhaarData2.put("phoneNumber", "9123456789");
        aadhaarData2.put("email", email);
        aadhaarData2.put("documentNumber", "110222054404");
        aadhaarData2.put("documentName", "Aadhaar Card");
        aadhaarData2.put("userPhoneNumber", "+919123456789");
        firestoreHelper.addOrUpdateKycData("Aadhaar_2", aadhaarData2);

        // Aadhaar Entry 3
        Map<String, Object> aadhaarData3 = new HashMap<>();
        aadhaarData3.put("fullName", "Amit Verma");
        aadhaarData3.put("address", "Pahar Ganj, Delhi");
        aadhaarData3.put("gender", "Male");
        aadhaarData3.put("dob", "1992-12-10");
        aadhaarData3.put("fingerprints", "Fingerprint data for Amit");
        aadhaarData3.put("irisScans", "Iris scan data for Amit");
        aadhaarData3.put("photoUrl", "http://example.com/photo_amit.jpg");
        aadhaarData3.put("phoneNumber", "9988776655");
        aadhaarData3.put("email", email);
        aadhaarData3.put("documentNumber", "110222054405");
        aadhaarData3.put("documentName", "Aadhaar Card");
        aadhaarData3.put("userPhoneNumber", "+919998776655");
        firestoreHelper.addOrUpdateKycData("Aadhaar_3", aadhaarData3);
    }

    private void addNationalIdData() {

        // National ID Entry 1
        Map<String, Object> nationalIdData1 = new HashMap<>();
        nationalIdData1.put("fullName", "Vikram Singh");
        nationalIdData1.put("dob", "1985-05-05");
        nationalIdData1.put("gender", "Male");
        nationalIdData1.put("nationality", "Indian");
        nationalIdData1.put("documentNumber", "NID123456");
        nationalIdData1.put("address", "Jayanagar, Bangalore");
        nationalIdData1.put("photograph", "http://example.com/photo_vikram.jpg");
        nationalIdData1.put("signature", "http://example.com/signature_vikram.jpg");
        nationalIdData1.put("expiryDate", "2030-05-05");
        nationalIdData1.put("documentName", "National ID Card");
        nationalIdData1.put("userPhoneNumber", "+918765432109");
        nationalIdData1.put("email", email);
        firestoreHelper.addOrUpdateKycData("National_ID_1", nationalIdData1);

        // National ID Entry 2
        Map<String, Object> nationalIdData2 = new HashMap<>();
        nationalIdData2.put("fullName", "Anjali Rao");
        nationalIdData2.put("dob", "1990-06-15");
        nationalIdData2.put("gender", "Female");
        nationalIdData2.put("nationality", "Indian");
        nationalIdData2.put("documentNumber", "NID654321");
        nationalIdData2.put("address", "Dadar, Mumbai");
        nationalIdData2.put("photograph", "http://example.com/photo_anjali.jpg");
        nationalIdData2.put("signature", "http://example.com/signature_anjali.jpg");
        nationalIdData2.put("expiryDate", "2035-06-15");
        nationalIdData2.put("documentName", "National ID Card");
        nationalIdData2.put("userPhoneNumber", "+919876543210");
        nationalIdData2.put("email", email);
        firestoreHelper.addOrUpdateKycData("National_ID_2", nationalIdData2);

        // National ID Entry 3
        Map<String, Object> nationalIdData3 = new HashMap<>();
        nationalIdData3.put("fullName", "Ravi Kumar");
        nationalIdData3.put("dob", "1992-07-25");
        nationalIdData3.put("gender", "Male");
        nationalIdData3.put("nationality", "Indian");
        nationalIdData3.put("documentNumber", "NID789456");
        nationalIdData3.put("address", "Nehru Place, Delhi");
        nationalIdData3.put("photograph", "http://example.com/photo_ravi.jpg");
        nationalIdData3.put("signature", "http://example.com/signature_ravi.jpg");
        nationalIdData3.put("expiryDate", "2032-07-25");
        nationalIdData3.put("documentName", "National ID Card");
        nationalIdData3.put("userPhoneNumber", "+919123456789");
        nationalIdData3.put("email", email);
        firestoreHelper.addOrUpdateKycData("National_ID_3", nationalIdData3);
    }

    private void addPassportData() {

        // Passport Entry 1
        Map<String, Object> passportData1 = new HashMap<>();
        passportData1.put("fullName", "Suresh Mehta");
        passportData1.put("dob", "1990-01-01");
        passportData1.put("nationality", "Indian");
        passportData1.put("documentNumber", "P987654321");
        passportData1.put("photograph", "http://example.com/photo_suresh.jpg");
        passportData1.put("signature", "http://example.com/signature_suresh.jpg");
        passportData1.put("expiryDate", "2030-01-01");
        passportData1.put("placeOfIssue", "Delhi");
        passportData1.put("dateOfIssue", "2020-01-01");
        passportData1.put("documentName", "Passport");
        passportData1.put("userPhoneNumber", "+919876543201");
        passportData1.put("email", email);
        firestoreHelper.addOrUpdateKycData("Passport_1", passportData1);

        // Passport Entry 2
        Map<String, Object> passportData2 = new HashMap<>();
        passportData2.put("fullName", "Geeta Anand");
        passportData2.put("dob", "1985-03-15");
        passportData2.put("nationality", "Indian");
        passportData2.put("documentNumber", "P987654322");
        passportData2.put("photograph", "http://example.com/photo_geeta.jpg");
        passportData2.put("signature", "http://example.com/signature_geeta.jpg");
        passportData2.put("expiryDate", "2033-03-15");
        passportData2.put("placeOfIssue", "Mumbai");
        passportData2.put("dateOfIssue", "2020-03-15");
        passportData2.put("documentName", "Passport");
        passportData2.put("userPhoneNumber", "+919876543202");
        passportData2.put("email", email);
        firestoreHelper.addOrUpdateKycData("Passport_2", passportData2);

        // Passport Entry 3
        Map<String, Object> passportData3 = new HashMap<>();
        passportData3.put("fullName", "Rakesh Rathi");
        passportData3.put("dob", "1993-09-10");
        passportData3.put("nationality", "Indian");
        passportData3.put("documentNumber", "P987654323");
        passportData3.put("photograph", "http://example.com/photo_rakesh.jpg");
        passportData3.put("signature", "http://example.com/signature_rakesh.jpg");
        passportData3.put("expiryDate", "2035-09-10");
        passportData3.put("placeOfIssue", "Hyderabad");
        passportData3.put("dateOfIssue", "2020-09-10");
        passportData3.put("documentName", "Passport");
        passportData3.put("userPhoneNumber", "+919876543203");
        passportData3.put("email", email);
        firestoreHelper.addOrUpdateKycData("Passport_3", passportData3);
    }

    private void addDriverLicenseData() {

        // Driver License Entry 1
        Map<String, Object> driverLicenseData1 = new HashMap<>();
        driverLicenseData1.put("fullName", "Manoj Tiwari");
        driverLicenseData1.put("dob", "1988-11-22");
        driverLicenseData1.put("documentNumber", "DL123456789012");
        driverLicenseData1.put("address", "Kanpur, Uttar Pradesh");
        driverLicenseData1.put("issueDate", "2020-11-22");
        driverLicenseData1.put("expiryDate", "2030-11-22");
        driverLicenseData1.put("photograph", "http://example.com/photo_manoj.jpg");
        driverLicenseData1.put("signature", "http://example.com/signature_manoj.jpg");
        driverLicenseData1.put("documentName", "Driver License");
        driverLicenseData1.put("userPhoneNumber", "+919876543204");
        driverLicenseData1.put("email", email);
        firestoreHelper.addOrUpdateKycData("Driver_License_1", driverLicenseData1);

        // Driver License Entry 2
        Map<String, Object> driverLicenseData2 = new HashMap<>();
        driverLicenseData2.put("fullName", "Deepak Joshi");
        driverLicenseData2.put("dob", "1991-02-18");
        driverLicenseData2.put("documentNumber", "DL987654321012");
        driverLicenseData2.put("address", "Indore, Madhya Pradesh");
        driverLicenseData2.put("issueDate", "2021-02-18");
        driverLicenseData2.put("expiryDate", "2031-02-18");
        driverLicenseData2.put("photograph", "http://example.com/photo_deepak.jpg");
        driverLicenseData2.put("signature", "http://example.com/signature_deepak.jpg");
        driverLicenseData2.put("documentName", "Driver License");
        driverLicenseData2.put("userPhoneNumber", "+919876543205");
        driverLicenseData2.put("email", email);
        firestoreHelper.addOrUpdateKycData("Driver_License_2", driverLicenseData2);

        // Driver License Entry 3
        Map<String, Object> driverLicenseData3 = new HashMap<>();
        driverLicenseData3.put("fullName", "Sunita Yadav");
        driverLicenseData3.put("dob", "1985-03-30");
        driverLicenseData3.put("documentNumber", "DL543216789012");
        driverLicenseData3.put("address", "Bhopal, Madhya Pradesh");
        driverLicenseData3.put("issueDate", "2022-03-30");
        driverLicenseData3.put("expiryDate", "2032-03-30");
        driverLicenseData3.put("photograph", "http://example.com/photo_sunita.jpg");
        driverLicenseData3.put("signature", "http://example.com/signature_sunita.jpg");
        driverLicenseData3.put("documentName", "Driver License");
        driverLicenseData3.put("userPhoneNumber", "+919876543206");
        driverLicenseData3.put("email", email);
        firestoreHelper.addOrUpdateKycData("Driver_License_3", driverLicenseData3);
    }

    private void addVoterIdData() {

        // Voter ID Entry 1
        Map<String, Object> voterIdData1 = new HashMap<>();
        voterIdData1.put("fullName", "Suman Gupta");
        voterIdData1.put("dob", "1990-10-15");
        voterIdData1.put("gender", "Female");
        voterIdData1.put("state", "Uttar Pradesh");
        voterIdData1.put("documentNumber", "VOTER123456789");
        voterIdData1.put("address", "Bareilly, Uttar Pradesh");
        voterIdData1.put("photograph", "http://example.com/photo_suman.jpg");
        voterIdData1.put("signature", "http://example.com/signature_suman.jpg");
        voterIdData1.put("documentName", "Voter ID");
        voterIdData1.put("userPhoneNumber", "+919876543207");
        voterIdData1.put("email", email);
        firestoreHelper.addOrUpdateKycData("Voter_ID_1", voterIdData1);

        // Voter ID Entry 2
        Map<String, Object> voterIdData2 = new HashMap<>();
        voterIdData2.put("fullName", "Rahul Sharma");
        voterIdData2.put("dob", "1989-12-01");
        voterIdData2.put("gender", "Male");
        voterIdData2.put("state", "Maharashtra");
        voterIdData2.put("documentNumber", "VOTER987654321");
        voterIdData2.put("address", "Thane, Maharashtra");
        voterIdData2.put("photograph", "http://example.com/photo_rahul.jpg");
        voterIdData2.put("signature", "http://example.com/signature_rahul.jpg");
        voterIdData2.put("documentName", "Voter ID");
        voterIdData2.put("userPhoneNumber", "+919876543208");
        voterIdData2.put("email", email);
        firestoreHelper.addOrUpdateKycData("Voter_ID_2", voterIdData2);

        // Voter ID Entry 3
        Map<String, Object> voterIdData3 = new HashMap<>();
        voterIdData3.put("fullName", "Arun Nair");
        voterIdData3.put("dob", "1995-08-30");
        voterIdData3.put("gender", "Male");
        voterIdData3.put("state", "Kerala");
        voterIdData3.put("documentNumber", "VOTER123459876");
        voterIdData3.put("address", "Kochi, Kerala");
        voterIdData3.put("photograph", "http://example.com/photo_arun.jpg");
        voterIdData3.put("signature", "http://example.com/signature_arun.jpg");
        voterIdData3.put("documentName", "Voter ID");
        voterIdData3.put("userPhoneNumber", "+919876543209");
        voterIdData3.put("email", email);
        firestoreHelper.addOrUpdateKycData("Voter_ID_3", voterIdData3);
    }

    //Fetch the data from firebase
    private void fetchKycData(String documentNumber) {
        firestore.collection("KYC") // Replace with your actual collection name
                .whereEqualTo("documentNumber", documentNumber)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult(); // Get the result of the query
                        Log.d(TAG, "Query successful, number of documents found: " + querySnapshot.size());
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            for (QueryDocumentSnapshot document : querySnapshot) {
                                KycData kycData = document.toObject(KycData.class);
                                displayKycData(kycData);
                            }
                        } else {
                            Log.w(TAG, "No documents found for this number");
                            Toast.makeText(KycRegistrationActivity.this, "No documents found for this number", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e(TAG, "Error getting documents: " + task.getException());
                        Toast.makeText(KycRegistrationActivity.this, "Error getting documents: " + task.getException(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    //Display the data
    private void displayKycData(KycData kycData) {
        String details = "Name: " + kycData.getFullName() + "\n" +
                "Address: " + kycData.getAddress() + "\n" +
                "Gender: " + kycData.getGender() + "\n" +
                "DOB: " + kycData.getDob() + "\n" +
                "Document Number: " + kycData.getDocumentNumber() + "\n" +
                "Document Name: " + kycData.getDocumentName() + "\n" +
                "Email: " + kycData.getEmail() + "\n" +
                "Phone Number: " + kycData.getUserPhoneNumber() + "\n" +
                "Expiry Date: " + kycData.getExpiryDate() + "\n" +
                "Photograph URL: " + kycData.getPhotograph() + "\n" +
                "Signature URL: " + kycData.getSignature();

        tvUserDetails.setText(details);
        cardView.setVisibility(View.VISIBLE);
    }
}
