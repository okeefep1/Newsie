package wit.edu.newsie;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class CustomListViewAdapter extends ArrayAdapter<String> {

	private final Activity context;
	private final String[] text;
	private final Integer[] imageId;
	
	public CustomListViewAdapter(Activity context, String[] text, Integer[] imageId) {
		super(context, R.layout.nav_list_item, text);
		this.context = context;
		this.text = text;
		this.imageId = imageId;
	}
	
	@SuppressLint({ "ViewHolder", "InflateParams" })
	@Override
	public View getView(int position, View view, ViewGroup parent) {
		LayoutInflater inflater = context.getLayoutInflater();
		View rowView = inflater.inflate(R.layout.nav_list_item, null, true);
		TextView txtTitle = (TextView) rowView.findViewById(R.id.tvNavItem);
		ImageView imageView = (ImageView) rowView.findViewById(R.id.ivNavItem);
		txtTitle.setText(text[position]);
		imageView.setImageResource(imageId[position]);
		return rowView;
	}
}
