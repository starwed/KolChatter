package com.starwed.kolchat;


import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

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
		
		public void postToast(String msg){

			Toast toast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT);
			toast.show();
			
		}
		
	    public class StartSessionTask extends AsyncTask<String, String, String >{
	    	 
	    	
			protected String doInBackground(String... args) {
				//we need to reset session to null so if we're returning to this activity from a logged in state all assumptions work
	    		session = null; 
				try{
					session = new KolSession(args[0], args[1]);
				}catch(KolLoginException e){
					return("Problem creating session: " + e.message);
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
				if(session!=null){
					try{
			    		Intent kolLoggedIn = new Intent( Login.this , KolApp.class);
			    		kolLoggedIn.putExtra("session", session);
			    		kolLoggedIn.putExtra("message", result);
			    		
			    		
			    		//gives the intent to start, and the request code so we know when it's called back
			    		startActivity(kolLoggedIn);
			    		
			    	}catch(Exception e){ postToast("Can't start chat activity: " + e.toString()); }
					
					
					
				} else {
					postToast(result);
				}
            	
            	
	        }
	    }
}
