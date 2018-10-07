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

import java.util.HashMap;

import httpHandlers.HttpHandlerBase;
import io.Checksums;
import io.Compressions;
import io.ZFSIO;
import io.RedBlockPointer;
import io.SingleBlockPointer;

public class DataHandlerBlockPointer 
{

	/**
	 * print block pointer to html
	 */
	public static boolean printBlkptrToHtml(RedBlockPointer data, HttpHandlerBase http)
	{
		if (data == null || data.blocks == null)
		{
			http.append("invalid blockpointer");
			return false;
		}
		if ("1".equals(http.options.substring(0, 1)))
		{
			return printBlkptrToHtmlLong(data, http);
		}
		return printBlkptrToHtmlShort(data, http);
	}
	
	/*
	 * get zfs object type names
	 */
	private static final String[] DMUTYPES = {
			/* general: */
			"NONE","OBJECT DIRECTORY","OBJECT ARRAY","PACKED NVLIST","NVLIST SIZE","BPLIST","BPLIST HDR", // 0-6
			/* spa: */
			"SPACE MAP HEADER","SPACE MAP", // 7-8
			/* zil: */
			"INTENT LOG", // 9
			/* dmu: */
			"DNODE","OBJSET", // 10-11
			/* dsl: */
			"DSL DIR","DSL DIR CHILD MAP","DSL DS SNAP MAP","DSL PROPS","DSL DATASET", // 12-16
			/* zpl: */
			"ZNODE","OLDACL","PLAIN FILE CONTENTS","DIRECTORY CONTENTS","MASTER NODE","UNLINKED SET", // 17-22
			/* zvol: */
			"ZVOL","ZVOL PROP", // 23-24
			/* other; for testing only! */
			"PLAIN OTHER","UINT64 OTHER","ZAP OTHER", // 25-27
			/* new object types: */
			"ERROR LOG","SPA HISTORY","SPA HISTORY OFFSETS","POOL_PROPS","DSL PERMS","ACL","SYSACL","FUID", // 28-35
			"FUID SIZE","NEXT CLONES","SCAN QUEUE","USERGROUP USED","USERGROUP QUOTA","USERREFS","DDT ZAP","DDT STATS","SA", // 36-43
			"SA MASTER NODE","SA ATTR REGISTRATION","SA ATTR LAYOUTS","SCAN XLATE","DEDUP","DEADLIST","DEADLIST HDR","DSL CLONES","BPOBJ SUBOBJ"};
	private static final String[] CUSTOMTYPES = {"Hexdump","Blocktable","Generic ZAP"}; 
	public static String getDMUName(int type)
	{
		if (type >=0 && type<DMUTYPES.length)
		{
			return DMUTYPES[type]+" ("+type+")";
		}
		if (type <0 && (-1-type)<CUSTOMTYPES.length)
		{
			return CUSTOMTYPES[-1-type];
		}
			
		return "Invalid ("+type+")";
	}
	
	private static boolean printBlkptrToHtmlShort(RedBlockPointer data, HttpHandlerBase http)
	{
		boolean _valid = false;

		if (data.embedded != null)
		{
			if (data.embedded.getLong(0) == 0x8000000000000003l)
			{
				HashMap<String, Object> map = DataHandlerZAP.getZAP(data.embedded, 0, data.dsize);
				if (map.isEmpty())
				{
					http.append("Embedded Micro-ZAP (empty)<br>");
				}
				else
				{
					http.append("Embedded Micro-ZAP<br>");
					DataHandlerNVTable.printNvTableToHtml(map, http);
				}
			}
			else
			{
				http.append("Embedded Data<br>");
				DataHandlerHexdump.printHexdumpToHtml(data.embedded, 0, data.dsize, http);
			}
		}
		else if (data.objtype == 0)
		{
			http.append("empty");
		}
		else
		{
			http.append(getDMUName(data.objtype));
			http.append(", ");
	
			for (SingleBlockPointer ptr : data.blocks)
			{
				if (!ptr.isValid())
				{
					http.append(" <font color=red>invalid</font>");
				}
				else
				{
					try 
					{
						http.append("<a href=\"block?vDev="+ptr.vDevId+"&sector="+ptr.block+"&csize="+ptr.csize+"&dsize="+data.dsize+"&compress="+data.comptype+"&objtype="+(data.level>0 ? -2 : data.objtype)+"&endian="+data.endian+"&options="+http.options+"\">");
						if (ZFSIO.checkCRC(ptr.vDevId, ptr.block, ptr.csize, data.sumtype, data.endian, data.crc))
						{
							http.append(" valid");
							_valid = true;
						}
						else
						{
							http.append(" <font color=red>invalid</font>");
						}
						http.append("</a> ");
					} 
					catch (Exception e) 
					{
						http.logError(e.getLocalizedMessage());
						e.printStackTrace(http.errorlog);
					}
				}
			}
		}
		return _valid;
	}
	
	
	protected static boolean printBlkptrToHtmlLong(RedBlockPointer data, HttpHandlerBase http)
	{
		boolean _valid = false;
		int i = 0;
		if (data.embedded != null)
		{
			if (data.embedded.getLong(0) == 0x8000000000000003l)
			{
				HashMap<String, Object> map = DataHandlerZAP.getZAP(data.embedded, 0, data.dsize);
				if (map.isEmpty())
				{
					http.append("Embedded Micro-ZAP (empty)<br>");
				}
				else
				{
					http.append("Embedded Micro-ZAP<br>");
					DataHandlerNVTable.printNvTableToHtml(map, http);
				}
			}
			else
			{
				http.append("Embedded Data<br>");
				DataHandlerHexdump.printHexdumpToHtml(data.embedded, 0, data.dsize, http);
			}
		}
		else
		{
			http.append("<table border=1px width=100%>");
			for (SingleBlockPointer ptr : data.blocks)
			{
				http.append("<tr><td>Block copy #");
				http.append(Integer.toString(i++));
				http.append("</td><td>");
				http.append("<a href=\"block?vDev="+ptr.vDevId+"&sector="+ptr.block+"&csize="+ptr.csize+"&dsize="+data.dsize+"&compress="+data.comptype+"&objtype="+(data.level>0 ? -2 : data.objtype)+"&endian="+data.endian+"&options="+http.options+"\">");
				http.append("Size=");
				http.append(Integer.toString(ptr.csize));
				http.append(", vdev=");
				http.append(Integer.toString(ptr.vDevId));
				http.append(", sector=");
				http.append(Long.toString(ptr.block));
				http.append("</a>");
				if (ZFSIO.getDevice(ptr.vDevId) == null)
				{
					http.append(" <font color=red>invalid vdev</font>");
				}
				else if (ptr.csize<=0)
				{
					http.append(" <font color=red>invalid size</font>");
				}
				else
				{
					try 
					{
						if (ZFSIO.checkCRC(ptr.vDevId, ptr.block, ptr.csize, data.sumtype, data.endian, data.crc))
						{
							http.append(" <font color=green>valid checksum</font>");
							_valid = true;
						}
						else
						{
							http.append(" <font color=red>invalid checksum</font>");
						}
					} 
					catch (Exception e) 
					{
						http.logError(e.getLocalizedMessage());
						e.printStackTrace(http.errorlog);
					}
				}
				
				http.append("</td></tr>");
			}
			
			http.append("<tr><td>Logical size</td><td>");
			http.append(Integer.toString(data.dsize));
			http.append("</td></tr><tr><td>Physical size</td><td>");
			http.append(Integer.toString(data.raw.getInt(50) & 0xffff));
			http.append("</td></tr><tr><td>Compression</td><td>");
			http.append(Compressions.getName(data.comptype));
			http.append("</td></tr><tr><td>Checksum</td><td>");
			http.append(Checksums.getName(data.sumtype));
			http.append("</td></tr><tr><td>Type</td><td>");
			http.append(getDMUName(data.objtype));
			http.append("</td></tr><tr><td>Lvl</td><td>");
			http.append(Integer.toString(data.level));
			http.append("</td></tr><tr><td>Endian</td><td>");
			http.append(data.endian==1 ? "Little" : "Big");
			http.append("</td></tr><tr><td>Birth Transaction</td><td>");
			http.append(Long.toString(data.raw.getLong(80)));
			http.append("</td></tr><tr><td>Fill count</td><td>");
			http.append(Long.toString(data.raw.getLong(88)));
			http.append("</td></tr><tr><td>Checksum[0]</td><td>");
			http.append(Long.toString(data.crc[0]));
			http.append("</td></tr><tr><td>Checksum[1]</td><td>");
			http.append(Long.toString(data.crc[1]));
			http.append("</td></tr><tr><td>Checksum[2]</td><td>");
			http.append(Long.toString(data.crc[2]));
			http.append("</td></tr><tr><td>Checksum[3]</td><td>");
			http.append(Long.toString(data.crc[3]));
			http.append("</td></tr></table>");
		}
		return _valid;
	}
	

}
