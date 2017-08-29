package org.command;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

public class GmCommandManager {
	/** Author Dysta **/

	private int id;
	private String name;
	private String description;
	private int function;
	private String args;
	private int gmlvl;

	public static Map<Integer, GmCommandManager> GMcommand = new TreeMap<Integer, GmCommandManager>();
	public static Map<String, Integer> GMcommandByName = new TreeMap<String, Integer>();
	public static String GMcommandList = "";

	public GmCommandManager(int _id, String _name, int _gmlvl, int _function, String _description, String _args) {
		setId(_id);
		setName(_name);
		setGMlevel(_gmlvl);
		setFunction(_function);
		setDescription(_description);
		setArgs(_args);
	}

	public static void addCommand(GmCommandManager x) {
		GMcommand.put(x.getId(), x);
		GMcommandByName.put(x.getName().toLowerCase(), x.getId());
	}

	public static GmCommandManager getCommandById(int id) {
		return GMcommand.get(id);
	}

	public static Collection<GmCommandManager> getCommands() {
		return GMcommand.values();
	}

	public static String getCommandList(int gmlvl) {
		String toReturn = "";
		GMcommandList = "";

		for (GmCommandManager cm : getCommands()) {
			if (cm.getGMlevel() > gmlvl)
				continue;
			if (cm.getArgs() != null && cm.getDescription() != null)
				GMcommandList += "\n" + cm.getName() + " " + cm.getArgs() + "- " + cm.getDescription();
			else if (cm.getArgs() == null && cm.getDescription() != null)
				GMcommandList += "\n" + cm.getName() + " - " + cm.getDescription();
			else if (cm.getArgs() != null && cm.getDescription() == null)
				GMcommandList += "\n" + cm.getName() + " " + cm.getArgs();
			else if (cm.getArgs() == null && cm.getDescription() == null)
				GMcommandList += "\n" + cm.getName();
			else
				GMcommandList += "\n" + cm.getName();
		}
		
		
		if (GMcommandList == "")
			GMcommandList = "\nAucune commande est disponible !";

		toReturn = "\nCommandes disponibles pour le gm " + gmlvl + " :" + GMcommandList;

		return toReturn;
	}

	public static GmCommandManager getCommandByName(String name) {
		try {
			return GMcommand.get(GMcommandByName.get(name.toLowerCase()));
		} catch (Exception e) {
			for (GmCommandManager cm : getCommands()) {
				if (name.split(" ")[0].equals(cm.getName().trim()))
					return cm;
			}
		}
		return null;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public int getFunction() {
		return function;
	}

	public void setFunction(int funtion) {
		this.function = funtion;
	}
	
	public String getArgs() {
		if(args != null && !args.isEmpty()) {
			String Args = "";
			for(String curArgs : args.split(","))
				Args+= "[" + curArgs.toUpperCase() + "] ";
			return Args;
		} else
			return null;
	}
	public void setArgs(String args) {
		this.args = args;
	}

	public int getGMlevel() {
		return gmlvl;
	}

	public void setGMlevel(int gm) {
		this.gmlvl = gm;
	}

	public String getName() {
		return name.toUpperCase();
	}

	public String getDescription() {
		return description;
	}

	private void setDescription(String _description) {
		this.description = _description;
	}

	public void setName(String name) {
		this.name = name;
	}
}