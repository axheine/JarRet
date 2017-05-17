package worker;

public interface Worker {

	// Return a JSON String with the result of computing task number taskNumber
	public String compute(int taskNumber);

}
