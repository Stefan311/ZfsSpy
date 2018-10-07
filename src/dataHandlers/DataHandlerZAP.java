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

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;

public class DataHandlerZAP 
{
	/**
	 * get string data 
	 */
	private static String getChunkString(ByteBuffer data, int offset, int chunk, int len)
	{
		int c = chunk;
		int l = 0;
		String result = null;
		while (l < len && c != 0xffff)
		{
			if (data.get(offset + c*24) == -5)
			{
				data.position(offset + c*24 + 1);
				int cs = (len-l > 21 ? 21 : len-l-1);
				byte[] d = new byte[cs];
				data.get(d);
				try 
				{
					if (result == null)
					{
						result = new String(d,"UTF8");
					}
					else
					{
						result += new String(d,"UTF8");
					}
				} 
				catch (UnsupportedEncodingException e) {}
				l += cs;
				c = data.getInt(offset + c*24 + 22) & 0xffff;
			}
		}
		return result;
	}
	
	/**
	 * get long data
	 */
	private static Long getChunkLong(ByteBuffer data, int offset, int chunk) 
	{
		if (data.get(offset + chunk*24) == -5)
		{
			ByteOrder old = data.order();
			data.order(ByteOrder.BIG_ENDIAN);
			Long result = Long.valueOf(data.getLong(offset + chunk*24 + 1));
			data.order(old);
			return result; 
		}
		return null;
	}
	
	/**
	 * get name/value list from ZAP data block
	 */
	public static HashMap<String,Object> getZAP(ByteBuffer data, int offset, int size)
	{
		int _offset = offset;
		HashMap<String,Object> result = new HashMap<>();
		
		// micro-zap
		if (data.getLong(_offset) == 0x8000000000000003l)
		{
			_offset += 64;
			while (_offset < (offset+size))
			{
				long _val = data.getLong(_offset);
				byte[] _buffer = new byte[50];
				data.position(_offset+14);
				data.get(_buffer);
				String _name = new String(_buffer).trim();
				if (data.getLong(_offset+14) != 0)
				{
					result.put(_name, Long.valueOf(_val));
				}
				_offset+=64;
			}
			return result;
		}
		
		// zap header
		if (data.getLong(_offset) == 0x8000000000000001l)
		{
			if (data.getLong(_offset+8) == 0x2F52AB2ABl && size > 0x4000)
			{
				_offset += 0x4000;
			}
		}
		
		// zap leaf
		while (_offset < (offset+size) && data.getLong(_offset) == 0x8000000000000000l)
		{
			_offset += 0x0430;
			
			for (int i=0;i<638;i++)
			{
				if (data.get(_offset + i*24) == -4)
				{
					int _nchunk = data.getInt(_offset + i*24 + 4) & 0xffff;
					int _nlen = data.getInt(_offset + i*24 + 6) & 0xffff;
					int _vchunk = data.getInt(_offset + i*24 + 8) & 0xffff;
					int _vlen = data.getInt(_offset + i*24 + 10) & 0xffff;
					String _name = getChunkString(data, _offset, _nchunk, _nlen);
					Object _val = null;
					switch (_vlen)
					{
						case 1:
						case 8: _val = getChunkLong(data, _offset, _vchunk); break;
						default: _val = getChunkString(data, _offset, _vchunk, _vlen); break;
					}
					result.put(_name, _val);
				}
			}
			
			_offset += 0x3BD0;
		}
		return result;
	}
	
}
