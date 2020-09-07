package com.lazerwars2563.Activitys;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.lazerwars2563.util.UserClient;
import com.lazerwars2563.Class.UserDetails;
import com.lazerwars2563.R;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 1997;
    private List<AuthUI.IdpConfig> providers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


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

                //go to nextPage
                LeaveToNextPage();
                //setUserDetails
                SetUserDetails();
            }
            else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                Toast.makeText(this, ""+response.getError().getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void SetUserDetails() {
        FirebaseUser userFb = FirebaseAuth.getInstance().getCurrentUser();
        UserDetails user = new UserDetails(userFb.getDisplayName(),userFb.getUid());
        UserClient.getInstance().setUser(user);
        UserClient.getInstance().setGameId("None");
    }

    private void LeaveToNextPage() {
        //Intent intent = new Intent(this, ChooseRoomActivity.class);
        Intent intent = new Intent(this, ChooseRoomActivity.class);

        startActivity(intent);
    }

}
