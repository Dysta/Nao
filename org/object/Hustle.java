package org.object;

public class Hustle {
	
	private int ID;
	private int AnimationId;
	private String AnimationName;
	private int AnimationArea;
	private int AnimationAction;
	private int AnimationSize;
	
	public Hustle(int Id, int AnimId, String Name, int Area, int Action, int Size) 
	{
		this.ID = Id;
		this.AnimationId = AnimId;
		this.AnimationName = Name;
		this.AnimationArea = Area;
		this.AnimationAction = Action;
		this.AnimationSize = Size;
	}
	
	public int getId() 
	{
		return ID;
	}
	
	public String getName() 
	{
		return AnimationName;
	}
	
	public int getArea() 
	{
		return AnimationArea;
	}
	
	public int getAction() 
	{
		return AnimationAction;
	}
	
	public int getSize() 
	{
		return AnimationSize;
	}
	
	public int getAnimationId() 
	{
		return AnimationId;
	}
	
	public static String PrepareToGA(Hustle animation) 
	{
		StringBuilder Packet = new StringBuilder();
		Packet.append(animation.getAnimationId()).append(",").append(animation.getArea()).append(",").append(animation.getAction()).append(",").append(animation.getSize());
		return Packet.toString();
	}
	
}