/**
 * Copyright (c) 2003-2004 Craig Setera
 * All Rights Reserved.
 * Licensed under the Academic Free License version 1.2
 * For more information see http://www.opensource.org/licenses/academic.php
 */
package soht.client.socks;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.log4j.Logger;

import soht.client.configuration.ConfigurationException;
import soht.client.configuration.ConfigurationManager;

/**
 * The SOCKS main server program.
 * <p />
 * Copyright (c) 2003-2004 Craig Setera<br>
 * All Rights Reserved.<br>
 * Licensed under the Academic Free License version 1.2<p/>
 * <br>
 * $Revision: 1.1 $
 * <br>
 * $Date: 2004/06/29 21:08:52 $
 * <br>
 * @author Craig Setera
 */
public class SocksServer extends Thread {
	// Whether we are currently running
	private boolean running;
	
	// The configuration manager instance
	private ConfigurationManager configurationManager;
	private ServerSocket listeningSocket;

    private static Logger log = Logger.getLogger( SocksServer.class );

	/**
	 * Construct a new SOCKS server program.
	 * 
	 * @param configurationManager
	 */
	public SocksServer(ConfigurationManager configurationManager) {
		this.configurationManager = configurationManager;
		setPriority(Thread.NORM_PRIORITY - 1);
        setName( "Socks" );
        log.debug( "Socks Server Created." );
	}
	
	/**
	 * Run the SOCKS server main loop.
	 */
	public void run() {
		running = true;

		try {
			listeningSocket = new ServerSocket(configurationManager.getSocksServerPort());

            log.info( "Socks Server listening on port: " + configurationManager.getSocksServerPort() );

			while (running) {
				try {
					// Wait for the connection and pass on the
					// connection to the handler
					Socket connected = listeningSocket.accept();

                    log.debug( "New connection received." );

					// Start up a new proxy for the client connection
					SocksProxy proxy = new SocksProxy(configurationManager, connected);
					proxy.start();

				}
                catch (IOException e)
                {
					e.printStackTrace();
				}
                catch (ConfigurationException e)
                {
					e.printStackTrace();
				}
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Set the runnable state of the listener.
	 * @param running
	 */
	public void setRunning(boolean running) {
		this.running = running;
		
		if (!running && (listeningSocket != null)) {
			try {
				listeningSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
