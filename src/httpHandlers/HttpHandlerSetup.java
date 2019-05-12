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
import io.ZFSIO;

public class HttpHandlerSetup extends HttpHandlerBase 
{
	/*
	 * manages device setup page
	 */
	@Override
	protected void createHtmlBody() 
	{
		// remove device
		if (params.containsKey("remove") && !publicdemo)
		{
			try
			{
				int i = Integer.parseInt(params.get("remove"));
				ZFSIO.Device dev = ZFSIO.getDevice(i);
				if (dev.reader != null && dev.reader.isOpen())
				{
					dev.reader.close();
				}
				ZFSIO.devices.remove(dev);
			}
			catch (Exception e)
			{
				errorlog.println(e.getLocalizedMessage());
			}
		}
		
		// add device
		if (params.containsKey("add") && !publicdemo)
		{
			String s = null;
			try
			{
				s = params.get("add");
				ZFSIO.addDevice(s);
			}
			catch (Exception e)
			{
				errorlog.println(e.getLocalizedMessage());
			}
		}
		
		// print device list
		append("Current devices:<br>");
		if (ZFSIO.devices.size() == 0)
		{
			append(red("none"));
		}
		else
		{
			append("<table border=1px><tr><td>Filename</td><td>Size</td><td>vDevId</td><td>Action</td></tr>");
			for (ZFSIO.Device dev : ZFSIO.devices)
			{
				append("<tr><td>"+dev.filename+"</td><td>"+dev.size+"</td><td>"+dev.vDevId+"</td><td>");
				append("<form action=\"setup\" method=\"post\"><input type=\"hidden\" name=\"remove\" value=\""+dev.vDevId+"\"><input type=\"submit\" value=\"remove"+(publicdemo?"*":"")+"\"></form></td></tr>");
			}
			append("</table>");
		}
		
		// print add/remove form html 
		append("<br><br>Add new device<br><form action=\"setup\" method=\"post\"><input type=\"text\" name=\"add\"><input type=\"submit\" value=\"add"+(publicdemo?"*":"")+"\"></form><br><br>");
		if (publicdemo)
		{
			append(red("*not aviable in public demo modus"));
		}
	}

}
