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
import java.util.HashMap;

import dataHandlers.DataHandlerDirectory;
import dataHandlers.DataHandlerZAP;
import io.FileBlockPointer;
import io.ObjCache;
import io.RedBlockPointer;

public class HttpHandlerDataset extends HttpHandlerBase 
{
	/**
	 * load dataset and show root directory 
	 */
	@Override
	protected void createHtmlBody() 
	{
		try
		{
			int _listId = Integer.parseInt(params.get("listId"));
			
			// get the cached zfs object list
			FileBlockPointer _objList = ObjCache.getListObject(_listId);
	
			long _fileCount = 0;
			long _dirCount = 0;
			long _fileSizeSum = 0;
			
	        for (int i=0;i<_objList.blockcount;i++)
	        {
	        	// load one list table block
	        	ByteBuffer _data = _objList.getFileBlock(i);
	        	if (_data == null)
	        	{
	        		logError("Failed to load objectlist block #"+i+" (objects "+(i*32)+"-"+(i*32+31)+")");
	        		continue;
	        	}
	        	
	        	// parse all block entries
	        	for (int j=0;j<32;j++)
	        	{
		        	_data.position(0);
	        		if ((j+1)*512 > _data.remaining())
	        		{
		        		logError("Objectlist block is too short: #"+i+" (objects "+(i*32+j)+"-"+(i*32+31)+") rem "+_data.remaining());
	        			break;
	        		}
	        		
	        		// create and fill new zfs object
					FileBlockPointer _newFbp = new FileBlockPointer();

					// get some needed values from the object 
					int _dmutype = _data.get(j*512) & 0xff;
					int _nblkptr = _data.get(j*512 + 3) & 0xff;
					int _bonustype = _data.get(j*512 + 4) & 0xff;
					int _datablkszsec = _data.getInt(j*512 + 8) & 0xffff;
					int _bonuslen = _data.getInt(j*512 + 10) & 0xffff;
					long _maxblkid = _data.getLong(j*512 + 16);

					// get data length
					if (_bonustype == 44 && _bonuslen == 168)
					{
						_newFbp.filesize = _data.getLong(j*512 + 64 + 128 + 16);
					}
					else
					{
						_newFbp.filesize = _datablkszsec*512*(_maxblkid+1);
					}
					_newFbp.blockcount = _maxblkid+1;

					// add pointers to block data
					_newFbp.blocks = new RedBlockPointer[_nblkptr];
					for (int k=0;k<_nblkptr;k++)
					{
						_newFbp.blocks[k] = new RedBlockPointer(_data, j*512 + 64 + k*128);
					}
	
					// directory?
					if (_dmutype == 20)
					{
						_dirCount++;
					}
					
					// plain file?
					if (_dmutype == 19)
					{
						_fileCount++;
						_fileSizeSum += _newFbp.filesize;
					}

					// finally add the zfs object to cache
					ObjCache.addObject(_listId, i*32+j, _newFbp);
	        	}
	        }
			
	        // display dataset summary
			append("Dataset:<br>");
			append("<table border=1px>");
			append("<tr><td>Directory Count</td><td>"+_dirCount+"</td></tr>");
			append("<tr><td>File Count</td><td>"+_fileCount+"</td></tr>");
			append("<tr><td>File Size Summary</td><td>"+_fileSizeSum+"</td></tr>");
			append("</table><br>");

			// get master node
			FileBlockPointer _fbp = ObjCache.getObject(_listId, 1);
			if (_fbp.blocks[0].objtype != 21)
			{
				throw new Exception("blockpointer #1 ist not a MASTER NODE (21)");
			}
		
			ByteBuffer _data = _fbp.getFileBlock(0);
			if (_data == null)
			{
				throw new Exception("cannot read MASTER NODE");
			}

			// extract the "ROOT" (root directory) property 
			_data.position(0);
			HashMap<String,Object> _masterNode = DataHandlerZAP.getZAP(_data, 0, _data.remaining());
			Object _rootId = _masterNode.get("ROOT");
			if (_rootId == null)
			{
				throw new Exception("cannot get ROOT id from MASTER NODE");
			}

			// display root directory
			append("Root Directory:<br>");
			DataHandlerDirectory.printDirectoryToHtml(_listId, ((Long)_rootId).longValue(), this);
		}
		catch (Exception e) 
		{
			errorlog.println(e.getLocalizedMessage());
			e.printStackTrace(errorlog);
		}
	}

}
