/*
BSD 2-Clause License

Copyright (c) 2018, Stefan Berndt
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package io;
import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;

import dataHandlers.DataHandlerNVTable;

public class ZFSIO 
{
	private static int nvdev = 1000;
	public static ArrayList<Device> devices = new ArrayList<>();
	
	// TODO: the whole vDevId handling is not correct for some raid configurations. FIX! 

	/**
	 * device data
	 */
	public static class Device
	{
		public long size;
		public String filename;
		public int vDevId;
	}
	
	/**
	 * add new device and try to get it's real vdev id
	 */
	public static void addDevice(String _filename) throws Exception
	{
		Device dev = new Device();
		dev.filename = _filename;

		File f = new File(dev.filename);

		if (!f.exists() || f.isDirectory() || !f.canRead())
		{
			throw new Exception("Device not accessable!");
		}
		
		dev.size = f.length();
		byte[] result = new byte[256*1024];

		FileInputStream fr = new FileInputStream(f);
		fr.read(result);
		fr.close();
		
		ByteBuffer bf = ByteBuffer.wrap(result);
		HashMap<String,Object> vnp = DataHandlerNVTable.getNVTable(bf, 0x4000, 0x1c000);
		
		if (!initVdevIds(vnp,dev))
		{
			dev.vDevId = nvdev;
			nvdev++;
		}
		
		devices.add(dev);
	}

	/**
	 * get data block from device (by device number)
	 */
	public static ByteBuffer getBlock(int vDev, long sector, int length) throws Exception
	{
		Device dev = getDevice(vDev);
		return getBlock(dev, sector, length);
	}
	
	/**
	 * get data block from device (by device object)
	 */
	public static ByteBuffer getBlock(Device dev, long sector, int length) throws Exception
	{
		File f = new File(dev.filename);
		if (dev.size < (sector * 512 + length))
		{
			throw new Exception("Sector outside device!");
		}
		
		byte[] result = new byte[length];

		FileInputStream fr = new FileInputStream(f);
		fr.skip(sector * 512);
		fr.read(result);
		fr.close();
		
		return ByteBuffer.wrap(result);
	}
	
	/**
	 * load block and check crc
	 */
	public static boolean checkCRC(int vdevId, long sector, int csize, int checksumtype, int endian, long[] sum) throws Exception
	{
		Device dev = getDevice(vdevId);
		if (dev == null) return false;
		if (dev.size < (sector + 0x2000 + csize) * 512) return false;
		ByteBuffer b = getBlock(dev, sector + 0x2000, csize * 512);
		b.order(endian==1 ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
		long[] s = Checksums.createChecksum(b, 0, csize * 512, checksumtype);
		return (s[0] == sum[0] && s[1] == sum[1] && s[2] == sum[2] && s[3] == sum[3]);
	}
	
	/**
	 * get device object from number
	 */
	public static Device getDevice(int vdevId)
	{
		for (Device dev : devices)
		{
			if (dev.vDevId == vdevId)
			{
				return dev;
			}
		}
		return null;
	}
	
	/**
	 * get blocknumber for disklabel
	 */
	public static long getLabelSector(Device dev, int labelnr)
	{
		switch (labelnr)
		{
			case 0: return 0;
			case 1: return 512;
			case 2: return (dev.size / 512 - 1024);
			case 4: return (dev.size / 512 - 512);
			default: return 0;
		}
	}
	
	/**
	 * try to get the vdev id for this device
	 */
	public static boolean initVdevIds(HashMap<String,Object> vnp, Device dev) 
	{
		Object o = vnp.get("guid");
		if (!(o instanceof Long)) return false;
		long guid = ((Long)o).longValue();
		
		o = vnp.get("vdev_tree");
		if (!(o instanceof HashMap)) return false;
		@SuppressWarnings("unchecked")
		HashMap<String,Object> devtree = (HashMap<String, Object>)o;
		
		o = devtree.get("guid");
		if (!(o instanceof Long)) return false;
		long guid2 = ((Long)o).longValue();
		
		o = devtree.get("id");
		if (!(o instanceof Long)) return false;
		int vdevId = ((Long)o).intValue();

		if (guid == guid2)
		{
			dev.vDevId = vdevId;
			return true;
		}
		
		o = devtree.get("children");
		if (!(o instanceof ArrayList)) return false;
		@SuppressWarnings("unchecked")
		ArrayList<HashMap<String,Object>> childs = (ArrayList<HashMap<String,Object>>)o;
		
		for (HashMap<String,Object> child : childs)
		{
			o = child.get("guid");
			if (!(o instanceof Long)) continue;
			guid2 = ((Long)o).longValue();

			o = child.get("id");
			if (!(o instanceof Long)) continue;
			vdevId = ((Long)o).intValue();

			if (guid == guid2)
			{
				dev.vDevId = vdevId;
				return true;
			}
		}
		return false;
	} 
	
}
