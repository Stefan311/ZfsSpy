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

import dataHandlers.DataHandlerNVTable;
import io.ZFSIO;

public class HttpHandlerVdevSummary extends HttpHandlerBase
{
	/*
	 * display device summary
	 */
	@Override
	protected void createHtmlBody() 
	{
		append("VDev Summary:<br>");
		append("<table border=1px><tr><td>Vdev</td><td>label nr</td><td>pool name</td><td>pool state</td><td>pool guid</td><td>transaction</td><td>top guid</td><td>vdev guid</td><td>uberblock</td></tr>");

		for (ZFSIO.Device _dev : ZFSIO.devices)
		{
			for (int j=0;j<=3;j++)
			{
				ByteBuffer _data;
				try 
				{
					_data = ZFSIO.getBlock(_dev, ZFSIO.getLabelSector(_dev,j), 256*1024);
					HashMap<String,Object> _vnp = DataHandlerNVTable.getNVTable(_data, 0x4000, 0x1c000);

					append("<tr><td>"+_dev.filename+"</td><td>L"+j+"</td><td>");
					Object o = _vnp.get("name");
					if (o instanceof String)
					{
						append((String)o);
					}
					else
					{
						append(red("not set!"));
					}
					append("</td><td>");
					o = _vnp.get("state");
					if (o instanceof Long)
					{
						switch (((Long) o).intValue())
						{
							case 0:  
								append("active");
								break;
							case 1:
								append("exported");
								break;
							case 2:
								append("destroyed");
								break;
							default:
								append(red("invalid state ("+(((Long) o).longValue())+")"));
								break;
						}
					}
					else
					{
						append(red("not set!"));
					}
		
					append("</td><td>");
					o = _vnp.get("pool_guid");
					append(o instanceof Long ? Long.toHexString(((Long)o).longValue()) : red("not set!"));
					
					append("</td><td>");
					o = _vnp.get("txg");
					append(o instanceof Long ? o.toString() : red("not set!"));
					
					append("</td><td>");
					o = _vnp.get("top_guid");
					append(o instanceof Long ? Long.toHexString(((Long)o).longValue()) : red("not set!"));
					
					append("</td><td>");
					o = _vnp.get("guid");
					append(o instanceof Long ? Long.toHexString(((Long)o).longValue()) : red("not set!"));
					
					append("</td><td>");
					append("<a href=\"labeldetails?vDev="+_dev.vDevId+"&labelNr="+j+"&options="+options+"\">explore</a>");
				} 
				catch (Exception e) 
				{
					errorlog.println(e.getLocalizedMessage());
					e.printStackTrace(errorlog);
				}
			}
		}
		append("</td></table>");
	}

}
