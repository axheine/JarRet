package server;

import com.fasterxml.jackson.annotation.JsonView;

public class JobDefinition {
	@JsonView()
	public int JobId;
	
	@JsonView()
	public String JobDescription;
	
	@JsonView()
	public String WorkerVersionNumber;
	
	@JsonView()
	public String WorkerURL;
	
	@JsonView()
	public String WorkerClassName;
	
	@JsonView()
	public int JobTaskNumber;
	
	@JsonView()
	public int JobPriority;
}
