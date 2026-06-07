package com.example.authentication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import android.widget.LinearLayout;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    private ActivityResultLauncher<Intent> emailAppLauncher;

    private TextView tvSwitchAuthMode;
    private LinearLayout containerSignUp;
    private LinearLayout containerLogin;

    private EditText etSignUpUsername, etSignUpEmail, etSignUpPassword, etSignUpConfirmPassword;
    private Button btnSubmitSignUp;

    private EditText etLoginEmail, etLoginPassword;
    private Button btnSubmitLogin;
    private ImageButton btnGoogleSignIn;
    private TextView tvForgotPassword;

    private boolean isSignUpModeActive = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tvSwitchAuthMode = findViewById(R.id.tvSwitchAuthMode);

        containerSignUp = findViewById(R.id.containerSignUp);
        containerLogin = findViewById(R.id.containerLogin);

        etSignUpUsername = findViewById(R.id.etSignUpUsername);
        etSignUpEmail = findViewById(R.id.etSignUpEmail);
        etSignUpPassword = findViewById(R.id.etSignUpPassword);
        etSignUpConfirmPassword = findViewById(R.id.etSignUpConfirmPassword);
        btnSubmitSignUp = findViewById(R.id.btnSubmitSignUp);

        etLoginEmail = findViewById(R.id.etLoginEmail);
        etLoginPassword = findViewById(R.id.etLoginPassword);
        btnSubmitLogin = findViewById(R.id.btnSubmitLogin);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("YOUR_DEFAULT_WEB_CLIENT_ID_PLACEHOLDER")
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        try {
                            GoogleSignInAccount account = task.getResult(ApiException.class);
                            if (account != null) {
                                configureFirebaseWithGoogleAccount(account);
                            }
                        } catch (ApiException e) {
                            Toast.makeText(MainActivity.this, "Google sign in error code: " + e.getStatusCode(), Toast.LENGTH_LONG).show();
                        }
                    }
                }
        );

        emailAppLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    showLoginScreenAndClearFields();
                }
        );

        tvSwitchAuthMode.setOnClickListener(v -> toggleAuthenticationInterfaceMode());
        tvForgotPassword.setOnClickListener(v -> handleForgotPassword());

        btnSubmitSignUp.setOnClickListener(v -> processUserRegistrationAccountRequest());
        btnSubmitLogin.setOnClickListener(v -> processUserLoginVerificationSession());
        btnGoogleSignIn.setOnClickListener(v -> triggerGoogleImplicitAuthIntent());

        toggleAuthenticationInterfaceMode();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            navigateBasedOnUserRole(currentUser);
        }
    }

    private void toggleAuthenticationInterfaceMode() {
        if (isSignUpModeActive) {
            containerSignUp.setVisibility(View.VISIBLE);
            containerLogin.setVisibility(View.GONE);
            tvSwitchAuthMode.setText(getString(R.string.loginstring));
            isSignUpModeActive = false;
        } else {
            containerSignUp.setVisibility(View.GONE);
            containerLogin.setVisibility(View.VISIBLE);
            tvSwitchAuthMode.setText(R.string.signupstring);
            isSignUpModeActive = true;
        }
    }

    private void processUserRegistrationAccountRequest() {
        String inputUsername = etSignUpUsername.getText().toString().trim();
        String inputEmail = etSignUpEmail.getText().toString().trim();
        String inputPassword = etSignUpPassword.getText().toString().trim();
        String inputConfirmPassword = etSignUpConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(inputUsername)) {
            etSignUpUsername.setError("Username is required.");
            etSignUpUsername.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(inputEmail) || !Patterns.EMAIL_ADDRESS.matcher(inputEmail).matches()) {
            etSignUpEmail.setError("Provide a valid operational email.");
            etSignUpEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(inputPassword) || inputPassword.length() < 6) {
            etSignUpPassword.setError("Password constraint parameter requires minimum 6 characters.");
            etSignUpPassword.requestFocus();
            return;
        }

        if (!inputPassword.equals(inputConfirmPassword)) {
            etSignUpConfirmPassword.setError("Mismatched verification keys. Please make sure the verification keys are both the same.");
            etSignUpConfirmPassword.requestFocus();
            return;
        }

        mAuth.createUserWithEmailAndPassword(inputEmail, inputPassword)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveUserToFirestore(user.getUid(), inputUsername, inputEmail);
                        }
                    } else {
                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            Toast.makeText(MainActivity.this, "Identity collision: Email already in use.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(MainActivity.this, "Registration aborted: " + task.getException().getLocalizedMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void saveUserToFirestore(String userId, String username, String email) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("username", username);
        userMap.put("email", email);
        userMap.put("createdAt", System.currentTimeMillis());

        db.collection("users").document(userId).set(userMap)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(MainActivity.this, "Account provisioned successfully!", Toast.LENGTH_SHORT).show();
                    navigateBasedOnUserRole(mAuth.getCurrentUser());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Failed to save user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void processUserLoginVerificationSession() {
        String inputEmail = etLoginEmail.getText().toString().trim();
        String inputPassword = etLoginPassword.getText().toString().trim();

        if (TextUtils.isEmpty(inputEmail)) {
            etLoginEmail.setError("Please enter your account email.");
            etLoginEmail.requestFocus();
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(inputPassword)) {
            etLoginPassword.setError("Password field required.");
            etLoginPassword.requestFocus();
            Toast.makeText(this, "Please enter your password", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(inputEmail, inputPassword)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(MainActivity.this, "Welcome Operator! Connection Established.", Toast.LENGTH_SHORT).show();
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            navigateBasedOnUserRole(user);
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Access Denied: " + task.getException().getLocalizedMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void handleForgotPassword() {
        String userEmail = etLoginEmail.getText().toString().trim();

        if (TextUtils.isEmpty(userEmail)) {
            new AlertDialog.Builder(this)
                    .setTitle("Email Required")
                    .setMessage("Please enter your email address to reset password.")
                    .setPositiveButton("OK", (dialog, which) -> etLoginEmail.requestFocus())
                    .show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(userEmail).matches()) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.sendPasswordResetEmail(userEmail)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(MainActivity.this, "Password reset email sent to " + userEmail, Toast.LENGTH_LONG).show();

                    Intent emailIntent = new Intent(Intent.ACTION_MAIN);
                    emailIntent.addCategory(Intent.CATEGORY_APP_EMAIL);
                    emailIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    if (emailIntent.resolveActivity(getPackageManager()) != null) {
                        emailAppLauncher.launch(Intent.createChooser(emailIntent, "Open Email App"));
                    } else {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW);
                        browserIntent.setData(Uri.parse("https://mail.google.com"));
                        emailAppLauncher.launch(browserIntent);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Failed to send reset email: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showLoginScreenAndClearFields() {
        if (!isSignUpModeActive) {
            containerSignUp.setVisibility(View.GONE);
            containerLogin.setVisibility(View.VISIBLE);
            tvSwitchAuthMode.setText("Don't have an account? Sign Up");
            isSignUpModeActive = true;
        }

        etLoginEmail.setText("");
        etLoginPassword.setText("");
        etLoginEmail.requestFocus();

        Toast.makeText(this, "Password reset email sent! Please check your email, change your password, then login with your new password.", Toast.LENGTH_LONG).show();
    }

    private void triggerGoogleImplicitAuthIntent() {
        Intent signIntent = mGoogleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signIntent);
    }

    private void configureFirebaseWithGoogleAccount(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String username = account.getDisplayName();
                            if (username == null || username.isEmpty()) {
                                username = user.getEmail().split("@")[0];
                            }
                            String finalUsername = username;
                            saveGoogleUserToFirestore(user.getUid(), finalUsername, user.getEmail());
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Firebase cross-validation failure.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveGoogleUserToFirestore(String userId, String username, String email) {
        db.collection("users").document(userId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().exists()) {
                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("username", username);
                        userMap.put("email", email);
                        userMap.put("createdAt", System.currentTimeMillis());

                        db.collection("users").document(userId).set(userMap);
                    }
                    navigateBasedOnUserRole(mAuth.getCurrentUser());
                });
    }

    private void navigateBasedOnUserRole(FirebaseUser user) {
        if (user != null && user.getEmail() != null) {
            String email = user.getEmail().toLowerCase();

            if (email.equals("admin@waterfilter.com") || email.endsWith("@admin.com")) {
                // Admin goes to Admin Dashboard
                Intent intent = new Intent(MainActivity.this, AdminDashboardActivity.class);
                startActivity(intent);
                Toast.makeText(this, "Welcome Admin!", Toast.LENGTH_SHORT).show();
            } else {
                // Regular user goes to Emir's Homepage
                Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                // Pass username to homepage
                String username = user.getDisplayName();
                if (username == null) {
                    username = user.getEmail().split("@")[0];
                }
                intent.putExtra("USERNAME", username);
                startActivity(intent);
                Toast.makeText(this, "Welcome " + username + "!", Toast.LENGTH_SHORT).show();
            }
            finish();
        }
    }
}