package org.game;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.check.IpCheck;
import org.client.Accounts;
import org.client.Characters;
import org.common.CryptManager;
import org.common.Formulas;
import org.common.SQLManager;
import org.common.SocketManager;
import org.common.World;
import org.kernel.*;
import org.simplyfilter.filter.Filter;
import org.simplyfilter.filter.Filters;

public class GameServer implements Runnable {

	private ServerSocket _SS;
	private Thread _t;
	private ArrayList<GameThread> _clients = new ArrayList<GameThread>();
	private ArrayList<Accounts> _waitings = new ArrayList<Accounts>();
	private Timer _saveTimer;
	// private Timer _loadActionTimer;
	private Timer _reloadMobTimer;
	private long _startTime;
	private int _maxPlayer = 0;
	private int _maxPlayerUnique = 0;
	private Timer _loadPubTimer;
	private Timer _loadActionTimer;
	private Timer _loadMountTrash;
	private Timer _Inactivity;
	private Timer _reboot;
	private int _rebootTime = Config.CONFIG_REBOOT_TIME; // hour
	public static boolean isFirstLoad = false;
	public static Map<String, Long> lastIpTiming = new HashMap<String, Long>();
	public static String ip;

	public GameServer(String Ip) {
		ip = Ip;
		try {
			_saveTimer = new Timer("SaveTimer");
			_saveTimer.schedule(new TimerTask() {
				public void run() {
					World.saveAll(null);
				}
			}, Config.CONFIG_SAVE_TIME, Config.CONFIG_SAVE_TIME);
			
			_Inactivity = new Timer("Inactivity");
			_Inactivity.schedule(new TimerTask() {
				public void run() {
					Logs.addToGameSockLog("Lancement du kick pour inactivite");
					for (Characters c : World.getOnlinePersos()) {
						try {
							if (c == null || c.get_compte() == null || c.get_compte().getGameThread() == null)
								continue;
							long minute_inactive =  c.getLastPacketTime() + Config.CONFIG_LOAD_KICK;
							if (minute_inactive < System.currentTimeMillis()) {
								SocketManager.REALM_SEND_MESSAGE(c.get_compte().getGameThread().get_out(), "01|");
								c.get_compte().getGameThread().kick();
							}
							
						} catch (Exception e) {
							Logs.addToGameLog("Erreur : " + e.getMessage());
						}
					}
					Logs.addToGameSockLog("Kick pour inactivite termine");
				}
			}, Config.CONFIG_LOAD_KICK, Config.CONFIG_LOAD_KICK);

			//Système de pub : Dysta
			if (Config.CONFIG_PUB) {
				_loadPubTimer = new Timer("TimerPub");
				_loadPubTimer.schedule(new TimerTask() {
					public void run() {
						if(World.getOnlinePersos().size() > 0){
							int rand = Formulas.getRandomValue(1, 3);
							switch (rand) {
							case 1:
								SocketManager.GAME_SEND_PUB(Config.PUB1.toString());
								break;
							case 2:
								SocketManager.GAME_SEND_PUB(Config.PUB2.toString());
								break;
							case 3:
								SocketManager.GAME_SEND_PUB(Config.PUB3.toString());
								break;
							}
						}
					}
				}, Config.CONFIG_LOAD_PUB, Config.CONFIG_LOAD_PUB);
			}
			
			//Systéme de pub : Taparisse
			/*
			if (Config.CONFIG_PUB) {
				_loadPubTimer = new Timer("TimerPub");
				_loadPubTimer.schedule(new TimerTask() {
					public void run() {
						int rand = Formulas.getRandomValue(1, 3);
						switch (rand) {
						case 1:
							SocketManager.GAME_SEND_MESSAGE_TO_ALL(Config.PUB1, Config.CONFIG_COLOR_BLEU);
							break;
						case 2:
							SocketManager.GAME_SEND_MESSAGE_TO_ALL(Config.PUB2, Config.CONFIG_COLOR_BLEU);
							break;
						case 3:
							SocketManager.GAME_SEND_MESSAGE_TO_ALL(Config.PUB3, Config.CONFIG_COLOR_BLEU);
							break;
						}
					}
				}, Config.CONFIG_LOAD_PUB, Config.CONFIG_LOAD_PUB);
			}
			_reboot = new Timer("TimerReboot");
			_reboot.schedule(new TimerTask() {
				public void run() {
					_rebootTime -= 3600000;
					int timeLeft = (_rebootTime / 1000) / 3600 ;
					int time = 30;
					if (_rebootTime <= 0)
						Reboot.start();
					else {
						SocketManager.GAME_SEND_Im_PACKET_TO_ALL("115;" + timeLeft + (timeLeft > 1 ? "heures" : "heure"));
							if (_rebootTime - System.currentTimeMillis() == time) {
							   time = (time - 5);
							}
					}
				}
			}, 3600000, 3600000);
			*/
			_reboot = new Timer("TimerReboot");
			_reboot.schedule(new TimerTask() {
				public void run() {
					_rebootTime --;
					if (_rebootTime <= 0)
						Reboot.start();
					else
						SocketManager.GAME_SEND_Im_PACKET_TO_ALL("115;" + _rebootTime + (_rebootTime > 1 ? "heures" : "heure"));
				}
			}, 3600000, 3600000);
			
			if (Config.USE_LIVE_ACTION) {
				_loadActionTimer = new Timer("TimerLiveActions");
				_loadActionTimer.schedule(new TimerTask() {
					public void run() {
						// On évite d'enclencher les lives actions si personne est co
						if(World.getOnlinePersos().size() > 0)
							SQLManager.LOAD_ACTION();
					}
				}, Config.CONFIG_LOAD_DELAY, Config.CONFIG_LOAD_DELAY);
			}

			_reloadMobTimer = new Timer("TimerReloadMobs");
			_reloadMobTimer.schedule(new TimerTask() {
				public void run() {
					World.RefreshAllMob();
					Logs.addToGameLog("La recharge des mobs est finie");
				}
			}, Config.CONFIG_RELOAD_MOB_DELAY, Config.CONFIG_RELOAD_MOB_DELAY);

			_loadMountTrash = new Timer("TimerMountTrash");
			_loadMountTrash.schedule(new TimerTask() {
				public void run() {
					SQLManager.RESET_MOUNTPARKS();
					SQLManager.LOAD_MOUNTPARKS();
					Logs.addToGameLog("\nSupression montures enclos map 8747");
				}
			}, Config.CONFIG_RELOAD_MOUNT_DELAY, Config.CONFIG_RELOAD_MOUNT_DELAY);

			java.util.Timer timer = new java.util.Timer();
			timer.schedule(new TimerTask() {
				public void run() {
					World.MoveMobsOnMaps();
				}
			}, Formulas.getRandomValue(10000, 30000), Formulas.getRandomValue(120000, 360000));

			_SS = new ServerSocket(Config.CONFIG_GAME_PORT);
			if (Config.CONFIG_USE_IP)
				Config.GAMESERVER_IP = CryptManager.CryptIP(Ip) + CryptManager.CryptPort(Config.CONFIG_GAME_PORT);
			_startTime = System.currentTimeMillis();
			_t = new Thread(this, "GameServer");
			_t.start();
		} catch (IOException e) {
			Logs.addToGameLog("IOException: " + e.getMessage());
			System.exit(0);
		}
		Logs.addToGameLog("Game server started on port " + Config.CONFIG_GAME_PORT + " with ip " + Ip);
	}

	public void restartGameServer() {
		if (!_t.isAlive()) {
			Logs.addToDebug("GameServer plante, tentative de redemarrage.");
			_t.start();
		}
	}

	public static class SaveThread implements Runnable {
		public void run() {
			if (!Config.isSaving) {
				World.saveAll(null);
			}
		}
	}

	public void stop() {
		this.stop();
	}

	public ArrayList<GameThread> getClients() {
		return _clients;
	}

	public long getStartTime() {
		return _startTime;
	}

	public int getMaxPlayer() {
		return _maxPlayer;
	}

	public int getPlayerNumber() {
		return _clients.size();
	}
	
	public int getClientUnique(){
		int nbr = 0;
		ArrayList<String> ip = new ArrayList<String>();
		for (GameThread c : _clients) {
			if (c == null || c.getAccount() == null)
				continue;
			if (!ip.contains(c.getAccount().get_curIP())) {
				ip.add(c.getAccount().get_curIP());
				nbr++;
			}
		}
		if (nbr > _maxPlayerUnique)
			_maxPlayerUnique = nbr;
		return nbr;
	}

	public void run() {
		Filter filter = Filters.createNewUnSafe(3, 500); //3 connections authorizes in 0.5s
		while (Main.isRunning)// bloque sur _SS.accept()
		{
			try {
				Socket _s = _SS.accept();
				 
				if (!IpCheck.canGameConnect(_s.getInetAddress().getHostAddress())
						|| !filter.authorize(_s.getInetAddress().getHostAddress())) {
					_s.close();
				} else {
					_clients.add(new GameThread(_s));
					if (_clients.size() > _maxPlayer)
						_maxPlayer = _clients.size();
				}
			} catch (IOException e) {
				Logs.addToDebug("IOException Game-run: " + e.getMessage());
				e.printStackTrace();
				if (_SS.isClosed()) {
					Logs.addToDebug("Le GameServer est HS, redemarage oblige.");
					System.exit(0);
				}
			}
		}
	}

	public void kickAll() throws Exception {
		try {
			_SS.close();
		} catch (IOException e) {
		}
		// Copie
		ArrayList<GameThread> c = new ArrayList<GameThread>();
		c.addAll(_clients);
		for (GameThread GT : c) {
			try {
				GT.closeSocket();
			} catch (Exception e) {
				Logs.addToDebug("Erreur kickAll");
			}
		}
	}

	public void delClient(GameThread gameThread) {
		_clients.remove(gameThread);
		if (_clients.size() > _maxPlayer)
			_maxPlayer = _clients.size();
	}

	public synchronized Accounts getWaitingCompte(int guid) {
		for (int i = 0; i < _waitings.size(); i++) {
			if (_waitings.get(i).get_GUID() == guid)
				return _waitings.get(i);
		}
		return null;
	}

	public synchronized void delWaitingCompte(Accounts _compte) {
		_waitings.remove(_compte);
	}

	public synchronized void addWaitingCompte(Accounts _compte) {
		_waitings.add(_compte);
	}

	public static String getServerTime() {
		Date actDate = new Date();
		return "BT" + (actDate.getTime() + 3600000);
	}

	public static String getServerDate() {
		Date date = new Date();
		DateFormat dateFormat = new SimpleDateFormat("dd");

		String day = Integer.parseInt(dateFormat.format(date)) + "";

		while (day.length() < 2)
			day = "0" + day;

		dateFormat = new SimpleDateFormat("MM");
		String mounth = (Integer.parseInt(dateFormat.format(date)) - 1) + "";

		while (mounth.length() < 2)
			mounth = "0" + mounth;

		dateFormat = new SimpleDateFormat("yyyy");
		String year = (Integer.parseInt(dateFormat.format(date)) - 1370) + "";
		return "BD" + year + "|" + mounth + "|" + day;
	}

	public Thread getThread() {
		return _t;
	}
}
