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
package org.exoplatform.services.jcr.impl.dataflow;

import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.datamodel.Identifier;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.util.io.SpoolFile;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;

/**
 * Created by The eXo Platform SAS.<br/>
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id$
 */
public class TransientValueData implements ValueData
{

   protected final static Log LOG = ExoLogger.getLogger("exo.jcr.component.core.TransientValueData");

   protected ValueData delegate;

   /**
    * TransientValueData constructor.
    */
   public TransientValueData(int orderNumber, InputStream stream, SpoolFile spoolFile, SpoolConfig spoolConfig,
      boolean closeTmpStream) throws IOException
   {
      this.delegate = new StreamNewValueData(orderNumber, stream, spoolFile, spoolConfig, closeTmpStream);
   }

   /**
    * TransientValueData constructor.
    * 
    * @param stream
    *          InputStream
    * @throws IOException 
    */
   public TransientValueData(InputStream stream, SpoolConfig spoolConfig) throws IOException
   {
      this(0, stream, null, spoolConfig, false);
   }

   /**
    * Creates TransientValueData with incoming input stream. the stream will be lazily spooled to
    * file or byte array depending on maxBufferSize.
    * 
    * @param orderNumber
    *          int
    * @param stream
    *          InputStream
    * @throws IOException 
    */
   public TransientValueData(int orderNumber, InputStream stream, SpoolConfig spoolConfig) throws IOException
   {
      this(orderNumber, stream, null, spoolConfig, false);
   }

   /**
    * Creates TransientValueData with incoming byte array.
    * 
    * @param value
    *          byte[]
    */
   public TransientValueData(byte[] value)
   {
      this.delegate = new ByteArrayNewValueData(0, value);
   }

   /**
    * Creates TransientValueData with incoming byte array.
    * @param orderNumber
    *          int
    * @param value
    *          byte[]
    */
   public TransientValueData(int orderNumber, byte[] value)
   {
      this.delegate = new ByteArrayNewValueData(orderNumber, value);
   }

   /**
    * Constructor for String value data.
    * 
    * @param value
    *          String
    */
   public TransientValueData(String value)
   {
      this.delegate = new StringNewValueData(0, value);
   }

   /**
    * Constructor for String value data.
    * 
    * @param orderNumber
    *          int 
    * @param value
    *          String
    */
   public TransientValueData(int orderNumber, String value)
   {
      this.delegate = new StringNewValueData(orderNumber, value);
   }

   /**
    * Constructor for boolean value data.
    * 
    * @param value
    *          boolean
    */
   public TransientValueData(boolean value)
   {
      this.delegate = new BooleanNewValueData(0, value);
   }

   /**
    * Constructor for boolean value data.
    * 
    * @param orderNumber
    *          int
    * @param value
    *          boolean
    */
   public TransientValueData(int orderNumber, boolean value)
   {
      this.delegate = new BooleanNewValueData(orderNumber, value);
   }

   /**
    * Constructor for Calendar value data.
    * 
    * @param value
    *          Calendar
    */
   public TransientValueData(Calendar value)
   {
      this.delegate = new CalendarNewValueData(0, value);
   }

   /**
    * Constructor for Calendar value data.
    * 
    * @param orderNumber
    *          int
    * @param value
    *          Calendar
    */
   public TransientValueData(int orderNumber, Calendar value)
   {
      this.delegate = new CalendarNewValueData(orderNumber, value);
   }

   /**
    * Constructor for double value data.
    * 
    * @param value
    *          double
    */
   public TransientValueData(double value)
   {
      this.delegate = new DoubleNewValueData(0, value);
   }

   /**
    * Constructor for double value data.
    * 
    * @param orderNumber
    *          int
    * @param value
    *          double
    */
   public TransientValueData(int orderNumber, double value)
   {
      this.delegate = new DoubleNewValueData(orderNumber, value);
   }

   /**
    * Constructor for long value data.
    * 
    * @param value
    *          long
    */
   public TransientValueData(long value)
   {
      this.delegate = new LongNewValueData(0, value);
   }

   /**
    * Constructor for long value data.
    * 
    * @param orderNumber
    *          int
    * @param value
    *          long
    */
   public TransientValueData(int orderNumber, long value)
   {
      this.delegate = new LongNewValueData(orderNumber, value);
   }

   /**
    * Constructor for Name value data.
    * 
    * @param value
    *          InternalQName
    */
   public TransientValueData(InternalQName value)
   {
      this.delegate = new StringNewValueData(0, value.getAsString());
   }

   /**
    * Constructor for Name value data.
    *
    * @param orderNumber
    *          int
    * @param value
    *          InternalQName
    */
   public TransientValueData(int orderNumber, InternalQName value)
   {
      this.delegate = new StringNewValueData(orderNumber, value.getAsString());
   }

   /**
    * Constructor for Path value data.
    * 
    * @param value
    *          QPath
    */
   public TransientValueData(QPath value)
   {
      this.delegate = new StringNewValueData(0, value.getAsString());
   }

   /**
    * Constructor for Path value data.
    * 
    * @param orderNumber
    *          int
    * @param value
    *          QPath
    */
   public TransientValueData(int orderNumber, QPath value)
   {
      this.delegate = new StringNewValueData(orderNumber, value.getAsString());
   }

   /**
    * Constructor for Reference value data.
    * 
    * @param value
    *          Identifier
    */
   public TransientValueData(Identifier value)
   {
      this.delegate = new StringNewValueData(0, value.getString());
   }

   /**
    * Constructor for Reference value data.
    * 
    * @param orderNumber
    *          int
    * @param value
    *          Identifier
    */
   public TransientValueData(int orderNumber, Identifier value)
   {
      this.delegate = new StringNewValueData(orderNumber, value.getString());
   }

   /**
    * Constructor for Permission value data.
    * 
    * @param value
    *          AccessControlEntry
    */
   public TransientValueData(AccessControlEntry value)
   {
      this.delegate = new StringNewValueData(0, value.getAsString());
   }

   /**
    * Constructor for Permission value data.
    * 
    * @param orderNumber
    *          int
    * @param value
    *          AccessControlEntry
    */
   public TransientValueData(int orderNumber, AccessControlEntry value)
   {
      this.delegate = new StringNewValueData(orderNumber, value.getAsString());
   }

   /**
    * Constructor for Editable value data.
    */
   protected TransientValueData()
   {
   }

   /**
    * {@inheritDoc}
    */
   public byte[] getAsByteArray() throws IllegalStateException, IOException
   {
      return delegate.getAsByteArray();
   }

   /**
    * {@inheritDoc}
    */
   public InputStream getAsStream() throws IOException
   {
      return delegate.getAsStream();
   }

   /**
    * {@inheritDoc}
    */
   public long getLength()
   {
      return delegate.getLength();
   }

   /**
    * {@inheritDoc}
    */
   public boolean isByteArray()
   {
      return delegate.isByteArray();
   }

   /**
    * {@inheritDoc}
    */
   public long read(OutputStream stream, long length, long position) throws IOException
   {
      return delegate.read(stream, length, position);
   }

   /**
    * {@inheritDoc}
    */
   public int getOrderNumber()
   {
      return delegate.getOrderNumber();
   }

   /**
    * Re-init this TransientValueData with another value.  
    * 
    * @param newValue ValueData
    */
   public void delegate(ValueData newValue)
   {
      this.delegate = newValue;
   }

   /**
    * Return temporary spool file. Can be null. For persistent operations on newly created data only.
    * 
    * @return File temp file
    */
   public SpoolFile getSpoolFile()
   {
      if (delegate instanceof StreamNewValueData)
      {
         return ((StreamNewValueData)delegate).spoolFile;
      }

      return null;
   }

   /**
    * Get original stream. Can be consumed or null. 
    * WARN: method for persistent operations on modified ValueData only.
    * 
    * @return InputStream original stream
    */
   public InputStream getOriginalStream()
   {
      if (delegate instanceof StreamNewValueData)
      {
         return ((StreamNewValueData)delegate).tmpStream;
      }

      return null;
   }

   /**
    * {@inheritDoc}
    */
   public boolean equals(ValueData another)
   {
      if (this == another)
      {
         return true;
      }

      return delegate.equals(another);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean equals(Object obj)
   {
      if (obj instanceof ValueData)
      {
         return equals((ValueData)obj);
      }

      return false;
   }
}