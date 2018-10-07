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

import httpHandlers.HttpHandlerBase;

public class DataHandlerHexdump 
{
	/**
	 * print binary data as hexdump to html
	 */
	public static void printHexdumpToHtml(ByteBuffer data, int offset, int length, HttpHandlerBase http)
	{
		http.append("<table>");
		String a = "";
		String s;
		
		for (int i=0;i<length;i++)
		{
			if ((i & 31) == 0)
			{
				http.append("<tr><td>");
				s = "000000000000000" + Integer.toHexString(i);
				s = s.substring(s.length() - Integer.toHexString(length).length());
				http.append(s.toUpperCase());
				http.append("</td><td>:</td>");
			}
			
			s = "0"+Integer.toHexString(data.get(i+offset) & 0xff);
			s = s.substring(s.length()-2);
			
			if (data.get(i+offset)>=32 && data.get(i+offset)<=126)
			{
				a += (char)data.get(i+offset);
			}
			else
			{
				a += ".";
			}
			
			http.append("<td>");
			http.append(s.toUpperCase());
			http.append("</td>");
			
			if ((i & 31) == 31)
			{
				http.append("<td> </td><td>");
				http.append(a);
				a = "";
				http.append("</td></tr>");
			}
		}
		http.append("</table>");
	}
	
}
