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
	private ByteBuffer in;
	private ByteBuffer out;
	private String status;
	private StringBuilder sb;
	private HashMap<String, String> headerProperties;
	private HTTPHeader httpheader;
	private int bodyLength;
	
	
	private enum ReadState {
		HEADER,
		HEADER_R,
		BODY,
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
		in = super.getIn().flip();
		
		if(currentReadState == ReadState.HEADER || currentReadState == ReadState.HEADER_R) {
			System.out.println("ReadHeader");
			receiveHeader();
		}
		if(currentReadState == ReadState.BODY) {
			System.out.println("Read body");
			receiveBody();
		}
		if(currentReadState == ReadState.FINISHED) {
			System.out.println(sb.toString());
			String answer = generateAnswerForWait();
			out.put(Charset.forName("UTF-8").encode(answer));
		}
		
	}

	private void receiveHeader() {
		while (in.remaining() >= Character.BYTES) {
			CharBuffer cb = Charset.forName("ASCII").decode(in);
			for (int i = 0; i < cb.length(); i++) {
				char c = cb.charAt(i);
				//System.out.println(c);
				if(c == '\r') { //si on lit un \r on change d'état pour indiquer qu'on en à lu un
					this.currentReadState = ReadState.HEADER_R;
					continue;
				}
				if(c == '\n' && currentReadState == ReadState.HEADER_R) { // si on lit un \n et que le \r à été lu a l'étape d'avant
					if(sb.length() == 0) { // si le sb est vide, on a lu un \r\n avant, c'est la fin du header
						try {
							System.out.println("create header");
							httpheader = HTTPHeader.create(status, headerProperties);
							bodyLength = httpheader.getContentLength();
							this.currentReadState = ReadState.FINISHED;
							return;
						} catch (HTTPException e) {
							
						}
					} else { // si il n'y a pas qu'un \r dans le sb, on le découpe pour créer la ligne
						String line = sb.toString();
						sb.delete(0, sb.length());
						int splitterIndex = line.indexOf(':');
						System.out.println("Splitter value : " + splitterIndex);
						if (splitterIndex == -1) { // si il n'y a pas de ";" la ligne est l'entete
							String[] tokens = line.split(" ");
							status = tokens[0] + " " + tokens[1];
							System.out.println(status);
						} else { // sinon c'est une propriété
							//System.out.println("Header line: "+line.substring(0, splitterIndex) +" / "+ line.substring(splitterIndex + 2));
							headerProperties.put(line.substring(0, splitterIndex), line.substring(splitterIndex + 2));
						}
					}
					continue;
				}
				if(c!='\r') {
					sb.append(c);
				}
				
				System.out.println(sb.toString());
			}
		}
	}
	
	private void receiveBody() {
		System.out.println("enter create body");
		while(in.hasRemaining()) {
			if(bodyLength <= 0) {
				currentReadState = ReadState.FINISHED;
				break;
			}
			char c = in.getChar();
			sb.append(c);
			bodyLength --;
		}
	}

	/**
	 * Return string containing header and body with new task generated
	 * @return
	 */
	private String generateAnswerForTask() {
		StringBuilder sb = new StringBuilder();
		String task = server.getNextTaskToCompute();
		int bodyLength = Charset.forName("UTF-8").encode(task).limit();
		
		sb	.append("HTTP/1.1 200 OK\r\nContent-type: application/json; charset=utf-8\r\nContent-length: ")
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
		
		sb	.append("HTTP/1.1 200 OK\r\nContent-type: application/json; charset=utf-8\r\nContent-length: ")
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
