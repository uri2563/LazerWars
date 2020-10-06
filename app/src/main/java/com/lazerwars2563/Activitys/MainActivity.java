package com.lazerwars2563.Activitys;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.lazerwars2563.util.UserClient;
import com.lazerwars2563.Class.UserDetails;
import com.lazerwars2563.R;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static String TAG = "MainActivity";

    private static final int RC_SIGN_IN = 1997;
    private List<AuthUI.IdpConfig> providers;

    private Button chooseRoomBtn;
    private Button logOutBtn;

    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        chooseRoomBtn = findViewById(R.id.choose_room_btn);
        logOutBtn = findViewById(R.id.log_out_btn);

        //if user is not logged on
        user = FirebaseAuth.getInstance().getCurrentUser();
        if(user == null)
        {
            LogIn();
        }
        else
        {
            SetUserDetails();
        }

        chooseRoomBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //go to nextPage
                Intent intent = new Intent(MainActivity.this, ChooseRoomActivity.class);
                startActivity(intent);
            }
        });
        logOutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LogOut();
            }
        });

    }

    private void LogIn()
    {
            //init providers
            providers  = Arrays.asList(
                    new AuthUI.IdpConfig.EmailBuilder().build(),
                    new AuthUI.IdpConfig.GoogleBuilder().build());

            themeAndLogo();
    }

    //set theme and logo for first page
    public void themeAndLogo() {
        // [START auth_fui_theme_logo]
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .setLogo(R.drawable.logolazer)      // Set logo drawable
                        .setTheme(R.style.AppTheme)      // Set theme
                        .build(),
                RC_SIGN_IN);
        // [END auth_fui_theme_logo]
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                // Successfully signed in
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                //show email in toast
                Toast.makeText(this, ""+user.getEmail(), Toast.LENGTH_SHORT).show();

                //setUserDetails
                SetUserDetails();
            }
            else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                if(response != null)
                {
                    Toast.makeText(this, ""+response.getError().getMessage(), Toast.LENGTH_SHORT).show();
                }
                else
                {
                    LogOut();
                }
            }
        }
    }

    private void SetUserDetails() {
            Toast.makeText(this, "SetUserDetails", Toast.LENGTH_SHORT).show();

            FirebaseUser userFb = FirebaseAuth.getInstance().getCurrentUser();

            String gameId = "None";
            if(UserClient.getInstance() != null && UserClient.getInstance().getGameId() != null)
            {
               gameId = UserClient.getInstance().getGameId();
            }

            UserDetails user = new UserDetails(userFb.getDisplayName(),userFb.getUid());
            UserClient.getInstance().setUser(user);
            UserClient.getInstance().setGameId(gameId);
    }

    @Override
    public void onBackPressed() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage("Are you sure you want to LogOut?")
                .setCancelable(false)
                .setPositiveButton("yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        LogOut();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();


    }

    private void LogOut()
    {
        AuthUI.getInstance()
                .signOut(this)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    public void onComplete(@NonNull Task<Void> task) {
                        user = null;
                        LogIn();
                    }
                });
    }

}
