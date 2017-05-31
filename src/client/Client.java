package client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.nio.ByteBuffer;
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
	private HTTPClient httpclient;
	private HashMap<String, Worker> workers;
	private final ByteBuffer queries = ByteBuffer.allocate(4096);

	public Client(String serverAddress, int port, String clientID) throws IOException {
		Objects.requireNonNull(serverAddress);
		if (port <= 0) {
			throw new IllegalArgumentException("port can't be null or regative.");
		}
		this.serverAddress = new InetSocketAddress(serverAddress, port);
		this.clientID = Objects.requireNonNull(clientID);
		this.httpclient = new HTTPClient(this.serverAddress);
		workers = new HashMap<String, Worker>();
	}

	public void run() {
		while (!Thread.interrupted()) {
			ServerTaskOrder sto = null;
			Worker worker = null;
			try {
				System.out.println("Asking new task to server...");
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

			System.out.println("Found some task! Getting worker...");
			worker = getWorker(sto);
			
			
			System.out.println("Computing answer...");
			String answer = worker.compute(sto.getTask());
			
			System.out.println("Building and sending answer packet to server...");
			buildResponse(sto, answer);
			
			HTTPResponse r = null;
			try {
				r = httpclient.sendQuery(serverAddress, queries);
			} catch (IOException e) {
				e.printStackTrace();
			}

			System.out.println("Response : " + r.header);
		}
	}
	
	private Worker getWorker(ServerTaskOrder sto) {
		try {
			String key = sto.getWorkerURL() + "/" + sto.getWorkerVersion();
			if (workers.containsKey(key)) {
				System.out.println("Already have the worker, getting it...");
				return workers.get(key);
			} else {
				System.out.println("Asking the worker to the server...");
				System.out.println("Get some Work at " + sto.getWorkerURL() + "/" + sto.getWorkerClassName());
				
				Worker worker = WorkerFactory.getWorker(sto.getWorkerURL(), sto.getWorkerClassName());
				workers.put(key, worker);
				return worker;
			}
		} catch (MalformedURLException | ClassNotFoundException | IllegalAccessException | InstantiationException
				| IllegalArgumentException | SecurityException e) {
			System.err.println("Something went wrong while getting worker " + e);
			return null;
		}
	}

	private void buildResponse(ServerTaskOrder sto, String answer) {
		StringBuilder contentBuilder = new StringBuilder();

		contentBuilder.append("{\n").append("\"JobId\": \"").append(sto.getJobID()).append("\",\n")
				.append("\"WorkerVersion\": \"").append(sto.getWorkerVersion()).append("\",\n")
				.append("\"WorkerURL\": \"").append(sto.getWorkerURL()).append("\",\n")
				.append("\"WorkerClassName\": \"").append(sto.getWorkerClassName()).append("\",\n")
				.append("\"Task\": \"").append(sto.getTask()).append("\",\n").append("\"ClientId\": \"")
				.append(clientID).append("\",\n").append("\"Answer\": ").append(answer).append("}");

		ByteBuffer content = Charset.forName("ASCII").encode(contentBuilder.toString());
		int bodyAnswerLength = content.capacity()+Long.BYTES+Integer.BYTES;
		//System.out.println("Capacity: "+content.capacity()+" "+Long.BYTES+" "+Integer.BYTES);
		
		StringBuilder headerBuilder = new StringBuilder("POST Answer HTTP/1.1\r\nHost: ");
		headerBuilder.append(serverAddress.getHostName())
		   .append("\r\nContent-Type: application/json\r\nContent-Length: ")
		   .append(bodyAnswerLength)
		   .append("\r\n\r\n");
		   //.append(Charset.forName("UTF-8").decode(content));
		
		ByteBuffer header = Charset.forName("ASCII").encode(headerBuilder.toString());
		queries.clear();
		queries.put(header);
		queries.putLong((long) sto.getJobID());
		queries.putInt(sto.getTask());
		queries.put(content);
		queries.flip();
	}

	private ServerTaskOrder getTaskFromServer() throws IOException {
		ObjectMapper mapper = new ObjectMapper();

		String query = "GET Task HTTP/1.1\r\nHost: " + serverAddress + "\r\n\r\n";
		System.out.println(query);

		HTTPResponse serverAnswer = httpclient.sendQuery(serverAddress, query);

		ServerTaskOrder sto = mapper.readValue(serverAnswer.body, ServerTaskOrder.class);
		// System.out.println(sto);
		return sto;
	}

	private static void use() {
		System.out.println("Client serverAddress port clientID");
	}

	public static void main(String[] args) throws NumberFormatException, IOException {
		if (args.length != 3) {
			use();
		}
		Client c = new Client(args[0], Integer.parseInt(args[1]), args[2]);
		c.run();
	}

}
