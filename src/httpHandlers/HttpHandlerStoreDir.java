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

import dataHandlers.DataHandlerZAP;
import io.ObjCache;
import io.FileBlockPointer;
import io.RedBlockPointer;

public class HttpHandlerStoreDir extends HttpHandlerBase 
{
	/*
	 * recursive store directories
	 */
	private void storeDir(int _listId, long _index, String _basePath)
	{
		// get cached directory object
		FileBlockPointer _fbp = ObjCache.getObject(_listId, _index);
		if (_fbp == null)
		{
			logError("cannot find directory (obj id="+_index+")");
			return;
		}
		
		// get directory name
		String _thisPath = _fbp.filename;
		
		// get object list
		FileBlockPointer _listFbp = ObjCache.getListObject(_listId);
		
		// create block pointer list for this zfs objectlist
		RedBlockPointer[] _listBlocks = new RedBlockPointer[(int) _listFbp.blockcount];
		for (int i=0;i<_listFbp.blockcount;i++)
		{
			_listBlocks[i] = _listFbp.getBlockPointer(i);
		}
		
		// all blocks of the directory
		for (int i=0;i<_fbp.blockcount;i++)
		{
			// load directory block
			ByteBuffer _data = _fbp.getFileBlock(i);
			if (_data == null)
			{
				logError("cannot read directory block #"+i+", skipped");
				continue;
			}

			// parse directory
			_data.position(0);
			HashMap<String,Object> _dirContent = DataHandlerZAP.getZAP(_data, 0, _data.remaining());

			log("Enter directory: \""+_thisPath+"\"");

			// for every entry
			for (String s : _dirContent.keySet())
			{
				// get object id
				Object o = _dirContent.get(s);
				if (!(o instanceof Long))
				{
					logError("Invalid index data type for \""+s+"\"");
					continue;
				}
				long f = ((Long)o).longValue();
				
				// the uppermost 8bit are the filetype
				long _fileId = f & 0x00ffffffffffffffl;
				int _fileType = (int)(f >> 56) & 0xff;
				
				// get the file object from cache 
				FileBlockPointer _fbpObj = ObjCache.getObject(_listId, _fileId);
				if (_fbpObj == null)
				{
					logError("invalid object (#"+_fileId+"), file \""+s+"\"");
					continue;
				}

				// set the filename
				_fbpObj.filename = s;
				
				switch (_fileType)
				{
					case 0x40: // directory
						storeDir(_listId, _fileId, _basePath+"/"+_thisPath);
						break;
					case 0x80: // plain file
						if (HttpHandlerStoreFile.StoreFile(_listId, _fileId, this, outDir+_basePath+"/"+_thisPath))
						{
							append(_basePath+"/"+_thisPath+"/"+s+" written<br>");
							log(_basePath+"/"+_thisPath+"/"+s+" written");
						}
						else
						{
							append(red(_basePath+"/"+_thisPath+"/"+s+" failed<br>"));
							log(_basePath+"/"+_thisPath+"/"+s+" failed");
						}
						break;
					default:
						logError("UNKNOWN directory entry type \""+s+"\" ("+_fileType+")");
						break;
				}
			}
		}
	}
	
	/*
	 * store zfs directory to server disk
	 */
	@Override
	protected void createHtmlBody() 
	{
		int _listId = Integer.parseInt(params.get("listId"));
		long _index = Long.parseLong(params.get("index"));
		
		storeDir(_listId,_index,"");
	}

}
