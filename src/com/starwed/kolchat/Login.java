package com.starwed.kolchat;


import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class Login extends Activity {
		private Button login_button;
		private EditText nameEdit;
		private EditText pwdEdit;
		
		private KolSession session;
		
		private ProgressDialog dialog;

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setContentView(R.layout.login);
		       
			nameEdit = (EditText) findViewById(R.id.name_edit);
			pwdEdit = (EditText) findViewById(R.id.pwd_edit);
			
	        login_button = (Button) findViewById(R.id.login_button);
	        login_button.setOnClickListener(new View.OnClickListener() {
	            public void onClick(View v) {
	            	//Intent myIntent = new Intent(v.getContext(), KolApp.class);
	                //startActivityForResult(myIntent, 0);
	            	
	            	String user = nameEdit.getText().toString();
	            	String password = pwdEdit.getText().toString();
	            	new StartSessionTask().execute(user, password);
	            }
	        });

        
        
		}
	    public class StartSessionTask extends AsyncTask<String, String, String >{
	    	 
	    	
			protected String doInBackground(String... args) {
				
				try{
					session = new KolSession(args[0], args[1]);
				}catch(Exception e ){
					return( "Problem creating session: " + e.toString() );
				}
				return( "Session created for " + args[0] );	
			}
			
			protected void onProgressUpdate(String... progress) {
		        // setProgressPercent(progress[0]);
				
		     }
			
			protected void onPreExecute() {
				dialog = ProgressDialog.show(Login.this, "",  "Logging in to the Kingdom" , true);
			}
			
			protected void onPostExecute(String result) {
				dialog.cancel();
				//return to initial app
				Intent loginInfo = new Intent();
				loginInfo.putExtra("session", session);
				setResult(RESULT_OK, loginInfo);
            	finish();
            	
            	//TODO move thisinto KolApp after return
				/*if(kolData.session != null){
					updateChatView("<font color=green>Currently in channel: " + kolData.session.initialChatChannel + "</font><br/>");
					chat = new KolChat(kolData.session, KolApp.this);
	            	chat.startChat();
				}*/
				//postText(result);
	        }
	    }
}
