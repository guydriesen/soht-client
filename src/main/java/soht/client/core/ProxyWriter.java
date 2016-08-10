/******************************************************************************
 * $Source: /cvsroot/telnetoverhttp/clients/java/src/java/soht/client/java/core/ProxyWriter.java,v $
 * $Revision: 1.5 $
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
import java.net.SocketException;

import soht.client.configuration.ConfigurationManager;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.InputStream;

/**
 * Handles the ougoing data to the remote host.
 *
 * @author Eric Daugherty
 */
public class ProxyWriter extends BaseProxy {
    //***************************************************************
    // Variables
    //***************************************************************

    /** The input stream to read from the local client */
    private InputStream in;

    //***************************************************************
    // Constructor
    //***************************************************************

    public ProxyWriter( String name, ConfigurationManager configurationManager, long connectionId, Socket socket) throws IOException {

        super( name, configurationManager, connectionId, socket );

        this.in = socket.getInputStream();
    }

    //***************************************************************
    // Thread Methods
    //***************************************************************

    public void run() {

        try {

            HttpURLConnection urlConnection;
            BufferedWriter out;

            byte[] bytes = new byte[1024];
            int count = 0;
            while( true ) {

                try
                {
                    count = in.read( bytes );
                }
                catch( SocketException socketException )
                {
                    // This is normal.  Only print out an error if it was not a
                    // Socket Closed Exception.
                    if( !"Socket closed".equals( socketException.getMessage() ) )
                    {
                        System.out.println( "Error reading data from server: " + socketException );
                    }
                    break;
                }

                if( count == -1 ) {
                    closeServer();
                    break;
                }

                urlConnection = configurationManager.getURLConnection();

                //Write parameters.
                out = new BufferedWriter( new OutputStreamWriter( urlConnection.getOutputStream() ) );
                out.write("action=write" );
                out.write("&");
                out.write("id=" + connectionId);
                out.write("&");
                out.write("datalength=" + count );
                out.write("&");
                out.write("data=");
                out.write( encode( bytes, count ) );

                out.flush();
                out.close();

                urlConnection.connect();
                urlConnection.getInputStream();
            }
        }
        catch( IOException ioe ) {
            ioe.printStackTrace();
        }
    }

}
