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
import java.util.Calendar;

import dataHandlers.DataHandlerBlockPointer;
import io.ZFSIO;
import io.RedBlockPointer;

public class HttpHandlerUberblock extends HttpHandlerBase 
{
	/*
	 * display uberblock details
	 */
	@Override
	protected void createHtmlBody() 
	{
		int labelnr = Integer.parseInt(params.get("labelNr"));
		int ubnr = Integer.parseInt(params.get("uberBl"));

		append("VDev: "+device.filename+", Label: L"+labelnr+", Uberblock: "+ubnr+"<br><br>");
		
		try
		{
			// load whole disklabel
			ByteBuffer data = ZFSIO.getBlock(device, ZFSIO.getLabelSector(device, labelnr), 256*1024);
			data.order(endian==1 ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
			
			append("Uberblock<br><table border=1px>");
			
			// display uberblock data
			long a = data.getLong(0x20000 + ubnr*0x400);
			append("<tr><td>Magic Number</td><td>"+Long.toHexString(a)+" ");
			append(a == 0x00bab10c ? green("valid") : red("invalid"));
			append("</td></tr><tr><td>Version</td><td>");
			append(Long.toString(data.getLong(0x20008 + ubnr*0x400)));
			append("</td></tr><tr><td>Transaction</td><td>");
			append(Long.toString(data.getLong(0x20010 + ubnr*0x400)));
			append("</td></tr><tr><td>GUID Checksum</td><td>");
			append(Long.toHexString(data.getLong(0x20018 + ubnr*0x400)));
			append("</td></tr><tr><td>Timestamp</td><td>");
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(data.getLong(0x20020 + ubnr*0x400) * 1000);
			append(cal.getTime().toString());
			append("</td></tr><tr><td>Pointers</td><td>");
			DataHandlerBlockPointer.printBlkptrToHtml(new RedBlockPointer(data, 0x20028 + ubnr*0x400), this);
			append("</td></tr></table>");
		}
		catch (Exception e) 
		{
			errorlog.println(e.getLocalizedMessage());
			e.printStackTrace(errorlog);
		}
	}

}
