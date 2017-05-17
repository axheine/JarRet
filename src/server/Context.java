package server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

abstract class Context {
	private boolean inputClosed = false;
	private final ByteBuffer in;
	private final ByteBuffer out;
	private final SelectionKey key;
	final SocketChannel sc;

	public Context(SelectionKey key, int BUFF_SIZE) {
		this.key = key;
		this.sc = (SocketChannel) key.channel();
		in = ByteBuffer.allocate(BUFF_SIZE);
		out = ByteBuffer.allocate(BUFF_SIZE);
	}

	public void doRead() throws IOException {
		if (sc.read(in) == -1) {
			inputClosed = true;
		}
		process();
		updateInterestOps();
	}

	public void doWrite() throws IOException {
		out.flip();
		sc.write(out);
		out.compact();
		process();
		updateInterestOps();
	}

	abstract void process();
	
	private void updateInterestOps() {
    	int ops = 0;
	    if(out.position() != 0) {
	    	ops = ops | SelectionKey.OP_WRITE;
	    }
    	if(in.hasRemaining() && !inputClosed) {
    		ops = ops | SelectionKey.OP_READ;
    	}
    	if(ops == 0) {
    		Server.silentlyClose(sc);
    	}
    	
    	key.interestOps(ops);
	}

	public String getClientAdress() {
		String cl = "";
		try {
			cl = sc.getRemoteAddress().toString();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return cl;
	}
}