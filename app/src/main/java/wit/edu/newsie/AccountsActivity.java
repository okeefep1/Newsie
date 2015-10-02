package wit.edu.newsie;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterLoginButton;

import br.com.dina.oauth.instagram.InstagramApp;


public class AccountsActivity extends ActionBarActivity {

    private AccessToken accessToken;
    private LoginButton fbLogin;
    private Button btnInstaConnect, btnAccountsDone;
    private TwitterLoginButton btnTwitterLogin;
    private InstagramApp mApp;
    private CallbackManager callbackManager;
    private CheckBox cbFb, cbInsta, cbTwitter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accounts);
        fbLogin = (LoginButton) findViewById(R.id.fb_login_button);
        btnInstaConnect = (Button) findViewById(R.id.btnInstagram);
        btnAccountsDone = (Button) findViewById(R.id.btnAccountsDone);
        cbFb = (CheckBox) findViewById(R.id.cbFb);
        cbInsta = (CheckBox) findViewById(R.id.cbInstagram);
        cbTwitter = (CheckBox) findViewById(R.id.cbTwitter);

        setOnCheckListeners();
        onClickListeners();
        checkFb();
        checkInstagram();
        checkTwitter();
        checkCheckboxes();
    }

    private void checkCheckboxes() {
        SharedPreferences sp = getSharedPreferences(getResources().getText(R.string.app_name).toString(), Context.MODE_PRIVATE);
        String hasFb = sp.getString("has_facebook", null);
        String hasIntsa = sp.getString("has_instagram", null);
        String hasTwitter = sp.getString("has_twitter", null);
        if (hasFb == null) {

        } else if (hasFb.equals("false")) {
            cbFb.setChecked(true);
        }
        if (hasIntsa == null) {

        } else if (hasIntsa.equals("false")) {
            cbInsta.setChecked(true);
        }
        if (hasTwitter == null) {

        } else if (hasTwitter.equals("false")) {
            cbTwitter.setChecked(true);
        }
    }

    private void setOnCheckListeners() {
        cbFb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                SharedPreferences sp = getSharedPreferences(getResources().getText(R.string.app_name).toString(), Context.MODE_PRIVATE);
                SharedPreferences.Editor Ed = sp.edit();
                if (isChecked) {
                    Ed.putString("has_facebook", "false");
                    Ed.commit();
                } else {
                    Ed.putString("has_facebook", "true");
                    Ed.commit();
                }
            }
        });
        cbInsta.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                SharedPreferences sp = getSharedPreferences(getResources().getText(R.string.app_name).toString(), Context.MODE_PRIVATE);
                SharedPreferences.Editor Ed = sp.edit();
                if (isChecked) {
                    Ed.putString("has_instagram", "false");
                    Ed.commit();
                } else {
                    Ed.putString("has_instagram", "true");
                    Ed.commit();
                }
            }
        });
        cbTwitter.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                SharedPreferences sp = getSharedPreferences(getResources().getText(R.string.app_name).toString(), Context.MODE_PRIVATE);
                SharedPreferences.Editor Ed = sp.edit();
                if (isChecked) {
                    Ed.putString("has_twitter", "false");
                    Ed.commit();
                } else {
                    Ed.putString("has_twitter", "true");
                    Ed.commit();
                }
            }
        });
    }

    private void checkFb() {
        callbackManager = CallbackManager.Factory.create();
        fbLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            }
        });
        fbLogin.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Toast.makeText(getApplicationContext(), "Login Successful", Toast.LENGTH_SHORT).show();
                accessToken = loginResult.getAccessToken();
            }

            @Override
            public void onCancel() {
                Toast.makeText(getApplicationContext(), "Faceobook Login Canceled", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(FacebookException error) {
                Toast.makeText(getApplicationContext(), "Facebook Login Error", Toast.LENGTH_SHORT).show();
            }
        });
        accessToken = AccessToken.getCurrentAccessToken();
        if (accessToken != null) {
        } else {
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 140) {
            btnTwitterLogin.onActivityResult(requestCode, resultCode, data);
        } else {
            callbackManager.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void checkInstagram() {
        mApp = new InstagramApp(this, AppData.INSTAGRAM_CLIENT_ID, AppData.INSTAGRAM_CLIENT_SECRET, AppData.INSTAGRAM_CALLBACK_URL);
        mApp.setListener(listener);
        if (mApp.hasAccessToken()) {

        } else {

        }
    }

    private void checkTwitter() {
        btnTwitterLogin = (TwitterLoginButton) findViewById(R.id.btnTwitter);
        btnTwitterLogin.setCallback(new Callback<TwitterSession>() {
            @Override
            public void success(Result<TwitterSession> result) {
                Toast.makeText(AccountsActivity.this, "Twitter Login Successful.", Toast.LENGTH_LONG).show();
            }

            @Override
            public void failure(TwitterException exception) {
                Toast.makeText(AccountsActivity.this, "Twitter login failed, please try again", Toast.LENGTH_LONG).show();
                Log.e("TwitterLoginError", "Error: " + exception.getMessage());
            }
        });
    }

    InstagramApp.OAuthAuthenticationListener listener = new InstagramApp.OAuthAuthenticationListener() {
        @Override
        public void onSuccess() {
            Toast.makeText(AccountsActivity.this, "Instagram: Connected as " + mApp.getUserName(), Toast.LENGTH_LONG).show();
        }

        @Override
        public void onFail(String error) {
            Toast.makeText(AccountsActivity.this, "Instagram Error: " + error, Toast.LENGTH_LONG).show();
        }
    };

    public void onClickListeners() {
        btnInstaConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mApp.hasAccessToken()) {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(
                            AccountsActivity.this);
                    builder.setMessage("Disconnect from Instagram?")
                            .setCancelable(false)
                            .setPositiveButton("Yes",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(
                                                DialogInterface dialog, int id) {
                                            mApp.resetAccessToken();
                                            Toast.makeText(getApplicationContext(), "Not connected", Toast.LENGTH_LONG).show();
                                        }
                                    })
                            .setNegativeButton("No",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(
                                                DialogInterface dialog, int id) {
                                            dialog.cancel();
                                        }
                                    });
                    final AlertDialog alert = builder.create();
                    alert.show();
                } else {
                    mApp.authorize();
                }
            }
        });

        btnAccountsDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(AccountsActivity.this, MainActivity.class);
                startActivity(i);
                finish();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
