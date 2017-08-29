package org.kernel;

import org.common.Constant;
import org.common.SQLManager;
import org.common.SocketManager;
import org.common.World;
import org.game.GameServer;
import org.kernel.Console.Color;
import org.login.server.LoginServer;

public class Main {

	// Thread Groups
	public static ThreadGroup THREAD_GAME = null;
	public static ThreadGroup THREAD_GAME_SEND = null;
	public static ThreadGroup THREAD_REALM = null;
	public static ThreadGroup THREAD_IA = null;
	public static ThreadGroup THREAD_SAVE = null;

	// System
	public static boolean isRunning = false;

	public static GameServer gameServer;
	public static LoginServer loginServer;

	static {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				Reboot.start();
			}
		});

		THREAD_GAME   	 = new ThreadGroup(Thread.currentThread().getThreadGroup(), "GameThread");
		THREAD_REALM 	 = new ThreadGroup(Thread.currentThread().getThreadGroup(), "RealmThread");
		THREAD_IA 		 = new ThreadGroup(Thread.currentThread().getThreadGroup(), "IaThread");
		THREAD_SAVE 	 = new ThreadGroup(Thread.currentThread().getThreadGroup(), "SaveThread");
		THREAD_GAME_SEND = new ThreadGroup(Thread.currentThread().getThreadGroup(), "GameThreadSend");
	}
	
	public static void header() {
		Console.println("Rubrum Solem for Dofus 1.29.1", Color.WHITE);
		Console.println("Version : " + Constant.EMU_VERSION, Color.WHITE);
		Console.println("Thanks to : Starlight, WalakaZ, Return, Deathdown, Diabu\n", Color.WHITE);
		Console.println("-------------------------------------------------------------------------------\n",Color.WHITE);
	}
	
	public static void main(String[] arg) {
		Console.setTitle("Loading emulator...");
		Console.clear();
		header();

		//load config
		Console.println("Loading Configuration : " + Config.load(), Color.MAGENTA);
		//create logs folder
		Console.println("Creating logs folders : " + Logs.createLog(), Color.MAGENTA);
		//load database
		if (SQLManager.setUpConnexion()) {
			Console.println("Connection to the database : Ok", Color.MAGENTA);
		} else {
			System.exit(0);
		}

		// Chargement de la base de donnée
		World.createWorld();

		isRunning = true;
		
		loginServer = new LoginServer();
		loginServer.start();
		
		gameServer = new GameServer(Config.IP);
		
		Console.bright();
		Console.println("\nEmulator ready!", Color.GREEN);
		
		try {
			Thread.sleep(2000);
		} catch (InterruptedException ex) {
			// Ne devrait pas arriver
			Console.print(ex.getMessage(), Color.RED);
		}
		
		Clear();
		Console.setTitle("Emulator ready, waiting for connection...");
		new ConsoleInputAnalyzer();
		System.out.print("Command > ");	
	}

	public static void Clear() {
		Console.clear();
		header();
	}

	public static void listThreads(boolean isError) throws Exception {
		Console.println("\nListage des threads", Color.YELLOW);

		if (isError)
			SocketManager.GAME_SEND_cMK_PACKET_TO_ADMIN("@", 0, "Thread", "La RAM est surchargée !");

		try {
			Console.println(gameServer.getPlayerNumber() + " player online", Color.GREEN);
			ThreadGroup threadg = Thread.currentThread().getThreadGroup();

			if (!Thread.currentThread().getThreadGroup().getName().equalsIgnoreCase("main"))
				threadg = threadg.getParent();

			Console.println(threadg.activeCount() + " thread active", Color.YELLOW);
			Thread[] threads = new Thread[threadg.activeCount()];
			threadg.enumerate(threads);

			for (Thread t : threads)
				if (!isError)
					Console.println(t.getThreadGroup().getName() + " " + t.getName() + " (" + t.getState() + ") => "
							+ t.getId(), Color.YELLOW);
		} catch (Exception e) {
			Console.println("listing error" + e.getMessage(), Color.RED);
		}

		if (isError) {
			try {
				Console.println(THREAD_IA.activeCount() + " threads IA deleted", Color.YELLOW);
				Thread[] threadd = new Thread[THREAD_IA.activeCount()];
				THREAD_IA.enumerate(threadd);

				for (Thread t : threadd)
					t.interrupt();

				Console.println(THREAD_IA.activeCount() + " threads IA remaining", Color.GREEN);
			} catch (Exception e) {
				e.printStackTrace();
				SocketManager.GAME_SEND_cMK_PACKET_TO_ADMIN("@", 0, "Thread", "Suppression impossible");
			}
			try {
				Console.println("Attempt to purge the ram", Color.YELLOW);
				Runtime r = Runtime.getRuntime();
				r.runFinalization();
				r.gc();
				System.gc();
				Console.println("Ram purged", Color.GREEN);
				gameServer.restartGameServer();
			} catch (Exception e) {
				Console.println("impossible to purge the ram", Color.RED);
			}
		}
	}
}
