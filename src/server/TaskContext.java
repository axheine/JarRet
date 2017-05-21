package server;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.charset.Charset;

public class TaskContext extends Context {
	private final Server server;


	public TaskContext(SelectionKey key, int BUFF_SIZE, Server server) {
		super(key, BUFF_SIZE);
		this.server = server;
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
		sb	.append("HTTP/1.1 400 OK\r\nContent-type: application/json; charset=utf-8\r\nContent-length: ")
		.append("0")
		.append("\r\n\r\n");
		
		return sb.toString();
	}
}
