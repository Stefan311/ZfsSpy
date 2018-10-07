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

package io;
import java.nio.ByteBuffer;

public class Checksums 
{
	public static final String[] CHECKSUMNAMES = {"INHERIT","ON (FLETCHER 2)","OFF","LABEL","GANG HEADER","ZILOG","FLETCHER 2","FLETCHER 4","SHA256","ZILOG2","NOPARITY","SHA512","SKEIN","EDONR","FUNCTIONS"};

	/*
	 * gets the name of checksum algorithm
	 */
	public static String getName(int type)
	{
		if (type >=0 && type<CHECKSUMNAMES.length)
		{
			return CHECKSUMNAMES[type]+" ("+type+")";
		}
		return "Invalid ("+type+")";
	}
	
	/*
	 * create checksum
	 */
	public static long[] createChecksum(ByteBuffer data, int offset, int length, int type)
	{
		switch (type)
		{
			case 7: // ZIO_CHECKSUM_FLETCHER_4
				long[] s = new long[] {0,0,0,0};
				for (int i=0;i<length;i+=4)
				{
					s[0] += (data.getInt(offset + i) & 0xffffffffl);
					s[1] += s[0];
					s[2] += s[1];
					s[3] += s[2];
				}
				return s;
			default: // currently not supported
				/*
				ZIO_CHECKSUM_INHERIT = 0
				ZIO_CHECKSUM_ON = 1
				ZIO_CHECKSUM_OFF = 2
				ZIO_CHECKSUM_LABEL = 3
				ZIO_CHECKSUM_GANG_HEADER = 4
				ZIO_CHECKSUM_ZILOG = 5
				ZIO_CHECKSUM_FLETCHER_2 = 6
				ZIO_CHECKSUM_SHA256 = 8
				ZIO_CHECKSUM_ZILOG2 = 9
				ZIO_CHECKSUM_NOPARITY = 10
				ZIO_CHECKSUM_SHA512 = 11
				ZIO_CHECKSUM_SKEIN = 12
				ZIO_CHECKSUM_EDONR = 13
				ZIO_CHECKSUM_FUNCTIONS = 14
				*/
				return null;
		}
	}
}
