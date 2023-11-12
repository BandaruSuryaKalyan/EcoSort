package com.suryakalyan.ecosort;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {
    ImageView underTheCard, googleAuthButton;
    TextView titleTextView, createAccountTextView, forgotPassword;
    AppCompatButton loginButton, signupButton;
    
    EditText emailEditText, passwordEditText;
    private FirebaseAuth firebaseAuth;
    
    private static final int RC_SIGN_IN = 123;
    private GoogleSignInOptions googleSignInOptions;
    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    
    @Override
    protected void onCreate( @Nullable Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.login_activity );
        
        underTheCard = findViewById( R.id.illustration_under_card_view );
        titleTextView = findViewById( R.id.title );
        createAccountTextView = findViewById( R.id.text_view_create_account );
        loginButton = findViewById( R.id.button_login );
        signupButton = findViewById( R.id.button_signup );
        emailEditText = findViewById( R.id.edit_text_email );
        passwordEditText = findViewById( R.id.edit_text_password );
        forgotPassword = findViewById( R.id.text_view_forgot_password );
        googleAuthButton = findViewById( R.id.image_view_google );
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            underTheCard.setRenderEffect( RenderEffect.createBlurEffect( 10, 10, Shader.TileMode.MIRROR ) );
        }
        
        // Set initial visibility and text
        loginButton.setVisibility( View.VISIBLE );
        signupButton.setVisibility( View.INVISIBLE );
        titleTextView.setText( "Log in" );
        
        // Handle create account text view click
        createAccountTextView.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick( View v ) {
                // Switch between login and signup buttons
                if (loginButton.getVisibility() == View.VISIBLE) {
                    loginButton.setVisibility( View.INVISIBLE );
                    signupButton.setVisibility( View.VISIBLE );
                    titleTextView.setText( "Signup" );
                } else {
                    loginButton.setVisibility( View.VISIBLE );
                    signupButton.setVisibility( View.GONE );
                    titleTextView.setText( "Log in" );
                }
            }
        } );
        
        firebaseAuth = FirebaseAuth.getInstance();
        
        loginButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick( View v ) {
                signIn();
            }
        } );
        
        signupButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick( View v ) {
                signUp();
            }
        } );
        
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder( GoogleSignInOptions.DEFAULT_SIGN_IN )
                .requestIdToken( getString( R.string.default_web_client_id ) )
                .requestEmail()
                .build();
        
        googleSignInClient = GoogleSignIn.getClient( this, gso );
        
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult( ActivityResult result ) {
                        if (result.getResultCode() == RESULT_OK) {
                            Intent data = result.getData();
                            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent( data );
                            handleGoogleSignInResult( task );
                        } else {
                            Toast.makeText( getApplicationContext(), "Google Sign-In failed", Toast.LENGTH_SHORT ).show();
                        }
                    }
                } );
        
        googleAuthButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick( View view ) {
                Intent signInIntent = googleSignInClient.getSignInIntent();
                googleSignInLauncher.launch( signInIntent );
            }
        } );
        
        forgotPassword.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick( View view ) {
                ForgotPassword();
            }
        } );
        
        
    }
    private void ForgotPassword() {
        
        String email = emailEditText.getText().toString().trim();
        
        if (TextUtils.isEmpty( email )) {
            emailEditText.setError( "Required" );
            return;
        }
        String gmailPattern = "[a-zA-Z0-9._%+-]+@gmail\\.com";
        
        if (!email.matches(gmailPattern)) {
            emailEditText.setError("Invalid Gmail format");
            return;
        }
        
        
        firebaseAuth.sendPasswordResetEmail( email )
                .addOnSuccessListener( new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess( Void aVoid ) {
                        // Password reset email sent successfully
                        Toast.makeText( LoginActivity.this, "Password reset email sent",
                                Toast.LENGTH_SHORT ).show();
                    }
                } )
                .addOnFailureListener( new OnFailureListener() {
                    @Override
                    public void onFailure( @NonNull Exception e ) {
                        // Error occurred while sending the password reset email
                        Toast.makeText( LoginActivity.this, "Failed to send password reset email",
                                Toast.LENGTH_SHORT ).show();
                    }
                } );
    }
    private void signIn() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        
        if (TextUtils.isEmpty( email )) {
            emailEditText.setError( "Required" );
            return;
        }
        String gmailPattern = "[a-zA-Z0-9._%+-]+@gmail\\.com";
        
        if (!email.matches( gmailPattern )) {
            emailEditText.setError( "Invalid Gmail format" );
            return;
        }
        if (TextUtils.isEmpty( password )) {
            passwordEditText.setError( "Required" );
            passwordEditText.setTextColor( Color.parseColor( "#000000" ) );
            return;
        }
        
        String username = email.split( "@" )[ 0 ];
        
        firebaseAuth.signInWithEmailAndPassword( email, password )
                .addOnCompleteListener( this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete( @NonNull Task<AuthResult> task ) {
                        if (task.isSuccessful()) {
                            
                            SharedPreferences preference = getSharedPreferences( "UserLoginActivity", MODE_PRIVATE );
                            SharedPreferences.Editor editor = preference.edit();
                            editor.putBoolean( "login", true );
                            editor.putString( "UserEmailPref", username );
                            editor.apply();
                            
                            Intent UserNameIntent = new Intent( LoginActivity.this, MainActivity.class );
                            startActivity( UserNameIntent );
                            finish();
                            
                        } else {
                            passwordEditText.setTextColor( Color.parseColor( "#FF0000" ) );
                            Toast.makeText( getApplicationContext(), "Authentication failed!\nEmail Doesn't match Please Sign Up", Toast.LENGTH_LONG ).show();
                        }
                    }
                } );
    }
    
    private void signUp() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        
        if (TextUtils.isEmpty( email )) {
            Toast.makeText( getApplicationContext(), "Enter email address!", Toast.LENGTH_SHORT ).show();
            return;
        }
        String gmailPattern = "[a-zA-Z0-9._%+-]+@gmail\\.com";
        
        if (!email.matches( gmailPattern )) {
            emailEditText.setError( "Invalid Gmail format" );
            return;
        }
        if (TextUtils.isEmpty( password )) {
            Toast.makeText( getApplicationContext(), "Enter password!", Toast.LENGTH_SHORT ).show();
            return;
        }
        
        String username = email.split( "@" )[ 0 ];
        
        firebaseAuth.createUserWithEmailAndPassword( email, password )
                .addOnCompleteListener( this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete( @NonNull Task<AuthResult> task ) {
                        if (task.isSuccessful()) {
                            
                            SharedPreferences preference = getSharedPreferences( "UserLoginActivity", MODE_PRIVATE );
                            SharedPreferences.Editor editor = preference.edit();
                            editor.putBoolean( "login", true );
                            editor.putString( "UserEmailPref", username );
                            editor.apply();
                            
                            Intent UserNameIntent = new Intent( LoginActivity.this, MainActivity.class );
                            startActivity( UserNameIntent );
                            finish();
                        } else {
                            Toast.makeText( getApplicationContext(), "Registration failed!\nPlease try Again...",
                                    Toast.LENGTH_SHORT ).show();
                        }
                    }
                } );
    }
    
    private void handleGoogleSignInResult( Task<GoogleSignInAccount> completedTask ) {
        try {
            GoogleSignInAccount account = completedTask.getResult( ApiException.class );
            firebaseAuthWithGoogle( account );
        } catch (ApiException e) {
            Toast.makeText( getApplicationContext(), "Google Sign-In failed",
                    Toast.LENGTH_SHORT ).show();
        }
    }
    
    private void firebaseAuthWithGoogle( GoogleSignInAccount account ) {
        AuthCredential credential = GoogleAuthProvider.getCredential( account.getIdToken(), null );
        
        String email = account.getEmail();
        assert email != null;
        String username = email.split( "@" )[ 0 ];
        
        
        firebaseAuth.signInWithCredential( credential )
                .addOnCompleteListener( this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete( @NonNull Task<AuthResult> task ) {
                        if (task.isSuccessful()) {
                            
                            SharedPreferences preference = getSharedPreferences( "UserLoginActivity", MODE_PRIVATE );
                            SharedPreferences.Editor editor = preference.edit();
                            editor.putBoolean( "login", true );
                            editor.putString( "UserEmailPref", username );
                            editor.apply();
                            
                            Intent UserNameIntent = new Intent( LoginActivity.this, MainActivity.class );
                            startActivity( UserNameIntent );
                            finish();
                        } else {
                            Toast.makeText( getApplicationContext(), "Firebase Authentication failed", Toast.LENGTH_SHORT ).show();
                        }
                    }
                } );
        
    }
}