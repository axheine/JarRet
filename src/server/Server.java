package server;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;

public class Server {

	static final int BUFF_SIZE = 512;
	private final ServerSocketChannel serverSocketChannel;
	private final Selector selector;
	private final Set<SelectionKey> selectedKeys;
	private static final long TIMEOUT = 2000;
	private Queue<String> queue;

	public Server(int port) throws IOException {
		serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.bind(new InetSocketAddress(port));
		selector = Selector.open();
		selectedKeys = selector.selectedKeys();
		queue = new ArrayBlockingQueue<>(5);
	}

	public void launch() throws IOException {
		serverSocketChannel.configureBlocking(false);
		serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		Set<SelectionKey> selectedKeys = selector.selectedKeys();
		while (!Thread.interrupted()) {
			printKeys();
			System.out.println("Starting select");

			selector.select(TIMEOUT / 10);
			if (processCommand()) {
				return;
			}
			System.out.println("Select finished");
			printSelectedKey();
			processSelectedKeys();

			selectedKeys.clear();
		}
	}

	private void processSelectedKeys() throws IOException {
		for (SelectionKey key : selectedKeys) {

			if (key.isValid() && key.isAcceptable()) {
				doAccept(key);
			}
			try {
				Context cntxt = (Context) key.attachment();
				if (key.isValid() && key.isWritable()) {
					cntxt.doWrite();
				}
				if (key.isValid() && key.isReadable()) {
					cntxt.doRead();
				}

			} catch (IOException e) {
				silentlyClose(key.channel());
			}
		}
	}

	private void doAccept(SelectionKey key) throws IOException {
		SocketChannel sc = serverSocketChannel.accept();
		sc.configureBlocking(false);
		SelectionKey clientKey = sc.register(selector, SelectionKey.OP_READ);
		clientKey.attach(new TaskContext(clientKey, BUFF_SIZE));
	}

	static void silentlyClose(SelectableChannel sc) {
		if (sc == null)
			return;
		try {
			sc.close();
		} catch (IOException e) {
			// silently ignore
		}
	}

	private static void usage() {
		System.out.println("ServerSumNew <listeningPort>");
	}

	public static void main(String[] args) throws NumberFormatException, IOException {
		if (args.length != 1) {
			usage();
			return;
		}
		Server server = new Server(Integer.parseInt(args[0]));
		server.startCommandListener(System.in);
		server.launch();
	}

	private void shutdown() {
		try {
			serverSocketChannel.close();
		} catch (IOException e) {
			// DO NOTHING
		}
	}

	private boolean processCommand() {
		while (!queue.isEmpty()) {
			String command = queue.poll();
			switch (command) {
			case "HALT":
				shutdownNow();
				return true;
			case "STOP":
				shutdown();
				return true;
			case "FLUSH":
				flush();
				break;
			case "SHOW":
				show();
				break;
			default:
				System.err.println("Bad request");
			}
		}
		return false;
	}

	private void startCommandListener(InputStream in) {
		Thread t = new Thread(() -> {
			try (Scanner sc = new Scanner(in)) {
				while (sc.hasNextLine()) {
					String line = sc.nextLine();
					switch (line) {
					case "HALT":

					case "STOP":
						queue.add(line);
						selector.wakeup();
						return;
					case "FLUSH":

					case "SHOW":
						queue.add(line);
						selector.wakeup();
						break;
					default:
						System.err.println("Bad request");
					}
				}
			}
		});
		t.start();
	}

	private void shutdownNow() {
		shutdown();
		flush();
	}

	private void flush() {
		for (SelectionKey key : selectedKeys) {
			Object attachment = key.attachment();
			if (attachment != null) {
				try {
					((Context) attachment).sc.close();
				} catch (IOException e) {
					// DO NOTHING
				}
				;
			}
		}
	}

	private void show() {
		int nbClient = 0;
		System.out.println("List of address : ");
		for (SelectionKey key : selectedKeys) {
			Object attachment = key.attachment();
			if (attachment != null) {
				nbClient++;
				System.out.println(((Context) attachment).getClientAdress());
			}
		}
		System.out.println(nbClient + " clients connected");

	}

	/***
	 * Theses methods are here to help understanding the behavior of the
	 * selector
	 ***/

	private String interestOpsToString(SelectionKey key) {
		if (!key.isValid()) {
			return "CANCELLED";
		}
		int interestOps = key.interestOps();
		ArrayList<String> list = new ArrayList<>();
		if ((interestOps & SelectionKey.OP_ACCEPT) != 0)
			list.add("OP_ACCEPT");
		if ((interestOps & SelectionKey.OP_READ) != 0)
			list.add("OP_READ");
		if ((interestOps & SelectionKey.OP_WRITE) != 0)
			list.add("OP_WRITE");
		return String.join("|", list);
	}

	public void printKeys() {
		Set<SelectionKey> selectionKeySet = selector.keys();
		if (selectionKeySet.isEmpty()) {
			System.out.println("The selector contains no key : this should not happen!");
			return;
		}
		System.out.println("The selector contains:");
		for (SelectionKey key : selectionKeySet) {
			SelectableChannel channel = key.channel();
			if (channel instanceof ServerSocketChannel) {
				System.out.println("\tKey for ServerSocketChannel : " + interestOpsToString(key));
			} else {
				SocketChannel sc = (SocketChannel) channel;
				System.out.println("\tKey for Client " + remoteAddressToString(sc) + " : " + interestOpsToString(key));
			}
		}
	}

	private String remoteAddressToString(SocketChannel sc) {
		try {
			return sc.getRemoteAddress().toString();
		} catch (IOException e) {
			return "???";
		}
	}

	private void printSelectedKey() {
		if (selectedKeys.isEmpty()) {
			System.out.println("There were not selected keys.");
			return;
		}
		System.out.println("The selected keys are :");
		for (SelectionKey key : selectedKeys) {
			SelectableChannel channel = key.channel();
			if (channel instanceof ServerSocketChannel) {
				System.out.println("\tServerSocketChannel can perform : " + possibleActionsToString(key));
			} else {
				SocketChannel sc = (SocketChannel) channel;
				System.out.println(
						"\tClient " + remoteAddressToString(sc) + " can perform : " + possibleActionsToString(key));
			}

		}
	}

	private String possibleActionsToString(SelectionKey key) {
		if (!key.isValid()) {
			return "CANCELLED";
		}
		ArrayList<String> list = new ArrayList<>();
		if (key.isAcceptable())
			list.add("ACCEPT");
		if (key.isReadable())
			list.add("READ");
		if (key.isWritable())
			list.add("WRITE");
		return String.join(" and ", list);
	}

}
