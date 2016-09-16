package soht.client.core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.UnknownHostException;

import soht.client.configuration.ConfigurationManager;

public class Resolver {

	ConfigurationManager configurationManager;

	public Resolver(ConfigurationManager configurationManager) {
		this.configurationManager = configurationManager;
	}

	public InetAddress resolve(String domainName) throws UnknownHostException, IOException {
		HttpURLConnection urlConnection = configurationManager.getURLConnection();

		// Write parameters.
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(urlConnection.getOutputStream()));
		out.write("action=resolve");
		out.write("&");
		out.write("domainName=" + domainName);
		out.flush();
		out.close();

		// Post the read request to the server.
		urlConnection.connect();

		InputStream in = null;

		// Make sure we can do cleanup even if there is an error...
		try {
			in = urlConnection.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(in));

			if (br.readLine().equals("SUCCESS")) {
				return InetAddress.getByName(br.readLine());
			} else {
				throw new UnknownHostException(domainName);
			}

		} finally {
			if (in != null) {
				in.close();
			}
			urlConnection.disconnect();
		}
	}

}
