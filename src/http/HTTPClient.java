package http;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedList;

public class HTTPClient {
	private final Charset ASCII_CHARSET = Charset.forName("ASCII");

	private static final int BUFFER_SIZE = 4096;
	private SocketChannel sc;
	private ByteBuffer buff;

	public HTTPClient() {
		this.buff = ByteBuffer.allocate(BUFFER_SIZE);
	}

	public HTTPResponse sendQuery(InetSocketAddress address, String query) throws IOException {
		buff.clear();
		this.sc = SocketChannel.open();
		sc.connect(address);
		sc.write(ASCII_CHARSET.encode(query));

		HTTPHeader header = readHeader();

		String body = null;

		if (header.getContentLength() > 0) {
			//System.out.println("Body length: " + header.getContentLength());
			ByteBuffer content;

			if (!header.isChunkedTransfer())
				content = readBytes(header.getContentLength());
			else
				content = readChunks();
			content.flip();

			Charset bodyCharset = header.getCharset();
			//System.out.println("Charset: "+bodyCharset);
			body = bodyCharset.decode(content).toString();
		}
		
		//System.out.println("Body: \n"+body);
		return new HTTPResponse(header, body);
	}

	/**
	 * @return The ASCII string terminated by CRLF
	 *         <p>
	 *         The method assume that buff is in write mode and leave it in
	 *         write-mode The method never reads from the socket as long as the
	 *         buffer is not empty
	 * @throws IOException
	 *             HTTPException if the connection is closed before a line could
	 *             be read
	 */
	public String readLineCRLF() throws IOException {
		StringBuilder sb = new StringBuilder();
		boolean lastCR = false;

		while (true) {
			//System.out.println("Beginning of CRLF: "+buff);
			buff.flip(); // read mode
			
			while (buff.hasRemaining()) {
				char o = (char) buff.get();
				sb.append(o);
				if (o == '\n' && lastCR) {
					buff.compact();
					sb.setLength(sb.length() - 2);
					//System.out.println("End of CRLF: "+buff);
					return sb.toString();
				}
				lastCR = (o == '\r');
			}
			buff.clear();
			if (sc.read(buff) == -1) {
				throw new HTTPException("Wrong Protocol");
			}
		}
	}

	/**
	 * @return The HTTPHeader object corresponding to the header read
	 * @throws IOException
	 *             HTTPException if the connection is closed before a header
	 *             could be read if the header is ill-formed
	 */
	public HTTPHeader readHeader() throws IOException {
		HashMap<String, String> properties = new HashMap<>();
		String line = "";
		String status = "";
		//System.out.println("Buffer: "+buff);
		while ((line = readLineCRLF()).length() != 0) {
			//System.out.println("Buffer: "+buff);
			
			int splitterIndex = line.indexOf(':');
			if (splitterIndex == -1) {
				String[] tokens = line.split(" ");
				status = tokens[0] + " " + tokens[1];
			} else {
				//System.out.println("Header line: "+line.substring(0, splitterIndex) +" / "+ line.substring(splitterIndex + 2));
				properties.put(line.substring(0, splitterIndex), line.substring(splitterIndex + 2));
			}
		}

		if (status.equals(""))
			throw new HTTPException("Malformed answer header (couldn't read answer status)");
		
		
		return HTTPHeader.create(status, properties);
	}

	/**
	 * @param size
	 * @return a ByteBuffer in write-mode containing size bytes read on the
	 *         socket
	 * @throws IOException
	 *             HTTPException is the connection is closed before all bytes
	 *             could be read
	 */
	public ByteBuffer readBytes(int size) throws IOException {
		ByteBuffer content = ByteBuffer.allocate(size);

		buff.flip(); // lecture
		if (buff.remaining() < size) {
			//System.out.println("There's a little to get from buff buffer first.");
			content.put(buff);
			if (!readFully(content, sc)) {
				throw new HTTPException("Connection closed while reading");
			}
		} else {
			//System.out.println("All can be read from buff buffer.");
			int oldLimit = buff.limit();
			buff.limit(size);
			content.put(buff);
			buff.limit(oldLimit);
		}
		buff.compact();
		//System.out.println("Content: " + content);
		return content;
	}

	/**
	 * @return a ByteBuffer in write-mode containing a content read in chunks
	 *         mode
	 * @throws IOException
	 *             HTTPException if the connection is closed before the end of
	 *             the chunks if chunks are ill-formed
	 */

	public ByteBuffer readChunks() throws IOException {
		LinkedList<ByteBuffer> chunks = new LinkedList<>();

		while (true) {
			int chunkSize = Integer.parseInt(readLineCRLF(), 16);
			if (chunkSize == 0)
				break;

			ByteBuffer bb = readBytes(chunkSize);
			bb.flip();
			chunks.add(bb);

			readLineCRLF();
		}

		int totalSize = chunks.stream().mapToInt((bb) -> bb.remaining()).sum();
		ByteBuffer total = ByteBuffer.allocate(totalSize);
		chunks.stream().forEach((bb) -> total.put(bb));

		return total;
	}

	public static boolean readFully(ByteBuffer bb, SocketChannel sc) throws IOException {
		//System.out.println(bb.remaining());
		while (bb.hasRemaining()) {
			//System.out.println("before read bytes" + bb);
			if (sc.read(bb) == -1) {
				return false;
			}
			//System.out.println("read bytes");
		}
		return true;
	}

	public static boolean writeFully(ByteBuffer bb, SocketChannel sc) throws IOException {
		while (bb.hasRemaining()) {
			if (sc.write(bb) == -1) {
				return false;
			}
		}
		return true;
	}
}
