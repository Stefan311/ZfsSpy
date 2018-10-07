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

import dataHandlers.DataHandlerBlockPointer;
import dataHandlers.DataHandlerBonusBuffer;
import dataHandlers.DataHandlerHexdump;
import dataHandlers.DataHandlerNVTable;
import dataHandlers.DataHandlerZAP;
import io.Compressions;
import io.ObjCache;
import io.ZFSIO;
import io.FileBlockPointer;
import io.RedBlockPointer;

public class HttpHandlerBlock extends HttpHandlerBase 
{
	/*
	 * prints the zfs object summary to the html and create a new FileBlockPointer object from it
	 */
	private FileBlockPointer getHtmlFromObject(ByteBuffer data, int offset, int listid, int listindex)
	{
		FileBlockPointer result = new FileBlockPointer();
		
		int _dmutype = data.get(offset) & 0xff;
		int _nlevels = data.get(offset+2) & 0xff;
		int _nblkptr = data.get(offset+3) & 0xff;
		int _bonustype = data.get(offset+4) & 0xff;
		int _datablkszsec = data.getInt(offset+8) & 0xffff;
		int _bonuslen = data.getInt(offset+10) & 0xffff;
		int _compression = data.get(offset+6) & 0xff;
		long _maxblkid = data.getLong(offset+16);
		long _secphys = data.getLong(offset+24);
		
		append("<table border=1px>");
		append("<tr><td>Object Type</td><td>");
		append(DataHandlerBlockPointer.getDMUName(_dmutype));
		append("</td></tr><tr><td>indblkshift</td><td>");
		append(Integer.toString(data.get(offset+1) & 0xff));
		append("</td></tr><tr><td>Indirection Levels</td><td>");
		append(Integer.toString(_nlevels));
		append("</td></tr><tr><td>Blockpointer Count</td><td>");
		append(Integer.toString(_nblkptr));
		append("</td></tr><tr><td>Bonus Type</td><td>");
		append(Integer.toString(_bonustype));
		append("</td></tr><tr><td>Checksum</td><td>");
		append(Integer.toString(data.get(offset+5) & 0xff));
		append("</td></tr><tr><td>Compression</td><td>");
		append(Integer.toString(_compression));
		append("</td></tr><tr><td>datablkszsec</td><td>");
		append(Integer.toString(_datablkszsec));
		append("</td></tr><tr><td>bonuslen</td><td>");
		append(Integer.toString(_bonuslen));
		append("</td></tr><tr><td>dn_flags</td><td>");
		append(Integer.toString(data.get(offset+7) & 0xff));
		append("</td></tr><tr><td>maxblkid</td><td>");
		append(Long.toString(_maxblkid));
		append("</td></tr><tr><td>secphys</td><td>");
		append(Long.toString(_secphys));
		append("</td></tr>");
		
		boolean _valid = false;
		
		result.blocks = new RedBlockPointer[_nblkptr];
		if (_bonustype == 44 && _bonuslen == 168)
		{
			result.filesize = data.getLong(offset + 64 + 128 + 16);
		}
		else
		{
			result.filesize = _datablkszsec*512*(_maxblkid+1);
		}
		result.blockcount = _maxblkid+1;
		
		for (int i=0;i<_nblkptr;i++)
		{
			result.blocks[i] = new RedBlockPointer(data, offset + 64 + i*128);
			append("<tr><td>Block pointer #"+i+"</td><td>");
			_valid |= DataHandlerBlockPointer.printBlkptrToHtml(result.blocks[i] , this);
			append("</td></tr>");
		}

		if (_bonustype!=0 && _bonuslen>0)
		{
			append("<tr><td>Bonus Data</td><td>");
			DataHandlerBonusBuffer.printBonusBufferToHtml(_bonustype, data, offset + 64 + 128, _bonuslen, this);
			append("</td></tr>");
		}
		append("</table>");
		
		if (_valid && _dmutype == 10 && objtype == 11)
		{
			append("<a href=\"dataset?listId="+listid+"&index="+listindex+"\">Use target as Filesystem</a><br>");
		}

		if (_valid && _dmutype == 19)
		{
			append("<a href=\"loaddata?listId="+listid+"&index="+listindex+"\" target=\"_blank\">download</a>&nbsp;&nbsp;");
			append("<a href=\"storedata?listId="+listid+"&index="+listindex+"\">save on server"+(publicdemo ? "*" : "")+"</a><br>");
		}
		
		return result;
	}
	
	/*
	 * Displays content of generic data blocks, decoding depends on zfs object type
	 */
	@Override
	protected void createHtmlBody() 
	{
		append("VDev: "+device.filename+", Sector: "+sector+", compressed size: "+csize+",decompressed size: "+dsize+", compression: "+Compressions.getName(compress)+", objecttype:"+DataHandlerBlockPointer.getDMUName(objtype)+"<br><br>");
		
		try 
		{
			
			ByteBuffer _cdata = ZFSIO.getBlock(device, sector + 0x2000, csize*512);
			ByteBuffer _ddata = Compressions.decompress(_cdata, 0, dsize*512, compress);
			_ddata.order(endian==1 ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);

			FileBlockPointer fbp = new FileBlockPointer();
			fbp.filesize = dsize*512;
			fbp.blockcount = 1;
			fbp.blocks = new RedBlockPointer[] { new RedBlockPointer(0, dsize, compress, 0, 10, endian, device.vDevId, sector, csize) };
			int listid;
			
			switch (objtype)
			{
				case 3: // nv-table
					HashMap<String,Object> map = DataHandlerNVTable.getNVTable(_ddata, 0, dsize*512);
					append("NV-Table<br>");
					DataHandlerNVTable.printNvTableToHtml(map, this);
					return;
				case 10: // DMU Object list
					listid = ObjCache.getListId(fbp);
					for (int i=0;i<dsize;i++)
					{
						append("<div id=\"object"+i+"\">");
						append("DMU Object #"+i+"<br>");
						ObjCache.addObject(listid, i, getHtmlFromObject(_ddata,i*512,listid,i));
						append("</div>");
						append("<br>");
					}
					if (publicdemo)
					{
						append(red("*not aviable in public demo modus"));
					}
					return;
				case 11: // DMU Object
					listid = ObjCache.getNewListId();
					append("DMU Object<br>");
					ObjCache.setListObject(listid, getHtmlFromObject(_ddata,0,listid,0));
					return;
				case 20:
				case 21:
				case 46:
				case -3: // display as ZAP object
					HashMap<String,Object> map2 = DataHandlerZAP.getZAP(_ddata, 0, dsize*512);
					append("ZAP<br>");
					DataHandlerNVTable.printNvTableToHtml(map2, this);
					return;
				case -1: // display as hex data
					DataHandlerHexdump.printHexdumpToHtml(_ddata,0,dsize*512, this);
					return;
				case -2: // display as block table
					append("Block-Table<br>");
					append("<table border=1px><tr><td>Name</td><td>Value</td></tr>");
					for (int i=0;i<dsize*4;i++)
					{
						append("<tr><td>Pointer #"+i+"</td><td>");
						DataHandlerBlockPointer.printBlkptrToHtml(new RedBlockPointer(_ddata, i*128), this);
						append("</td></tr>");
					}
					append("</table>");
					return;
				default: // default: display as hex data
					DataHandlerHexdump.printHexdumpToHtml(_ddata,0,dsize*512, this);
					return;
			}
		} 
		catch (Exception e) 
		{
			errorlog.println(e.getLocalizedMessage());
			e.printStackTrace(errorlog);
		} 

	}

}
