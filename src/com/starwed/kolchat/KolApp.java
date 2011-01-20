package com.starwed.kolchat;




import com.starwed.kolchat.R;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Picture;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnKeyListener;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.TextView;


// TODO list 
// #1 TODO complete refactoring of chat code into its own object
// - App needs to create chat object in the first place
// - Handle parceling of chat object, if necessary
// - 


// ? Optionally allow info to be stored?  Not really sure how hard that is yet.

// DO This later, not necessary right now.
// - UI for general consumption
//		- Create menu -- kind of done, but need logic to enable/disable certain buttons
//		- Bring up login screen on app start?  Not yet, I guess

//	- Use /c command to get chat location, rather than current method

// ? Switching channels -- This can be done from chat directly, so not super high priority
// ? Better error handling -- Need to have concrete flow for what should happen when errors are encountered.
// ? Fix loading html data thingy  -- maybe fixed, but still saw a sim. error once


// + Load intro chat screen, so we know what channel we're in
// + Allow user to input login/password!
// + Implement secure login -- Working!
// + Fix screen rotation issues -- Looks like this is done, but for resetting the display text that's for errors anyway
// + Char escape stuff for form submission -- Done!
// + Implement sending chats :) -- done!
// + Restart chat service on recreation, keep old log
// + Handle that scrolling issue more gracefully



public class KolApp extends Activity {
	
	private WebViewClient client;
	//private KolSession session;
	
	//public KolSession kSession;
	
	public class KolData
	{
		public KolSession session;
		public boolean runchat = false;	
		 		
	}
	public KolData kolData; 
	
	public KolChat chat;
		//UI objects
	private WebView chatView;
	public Rect chatWindowRect = new Rect();
	public int scrollPosition;
	
	//private Button mainbutton;


	private EditText chatedit;
	private boolean chatDirty=false;
			

	private TextView output_textview;
	
	private ProgressDialog dialog;

	private static final int REQUESTCODE_LOGIN = 1;
	
	//This method is called before the activity is destroyed, so we save all the data needed for restoring later
	@Override
	protected void onSaveInstanceState (Bundle outState)
	{
		super.onSaveInstanceState (outState);
		chatView.saveState(outState);
		outState.putString("lastseen", chat.lastseen);
		outState.putString("chatLog", chat.chatLog);
		outState.putParcelable("kolSession", kolData.session);
	}
	
	//This method is called as the app is restored, so we load all the data saved in onSaveInstanceState
	@Override
	protected void onRestoreInstanceState (Bundle savedInstanceState)
	{
		super.onRestoreInstanceState(savedInstanceState);
		kolData = new KolData();
		
		
		kolData.session = savedInstanceState.getParcelable("kolSession");
		chat = new KolChat(kolData.session);
		chat.lastseen = savedInstanceState.getString("lastseen");
		chat.chatLog = savedInstanceState.getString("chatLog");
		
		try{
			chatView.restoreState(savedInstanceState);
		}catch(Exception e){postText("error on restoreState: " + e.toString() ); }
		
		chat.startChat();
		
	}
	
	//called after a subactivity has completed
	protected void onActivityResult(int requestCode, int resultCode, Intent data){
	    // See which child activity is calling us back.
	    switch (requestCode) {
	    	case KolApp.REQUESTCODE_LOGIN:
	            // This is the standard resultCode that is sent back if the
	            // activity crashed or didn't doesn't supply an explicit result.
	            if (resultCode == RESULT_CANCELED){	                
	            	postText("cancelled");
	            } 
	            else {
	            	String name = data.getStringExtra("name");
	            	String pwd = data.getStringExtra("password");
	            	//postText(name + " " + pwd);
	            	startSession(name, pwd);
	            	chat = new KolChat(kolData.session);
	            	chat.startChat();
	            }
	        default:
	            break;
	    }
	}

	
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       
        setContentView(R.layout.main);
        
        // Textview to just dump messages to
        output_textview = (TextView) findViewById(R.id.output_text);
               
        chatedit = (EditText) findViewById(R.id.chatedit);
        chatedit.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                    (keyCode == KeyEvent.KEYCODE_ENTER)) {
                  // Perform action on key press
                	chat.submitChat( chatedit.getText().toString() );
                	chatedit.setText("");
                	
                	return true;
                }
                return false;
            }
        });
        
        
        // Webview
        chatView = (WebView) findViewById(R.id.appView);
        
        //TODO handle any interesting chat links user might interact with
        client = new WebViewClient() {  
        	@Override public boolean shouldOverrideUrlLoading(WebView view, String url) { 
        		return true;
        	}
        	
        	public void onPageFinished(WebView view, String url)  {  
        		 	//appView.pageDown(true);
        	}  
        };
        chatView.setWebViewClient(client);
        
        // This is so it scrolls down to the bottom when chat is added
        chatView.setPictureListener( new WebView.PictureListener() {
			public void onNewPicture(WebView view, Picture picture) {
				
				//only scroll if chat has been "dirtied"
				if(chatDirty==true)
				{
					try{
						//appView.requestRectangleOnScreen(chatWindowRect, true);
						
						//Jump to last position, then visibly scroll down to the bottom?  Hopefully this looks best.
						//TODO deal with case when user has scrolled up
						chatView.scrollTo(chatWindowRect.left, chatWindowRect.top);
						chatView.pageDown(true);
						postText(chatWindowRect.toShortString() + " | scrollX = " + scrollPosition + " | cH " + chatView.getContentHeight() );
					}catch(Exception e){ postText(e.toString()); }
					//appView.pageDown(true);
					chatDirty=false;
				}
			}
		}
        		
        
        );
        WebSettings webSettings = chatView.getSettings();
        webSettings.setSavePassword(false);
        webSettings.setSaveFormData(false);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setSupportZoom(true);

 

        kolData = new KolData();

    }
    
    public void startSession(String user, String password)
    {
    	
    		
   		//kolData.session = new KolSession(user, password);
    	try{
   			new StartSessionTask().execute(user, password);
    	}catch(Exception e)
    	{
    		postText("While creating sessionTask: " + e.toString());
    	}
    	
    	/*}catch(KolLoginException e) {
    		//Handle login specific errors
    		postText(e.toString());
    		loadHtml(e.kolResponse);
    	}catch(Exception e){
    		postText("Generic login exception: " + e.toString());

    	}*/
    }
    

    
    public void loadHtml(String source)
    {
    	//appView.loadData(source, "text/html", "utf-8");
    	
    	//Hopefully prevent that annoying data:url bug/problem.
    	chatView.loadDataWithBaseURL(null, source,"text/html", "UTF-8", null);
    }
    
    public void postText(String text)
    {
    	output_textview.setText(text);
    }
    
    
    
    public void updateChatView(String chatlog)
    {
    	//note: if the chatlog completely changes (as opposed to being updated) the scrolling will get messed up. 
    	
      	try{

        		chatView.getDrawingRect(chatWindowRect);
       	}catch(Exception e){ postText("rect error: " + e.toString()); }

		//set flag so that webview knows to scroll (TODO is this even necessary now?)
		chatDirty = true;
		scrollPosition = chatView.getScrollX();
		chatView.getDrawingRect(chatWindowRect);
		chatView.loadData(chatlog, "text/html", "utf-8");
 	
    	   	
		
    }
 
    
    
    public class StartSessionTask extends AsyncTask<String, String, String >{
    	 
    	
		protected String doInBackground(String... args) {
			
			try{
				kolData.session = new KolSession(args[0], args[1]);
			}catch(Exception e ){
				return( "Problem creating session: " + e.toString() );
			}
			return( "Session created for " + args[0] );	
		}
		
		protected void onProgressUpdate(String... progress) {
	        // setProgressPercent(progress[0]);
			
	     }
		
		protected void onPreExecute()
		{
			dialog = ProgressDialog.show(KolApp.this, "",  "Logging in to the Kingdom" , true);
		}
		
		protected void onPostExecute(String result) {
			dialog.cancel();
			if(kolData.session != null)
				updateChatView("<font color=green>Currently in channel: " + kolData.session.initialChatChannel + "</font><br/>");
			postText(result);
        }
    }
    
    //creates options menu from the xml resource
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }
    
    //implement menu item functionality.  (Not really sure what consumes the return type.)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.menu_login:
        	requestLogin();
            return true;
        case R.id.menu_quit:
        	quit();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    public void requestLogin()
    {
    	try{
    		Intent myIntent = new Intent( this , Login.class);
    		
    		//gives the intent to start, and the request code so we know when it's called back
    		startActivityForResult(myIntent, KolApp.REQUESTCODE_LOGIN);
    		
    	}catch(Exception e){ postText("Can't start activity: " + e.toString()); }
    	
    }
    
    public void quit(){
    	try{
    		chat.runchat=false;
    		if(kolData.session != null)
    			kolData.session.logOut();
    		finish();
    		//kolData.session=null;  //TODO figure out if this needs to happen after asyn logout request
    		
    	}
    	catch(Exception e){
    		postText("Error during logout: " + e.toString());
    	}
    }
    
}