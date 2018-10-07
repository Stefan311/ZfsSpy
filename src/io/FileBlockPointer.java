package io;

import java.nio.ByteBuffer;

/**
 * zfs file, directory or metadata object 
 */
public class FileBlockPointer 
{
	public long blockcount;
	public long filesize;
	public RedBlockPointer[] blocks = new RedBlockPointer[0];
	public String filename;

	/**
	 * equality check 
	 */
	public boolean equals(FileBlockPointer pointer)
	{
		if (blockcount != pointer.blockcount) return false;
		if (filesize != pointer.filesize) return false;
		if (blocks.length != pointer.blocks.length) return false;
		for (int i=0;i<blocks.length;i++)
		{
			if (!blocks[i].equals(pointer.blocks[i])) return false;
		}
		return true;
	}
	
	/**
	 * get a blockpointer from blockpointer tree
	 */
	private RedBlockPointer getIndirectBlockPointer(RedBlockPointer ptr, long blockId)
	{
		// we are at leaf level? give the leaf! 
		if (ptr.level == 0)
		{
			return ptr;
		}
		
		// load blockpointer list
		ByteBuffer data = ptr.loadBlock();
		
		// calc index number
		int nr = (int)(blockId >> (9*(ptr.level-1))) & 1023;
		
		// create new blockpointer object 
		RedBlockPointer newptr = new RedBlockPointer(data, nr*128);
		
		// recursive call!
		return getIndirectBlockPointer(newptr, blockId);
	}
	
	/**
	 * get one blockpointer from this object
	 */
	public RedBlockPointer getBlockPointer(long blockId)
	{
		int nr = (int)(blockId >> 9*blocks[0].level) & 3;
		return getIndirectBlockPointer(blocks[nr], blockId);
	}

	/**
	 * get block data from blockpointer tree
	 */
	private ByteBuffer getIndirectBlock(RedBlockPointer ptr, long blockId)
	{
		// load block data
		ByteBuffer data = ptr.loadBlock();

		// we are still in branch level?
		if (ptr.level > 0)
		{
			// calc index number
			int nr = (int)(blockId >> (10*(ptr.level-1))) & 1023;

			// create new blockpointer object 
			RedBlockPointer newptr = new RedBlockPointer(data, nr*128);

			// recursive call!
			return getIndirectBlock(newptr, blockId);
		}
		
		// finally we got the data! 
		return data;
	}
	
	/**
	 * get block data from this object
	 */
	public ByteBuffer getFileBlock(long blockId)
	{
		int nr = (int)(blockId >> 10*blocks[0].level) & 3;
		return getIndirectBlock(blocks[nr], blockId);
	}
	
	/**
	 * get the all data blocks from this object
	 */
	public static ByteBuffer getFileData(FileBlockPointer fbp)
	{
		ByteBuffer _result = ByteBuffer.allocate((int)fbp.filesize);
        long l = fbp.filesize;

		for (int i=0;i<fbp.blockcount;i++)
		{
			ByteBuffer _chunk = fbp.getFileBlock(i);
			if (_chunk == null)
			{
				return null;
			}
			
			_chunk.position(0);			
	        if (l>_chunk.remaining())
	        {
				_result.put(_chunk.array(),0,(int)l);
	        }
	        else
	        {
				_result.put(_chunk);
	        }
			_chunk.position(0);			
	        l -= _chunk.remaining();
	        _result.order(_chunk.order());
		}
		return _result;
	}
}
