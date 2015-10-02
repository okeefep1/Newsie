package wit.edu.newsie;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.DeleteCallback;
import com.parse.ParseException;
import com.parse.ParseObject;

import java.util.List;

public class EditSelectionLvAdapter extends ArrayAdapter<String> {

    private final Activity context;
    private final List<String> names;
    private final List<String> objectIds;
    private final String source;

    public EditSelectionLvAdapter(Activity context, List<String> names, List<String> objectIds, String source) {
        super(context, R.layout.topic_item_layout, names);
        this.context = context;
        this.names = names;
        this.objectIds = objectIds;
        this.source = source;
    }

    @Override
    public int getCount() {
        return this.names.size();
    }

    @Override
    public View getView(final int position, View view, ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        View editItem = inflater.inflate(R.layout.edit_selection_item, null, true);
        TextView tvSelectedName = (TextView) editItem.findViewById(R.id.tvSelectionName);
        final Button remove = (Button) editItem.findViewById(R.id.btnRemove);
        remove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ParseObject topic = new ParseObject(source);
                topic.setObjectId(objectIds.get(position));
                topic.deleteInBackground(new DeleteCallback() {
                    @Override
                    public void done(ParseException e) {
                        if (e == null) {
                            Toast.makeText(context.getApplicationContext(), "Delete successful", Toast.LENGTH_LONG).show();
                            remove.setEnabled(false);
                            remove.setText("Removed");
                        } else {
                            Toast.makeText(context.getApplicationContext(), "Delete unsuccessful", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
        tvSelectedName.setText(names.get(position));

        return editItem;
    }
}
