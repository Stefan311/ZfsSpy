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

package dataHandlers;

import java.nio.ByteBuffer;
import java.util.HashMap;

import httpHandlers.HttpHandlerBase;
import io.ObjCache;
import io.FileBlockPointer;
import io.RedBlockPointer;
import io.SingleBlockPointer;

public class DataHandlerDirectory 
{
	/**
	 * print file lists to html
	 */
	public static void printDirectoryToHtml(int _listId, long _objectId, HttpHandlerBase http) throws Exception
	{
		FileBlockPointer _fbp = ObjCache.getObject(_listId, _objectId);
		if (_fbp == null)
		{
			http.logError("cannot find ROOT directory (obj id="+_objectId+")");
			return;
		}
		
		FileBlockPointer _listFbp = ObjCache.getListObject(_listId);
		RedBlockPointer[] _listBlocks = new RedBlockPointer[(int) _listFbp.blockcount];
		for (int i=0;i<_listFbp.blockcount;i++)
		{
			_listBlocks[i] = _listFbp.getBlockPointer(i);
		}

		http.append("<table border=1px>");
		http.append("<tr><td>Name</td><td>Type</td><td>Size</td><td>File Operations</td><td>Block Operations</td></tr>");
		
		for (int i=0;i<_fbp.blockcount;i++)
		{
			ByteBuffer _data = _fbp.getFileBlock(i);
			if (_data == null)
			{
				http.logError("cannot read directory block #"+i);
				continue;
			}

			_data.position(0);
			HashMap<String,Object> _dirContent = DataHandlerZAP.getZAP(_data, 0, _data.remaining());
			
			for (String s : _dirContent.keySet())
			{
				Object o = _dirContent.get(s);
				if (!(o instanceof Long))
				{
					continue;
				}
				long f = ((Long)o).longValue();
				long _fileId = f & 0x00ffffffffffffffl;
				int _fileType = (int)(f >> 56) & 0xff;
				
				http.append("<tr><td>"+s+"</td>");
				
				switch (_fileType)
				{
					case 0x40:
						http.append("<td>Directory</td>");
						break;
					case 0x80:
						http.append("<td>File</td>");
						break;
					default:
						http.append("<td>UNKNOWN ("+_fileType+")</td>");
						break;
				}

				FileBlockPointer _fbpObj = ObjCache.getObject(_listId, _fileId);
				if (_fbpObj == null)
				{
					http.append("<td>invalid object (#"+_fileId+")</td><td></td><td></td></tr>");
					continue;
				}

				_fbpObj.filename = s;
				http.append("<td>"+_fbpObj.filesize+"</td>");
				
				switch (_fileType)
				{
					case 0x40:
						http.append("<td>");
						http.append("<a href=\"filelist?listId="+_listId+"&index="+_fileId+"\">browse</a>&nbsp;&nbsp;");
						http.append("<a href=\"storedir?listId="+_listId+"&index="+_fileId+"\">save"+(HttpHandlerBase.publicdemo ? "*" : "")+"</a>");
						http.append("</td>");
						break;
					case 0x80:
						http.append("<td>");
						http.append("<a href=\"loaddata?listId="+_listId+"&index="+_fileId+"\" target=\"_blank\">download</a>&nbsp;&nbsp;");
						http.append("<a href=\"storedata?listId="+_listId+"&index="+_fileId+"\">save"+(HttpHandlerBase.publicdemo ? "*" : "")+"</a>");
						http.append("</td>");
						break;
					default:
						http.append("<td>&nbsp;</td>");
						break;
				}

				http.append("<td>");
				RedBlockPointer _rbp = _listBlocks[(int) (_fileId / 32)];
				SingleBlockPointer ptr = _rbp.getValidCopy();
				if (ptr == null)
				{
					http.append("invalid blockpointers");
				}
				else
				{
					http.append("<a href=\"block?vDev="+ptr.vDevId+"&sector="+ptr.block+"&csize="+ptr.csize+"&dsize="+_rbp.dsize+"&compress="+_rbp.comptype+"&objtype="+(_rbp.level>0 ? -2 : _rbp.objtype)+"&endian="+_rbp.endian+"&options="+http.options+"#object"+(_fileId % 32)+"\">Explore</a>");
				}
				
				http.append("</td></tr>");
				
			}
		}
		
		http.append("</table>");
		
		if (HttpHandlerBase.publicdemo)
		{
			http.append(http.red("*not aviable in public demo modus"));
		}
	}
}
