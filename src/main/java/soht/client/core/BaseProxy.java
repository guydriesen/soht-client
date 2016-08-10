/******************************************************************************
 * $Source: /cvsroot/telnetoverhttp/clients/java/src/java/soht/client/java/core/BaseProxy.java,v $
 * $Revision: 1.1 $
 * $Author: edaugherty $
 * $Date: 2003/11/26 16:35:55 $
 ******************************************************************************
 * Copyright (c) 2003, Eric Daugherty (http://www.ericdaugherty.com)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Eric Daugherty nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 * *****************************************************************************
 * For current versions and more information, please visit:
 * http://www.ericdaugherty.com/dev/soht
 *
 * or contact the author at:
 * soht@ericdaugherty.com
 *****************************************************************************/

package soht.client.core;

import java.net.HttpURLConnection;
import java.net.Socket;

import soht.client.configuration.ConfigurationManager;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;

/**
 * Common base class for Proxy Readers and Writers.
 *
 * @author Eric Daugherty
 */
public class BaseProxy extends Thread {

    //***************************************************************
    // Variables
    //***************************************************************

    /** The general configuration information */
    protected ConfigurationManager configurationManager;

    /** The unique ID of this proxy connection */
    protected long connectionId;

    /** The socket connection to the local client */
    protected Socket socket;

    //***************************************************************
    // Constructor
    //***************************************************************

    public BaseProxy( String name, ConfigurationManager configurationManager, long connectionId, Socket socket ) {

        super(name);

        this.configurationManager = configurationManager;
        this.connectionId = connectionId;
        this.socket = socket;
    }

    //***************************************************************
    // Helper Methods
    //***************************************************************

    /**
     * Tells the server that the client closed the connection.
     */
    protected void closeServer() {

        try {
            HttpURLConnection urlConnection = configurationManager.getURLConnection();

            //Write parameters.
            BufferedWriter out = new BufferedWriter( new OutputStreamWriter( urlConnection.getOutputStream() ) );
            out.write("action=close");
            out.write("&");
            out.write("id=" + connectionId);
            out.flush();
            out.close();

            urlConnection.connect();
            urlConnection.getInputStream();
        }
        catch( Exception e ) {
            e.printStackTrace();
        }
    }

    /**
     * Converts the raw bytes into an encoded String.  See the
     * <a href='http://www.ericdaugherty.com/dev/soht/protocol.html'>
     * Protocol definition</a> for more details.
     *
     * @param data the raw bytes to convert.
     * @param length the number of bytes in the array to convert.
     * @return an encoded String
     */
    protected String encode( byte[] data, int length )
    {
        StringBuffer encodedData = new StringBuffer();

        for( int index = 0; index < length; index++ )
        {
            if( needsEncoding( data[index] ) )
            {
                encodedData.append( encode( data[index] ) );
            }
            else
            {
                encodedData.append( (char) data[index] );
            }
        }

        return encodedData.toString();
    }

    private boolean needsEncoding( byte data )
    {
        boolean result = true;
        if( data >= 33 && data <= 126 )
        {
            result = (data == '%' || data == '?' || data == '@' ||
                data == '&' || data == '=' || data == '+' || data == ':' || data == '#' );
        }

        return result;
    }

    /**
     * Convert a byte to a hex value. (%xx).
     * @param data
     * @return
     */
    private String encode( byte data )
    {
        StringBuffer result = new StringBuffer( 3 );
        result.append( '#' );

        // Convert it to an unsigned number between 0-255.
        int intData = data;
        intData = intData & 255;

        String hexString = Integer.toHexString( intData );
        if( hexString.length() == 1 )
        {
            hexString = "0" + hexString;
        }
        result.append( hexString );

        return result.toString();
    }
}
