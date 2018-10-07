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
import java.util.Calendar;

import httpHandlers.HttpHandlerBase;
import io.RedBlockPointer;

public class DataHandlerBonusBuffer 
{
	/*
	 * prints bonus buffer data to html
	 */
	public static void printBonusBufferToHtml(int _bonustype, ByteBuffer data, int _offset, int _bonuslen, HttpHandlerBase _html)
	{
		Calendar cal = Calendar.getInstance();
		switch (_bonustype)
		{
			case 12: //dsl_dir_phys
				_html.append("<table border=1px>");
				cal.setTimeInMillis(data.getLong(_offset) * 1000);
				_html.append("<tr><td>Creation time</td><td>"+cal.getTime().toString()+"</td></tr>");
				_html.append("<tr><td>Head dataset obj</td><td>"+data.getLong(_offset+8)+"</td></tr>");
				_html.append("<tr><td>Parent obj</td><td>"+data.getLong(_offset+16)+"</td></tr>");
				_html.append("<tr><td>Origin obj</td><td>"+data.getLong(_offset+24)+"</td></tr>");
				_html.append("<tr><td>Child dir zapobj</td><td>"+data.getLong(_offset+32)+"</td></tr>");
				_html.append("<tr><td>Used bytes</td><td>"+data.getLong(_offset+40)+"</td></tr>");
				_html.append("<tr><td>Compressed bytes</td><td>"+data.getLong(_offset+48)+"</td></tr>");
				_html.append("<tr><td>Uncompressed bytes</td><td>"+data.getLong(_offset+56)+"</td></tr>");
				_html.append("<tr><td>Admin quota</td><td>"+data.getLong(_offset+64)+"</td></tr>");
				_html.append("<tr><td>Admin reserved</td><td>"+data.getLong(_offset+72)+"</td></tr>");
				_html.append("<tr><td>Props zapobj</td><td>"+data.getLong(_offset+80)+"</td></tr>");
				_html.append("<tr><td>Deleg zapobj</td><td>"+data.getLong(_offset+88)+"</td></tr>");
				_html.append("<tr><td>Flags</td><td>"+data.getLong(_offset+96)+"</td></tr>");
				_html.append("<tr><td>Used breakdown #1</td><td>"+data.getLong(_offset+104)+"</td></tr>");
				_html.append("<tr><td>Used breakdown #2</td><td>"+data.getLong(_offset+112)+"</td></tr>");
				_html.append("<tr><td>Used breakdown #3</td><td>"+data.getLong(_offset+120)+"</td></tr>");
				_html.append("<tr><td>Used breakdown #4</td><td>"+data.getLong(_offset+128)+"</td></tr>");
				_html.append("<tr><td>Used breakdown #5</td><td>"+data.getLong(_offset+136)+"</td></tr>");
				_html.append("<tr><td>clones</td><td>"+data.getLong(_offset+144)+"</td></tr>");
				_html.append("</table>");
				break;
			case 16: //dsl_dataset_phys
				_html.append("<table border=1px>");
				_html.append("<tr><td>Dir Obj</td><td>"+data.getLong(_offset)+"</td></tr>");
				_html.append("<tr><td>Prev snap obj</td><td>"+data.getLong(_offset+8)+"</td></tr>");
				_html.append("<tr><td>Prev snap txg</td><td>"+data.getLong(_offset+16)+"</td></tr>");
				_html.append("<tr><td>Next snap obj</td><td>"+data.getLong(_offset+24)+"</td></tr>");
				_html.append("<tr><td>Snapnames zapobj</td><td>"+data.getLong(_offset+32)+"</td></tr>");
				_html.append("<tr><td>Num children</td><td>"+data.getLong(_offset+40)+"</td></tr>");
				_html.append("<tr><td>Creation time</td><td>"+data.getLong(_offset+48)+"</td></tr>");
				_html.append("<tr><td>Creation txg</td><td>"+data.getLong(_offset+56)+"</td></tr>");
				_html.append("<tr><td>Deadlist obj</td><td>"+data.getLong(_offset+64)+"</td></tr>");
				_html.append("<tr><td>Referenced bytes</td><td>"+data.getLong(_offset+72)+"</td></tr>");
				_html.append("<tr><td>Compressed bytes</td><td>"+data.getLong(_offset+80)+"</td></tr>");
				_html.append("<tr><td>Uncompressed bytes</td><td>"+data.getLong(_offset+88)+"</td></tr>");
				_html.append("<tr><td>Unique bytes</td><td>"+data.getLong(_offset+96)+"</td></tr>");
				_html.append("<tr><td>Fsid guid</td><td>"+data.getLong(_offset+104)+"</td></tr>");
				_html.append("<tr><td>Guid</td><td>"+data.getLong(_offset+112)+"</td></tr>");
				_html.append("<tr><td>Flags</td><td>"+data.getLong(_offset+120)+"</td></tr>");
				_html.append("<tr><td>Block pointer</td><td>");
				DataHandlerBlockPointer.printBlkptrToHtml(new RedBlockPointer(data, _offset+128), _html);
				_html.append("</td></td>");
				_html.append("<tr><td>Next clones obj</td><td>"+data.getLong(_offset+256)+"</td></tr>");
				_html.append("<tr><td>Props obj</td><td>"+data.getLong(_offset+264)+"</td></tr>");
				_html.append("<tr><td>Userrefs obj</td><td>"+data.getLong(_offset+272)+"</td></tr>");
				_html.append("</table>");
				break;
			case 44: // file attributes
				_html.append("<table border=1px>");
				_html.append("<tr><td>Data length</td><td>"+data.getLong(_offset+16)+"</td></tr>");
				_html.append("</table>");
				// TODO: find out file attribut coding, implement display 
				DataHandlerHexdump.printHexdumpToHtml(data,_offset, _bonuslen, _html);
				break;
			default: // hex dump
				DataHandlerHexdump.printHexdumpToHtml(data,_offset, _bonuslen, _html);
				break;
		}
	}
}
