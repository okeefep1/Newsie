package wit.edu.newsie;

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import br.com.dina.oauth.instagram.InstagramApp;
import wit.edu.newsie.data.InstagramQueries;
import wit.edu.newsie.objects.InstagramFollow;


public class FragmentInstagramSelections extends Fragment {

    private ListView lvEditInstaSelections;
    private HashMap<String, InstagramFollow> followMap, followsIds;
    private List<InstagramFollow> topicInstaFollows;
    private List<String> followNames, objectIds;
    private AutoCompleteTextView actvAddEditFollow;
    private Button btnAddEditFollow, btnInstaDone;
    private String topicId;
    private InstagramApp mApp;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_instagram_selections, container, false);
        lvEditInstaSelections = (ListView) v.findViewById(R.id.lvEditInstaSelections);
        actvAddEditFollow = (AutoCompleteTextView) v.findViewById(R.id.actvAddEditFollow);
        btnAddEditFollow = (Button) v.findViewById(R.id.btnAddEditFollow);
        btnInstaDone = (Button) v.findViewById(R.id.btnInstaDone);
        followMap = new HashMap<String, InstagramFollow>();
        followsIds = new HashMap<String, InstagramFollow>();
        topicInstaFollows = new ArrayList<InstagramFollow>();
        followNames = new ArrayList<String>();
        objectIds = new ArrayList<String>();

        SharedPreferences sp = getActivity().getSharedPreferences(getResources().getText(R.string.app_name).toString(), Context.MODE_PRIVATE);
        topicId = sp.getString("current_topic_id", null);

        btnAddEditFollow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String followText = actvAddEditFollow.getText().toString();
                if (!followText.isEmpty()) {
                    String instagramId = followMap.get(followText).getInstagramId();
                    if (!instagramId.isEmpty()) {
                        ParseObject newLike = new ParseObject("InstagramFollow");
                        newLike.put("instagramId", instagramId);
                        newLike.put("topicId", topicId);
                        newLike.saveInBackground(new SaveCallback() {
                            @Override
                            public void done(ParseException e) {
                                if (e == null) {
                                    Toast.makeText(getActivity().getApplicationContext(), followText + " added to topic", Toast.LENGTH_SHORT).show();
                                    actvAddEditFollow.setText("");
                                }
                                else {
                                    Toast.makeText(getActivity().getApplicationContext(), "Invalid entry, check the name of selected follow.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });

                        return;
                    }
                }
            }
        });

        btnInstaDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences sp = getActivity().getSharedPreferences(getResources().getText(R.string.app_name).toString(), Context.MODE_PRIVATE);
                String hasTwitter = sp.getString("has_twitter", null);
                if (hasTwitter.equals("false")) {
                    getActivity().finish();
                } else {
                    getFragmentManager().beginTransaction().replace(R.id.selectionContainer, new FragmentTwitterSelections()).commit();
                }
            }
        });
        instagramPrep();
        getAllInstaFollows();
        return v;
    }

    private void instagramPrep() {
        mApp = new InstagramApp(getActivity().getApplicationContext(), AppData.INSTAGRAM_CLIENT_ID, AppData.INSTAGRAM_CLIENT_SECRET, AppData.INSTAGRAM_CALLBACK_URL);
        mApp.setListener(listener);
        if (!mApp.hasAccessToken()) {
            mApp.authorize();
        } else {
            /*Intent i = new Intent(getActivity(), AccountsActivity.class);
            startActivity(i);
            getActivity().finish();*/
        }
    }

    InstagramApp.OAuthAuthenticationListener listener = new InstagramApp.OAuthAuthenticationListener() {
        @Override
        public void onSuccess() {
            Toast.makeText(getActivity(), "Connected as " + mApp.getUserName(), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFail(String error) {
            Toast.makeText(getActivity(), error, Toast.LENGTH_SHORT).show();
        }
    };

    private void getAllInstaFollows() {
        InstagramQueries iq = new InstagramQueries();
        followMap = iq.getUserFollows(mApp.getId(), mApp.getAccessToken());
        followsIds = iq.getFollowsIds();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity().getApplicationContext(), android.R.layout.simple_dropdown_item_1line, iq.getFollowsNames());
        actvAddEditFollow.setAdapter(adapter);
        getSelectedInstagramFollows();
    }

    private void getSelectedInstagramFollows() {
        ParseQuery query = ParseQuery.getQuery("InstagramFollow");
        query.whereEqualTo("topicId", topicId);
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> follows, ParseException e) {
                if (e == null) {
                    InstagramFollow instaFollow;
                    for (ParseObject po : follows) {
                        instaFollow = new InstagramFollow();
                        instaFollow.setId(po.getObjectId());
                        instaFollow.setInstagramId(po.getString("instagramId"));
                        instaFollow.setTopicId(po.getString("topicId"));
                        instaFollow.setFullName(followsIds.get(instaFollow.getInstagramId()).getFullName());
                        followNames.add(instaFollow.getFullName());
                        objectIds.add(instaFollow.getId());
                        topicInstaFollows.add(instaFollow);
                    }
                    EditSelectionLvAdapter adapter = new EditSelectionLvAdapter(getActivity(), followNames, objectIds, "InstagramFollow");
                    lvEditInstaSelections.setAdapter(adapter);
                } else {
                    Toast.makeText(getActivity().getApplicationContext(), "Error getting follows", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
