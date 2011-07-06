package minebot;

import java.io.IOException;
import java.io.ObjectInputStream.GetField;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.*;
import java.lang.Integer;

public class minebot {
    private static Player player;
    private static int lastOpcode;
    private static Map map;
    private static Logger myLogger;

    public static void log(String[] messages) {
	for (String message : messages) {
	    myLogger.log(Level.ALL, message);
	    return;
	}
    }

    public static void log(String message) {
	myLogger.log(Level.ALL, message);
	return;
    }

    public static void log(Level level, String[] messages) {
	for(String message : messages) {
	    myLogger.log(level, message);
	}
    }

    public static void main(String args[]) throws UnknownHostException,
	    IOException {
	BufferedReader fileInput = null;
	String username = null;
	String password = null;
	myLogger = Logger.getLogger("MineBot");
	myLogger.setLevel(Level.ALL);
	try {
	    fileInput = new BufferedReader(new FileReader("login.txt"));
	    username = fileInput.readLine();
	    password = fileInput.readLine();
	} finally {
	    if (fileInput != null) {
		fileInput.close();
	    }
	}

	if (username == null || password == null) {
	    log("Invalid login.txt");
	    System.exit(0);
	}

	// Login to Minecraft
	String loginData = URLEncoder.encode("user", "UTF-8") + "="
		+ URLEncoder.encode(username, "UTF-8");
	loginData += "&" + URLEncoder.encode("password", "UTF-8") + "="
		+ URLEncoder.encode(password, "UTF-8");
	loginData += "&" + URLEncoder.encode("version", "UTF-8") + "="
		+ URLEncoder.encode("12", "UTF-8");

	URL url = new URL("https://login.minecraft.net");
	URLConnection conn = url.openConnection();
	conn.setDoOutput(true);
	OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
	wr.write(loginData);
	wr.flush();

	BufferedReader rd = new BufferedReader(new InputStreamReader(
		conn.getInputStream()));
	String line = rd.readLine();
	log(line);
	String[] loginResponse = line.split(":");

	wr.close();
	rd.close();

	// Connect to the server
	String host = "localhost"; // "snarglozog.zapto.org";
	int port = 25565;
	Socket socket = new Socket(host, port);
	DataInputStream input = new DataInputStream(socket.getInputStream());
	DataOutputStream output = new DataOutputStream(socket.getOutputStream());

	map = new Map();
	player = new Player(output, map);

	output.writeChar(2);
	byte buff[] = new byte[1024];
	sendString("liquidman3", output);

	log(input.readByte());
	log(input.readShort());
	input.read(buff);
	String hash = new String(buff, "UTF-16BE");
	log(hash);

	String authURL = "http://www.minecraft.net/game/joinserver.jsp?user=liquidman3&sessionId="
		+ loginResponse[3] + "&serverId=" + hash;
	url = new URL(authURL);
	conn = url.openConnection();
	rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	line = rd.readLine();
	log("Attempting to join server:" + line + " | "
		+ authURL);
	rd.close();
	if (!line.equals("OK")) {
	    System.exit(0);
	}

	output.writeByte(1);
	output.writeInt(14);
	sendString("liquidman3", output);
	output.writeLong(0);
	output.writeByte(0);

	String loginResponseStr = "Login Response: " + input.readUnsignedByte()
		+ " ";
	player.EID = input.readInt();
	loginResponseStr += player.EID + " " + input.readShort() + " "
		+ input.readLong() + " " + input.readUnsignedByte();
	log(loginResponseStr);

	while (socket.isConnected()) {
	    try {
		parsePackets(input, output);
		player.logic();
	    } catch (EOFException e) {
		log("EOFexception caught. Exiting.");
		break;
	    }
	}
    }

    public static void sendString(String str, DataOutputStream output)
	    throws IOException {
	output.writeShort(str.length());
	byte buff[] = new byte[str.length() * 2];
	buff = str.getBytes("UTF-16BE");
	output.write(buff, 0, str.length() * 2);
    }

    public static void parsePackets(DataInputStream input,
	    DataOutputStream output) throws IOException {
	// Position

	byte buff[] = new byte[4092];
	int opcode = 0;
	try {
	    opcode = input.readUnsignedByte();
	    // log("op:"+Integer.toHexString(opcode));
	    switch (opcode) {
	    case 0x00:
		break;
	    case 0x01:
		log("Login Response: "
			+ input.readUnsignedByte() + " " + input.readInt()
			+ " " + input.readShort() + " " + input.readLong()
			+ " " + input.readUnsignedByte());
		break;
	    case 0x03: {// Chat message
		int len = input.readShort();
		input.read(buff, 0, len * 2);
		log(new String(buff, "UTF-16BE"));
		break;
	    }
	    case 0x04: // Time
		input.readLong();
		break;
	    case 0x05: // Entity Equipment
		input.readInt();
		input.readShort();
		input.readShort();
		input.readShort();
		break;
	    case 0x06: { // Spawn position in block coords
		player.spawnX = input.readInt();
		player.spawnY = input.readInt();
		player.spawnZ = input.readInt();
		break;
	    }
	    case 0x07:
		input.readInt();
		input.readInt();
		input.readBoolean();
		break;
	    case 0x08:
		int hp = input.readShort();
		if (hp < 1) {
		    output.writeByte(0x09);
		    output.writeByte(0);
		    player.stance = player.y = player.spawnY * 32;
		}
		break;
	    case 0x09:
		input.readByte();
		break;
	    case 0x0D:
		player.x = input.readDouble();
		player.stance = input.readDouble();
		player.y = input.readDouble();
		player.z = input.readDouble();
		player.yaw = input.readFloat();
		player.pitch = input.readFloat();
		player.onGround = input.readBoolean();
		player.spawned = true;
		break;
	    case 0x0F: {
		input.readInt();
		input.readByte();
		input.readInt();
		input.readByte();
		int id = input.readShort();
		if (id >= 0) {
		    input.readByte();
		    input.readShort();
		}
		break;
	    }
	    case 0x11:
		input.readInt();
		input.readByte();
		input.readInt();
		input.readByte();
		input.readInt();
		break;
	    case 0x12:
		input.readInt();
		input.readByte();
		break;
	    case 0x13:
		input.readInt();
		input.readByte();
		break;
	    case 0x14: { // Named Entity Spawn
		input.readInt();
		int len = input.readShort();
		input.read(buff, 0, len * 2);
		input.readInt();
		input.readInt();
		input.readInt();
		input.readByte();
		input.readByte();
		input.readShort();
		break;
	    }
	    case 0x15: // Pickup Spawn
		input.read(buff, 0, 24);
		break;
	    case 0x16:
		input.readInt();
		input.readInt();
		break;
	    case 0x17: {
		input.readInt();
		input.readByte();
		input.readInt();
		input.readInt();
		input.readInt();
		int flag = input.readInt();
		if (flag > 0) {
		    input.readShort();
		    input.readShort();
		    input.readShort();
		}
		break;
	    }
	    case 0x18: {
		input.readInt();
		input.readByte();
		input.readInt();
		input.readInt();
		input.readInt();
		input.readByte();
		input.readByte();
		readMetadata(input);
		break;
	    }
	    case 0x1C:
		input.readInt();
		input.readShort();
		input.readShort();
		input.readShort();
		break;
	    case 0x1d:
		input.readInt();
		break;
	    case 0x1E:
		input.readInt();
		break;
	    case 0x1F: {
		int EID = input.readInt();
		int x = input.readByte();
		int y = input.readByte();
		int z = input.readByte();
		if (EID == player.EID) {
		    player.x += (double) x / 32;
		    player.stance = player.y += (double) y / 32;
		    player.z += (double) z / 32;
		}
		break;
	    }
	    case 0x20:
		input.readInt();
		input.readByte();
		input.readByte();
		break;
	    case 0x21:
		input.readInt();
		input.read(buff, 0, 5);
		break;
	    case 0x22:
		input.read(buff, 0, 18);
		break;
	    case 0x26:
		input.readInt();
		input.readByte();
		break;
	    case 0x27:
		input.readInt();
		input.readInt();
		break;
	    case 0x28:
		input.readInt();
		readMetadata(input);
		break;
	    case 0x32:
		input.readInt();
		input.readInt();
		input.readBoolean();
		break;
	    case 0x33: {
		int x = input.readInt();
		int y = input.readShort();
		int z = input.readInt();
		int sx = input.readByte();
		int sy = input.readByte();
		int sz = input.readByte();
		int size = input.readInt();
		byte data[] = new byte[size];
		int recv = input.read(data, 0, size);
		while (recv < size) {
		    log("0x33 recieved " + recv + " instead of "
			    + size);
		    recv += input.read(data, recv, size - recv);
		}
		map.readChunkData(x, y, z, sx, sy, sz, data);
		break;
	    }
	    case 0x34: {
		log("IMPLEMENT 0x34 YOU LAZY BUM!!!!");
		input.readInt();
		input.readInt();
		int len = input.readShort();
		input.read(buff, 0, len * 4);
		break;
	    }
	    case 0x35: { // Block change
		int x = input.readInt();
		int y = input.readByte();
		int z = input.readInt();
		int type = input.readByte();
		int metadat = input.readByte();
		map.setBlockType(x, y, z, type);
		break;
	    }
	    case 0x3D:
		input.read(buff, 0, 17);
		break;
	    case 0x46: // Invalid Bed and Rain states
		input.readByte();
		break;
	    case 0x47: // Thunderbolts
		input.readInt();
		input.readBoolean();
		input.readInt();
		input.readInt();
		input.readInt();
		break;
	    case 0x67: {
		input.readByte();
		int slot = input.readShort();
		int type = input.readShort();
		if (type != -1 && slot != -1) {
		    int count = input.readByte();
		    int uses = input.readShort();
		    player.inventory.addItem(slot, type, count, uses);
		} else if (slot != -1) {
		    player.inventory.deleteItem(slot);
		} else {
		    if (type != -1) {
			input.readByte();
			input.readShort();
		    }
		}
		break;
	    }
	    case 0x68: {
		input.readByte();
		int len = input.readShort();
		int type, count, uses;
		for (int i = 0; i < len; i++) {
		    type = input.readShort();
		    if (type != -1) {
			count = input.readByte();
			uses = input.readShort();
			player.inventory.addItem(i, type, count, uses);
		    }
		}
		player.inventory.print();
		break;
	    }
	    case 0x83: {
		input.readShort();
		input.readShort();
		int len = input.readUnsignedByte();
		input.read(buff, 0, len);
		break;
	    }
	    case 0xC8:
		input.readInt();
		input.readByte();
		break;
	    case 0xFF: {
		int len = input.readShort();
		input.read(buff, 0, len * 2);
		log("Kick: " + new String(buff, "UTF-16BE"));
		System.exit(0);
	    }

	    // We don't care about these yet. All of these are silently ignored.
	    default: {
		input.read(buff);
		log("Unknown opcode: "
			+ Integer.toHexString(opcode) + "|" + opcode);
		log("Last opcode:"
			+ Integer.toHexString(lastOpcode));
		log("Quitting.");
		System.exit(0);
		break;
	    }
	    }
	} catch (IOException e) {

	}
	lastOpcode = opcode;
    }

    public static void readMetadata(DataInputStream input) throws IOException {
	byte x = input.readByte();
	while (x != 127) {
	    switch (x >> 5) {
	    case 0:
		input.readByte();
		break;
	    case 1:
	    case 2:
	    case 3:
	    case 4:
	    case 5:
	    case 6:
		log("Op 0x18 non case 0.");
		System.exit(0);
	    }
	    x = input.readByte();
	}
    }
}
