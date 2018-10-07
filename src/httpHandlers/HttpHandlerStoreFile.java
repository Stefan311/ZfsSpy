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

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;

import io.ObjCache;
import io.FileBlockPointer;

public class HttpHandlerStoreFile extends HttpHandlerBase 
{
	/*
	 * Store single file to server disk
	 */
	public static boolean StoreFile(int _listId, long _index, HttpHandlerBase _http, String _dstPath)
	{
		// get the cached object
		FileBlockPointer _file = ObjCache.getObject(_listId, _index);
		
		String n = _file.filename;
		// unknown filename? create dummy
		if (n == null)
		{
			n = _listId+"-"+_index+".bin";
		}
		
		// target path not exists? create new
		File d = new File(_dstPath);
		if (!d.exists())
		{
			d.mkdirs();
		}
		
		// more target file checks
		if (!d.isDirectory())
		{
			_http.logError("Output directiry \""+_dstPath+"\" is a file!");
			return false;
		}
		File f = new File(_dstPath+"/"+n);
		if (f.exists())
		{
			_http.logError("Output file \""+n+"\" alread exist!");
			return false;
		}
		
		try 
		{
			FileOutputStream out = new FileOutputStream(f, false);

			long l = _file.filesize;

			// copy all blocks to file
	        for (int i=0;i<_file.blockcount;i++)
	        {
	        	// read block from zfs
	        	ByteBuffer _buffer = _file.getFileBlock(i);
	        	if (_buffer == null)
	        	{
	        		_http.logError("Unreadable file Block #"+i+"! This block is skipped, the resulting file is corrupt!");
	        		// we continue copying, maybe the resulting file is still usefull
	        	}
	        	else
	        	{
	        		// write block to file
		        	byte[] b = _buffer.array();
		        	out.write(b,0,(l>b.length ? b.length : (int)l));
		        	l -= b.length;
	        	}
	        }
	        
	        out.close();
		} 
		catch (Exception e) 
		{
			_http.errorlog.println(e.getLocalizedMessage());
			e.printStackTrace(_http.errorlog);
		}
		return true;
	}

	/*
	 * Store single file to server disk
	 */
	@Override
	protected void createHtmlBody() 
	{
		int _listId = Integer.parseInt(params.get("listId"));
		long _index = Long.parseLong(params.get("index"));
		
		if (StoreFile(_listId, _index, this, outDir))
		{
			append("File written.<br>");
		}
		else
		{
			append(red("Error!<br>"));
		}
	}

}
