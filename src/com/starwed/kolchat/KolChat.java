package com.starwed.kolchat;

import java.util.Timer;
import java.util.TimerTask;

import android.os.AsyncTask;
import android.os.Handler;



public class KolChat {
		
	private KolSession session;
	private KolApp app;

	public String lastseen;
	public boolean runchat ;	
	public String chatLog; 
	private Handler handler = new Handler();
	private Timer timer = new Timer();
	
	public KolChat(KolSession kolSession, KolApp a)
	{
		session = kolSession;
		app = a;
		lastseen = "0";
		runchat = false; //default chat to off
		chatLog= "";
		
	}
	
	private static final int CHAT_DELAY = 8000;

	
//Chat Stuff TODO make into object
    
    public void startChat()
    {
 	   //Only start the chat loop if runchat is currently false
 	   if(runchat == false)
 		   runchat = true;
 	   else
 		   return;
 	   
 	   //if there is stuff logged, show it, otherwise show a temp message.
 	   if(chatLog.length() == 0) {	  
 		   // maybe return?  This should never happen if we've initialised
  	   } else
 		   app.updateChatView(chatLog);
 	   // Initialise the first chat request to occur in 800ms.  
 	   // After that, each successful chat request sets up another one to occur in KolApp.CHAT_DELAY ms.
 	   try {
 		   timer.schedule(new FetchChat(), 800);
        } catch(Exception e) {
     	   app.postText("Problem starting chat timer" + e.toString() );
        }
 	   
    }
    
	public void submitChat(String postedgraf) {
		String response;
		KolRequest kolreq;
		try{
	    	//construct request
	    	kolreq = session.createKolRequest("submitnewchat.php");
	    	kolreq.addQuery("graf",  postedgraf);
			try{
		    	//get the response and display it.  This blocks the UI thread... is that what we want?
		    	response = kolreq.doRequestWithPassword();
		    	//postText(response);  //just for debugging purposes
		    	ParsedChatResponse pResponse = new ParsedChatResponse(response);
		    	ProcessChatResponse(pResponse);
			}catch(Exception e){ app.postText("Error in submitChat:getting a response: " + e.toString()); };
		}catch(Exception e){ app.postText("Error in submitChat:creating query: " + e.toString()); };
    }
    
	//TODO need better logic on when response is valid
	// - in particular, figure out if we got logged out.
	
	// Here's a string that occurs in the source of the login that I don't think can occur in chat:
	//			<b>Enter the KPaingdom:</b>
	 
    public void ProcessChatResponse(ParsedChatResponse response) {
    	if(response.loggedOutResponse == true) {
    		app.unexpectedLogout("oops");
    		return;
    	}
    	
    	try{
	    	chatLog = chatLog + response.rawResponse;
	    	if(response.empty == false)
	    		app.updateChatView(chatLog);
    	}catch(Exception e){ 
    		app.postText("Error in ProcessChatResponse " + e.toString()); 
    	}
    }
	

		
 
    //This class is a runnable object that, when ran, makes an async request to update chat
    //  --The class seems to be necessary so that we can schedule a TimerTask object 
    //	-- I couldn't figure out how to do this directly with the async request
    public class FetchChat extends TimerTask{
    	private Runnable runnable = new Runnable(){
    		public void run(){
    			if(runchat==true)
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
			if(runchat==false)
				return "";
			if( session == null)
				return "";
			try{
				KolRequest req = session.createKolRequest("newchatmessages.php");
				req.addQuery("lasttime", lastseen);
				String responseBody = req.doRequest();
				return responseBody;
			}catch(Exception e){return e.toString();}
		}
		
		protected void onProgressUpdate(String... progress) {
	        // setProgressPercent(progress[0]);
	    }

		protected void onPostExecute(String result) {
            
			
			ParsedChatResponse response = new ParsedChatResponse(result);

			//I believe every valid response from a request for newchatmessages.php will contain lastseen info, 
			// so only process the response if that's the case
			// if we get a logged out response, send on to ProcessChatResponse so everything is dealt with in a uniform way
        	if(response.validChatUpdate == true) {
        		lastseen = response.lastseen;
        	} else if(response.loggedOutResponse == false  && session != null)	//If the session isn't active there's nothing interesting to say
        		app.postText("Error getting response from newchatmessages.php: " + result);
      		ProcessChatResponse(response);

        	//Schedule next fetch of chat, regardless of whether result makes sense, but only if runchat flag is set
        	if(runchat==true)
        		timer.schedule(new FetchChat(), CHAT_DELAY);

        }
    }
}
