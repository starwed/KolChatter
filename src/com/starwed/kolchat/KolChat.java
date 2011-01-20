package com.starwed.kolchat;

import java.util.Timer;
import java.util.TimerTask;

import android.os.AsyncTask;
import android.os.Handler;


//TODO!
/*
 * 
 * 
 * 
 */
public class KolChat {
		
	private KolSession session;
	private KolApp app;
	//things that should be interior to this class, and need to be removed from the KolApp class
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
 	   if(chatLog.length() == 0)
 	   {	  
 		   // maybe return?  This should never happen if we've initialised
 		   
 	   }
 	   else
 		   app.updateChatView(chatLog);
 	   // Initialise the first chat request to occur in 800ms.  
 	   // After that, each successful chat request sets up another one to occur in KolApp.CHAT_DELAY ms.
 	   try{
 		   timer.schedule(new FetchChat(), 800);
        }catch(Exception e){
     	   app.postText("Problem starting chat timer" + e.toString() );
        }
 	   
    }
    
  


	public void submitChat(String postedgraf)
    {
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
	    	ProcessChatResponse(response);
			}catch(Exception e){ app.postText("Error in submitChat:getting a response: " + e.toString()); };
		}catch(Exception e){ app.postText("Error in submitChat:creating query: " + e.toString()); };

    }
    
    public void ProcessChatResponse(String response)
    {
    	try{
    	chatLog = chatLog + response;
    	/*
    	 * Three cases:
    	 * 	index == 0 --> response ONLY contains the lastseen tick, so don't update
    	 *  index < 0  --> response DOES NOT contain lastseen, so contains something else important. Update!
    	 *  index >0   --> response is standard chat update with lastseen tagged on at the end, so Update!
    	 * Would be more elegant to simply remove lastseen block and then update if there was anything left.
    	 * We might need to have the logic anyway, to process dojax scripts that are sent to us.
    	 */
    	if(response.indexOf("<!--lastseen") != 0 )
    		app.updateChatView(chatLog);
		}catch(Exception e){ app.postText("Error in ProcessChatResponse " + e.toString()); };

		
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
            
			//I believe every valid response from a request for newchatmessages.php will contain lastseen info, 
			// so only process the response if that's the case
        	if(result.contains("lastseen"))
        	{
        			//Parse lastseen value from response.  Lastindex because it's appended to the end of the chat itself.
        			int index = result.lastIndexOf("lastseen");
        			lastseen = result.substring(index+9, index+19);
 
        			//Do whatever is necessary with this result.  Later, maybe move all parsing (including lastseen) into this function
        			ProcessChatResponse(result);
        	}
        	else if(session != null)	//If the session isn't active there's nothing interesting to say
        		app.postText("Error getting response from newchatmessages.php: " + result);
        	//Schedule next fetch of chat, regardless of whether result makes sense, but only if runchat flag is set
        	if(runchat==true)
        		timer.schedule(new FetchChat(), CHAT_DELAY);
        }
    }



}
