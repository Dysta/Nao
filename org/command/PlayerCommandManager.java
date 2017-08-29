package org.command;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

public class PlayerCommandManager {
	/** Author S **/

	private int id;
	private String name;
	private String description;
	private String function;
	private String cond;
	private int price;

	public static Map<Integer, PlayerCommandManager> command = new TreeMap<Integer, PlayerCommandManager>();
	public static Map<String, Integer> commandByName = new TreeMap<String, Integer>();
	public static String commandList = "";
	public static String vipCommandList = "";

	public PlayerCommandManager(int _id, String _name, String _description, String _function, String _cond, int _price) {
		setId(_id);
		setName(_name);
		setDescription(_description);
		setFunction(_function);
		setCond(_cond);
		setPrice(_price);
	}

	public static void addCommand(PlayerCommandManager x) {
		command.put(x.getId(), x);
		commandByName.put(x.getName().toLowerCase(), x.getId());
	}

	public static PlayerCommandManager getCommandById(int id) {
		return command.get(id);
	}

	public static Collection<PlayerCommandManager> getCommands() {
		return command.values();
	}

	public static String getCommandList(boolean isVip) {
		String toReturn = "";

		if (commandList == "" || vipCommandList == "") {
			for (PlayerCommandManager cm : getCommands()) {
				if (cm.getFunction().equals("1") || cm.getFunction().equals("0"))
					continue;

				if (cm.getCond() != null && cm.getCond().contains("PZ=1")) {
					if (cm.getPrice() > 0 && cm.getDescription() == null)
						vipCommandList += "\n<b>." + cm.getName() + " [" + cm.getPrice() + " pts]</b>";
					else if (cm.getPrice() > 0 && cm.getDescription() != null)
						vipCommandList += "\n<b>." + cm.getName() + " - " + cm.getDescription() + " [" + cm.getPrice()
								+ " pts]</b>";
					else if (cm.getPrice() == 0 && cm.getDescription() != null)
						vipCommandList += "\n<b>." + cm.getName() + " - " + cm.getDescription() + "</b>";
					else
						vipCommandList += "\n<b>." + cm.getName() + "</b>";
				} else {
					if (cm.getPrice() > 0 && cm.getDescription() == null)
						commandList += "\n<b>." + cm.getName() + " [" + cm.getPrice() + " pts]</b>";
					else if (cm.getPrice() > 0 && cm.getDescription() != null)
						commandList += "\n<b>." + cm.getName() + " - " + cm.getDescription() + " [" + cm.getPrice() + " pts]</b>";
					else if (cm.getPrice() == 0 && cm.getDescription() != null)
						commandList += "\n<b>." + cm.getName() + " - " + cm.getDescription() + "</b>";
					else
						commandList += "\n<b>." + cm.getName() + "</b>";
				}
			}
			if (vipCommandList == "")
				vipCommandList = "Aucune commande V.I.P n'est disponible !";
			else if (commandList == "")
				commandList = "Aucune commande disponible !";
		}

		if (isVip)
			toReturn = "\n<b>Commandes V.I.P disponibles:</b>" + vipCommandList;
		else
			toReturn = "\n<b>Commandes disponibles:</b>" + commandList;

		return toReturn;
	}

	public static PlayerCommandManager getCommandByName(String name) {
		try {
			return command.get(commandByName.get(name.toLowerCase()));
		} catch (Exception e) {
			for (PlayerCommandManager cm : getCommands()) {
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

	public String getFunction() {
		return function;
	}

	public void setFunction(String funtion) {
		this.function = funtion;
	}

	public String getCond() {
		return cond;
	}

	public void setCond(String cond) {
		this.cond = cond;
	}

	public int getPrice() {
		return price;
	}

	public void setPrice(int price) {
		this.price = price;
	}

	public String getName() {
		return name;
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