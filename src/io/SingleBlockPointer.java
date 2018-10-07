package io;

import java.nio.ByteBuffer;

import io.ZFSIO.Device;

/**
 * pointer to single block 
 */
public class SingleBlockPointer 
{
	public int vDevId;
	public long block;
	public int csize;
	public SingleBlockPointer(int _vDevId, long _block, int _csize)
	{
		csize = _csize;
		vDevId = _vDevId;
		block = _block;
	}
	public SingleBlockPointer(ByteBuffer data, int offset)
	{
		csize = data.getInt(offset) & 0xFFFFFF;
		vDevId = data.getInt(offset + 4);
		block = data.getLong(offset + 8) & 0x7FFFFFFF;
		// TODO: add gangblock support
		//boolean _gang = (_ddata.get(offset + 15) & 0x80) == 0x80;
	}
	
	/**
	 * equality check 
	 */
	public boolean equals(SingleBlockPointer pointer)
	{
		if (vDevId != pointer.vDevId) return false;
		if (block != pointer.block) return false;
		if (csize != pointer.csize) return false;
		return true;
	}
	
	/**
	 * short validity check
	 */
	public boolean isValid()
	{
		if (csize<=0 || block==0) return false;
		Device dev = ZFSIO.getDevice(vDevId); 
		if (dev == null) return false;
		if (dev.size-csize < block*512) return false;
		return true;
	}
}
