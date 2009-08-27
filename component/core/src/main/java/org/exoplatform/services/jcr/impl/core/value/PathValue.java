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
package org.exoplatform.services.jcr.impl.core.value;

import java.io.IOException;
import java.util.Calendar;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.impl.core.JCRPath;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;

/**
 * a <code>PATH</code> value impl (an absolute or relative workspace path).
 * 
 * @author Gennady Azarenkov
 */
public class PathValue
   extends BaseValue
{

   public static final int TYPE = PropertyType.PATH;

   private final LocationFactory locationFactory;

   public PathValue(QPath path, LocationFactory locationFactory) throws IOException
   {
      super(TYPE, new TransientValueData(path));
      this.locationFactory = locationFactory;
   }

   public PathValue(TransientValueData data, LocationFactory locationFactory) throws IOException, RepositoryException
   {
      super(TYPE, data);
      this.locationFactory = locationFactory;
   }

   /*
    * (non-Javadoc)
    * @see org.exoplatform.services.jcr.impl.core.value.BaseValue#getString()
    */
   public String getString() throws ValueFormatException, IllegalStateException, RepositoryException
   {
      JCRPath path = locationFactory.createJCRPath(getQPath());
      return path.getAsString(false);
   }

   /*
    * (non-Javadoc)
    * @see org.exoplatform.services.jcr.impl.core.value.BaseValue#getDate()
    */
   public Calendar getDate() throws ValueFormatException, IllegalStateException, RepositoryException
   {
      throw new ValueFormatException("conversion to date failed: inconvertible types");
   }

   /*
    * (non-Javadoc)
    * @see org.exoplatform.services.jcr.impl.core.value.BaseValue#getLong()
    */
   public long getLong() throws ValueFormatException, IllegalStateException, RepositoryException
   {
      throw new ValueFormatException("conversion to long failed: inconvertible types");
   }

   /*
    * (non-Javadoc)
    * @see org.exoplatform.services.jcr.impl.core.value.BaseValue#getBoolean()
    */
   public boolean getBoolean() throws ValueFormatException, IllegalStateException, RepositoryException
   {
      throw new ValueFormatException("conversion to boolean failed: inconvertible types");
   }

   /*
    * (non-Javadoc)
    * @see org.exoplatform.services.jcr.impl.core.value.BaseValue#getDouble()
    */
   public double getDouble() throws ValueFormatException, IllegalStateException, RepositoryException
   {
      throw new ValueFormatException("conversion to double failed: inconvertible types");
   }

   /**
    * @return qpath
    * @throws ValueFormatException
    * @throws IllegalStateException
    * @throws RepositoryException
    */
   public QPath getQPath() throws ValueFormatException, IllegalStateException, RepositoryException
   {
      return QPath.parse(getInternalString());
   }
}
