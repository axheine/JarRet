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
    	System.out.println("out : " + out);
    	System.out.println("in :  " + in);
    	System.out.println("in remaining : " + in.remaining());
    	System.out.println("input : " + inputClosed);
	    if(out.position() != 0) {
	    	ops = ops | SelectionKey.OP_WRITE;
	    }
    	if(in.remaining() > 0 && !inputClosed) {
    		ops = ops | SelectionKey.OP_READ;
    	}
    	if(ops == 0) {
    		Server.silentlyClose(sc);
    	}else {
    		key.interestOps(ops); //TODO: ça renvoie des exceptions
    	}
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
	
	public ByteBuffer getIn() {
		return this.in;
	}
	
	public ByteBuffer getOut() {
		return this.out;
	}
}