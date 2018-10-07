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
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;

import httpHandlers.HttpHandlerBase;

public class DataHandlerNVTable 
{
	
	@SuppressWarnings("unchecked")
	/**
	 * print single name/value to html
	 */
	private static void printNVValue(Object o, HttpHandlerBase http)
	{
		if (o instanceof Integer)
		{
			Integer i = (Integer)o;
			http.append(i+" (0x"+Integer.toHexString(i.intValue())+")");
		}
		else if (o instanceof Long)
		{
			Long i = (Long)o;
			http.append(i+" (0x"+Long.toHexString(i.longValue())+")");
		}
		else if (o instanceof Short)
		{
			Short i = (Short)o;
			http.append(i+" (0x"+Integer.toHexString(i.shortValue())+")");
		}
		else if (o instanceof String)
		{
			http.append((String)o);
		}
		else if (o instanceof HashMap)
		{
			printNvTableToHtml((HashMap<String,Object>)o, http);
		}
		else if (o instanceof ArrayList)
		{
			ArrayList<?> list = (ArrayList<?>)o;
			for (Object p : list)
			{
				printNVValue(p, http);
				http.append("<br>");
			}
		}
		else if (o == null)
		{
			http.append("(Empty Value)");
		}
	}
	

	/**
	 * print name/value-lists to html
	 */
	public static void printNvTableToHtml(HashMap<String,Object> table, HttpHandlerBase http)
	{
		http.append("<table border=1px><tr><td>Name</td><td>Value</td></tr>");
		for (String s : table.keySet())
		{
			http.append("<tr><td>"+s+"</td><td>");
			Object o = table.get(s);
			printNVValue(o, http);
		}
		http.append("</table>");
	}
	
	static int retoffs;
	
	/**
	 * extract name/value list from block data 
	 */
	public static HashMap<String,Object> getNVTable(ByteBuffer data, int offset, int size)
	{
		HashMap<String,Object> result = new HashMap<>();
		ByteOrder _oldorder = data.order();
		data.order(ByteOrder.BIG_ENDIAN);
		
		int i = offset;
		
		i+=12;
		
		while (i-offset < size)
		{
			int oldoffs = i;
			
			int encsize = data.getInt(i);
			i+=4;
			
			int decsize = data.getInt(i);
			i+=4;
			
			if (encsize == 0 || decsize==0) break;
			
			int len = data.getInt(i);
			i+=4;
			
			byte[] s = new byte[len];
			data.position(i);
			data.get(s);
			String n = new String(s);
			i = i + (len+3 & 0xfc) + 3;
			
			int datatype = data.get(i);
			i++;

			int valct = data.getInt(i);
			i+=4;
			
			Object val = null;
			if (valct>0)
			{
				switch (datatype)
				{
					case 1: // DATA_TYPE_BOOLEAN
						val = new Boolean(true);
						break;
					case 2: // DATA_TYPE_BYTE
						val = new Byte(data.get(i));
						break;
					case 3: // DATA_TYPE_INT16
					case 4: // DATA_TYPE_UINT16
						val = new Short(data.getShort(i));
						break;
					case 5: // DATA_TYPE_INT32
					case 6: // DATA_TYPE_UINT32
						val = new Integer(data.getInt(i));
						break;
					case 7: // DATA_TYPE_INT64
					case 8: // DATA_TYPE_UINT64
						val = new Long(data.getLong(i));
						break;
					case 9: // DATA_TYPE_STRING
						len = data.getInt(i);
						i+=4;
						s = new byte[len];
						data.position(i);
						data.get(s);
						val = new String(s);
						i = i + (len+3 & 0xfc) + 3;
						break;
					case 19: // DATA_TYPE_NVLIST,
						val = getNVTable(data,i-4,encsize);
						break;
					case 20: // DATA_TYPE_NVLIST_ARRAY,
						ArrayList<HashMap<String,Object>> list = new ArrayList<>();
						for (int a = 0;a<valct;a++)
						{
							list.add(getNVTable(data,i-4,encsize));
							i = retoffs;
						}
						val = list;
						break;
					case 10: // DATA_TYPE_BYTE_ARRAY,
					case 11: // DATA_TYPE_INT16_ARRAY,
					case 12: // DATA_TYPE_UINT16_ARRAY,
					case 13: // DATA_TYPE_INT32_ARRAY,
					case 14: // DATA_TYPE_UINT32_ARRAY,
					case 15: // DATA_TYPE_INT64_ARRAY,
					case 16: // DATA_TYPE_UINT64_ARRAY,
					case 17: // DATA_TYPE_STRING_ARRAY,
					case 18: // DATA_TYPE_HRTIME,
					case 21: // DATA_TYPE_BOOLEAN_VALUE,
					case 22: // DATA_TYPE_INT8,
					case 23: // DATA_TYPE_UINT8,
					case 24: // DATA_TYPE_BOOLEAN_ARRAY,
					case 25: // DATA_TYPE_INT8_ARRAY,
					case 26: // DATA_TYPE_UINT8_ARRAY,
					case 27: // DATA_TYPE_DOUBLE
					default:
						val = "Unsupported Datatype! ("+datatype+")";
						break;
				}
			}
			result.put(n, val);
			i = oldoffs + encsize;			
		}
		retoffs = i;
		data.order(_oldorder);
		return result;
	}

	
}
