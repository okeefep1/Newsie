package wit.edu.newsie;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import java.io.InputStream;
import java.util.List;

import wit.edu.newsie.objects.FeedObject;


public class TopicFeedListAdapter extends ArrayAdapter<String> {
    private final Activity context;
    private final List<FeedObject> feedObjects;

    public TopicFeedListAdapter(Activity context, List<FeedObject> feedObjects, List<String> names) {
        super(context, R.layout.topic_item_layout, names);
        this.context = context;
        this.feedObjects = feedObjects;
        Log.v("AdapterObjectsSize", "" + feedObjects.size());
    }

    @Override
    public int getCount() {
        return this.feedObjects.size();
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        final int pos = position;
        LayoutInflater inflater = context.getLayoutInflater();
        View rowView = inflater.inflate(R.layout.topic_item_layout, null, true);
        TextView tvName = (TextView) rowView.findViewById(R.id.tvTopicName);
        TextView tvTime = (TextView) rowView.findViewById(R.id.tvTimeStamp);
        TextView tvContent = (TextView) rowView.findViewById(R.id.tvTopicContent);
        ImageView ivPostImage = (ImageView) rowView.findViewById(R.id.ivPostImage);
        ImageView ivSocialMediaIcon = (ImageView) rowView.findViewById(R.id.ivSocialMediaIcon);
        final VideoView vvPostVideo = (VideoView) rowView.findViewById(R.id.vvPostVideo);
        tvName.setText(feedObjects.get(position).getName());
        tvTime.setText(feedObjects.get(position).getTime());
        if (feedObjects.get(position).getContent() != null) {
            tvContent.setText(feedObjects.get(position).getContent());
        } else {
            tvContent.setVisibility(View.GONE);
        }
        if (feedObjects.get(position).getSource().equals("facebook")) {
            ivSocialMediaIcon.setImageResource(R.drawable.fb_icon);
        } else if (feedObjects.get(position).getSource().equals("instagram")) {
            ivSocialMediaIcon.setImageResource(R.drawable.instagram_icon_large);
        } else if (feedObjects.get(position).getSource().equals("twitter")) {
            ivSocialMediaIcon.setImageResource(R.drawable.twitter_icon);
        }

        if (feedObjects.get(position).getType().equals("text")) {
            ivPostImage.setVisibility(View.GONE);
        } else if (feedObjects.get(position).getType().equals("image") || feedObjects.get(position).getType().equals("photo")) {
            new DownloadImageTask(ivPostImage).execute(feedObjects.get(position).getPicUrl());
        } else if (feedObjects.get(position).getType().equals("twitter_video") || (feedObjects.get(position).getType().equals("video") && feedObjects.get(position).getSource().equals("twitter"))) {
            new DownloadImageTask(ivPostImage).execute(feedObjects.get(position).getPicUrl());
            tvContent.setText(tvContent.getText().toString() + " Click the image to view the video!");
            ivPostImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.addCategory(Intent.CATEGORY_BROWSABLE);
                    intent.setData(Uri.parse(feedObjects.get(pos).getVideoUrl()));
                    context.startActivity(intent);
                }
            });
        } else if (feedObjects.get(position).getType().equals("video")) {
            ivPostImage.setVisibility(View.GONE);
            vvPostVideo.setVisibility(View.VISIBLE);
            MediaController videoMediaController = new MediaController(context);
            vvPostVideo.setVideoPath(feedObjects.get(position).getVideoUrl());
            videoMediaController.setMediaPlayer(vvPostVideo);
            vvPostVideo.setMediaController(videoMediaController);
            vvPostVideo.requestFocus();
            vvPostVideo.start();
        }

        return rowView;
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", "" + e.getMessage());
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
    }
}
