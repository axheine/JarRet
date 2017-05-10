package http;

public class HTTPResponse {
	public final HTTPHeader header;
	public final String body;
	
	public HTTPResponse(HTTPHeader header, String body) {
		this.header = header;
		this.body = body;
	}
	
	
}
