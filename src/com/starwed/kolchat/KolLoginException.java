package com.starwed.kolchat;

public class KolLoginException extends Exception {
	
	//auto generate id
	private static final long serialVersionUID = 8271846850613861541L;

	//Define different error response kol gives us
	public static int UNKNOWN_PROBLEM = 0;

	public static int BAD_PASSWORD = 1;
	public static int SESSION_NOT_LOGGED_OUT = 2;
	public static int TOO_MANY_ATTEMPTS = 3;
	public static int INVALID_CHALLENGE = 4;
	public static int NIGHTLY_MAINTENANCE = 5;

	public int problemCode=0;
	public String  message;
	public String kolResponse="";
		public KolLoginException(String detailMessage, String htmlCode, int code)
		{
			super(detailMessage);
			problemCode= code;
			kolResponse=htmlCode;
			message = detailMessage;
		}
	

}
