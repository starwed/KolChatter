package com.starwed.kolchat;


import java.util.Timer;
import java.util.TimerTask;

import com.starwed.kolchat.R;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Picture;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
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
	
	public KolSession kSession;
	
	public class KolData
	{
		public KolSession session;
		public String lastseen = "0";
		public boolean runchat = false;	
		public String chatLog = ""; 
		//data for cookie?
		// version:0, name: PHPSESSID, value:~~, domain:host, path: /, expiry: null
		public String persistText = "Initial";
	}
	public KolData kolData; 
		//UI objects
	private WebView appView;
	public Rect chatWindowRect = new Rect();
	public int scrollPosition;
	
	//private Button mainbutton;


	private EditText chatedit;
	private boolean chatDirty=false;
		

	
	
	private Handler handler = new Handler();
	private Timer timer = new Timer();
	private TextView output_textview;
	
	private ProgressDialog dialog;

	private static final int CHAT_DELAY = 8000;
	private static final int REQUESTCODE_LOGIN = 1;
	
	//This method is called before the activity is destroyed, so we save all the data needed for restoring later
	@Override
	protected void onSaveInstanceState (Bundle outState)
	{
		super.onSaveInstanceState (outState);
		appView.saveState(outState);
		outState.putString("lastseen", kolData.lastseen);
		outState.putString("chatLog", kolData.chatLog);
		outState.putParcelable("kolSession", kolData.session);
	}
	
	//This method is called as the app is restored, so we load all the data saved in onSaveInstanceState
	@Override
	protected void onRestoreInstanceState (Bundle savedInstanceState)
	{
		super.onRestoreInstanceState(savedInstanceState);
		kolData = new KolData();
		kolData.lastseen = savedInstanceState.getString("lastseen");
		kolData.chatLog = savedInstanceState.getString("chatLog");
		kolData.session = savedInstanceState.getParcelable("kolSession");
		try{
			appView.restoreState(savedInstanceState);
		}catch(Exception e){postText("error on restoreState: " + e.toString() ); }
		startChat();
		
	}
	
	//called after a subactivity has completed
	protected void onActivityResult(int requestCode, int resultCode, Intent data){
	    // See which child activity is calling us back.
	    switch (requestCode) {
	    	case KolApp.REQUESTCODE_LOGIN:
	            // This is the standard resultCode that is sent back if the
	            // activity crashed or didn't doesn't supply an explicit result.
	            if (resultCode == RESULT_CANCELED){
	                //myMessageboxFunction("Fight cancelled");
	            	postText("cancelled");
	            } 
	            else {
	            	String name = data.getStringExtra("name");
	            	String pwd = data.getStringExtra("password");
	            	//postText(name + " " + pwd);
	            	startSession(name, pwd);
	            	startChat();
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
        
        // Textview to just dump stuff to
        output_textview = (TextView) findViewById(R.id.output_text);
        
        
        chatedit = (EditText) findViewById(R.id.chatedit);

        //First button "main" temp to login travesty TODO remove! from both code and xml
        /*mainbutton = (Button) findViewById(R.id.mainbutton);
        mainbutton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
              
            			startSession("name", "pwd");
            }
        });*/
        
       
        
        chatedit = (EditText) findViewById(R.id.chatedit);
        chatedit.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                    (keyCode == KeyEvent.KEYCODE_ENTER)) {
                  // Perform action on key press
                	SubmitChat( chatedit.getText().toString() );
                	chatedit.setText("");
                	
                	return true;
                }
                return false;
            }
        });
        
        
        // Webview
        appView = (WebView) findViewById(R.id.appView);
        
        //TODO handle any interesting chat links user might interact with
        client = new WebViewClient() {  
        	@Override public boolean shouldOverrideUrlLoading(WebView view, String url) { 
        		return true;
        	}
        	
        	public void onPageFinished(WebView view, String url)  {  
        		 	//appView.pageDown(true);
        	}  
        };
        appView.setWebViewClient(client);
        
        // This is so it scrolls down to the bottom when chat is added
        appView.setPictureListener( new WebView.PictureListener() {
			public void onNewPicture(WebView view, Picture picture) {
				
				//only scroll if chat has been "dirtied"
				if(chatDirty)
				{
					try{
						//appView.requestRectangleOnScreen(chatWindowRect, true);
						
						//Jump to last position, then visibly scroll down to the bottom?  Hopefully this looks best.
						//TODO deal with case when user has scrolled up
						appView.scrollTo(chatWindowRect.left, chatWindowRect.top);
						appView.pageDown(true);
						postText(chatWindowRect.toShortString() + " | scrollX = " + scrollPosition + " | cH " + appView.getContentHeight() );
					}catch(Exception e){ postText(e.toString()); }
					//appView.pageDown(true);
					chatDirty=false;
				}
			}
		}
        		
        
        );
        WebSettings webSettings = appView.getSettings();
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
    
   public void startChat()
   {
	   //Only start the chat loop if runchat is currently false
	   if(kolData.runchat == false)
		   kolData.runchat = true;
	   else
		   return;
	   
	   //if there is stuff logged, show it, otherwise show a temp message.
	   if(kolData.chatLog.length() == 0)
	   {	  
		   // maybe return?  This should never happen if we've initialised
		   
	   }
	   else
		   loadHtml(kolData.chatLog);
	   // Initialise the first chat request to occur in 800ms.  
	   // After that, each successful chat request sets up another one to occur in KolApp.CHAT_DELAY ms.
	   try{
		   timer.schedule(new FetchChat(), 800);
       }catch(Exception e){
    	   postText("Problem starting chat timer" + e.toString() );
       }
	   
   }
    
    public void loadHtml(String source)
    {
    	//appView.loadData(source, "text/html", "utf-8");
    	
    	//Hopefully prevent that annoying data:url bug/problem.
    	appView.loadDataWithBaseURL(null, source,"text/html", "UTF-8", null);
    }
    
    public void postText(String text)
    {
    	output_textview.setText(text);
    }
    
    
    public void SubmitChat(String postedgraf)
    {
    	
    	//construct request
    	KolRequest kolreq = kolData.session.createKolRequest("submitnewchat.php");
    	kolreq.addQuery("graf",  postedgraf);
    	
    	//get the response and display it.  This blocks the UI thread... is that what we want?
    	String response = kolreq.doRequestWithPassword();
    	//postText(response);  //just for debugging purposes
    	ProcessChatResponse(response);

    }
    
    public void ProcessChatResponse(String response)
    {
    	kolData.chatLog = kolData.chatLog + response;
    	//postText(kolData.lastseen);
    	/*postText("X:" + appView.getScrollY() 
    			+ "; mH: " + appView.getMeasuredHeight()
    			+ "; vH: " + appView.getHeight()
    	);*/
    	
    	/*
    	 * Three cases:
    	 * 	index == 0 --> response ONLY contains the lastseen tick, so don't update
    	 *  index < 0  --> response DOES NOT contain lastseen, so contains something else important. Update!
    	 *  index >0   --> response is standard chat update with lastseen tagged on at the end, so Update!
    	 * Would be more elegant to simply remove lastseen block and then update if there was anything left.
    	 * We might need to have the logic anyway, to process dojax scripts that are sent to us.
    	 */
    	try{
        	/*postText("X:" + appView.getScrollX() 
        			+ "; mH: " + appView.getMeasuredHeight()
        			+ "; vH: " + appView.getHeight()
        	);*/
        		appView.getDrawingRect(chatWindowRect);
        		//postText(chatWindowRect.toShortString());
        	}catch(Exception e){ postText("rect error: " + e.toString()); }
    	if(response.indexOf("<!--lastseen") != 0 )
    	{
    		//only reload webview if something has changed
    		chatDirty = true;
    		scrollPosition = appView.getScrollX();
    		appView.getDrawingRect(chatWindowRect);
    		appView.loadData(kolData.chatLog, "text/html", "utf-8");
 	
    	}
    		

    	//postText( response.substring(0, 10));
    	
		
    }
 
    //This class is a runnable object that, when ran, makes an async request to update chat
    //  --The class seems to be necessary so that we can schedule a TimerTask object 
    //	-- I couldn't figure out how to do this directly with the async request
    public class FetchChat extends TimerTask{
    	private Runnable runnable = new Runnable(){
    		public void run(){
    			if(kolData.runchat==true)
    				new GetChatTask().execute("meh");
    		}
    	};
    	public void run(){
    		try{
    			handler.post(runnable);
    		}catch(Exception e){ };
    	}
    }
    
    //This performs the asyn request to get updated chat stuff, and then hands the response to ProcessChatResponse
    public class GetChatTask extends AsyncTask <Object, String, String> {
 
   
	
		protected String doInBackground(Object... args) {
			if(kolData.runchat==false)
				return "";
			if( kolData.session == null)
				return "";
			try{
				KolRequest req = kolData.session.createKolRequest("newchatmessages.php");
				req.addQuery("lasttime", kolData.lastseen);
				String responseBody = req.doRequest();
				return responseBody;
			}catch(Exception e){return e.toString();}
		}
		
		protected void onProgressUpdate(String... progress) {
	        // setProgressPercent(progress[0]);
	     }

		
		protected void onPostExecute(String result) {
            
			//I believe every valid response from a request for newchatmessages.php will contain lastseen info, 
			// so only process the response if that's the case
        	if(result.contains("lastseen"))
        	{
        			//Parse lastseen value from response.  Lastindex because it's appended to the end of the chat itself.
        			int index = result.lastIndexOf("lastseen");
        			kolData.lastseen = result.substring(index+9, index+19);
 
        			//Do whatever is necessary with this result.  Later, maybe move all parsing (including lastseen) into this function
        			ProcessChatResponse(result);
        	}
        	else if(kolData.session != null)	//If the session isn't active there's nothing interesting to say
        		postText("Error getting response from newchatmessages.php: " + result);
        	//Schedule next fetch of chat, regardless of whether result makes sense, but only if runchat flag is set
        	if(kolData.runchat==true)
        		timer.schedule(new FetchChat(), CHAT_DELAY);
        }
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
				ProcessChatResponse("<font color=green>Currently in channel: " + kolData.session.initialChatChannel + "</font><br/>");
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
    		kolData.runchat=false;
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