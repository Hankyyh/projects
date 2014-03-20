package com.example.music;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.Display;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class Servlet_response extends Activity {
	JSONArray result = null;
	String type;
	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_servlet_response);
		Intent intent = getIntent();
		String title = intent.getStringExtra("com.example.music.TITLE");
		type = intent.getStringExtra("com.example.music.TITLE_TYPE");

		try {
			title = URLEncoder.encode(title, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    
	    String url = "http://cs-server.usc.edu:23825/examples/servlet/HelloWorldExample?input="+title+"&type="+type;
	    
	    HttpClient httpclient = new DefaultHttpClient();
	    HttpResponse response = null;
		try {
			response = httpclient.execute((HttpUriRequest) new HttpGet(url));
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//========================================================
		StatusLine statusLine = response.getStatusLine();
		if(statusLine.getStatusCode() == HttpStatus.SC_OK)
		{
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			try {
				response.getEntity().writeTo(out);
				out.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
		//=========================================================
		
		String servletResponse = out.toString();
//		TableLayout table = new TableLayout(this);
		TableLayout table;
	    JSONObject results = null;
	    JSONObject Obj = null;
	    
	    
	    try {
			Obj = new JSONObject(servletResponse);
			results = Obj.getJSONObject("results");

			result = results.getJSONArray("result");
			
			
			} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			}
		
	    	table = (TableLayout) findViewById(R.id.table);
	    	if(type.equals("artists"))
	    	{
	    		
	    		for(int i=1;i<result.length();i++)
	    		{
	    			Display display = getWindowManager().getDefaultDisplay();
	    			Point size = new Point();
					display.getSize(size);
					int width = size.x;
					
	    			TableRow row = new TableRow(this);
	    			row.setMinimumHeight(70);
	  
	    			TextView nameView = new TextView(this);
					TextView genreView = new TextView(this);
					TextView yearView = new TextView(this);
					nameView.setWidth(width/4);
					genreView.setWidth(width/4);
					yearView.setWidth(width/4);
					row.setDividerPadding(10);
					nameView.setPadding(5, 5, 5, 5);
					genreView.setPadding(5, 5, 5, 5);
					yearView.setPadding(5, 5, 5, 5);
					
					
					ImageView imgView = new ImageView(this);
					imgView.setScaleType(ImageView.ScaleType.FIT_CENTER);
					imgView.setPadding(5, 5, 5, 5);
					
					
					try {
						nameView.setText("Name:\n"+result.getJSONObject(i).getString("name").toString());
						genreView.setText("Genre:\n"+result.getJSONObject(i).getString("genre").toString());
						yearView.setText("Year:\n"+result.getJSONObject(i).getString("year").toString());
						String image_artist = result.getJSONObject(i).getString("cover").toString();
						if(image_artist.equals("NA"))
						{
							imgView.setImageResource(R.drawable.music);
						}
						else
						{
						imgView.setImageDrawable(grabImageFromUrl(image_artist));
						}
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					row.addView(imgView,50,50);
					row.addView(nameView);
					row.addView(genreView);
					row.addView(yearView);
					
					row.setClickable(true);
					row.setId(i);
					row.setFocusable(true);
//					PopupWindow popUp = new PopupWindow(this);
					
					row.setOnClickListener(new OnClickListener(){
						String details = null;
						String name= null;
						String genre = null;
						String year = null;
						String cover = null;

						@Override
						public void onClick(View v) {
							// TODO Auto-generated method stub
							Dialog dialog = new Dialog(Servlet_response.this);
							dialog.setContentView(R.layout.fb_dialog);
							dialog.setTitle("Post to Facebook");
							dialog.setCancelable(true);
							int id = v.getId();
							
							try {
								 details = result.getJSONObject(id).getString("details").toString();
								 name = result.getJSONObject(id).getString("name").toString();
								 genre = result.getJSONObject(id).getString("genre").toString();
								 year = result.getJSONObject(id).getString("year").toString();
								 cover = result.getJSONObject(id).getString("cover").toString();
							} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							
							Button button = (Button)dialog.findViewById(R.id.Button1);
							button.setOnClickListener(new OnClickListener(){

								@Override
								public void onClick(View v) {
									// TODO Auto-generated method stub
									Intent intent = new Intent(Servlet_response.this,Facebook_post.class);
									intent.putExtra("com.example.music.COVER", cover);
									intent.putExtra("com.example.music.DETAIL", details);
									intent.putExtra("com.example.music.GENRE", genre);	
									intent.putExtra("com.example.music.YEAR", year);
									intent.putExtra("com.example.music.NAME", name);
									intent.putExtra("com.example.music.TITLE_TYPE", type);
									startActivity(intent);
									
								}
								
							});
							dialog.show();
						}
						
					});
					
					table.addView(row,new TableLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
					
	    		}
	    		if(table.getChildCount() == 0)
	    		{
	    			TextView tv = (TextView)findViewById(R.id.view);
					tv.setText("No Result found");
	    		}
	    	}
	    	else if(type.equals("albums"))
			{
				for(int i=1;i<result.length();i++)
				{
					Display display = getWindowManager().getDefaultDisplay();
					Point size = new Point();
					display.getSize(size);
					int width = size.x;
					
					TableRow row = new TableRow(this);	//new row
					TextView titleView = new TextView(this);
					TextView nameView = new TextView(this);
					TextView genreView = new TextView(this);
					TextView yearView = new TextView(this);
					
					titleView.setWidth(width/5);
					titleView.setPadding(5, 5, 5, 5);
					nameView.setWidth(width/5);
					nameView.setPadding(5, 5, 5, 5);
					genreView.setWidth(width/5);
					genreView.setPadding(5, 5, 5, 5);
					yearView.setWidth(width/5);
					yearView.setPadding(5, 5, 5, 5);
					
					ImageView imgView = new ImageView(this);
					imgView.setScaleType(ImageView.ScaleType.FIT_CENTER);
					imgView.setPadding(5, 5, 5, 5);
					
					try {
						titleView.setText("Title:\n"+result.getJSONObject(i).getString("title").toString());
						nameView.setText("Artist:\n"+result.getJSONObject(i).getString("artist").toString());
						genreView.setText("Genre:\n"+result.getJSONObject(i).getString("genre").toString());
						yearView.setText("Year:\n"+result.getJSONObject(i).getString("year").toString());
						String image_artist = result.getJSONObject(i).getString("cover").toString();
						if(image_artist.equals("NA"))
						{
							imgView.setImageResource(R.drawable.music);
						}
						else
						{
						imgView.setImageDrawable(grabImageFromUrl(image_artist));
						}
//						imgView.setImageDrawable(grabImageFromUrl(result.getJSONObject(i).getString("cover").toString()));
					} catch (JSONException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					
					row.addView(imgView,50,50);
					row.addView(titleView);
					row.addView(nameView);
					row.addView(genreView);
					row.addView(yearView);
					row.setClickable(true);
					row.setId(i);
					row.setFocusable(true);
					
					row.setOnClickListener(new OnClickListener(){
						String detail = null;
						String artist= null;
						String genre = null;
						String year = null;
						String cover = null;
						String title = null;

						@Override
						public void onClick(View v) {
							// TODO Auto-generated method stub
							Dialog dialog = new Dialog(Servlet_response.this);
							dialog.setContentView(R.layout.fb_album);
							dialog.setTitle("Post to Facebook");
							dialog.setCancelable(true);
							int id = v.getId();
						
							
							try {
								 detail = result.getJSONObject(id).getString("detail").toString();
								 artist = result.getJSONObject(id).getString("artist").toString();
								 genre = result.getJSONObject(id).getString("genre").toString();
								 year = result.getJSONObject(id).getString("year").toString();
								 cover = result.getJSONObject(id).getString("cover").toString();
								 title = result.getJSONObject(id).getString("title").toString();
						
								 
							} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							
							Button button = (Button)dialog.findViewById(R.id.Button3);
							button.setOnClickListener(new OnClickListener(){

								@Override
								public void onClick(View v) {
									// TODO Auto-generated method stub
									Intent intent = new Intent(Servlet_response.this,Facebook_post.class);
									intent.putExtra("com.example.music.COVER_ALBUM", cover);
									intent.putExtra("com.example.music.DETAIL_ALBUM", detail);
									intent.putExtra("com.example.music.GENRE_ALBUM", genre);	
									intent.putExtra("com.example.music.YEAR_ALBUM", year);
									intent.putExtra("com.example.music.ARTIST_ALBUM", artist);
									intent.putExtra("com.example.music.TITLE_ALBUM", title);
									intent.putExtra("com.example.music.TITLE_TYPE", type);
//									Log.d("error:",cover+" "+detail+" "+title);
									startActivity(intent);
									
								}
								
							});
							dialog.show();
						}
						
					});
					
			
					
					table.addView(row,new TableLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

				}
				if(table.getChildCount() == 0)
	    		{
	    			TextView tv = (TextView)findViewById(R.id.view);
					tv.setText("No Result found");
	    		}
			}
	    	else
			{
				for(int i=1;i<result.length();i++)
				{
					android.view.Display display = getWindowManager().getDefaultDisplay();
					Point size = new Point();
					display.getSize(size);
					int width = size.x;
					
					TableRow row = new TableRow(this);	//new row
					
					TextView titleView = new TextView(this);
					TextView performer = new TextView(this);
					TextView composer = new TextView(this);
					titleView.setWidth(width/4);
					titleView.setPadding(5, 5, 5, 5);
					performer.setWidth(width/4);
					performer.setPadding(5, 5, 5, 5);
					composer.setWidth(width/4);
					composer.setPadding(5, 5, 5, 5);
					
					ImageView imgView = new ImageView(this);
					imgView.setScaleType(ImageView.ScaleType.FIT_CENTER);
					imgView.setPadding(5, 5, 5, 5);
					imgView.setImageResource(R.drawable.music);
					
					try {
						titleView.setText("Title:\n"+result.getJSONObject(i).getString("title").toString());
						performer.setText("Performer:\n"+result.getJSONObject(i).getString("performer").toString());
						composer.setText("Composer:\n"+result.getJSONObject(i).getString("composer").toString());
//						imgView.setImageDrawable(grabImageFromUrl(result.getJSONObject(i).getString("cover").toString()));
		
					} catch (JSONException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				
					
					row.addView(imgView,50,50);
					row.addView(titleView);
					row.addView(performer);
					row.addView(composer);
					row.setClickable(true);
					row.setId(i);
					row.setFocusable(true);
//					popUp = new PopupWindow(this);
					
					row.setOnClickListener(new OnClickListener(){
						String detail = null;
						String sample= null;
						String performer = null;
						String composer = null;
//						String cover = null;
						String title = null;

						@Override
						public void onClick(View v) {
							// TODO Auto-generated method stub
							Dialog dialog = new Dialog(Servlet_response.this);
							dialog.setContentView(R.layout.fb_song);
							dialog.setTitle("Post to Facebook");
							dialog.setCancelable(true);
							int id = v.getId();
							
							try {
								 detail = result.getJSONObject(id).getString("detail").toString();
								 sample = result.getJSONObject(id).getString("sample").toString();
								 performer = result.getJSONObject(id).getString("performer").toString();
								 composer = result.getJSONObject(id).getString("composer").toString();
								 title = result.getJSONObject(id).getString("title").toString();
							} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							
							Button button = (Button)dialog.findViewById(R.id.Button2);
							button.setOnClickListener(new OnClickListener(){

								@Override
								public void onClick(View v) {
									// TODO Auto-generated method stub
									Intent intent = new Intent(Servlet_response.this,Facebook_post.class);
									intent.putExtra("com.example.music.SAMPLE_SONG", sample);
									intent.putExtra("com.example.music.DETAIL_SONG", detail);
									intent.putExtra("com.example.music.PERFORMER_SONG", performer);	
									intent.putExtra("com.example.music.COMPOSER_SONG", composer);
									intent.putExtra("com.example.music.TITLE_SONG", title);
									intent.putExtra("com.example.music.TITLE_TYPE", type);
									startActivity(intent);
									
								}
								
							});
							final MediaPlayer mediaPlayer = new MediaPlayer();
							Button sample_button = (Button)dialog.findViewById(R.id.Button3);
							sample_button.setOnClickListener(new OnClickListener(){

								@Override
								public void onClick(View v) {
									// TODO Auto-generated method stub
									if(!mediaPlayer.isPlaying())
									{
										mediaPlayer.start();
									}
									String url = sample;
									
									mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
									try {
										mediaPlayer.setDataSource(url);
										mediaPlayer.prepare(); // might take long! (for buffering, etc)
									} catch (IllegalArgumentException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									} catch (SecurityException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									} catch (IllegalStateException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								
									mediaPlayer.start();
									
								}
								
							});
							
							Button sample_stop = (Button)dialog.findViewById(R.id.Button4);
							sample_stop.setOnClickListener(new OnClickListener(){

								@Override
								public void onClick(View arg0) {
									// TODO Auto-generated method stub
									if(mediaPlayer.isPlaying())
									{
										mediaPlayer.stop();
									}
								}
								
							});
							dialog.show();
						}
						
					});
					
					
					
					table.addView(row,new TableLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
					
				}
				if(table.getChildCount() == 0)
	    		{
	    			TextView tv = (TextView)findViewById(R.id.view);
					tv.setText("No Result found");
	    		}
			}

		
		}
	}

	

	@SuppressLint("NewApi")
	private Drawable grabImageFromUrl(String url) throws Exception{
		// TODO Auto-generated method stub
		Drawable dr = Drawable.createFromStream((InputStream)new URL(url).getContent(), "src");
    	Bitmap bitmap = ((BitmapDrawable) dr).getBitmap();
    	Drawable d = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(bitmap, 100, 100, true));
    	return d;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.servlet_response, menu);
		return true;
	}

}
