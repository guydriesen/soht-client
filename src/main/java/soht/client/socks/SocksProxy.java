/**
 * Copyright (c) 2003-2004 Craig Setera
 * All Rights Reserved.
 * Licensed under the Academic Free License version 1.2
 * For more information see http://www.opensource.org/licenses/academic.php
 */
package soht.client.socks;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import soht.client.configuration.ConfigurationException;
import soht.client.configuration.ConfigurationManager;
import soht.client.configuration.Host;
import soht.client.core.BaseProxy;
import soht.client.core.Proxy;
import soht.client.core.ProxyReadWrite;
import soht.client.core.ProxyReader;
import soht.client.core.ProxyWriter;

import org.apache.log4j.Logger;

/**
 * Proxy for a SOCKS server connection.
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
public class SocksProxy extends Proxy {
	// Version 4 constants
	public static final int V4_CMD_BIND = 0x02;
	public static final int V4_CMD_CONNECT = 0x01;
	public static final int V4_CMD_UDP_ASSOCIATE = 0x03;
	
	public static final int V4_RESPONSE_CANT_CONNECT_IDENTD = 92;
	public static final int V4_RESPONSE_FAILED = 91;
	public static final int V4_RESPONSE_ID_MISMATCH = 93;
	public static final int V4_RESPONSE_SUCCESS = 90;
	
	// Version 5 constants
	public static final int V5_ADDR_DOMAINNAME = 0x03;	
	public static final int V5_ADDR_IP_V4 = 0x01;
	public static final int V5_ADDR_IP_V6 = 0x04;
	
	public static final int V5_AUTH_GSSAPI = 0x01;
	public static final int V5_AUTH_NO_AUTH = 0x00;
	public static final int V5_AUTH_NONE_ACCEPTABLE = 0xFF;
	public static final int V5_AUTH_USER_PASS = 0x02;
	
	public static final int V5_CMD_BIND = 0x02;
	public static final int V5_CMD_CONNECT = 0x01;
	public static final int V5_CMD_UDP_ASSOCIATE = 0x03;
	
	public static final int V5_REPLY_ADDRESS_TYPE_NOT_SUPPORTED = 0x08;
	public static final int V5_REPLY_COMMAND_NOT_SUPPORTED = 0x07;
	public static final int V5_REPLY_CONNECT_REFUSED = 0x05;
	public static final int V5_REPLY_GENERAL_FAILURE = 0x01;
	public static final int V5_REPLY_HOST_UNREACHABLE = 0x04;
	public static final int V5_REPLY_NETWORK_UNREACHABLE = 0x03;
	public static final int V5_REPLY_NOT_ALLOWED = 0x02;	
	public static final int V5_REPLY_SUCCESS = 0x00;
	public static final int V5_REPLY_TTL_EXPIRED = 0x06;
	
	// Instance variables
	private ConfigurationManager configurationManager;
	private Host host;
	private Socket socksSocket;

    private static Logger log = Logger.getLogger( SocksProxy.class );

	/**
	 * Create a new SocksProxy.
	 * 
	 * @param configurationManager
	 * @param host
	 * @param socksSocket
	 */
	public SocksProxy(
		ConfigurationManager configurationManager,
		Host host,
		Socket socksSocket)
	{
		super(configurationManager, host);
		
		this.host = host;
		this.configurationManager = configurationManager;
		this.socksSocket = socksSocket;
		
		setPriority(Thread.NORM_PRIORITY - 1);
	}
	
	/**
	 * Create a new SocksProxy.
	 * 
	 * @param configurationManager
	 * @param socksSocket
	 * @throws ConfigurationException
	 */
	public SocksProxy(
		ConfigurationManager configurationManager, 
		Socket socksSocket) 
			throws ConfigurationException 
	{
		this(configurationManager, new Host("0", "", "0"), socksSocket);
	}
	
	/**
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		try
        {
            log.debug( "SocksProxy Started" );
			// Need to figure out the version number before we can
			// do anything else
			DataInputStream inputStream = 
				new DataInputStream(socksSocket.getInputStream());
			DataOutputStream outputStream =
				new DataOutputStream(socksSocket.getOutputStream());
			
			int socksVersion = inputStream.read();

			// Switch based on the socks version requested by the
			// client
			if (socksVersion == 4)
            {
                log.debug( "Socks Version 4" );
				v4Run(inputStream, outputStream);
			}
            else if (socksVersion == 5)
            {
                log.debug( "Socks Version 5" );
				v5Run(inputStream, outputStream);
			}
            else
            {
                log.error( "Unable to identify Socks Version." );
				// We don't recognize the version, so just bail out...
				socksSocket.close();
			}
		}
        catch (Exception e)
        {
            log.error( "Error in SocksProxy.", e );
		}
	}

	/**
	 * Do the common work of the proxy connection.
	 * @throws Exception
	 */
	private void commonProxyConnect() 
		throws Exception 
	{
		// TODO This connection really should pass back information
		// to the client if something goes wrong during the proxy
		// connection.  V5 has space in the response for connection
		// errors.
		
        // Initiate the connection to the remote host.
        long connectionId = openHost();

        // Start the proxy threads.
        if( configurationManager.isUseStatelessConnection() ) {
            System.out.println("Using ReadWrite Thread.");
            BaseProxy proxy = new ProxyReadWrite( 
            	getName() + "-ReadWrite", 
				configurationManager, 
				connectionId, 
				socksSocket);
            
            proxy.start();
        }
        // Use the normal stateful read connection.
        else {
            System.out.println("Using seperate Read and Write threads.");
            
            BaseProxy readProxy = new ProxyReader( 
            	getName() + "-Reader", 
				configurationManager, 
				connectionId, 
				socksSocket);
            readProxy.start();
            
            BaseProxy writeProxy = new ProxyWriter( 
            	getName() + "-Writer", 
				configurationManager, 
				connectionId, 
				socksSocket);
            writeProxy.start();
        }
	}
	
	/**
	 * Do a version 4 connection and response.
	 * 
	 * @param inputStream
	 * @param outputStream
	 * @throws Exception
	 */
	private void v4Connect(DataInputStream inputStream, DataOutputStream outputStream) 
		throws Exception 
	{
		// Read destination information
		byte[] addressBytes = new byte[4];
		int destinationPort = inputStream.readUnsignedShort();
		inputStream.read(addressBytes);
		
		// Read the username (which we will ignore)
		int byteRead = 0;
		ByteArrayOutputStream username = new ByteArrayOutputStream();
		do {
			byteRead = inputStream.read();
			if (byteRead != 0) username.write(byteRead);
		} while (byteRead != 0);
		
		// Update the host before attempting the connection
		// TODO This address *should* be the actual address, although
		// the clients may not really care.  This would have to
		// be retrieved from the proxy somehow.
		InetAddress address = InetAddress.getByAddress(addressBytes);
		host.setRemoteHost(address.getHostAddress());
		host.setRemotePort(destinationPort);
		
		// Reply to the client
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(bos);
		dos.write(0x00);
		dos.write(V4_RESPONSE_SUCCESS);
		dos.writeShort(destinationPort);
		dos.write(addressBytes);
		dos.flush();
		outputStream.write(bos.toByteArray());
		
		// Do the proxy connection
		commonProxyConnect();
	}
	
	/**
	 * Run a SOCKS version 4 handler.
	 * 
	 * @param inputStream
	 * @param outputStream
	 * @throws Exception
	 */
	private void v4Run(DataInputStream inputStream, DataOutputStream outputStream) 
		throws Exception 
	{
		int command = inputStream.read();
		switch (command) {
			case V4_CMD_CONNECT:
				v4Connect(inputStream, outputStream);
				break;
				
			default:
				v4SendErrorResponse(outputStream, V4_RESPONSE_FAILED);
				break;
		}
	}

	/**
	 * Send a Version 4 error response down the stream.
	 * 
	 * @param outputStream
	 * @param code
	 * @throws IOException
	 */
	private void v4SendErrorResponse(DataOutputStream outputStream, int code) 
		throws IOException 
	{
		v4SendResponse(outputStream, code, 0, null);
		
		// Errors cause the connection to be closed
		socksSocket.close();
	}

	/**
	 * Send a Version 4 response down the stream.
	 * 
	 * @param outputStream
	 * @param code
	 * @param port
	 * @param address
	 * @throws IOException
	 */
	private void v4SendResponse(
		DataOutputStream outputStream, 
		int code, 
		int port, 
		InetAddress address) 
			throws IOException
	{
		outputStream.write(code);
		outputStream.writeShort(port);
		outputStream.write(address.getAddress());
	}

	/**
	 * Do a version 5 connection.
	 * 
	 * @param inputStream
	 * @param outputStream
	 * @param address
	 * @param destinationPort
	 * @throws Exception
	 */
	private void v5Connect(
		DataInputStream inputStream, 
		DataOutputStream outputStream, 
		InetAddress address, 
		int destinationPort) 
			throws Exception 
	{
		// Build the successful reply to the client
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(bos);
		
		byte[] addressBytes = address.getAddress();
		dos.write(new byte[] { (byte) 0x05, (byte) V5_REPLY_SUCCESS, (byte) 0x00 });
		if (addressBytes.length == 4) {
			dos.write(V5_ADDR_IP_V4);
		} else {
			dos.write(V5_ADDR_IP_V6);
		}
		dos.write(addressBytes);
		dos.writeShort(50000);
		dos.flush();

		// Update the host before attempting the connection
		// TODO This address should be the actual address to
		// which the connection was made.  Although it is unclear
		// whether the client really cares.
		host.setRemoteHost(address.getHostAddress());
		host.setRemotePort(destinationPort);

		// Let the client know it is ok to proceed
		outputStream.write(bos.toByteArray());
		
		// Do the proxy connection
		commonProxyConnect();
	}

	/**
	 * Get the InetAddress from a V5 SOCKS stream.
	 * 
	 * @param inputStream
	 * @return
	 * @throws IOException
	 */
	private InetAddress v5GetInetAddress(DataInputStream inputStream) 
		throws IOException 
	{
		InetAddress address = null;
		
		int addressType = inputStream.read();
		byte[] addressBytes = null;
		
		switch (addressType) {
			case V5_ADDR_IP_V4:
				addressBytes = new byte[4];
				inputStream.read(addressBytes);
				address = InetAddress.getByAddress(addressBytes);
				break;
			
			case V5_ADDR_IP_V6:
				addressBytes = new byte[16];
				inputStream.read(addressBytes);
				address = InetAddress.getByAddress(addressBytes);
				break;
				
			case V5_ADDR_DOMAINNAME:
				int domainByteCount = inputStream.read();
				addressBytes = new byte[domainByteCount];
				inputStream.read(addressBytes);
				String domainName = new String(addressBytes);
				address = InetAddress.getByName(domainName);
				break;
		}
		
		return address;
	}

	/**
	 * Negotiate the authentication method with the client.  Return 
	 * a boolean indicating whether or not the negotiation was 
	 * successful.
	 * 
	 * @param inputStream
	 * @param outputStream
	 * @return
	 * @throws IOException
	 */
	private boolean v5NegotiateAuthentication(
		DataInputStream inputStream, 
		DataOutputStream outputStream)
			throws IOException
	{
		int numberOfMethods = inputStream.read();
		
		// We only support no authentication option... Let's make sure
		// that they allow for that.
		boolean foundNoAuthRequiredMethod = false;
		for (int i = 0; i < numberOfMethods; i++) {
			int authMethod = inputStream.read();
			if (authMethod == V5_AUTH_NO_AUTH) {
				foundNoAuthRequiredMethod = true;
			}
		}
		
		// Respond to the client concerning authentication attempts
		byte[] response = null;
		if (foundNoAuthRequiredMethod) {
			response = new byte[] { (byte) 0x05, (byte) V5_AUTH_NO_AUTH };
		} else {
			response = new byte[] { (byte) 0x05, (byte) V5_AUTH_NONE_ACCEPTABLE };			
		}
		
		outputStream.write(response);
		
		return foundNoAuthRequiredMethod;
	}
	
	/**
	 * Run a SOCKS version 5 handler
	 * 
	 * @param inputStream
	 * @param outputStream
	 * @throws Exception
	 */
	private void v5Run(DataInputStream inputStream, DataOutputStream outputStream) 
		throws Exception 
	{
		if (v5NegotiateAuthentication(inputStream, outputStream)) {
			int version = inputStream.read();
			int command = inputStream.read();
			inputStream.skipBytes(1);
			
			// Read the requested address from the input stream
			InetAddress address = v5GetInetAddress(inputStream);
			int port = inputStream.readUnsignedShort();
			
			// Only handle the CONNECT command
			switch (command) {
				case V5_CMD_CONNECT:
					v5Connect(inputStream, outputStream, address, port);
					break;
					
				default:
					// TODO Is there some relatively easy to implement way
					// to implement UDP associate support?  Does it really
					// get used that much?
					v5SendCommandNotSupported(outputStream);
					break;
			}
		}
	}

	/**
	 * Send a command not supported response.
	 * 
	 * @param outputStream
	 * @throws IOException
	 */
	private void v5SendCommandNotSupported(DataOutputStream outputStream) 
		throws IOException 
	{
		byte[] response = 
			new byte[] { (byte) 0x05, (byte) V5_REPLY_COMMAND_NOT_SUPPORTED, (byte) 0x00 };
		outputStream.write(response);
		outputStream.flush();
		socksSocket.close();
	}
}
