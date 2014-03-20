package com.example.music;



import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Spinner spin = (Spinner) findViewById(R.id.title_type);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.title_type, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spin.setAdapter(adapter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	public void sendMessage(View view)
	{
		EditText editText = (EditText) findViewById(R.id.title);
		String title = editText.getText().toString();
		Spinner spin = (Spinner) findViewById(R.id.title_type);
		String type = spin.getSelectedItem().toString();
		if(title.equals(""))
		{
			Toast error = Toast.makeText(getBaseContext(), "Please Enter something", Toast.LENGTH_LONG);
			error.show();
		}
		else
		{
			Intent intent = new Intent(this, Servlet_response.class);
			intent.putExtra("com.example.music.TITLE", title);
			intent.putExtra("com.example.music.TITLE_TYPE", type);
			startActivity(intent);
		}
	}

}
