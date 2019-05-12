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

What you can _NOT_ do with this tool
----------------------------------
- repair damaged ZFS pools

How does this tool work?
------------------------
This tool works by accessing the raw ZFS devices. It provide a simple web-server for Web-GUI access.

[ZFS Device] <--file-access-- [ZfsSpy] <--http-- [Browser]

How to start the ZfsSpy
------------------------
It requires the JAVA runtime (7 or above), and a working network interface (at least loopback).  
**Command to start:**  
`java -jar ZfsSpy.jar [parameters]`

**Parameters are:**  
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

**Example 1:**  
`java -jar ZfsSpy.jar -v /dev/sda1 -v /dev/sda2` and calling `http://127.0.0.1:8000/` in your browser.

**Example 2:**  
remote: `java -jar ZfsSpy.jar -i 192.168.16.4 -p 9999 -v /dev/sda1 -v /dev/sda2`  
local: Calling `http://192.168.16.4:9999/` in your browser.

**Example 3:**  
remote: `java -jar ZfsSpy.jar -v /dev/sda1 -v /dev/sda2`  
local: `ssh -L 8000:127.0.0.1:8000 me@192.168.16.4` and calling `http://127.0.0.1:8000/` in your browser.

Public demo
-----------
You can show a public demo on https://imoriath.com/zfsspy/

Exploring ZFS pools
-------------------
There are 2 exploration modes: **Block Mode** and **Filesystem Mode**.  

In **Block Mode** you have to follow block pointers to finally access your files. This is very difficoult, but it works on very damaged pools too.  
1. Device Label Overview --> chose one label
2. Device Label Detail + Uberblock table --> chose one uberblock. The most rescent is marked yellow.
3. **Block Pointer Tree** to the master object list  
A block pointer tree starts with one single DMU object as root, continues with multiple levels of blockpointer lists as branches, and ends to a object list as leafes.  
4. The master object list contains between other object types also **DSL DATASET (16)** objects, who pointing to the ZFS datasets, again with the use of a block pointer tree.
5. Each dataset object list contains 
- **DIRECTORY CONTENTS (20)** objects who holding file and directory names
- **PLAIN FILE CONTENTS (19)** objects who holding the file data. Each file content object have a link to download the file data or to save the file data to disk. In block mode the ZfsSpy does not know the filename, a random name will be used.

In **Filesystem Mode** the ZfsSpy tool does the blockpointer hunting automatic.  
Chose a ZFS dataset, browse through the directories. On files you can download or save the file data. On dirctories you can enter the directory or save all containing file data to disk.

You can also switch between Block and Filesystem mode at some points.

Missing Features
----------------
The ZfsSpy is in an early stage of development, so many important features are still missing:  
- All Compressions except LZ4
- All Checksumms except Fletcher4
- Display of file/directory attributes
- Complex pool geometries like RAIDZ or mixed STRIPE+MIRROR
- usefull help pages on every step
- more fault tolerance
- encryption support
- gangblock support

Known Issues
------------
- Raw devices are not accessable in FreeBSD, please use image files as workarround
