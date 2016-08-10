/******************************************************************************
 * $Source: /cvsroot/telnetoverhttp/clients/java/src/java/soht/client/java/core/Proxy.java,v $
 * $Revision: 1.6 $
 * $Author: edaugherty $
 * $Date: 2004/06/29 21:08:52 $
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

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.HttpURLConnection;
import java.text.MessageFormat;

import org.apache.log4j.Logger;

import soht.client.configuration.ConfigurationManager;
import soht.client.configuration.Host;

/**
 * Implements a proxy connection for a single remote host:port.
 * <p>
 * Each remote host:port must have a unique local port that client applications
 * connect to.  This class implements a single connection, or port proxy.
 *
 * @author Eric Daugherty
 */
public class Proxy extends Thread
{
    //***************************************************************
    // Variables
    //***************************************************************

    /** The local -> remote mapping information */
    private Host host;

    /** The general configuration information */
    private ConfigurationManager configurationManager;

    /** Keeps track of whether the proxy is running or not. */
    private boolean running = false;

    private static Logger log = Logger.getLogger( Proxy.class );

    //***************************************************************
    // Constructor
    //***************************************************************

    /**
     * Creates a new Proxy instance.
     * <p>
     * After creating a instance, the proxy should be started using
     * the startProxy Method.
     *
     * @param configurationManager the general configuration information.
     * @param host the specific host mapping to implement.
     */
    public Proxy( ConfigurationManager configurationManager, Host host ) {

        super( "Proxy-" + host.getLocalPort() );

        this.configurationManager = configurationManager;
        this.host = host;

        log.debug( "Proxy Created." );
    }

    //***************************************************************
    // Control Methods
    //***************************************************************

    public void startProxy()
    {
        if( !running )
        {
            this.start();
        }
    }

    public void stopProxy()
    {
        running = false;
    }

    //***************************************************************
    // Public Methods
    //***************************************************************

    /**
     * Process incoming connections.
     */
    public void run()
    {
        log.debug( "Proxy starting on port: " + host.getLocalPort() );

        ServerSocket serverSocket = null;
        try
        {
            serverSocket = new ServerSocket( host.getLocalPort() );
            serverSocket.setSoTimeout( 1000 );
            running = true;
        }
        catch( IOException ioException )
        {
            log.fatal( "Error creating Server Socket", ioException );
            log.fatal( "Thread Ending" );
            return;
        }

        log.info( MessageFormat.format( "Proxy started to remote host: {0}:{1}, using SOHT Server at: {2}", new Object[] { host.getRemoteHost(), String.valueOf( host.getRemotePort() ), configurationManager.getServerURL() }  ) );

        while( running ) {
            try {

                // Accept incoming connections.
                Socket socket = serverSocket.accept();

                log.debug( "New connection received." );

                // Initiate the connection to the remote host.
                long connectionId = openHost();

                log.debug( "Connection opened to Server." );

                // Start the proxy threads.
                if( configurationManager.isUseStatelessConnection() ) {
                    log.debug("Using ReadWrite Thread.");
                    new ProxyReadWrite( getName() + "-ReadWrite", configurationManager, connectionId, socket ).start();
                }
                // Use the normal stateful read connection.
                else {
                    log.debug("Using seperate Read and Write threads.");
                    new ProxyReader( getName() + "-Reader", configurationManager, connectionId, socket ).start();
                    new ProxyWriter( getName() + "-Writer", configurationManager, connectionId, socket ).start();
                }
            }
            catch( IOException ioException )
            {
                // Ignore IOExceptions.  They occur every second when the block timout expires.
            }
            catch( Exception e ) {
                log.error( "Error creating new connection.", e );
                //TODO: Need an error sink!
            }
        }
    }

    /**
     * Connect to the Proxy Server and request a connection to the remote
     * host:port.
     *
     * @return returns a unique connection ID for the new connection.
     * @throws Exception
     */
    public long openHost() throws Exception {

        HttpURLConnection urlConnection = configurationManager.getURLConnection();

        //Write parameters.
        BufferedWriter out = new BufferedWriter( new OutputStreamWriter( urlConnection.getOutputStream() ) );
        out.write("action=open");
        out.write("&");
        out.write("host=" + host.getRemoteHost());
        out.write("&");
        out.write("port=" + host.getRemotePort());

        if( configurationManager.isServerLoginRequired() )
        {
            out.write("&");
            out.write("username=" + configurationManager.getServerUsername());
            out.write("&");
            out.write("password=" + configurationManager.getServerPassword());
        }

        out.flush();
        out.close();

        // Post the request to the server.
        urlConnection.connect();

        BufferedReader reader = null;

        // Make sure we can do cleanup even if there is an error...
        try
        {
            // Get the response stream.
            reader = new BufferedReader( new InputStreamReader( urlConnection.getInputStream() ) );

            // If the post was successful, return the new id, otherwise
            // throw an exception.
            String result = reader.readLine();
            if( result.startsWith("SUCCESS") ) {
                long connectionId = Long.parseLong( reader.readLine() );
                System.out.println("Connection Successful");
                return connectionId;
            }
            else {
                throw new Exception( "Unable to connect to remote host: " + result );
            }
        }
        finally
        {
            // Do proper housekeeping.  If the urlConnection is
            // not closed, the read operation will fail.
            if( reader != null )
            {
                reader.close();
            }
            urlConnection.disconnect();
        }
    }
}
//EOF