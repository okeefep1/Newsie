package wit.edu.newsie;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.TextView;

public class TopicSelectionsActivity extends AppCompatActivity {

    private String topicTitle;
    private TextView tvTopicId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_topic_selections);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        Intent i = getIntent();
        i.getStringExtra("topic_id");
        topicTitle = i.getStringExtra("topic_title");
        getSupportActionBar().setTitle("Edit " + topicTitle + " Selections");

        SharedPreferences sp = getSharedPreferences(getResources().getText(R.string.app_name).toString(), Context.MODE_PRIVATE);
        String hasFb = sp.getString("has_facebook", null);
        String hasInsta = sp.getString("has_instagram", null);
        String hasTwitter = sp.getString("has_twitter", null);

        if (hasFb.equals("false")) {
            if (hasInsta.equals("false")) {
                if (hasTwitter.equals("false")) {
                } else {
                    getFragmentManager().beginTransaction().replace(R.id.selectionContainer, new FragmentTwitterSelections()).commit();
                }
            } else {
                getFragmentManager().beginTransaction().replace(R.id.selectionContainer, new FragmentInstagramSelections()).commit();
            }
        } else {
            getFragmentManager().beginTransaction().replace(R.id.selectionContainer, new FragmentFacebookSelections()).commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
