package wit.edu.newsie;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterAuthToken;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterLoginButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import br.com.dina.oauth.instagram.InstagramApp;
import twitter4j.User;
import wit.edu.newsie.data.FacebookQueries;
import wit.edu.newsie.data.InstagramQueries;
import wit.edu.newsie.data.TwitterQueries;
import wit.edu.newsie.objects.InstagramFollow;


public class NewTopicActivity extends AppCompatActivity {

    private Button btnSave, btnAddLike, btnNext, btnInstaConnect, btnAddInstFollow, btnAddTwitterFollow;
    private ParseUser currentUser;
    private EditText etTopicTitle;
    private LoginButton fbLogin;
    private CallbackManager callbackManager;
    private AccessToken accessToken;
    private AutoCompleteTextView actvLikes, actvInstFollows, actvTwitterFollows;
    private HashMap<String, String> likeMap;
    private List<ParseObject> selectedLikes, selectedFollows, selectedTwitFollows;
    private LinearLayout likeSearch;
    private String topicId, topicName;
    private String userId;
    private InstagramApp mApp;
    private HashMap<String, InstagramFollow> follows;
    private HashMap<String, User> twitterFriends;
    private TwitterLoginButton btnTwitterLogin;
    private LinearLayout fbSearchLayout, instSearchLayout, twitSearchLayout;
    private TwitterSession twitterSession;
    private CheckBox cbFb, cbInsta, cbTwitter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_topic);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        selectedLikes = new ArrayList<ParseObject>();
        selectedFollows = new ArrayList<ParseObject>();
        selectedTwitFollows = new ArrayList<ParseObject>();
        likeMap = new HashMap<String, String>();
        follows = new HashMap<String, InstagramFollow>();
        twitterFriends = new HashMap<String, User>();
        likeSearch = (LinearLayout) findViewById(R.id.likeSearch);
        etTopicTitle = (EditText) findViewById(R.id.etTopicTitle);
        btnSave = (Button) findViewById(R.id.btnSave);
        btnNext = (Button) findViewById(R.id.btnNext);
        btnInstaConnect = (Button) findViewById(R.id.btnInstaConnect);
        btnAddInstFollow = (Button) findViewById(R.id.btnAddInstFollow);
        btnAddTwitterFollow = (Button) findViewById(R.id.btnAddTwitterFollow);
        btnAddLike = (Button) findViewById(R.id.btnAddLike);
        fbLogin = (LoginButton) findViewById(R.id.login_button);
        actvLikes = (AutoCompleteTextView) findViewById(R.id.actvLikes);
        actvInstFollows = (AutoCompleteTextView) findViewById(R.id.actvInstFollows);
        actvTwitterFollows = (AutoCompleteTextView) findViewById(R.id.actvTwitterFollows);
        fbSearchLayout = (LinearLayout) findViewById(R.id.fbSearchLayout);
        instSearchLayout = (LinearLayout) findViewById(R.id.instSearchLayout);
        twitSearchLayout = (LinearLayout) findViewById(R.id.twitSearchLayout);
        cbFb = (CheckBox) findViewById(R.id.cbNoFb);
        cbInsta = (CheckBox) findViewById(R.id.cbNoInstagram);
        cbTwitter = (CheckBox) findViewById(R.id.cbNoTwitter);

        likeSearch.setVisibility(View.GONE);
        currentUser = ParseUser.getCurrentUser();
        checkFbLogin();
        facebookPrep();
        instagramPrep();
        twitterPrep();

        setOnClickListeners();
        setOnCheckListeners();
        checkCheckboxes();
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
                    fbSearchLayout.setVisibility(View.GONE);
                } else {
                    Ed.putString("has_facebook", "true");
                    Ed.commit();
                    fbSearchLayout.setVisibility(View.VISIBLE);
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
                    instSearchLayout.setVisibility(View.GONE);
                } else {
                    Ed.putString("has_instagram", "true");
                    Ed.commit();
                    instSearchLayout.setVisibility(View.VISIBLE);
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
                    twitSearchLayout.setVisibility(View.GONE);
                } else {
                    Ed.putString("has_twitter", "true");
                    Ed.commit();
                    twitSearchLayout.setVisibility(View.VISIBLE);
                }
            }
        });
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

    private void twitterPrep() {
        twitSearchLayout.setVisibility(View.GONE);
        btnTwitterLogin = (TwitterLoginButton) findViewById(R.id.btnTwitterLogin);
        btnTwitterLogin.setCallback(new Callback<TwitterSession>() {
            @Override
            public void success(Result<TwitterSession> result) {
                Toast.makeText(NewTopicActivity.this, "Twitter Login Successful.", Toast.LENGTH_LONG).show();
                btnTwitterLogin.setVisibility(View.GONE);
                twitterSession = result.data;
                twitSearchLayout.setVisibility(View.VISIBLE);
                getTwitterFollows();
            }

            @Override
            public void failure(TwitterException exception) {
                Toast.makeText(NewTopicActivity.this, "Twitter login failed, please try again", Toast.LENGTH_LONG).show();
                Log.e("TwitterLoginError", "Error: " + exception.getMessage());
                exception.printStackTrace();
            }
        });
        TwitterSession session = Twitter.getSessionManager().getActiveSession();
        if (session != null) {
            twitterSession = session;
            TwitterAuthToken authToken = session.getAuthToken();
            String token = authToken.token;
            String secret = authToken.secret;
            btnTwitterLogin.setVisibility(View.GONE);
            twitSearchLayout.setVisibility(View.VISIBLE);
            getTwitterFollows();
        } else {
            btnTwitterLogin.setVisibility(View.VISIBLE);
            twitSearchLayout.setVisibility(View.GONE);
        }
    }

    private void instagramPrep() {
        mApp = new InstagramApp(this, AppData.INSTAGRAM_CLIENT_ID, AppData.INSTAGRAM_CLIENT_SECRET, AppData.INSTAGRAM_CALLBACK_URL);
        mApp.setListener(listener);
        if (mApp.hasAccessToken()) {
            btnInstaConnect.setVisibility(View.GONE);
            instSearchLayout.setVisibility(View.VISIBLE);
            getInsagramFollowings();
        } else {
            instSearchLayout.setVisibility(View.GONE);
        }
    }

    InstagramApp.OAuthAuthenticationListener listener = new InstagramApp.OAuthAuthenticationListener() {
        @Override
        public void onSuccess() {
            Toast.makeText(NewTopicActivity.this, "Connected as " + mApp.getUserName(), Toast.LENGTH_SHORT).show();
            getInsagramFollowings();
            btnInstaConnect.setVisibility(View.GONE);
            instSearchLayout.setVisibility(View.VISIBLE);
        }

        @Override
        public void onFail(String error) {
            Toast.makeText(NewTopicActivity.this, error, Toast.LENGTH_SHORT).show();
        }
    };

    private void getTwitterFollows() {
        TwitterAuthToken tat = twitterSession.getAuthToken();
        TwitterQueries tq = new TwitterQueries();
        twitterFriends = tq.getFriendList("" + twitterSession.getUserId(), twitterSession.getUserName(), tat.token, tat.secret);
        if (twitterFriends != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_dropdown_item_1line, tq.getFriendNames());
            actvTwitterFollows.setAdapter(adapter);
        }
    }

    private void getInsagramFollowings() {
        InstagramQueries iq = new InstagramQueries();
        follows = iq.getUserFollows(mApp.getId(), mApp.getAccessToken());
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_dropdown_item_1line, iq.getFollowsNames());
        actvInstFollows.setAdapter(adapter);
    }

    private void checkFbLogin() {
        accessToken = AccessToken.getCurrentAccessToken();
        if (accessToken != null) {
            fbLogin.setVisibility(View.GONE);
            getFbLikes();
        } else {
            fbLogin.setVisibility(View.VISIBLE);
            fbSearchLayout.setVisibility(View.GONE);
        }
    }

    private void getFbLikes() {
        FacebookQueries fq = new FacebookQueries();
        likeMap = fq.getFacebookLikes(accessToken);
        List<String> likeNames = fq.getLikeNames();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_dropdown_item_1line, likeNames);
        actvLikes.setAdapter(adapter);
    }

    private void setOnClickListeners() {
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                topicName = etTopicTitle.getText().toString().trim();
                btnNext.setEnabled(false);
                if (topicName.isEmpty()) {
                    return;
                }
                final ParseObject newTopic = new ParseObject("Topic");
                topicName = etTopicTitle.getText().toString();
                newTopic.put("title", topicName);
                newTopic.put("userId", currentUser.getObjectId());
                newTopic.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        if (e == null) {
                            Toast t = Toast.makeText(getApplicationContext(), "Topic Saved", Toast.LENGTH_SHORT);
                            t.show();
                            topicId = newTopic.getObjectId();
                            likeSearch.setVisibility(View.VISIBLE);
                            etTopicTitle.setEnabled(false);
                            btnNext.setEnabled(false);
                        }
                        else {
                            Toast t = Toast.makeText(getApplicationContext(), "Error saving topic, try again.", Toast.LENGTH_SHORT);
                            t.show();
                            btnNext.setEnabled(true);
                        }
                    }
                });
            }
        });
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (selectedLikes.size() < 1 && selectedFollows.size() < 1 && selectedTwitFollows.size() < 1) {
                    Toast.makeText(getApplicationContext(), "Please select at least one like or follow.", Toast.LENGTH_SHORT).show();
                }
                if (selectedLikes.size() > 0) {
                    ParseObject.saveAllInBackground(selectedLikes, new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e == null) {
                                Toast.makeText(getApplicationContext(), "Likes saved to " + topicName, Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                Toast.makeText(getApplicationContext(), "Error saving Facebook likes to topic, please try again.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
                if (selectedFollows.size() > 0) {
                    ParseObject.saveAllInBackground(selectedFollows, new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e == null) {
                                Toast.makeText(getApplicationContext(), "Instagram follows saved to " + topicName, Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                Toast.makeText(getApplicationContext(), "Error saving Instagram follows to topic, please try again.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
                if (selectedTwitFollows.size() > 0) {
                    ParseObject.saveAllInBackground(selectedTwitFollows, new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e == null) {
                                Toast.makeText(getApplicationContext(), "Twitter follows saved to " + topicName, Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                Toast.makeText(getApplicationContext(), "Error saving Twitter follows to topic, please try again.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        });
        btnAddLike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String likeText = actvLikes.getText().toString();
                if (!likeText.isEmpty()) {
                    String likeId = likeMap.get(likeText);
                    if (!likeId.isEmpty()) {
                        ParseObject newLike = new ParseObject("FacebookLikes");
                        newLike.put("facebookId", likeId);
                        newLike.put("topicId", topicId);
                        selectedLikes.add(newLike);
                        Toast.makeText(getApplicationContext(), likeText + " selected", Toast.LENGTH_SHORT).show();
                        actvLikes.setText("");
                        return;
                    }
                }
                Toast.makeText(getApplicationContext(), "Invalid entry, check the name of selected like.", Toast.LENGTH_SHORT).show();
            }
        });
        btnAddInstFollow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String followText = actvInstFollows.getText().toString();
                if (!followText.isEmpty()) {
                    InstagramFollow follow = follows.get(followText);
                    if (follow != null) {
                        ParseObject newFollow = new ParseObject("InstagramFollow");
                        newFollow.put("instagramId", follow.getInstagramId());
                        newFollow.put("topicId", topicId);
                        selectedFollows.add(newFollow);
                        Toast.makeText(getApplicationContext(), followText + " selected", Toast.LENGTH_SHORT).show();
                        actvInstFollows.setText("");
                        return;
                    }
                }
                Toast.makeText(getApplicationContext(), "Invalid entry, check the name of selected like.", Toast.LENGTH_SHORT).show();
            }
        });
        btnAddTwitterFollow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String followText = actvTwitterFollows.getText().toString();
                if (!followText.isEmpty()) {
                    User user = twitterFriends.get(followText);
                    if (user != null) {
                        ParseObject newFollow = new ParseObject("TwitterFollow");
                        newFollow.put("twitterId", user.getId());
                        newFollow.put("topicId", topicId);
                        selectedTwitFollows.add(newFollow);
                        Toast.makeText(getApplicationContext(), followText + " selected", Toast.LENGTH_SHORT).show();
                        actvTwitterFollows.setText("");
                        return;
                    }
                }
                Toast.makeText(getApplicationContext(), "Invalid entry, check the name of selected like.", Toast.LENGTH_SHORT).show();
            }
        });
        actvLikes.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                //selectedLikes.add(likeMap.get(adapterView.getItemAtPosition(i).toString()));
            }
        });
        btnInstaConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mApp.hasAccessToken()) {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(
                            NewTopicActivity.this);
                    builder.setMessage("Disconnect from Instagram?")
                            .setCancelable(false)
                            .setPositiveButton("Yes",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(
                                                DialogInterface dialog, int id) {
                                            mApp.resetAccessToken();
                                            Toast.makeText(getApplicationContext(), "Not connected", Toast.LENGTH_SHORT).show();
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
    }

    private void facebookPrep() {
        callbackManager = CallbackManager.Factory.create();
        fbLogin.setReadPermissions("user_likes");
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
                fbSearchLayout.setVisibility(View.VISIBLE);
                fbLogin.setVisibility(View.GONE);
                getFbLikes();
            }

            @Override
            public void onCancel() {
                Toast.makeText(getApplicationContext(), "Login Canceled", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(FacebookException error) {
                Toast.makeText(getApplicationContext(), "Login Error", Toast.LENGTH_SHORT).show();
            }
        });
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
