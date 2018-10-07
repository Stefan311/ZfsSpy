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
import io.ZFSIO;

public class HttpHandlerLabelDetails extends HttpHandlerBase 
{
	/*
	 * display disklabel details
	 */
	@Override
	protected void createHtmlBody() 
	{
		int vdev = Integer.parseInt(params.get("vDev"));
		ZFSIO.Device dev = ZFSIO.getDevice(vdev);
		int labelnr = Integer.parseInt(params.get("labelNr"));

		append("VDev: "+dev.filename+", Label: L"+labelnr+"<br><br>");
		
		ByteBuffer data;
		HashMap<String,Object> vnp;

		try 
		{
			// get disklabel
			data = ZFSIO.getBlock(vdev, ZFSIO.getLabelSector(dev,labelnr), 256*1024);
			
			// parse nv-map from disklabel
			vnp = DataHandlerNVTable.getNVTable(data, 0x4000, 0x1c000);

			// get last transaktion group
			Object o = vnp.get("txg");
			long lasttx = 0;
			if (o instanceof Long)
			{
				lasttx = ((Long)o).longValue();
			}
			
			// display uberblocks
			append("Uberblocks<br><table border=1px>");
			for (int i=0;i<128;i++)
			{
				if ((i & 15) == 0) append("<tr>");
				
				boolean valid = true;
				
				// fix byte endian
				int order = (data.order() == ByteOrder.LITTLE_ENDIAN ? 1 : 0);
				long magic = data.getLong(0x20000 + i*0x400);
				if (magic == 0xCB1BA0000000000l)
				{
					if (order == 0)
					{
						data.order(ByteOrder.LITTLE_ENDIAN);
						order = 1;
					}
					else
					{
						data.order(ByteOrder.BIG_ENDIAN);
						order = 0;
					}
				}

				// validity checks 
				magic = data.getLong(0x20000 + i*0x400);
				if (magic != 0x00bab10c)
				{
					valid = false;
				}
				long tnr = data.getLong(0x20000 + i*0x400 + 16);
				if (tnr>lasttx)
				{
					valid = false;
				}
				
				// print uberblock link
				append("<td><a href=\"uberblock?vDev="+vdev+"&labelNr="+labelnr+"&uberBl="+i+"&endian="+order+"&options="+options+"\">");

				// print uberblock number
				if (valid)
				{
					if (tnr == lasttx)
					{
						append(green(Long.toString(tnr)));
					}
					else
					{
						append(Long.toString(tnr));
					}
				}
				else
				{
					append(red("invalid"));
				}
				append("</a></td>");
				if ((i & 15) == 15) append("</tr>");
			}

			// print nv-table
			append("</table><br><br>Name/Value Pairs<br>");
			DataHandlerNVTable.printNvTableToHtml(vnp, this);
		} 
		catch (Exception e) 
		{
			errorlog.println(e.getLocalizedMessage());
			e.printStackTrace(errorlog);
		}
	}

}
