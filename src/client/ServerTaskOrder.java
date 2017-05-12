package client;

import com.fasterxml.jackson.annotation.JsonView;

public class ServerTaskOrder {
	@JsonView()
	private String jobID;
	
	@JsonView()
	private String workerVersion;
	
	@JsonView()
	private String workerURL;
	
	@JsonView()
	private String workerClassName;
	
	@JsonView()
	private String task;

	public String getJobID() {
		return jobID;
	}

	public String getWorkerVersion() {
		return workerVersion;
	}

	public String getWorkerURL() {
		return workerURL;
	}

	public String getWorkerClassName() {
		return workerClassName;
	}

	public String getTask() {
		return task;
	}
}
