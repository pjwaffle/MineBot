package minebot;

public class Inventory {
	private Item items[];
	public int holdingSlot;
	
	public Inventory() {
		items = new Item[45];
		for (int i = 0; i < items.length; i++) {
			items[i] = null;
		}
	}
	
	public void addItem(int slot, int type, int count, int uses) {
		items[slot] = new Item(type, count, uses);
	}
	
	public void print() {
		String str = "Items:";
		for (int i = 0; i < items.length; i ++) {
			if (items[i] != null) {
				str += " "+items[i].type;
			}
		}
		System.out.println(str);
	}
	
	public int equip(int type) {
		for (int i = 36; i < 45; i++) {
			if (items[i] != null && items[i].type == type) {
				holdingSlot = i;
				return i;
			}
		}
		return -1;
	}
	
	public Item getItem(int slot) {
		return items[slot];
	}
	
	public void deleteItem(int slot) {
		if (items[slot] != null) 
			items[slot] = null;
	}
}
