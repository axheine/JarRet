package server;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;

import http.HTTPException;
import http.HTTPHeader;

public class TaskContext extends Context {
	private final Server server;
	private ReadState currentReadState;
	//private ByteBuffer in;
	//private ByteBuffer out;
	private String status;
	private StringBuilder sb;
	private HashMap<String, String> headerProperties;
	private String httpVersion;
	private HTTPHeader httpheader;
	private int bodyLength;
	private ByteBuffer answer;
	
	
	private enum ReadState {
		HEADER,
		BODY,
		PROCESS_HEADER,
		PROCESS_ANSWER,
		FINISHED
	}


	public TaskContext(SelectionKey key, int BUFF_SIZE, Server server) {
		super(key, BUFF_SIZE);
		this.server = server;
		this.currentReadState = ReadState.HEADER;
		this.sb = new StringBuilder();
		this.headerProperties = new HashMap<>();
	}

	@Override
	void process() {
		// TODO Auto-generated method stub
		/*
		 * - lire les infos dedans
		 * - Lire l'entete 
		 * 		- si c'est une demande de task
		 * 				- on en demande une server.getNextTaskToCompute();
		 * 				- on retourne la réponse avec le json propre
		 * 
		 * 		- si c'est un post
		 * 			- on vérifie si il est bien formé
		 * 			- on enregistre les infos dans le bon fichier --> acknowledgeTaskComputation();
		 * 			- on renvoie la réponse
		 */
		super.getIn().flip();
		super.setIsRunningProcess(true);
		if(currentReadState == ReadState.HEADER) {
			System.out.println("Read Header");
			receiveHeader();
		}
		if(currentReadState == ReadState.BODY) {
			System.out.println("Read body");
			receiveBody();
		}
		if(currentReadState == ReadState.PROCESS_HEADER) {
			processHeader();
		}
		if(currentReadState == ReadState.PROCESS_ANSWER) {
			processAnswer();
		}
		if(currentReadState == ReadState.FINISHED) {
			super.setIsRunningProcess(false);
		}
	}

	private void receiveHeader() {
		while (super.getIn().remaining() >= Character.BYTES) {
			CharBuffer cb = Charset.forName("ASCII").decode(super.getIn());
			sb.append(cb);
			
			if(sb.indexOf("\r\n\r\n") != -1) {
				System.out.println("All bytes received for header");
				this.currentReadState = ReadState.BODY;
				processHeader();
			}
		}
	}
	
	private void processHeader() {
		for (String line: sb.toString().split("\r\n")) {
			System.out.println("Line : " + line);
			int splitterIndex = line.indexOf(':');
			if (splitterIndex == -1) {
				status = line;
				System.out.println("Status : " + status);
			} else {
				headerProperties.put(line.substring(0, splitterIndex), line.substring(splitterIndex + 2));
			}
		}
		try {
			httpheader = HTTPHeader.create("HTTP/1.1 200 OK", headerProperties);
		} catch (HTTPException e) {
			System.err.println("Error while creating HTTPHeader object");
			e.printStackTrace();
		}
	}

	private void receiveBody() {
		System.out.println("Enter create body");
		while(super.getIn().hasRemaining()) {
			if(bodyLength <= 0) {
				currentReadState = ReadState.PROCESS_ANSWER;
				break;
			}
			
			char c = super.getIn().getChar();
			sb.append(c);
			bodyLength --;
		}
	}
	
	private void processAnswer() {
		System.out.println("Start processing answer");
		
		if(status.startsWith("GET Task")) {
			if(server.hasTaskToCompute()) {
				answer = Charset.forName("UTF-8").encode(generateAnswerForTask());
			} else {
				answer = Charset.forName("UTF-8").encode(generateAnswerForWait());
			}
		}
		
		if(status.startsWith("POST Answer")) {
			//TODO : save in jobs result file (thinks to read long and int)
			System.out.println("POST RECEIVED : ");
			answer = Charset.forName("UTF-8").encode(generateOKAnswer());
		}
		super.getOut().put(answer);
	}

	/**
	 * Return string containing header and body with new task generated
	 * @return
	 */
	private String generateAnswerForTask() {
		StringBuilder sb = new StringBuilder();
		String task = server.getNextTaskToCompute();
		int bodyLength = Charset.forName("UTF-8").encode(task).limit();
		
		sb	.append("HTTP/1.1 200 OK\r\nContent-type: application/json; charset=utf-8\r\nContent-Length: ")
			.append(bodyLength)
			.append("\r\n\r\n")
			.append(task);
		return sb.toString();
	}
	
	private String generateAnswerForWait() {
		StringBuilder sb = new StringBuilder();
		StringBuilder sbbody = new StringBuilder();
		sbbody	.append("{\"ComeBackInSeconds\" : ")
				.append(300)
				.append("}");
		
		int bodyLength = Charset.forName("UTF-8").encode(sbbody.toString()).limit();
		
		sb	.append("HTTP/1.1 200 OK\r\nContent-type: application/json; charset=utf-8\r\nContent-Length: ")
			.append(bodyLength)
			.append("\r\n\r\n")
			.append(sbbody);

		return sb.toString();
	}
	
	private String generateOKAnswer() {
		StringBuilder sb = new StringBuilder();
		sb	.append("HTTP/1.1 200 OK\r\nContent-length: ")
		.append("0")
		.append("\r\n\r\n");
		
		return sb.toString();
	}
	
	private String generateErrorAnswer() {
		StringBuilder sb = new StringBuilder();
		sb	.append("HTTP/1.1 400")
		.append("\r\n\r\n");
		
		return sb.toString();
	}
}
