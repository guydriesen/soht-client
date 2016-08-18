/******************************************************************************
 * $Source: /cvsroot/telnetoverhttp/clients/java/src/java/soht/client/java/console/ConsoleProxy.java,v $
 * $Revision: 1.5 $
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

package soht.client.console;

import java.util.Iterator;

import soht.client.configuration.ConfigurationManager;
import soht.client.configuration.Host;
import soht.client.core.Proxy;
import soht.client.socks.SocksServer;

/**
 * Provides a console based proxy client.  This client
 * can load a properties file and start a set of proxies.
 *
 * @author Eric Daugherty
 */
public class ConsoleProxy extends Thread {

    //***************************************************************
    // Helper Methods
    //***************************************************************

    private static void showUsage()
    {
        System.out.println( "SOHT Java Client" );
        System.out.println( "The SOHT Java Client requires a properties file.  Either start" );
        System.out.println( "the application in the same directory as the soht.properties" );
        System.out.println( "file, or specify the file name on the command line: " );
        System.out.println( "java -jar soht-cleint-<version>.jar c:\\soht.properties" );
    }

    //***************************************************************
    // Main Method
    //***************************************************************

    /**
     * Entry point for the Console Proxy.
     *
     * @param args
     * @throws Exception
     */
    public static void main( String args[] ) throws Exception {

        ConfigurationManager configurationManager = null;
        try
        {
            if( args.length == 0 )
            {
                configurationManager = new ConfigurationManager( "soht.properties" );
            }
            else if( args[0].equalsIgnoreCase( "?" ) ||
                    args[0].equalsIgnoreCase( "/?" ) ||
                    args[0].equalsIgnoreCase( "-h" ) ||
                    args[0].equalsIgnoreCase( "/h" ) ||
                    args[0].equalsIgnoreCase( "help" )
                    )
            {
                showUsage();
            }
            else if( args.length == 1  )
            {
                configurationManager = new ConfigurationManager( args[0] );
            }
            else
            {
                showUsage();
            }
        }
        catch( Exception e )
        {
            System.out.println( e.getMessage() );
            showUsage();
        }

        if( configurationManager != null )
        {
            Iterator<Host> hosts = configurationManager.getHosts().iterator();
            while( hosts.hasNext() )
            {
                Host host = hosts.next();
                new Proxy( configurationManager, host ).startProxy();
            }
            
            // Enable the socks server as requested
            if (configurationManager.isSocksServerEnabled()) {
            	SocksServer socksServer = new SocksServer(configurationManager);
            	socksServer.start();
            }
        }
    }
}
//EOF
