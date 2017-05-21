package client;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Objects;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import http.HTTPClient;
import http.HTTPResponse;
import upem.jarret.worker.Worker;
import upem.jarret.worker.WorkerFactory;

public class Client {
	private final InetSocketAddress serverAddress;
	private final String clientID;
	private HTTPClient httpclient = new HTTPClient();
	
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
			
			System.out.println("Get some Work at " + sto.getWorkerURL() +"/"+ sto.getWorkerClassName());
			
			try {
				worker = WorkerFactory.getWorker(sto.getWorkerURL(), sto.getWorkerClassName());
				System.out.println("Found some work!");
			} catch (MalformedURLException | ClassNotFoundException | IllegalAccessException
					| InstantiationException | IllegalArgumentException | SecurityException e) {
				System.err.println("Something went wrong while getting worker " + e);
			}
			
			String answer = worker.compute(sto.getTask());
			
			ObjectMapper mapper = new ObjectMapper(); 
		    TypeReference<HashMap<String,Object>> typeRef 
		            = new TypeReference<HashMap<String,Object>>() {};

		    HashMap<String, Object> o = null;
			try {
				o = mapper.readValue(answer, typeRef);
			} catch (IOException e) {

			} 
			
			//HashMap<String, Object> mapAnswer = new HashMap<String, Object>();
			
			
			StringBuilder sb = new StringBuilder();
			
			sb	.append("{\n")
				.append("\"JobId\": \"").append(sto.getJobID()).append("\",\n")
				.append("\"WorkerVersion\": \"").append(sto.getWorkerVersion()).append("\",\n")
				.append("\"WorkerURL\": \"").append(sto.getWorkerURL()).append("\",\n")
				.append("\"WorkerClassName\": \"").append(sto.getWorkerClassName()).append("\",\n")
				.append("\"Task\": \"").append(sto.getTask()).append("\",\n")
				.append("\"ClientId\": \"").append(clientID).append("\",\n")
				.append("\"Answer\": ").append(answer)
				.append('}');
			
			
			int bodyAnswerLength = Charset.forName("ASCII").encode(sb.toString()).capacity();
			
			StringBuilder sb2 = new StringBuilder("POST Answer HTTP/1.1\r\nHost: ");
			sb2	.append(serverAddress)
				.append("\r\nContent-Type: application/json\r\nContent-Length: ")
				.append(bodyAnswerLength)
				.append("\r\n\r\n")
				.append(sb);
			
			
			System.out.println("Answer to server : \n" + sb2.toString());
			HTTPResponse r = null;
			try {
				r = this.httpclient.sendQuery(serverAddress, sb2.toString());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			System.out.println("response : " + r.header);
		}
	}

	private ServerTaskOrder getTaskFromServer() throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		HTTPClient httpclient = new HTTPClient();
		
		String query = "GET Task HTTP/1.1\r\nHost:" + serverAddress + "\r\n\r\n";
		System.out.println(query);
		
		HTTPResponse serverAnswer = httpclient.sendQuery(serverAddress, query);
		
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
