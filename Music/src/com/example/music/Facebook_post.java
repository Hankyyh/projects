package com.example.music;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.facebook.FacebookException;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.model.GraphUser;
import com.facebook.widget.WebDialog;
import com.facebook.widget.WebDialog.OnCompleteListener;

public class Facebook_post extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_facebook_post);	
		
		Session.openActiveSession(this, true, new Session.StatusCallback() {

		      // callback when session changes state
		      @Override
		      public void call(Session session, SessionState state, Exception exception) {
		        if (session.isOpened()) {

		          // make request to the /me API
		          Request.executeMeRequestAsync(session, new Request.GraphUserCallback() {

		            // callback after Graph API response with user object
		           
					@Override
					public void onCompleted(GraphUser user, Response response) {
						// TODO Auto-generated method stub
						if (user != null) {
//			                TextView welcome = (TextView) findViewById(R.id.welcom);
//			                welcome.setText("Hello " + "!");
							Intent intent = getIntent();
							String type = intent.getStringExtra("com.example.music.TITLE_TYPE");
							Bundle posts = new Bundle();
							
							JSONObject properti = new JSONObject();
							JSONObject properties = new JSONObject();
							if(type.equals("artists"))
							{
								String cover = intent.getStringExtra("com.example.music.COVER");
								String details = intent.getStringExtra("com.example.music.DETAIL");
								String genre = intent.getStringExtra("com.example.music.GENRE");
								String year = intent.getStringExtra("com.example.music.YEAR");
								String name = intent.getStringExtra("com.example.music.NAME");
								
											
								try {
									properti.put("text","here");
									properti.put("href", details+"");
									properties.put("Look at details ", properti);
								} catch (JSONException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								posts.putString("link", details);
								posts.putString("picture", cover);
								posts.putString("name",name);
								posts.putString("caption", "I like "+name+" who is active since "+ year);
								posts.putString("description","Genre of Music is: "+genre);
								posts.putString("properties",properties.toString());
								
								
							}
							else if(type.equals("albums"))
							{
								String cover = intent.getStringExtra("com.example.music.COVER_ALBUM");
								String detail = intent.getStringExtra("com.example.music.DETAIL_ALBUM");
								String genre = intent.getStringExtra("com.example.music.GENRE_ALBUM");
								String year = intent.getStringExtra("com.example.music.YEAR_ALBUM");
								String artist = intent.getStringExtra("com.example.music.ARTIST_ALBUM");
								String title = intent.getStringExtra("com.example.music.TITLE_ALBUM");
								
								
								try {
									properti.put("text","here");
									properti.put("href", detail+"");
									properties.put("Look at details ", properti);
								} catch (JSONException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								
								posts.putString("link", detail);
								posts.putString("picture", cover);
								posts.putString("name",title);
								posts.putString("caption","I like "+title+" released in "+ year);
								posts.putString("description", "Artist: "+artist+" Genre: "+genre);
								posts.putString("properties",properties.toString());
								
							}
							else if(type.equals("songs"))
							{
								String composer = intent.getStringExtra("com.example.music.COMPOSER_SONG");
								String detail = intent.getStringExtra("com.example.music.DETAIL_SONG");
								String performer = intent.getStringExtra("com.example.music.PERFORMER_SONG");
								String title = intent.getStringExtra("com.example.music.TITLE_SONG");
								
								try {
									properti.put("text","here");
									properti.put("href", detail+"");
									properties.put("Look at details ", properti);
								} catch (JSONException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								
								posts.putString("link", detail);
								posts.putString("name",title);
								posts.putString("caption", "I like "+title+" composed by "+ composer);
								posts.putString("description", "Performer: "+performer);
								posts.putString("properties",properties.toString());
								
							}
							WebDialog feedDialog = (
									new WebDialog.FeedDialogBuilder(Facebook_post.this, Session.getActiveSession(),posts)).setOnCompleteListener(new OnCompleteListener(){
//										public void OnComplete(Bundle values,FacebookException error){}

										@Override
										public void onComplete(Bundle values,
												FacebookException error) {
											// TODO Auto-generated method stub

											final String postId = values.getString("post_id");
											if(postId != null){
												Toast.makeText(Facebook_post.this, "Post has been published", Toast.LENGTH_SHORT).show();
											}
											else
											{
												Toast.makeText(Facebook_post.this, "Post not published", Toast.LENGTH_SHORT).show();
												finish();
											}
										
											
										}
									})
									.build();
									feedDialog.show();
							
							
			              }
						
					}
		          });
		        }
		      }
		    });
		
		
		  }
	@Override
	  public void onActivityResult(int requestCode, int resultCode, Intent data) {
	      super.onActivityResult(requestCode, resultCode, data);
	      Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);}
	

}
