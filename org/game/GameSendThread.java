package org.game;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

import org.kernel.*;

public class GameSendThread implements Runnable {
	private PrintWriter _out;
	private boolean isRunning = false;
	private Thread _t;
	@SuppressWarnings("unused")
	private Socket _s;
	private ArrayList<String> _packets = new ArrayList<String>();

	public GameSendThread(GameThread g, Socket s, PrintWriter out) {
		_out = out;
		_s = s;
		isRunning = true;
		_t = new Thread(Main.THREAD_GAME_SEND, this);
		_t.setDaemon(true);
		try {
			_t.start();
		} catch (OutOfMemoryError e) {
			Logs.addToDebug("OutOfMemory dans le GameSend");
			e.printStackTrace();
			try {
				Main.listThreads(true);
			} catch (Exception ea) {
			}
			try {
				_t.start();
			} catch (OutOfMemoryError e1) {
			}
		}
	}

	public void send(String packet) {
		if (packet.equals("") || packet.equals("" + (char) 0x00))
			return;
		synchronized (_packets) {
			_packets.add(packet);
		}

		if (_t.getState() == Thread.State.WAITING || _t.getState() == Thread.State.TIMED_WAITING) {
			synchronized (_t) {
				_t.notify();
			}
		}
	}

	public void close() {
		if (!isRunning)
			return;
		isRunning = false;
		if (_t.getState() == Thread.State.WAITING || _t.getState() == Thread.State.TIMED_WAITING) {
			synchronized (_t) {
				_t.notify();
			}
		}
	}

	public void run() {
		ArrayList<String> packets = new ArrayList<String>();
		try {
			while (isRunning) {
				try {
					synchronized (_t) {
						_t.wait(2500);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if (!isRunning)
					break;
				synchronized (_packets) {
					packets.clear();
					packets.addAll(_packets);
					_packets.clear();
				}
				while (packets.size() != 0) {
					for (String p : packets) {
						if (_out == null)
							break;
						_out.print((p) + (char) 0x00);
						_out.flush();
						if (!isRunning)
							break;
					}
					synchronized (_packets) {
						packets.clear();
						packets.addAll(_packets);
						_packets.clear();
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		_out.close();
		_out = null;
	}

}
