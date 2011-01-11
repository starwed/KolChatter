/**
 * Copyright (c) 2005-2010, KoLmafia development team
 * http://kolmafia.sourceforge.net/
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  [1] Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *  [2] Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in
 *      the documentation and/or other materials provided with the
 *      distribution.
 *  [3] Neither the name "KoLmafia" nor the names of its contributors may
 *      be used to endorse or promote products derived from this software
 *      without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.starwed.kolchat;

import java.math.BigInteger;
import java.security.MessageDigest;

public class KolCrypto {
	
    public static final String digestPassword( final String password, final String challenge )
    throws Exception
    {
	    // KoL now makes use of a HMAC-MD5 in order to preprocess the
	    // password so that we aren't submitting plaintext passwords
	    // all the time. Here is the implementation. Note that the
	    // password is processed two times.
	
    	MessageDigest digester = MessageDigest.getInstance( "MD5" );
    	String hash1 = KolCrypto.getHexString( digester.digest( password.getBytes() ) );
    	digester.reset();
     	
    	String hash2 = KolCrypto.getHexString( digester.digest( ( hash1 + ":" + challenge ).getBytes() ) );
    	digester.reset();
    	
    	return hash2;
     }
    
    //from the KolMafia source TODO proper licensing etc.
    private static final String getHexString( final byte[] bytes )
	{
     	byte[] nonNegativeBytes = new byte[ bytes.length + 1 ];
     	System.arraycopy( bytes, 0, nonNegativeBytes, 1, bytes.length );
     	
     	StringBuffer hexString = new StringBuffer( 64 );
     	
     	hexString.append( "00000000000000000000000000000000" );
     	hexString.append( new BigInteger( nonNegativeBytes ).toString( 16 ) );
     	hexString.delete( 0, hexString.length() - 32 );
     	
     	return hexString.toString();
     }

}
