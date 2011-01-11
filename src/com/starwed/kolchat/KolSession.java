package com.starwed.kolchat;


import java.net.URLEncoder;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;

import android.os.Parcel;
import android.os.Parcelable;




public class KolSession implements Parcelable{
	public String host="";				//data[0]
	public String playerid ="";			//data[1]
	public String pwd ="";				//data[2]
	public CookieStore KolCookieStore;	//value is data[3]
	public String temphash;
	
	
	public String initialChatChannel = "";
	
	//Regular constructor logs on for us.  Reasoning here is a session is useless til we've logged on and gotten the proper data
	// Further, this process /should/ block UI.
	//TODO handle failure of logon or pwd fetching
	public KolSession(String username, String password) throws Exception
	{
		try{
			//start the session by logging on.  In the process, sets host and KolCookieStore
			String challenge = processLoginPage();

			//temphash = KolSession.digestPassword(password, challenge);
			 logOn(username, password, challenge);
			//Get pwd and playerid, and set them
			 fetchPwd();
		}catch(KolLoginException e){ throw(e); }
		catch(Exception e){ throw(e);}
	}
	
	// BEGIN parcel stuff
	public KolSession(Parcel in)
	{
		readFromParcel(in);
	}
	
	public int describeContents(){
         return 0;

	}
	
	public void readFromParcel(Parcel in)
	{
		String[] data = new String[4];
		in.readStringArray(data);
		this.host= data[0];
		this.playerid = data[1];
		this.pwd = data[2];
		String sessionid = data[3];
		this.setCookieStore(sessionid);
		
	}

	public void writeToParcel(Parcel out, int flags) {
		out.writeStringArray(new String[] { host, playerid, pwd, getSessionId() } );
	}
	
	
	public static final Parcelable.Creator<KolSession> CREATOR = new Parcelable.Creator<KolSession>() {
		public KolSession createFromParcel(Parcel in)
		{
			return new KolSession(in);

		}
		public KolSession[] newArray(int size) {
				return new KolSession[size];
		}
		
	};
	// END parcel stuff
	
	public KolRequest createKolRequest()
	{
		return new KolRequest(this);
	}
	
	public KolRequest createKolRequest(String path)
	{
		KolRequest req = new KolRequest(this);
		req.setPath(path);
		return(req);
	}

	// version:0, name: PHPSESSID, value:~~, domain:host, path: /, expiry: null
	public String getSessionId()
	{
		List<Cookie>  cookies = KolCookieStore.getCookies();
		//KolCookieStore.get
		if (cookies.isEmpty()) {
            //System.out.println("None");
        } else {
            for (int i = 0; i < cookies.size(); i++) {
                String cookieName = cookies.get(i).getName();
                if(cookieName.contains("PHPSESSID"))
                {
                	return cookies.get(i).getValue() ;
                }
            }
        }
		return "No matching cookie";
	}
	
	// version:0, name: PHPSESSID, value:~~, domain:host, path: /, expiry: null

	//only call when host has been set
	public void setCookieStore(String sessionid)
	{
		BasicClientCookie sessionCookie = new BasicClientCookie("PHPSESSID", sessionid);
		sessionCookie.setDomain(host);
		//are all these necessary?
		sessionCookie.setVersion(0);
		KolCookieStore = new BasicCookieStore();
		KolCookieStore.addCookie(sessionCookie);
	}
	
	public String processLoginPage() throws Exception
	{
		DefaultHttpClient httpclient = new DefaultHttpClient();
		host = "http://www.kingdomofloathing.com";
		HttpContext localContext = new BasicHttpContext();
		ResponseHandler<String> responseHandler = new BasicResponseHandler();
		HttpGet httpget = new HttpGet(host);
		String responseBody="";
		 try{
			 HttpResponse httpResponse =  httpclient.execute(httpget, localContext);
			 //HttpResponse response = httpclient.execute(httpget, localContext);
			 HttpHost httpHost = (HttpHost) localContext.getAttribute( ExecutionContext.HTTP_TARGET_HOST );
			 host =  httpHost.getHostName();
			 responseBody = responseHandler.handleResponse(httpResponse);
		 }catch(Exception e){ throw(new Exception("Error in login page processing", e));  }
		
		 
		 
		 String challenge="";
		//Check the response for any of the several possible ways Kol tells us it can't log us in
    	if(responseBody.indexOf("The system is currently down for nightly maintenance") >= 0)
    	{
    		throw new KolLoginException("System down for maintenance", responseBody, KolLoginException.NIGHTLY_MAINTENANCE);
    	}

		 
		 try{int challengeIndex=responseBody.indexOf("challenge value=") + 17;
		 	//Sample: ce2e902150c24ac094bd7623de5fdacc
		 	challenge = responseBody.substring(challengeIndex, challengeIndex+32);
		 }catch(Exception e){ throw(new Exception("Error in getting challenge key", e));  }
		 return challenge;
		
	}
    
    private void logOn(String username, String password, String challenge) throws Exception
    {
    	//MessageDigest digester = MessageDigest.getInstance( "MD5" );
    	DefaultHttpClient httpclient = new DefaultHttpClient();
    	
   	
    	username= URLEncoder.encode(username);
    	password= URLEncoder.encode(password);
    	
    	
    	//This is the logic for insecure logins
    	/*String request = "http://" +  host + "/login.php?loginname=" 
    						+ username + "&password=" + password + "&secure=0&loggingin=Yup";*/
    	
    	
    	String secureRequest="";
    	try{
    		//Construct the secure request.  Note that password is supposed to be blank, because the whole point is not to transmit it plaintext.
    		//It's the hash made with password+challenge that identifies us.  (response)
	    	secureRequest = "http://" +  host + "/login.php?loginname=" + username
			 + "&password=" + "" 		
			 + "&secure=1" + "&loggingin=Yup" 
			 + "&challenge=" + challenge 
			 + "&response=" + KolCrypto.digestPassword(password, challenge);
    	}
    	catch(Exception e){ throw new Exception("Problem constructing secure request", e); }
    
    
        String responseBody="";
    	try{
    		HttpGet httpget = new HttpGet(secureRequest); 
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
        	responseBody = httpclient.execute(httpget, responseHandler);
    		
    		
    	}catch(Exception e){ throw(new Exception("Problem with getting response: " + e.toString(), e ));  }
       
        
        	
    	//String responseBody = httpclient.execute(httpget, responseHandler);
    	//Test whether it has redirected us to login page again\
    	// using the string "If you've forgotten your password" on the grounds that it's most unique/relevant
    	//Check the response for any of the several possible ways Kol tells us it can't log us in
    	if(responseBody.indexOf("The system is currently down for nightly maintenance") >= 0)
    	{
    		//This actually shouldn't be thrown unless the system goes down between getting the challenge key and the user submitting.
       		throw new KolLoginException("System down for maintenance", responseBody, KolLoginException.NIGHTLY_MAINTENANCE);
    	}

    	
    	if(responseBody.indexOf("Bad password")>=0)
    	{
    		throw new KolLoginException("Bad password or username.", responseBody, KolLoginException.BAD_PASSWORD);
    	}
    	if(responseBody.indexOf("Invalid challenge.")>=0)
    	{
    		throw new KolLoginException("Invalid challenge.  Request was: " + secureRequest , responseBody, KolLoginException.INVALID_CHALLENGE);
    	}
    	if( responseBody.indexOf("Whoops -- it looks like you had a recent session open that didn't get logged out of properly.") >=0)
    	{
    		throw new KolLoginException("Not properly logged out.", responseBody, KolLoginException.SESSION_NOT_LOGGED_OUT);
    	}
    	if( responseBody.indexOf("Too many login attempts in too short a span of time.") >=0)
    	{
    		throw new KolLoginException("Too many login attempts.", responseBody, KolLoginException.TOO_MANY_ATTEMPTS);
    	}
    	
    	//Finally check to see if we loaded the main game frame, if not then presume login was not successful.
    	if(responseBody.indexOf("mainset")== -1)
    	{
    		throw new KolLoginException("Unknown login problem.", responseBody, KolLoginException.UNKNOWN_PROBLEM);
    	}
    	
    	//If login was ok, we need to get the cookie store that holds the PHPSESSIONID that tracks our login session
    	KolCookieStore = ((AbstractHttpClient) httpclient).getCookieStore();
    }
    
    

    
    private void fetchPwd() throws Exception
    {
        // Perform a second request to grab some user information
        //Need playerid and pwd hash at a minimum
    	// Also get initial chat channel
    	KolRequest req = createKolRequest("lchat.php");
 	   	String lChatSource = "";
 	   		   	
 	   	
        try{  
 		  lChatSource =  req.doRequest();
 	      int index1 = lChatSource.indexOf("playerid=");
 	      int index2 = lChatSource.indexOf("&pwd=");
 	      int index3 = lChatSource.indexOf("&graf=");
 	      if(index1 <0 || index2<0 || index3<0)
 	      {
 	    	  throw new Exception( "Badindex in: " + lChatSource);
 	      }
 		  playerid = lChatSource.substring(index1+9, index2);
 	      pwd = lChatSource.substring(index2+5, index3);
 	      //Log("PID: " + playerid + "   pwd:" + pwd);
        }catch(Exception e){ throw(new Exception("Error in fetching password", e)); };
        
        //Get initial chat channel, matching against a phrase that looks like
        // "Currently in channel: newbie"
        Pattern p = Pattern.compile("Currently in channel: (\\w+)");
        
        Matcher m = p.matcher(lChatSource);
        if( m.find() )
        {
        	initialChatChannel = m.group(1);
        }
        else
        	initialChatChannel = "unknown";

        
        
        
    	
    }

    public void logOut()
    {
    	KolRequest req = createKolRequest("logout.php");
    	req.doRequest();
    	
    }
}
