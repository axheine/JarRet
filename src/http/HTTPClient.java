package http;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedList;

import http.HTTPHeader;

public class HTTPClient {
	private final Charset ASCII_CHARSET = Charset.forName("ASCII");

	private final int BUFFER_SIZE = 4096;
	private SocketChannel sc;
	private final ByteBuffer buff;
	private final byte CR = 13;
	private final byte LF = 10;

	public HTTPClient() {
		this.buff = ByteBuffer.allocate(BUFFER_SIZE);
	}

	public HTTPResponse sendGetQuery(InetSocketAddress address, String query) throws IOException {
		buff.clear();
		this.sc = SocketChannel.open();
		sc.connect(address);
		sc.write(ASCII_CHARSET.encode(query));

		HTTPHeader header = readHeader();
		String body = null;

		if (header.getContentLength() > 0) {
			ByteBuffer content;

			if (!header.isChunkedTransfer())
				content = readBytes(header.getContentLength());
			else
				content = readChunks();
			content.flip();

			Charset bodyCharset = header.getCharset();
			body = bodyCharset.decode(content).toString();
		}
		
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
		boolean foundCR = false;
		StringBuilder sb = new StringBuilder();

		buff.flip(); // on passe en lecture
		while (true) {
			if (!buff.hasRemaining()) {
				buff.compact(); // écriture
				int conn = sc.read(buff);
				if (conn == -1) {
					throw new HTTPException("Connection was closed while reading");
				}
				buff.flip(); // lecture
			}

			byte c = buff.get();

			if (c == CR)
				foundCR = true;
			else if (c == LF && foundCR)
				break;
			else
				foundCR = false;

			// System.out.println("Char "+((char) c));
			sb.append((char) c);
		}

		buff.compact(); // on le repasse en écriture à la fin
		sb.setLength(sb.length() - 1);
		return sb.toString();
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
		while ((line = readLineCRLF()).length() != 0) {
			int splitterIndex = line.indexOf(':');
			if (splitterIndex == -1) {
				String[] tokens = line.split(" ");
				status = tokens[0] + " " + tokens[1];
			} else {
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

		buff.compact();
		if (buff.remaining() < size) {
			content.put(buff);
			buff.clear();
		} else {
			int oldLimit = buff.limit();
			buff.limit(size);
			content.put(buff);
			buff.limit(oldLimit);
			buff.compact();
		}
		if (!readFully(content, sc)) {
			throw new HTTPException("Connection closed while reading");
		}

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
		while (bb.hasRemaining()) {
			if (sc.read(bb) == -1) {
				return false;
			}
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
