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
import java.nio.ByteOrder;

import net.jpountz.lz4.LZ4JavaSafeUnknownSizeDecompressor;

public class Compressions 
{
	private static final String[] COMPRESSNAMES = {"INHERIT","ON (LZJB)","OFF","LZJB","EMPTY","GZIP 1","GZIP 2","GZIP 3","GZIP 4","GZIP 5","GZIP 6","GZIP 7","GZIP 8","GZIP 9","ZLE","LZ4","FUNCTIONS"};
	
	/*
	 * get name of compression algorithm
	 */
	public static String getName(int type)
	{
		if (type >=0 && type<COMPRESSNAMES.length)
		{
			return COMPRESSNAMES[type]+" ("+type+")";
		}
		return "Invalid ("+type+")";
	}
	
	/*
	 * decompress block data
	 */
	public static ByteBuffer decompress(ByteBuffer in, int offset, int maxsize, int type)
	{
		switch (type)
		{
			case 15: //	ZIO_COMPRESS_LZ4
				ByteOrder oldBe = in.order();
				in.order(ByteOrder.BIG_ENDIAN);
				int clen = in.getInt(offset);
				in.order(oldBe);
				byte[] target = new byte[maxsize];
				int dlen = LZ4JavaSafeUnknownSizeDecompressor.INSTANCE.decompress(in.array(), offset+4, clen, target, 0, maxsize);
				return ByteBuffer.wrap(target, 0, dlen);
			case 2: // ZIO_COMPRESS_OFF
			case 4: // ZIO_COMPRESS_EMPTY
				byte[] tg = new byte[maxsize];
				in.get(tg, offset, maxsize);
				return ByteBuffer.wrap(tg);
			default : // not supported
				/*
				ZIO_COMPRESS_INHERIT = 0,
				ZIO_COMPRESS_ON,1
				ZIO_COMPRESS_LZJB,3
				ZIO_COMPRESS_GZIP_1,5
				ZIO_COMPRESS_GZIP_2,6
				ZIO_COMPRESS_GZIP_3,7
				ZIO_COMPRESS_GZIP_4,8
				ZIO_COMPRESS_GZIP_5,9
				ZIO_COMPRESS_GZIP_6,10
				ZIO_COMPRESS_GZIP_7,11
				ZIO_COMPRESS_GZIP_8,12
				ZIO_COMPRESS_GZIP_9,13
				ZIO_COMPRESS_ZLE,14
				ZIO_COMPRESS_FUNCTIONS,16
				*/
				return null;
		}
	}
}
