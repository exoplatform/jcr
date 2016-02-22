/*
 * Copyright (C) 2009 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.services.jcr.dataflow.serialization;

import java.io.EOFException; //NOSONAR
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br>
 * Date: 13.02.2009
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: JCRObjectInput.java 111 2008-11-11 11:11:11Z rainf0x $
 */
public interface ObjectReader
{

   /**
    * Closes the input stream. Must be called to release any resources associated with the stream.
    * 
    * @exception IOException
    *              If an I/O error has occurred.
    */
   void close() throws IOException;

   /**
    * Reads some bytes from an input stream and stores them into the buffer array <code>b</code>. The
    * number of bytes read is equal to the length of <code>b</code>.
    * <p>
    * This method blocks until one of the following conditions occurs:
    * <p>
    * <ul>
    * <li><code>b.length</code> bytes of input data are available, in which case a normal return is
    * made.
    * 
    * <li>End of file is detected, in which case an <code>EOFException</code> is thrown.
    * 
    * <li>An I/O error occurs, in which case an <code>IOException</code> other than
    * <code>EOFException</code> is thrown.
    * </ul>
    * <p>
    * If <code>b</code> is <code>null</code>, a <code>NullPointerException</code> is thrown. If
    * <code>b.length</code> is zero, then no bytes are read. Otherwise, the first byte read is stored
    * into element <code>b[0]</code>, the next one into <code>b[1]</code>, and so on. If an exception
    * is thrown from this method, then it may be that some but not all bytes of <code>b</code> have
    * been updated with data from the input stream.
    * 
    * @param b
    *          the buffer into which the data is read.
    * @exception EOFException
    *              if this stream reaches the end before reading all the bytes.
    * @exception IOException
    *              if an I/O error occurs.
    */
   void readFully(byte b[]) throws IOException;

   /**
    * Read <code>length</code> bytes to the destination <code>stream</code> using NIO.
    * 
    * @param stream
    *          destination OutputStream
    * @param length
    *          long - bytes count to reaD
    * @return long - actual bytes readed count
    * @throws IOException
    *           if error occurs
    */
   long read(OutputStream stream, long length) throws IOException;

   /**
    * Reads one input byte and returns <code>true</code> if that byte is nonzero, <code>false</code>
    * if that byte is zero. This method is suitable for reading the byte written by the
    * <code>writeBoolean</code> method of interface <code>DataOutput</code>.
    * 
    * @return the <code>boolean</code> value read.
    * @exception EOFException
    *              if this stream reaches the end before reading all the bytes.
    * @exception IOException
    *              if an I/O error occurs.
    */
   boolean readBoolean() throws IOException;

   /**
    * Reads byte and returns a <code>byte</code> value.
    * 
    * @return the <code>byte</code> value read.
    * @exception EOFException
    *              if this stream reaches the end before reading all the bytes.
    * @exception IOException
    *              if an I/O error occurs.
    */
   byte readByte() throws IOException;

   /**
    * Reads four input bytes and returns an <code>int</code> value. Let <code>a</code> be the first
    * byte read, <code>b</code> be the second byte, <code>c</code> be the third byte, and
    * <code>d</code> be the fourth byte. The value returned is:
    * <p>
    * 
    * <pre>
    * &lt;code&gt;
    * (((a &amp; 0xff) &lt;&lt; 24) | ((b &amp; 0xff) &lt;&lt; 16) |
    *  ((c &amp; 0xff) &lt;&lt; 8) | (d &amp; 0xff))
    * &lt;/code&gt;
    * </pre>
    * 
    * This method is suitable for reading bytes written by the <code>writeInt</code> method of
    * interface <code>DataOutput</code>.
    * 
    * @return the <code>int</code> value read.
    * @exception EOFException
    *              if this stream reaches the end before reading all the bytes.
    * @exception IOException
    *              if an I/O error occurs.
    */
   int readInt() throws IOException;

   /**
    * Reads eight input bytes and returns a <code>long</code> value. Let <code>a</code> be the first
    * byte read, <code>b</code> be the second byte, <code>c</code> be the third byte, <code>d</code>
    * be the fourth byte, <code>e</code> be the fifth byte, <code>f</code> be the sixth byte,
    * <code>g</code> be the seventh byte, and <code>h</code> be the eighth byte. The value returned
    * is:
    * <p>
    * 
    * <pre>
    * &lt;code&gt;
    * (((long)(a &amp; 0xff) &lt;&lt; 56) |
    *  ((long)(b &amp; 0xff) &lt;&lt; 48) |
    *  ((long)(c &amp; 0xff) &lt;&lt; 40) |
    *  ((long)(d &amp; 0xff) &lt;&lt; 32) |
    *  ((long)(e &amp; 0xff) &lt;&lt; 24) |
    *  ((long)(f &amp; 0xff) &lt;&lt; 16) |
    *  ((long)(g &amp; 0xff) &lt;&lt;  8) |
    *  ((long)(h &amp; 0xff)))
    * &lt;/code&gt;
    * </pre>
    * <p>
    * This method is suitable for reading bytes written by the <code>writeLong</code> method of
    * interface <code>DataOutput</code>.
    * 
    * @return the <code>long</code> value read.
    * @exception EOFException
    *              if this stream reaches the end before reading all the bytes.
    * @exception IOException
    *              if an I/O error occurs.
    */
   long readLong() throws IOException;

   /**
    * Reads String. Constants.DEFAULT_ENCODING used.
    * 
    * @return String
    * @throws IOException
    *           if an I/O error occurs.
    */
   String readString() throws IOException;

   /**
    * Skip n bytes from stream.
    * 
    * @param n
    *          the number of bytes to be skipped.
    * @return the actual number of bytes skipped.
    * @exception IOException
    *              if an I/O error occurs.
    */
   long skip(long n) throws IOException;
}
