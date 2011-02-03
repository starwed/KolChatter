package com.starwed.kolchat;



//This will be a class that parses the chat responses
// For now it just checks the validity of the response and sets a bunch of flags.
// Almost definitely some of these flags need to be removed.
public class ParsedChatResponse {
	public String rawResponse;
	public boolean empty;
	public String lastseen;
	public boolean validChatUpdate;
	public boolean validChatResponse;
	public boolean loggedOutResponse = false;
	
	
	public ParsedChatResponse(String response) {
		rawResponse = response;
		
		//lastseen is a value embedded in the comment of responses to the cyclic chat update requests
		// * it is intended to be sent along with the next such request, to let the server know the last time we got a response
		// * In an empty request the lastseenIndex will be exactly 0
		int lastseenIndex = response.lastIndexOf("<!--lastseen");
		
		if(lastseenIndex==0)
    		empty=true;
		else
			empty = false;
		
		
		//I believe every valid response from a request for newchatmessages.php will contain lastseen info, 
		//	so set validChatUpdate to false if we can't find it

		if(lastseenIndex != -1) {
			//Parse lastseen value from response.  Lastindex was used because it's appended to the end of the chat itself.
			lastseen = response.substring(lastseenIndex+13, lastseenIndex+23);
			validChatUpdate = true;
			validChatResponse = true;
			return;
		} else
			validChatUpdate = false;
		
	
	    if(response.contains("<b>Invalid Session ID.</b>") ){
			validChatResponse = false;
			loggedOutResponse = true;
		} else if ( response.contains("<b>Not logged in.</b>")){
			validChatResponse = false;
			loggedOutResponse = true;
		} else {
			validChatResponse = false;
		}
	}
}

	    
		

			


		
