package client;

import com.fasterxml.jackson.annotation.JsonView;

public class ServerTaskOrder {
	@JsonView()
	private int JobId;
	
	@JsonView()
	private String WorkerVersion;
	
	@JsonView()
	private String WorkerURL;
	
	@JsonView()
	private String WorkerClassName;
	
	@JsonView()
	private int Task;
	
	@JsonView()
	private long ComeBackInSeconds;

	public int getJobID() {
		return JobId;
	}

	public String getWorkerVersion() {
		return WorkerVersion;
	}

	public String getWorkerURL() {
		return WorkerURL;
	}

	public String getWorkerClassName() {
		return WorkerClassName;
	}

	public int getTask() {
		return Task;
	}
	
	public long getComeBackInSeconds() {
		return ComeBackInSeconds;
	}
}
