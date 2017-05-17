package server;

import java.nio.channels.SelectionKey;

public class TaskContext extends Context {

	public TaskContext(SelectionKey key, int BUFF_SIZE) {
		super(key, BUFF_SIZE);
	}

	@Override
	void process() {
		// TODO Auto-generated method stub
		
	}
	
}
