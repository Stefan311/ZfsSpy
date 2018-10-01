# ZfsSpy
Tool to recover data from damaged ZFS pools

What is ZFS?
------------
ZFS is a combined file system and logical volume manager designed by Sun Microsystems. ZFS is scalable, and includes extensive protection against data corruption, support for high storage capacities, efficient data compression, integration of the concepts of filesystem and volume management, snapshots and copy-on-write clones, continuous integrity checking and automatic repair, RAID-Z, native NFSv4 ACLs, and can be very precisely configured. The two main implementations, by Oracle and by the OpenZFS project, are extremely similar, making ZFS widely available within Unix-like systems. 

What you can do with this tool
------------------------------
- explore internal data structures from ZFS pools
- recover data from damaged ZFS pools
- recover deleted data from ZFS pools

What you can NOT do with this tool
----------------------------------
- repair damaged ZFS pools

How does this tool work?
------------------------
This tool works by accessing the raw ZFS devices. It provide a simple web-server for Web-GUI access.

[ZFS Device] <--file-access-- [ZfsSpy] <--http-- [Browser]

How to start the ZfsSpy
------------------------
It requires the JAVA runtime (7 or above), and a working network interface (at least loopback).  
Command to start:  
`java -jar ZfsSpy.jar [parameters]`

Parameters are:  
`-d output-directory`  
The directory to save recovered files. Default is the current working directory.  

`-i ip-address`  
Interface to bind the webserver. Default is 127.0.0.1 only. To access the ZfsSpy from outside, you have to chose a real IP.

`-p port`  
Portnumber to bind the webserver. Default is 8000.

`-v zfs-volume`  
Chose one (or more) ZFS devices for access. This can also be done on the Web-GUI later.

`-publicdemo`  
Disables device setup and save-to-server feature for the public demo version.  

Example 1:  
`java -jar ZfsSpy.jar -v /dev/da1 -v /dev/da2` and calling `http://127.0.0.1:8000/` in your browser.

Example 2:  
remote: `java -jar ZfsSpy.jar -i 192.168.16.4 -p 9999 -v /dev/da1 -v /dev/da2`  
local: Calling `http://192.168.16.4:9999/` in your browser.

Example 3:  
remote: `java -jar ZfsSpy.jar -v /dev/da1 -v /dev/da2`  
local: `ssh -L 8000:127.0.0.1:8000 me@192.168.16.4` and calling `http://127.0.0.1:8000/` in your browser.

