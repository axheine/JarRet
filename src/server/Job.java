package server;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.BitSet;

public class Job {
	private final FileWriter outFile;
	private int numberOfTasks;
	
	private BitSet doneTasks;
	private BitSet askedTasks;
	private int tasksLeftToAck;
	private int tasksLeftToAsk;
	
	private final int id;
	private final int priority;
	private final String version;
	private final String workerUrl;
	private final String workerClass;

	public Job(File outFile, String workerUrl, String workerClass, int id, int numberOfTasks, int priority, String version)
			throws IOException {
		this.numberOfTasks = this.tasksLeftToAck = this.tasksLeftToAsk = numberOfTasks;
		doneTasks = new BitSet(numberOfTasks);
		askedTasks = new BitSet(numberOfTasks);
		this.outFile = new FileWriter(outFile);
		this.priority = priority;
		this.version = version;
		this.id = id;
		
		this.workerUrl = workerUrl;
		this.workerClass = workerClass;
	}
	
	public Job(File outFile, JobDefinition def) throws IOException {
		this(outFile, def.WorkerURL, def.WorkerClassName, def.JobId, def.JobTaskNumber, def.JobPriority, def.WorkerVersionNumber);
	}

	/**
	 * 
	 * @param task
	 * @param result
	 */
	public void acknowledgeTaskComputation(int task, String result) {
		try {
			outFile.write(result + "\n");
			doneTasks.set(task);
			tasksLeftToAck--;
		} catch (IOException e) {
			System.err.println("CRITICAL : Couldn't save task n°" + task + " result to file!!!");
		}
	}

	/**
	 * Return a JSON formatted task to ask to a client (called by getNextTaskToCompute server method)
	 * @return
	 */
	public String getNewTask() {
		if(isFinished()) throw new IllegalStateException("This job is finished");
		
		StringBuilder sb = new StringBuilder();
		int task = askedTasks.nextClearBit(0);
		tasksLeftToAsk --;
		askedTasks.set(task);
		
		sb.append('{').append("\"JobId\": \"").append(id).append("\",")
					  .append("\"WorkerVersion\": \"").append(version).append("\",")
					  .append("\"WorkerUrl\": \"").append(workerUrl).append("\",")
					  .append("\"WorkerClassName\": \"").append(workerClass).append("\",")
					  .append("\"Task\": \"").append(task).append("\"}");
		

		System.out.println("Built: "+sb.toString());
		return sb.toString();
	}
	
	/**
	 * If a task has been asked and a problem occurs, re-set this task to me asked to the next client
	 * @param task
	 */
	public void setTaskToReAsk(int task) {
		if(doneTasks.get(task)) throw new IllegalStateException("This task has already been acknowledged, you can't cancel it");
		askedTasks.set(task, false);
		tasksLeftToAsk++;
	}

	public int numberOfTasks() {
		return numberOfTasks;
	}
	
	public boolean isWaitingForAckToFinish() {
		return tasksLeftToAsk == 0;
	}
	
	public boolean isFinished() {
		return tasksLeftToAck == 0;
	}

	public int getPriority() {
		return priority;
	}

}
