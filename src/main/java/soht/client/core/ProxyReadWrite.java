/******************************************************************************
 * $Source: /cvsroot/telnetoverhttp/clients/java/src/java/soht/client/java/core/ProxyReadWrite.java,v $
 * $Revision: 1.2 $
 * $Author: edaugherty $
 * $Date: 2004/09/14 21:53:40 $
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

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.HttpURLConnection;
import java.net.SocketException;

import soht.client.configuration.ConfigurationManager;

/**
 * Handles the incoming and ougoing data when using
 * stateless connections.
 *
 * @author Eric Daugherty
 */
public class ProxyReadWrite extends BaseProxy
{

    //***************************************************************
    // Constants
    //***************************************************************

	private static final long DEFAULT_SLEEP_TIME = 200;
	private static final long MAX_SLEEP_TIME = 2000;

    //***************************************************************
    // Variables
    //***************************************************************

    /** The input stream to read from the local client */
    private InputStream in;

    /** The output stream to write to the local client */
    private OutputStream out;

    //***************************************************************
    // Constructor
    //***************************************************************

    public ProxyReadWrite( String name, ConfigurationManager configurationManager, long connectionId, Socket socket ) throws IOException {

        super( name, configurationManager, connectionId, socket );

        this.in = socket.getInputStream();
        this.out = socket.getOutputStream();
    }

    //***************************************************************
    // Thread Methods
    //***************************************************************

    public void run() {

        boolean isRunning = true;
        long sleepTime = DEFAULT_SLEEP_TIME;

        try {

            HttpURLConnection urlConnection;
            BufferedWriter out;

			byte[] inputBytes = new byte[1024];
			byte[] outputBytes= new byte[8192];
			int inputCount = 0;
			boolean inputShutdown = false;
			int outputCount = 0;
			while (isRunning)
			{
				try
				{
					inputCount = 0;
					int available = in.available();
					if (available > 0)
					{
						if (available >= inputBytes.length)
							inputCount = in.read(inputBytes);
						else
							inputCount = in.read(inputBytes,0,available);
					}
					else if (outputCount == 0)
					{
						long sleepTill = System.currentTimeMillis()+sleepTime;
						socket.setSoTimeout(50);
						while ((sleepTill > System.currentTimeMillis()) && (inputCount == 0))
						{
							try
							{
								// This is the only way I know to test for socket
								// conncection closed...
								inputCount = in.read(inputBytes);
								if (inputCount < 0)
								{
									inputShutdown = true;
									break;
								}
							}
							catch (Exception e)
							{
								//
							}
						}
						if (inputCount == 0)
						{
							sleepTime += 200;
							if (sleepTime >= MAX_SLEEP_TIME)
								sleepTime = MAX_SLEEP_TIME;
						}
						else
							sleepTime = DEFAULT_SLEEP_TIME;
					}
				}
				catch (SocketException socketException)
				{
					// This is normal. Only print out an error if it was not a
					// Socket Closed Exception.
					if (!"Socket closed".equals(socketException.getMessage()))
					{
						System.out.println("Error reading data from server: "
								+socketException);
					}
					closeServer();
					break;
				}
				if (inputShutdown)
				{
					closeServer();
					break;
				}
				urlConnection = configurationManager.getURLConnection();
				//Write parameters.
				out = new BufferedWriter(new OutputStreamWriter(urlConnection
						.getOutputStream()));
				out.write("action=readwrite");
				out.write("&");
				out.write("id="+connectionId);
				out.write("&");
				out.write("datalength="+inputCount);
				out.write("&");
				out.write("data=");
				out.write(encode(inputBytes, inputCount));

				out.flush();
				out.close();

				urlConnection.connect();
				InputStream serverInputStream = urlConnection.getInputStream();

				// Read data from the server and write it to our local client.

				outputCount = 0;
				boolean isFirst = true;
				int startIndex = 1;
				while (true)
				{

					int count = serverInputStream.read(outputBytes);

					// Read until the server disconnects.
					if (count==-1)
					{
						break;
					}
					if (isFirst&&count>0&&outputBytes[0]==0)
					{
						isRunning = false;
						break;
					}

					// Pass the data to the client, minus the status byte.
					startIndex = isFirst ? 1 : 0;
					try
					{
						this.out.write(outputBytes, startIndex, count-startIndex);
					}
					catch (IOException e)
					{
						// The local connection is closed, so close the server.
						closeServer();
					}
					isFirst = false;
					outputCount += (count-startIndex);
				}
			}
			// Close the socket.
			socket.close();
		}
		catch (Exception ioe)
		{
			ioe.printStackTrace();
		}
    }

}
