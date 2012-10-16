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
import org.exoplatform.services.jcr.impl.dataflow.persistent.PersistedValueData;
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
   /**
    * Logger.
    */
   protected final static Log LOG = ExoLogger.getLogger("exo.jcr.component.core.TransientValueData");

   /**
    * Delegated value data. At session level it is instance of {@link NewValueData}. At save it becomes
    * instance of {@link PersistedValueData}.
    */
   protected AbstractValueData delegate;

   /**
    * TransientValueData constructor.
    */
   public TransientValueData(int orderNumber, InputStream stream, SpoolFile spoolFile, SpoolConfig spoolConfig)
      throws IOException
   {
      this.delegate = new StreamNewValueData(orderNumber, stream, spoolFile, spoolConfig);
   }

   /**
    * TransientValueData constructor.
    */
   public TransientValueData(InputStream stream, SpoolConfig spoolConfig) throws IOException
   {
      this(0, stream, null, spoolConfig);
   }

   /**
    * Creates TransientValueData with incoming input stream. the stream will be lazily spooled to
    * file or byte array depending on maxBufferSize.
    */
   public TransientValueData(int orderNumber, InputStream stream, SpoolConfig spoolConfig) throws IOException
   {
      this(orderNumber, stream, null, spoolConfig);
   }

   /**
    * Creates TransientValueData with incoming byte array.
    */
   public TransientValueData(byte[] value)
   {
      this.delegate = new ByteArrayNewValueData(0, value);
   }

   /**
    * Creates TransientValueData with incoming byte array.
    */
   public TransientValueData(int orderNumber, byte[] value)
   {
      this.delegate = new ByteArrayNewValueData(orderNumber, value);
   }

   /**
    * Constructor for String value data.
    */
   public TransientValueData(String value)
   {
      this.delegate = new StringNewValueData(0, value);
   }

   /**
    * Constructor for String value data.
    */
   public TransientValueData(int orderNumber, String value)
   {
      this.delegate = new StringNewValueData(orderNumber, value);
   }

   /**
    * Constructor for boolean value data.
    */
   public TransientValueData(boolean value)
   {
      this.delegate = new BooleanNewValueData(0, value);
   }

   /**
    * Constructor for boolean value data.
    */
   public TransientValueData(int orderNumber, boolean value)
   {
      this.delegate = new BooleanNewValueData(orderNumber, value);
   }

   /**
    * Constructor for Calendar value data.
    */
   public TransientValueData(Calendar value)
   {
      this.delegate = new CalendarNewValueData(0, value);
   }

   /**
    * Constructor for Calendar value data.
    */
   public TransientValueData(int orderNumber, Calendar value)
   {
      this.delegate = new CalendarNewValueData(orderNumber, value);
   }

   /**
    * Constructor for double value data.
    */
   public TransientValueData(double value)
   {
      this.delegate = new DoubleNewValueData(0, value);
   }

   /**
    * Constructor for double value data.
    */
   public TransientValueData(int orderNumber, double value)
   {
      this.delegate = new DoubleNewValueData(orderNumber, value);
   }

   /**
    * Constructor for long value data.
    */
   public TransientValueData(long value)
   {
      this.delegate = new LongNewValueData(0, value);
   }

   /**
    * Constructor for long value data.
    */
   public TransientValueData(int orderNumber, long value)
   {
      this.delegate = new LongNewValueData(orderNumber, value);
   }

   /**
    * Constructor for Name value data.
    */
   public TransientValueData(InternalQName value)
   {
      this.delegate = new NameNewValueData(0, value);
   }

   /**
    * Constructor for Name value data.
    */
   public TransientValueData(int orderNumber, InternalQName value)
   {
      this.delegate = new NameNewValueData(orderNumber, value);
   }

   /**
    * Constructor for Path value data.
    */
   public TransientValueData(QPath value)
   {
      this.delegate = new PathNewValueData(0, value);
   }

   /**
    * Constructor for Path value data.
    */
   public TransientValueData(int orderNumber, QPath value)
   {
      this.delegate = new PathNewValueData(orderNumber, value);
   }

   /**
    * Constructor for Reference value data.
    */
   public TransientValueData(Identifier value)
   {
      this.delegate = new ReferenceNewValueData(0, value);
   }

   /**
    * Constructor for Reference value data.
    */
   public TransientValueData(int orderNumber, Identifier value)
   {
      this.delegate = new ReferenceNewValueData(orderNumber, value);
   }

   /**
    * Constructor for Permission value data.
    */
   public TransientValueData(AccessControlEntry value)
   {
      this.delegate = new PermissionNewValueData(0, value);
   }

   /**
    * Constructor for Permission value data.
    */
   public TransientValueData(int orderNumber, AccessControlEntry value)
   {
      this.delegate = new PermissionNewValueData(orderNumber, value);
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
    */
   public void delegate(AbstractValueData newValue)
   {
      this.delegate = newValue;
   }

   /**
    * Creates persisted copy of value. {@link AbstractValueData#createPersistedCopy(int)}
    */
   public PersistedValueData createPersistedCopy(int orderNumber) throws IOException
   {
      return delegate.createPersistedCopy(orderNumber);
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

   /**
    * {@inheritDoc}
    */
   @Override
   public void finalize() throws Throwable
   {
      super.finalize();
   }
}