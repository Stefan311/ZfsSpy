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

package httpHandlers;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;

import dataHandlers.DataHandlerNVTable;
import dataHandlers.DataHandlerZAP;
import io.FileBlockPointer;
import io.ObjCache;
import io.RedBlockPointer;
import io.ZFSIO;

public class HttpHandlerDatasetSummary extends HttpHandlerBase 
{
	/**
	 * load all data from a zfs object's blockpointers
	 */
	private ByteBuffer loadObjectDataFromPointers(ByteBuffer _data, int _offset, int _ptrcount, int _blockcount)
	{
		FileBlockPointer _fbp = new FileBlockPointer();
		
		_fbp.blocks = new RedBlockPointer[_ptrcount];
		for (int i=0;i<_ptrcount;i++)
		{
			_fbp.blocks[i] = new RedBlockPointer(_data, _offset + i*128);
		}

		ByteBuffer _result = ByteBuffer.allocate(_blockcount*0x4000);

		for (int i=0;i<_blockcount;i++)
		{
			ByteBuffer _chunk = _fbp.getFileBlock(i);
			if (_chunk == null)
			{
				return null;
			}
			
			_result.put(_chunk);
			_result.order(_chunk.order());
		}
		return _result;
	}
	
	/**
	 * parse and print dataset from DSL_DIR object
	 */
	private void printDataSetTreeFromDir(ByteBuffer _rootdata, int dirObj, String basename) throws Exception
	{
		// validity checks
		if (_rootdata.get(dirObj*512 + 0) != 12)
		{
			throw new Exception("Wrong object type on position #"+dirObj+" on master object list.");
		}
		if (_rootdata.get(dirObj*512 + 4) != 12)
		{
			throw new Exception("Wrong bonus type on position #"+dirObj+" on master object list.");
		}
		if ((_rootdata.getInt(dirObj*512 + 10) & 0xffff) != 256)
		{
			throw new Exception("Wrong bonus length on position #"+dirObj+" on master object list.");
		}

		// do head dataset
		int _headDataset = (int)_rootdata.getLong(dirObj*512 + 64 + 128 + 8);
		if (_headDataset != 0)
		{
			printDataSetTreeFromDataSet(_rootdata, _headDataset, basename);
		}

		// do child datasets
		int _childZap = (int)_rootdata.getLong(dirObj*512 + 64 + 128 + 32);
		if (_childZap != 0)
		{
			ByteBuffer _data = loadObjectDataFromPointers(_rootdata, _childZap*512 + 64, 3, (int)_rootdata.getLong(_childZap*512 + 16)+1);
			if (_data == null)
			{
				throw new Exception("Loading Child ZAP failed.");
			}
			
			HashMap<String,Object> _vnp = DataHandlerZAP.getZAP(_data, 0, _data.remaining());
			
			for (String s : _vnp.keySet())
			{
				if (!s.startsWith("$"))
				{
					Object o = _vnp.get(s);
					if (o instanceof Long)
					{
						int l = (int)((Long)o).longValue();
						printDataSetTreeFromDir(_rootdata, l, basename+"/"+s);
					}
				}
			}
		}
	}
	
	/*
	 * parse and print dataset from DSL_DATASET objects
	 */
	private void printDataSetTreeFromDataSet(ByteBuffer _rootdata, int _headDataset, String basename) throws Exception
	{
		// validity checks
		if (_rootdata.get(_headDataset*512 + 0) != 16)
		{
			throw new Exception("Wrong object type on position #"+_headDataset+" on master object list.");
		}
		if (_rootdata.get(_headDataset*512 + 4) != 16)
		{
			throw new Exception("Wrong bonus type on position #"+_headDataset+" on master object list.");
		}
		if ((_rootdata.getInt(_headDataset*512 + 10) & 0xffff) != 320)
		{
			throw new Exception("Wrong bonus length on position #"+_headDataset+" on master object list.");
		}
		
		// load object block
		RedBlockPointer ptr = new RedBlockPointer(_rootdata, _headDataset*512 + 64 + 128 + 128); 
		ByteBuffer _data = ptr.loadBlock();
		if (_data == null)
		{
			throw new Exception("Loading dataset DMU failed.");
		}

		// extract blockpointers to dataset zfs object list
		FileBlockPointer _fbp = new FileBlockPointer();
		_fbp.blocks = new RedBlockPointer[3];
		for (int i=0;i<3;i++)
		{
			_fbp.blocks[i] = new RedBlockPointer(_data,64 + i*128);
		}
		_fbp.blockcount = _data.getLong(16)+1;
		_fbp.filename = basename;
		
		// add the list to cache and get the new id for links
		int listId = ObjCache.getListId(_fbp);
		
		// add html for this zfs dataset
		append("<tr>");
		append("<td>"+basename+"</td>");
		append("<td><a href=\"dataset?listId="+listId+"&index=0\">explore Filesystem</a></td>");
		append("<td><a href=\"block?vDev="+ptr.blocks[0].vDevId+"&sector="+ptr.blocks[0].block+"&csize="+ptr.blocks[0].csize+"&dsize="+ptr.dsize+"&compress="+ptr.comptype+"&objtype="+(ptr.level>0 ? -2 : ptr.objtype)+"&endian="+ptr.endian+"&options="+options+"\">explore blockmode</a></td>");
		append("</tr>");

		// now handle snapshots
		int _snapZap = (int)_rootdata.getLong(_headDataset*512 + 64 + 128 + 32);
		if (_snapZap != 0)
		{
			// load snapshot list
			_data = loadObjectDataFromPointers(_rootdata, _snapZap*512 + 64, 3, (int)_rootdata.getLong(_snapZap*512 + 16)+1);
			if (_data == null)
			{
				throw new Exception("Loading Snapshot ZAP failed.");
			}
			
			// extract snapshots
			HashMap<String,Object> _vnp = DataHandlerZAP.getZAP(_data, 0, _data.remaining());
			
			// handle as dataset
			for (String s : _vnp.keySet())
			{
				Object o = _vnp.get(s);
				if (o instanceof Long)
				{
					int l = (int)((Long)o).longValue();
					printDataSetTreeFromDataSet(_rootdata, l, basename+"@"+s);
				}
			}
		}
	}
	
	/*
	 * display the list of all zfs datasets
	 */
	@Override
	protected void createHtmlBody() 
	{
		try 
		{
			append("Dataset summary:<br>");
			
			append("<table border=1px>");
			append("<tr><td>Name</td><td width=200>Filesystem mode</td><td width=200>Block mode</td></tr>");
			
			// first device
			ZFSIO.Device _dev = ZFSIO.devices.get(0);
			ByteBuffer _data;

			// load first disklabel
			_data = ZFSIO.getBlock(_dev, ZFSIO.getLabelSector(_dev,0), 256*1024);
			HashMap<String,Object> _vnp = DataHandlerNVTable.getNVTable(_data, 0x4000, 0x1c000);
			
			// get the transaction group property
			Object o = _vnp.get("txg");
			if (!(o instanceof Long))
			{
				throw new Exception("Transaction group not set in first disklabel.");
			}
			long txg = ((Long)o).longValue();

			// get the pool name property
			o = _vnp.get("name");
			if (!(o instanceof String))
			{
				throw new Exception("Pool name not set in first disklabel.");
			}
			String _poolname = (String)o; 
			
			// seek uberblock list for the right one
			RedBlockPointer _block = null; 
			for (int i=0;i<128;i++)
			{
				// fix byte order if needed
				int order = (_data.order() == ByteOrder.LITTLE_ENDIAN ? 1 : 0);
				long magic = _data.getLong(0x20000 + i*0x400);
				if (magic == 0xCB1BA0000000000l)
				{
					if (order == 0)
					{
						_data.order(ByteOrder.LITTLE_ENDIAN);
						order = 1;
					}
					else
					{
						_data.order(ByteOrder.BIG_ENDIAN);
						order = 0;
					}
				}
				
				magic = _data.getLong(0x20000 + i*0x400);
				long tnr = _data.getLong(0x20000 + i*0x400 + 16);

				// is this the right uberblock?
				if (magic == 0x00bab10c && tnr == txg)
				{
					// extract the block pointers
					_block = new RedBlockPointer(_data, 0x20000 + i*0x400 + 40); 
				}
			}
			
			if (_block == null)
			{
				throw new Exception("Uberblock with last transaktion group not found.");
			}
			
			// load the first DMU block
			_data = _block.loadBlock();
			if (_data == null)
			{
				throw new Exception("Loading first DMU object failed.");
			}
			if ((_data.get(0) & 0xff) != 10)
			{
				throw new Exception("Wrong object type on first DMU object.");
			}

			// load master object list
			int _blockcount = (int)_data.getLong(16)+1;
			ByteBuffer _rootList = loadObjectDataFromPointers(_data, 64, 3, _blockcount);
			if (_rootList == null)
			{
				throw new Exception("Loading master object list failed.");
			}

			if (_rootList.get(512) != 1)
			{
				throw new Exception("Wrong object type on position #1 on master object list.");
			}

			// allways little endian?
			_rootList.order(ByteOrder.LITTLE_ENDIAN);

			// load first object, this should be a zfs directory object
			_data = loadObjectDataFromPointers(_rootList, 512 + 64, 3, (int)_rootList.getLong(512 + 16)+1);
			if (_data == null)
			{
				throw new Exception("Loading ZFS directory failed.");
			}
			
			// parse as ZAP
			_data.position(0);
			_vnp = DataHandlerZAP.getZAP(_data, 0, _data.remaining());
			
			// get root dataset object id
			o = _vnp.get("root_dataset");
			if (!(o instanceof Long))
			{
				throw new Exception("Cannot get root dataset from ZFS directory object.");
			}
			int l = (int)((Long)o).longValue();
			
			// print datasets recursive, beginning with root dataset
			printDataSetTreeFromDir(_rootList, l, _poolname);
			
			append("</table>");
		} 
		catch (Exception e) 
		{
			errorlog.println(e.getLocalizedMessage());
			e.printStackTrace(errorlog);
		}
	}
}
