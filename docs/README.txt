SOHT (Socket over HTTP Tunneling)
http://www.ericdaugherty.com/dev/soht

Installation

To install the client, create a directory and extract the contents
of the zip file into that directory.

The client uses a properties file to map local ports to remote
hosts.  A sample file has been included.  Please modify this
file to suit your needs.

The client is packaged as executable .JAR file.  You can start it with
the following command:

java -jar soht-client-<version>.jar

where <version> matches the current version you are using.  You must
either have a soht.properties file in your current directory or specify one
as an argument, such as:

java -jar soht-client-<version>.jar c:\soht.properties
