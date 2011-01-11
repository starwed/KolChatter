package com.starwed.kolchat;

import com.starwed.kolchat.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

	public class Login extends Activity {
		private Button login_button;
		private EditText nameEdit;
		private EditText pwdEdit;
		
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
	            	Intent loginInfo = new Intent();
	            	loginInfo.putExtra("name", nameEdit.getText().toString() );
	            	loginInfo.putExtra("password", pwdEdit.getText().toString());
	            	setResult(RESULT_OK, loginInfo);
	            	finish();
	            }
	        });

        
        
	}
}
