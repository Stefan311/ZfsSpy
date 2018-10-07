package io;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

/**
 * redundant pointer to block (all blocks are copies)
 */
public class RedBlockPointer 
{
	public int level;
	public int dsize;
	public int psize;
	public int comptype;
	public int sumtype;
	public int objtype;
	public int endian;
	public long[] crc = new long[4];
	public SingleBlockPointer[] blocks = null;
	public ByteBuffer embedded = null;
	public ByteBuffer raw = null;
	private int validcopy = -1;
	
	/**
	 * create from given single date
	 */
	public RedBlockPointer(int _level, int _dsize, int _comptype, int _sumtype, int _objtype, int _endian, int _vDevId, long _block, int _csize)
	{
		level = _level;
		dsize = _dsize;
		comptype = _comptype;
		sumtype = _sumtype;
		objtype = _objtype;
		endian = _endian;
		psize = _csize;
		blocks = new SingleBlockPointer[] { new SingleBlockPointer(_vDevId, _block, _csize) };
	}
	
	/**
	 * create from data buffer
	 */
	public RedBlockPointer(ByteBuffer data, int offset)
	{
		try
		{
			byte[] d = new byte[128];
			data.position(offset);
			data.get(d);
			raw = ByteBuffer.wrap(d);
			raw.order(data.order());
			endian = (data.get(offset + 55) & 0x80) >> 7;
			objtype = data.get(offset + 54) & 0xff;
			
			// does this pointer contain embedded data?
			if ((data.get(offset+52) & 0x80) == 0x80)
			{
				byte[] emb = new byte[112];
				for (int i=0;i<48;i++)
				{
					emb[i] = data.get(offset + i);
				}
				for (int i=0;i<24;i++)
				{
					emb[i+48] = data.get(offset + 56 + i);
				}
				for (int i=0;i<40;i++)
				{
					emb[i+72] = data.get(offset + 88 + i);
				}
				
				embedded = Compressions.decompress(ByteBuffer.wrap(emb), 0, (data.getInt(offset+48) & 0x1ffffff) + 1000, data.get(offset+52) & 0x7f);
				embedded.position(0);
				embedded.order(endian==1 ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
				dsize = embedded.remaining();
				return;
			}
			
			// get shared data from buffer
			crc = new long[] { data.getLong(offset + 96), data.getLong(offset + 104), data.getLong(offset + 112), data.getLong(offset + 120)};
			comptype = data.get(offset + 52) & 0x7f;
			sumtype = data.get(offset + 53) & 0xff;
			dsize = (data.getInt(offset + 48) & 0xffff) + 1;
			psize = data.getInt(offset + 50) & 0xffff;
			level = data.get(offset + 55) & 0x7f;

			// get the single block pointers 
			ArrayList<SingleBlockPointer> l = new ArrayList<>();
			for (int i=0;i<3;i++)
			{
				SingleBlockPointer p = new SingleBlockPointer(data, offset + i*16);
				if (p.isValid())
				{
					l.add(p);
				}
			}
			blocks = l.toArray(new SingleBlockPointer[l.size()]);
		}
		catch (Exception e)
		{
			// this is no valid blockpointer
			validcopy = -2;
		}

		// more validity checks
		if (psize > 256)
		{
			validcopy = -2;
		}
		if (level > 8)
		{
			validcopy = -2;
		}
	}
	
	/**
	 * load data block and checksum it
	 */
	public ByteBuffer loadBlock()
	{
		// this one have embedded data?
		if (embedded != null)
		{
			// just give it
			return embedded;
		}
		
		// this is known invalid?
		if (validcopy == -2)
		{
			return null;
		}

		// try all single block copies
		for (SingleBlockPointer b : blocks)
		{
			try
			{
				// load the block
				ByteBuffer _cdata = ZFSIO.getBlock(b.vDevId, b.block + 0x2000, b.csize * 512);
				_cdata.order(endian==1 ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
				
				// create and verify checksum
				long[] s = Checksums.createChecksum(_cdata, 0, b.csize * 512, sumtype);
				if (s[0] != crc[0] || s[1] != crc[1] || s[2] != crc[2] || s[3] != crc[3])
				{
					continue;	// checksumm error - lets try the next block copy if possible
				}
				
				// decompress
				ByteBuffer _ddata = Compressions.decompress(_cdata, 0, dsize*512, comptype);
				
				// set endianess for later
				_ddata.order(endian==1 ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
				return _ddata;
			}
			catch (Exception e)
			{
			}
		}
		
		// placeholder data?
		if (psize == 0 && dsize>0)
		{
			return ByteBuffer.allocate(dsize * 512);
		}

		// no data at all
		return null;
	}
	
	/**
	 * returns the pointer to the first valid block copy
	 */
	public SingleBlockPointer getValidCopy()
	{
		// embedded data is also valid, but no block pointer
		if (embedded != null)
		{
			validcopy = -3;
		}
		
		// already tested: return valid blockpointer
		if (validcopy >= 0)
		{
			return blocks[validcopy];
		}

		// already tested: no valid blockpointer, return null
		if (validcopy < -1)
		{
			return null;
		}
		
		// check all block pointers
		for (int i=0;i<blocks.length;i++)
		{
			try
			{
				ByteBuffer _cdata = ZFSIO.getBlock(blocks[i].vDevId, blocks[i].block + 0x2000, blocks[i].csize * 512);
				_cdata.order(endian==1 ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
				long[] s = Checksums.createChecksum(_cdata, 0, blocks[i].csize * 512, sumtype);
				if (s[0] != crc[0] || s[1] != crc[1] || s[2] != crc[2] || s[3] != crc[3])
				{
					continue;	// checksumm error - lets try the next block copy if possible
				}
				// jea! we found one valid block copy!
				validcopy = i;
				return blocks[validcopy];
			}
			catch (Exception e)
			{
			}
		}
		
		// still here? so there is no valid block copy
		validcopy = -2;
		return null;
	}
	
	/*
	 * equality check 
	 */
	public boolean equals(RedBlockPointer pointer)
	{
		if (level != pointer.level) return false;
		if (dsize != pointer.dsize) return false;
		if (comptype != pointer.comptype) return false;
		if (sumtype != pointer.sumtype) return false;
		if (objtype != pointer.objtype) return false;
		if (endian != pointer.endian) return false;
		if (blocks.length != pointer.blocks.length) return false;
		for (int i=0;i<blocks.length;i++)
		{
			if (!blocks[i].equals(pointer.blocks[i])) return false;
		}
		for (int i=0;i<4;i++)
		{
			if (crc[i] != pointer.crc[i]) return false;
		}
		return true;
	}
}
