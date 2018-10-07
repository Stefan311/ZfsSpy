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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URLDecoder;
import java.util.HashMap;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import io.ZFSIO;

public abstract class HttpHandlerBase implements HttpHandler
{
	// some global variables
	public static boolean publicdemo = false;
	public static String outDir = ".";

	// builder for html text
	public StringBuilder response;

	// name/value list of the current http-get parameters
	public HashMap<String,String> params = new HashMap<>();
	
	// for error logging, will be printed red on the bottom of the html page 
	public PrintStream errorlog;
	
	// some shortcuts to common http-get parameters
	protected int vDevId;
	protected ZFSIO.Device device;
	protected long sector;
	protected int csize;
	protected int dsize;
	protected int compress;
	protected int endian;
	protected int objtype;
	public String options = "00000";
	
	/*
	 * holds the requested url
	 */
	public String request = "";
	
	/*
	 * this function will create the main section of the html page
	 */
	protected abstract void createHtmlBody();
	
	/*
	 * wraps a string in <font color=red> tages
	 */
	public String red(String i)
	{
		return "<font color=red>"+i+"</font>";
	}
	
	/*
	 * wraps a string in <font color=green> tages
	 */
	public String green(String i)
	{
		return "<font color=green>"+i+"</font>";
	}
	
	/*
	 * replace the value of a url parameter with an other 
	 */
	private String getLinkReplaceParam(String paramName, String newValue)
	{
		String r = null;

		for (String p : params.keySet())
		{
			if (r == null)
			{
				r = request + "?";
			}
			else
			{
				r += "&";
			}
			
			if (paramName.equals(p))
			{
				r += p + "=" + newValue;
			}
			else
			{
				r += p + "=" + params.get(p);
			}
		}
		return r;
	}

	/*
	 * mark link bold if objecttype match
	 */
	private void addMarkedObjTypeLink(String _text, int _type)
	{
		if (objtype == _type)
		{
			append("<b>");
		}
		append("<a href=\"."+getLinkReplaceParam("objtype",Integer.toString(_type))+"\">"+_text+"</a>");
		if (objtype == _type)
		{
			append("</b>");
		}
		append("<br>");
	}
	
	/*
	 * create the navigation section of the html 
	 */
	private void createHtmlNavi()
	{
		append("<table bgcolor=AliceBlue><tr><td width=200>Navigation</td><td width=400>Options</td><td width=400>View type</td></tr>");
		append("<tr valign=\"top\"><td><a href=\".\">Home</a><br>");
		append("<a href=\"./setup\">Setup devices</a><br>");
		append("<a href=\"./vdevsummary\">Browse Blocks</a><br>");
		append("<a href=\"./datasetsummary\">Browse Filesystem</a><br>");
		append("<a href=\"./help?file="+(request==null || request.length()==1 ? "main" : request.substring(1))+"\">Help</a><br>");
		append("</td><td>");
		if (params.containsKey("options"))
		{
			if ("1".equals(options.substring(0,1)))
			{
				append("<a href=\"."+getLinkReplaceParam("options","0"+options.substring(1))+"\">Blockpointer: Long-&gt;Short</a><br>");
			}
			else
			{
				append("<a href=\"."+getLinkReplaceParam("options","1"+options.substring(1))+"\">Blockpointer: Short-&gt;Long</a><br>");
			}
		}
		append("</td><td>");
		if (params.containsKey("objtype"))
		{
			addMarkedObjTypeLink("Block-Table",-2);
			addMarkedObjTypeLink("Hexdump",-1);
			addMarkedObjTypeLink("generic ZAP",-3);
			addMarkedObjTypeLink("NVLIST (3)",3);
			addMarkedObjTypeLink("DNODE (10)",10);
			addMarkedObjTypeLink("OBJSET (11)",11);
		}
		append("</td></tr></table><br><br>");
	}
	
	/*
	 * handles the http-request
	 */
	@Override
	public void handle(HttpExchange html) throws IOException 
	{
		response = new StringBuilder(65536);
		request = html.getRequestURI().getPath();

		// parse request parameters
		params.clear();
		String p1 = html.getRequestURI().getQuery();
		if (p1 != null)
		{
			String[] p2 = p1.split("&");
			for (String p3 : p2)
			{
				String[] p4 = p3.split("=");
				if (p4.length == 2)
				{
					params.put(p4[0], p4[1]);
				}
			}
		}

		// parse transmitted values to the parameter list if http POST
		if ("POST".equals(html.getRequestMethod()))
		{
			byte[] buf = new byte[1024];
			int len = html.getRequestBody().read(buf);
			String p5 = new String(buf,0,len);
			String[] p2 = p5.split("&");
			for (String p3 : p2)
			{
				String[] p4 = p3.split("=");
				if (p4.length == 2)
				{
					params.put(p4[0], URLDecoder.decode(p4[1],"UTF8"));
				}
			}
		}
		
		ByteArrayOutputStream errors = new ByteArrayOutputStream(1024); 
		errorlog = new PrintStream(errors);
		
		// create http page header
		append("<html><head><title>ZFS Spy</title><style>");
		append("table, th, td {border-collapse: collapse;}");
		append("</style></head><body><h1>ZFS Spy</h1>");
		
		try
		{
			// get common parameters
			String s;
			if ((s = params.get("vDev")) != null)
			{
				vDevId = Integer.parseInt(s);
				device = ZFSIO.getDevice(vDevId);
			}
			if ((s = params.get("sector")) != null)	sector = Long.parseLong(s);
			if ((s = params.get("csize")) != null)	csize = Integer.parseInt(s);
			if ((s = params.get("dsize")) != null)	dsize = Integer.parseInt(s);
			if ((s = params.get("compress")) != null)	compress = Integer.parseInt(s);
			if ((s = params.get("endian")) != null)	endian = Integer.parseInt(s);
			if ((s = params.get("objtype")) != null)	objtype = Integer.parseInt(s);
			if ((s = params.get("options")) != null)	options = s;

			// create navigation section
			createHtmlNavi();
			
			// create main section (by sub-classes)
			createHtmlBody();
		}
		catch (Exception e) 
		{
			errorlog.println(e.getLocalizedMessage());
			e.printStackTrace(errorlog);
		}

		// add error text (if any)
        if (errors.size()>0)
        {
        	append("<br>Errors:<br>");
        	append(red(errors.toString().replaceAll("\n", "<br>")));
        }
        
        // add html footer
        append("</body></html>");
        
        // send html to client
        byte[] b = response.toString().getBytes();
        html.sendResponseHeaders(200, b.length);
        OutputStream out = html.getResponseBody();
        out.write(b);
        out.close();
	}
	
	/*
	 *  shortcut funktion to add text to the html response
	 */
	public void append(String s) 
	{
		response.append(s);
	}

	/*
	 *  shortcut to log errors
	 */
	public void logError(String s) 
	{
		errorlog.println(s);
		System.err.println(s);
	}

	/*
	 *  shortcut to console output
	 */
	public void log(String s)
	{
		System.out.println(s);
	}
}
