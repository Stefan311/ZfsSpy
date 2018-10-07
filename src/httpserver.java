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

import java.io.File;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;

import httpHandlers.HttpHandlerDatasetSummary;
import httpHandlers.HttpHandlerBase;
import httpHandlers.HttpHandlerBlock;
import httpHandlers.HttpHandlerDownload;
import httpHandlers.HttpHandlerFileList;
import httpHandlers.HttpHandlerDataset;
import httpHandlers.HttpHandlerHelp;
import httpHandlers.HttpHandlerLabelDetails;
import httpHandlers.HttpHandlerSetup;
import httpHandlers.HttpHandlerStoreDir;
import httpHandlers.HttpHandlerStoreFile;
import httpHandlers.HttpHandlerUberblock;
import httpHandlers.HttpHandlerVdevSummary;
import httpHandlers.HttpHandlerWelcome;
import io.ZFSIO;

public class httpserver 
{
	/**
	 * - parse and apply command line options
	 * - setup http server
	 * - register all http path handlers
	 */
	public static void main(String[] args) throws Exception 
	{
		int _port = 8000;
		String _ip = "127.0.0.1";
		boolean _err = false;
		
		try
		{
			for (int i=0;i<args.length;i++)
			{
				switch (args[i])
				{
					case "-p":
						if (i<args.length-1)
						{
							_port = Integer.parseInt(args[i+1]);
							i++;
						}
						else
						{
							_err = true;
						}
						break;
					case "-i":
						if (i<args.length-1)
						{
							_ip = args[i+1];
							i++;
						}
						else
						{
							_err = true;
						}
						break;
					case "-v":
						if (i<args.length-1)
						{
							ZFSIO.addDevice(args[i+1]);
							System.out.println("Device: "+args[i+1]);
							i++;
						}
						else
						{
							_err = true;
						}
						break;
					case "-d":
						if (i<args.length-1)
						{
							String _dir = args[i+1];
							HttpHandlerBase.outDir = _dir;
							File f = new File(_dir);
							System.out.println("Output Directory: "+_dir);
							if (!f.exists())
							{
								System.err.println("Output Directory does not exist!");
								_err = true;
							}
							if (!f.isDirectory())
							{
								System.err.println("Output Directory is not a directory!");
								_err = true;
							}
							if (!f.canWrite())
							{
								System.err.println("Output Directory is not writeable!");
								_err = true;
							}
							i++;
						}
						else
						{
							_err = true;
						}
						break;
					case "-publicdemo":
						System.out.println("Public demo modus enabled");
						HttpHandlerBase.publicdemo = true;
						break;
					default:
						_err = true;
						break;
				}
			}
		}
		catch (Exception e)
		{
			_err = true;
		}
		
		if (_err)
		{
			System.out.println("Wrong parameter!\nUsage: java -jar ZFSSpy.jar [params...]\nwhere params are:");
			System.out.println("-p port     : port to bind the webserver, default 8000");
			System.out.println("-i ip       : ip to bind the webserver, default 127.0.0.1, need to be a real address if you want to reach it from outside!");
			System.out.println("-v device   : pre-add a ZFS blockdevice, no default, new devices can be add/removed from web later");
			System.out.println("-d directory: set output directory for file recovery, default is the current working directory");
			System.out.println("-publicdemo : switch to public demo mode. Some web-actions will be blocked.");
			return;
		}

		HttpServer server = HttpServer.create(new InetSocketAddress(_ip,_port), 0);
        server.createContext("/", new HttpHandlerWelcome());
        server.createContext("/vdevsummary", new HttpHandlerVdevSummary());
        server.createContext("/labeldetails", new HttpHandlerLabelDetails());
        server.createContext("/uberblock", new HttpHandlerUberblock());
        server.createContext("/block", new HttpHandlerBlock());
        server.createContext("/setup", new HttpHandlerSetup());
        server.createContext("/datasetsummary", new HttpHandlerDatasetSummary());
        server.createContext("/help", new HttpHandlerHelp());
        server.createContext("/dataset", new HttpHandlerDataset());
        server.createContext("/loaddata", new HttpHandlerDownload());
        server.createContext("/storedata", new HttpHandlerStoreFile());
        server.createContext("/storedir", new HttpHandlerStoreDir());
        server.createContext("/filelist", new HttpHandlerFileList());
        
        server.setExecutor(null);
        server.start();
        System.out.println("Bound to "+_ip+" Port "+_port);
	}

}
