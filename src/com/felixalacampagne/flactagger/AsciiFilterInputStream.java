package com.felixalacampagne.flactagger;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class AsciiFilterInputStream extends FilterInputStream
{
private final static byte REPLACE_CHAR = 0x2A;  // 0x2A - ASCII asterix
private final static byte QUOTE_CHAR = 0x27; // 0x27 = ASCII single quote
private final static byte DOUBLEQUOTE_CHAR = 0x22; // 0x22 - ASCI double quote
	public AsciiFilterInputStream(InputStream in)
	{
		super(in);
	}
	
	public int read() throws IOException 
   {
   	int b = in.read();
   	if(b > 126)
		{
   		b = REPLACE_CHAR;
		}   	
   	return b;
   }


   public int read(byte bytes[], int off, int len) throws IOException 
   {
	  int bytesin = in.read(bytes, off, len);
	  int butf8 = 0xE2 & 0xFF;
	  int blastascii =  0x7E & 0xFF;
	  
	  // Aaaagggghhhhh. Any byte greater than 127 has to be converted to an integer
	  // in order to do sensible comparisons as bytes with the high bit set are
	  // interpreted as being negative. So the test (0xE2 > 0x7E) return false!!!!
	  // NB tests for equals work as expected.
	  
	  for(int i=0; i < bytesin; i++)
	  {
		  // Simplest will be to zap values >=127. But some might be valid UTF8
		  // which could perhaps be kept - but since this filter is for
		  // for tagging FLACs and I don't want any characters that need to be
		  // UTF8'd so will get rid of everything... 
		  // But maybe I can provide some translations - the most annoying one
		  // is the apostrophe instead of the single quote...
		  // u2019 - 0xE2 0x80 0x99 - right single quotation
		  int b = bytes[i] & 0xFF;
		  if((b == butf8) && ((bytesin - i) > 2))
		  {
			  if((bytes[i+1] == (byte) 0x80))
			  {
				  if (bytes[i+2] == (byte) 0x99)
				  {
					  bytes[i] = QUOTE_CHAR; // 0x27 = ASCII single quote
				  }
				  else if ((bytes[i+2] == (byte) 0x9D) || (bytes[i+2] == (byte) 0x9C)) // Right/Left double quote
				  {
					  bytes[i] = DOUBLEQUOTE_CHAR;
				  }
				  else
				  {
					  // Not a recognised UTF8 sequence. Replace E2, the other high bit values will be replace by
					  // byte the next iterations. There will be sequences of three or more *s, but too bad. 
					  bytes[i] = REPLACE_CHAR;
					  continue;
				  }
					  
				  bytesin-=2;  // Making the data two bytes shorter!!
				  System.arraycopy(bytes, i+3, bytes, i+1, bytesin - i);
				  //System.out.println(new String(bytes, 0, bytesin));
				  continue;
			  }
		  }
		  
		  if(b > blastascii)
		  {
			  bytes[i] = REPLACE_CHAR;
		  }
	  }
	  return bytesin;
  }
}
