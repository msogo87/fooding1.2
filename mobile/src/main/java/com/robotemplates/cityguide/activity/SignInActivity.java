package com.robotemplates.cityguide.activity;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.robotemplates.cityguide.R;
import com.robotemplates.cityguide.common.SignInMethodEnum;
import com.robotemplates.cityguide.communication.DataImporter;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.Arrays;

/**
 * Created by msogolovsky on 24/07/2016.
 */
public class SignInActivity extends FragmentActivity implements GoogleApiClient.OnConnectionFailedListener
{
    private final static String TAG                       = "SignInActivity";
    private final static long STARTUP_DELAY               = 0;
    private final static long STARTUP_ITEM_DELAY          = 1000;
    private final static long ANIM_LOGO_DURATION          = 2000;
    private final static long ANIM_ITEM_DURATION          = 1000;

    private final static int TOTAL_NUM_OF_SIGN_IN_METHODS = 2;
    private final static int RC_GP_SIGN_IN                = 100; // google play sign-in request code
    private static int RC_FB_SIGN_IN;                            // facebook sign-in request code

    /* facebook's request codes:
    public enum RequestCodeOffset {
    Login(0),
    Share(1),
    Message(2),
    Like(3),
    GameRequest(4),
    AppGroupCreate(5),
    AppGroupJoin(6),
    AppInvite(7),
    ;
     */

    private ImageView    mLogoImgView;
    private LoginButton  mFacebookBtnView;
    private SignInButton mGoogleBtnView;
    private TextView     mSkipTxtView;
    private TextView     mStatusTxtView;

    private String       mIdToken;
    private String       mFirstName;
    private String       mLastName;
    private String       mNickName;
    private String       mEmail;

    private CallbackManager                mFacebookCallbackManager;
    private GoogleApiClient                mGoogleApiClient;
    private FirebaseAuth                   mAuthentication;
    private FirebaseAuth.AuthStateListener mAuthenticationListener;
    private AccessTokenTracker             mAccessTokenTracker;
    private int                            mNumOfSignInMethodsTried = 0; // number of signed in methods that were we tried to sign in with

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        boolean isSignedIn = IsSignedInSharedPref();
        if (isSignedIn) { // if sign-in is registered in shared preferences, skip sign-in - start main activity
            Log.d(TAG, "user sign-in is not registered in shared preferences");
            StartMainActivity();
        }
        else {

            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~//
            //   configure Facebook and Google Sign-in  //
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~//
            FacebookSdk.sdkInitialize(getApplicationContext());
            AppEventsLogger.activateApp(getApplication()); //activate Facebook Analitics

            GoogleSignInOptions signInOpt = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();

            // Build a GoogleApiClient with access to GoogleSignIn.API and the options above.
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .enableAutoManage(this, this)
                    .addApi(Auth.GOOGLE_SIGN_IN_API, signInOpt)
                    .build();

            mAuthentication = FirebaseAuth.getInstance();

            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~//
            //   Button listeners and callback listeners  //
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~//

            // firebase authentication listener for google sign-in
            mAuthenticationListener = new FirebaseAuth.AuthStateListener() {
                @Override
                public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                    FirebaseUser user = firebaseAuth.getCurrentUser();

                    if (user != null) {
                        // User is signed in
                        Log.d(TAG, "signed in with google. user id: " + user.getUid() + ", starting main activity");

                        SetSignInInfo(user.getToken(false).toString(), "Google", true); // false = do not force refresh token
                        StartMainActivity();
                    } else {
                        // User is signed out
                        Log.d(TAG, "user is not signed in with Google, trying silent sign-in");

                        // try silent sign-in
                        OptionalPendingResult<GoogleSignInResult> pendingResult =
                                Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient);

                        // TODO: enable after finished debug
                        if (pendingResult.isDone()) {
                            // There's immediate result available and silent sign-in is successful
                            Log.d(TAG, "silent sign-in is successful. signed-in with Google. starting main activity");
                            if (pendingResult.get().isSuccess()) {
                                SetSignInInfo(pendingResult.get().getSignInAccount().getIdToken(), "Google", true);
                                StartMainActivity();
                            }
                        } else {
                            mNumOfSignInMethodsTried++;
                            if (mNumOfSignInMethodsTried == TOTAL_NUM_OF_SIGN_IN_METHODS) {
                                LoadSignInView();
                            }
                        }
                    }
                }
            };

            //facebook sign-in authentication
            CheckFacebookLoggedIn();
        }
    } // End of OnCreate()

    public void CheckFacebookLoggedIn()
    {
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        if( accessToken != null && accessToken.getToken() != null )
        {
            Log.d(TAG, "Facebook authentication: signed-in succeded. token: " + accessToken.getToken());
            SetSignInInfo(accessToken.getToken(),"Facebook",true);
            StartMainActivity();
        }
        else
        {
            Log.d(TAG, "Facebook authentication: signed-in failed. token is null");
            mNumOfSignInMethodsTried++;
            if (mNumOfSignInMethodsTried == TOTAL_NUM_OF_SIGN_IN_METHODS)
            {
                LoadSignInView();
            }
        }
    }

    // load sign-in view - enable to sign in.
    private void LoadSignInView()
    {
        setContentView(R.layout.activity_sign_in);

        mLogoImgView     = (ImageView)   findViewById(R.id.img_logo);
        mFacebookBtnView = (LoginButton) findViewById(R.id.button_facebook_login);
        mGoogleBtnView   = (SignInButton)findViewById(R.id.button_goolge);
        mSkipTxtView     = (TextView)    findViewById(R.id.button_skip);
        mStatusTxtView   = (TextView)    findViewById(R.id.status_text_view);
        mFacebookCallbackManager = CallbackManager.Factory.create();

        mFacebookBtnView.setReadPermissions(Arrays.asList(
                "public_profile", "email", "user_birthday", "user_friends"));

        RC_FB_SIGN_IN = mFacebookBtnView.getRequestCode();

        Log.e(TAG,"Animation start");

        ViewCompat.animate(mLogoImgView)
                .translationY(-100)
                .setStartDelay(STARTUP_DELAY)
                .setDuration(ANIM_LOGO_DURATION).setInterpolator(
                new DecelerateInterpolator(1.2f)).start();

        ViewCompat.animate(mGoogleBtnView)
                .translationY(30)
                .setStartDelay(STARTUP_ITEM_DELAY)
                .setDuration(ANIM_ITEM_DURATION).setInterpolator(
                new DecelerateInterpolator(1.2f)).start();

        ViewCompat.animate(mSkipTxtView)
                .translationY(30)
                .setStartDelay(STARTUP_ITEM_DELAY)
                .setDuration(ANIM_ITEM_DURATION).setInterpolator(
                new DecelerateInterpolator(1.2f)).start();

        Log.e(TAG,"Animation end");


        // Google sign-in botton listener - starts Google sign-in activity
        mGoogleBtnView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
                startActivityForResult(signInIntent, RC_GP_SIGN_IN);
            }
        });

        // Skip sign-in - starts the main activity and closes this (sign-in) activity
        mSkipTxtView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(getBaseContext(), MainActivity.class);
                startActivity(intent);
                finish();
            }
        });


        // Facebook sign-in callback - handles Facebook sign-in result and user's info
        mFacebookBtnView.registerCallback(mFacebookCallbackManager, new FacebookCallback<LoginResult>()
        {
            @Override
            public void onSuccess(LoginResult loginResult)
            {
                GraphRequest request = GraphRequest.newMeRequest(
                        loginResult.getAccessToken(),
                        new GraphRequest.GraphJSONObjectCallback()
                        {
                            @Override
                            public void onCompleted(JSONObject object, GraphResponse response)
                            {
                                Log.e(TAG, "Successfully signed in with facebook!" + response.toString());

                                try
                                {
                                    // Application code
                                    String email    = object.getString("email");
                                    String birthday = object.getString("birthday"); // 01/31/1980 format
                                }
                                catch(JSONException e)
                                {
                                    Log.e(TAG, response.toString());
                                    e.printStackTrace();
                                }
                            }
                        });
                Bundle parameters = new Bundle();
                parameters.putString("fields", "id,name,email,gender,birthday");
                request.setParameters(parameters);
                request.executeAsync();

                // start the main activity
                Log.e(TAG,"Signed in with facebook!");
                StartMainActivity();
//                Intent intent = new Intent(getBaseContext(), MainActivity.class);
//                startActivity(intent);
//                finish();
            }

            @Override
            public void onCancel()
            {
                Log.e(TAG,"Facebook sign-in canceled!");
                mStatusTxtView.setText("Facebook sign-in canceled! Please pick a sign-in method and try again or press skip button to continue");
            }

            @Override
            public void onError(FacebookException exception)
            {
                Log.e(TAG,"Error, facebook sign-in failed!");
                mStatusTxtView.setText("Error! Could not sign-in with Facebook! Please pick a sign-in method and try again or press skip button to continue");
            }
        });
    }

    @Override
    // onActivityResult is using requestCode to determine callback type, and calls a callbackManager accordingly
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ( requestCode == RC_FB_SIGN_IN )
        {
            // call facebook's callback manager
            mFacebookCallbackManager.onActivityResult(requestCode, resultCode, data);
        }
        else if ( requestCode == RC_GP_SIGN_IN )
        {
            // call Google's callback manager
            GoogleCallbackManager(requestCode, resultCode, data);
        }
    }

    @Override
    public void onConnectionFailed (ConnectionResult result)
    {
        // relevant only for Google sign-in
        mStatusTxtView.setText("Error, Could not connect to Google");
        Log.e("TAG","Google sign-in failed");
    }

    @Override
    public void onStart()
    {
        super.onStart();

        // when activity starts, init listeners
        mAuthentication.addAuthStateListener(mAuthenticationListener);
    }

    @Override
    public void onStop()
    {
        super.onStop();

        // when activity stops, remove listeners
        if (mAuthenticationListener != null) {
            mAuthentication.removeAuthStateListener(mAuthenticationListener);
        }

        if (mAccessTokenTracker != null) {
            mAccessTokenTracker.stopTracking();
        }
    }

    private void GoogleCallbackManager(int requestCode, int resultCode, Intent data)
    {
        GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
        if ( result.isSuccess())
        {
            Log.e("TAG", "Successfuly signed in with google");
            GoogleSignInAccount acct = result.getSignInAccount();
            firebaseAuthWithGoogle(acct);

            // Get account information
            mNickName  = acct.getDisplayName();
            mFirstName = acct.getGivenName();
            mLastName  = acct.getFamilyName();
            mEmail     = acct.getEmail();
            mIdToken   = acct.getIdToken();
        }
        else
        {
            mStatusTxtView.setText("Could not sign in with Google");
            Log.e("TAG", "Could not sign in with Google");
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct)
    {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuthentication.signInWithCredential(credential)
            .addOnCompleteListener(this, new OnCompleteListener<AuthResult>()
            {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task)
                {
                    if (task.isSuccessful())
                    {
                        Log.e(TAG,"Google credetials check: completed!");
                        StartMainActivity();
                    }
                    else
                    {
                        mStatusTxtView.setText("Could not sign-in with Google account :_(  Credentials do not match");
                        Log.e(TAG, "signInWithCredential failed!", task.getException());
                    }
                }
            });
    }

    private void StartMainActivity ()
    {
        Intent intent = new Intent(getBaseContext(), MainActivity.class);
        startActivity(intent);
        finish();
    }

    // Save/Clear sign-in info in shared preference
    private void SetSignInInfo(String idOrToken, String signInMethod, boolean isSignIn)
    {
        // Store user ID in shared preference for later usage
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()); // .getContext()
        SharedPreferences.Editor editor = sharedPrefs.edit();

        if(isSignIn)
        {
            editor.putString ("USER_TOKEN", idOrToken);
            editor.putString ("SIGN_IN_METHOD", signInMethod);
            editor.putBoolean("IS_SIGNED_IN", true);
        }
        else
        {
            editor.putString ("USER_TOKEN", null);
            editor.putString ("SIGN_IN_METHOD", null);
            editor.putBoolean("IS_SIGNED_IN", false);
        }

        editor.apply();
    }

    private boolean IsSignedInSharedPref()
    {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()); // .getContext()
        boolean isSignedIn = sharedPrefs.getBoolean("IS_SIGNED_IN", false);
        return isSignedIn;
    }
}
