/******************************************************************************
 * $Source: /cvsroot/telnetoverhttp/clients/java/src/java/soht/client/java/configuration/Host.java,v $
 * $Revision: 1.1 $
 * $Author: edaugherty $
 * $Date: 2003/09/09 20:59:39 $
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

package soht.client.configuration;

/**
 * Represents the information for a specific Host mapping.
 *
 * @author Eric Daugherty
 */
public class Host
{
    //***************************************************************
    // Variables
    //***************************************************************

    private int localPort;
    private String remoteHost;
    private int remotePort;

    //***************************************************************
    // Constructor
    //***************************************************************

    /**
     * Creates a new Host instance.  If either port can not be converted
     * to an int, a ConfigurationException is thrown.
     *
     * @param localPort local port String.
     * @param remoteHost remote host String.
     * @param remotePort remote port String.
     * @throws ConfigurationException thrown if the port strings are invalid.
     */
    public Host( String localPort, String remoteHost, String remotePort ) throws ConfigurationException
    {
        try
        {
            this.localPort = Integer.parseInt( localPort );
        }
        catch( NumberFormatException e )
        {
            throw new ConfigurationException( "Invalid local port.  Port must be a number!" );
        }
        this.remoteHost = remoteHost;
        try
        {
            this.remotePort = Integer.parseInt( remotePort );
        }
        catch( NumberFormatException e )
        {
            throw new ConfigurationException( "Invalid remote port.  Port must be a number!" );
        }
    }

    //***************************************************************
    // Parameter Access Methods
    //***************************************************************

    public int getLocalPort()
    {
        return localPort;
    }

    public void setLocalPort( int localPort )
    {
        this.localPort = localPort;
    }

    public String getRemoteHost()
    {
        return remoteHost;
    }

    public void setRemoteHost( String remoteHost )
    {
        this.remoteHost = remoteHost;
    }

    public int getRemotePort()
    {
        return remotePort;
    }

    public void setRemotePort( int remotePort )
    {
        this.remotePort = remotePort;
    }
}
