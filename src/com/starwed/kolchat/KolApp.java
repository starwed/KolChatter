package com.starwed.kolchat;




import com.starwed.kolchat.R;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Picture;
import android.graphics.Rect;
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
import android.widget.Toast;


// TODO list 
// - investigate chat window not updating bug


// ? Optionally allow info to be stored?  Not really sure how hard that is yet.

// DO This later, not necessary right now.
// - UI for general consumption
//		- Create menu -- kind of done, but need logic to enable/disable certain buttons

//	- Use /c command to get chat location, rather than current method

// ? Switching channels -- This can be done from chat directly, so not super high priority
// ? Better error handling -- Need to have concrete flow for what should happen when errors are encountered.
// ? Fix loading html data thingy  -- maybe fixed, but still saw a sim. error once



public class KolApp extends Activity {
	
	private WebViewClient client;
	private boolean isLoggedIn=false;
	public class KolData {
		public KolSession session;
		public boolean runchat = false;	
	}
	public KolData kolData; 
	public KolChat chat;
	
	//UI objects
	private WebView chatView;
	public Rect chatWindowRect = new Rect();
	public int scrollPosition;
	private boolean chatDirty=false;

	private TextView output_textview;

	private EditText chatedit;
	private static final int REQUESTCODE_LOGIN = 1;
	
	//This method is called before the activity is destroyed, so we save all the data needed for restoring later
	@Override
	protected void onSaveInstanceState (Bundle outState)
	{
		super.onSaveInstanceState (outState);
		chatView.saveState(outState);
		try{
			outState.putString("lastseen", chat.lastseen);
			outState.putString("chatLog", chat.chatLog);
		}catch(Exception e){};
		outState.putBoolean("isLoggedIn", isLoggedIn);
		outState.putParcelable("kolSession", kolData.session);
	}
	
	//This method is called as the app is restored, so we load all the data saved in onSaveInstanceState
	@Override
	protected void onRestoreInstanceState (Bundle savedInstanceState)
	{
		super.onRestoreInstanceState(savedInstanceState);
		chatView.restoreState(savedInstanceState);

		kolData = new KolData();
		kolData.session = savedInstanceState.getParcelable("kolSession");

		if(kolData.session != null){
			chat = new KolChat(kolData.session, this);
			try{
				chat.lastseen = savedInstanceState.getString("lastseen");
				chat.chatLog = savedInstanceState.getString("chatLog");
			}catch(Exception e){};
			chat.startChat();			
		}
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
	            	try{
	            	kolData.session = data.getParcelableExtra("session");
	            	if(kolData.session == null)
	            	{
	            		postText("session is null");
	            		return;
	            	}
	            	try{chat = new KolChat(kolData.session, KolApp.this);
	            	
	            	}catch(Exception e){postText("error creating chat: " + e.toString());}
	            	chat.chatLog = "<font color=green>Currently in channel: " + kolData.session.initialChatChannel + "</font><br/>";
					chat.startChat();
					isLoggedIn=true;
	            	}catch(Exception e){postText(e.toString());}
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
                	try{chat.submitChat( chatedit.getText().toString() ); }
                	catch(Exception e){postText(e.toString());}
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
        			// This triggers before the actual rendering, so scrolling isn't useful
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
						//Jump to last position, then visibly scroll down to the bottom?  Hopefully this looks best.
						//TODO deal with case when user has scrolled up
						chatView.scrollTo(chatWindowRect.left, chatWindowRect.top);
						chatView.pageDown(true);
						//postText(chatWindowRect.toShortString() + " | scrollX = " + scrollPosition + " | cH " + chatView.getContentHeight() );
					}catch(Exception e){ postText(e.toString()); }
					chatDirty=false;
				}
			}
		} );
        WebSettings webSettings = chatView.getSettings();
        webSettings.setSavePassword(false);
        webSettings.setSaveFormData(false);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setSupportZoom(true);

        kolData = new KolData();

        // retrieve isLoggedIn state
        if(savedInstanceState !=null)
        	isLoggedIn = savedInstanceState.getBoolean("isLoggedIn", false);
        else
        	isLoggedIn = false;
       
        if(isLoggedIn == false) try{
 		   
 		   Intent loginInfo = getIntent();
 		   kolData.session = loginInfo.getParcelableExtra("session");
	       	if(kolData.session == null)
	       	{
	       		postText("session is null");
	       		return;
	       	}
	       	try{
	       		chat = new KolChat(kolData.session, KolApp.this);
       	
	       	}catch(Exception e){postText("error creating chat: " + e.toString());}
	       	chat.chatLog = "<font color=green>Currently in channel: " + kolData.session.initialChatChannel + "</font><br/>";
			chat.startChat();
			isLoggedIn=true;
			postText(loginInfo.getStringExtra("message"));
       	}catch(Exception e){postText("Some error getting intent/posting to chat: " + e.toString());}
       
    }
    
   
    public void loadHtml(String source){
       	//Hopefully using the base url will prevent that annoying data:url bug/problem.
    	chatView.loadDataWithBaseURL(null, source,"text/html", "UTF-8", null);
    }
    
    public void postText(String text){
    	output_textview.setText(text);
    }
    
    //pops up a notification that quickly goes away
	public void postToast(String text){
		Toast toast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT);
		toast.show();
	}
    
    public void updateChatView(String chatlog) {
    	//note: if the chatlog completely changes (as opposed to being updated) the scrolling will get messed up. 
    	
      	try{

        		chatView.getDrawingRect(chatWindowRect);
       	}catch(Exception e){ postText("rect error: " + e.toString()); }

		//set flag so that webview knows to scroll (TODO is this even necessary now?)
		chatDirty = true;
		scrollPosition = chatView.getScrollX();
		chatView.getDrawingRect(chatWindowRect);
		try{
			loadHtml(chatlog); 	
 		}catch(Exception e){ postText("Problem in updateChatView " + e.toString()); };

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
        case R.id.menu_info:
        	requestInfo();
            return true;
        case R.id.menu_quit:
        	quit();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    public void requestInfo(){
    	String msg = "Logged in as " + kolData.session.charName +" (#" + kolData.session.playerid + ") to " + kolData.session.host;
    	postToast(msg);
    }
    
    public void requestLogin() {
    	try{
    		Intent myIntent = new Intent( this , Login.class);
    		
    		//gives the intent to start, and the request code so we know when it's called back
    		startActivityForResult(myIntent, KolApp.REQUESTCODE_LOGIN);
    		
    	}catch(Exception e){ postText("Can't start activity: " + e.toString()); }
    	
    }
    
    public void quit(){
    	//todo -- need better idea of what to do here -- it should block but also allow the user to exit if they want
    	try{
    		chat.runchat=false;
    		if(kolData.session != null)
    			kolData.session.logOut();
    		kolData.session = null;
       		finish();
       	}
    	catch(Exception e){
    		postText("Error during logout: " + e.toString());
    	}
    }
    
    
    
}