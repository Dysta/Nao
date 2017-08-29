package org.check;

import java.io.IOException;
import java.util.Calendar;
import java.util.Map;
import java.util.TreeMap;

import org.kernel.Logs;


public class IpCheck {
	//Configuration de sécurité
	//Realm
	private final static int REALM_REFUSE_LIMIT = 3;//On autorise une tentative toutes les 5 secondes
	private final static int REALM_DELAY = 60;//On remet le compteur à 0 au bout d'une minute d'inactivité
	private final static int REALM_OVER_BAN = 20;//Si on fait plus de X connexions forcées à la suite on ban
	//Game
	private final static int GAME_REFUSE_LIMIT = 2;//On autorise une tentative toutes les 5 secondes
	private final static int GAME_DELAY = 60;//On remet le compteur à 0 au bout d'une minute d'inactivité
	private final static int GAME_OVER_BAN = 10;//Si on fait plus de X connexions forcées à la suite on ban
	
	
	//Surverillance du nombre de packets
	private final static int GAME_PACKET_PER_SECOND = 20;//On autorise au maximum X packets par ip par secondes
	private final static int GAME_PACKET_VERIF = 3;//On fait notre vérification sur X secondes
	private final static int GAME_PACKET_OVER_BAN = 10;//Au bout de X ban pour packets on banip
	
	private final static String[] GAME_PACKET_TO_IGNORE = {"BD","GT"};
	
	//Nombre de connexions simultanées max/ip
	private final static int GAME_MAX_CONNECTION = 16;
	private final static int REALM_MAX_CONNECTION = 16;

	private static Map<Integer,IpCheck.IpInstance> _instances = new TreeMap<Integer, IpCheck.IpInstance>();
	private static int index = 0;
	
	
	public static class IpInstance
	{
		private String _ip;
		private boolean _banned = false;
		//Connexion au org.walaka.rubrumsolem.realm
		private long _Realm_last;
		private int _Realm_over;
		//Connexionx au org.walaka.rubrumsolem.game server
		private long _Game_last;
		private int _Game_over;
		//Pakcet par seconde sur le org.walaka.rubrumsolem.game server
		private long _PacketGame_ref = 0;
		private int _PacketGame_count = 0;
		private int _PacketGame_over = 0;
		//Nombre de connexions actives
		private int _Game_connexions = 0;
		private int _Realm_connexions = 0;
		
		public IpInstance(String ip)
		{
			_ip = ip;
			_Realm_over=0;
			_Realm_last=0;
			_Realm_connexions = 0;
			_Game_last=0;
			_Game_over=0;
			_Game_connexions = 0;
		}
		public String getIp()
		{
			return _ip;
		}
		public void newGameConnection()
		{
			_Game_connexions++;
		}
		public void delGameConnection()
		{
			_Game_connexions--;
		}
		public void newRealmConnection()
		{
			_Realm_connexions++;
		}
		public void delRealmConnection()
		{
			_Realm_connexions--;
		}
		public boolean newRealMConnexion()
		{
			if(_ip == "127.0.0.1") return true;
			if(_banned)
			{
				IpCheck.addToLog("RM Ip bannie : "+_ip);
				return false;
			}
			if(_Realm_connexions >= REALM_MAX_CONNECTION)
			{
				IpCheck.addToLog("Realm plus de "+REALM_MAX_CONNECTION+" connexions simultanées : "+_ip);
				return false;
			}
			boolean r = true;
			long cur_t = (long) System.currentTimeMillis()/1000;
			if(cur_t-_Realm_last > REALM_DELAY && _Realm_over > 0)
			{
				_Realm_over=0;
				IpCheck.addToLog("RM Remise à 0 de l'ip "+_ip);
			}

			if(cur_t-_Realm_last <= REALM_REFUSE_LIMIT)
			{
				_Realm_over++;
				r = false;
				IpCheck.addToLog("RM Non respect de l'interval pour l'ip : "+_ip);
				if(_Realm_over >= REALM_OVER_BAN)
				{
					IpCheck.addToLog("RM Ban définitif pour l'ip : "+_ip);
					_banned = true;
				}
			}
			_Realm_last = cur_t;
			return r;
		}
		public boolean newGameConnexion()
		{
			if(_ip == "127.0.0.1") return true;
			if(_banned)
			{
				IpCheck.addToLog("Game Ip bannie : "+_ip);
				return false;
			}
			if(_Game_connexions >= GAME_MAX_CONNECTION)
			{
				IpCheck.addToLog("Game plus de "+GAME_MAX_CONNECTION+" connexions simultanées : "+_ip);
				return false;
			}
			boolean r = true;
			long cur_t = (long) System.currentTimeMillis()/1000;
			if(cur_t-_Game_last > GAME_DELAY && _Realm_over > 0)
			{
				_Game_over=0;
				IpCheck.addToLog("Game Remise à 0 de l'ip "+_ip);
			}

			if(cur_t-_Game_last <= GAME_REFUSE_LIMIT)
			{
				_Game_over++;
				r = false;
				IpCheck.addToLog("Game Non respect de l'interval pour l'ip : "+_ip);
				if(_Game_over >= GAME_OVER_BAN)
				{
					IpCheck.addToLog("Game Ban définitif pour l'ip : "+_ip);
					_banned = true;
				}
			}
			_Game_last = cur_t;
			return r;
		}
		public boolean newGamePacket(String packet)
		{
			if(_banned)
			{
				IpCheck.addToLog("GamePacket Ip bannie : "+_ip);
				return false;
			}
			if(IpCheck.isIgnorepacket(packet)) return true;
			boolean r = true;
			long cur_t = (long) System.currentTimeMillis()/1000;
			
			int lim = GAME_PACKET_VERIF * GAME_PACKET_PER_SECOND;
			if(cur_t - _PacketGame_ref > GAME_PACKET_VERIF)
			{
				_PacketGame_ref = cur_t;
				_PacketGame_count = 0;
			}
			
			_PacketGame_count++;
			if(_PacketGame_count >= lim)
			{
				IpCheck.addToLog("GamePacket ip au dessus de la limite : "+_ip);
				_PacketGame_over++;
				_PacketGame_count=0;
				r = false;
				if(_PacketGame_over >= GAME_PACKET_OVER_BAN)
				{
					IpCheck.addToLog("GamePacket Bannissement de l'ip : "+_ip);
					_banned = true;
				}
			}
			
			return r;
		}
	}
	
	public static boolean isIgnorepacket(String packet)
	{
		for(String p : GAME_PACKET_TO_IGNORE) if(p.equals(packet)) return true;
		return false;
	}
	
	public static IpCheck.IpInstance getInstance(String ip)
	{
		ip = ip.trim();
		if(ip.isEmpty()) return null;
		for(IpCheck.IpInstance ii : _instances.values())
		{
			if(ii.getIp().equalsIgnoreCase(ip))
			{
				return ii;
			}
		}
		return null;
	}
	public static IpCheck.IpInstance addInstance(String ip)
	{
		ip = ip.trim();
		if(ip.isEmpty()) return null;
		IpCheck.IpInstance ii = new IpInstance(ip);
		_instances.put(index, ii);
		index++;
		return ii;
	}
	public static boolean canRealMConnect(String ip)
	{
		ip = ip.trim();
		if(ip.isEmpty()) return false;
		IpCheck.IpInstance ii = getInstance(ip);
		if(ii == null) ii = addInstance(ip);
		if(ii == null)
		{
			addToLog("RM: Une erreur s'est produite lors de l'ajout de l'ip : "+ip);
			return false;
		}
		return ii.newRealMConnexion();
	}
	public static boolean canGameConnect(String ip)
	{
		ip = ip.trim();
		if(ip.isEmpty()) return false;
		IpCheck.IpInstance ii = getInstance(ip);
		if(ii == null) ii = addInstance(ip);
		if(ii == null)
		{
			addToLog("GAME: Une erreur s'est produite lors de l'ajout de l'ip : "+ip);
			return false;
		}
		return ii.newGameConnexion();
	}
	public static boolean onGamePacket(String ip, String packet)
	{
		ip = ip.trim();
		if(ip.isEmpty()) return false;
		IpCheck.IpInstance ii = getInstance(ip);
		if(ii == null) ii = addInstance(ip);
		if(ii == null)
		{
			addToLog("GamePacket: Une erreur s'est produite lors de l'ajout de l'ip : "+ip);
			return false;
		}
		return ii.newGamePacket(packet);
	}
	public static void newGameConnection(String ip)
	{
		ip = ip.trim();
		if(ip.isEmpty()) return;
		IpCheck.IpInstance ii = getInstance(ip);
		if(ii == null) ii = addInstance(ip);
		if(ii == null)
		{
			addToLog("Game: Une erreur s'est produite lors de l'ajout de l'ip : "+ip);
			return;
		}
		ii.newGameConnection();
	}
	public static void delGameConnection(String ip)
	{
		ip = ip.trim();
		if(ip.isEmpty()) return;
		IpCheck.IpInstance ii = getInstance(ip);
		if(ii == null) ii = addInstance(ip);
		if(ii == null)
		{
			addToLog("Game: Une erreur s'est produite lors de l'ajout de l'ip : "+ip);
			return;
		}
		ii.delGameConnection();
	}
	public static void newRealmConnection(String ip)
	{
		ip = ip.trim();
		if(ip.isEmpty()) return;
		IpCheck.IpInstance ii = getInstance(ip);
		if(ii == null) ii = addInstance(ip);
		if(ii == null)
		{
			addToLog("Realm: Une erreur s'est produite lors de l'ajout de l'ip : "+ip);
			return;
		}
		ii.newRealmConnection();
	}
	public static void delRealmConnection(String ip)
	{
		ip = ip.trim();
		if(ip.isEmpty()) return;
		IpCheck.IpInstance ii = getInstance(ip);
		if(ii == null) ii = addInstance(ip);
		if(ii == null)
		{
			addToLog("Realm: Une erreur s'est produite lors de l'ajout de l'ip : "+ip);
			return;
		}
		ii.delRealmConnection();
	}
	
	public static void addToLog(String data)
	{
		String date = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)+":"+Calendar.getInstance().get(+Calendar.MINUTE)+":"+Calendar.getInstance().get(Calendar.SECOND);
		try {
			Logs.Log_IpCheck.write("["+date+"]"+data);
			Logs.Log_IpCheck.newLine();
			Logs.Log_IpCheck.flush();
		} catch (IOException e) {}
	}
}
