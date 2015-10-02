package wit.edu.newsie;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.RequestPasswordResetCallback;
import com.parse.SignUpCallback;


public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPass, etConfirmPass;
    private Button btnLogin, btnRegister, btnForgotPass;
    private ProgressBar progressBar;
    private View loginForm;
    private String resetEmail = "";
    private View dialogView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = (EditText) findViewById(R.id.etEmail);
        etPass = (EditText) findViewById(R.id.etPass);
        etConfirmPass = (EditText) findViewById(R.id.etConfirmPass);
        btnLogin = (Button) findViewById(R.id.btnLogin);
        btnRegister = (Button) findViewById(R.id.btnRegister);
        btnForgotPass = (Button) findViewById(R.id.btnForgotPass);
        progressBar = (ProgressBar) findViewById(R.id.loginProgressBar);
        loginForm = findViewById(R.id.loginForm);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkViews(true)) {
                    showProgress(true);
                    attemptLogin();
                }
            }
        });

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (etConfirmPass.getVisibility() == View.VISIBLE) {
                    if (checkViews(false)) {
                        attemptRegister();
                    }
                }
                else {
                    etConfirmPass.setVisibility(View.VISIBLE);
                    Toast toast = Toast.makeText(getApplicationContext(), "Confirm Password!", Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });

        btnForgotPass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogView = v;
                // get prompts.xml view
                LayoutInflater layoutInflater = LayoutInflater.from(v.getContext());
                View promptView = layoutInflater.inflate(R.layout.reset_password_form, null);
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(v.getContext());

                // set prompts.xml to be the layout file of the alertdialog builder
                alertDialogBuilder.setView(promptView);
                final EditText input = (EditText) promptView.findViewById(R.id.etResetEmail);

                // setup a dialog window
                alertDialogBuilder
                        .setCancelable(false)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // get user input and set it to result
                                resetEmail = input.getText().toString();

                                ParseUser.requestPasswordResetInBackground(resetEmail, new RequestPasswordResetCallback() {
                                    public void done(ParseException e) {
                                        if (e == null) {
                                            Toast toast = Toast.makeText(dialogView.getContext(), "Check email to reset password.", Toast.LENGTH_SHORT);
                                            toast.show();
                                        } else {
                                            Toast toast = Toast.makeText(dialogView.getContext(), "Invalid email provided.", Toast.LENGTH_SHORT);
                                            toast.show();
                                        }
                                    }
                                });
                            }
                        })
                        .setNegativeButton("Cancel",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,	int id) {
                                        dialog.cancel();
                                    }
                                });

                // create an alert dialog
                AlertDialog alertD = alertDialogBuilder.create();
                alertD.show();
            }
        });
    }

    private void showProgress(final boolean show) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            loginForm.setVisibility(show ? View.GONE : View.VISIBLE);
            loginForm.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    loginForm.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            progressBar.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            loginForm.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private void attemptLogin() {
        String userEmail = etEmail.getText().toString().trim();
        String userPass = etPass.getText().toString();
        ParseUser.logInInBackground(userEmail, userPass, new LogInCallback() {
            public void done(ParseUser user, ParseException e) {
                if (user != null) {
                    showProgress(false);
                    Intent i = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(i);
                    finish();
                } else {
                    showProgress(false);
                    ParseUser.logOut();
                    if (e.getCode() == 101) { // Invalid credentials
                        Toast toast = Toast.makeText(getApplicationContext(), "Invalid email/password!", Toast.LENGTH_SHORT);
                        toast.show();
                    }
                }
            }
        });
    }

    private void attemptRegister() {
        ParseUser user = new ParseUser();
        user.setUsername(etEmail.getText().toString().trim());
        user.setPassword(etPass.getText().toString());
        user.setEmail(etEmail.getText().toString().trim());

        // other fields can be set just like with ParseObject
        // user.put("phone", "650-253-0000");

        user.signUpInBackground(new SignUpCallback() {
            public void done(ParseException e) {
                if (e == null) {
                    Intent i = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(i);
                    finish();
                } else {
                    if (e.getCode() == 202) { // email already taken
                        Toast toast = Toast.makeText(getApplicationContext(), etEmail.getText().toString().trim() + " is already taken!", Toast.LENGTH_SHORT);
                        toast.show();
                    }
                }
            }
        });
    }

    private boolean checkViews(boolean isLogin) {
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;
        CharSequence text = "";
        int err = 0;
        if (etEmail.getText().toString().trim().isEmpty()) {
            text =  text + "Email required!\n";
            //etEmail.setBackgroundResource(R.drawable.rect_text_error);
            err = 1;
        }
        else{
            //etEmail.setBackgroundResource(R.drawable.rect_text_edit);
        }
        if (etPass.getText().toString().trim().isEmpty()) {
            text =  text + "Password required!\n";
            //etPass.setBackgroundResource(R.drawable.rect_text_error);
            err = 1;
        }
        else{
            //etPass.setBackgroundResource(R.drawable.rect_text_edit);
        }
        if (!etEmail.getText().toString().trim().matches("[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+[.][a-zA-Z0-9-.]+")) {
            text = text + "Invalid Email: " + etEmail.getText().toString().trim() + "\n";
            //etEmail.setBackgroundResource(R.drawable.rect_text_error);
            err = 1;
        }
        else{
            //etEmail.setBackgroundResource(R.drawable.rect_text_edit);
        }
        if (!etPass.getText().toString().trim().matches("^.{6,}$")) {
            text =  text + "Password must be at least 6 chars long!\n";
            //etPass.setBackgroundResource(R.drawable.rect_text_error);
            err = 1;
        }else{
            //etPass.setBackgroundResource(R.drawable.rect_text_edit);
        }
        if (!isLogin) {
            if (!etConfirmPass.getText().toString().equals(etPass.getText().toString())) {
                text =  text + "Passwords must match to register!";
                //etPass.setBackgroundResource(R.drawable.rect_text_error);
                err = 1;
            }
        }
        if(err == 1){
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
            return false;
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }
}
