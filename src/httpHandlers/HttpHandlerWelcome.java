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
public class HttpHandlerWelcome extends HttpHandlerBase
{
	/*
	 * displays the welcome screen
	 */
	@Override
	protected void createHtmlBody() 
	{
		append("Welcome to the ZFS Spy (Revision 07.10.2018)<br><br>");
		append("With this tool you can explore internal data structures from ZFS devices to recover data from damaged pools.<br>");
		if (publicdemo)
		{
			append("The tool itself is written in java, acts as simple http-server. In this case it is chained to apache http-proxy for demonstration.<br><br>");
			append("Revision log:<br><br>");
			append("07.10.2018<br>-Added file and directory storage. <font color=blue>Full file recovery possible!</font><br>-Some code cleanup<br>-Source now public on <a href=\"https://github.com/Stefan311/ZfsSpy\">Github</a>.<br><br>");
			append("29.09.2018<br>-Added last filesystem browsing (directories). <font color=blue>Full filesystem browsing possible!</font><br><br>");
			append("25.09.2018<br>-Added first filesystem browsing (dataset summary)<br><br>");
			append("22.09.2018<br>-Added bigzap-handling<br>-Added embedded data handling<br>-Added helpfile-loader, 3 very basic help files<br><br>");
			append("16.09.2018<br>-Added objectlist-caching<br>-Added basic multiblock handling<br>-Implemented file download option. <font color=blue>First file recovery possible!</font><br>-Moved Uberblock-table up<br><br>");
			append("14.09.2018<br>-Added microzap handling<br>-changed menu outfit<br>-Added commandline options<br>-Added Setup-Page<br><br>");
			append("11.09.2018<br>-Corrected object type names<br>-disabled bad/empty block links<br>-implement DSL DIR and DSL DATASET decoder. <font color=blue>Now the first file content is readable!</font><br><br>");
			append("09.09.2018<br>-first public version<br><br>");
		}
	}
}
