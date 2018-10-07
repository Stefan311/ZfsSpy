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

import java.io.IOException;
import java.io.OutputStream;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import io.FileBlockPointer;
import io.ObjCache;


public class HttpHandlerDownload extends HttpHandlerBase 
{
	/**
	 * Download file data. This class overrides the base html handling
	 * to provide binary html download
	 */
	@Override
	public void handle(HttpExchange html) throws IOException
	{
		request = html.getRequestURI().getPath();
		params.clear();
		
		// parse http parameters
		String p1 = html.getRequestURI().getQuery();
		if (p1 != null)
		{
			String[] p2 = p1.split("&");
			for (String p3 : p2)
			{
				String[] p4 = p3.split("=");
				if (p4.length == 2)
				{
					params.put(p4[0], p4[1]);
				}
			}
		}

		// get parameters
		int _listId = Integer.parseInt(params.get("listId"));
		long _index = Long.parseLong(params.get("index"));
		
		// get file object from list-cache 
		FileBlockPointer _file = ObjCache.getObject(_listId, _index);
		
		// prepare http response
		Headers h = html.getResponseHeaders();
		h.add("content-type", "application/octet-stream");
		
		// filename known?
		if (_file.filename == null) 
		{
			// no, create generic one
			h.add("Content-Disposition","attachment; filename=\"zfs-spy-file-"+_listId+"-"+_index+".bin");
		}
		else
		{
			// yes, use!
			h.add("Content-Disposition","attachment; filename=\""+_file.filename);
		}
        html.sendResponseHeaders(200, _file.filesize);
        OutputStream out = html.getResponseBody();
		
        long l = _file.filesize;
        
        // load all file
        for (int i=0;i<_file.blockcount;i++)
        {
        	// load block
        	byte[] _buffer = _file.getFileBlock(i).array();
        	
        	// TODO: proper error handling
        	
        	// send block
        	out.write(_buffer,0,(l>_buffer.length ? _buffer.length : (int)l));
        	l -= _buffer.length;
        }

        // done
        out.close();
	}
	
	@Override
	protected void createHtmlBody() 
	{
		
	}
}
