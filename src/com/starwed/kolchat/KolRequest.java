package com.starwed.kolchat;

import java.net.URLEncoder;

import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

public class KolRequest {
	private KolSession session;
	
	private String path="";
	private String query = "";

	//TODO decide whether to store entire session, or just the needed bits like pwd and pid
	public KolRequest(KolSession s){
			session = s;
	}
	
	//sets the path for this particular query
	public void setPath(String p)
	{
		path=p;
	}
	
	//Adds a key value pair to the query string, properly encoding them
	public void addQuery(String key, String value)
	{
		//multiple queries need & between each one
		if(query.length()>0)
			query+="&";
		query += URLEncoder.encode(key) + "=" + URLEncoder.encode(value);	
	}
	
	
	public String doRequestWithPassword()
	{
		addQuery("pwd", session.pwd);
		addQuery("playerid", session.playerid);
		return( doRequest() );
		
	}
	//does the request and returns the response body
    public String doRequest()
    {
    	DefaultHttpClient tempClient = new DefaultHttpClient();
    	try{
    		
    		String request = "http://" + session.host + "/" + path;
    		if(query.length() > 0) {
    			request += "?" + query;
    		}
	    	//Set that phpsession cookie
	    	tempClient.setCookieStore(session.KolCookieStore);
			HttpGet httpget = new HttpGet(request); 
			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			String responseBody = tempClient.execute(httpget, responseHandler);
			return responseBody;
		}catch(Exception e){return e.toString();}	//TODO properly, should throw an exception
		finally
		{
			//Always shut down connection manager
			tempClient.getConnectionManager().shutdown();
		}
	
    }
    

    		
	
}
