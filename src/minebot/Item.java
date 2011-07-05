package minebot;

public class Item {
	public enum Types{AIR, STONE, GRASS, DIRT, COBBLESTONE}
	
	public int type;
	public int count;
	public int uses;
	
	public Item(int type, int count, int uses) {
		this.type = type;
		this.count = count;
		this.uses = uses;
	}
}
