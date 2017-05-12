package client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

import com.fasterxml.jackson.databind.ObjectMapper;

import http.HTTPClient;
import http.HTTPResponse;

public class Client {
	private final InetSocketAddress serverAddress;
	
	public Client (String serverAddress, int port) {
		this.serverAddress = new InetSocketAddress(serverAddress, port);
	}
	
	public void run(SocketChannel sc, String clientID) {
		while (!Thread.interrupted()) {
			ServerTaskOrder sto = getTaskFromServer();
			
			
			
		}
	}

	private ServerTaskOrder getTaskFromServer() throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		HTTPClient httpclient = new HTTPClient();
		
		String query = "GET Task HTTP/1.1\r\nHost:" + serverAddress + "\r\n\r\n";
		System.out.println(query);
		
		HTTPResponse serverAnswer = httpclient.sendGetQuery(serverAddress, query);
		
		return mapper.readValue(serverAnswer.body, ServerTaskOrder.class);
	}
}
