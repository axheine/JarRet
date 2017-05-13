package client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;

import http.HTTPClient;
import http.HTTPResponse;
import worker.Worker;
import worker.WorkerFactory;

public class Client {
	private final InetSocketAddress serverAddress;
	private final String clientID;
	
	public Client (String serverAddress, int port, String clientID) {
		Objects.requireNonNull(serverAddress);
		if (port <= 0) {
			throw new IllegalArgumentException("port can't be null or regative.");
		}
		this.serverAddress = new InetSocketAddress(serverAddress, port);
		this.clientID = Objects.requireNonNull(clientID);
	}
	
	public void run() {
		while (!Thread.interrupted()) {
			ServerTaskOrder sto = null;
			Worker worker = null;
			try {
				sto = getTaskFromServer();
			} catch (IOException e) {
				System.err.println("Got error from server : " + e);
			}
			
			if (sto.getComeBackInSeconds() > 0) {
				try {
					Thread.sleep(sto.getComeBackInSeconds() * 1000);
				} catch (InterruptedException e) {
					System.err.println("Something went wrong while sleeping : " + e);
				}
				continue;
			}
			
			try {
				worker = WorkerFactory.getWorker(sto.getWorkerURL(), sto.getWorkerClassName());
			} catch (MalformedURLException | ClassNotFoundException | IllegalAccessException
					| InstantiationException e) {
				System.err.println("Something went wrong while getting worker " + e);
			}
			
			worker.compute(1);
		}
	}

	private ServerTaskOrder getTaskFromServer() throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		HTTPClient httpclient = new HTTPClient();
		
		String query = "GET Task HTTP/1.1\r\nHost:" + serverAddress + "\r\n\r\n";
		System.out.println(query);
		
		HTTPResponse serverAnswer = httpclient.sendGetQuery(serverAddress, query);
		
		System.out.println(serverAnswer);
		
		ServerTaskOrder sto = mapper.readValue(serverAnswer.body, ServerTaskOrder.class);
		//System.out.println(sto);
		return sto;
	}
	
	
	private static void use() {
		System.out.println("Client serverAddress port clientID");
	}
	
	public static void main(String[] args) {
		if (args.length != 3) {
			use();
		}
		Client c = new Client(args[0], Integer.parseInt(args[1]), args[2]);
		c.run();
	}

	
}
