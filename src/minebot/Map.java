package minebot;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import java.util.*;

public class Map {
	private HashMap<String, byte[]> chunks;
	
	public Map() {
		chunks = new HashMap<String, byte[]>();
	}
	
	public void readChunkData(int x, int y, int z, int sx, int sy, int sz, byte[] data) throws IOException {
		sx = sx+1;
		sy = sy+1;
		sz = sz+1;
		int size = sx * sy * sz;
		
		Inflater inflater = new Inflater();
		inflater.setInput(data);
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length);
		
		byte[] buff = new byte[1024];
		while (!inflater.finished()) {
			try {
				int count = inflater.inflate(buff);
				bos.write(buff, 0, count);
			} catch (DataFormatException e) {
			}
		}
		bos.close();
		
		byte[] decompressedData = bos.toByteArray();
		
		byte[] blockData = new byte[size];
		for (int i = 0; i < blockData.length; i++) {
			blockData[i] = decompressedData[i];
		}
		
		int cx = (int)(x) >> 4;
		int cz = (int)(z) >> 4;
		String key = cx+"."+cz;
		if (size == 16*16*128) {
			chunks.put(cx+"."+cz, blockData);
		} else if (chunks.containsKey(key)) {
			x = x & 15;
			y = y & 127;
			z = z & 15;			
			for (int ix = 0; ix < sx; ix++) {
				for (int iy = 0; iy < sy; iy++) {
					for (int iz = 0; iz < sz; iz++) {
						chunks.get(key)[(y+iy) + (((z+iz) * sy) + ((x+ix) * sy * sx))] = blockData[iy + (iz * sy) + (ix * sy * sz)];
					}
				}
			}
		}
	}
	
	public int block(double x, double y, double z) {
		x = Math.floor(x);
		y = Math.floor(y);
		z = Math.floor(z);
		return block((int)x, (int)y, (int)z);
	}
		
	public int block(int x, int y, int z) {
		int index = getIndex(x,y,z);
		int cx = (int)(x) >> 4;
		int cz = (int)(z) >> 4;
		String key = cx+"."+cz;
		if (chunks.containsKey(key)) {
			return chunks.get(key)[index];
		} else {
			//System.out.println("Chunk doesn't exist."+x+" "+y+" "+z+" "+cx+" "+cz);
			return -1;
		}
	}
	
	public void setBlockType(int x, int y, int z, int type) {
		String key = (x >> 4) + "." + (z >> 4);
		if (chunks.containsKey(key)) {
			chunks.get(key)[getIndex(x,y,z)] = (byte)(type);
		}
	}
	
	private int getIndex(int x, int y, int z) {
		return (y&127) + (((z&15) * 128) + ((x&15) * 128 * 16));
	}
}
