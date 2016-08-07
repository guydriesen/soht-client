/******************************************************************************
 * $Source: /cvsroot/telnetoverhttp/clients/java/src/java/soht/client/java/core/ProxyReader.java,v $
 * $Revision: 1.4 $
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

package soht.client.java.core;

import java.net.HttpURLConnection;
import java.net.Socket;
import java.io.*;

import soht.client.java.configuration.ConfigurationManager;

/**
 * Handles the incoming data from the remote host.
 *
 * @author Eric Daugherty
 */
public class ProxyReader extends BaseProxy
{
    //***************************************************************
    // Variables
    //***************************************************************

    /** The output stream to write to the local client */
    private OutputStream out;

    //***************************************************************
    // Constructor
    //***************************************************************

    public ProxyReader( String name, ConfigurationManager configurationManager, long connectionId, Socket socket ) throws IOException {

        super( name, configurationManager, connectionId, socket );

        this.out = socket.getOutputStream();;
    }

    //***************************************************************
    // Thread Methods
    //***************************************************************

    /**
     * Reads from the remote server until the connection closes.
     */
    public void run() {

        try {
            HttpURLConnection urlConnection = configurationManager.getURLConnection();

            //Write parameters.
            BufferedWriter out = new BufferedWriter( new OutputStreamWriter( urlConnection.getOutputStream() ) );
            out.write("action=read");
            out.write("&");
            out.write("id=" + connectionId);
            out.flush();
            out.close();

            // Post the read request to the server.
            urlConnection.connect();

            InputStream in = null;

            // Make sure we can do cleanup even if there is an error...
            try
            {
                in = urlConnection.getInputStream();

                // Read data from the server and write it to our local client.
                byte[] bytes = new byte[1024];
                int count = 0;
                boolean isFirst = true;
                int startIndex = 1;
                while( true ) {

                    count = in.read( bytes );

                    // If the server disconnects, disconnect our client.
                    if( count == -1 || ( isFirst && count > 0 && bytes[0] == 0 ) ) {
                        out.close();
                        socket.close();
                        break;
                    }

                    // Pass the data to the client, minus the status byte.
                    startIndex = isFirst ? 1 : 0;
                    try {
                        this.out.write( bytes, startIndex, count - startIndex );
                    }
                    catch (IOException e) {
                        // The local connection is closed, so close the server.
                        closeServer();
                    }
                    isFirst = false;
                }
            }
            finally
            {
                if( in != null )
                {
                    in.close();
                }
                urlConnection.disconnect();
            }
        }
        catch( IOException ioe ) {
            if( out != null )
            {
                try
                {
                    out.close();
                    socket.close();
                }
                catch( IOException e )
                {
                    System.out.println( "Error closing output stream to client." );
                }
            }
            System.out.println( "IOException in ProxyReader." );
            ioe.printStackTrace();
        }
    }
}
