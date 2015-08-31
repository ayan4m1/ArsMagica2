package am2.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufProcessor;
import io.netty.buffer.Unpooled;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.network.ByteBufUtils;

public class AMDataReader {
	ByteArrayInputStream input;
	DataInputStream dataStream;
	public byte ID;

	public AMDataReader(byte[] data){
		this(data, true);
	}

	public AMDataReader(byte[] data, boolean getID){
		input = new ByteArrayInputStream(data);
		dataStream = new DataInputStream(input);

		//get id byte
		if (getID){
			try {
				ID = dataStream.readByte();
			} catch (IOException e) {
				FMLLog.severe("AMDataReader (getID): " + e.toString());
				e.printStackTrace();
			}
		}
	}

	public int getInt(){
		int value = 0;
		try {
			value = dataStream.readInt();
		} catch (IOException e) {
			FMLLog.severe("AMDataReader (getInt): " + e.toString());
			e.printStackTrace();
		}
		return value;
	}

	public float getFloat(){
		float value = 0;
		try {
			value = dataStream.readFloat();
		} catch (IOException e) {
			FMLLog.severe("AMDataReader (getFloat): " + e.toString());
			e.printStackTrace();
		}
		return value;
	}

	public double getDouble(){
		double value = 0;
		try {
			value = dataStream.readDouble();
		} catch (IOException e) {
			FMLLog.severe("AMDataReader (getDouble): " + e.toString());
			e.printStackTrace();
		}
		return value;
	}

	public boolean getBoolean(){
		boolean value = false;
		try {
			value = dataStream.readBoolean();
		} catch (IOException e) {
			FMLLog.severe("AMDataReader (getBoolean): " + e.toString());
			e.printStackTrace();
		}
		return value;
	}

	public String getString(){
		String value = "";
		try {
			value = dataStream.readUTF();
		} catch (IOException e) {
			FMLLog.severe("AMDataReader (getString): " + e.toString());
			e.printStackTrace();
		}
		return value;
	}

	public byte getByte(){
		byte value = 0;
		try {
			value = dataStream.readByte();
		} catch (IOException e) {
			FMLLog.severe("AMDataReader (getByte): " + e.toString());
			e.printStackTrace();
		}
		return value;
	}

	public short getShort(){
		short value = 0;
		try {
			value = dataStream.readShort();
		} catch (IOException e) {
			FMLLog.severe("AMDataReader (getShort): " + e.toString());
			e.printStackTrace();
		}
		return value;
	}

	public long getLong(){
		long value = 0;
		try {
			value = dataStream.readLong();
		} catch (IOException e) {
			FMLLog.severe("AMDataReader (getLong): " + e.toString());
			e.printStackTrace();
		}
		return value;
	}

	public NBTTagCompound getNBTTagCompound(){
		NBTTagCompound data = null;
		try{
			int len = dataStream.readInt();
			byte[] bytes = new byte[len];
			dataStream.read(bytes);
			ByteBuf buf = Unpooled.copiedBuffer(bytes);		
			data = ByteBufUtils.readTag(buf);
		}catch (IOException e){
			FMLLog.severe("AMDataReader (getNBTTagCompound): " + e.toString());
			e.printStackTrace();
		}
		return data;
	}

	public byte[] getRemainingBytes(){
		byte[] remaining = null;
		try {
			remaining = new byte[dataStream.available()];
			dataStream.read(remaining);
		} catch (IOException e) {
			FMLLog.severe("AMDataReader (getRemainingBytes): " + e.toString());
			e.printStackTrace();
		}

		return remaining;
	}

	public ItemStack getItemStack() {
		NBTTagCompound compound = getNBTTagCompound();
		if (compound == null) return null;
		ItemStack stack = ItemStack.loadItemStackFromNBT(compound);
		return stack;
	}

	public int[] getIntArray() {
		try {			
			int[] arr = new int[dataStream.readInt()];
			for (int i = 0; i < arr.length; ++i)
				arr[i] = dataStream.readInt();
			return arr;
		} catch (IOException e) {
			FMLLog.severe("AMDataReader (getIntArray): " + e.toString());
			e.printStackTrace();
		}

		return new int[0];
	}
}
