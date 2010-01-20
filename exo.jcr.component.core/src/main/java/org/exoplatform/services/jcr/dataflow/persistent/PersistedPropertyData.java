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
package org.exoplatform.services.jcr.dataflow.persistent;

import org.exoplatform.services.jcr.dataflow.ItemDataVisitor;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.dataflow.AbstractPersistedValueData;
import org.exoplatform.services.jcr.impl.dataflow.TransientItemData;
import org.exoplatform.services.jcr.impl.dataflow.TransientPropertyData;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.</br>
 * 
 * Persisted PropertyData
 * 
 * @author Gennady Azarenkov
 * @version $Id$
 */

public class PersistedPropertyData extends PersistedItemData implements PropertyData, Externalizable
{

   /**
    * serialVersionUID to serialization. 
    */
   private static final long serialVersionUID = 2035566403758848232L;

   protected final static int NULL_VALUES = -1;

   protected List<ValueData> values;

   protected int type;

   protected boolean multiValued;

   /**
    * Empty constructor to serialization.
    */
   public PersistedPropertyData()
   {
      super();
   }

   public PersistedPropertyData(String id, QPath qpath, String parentId, int version, int type, boolean multiValued,
      List<ValueData> values)
   {
      super(id, qpath, parentId, version);
      this.values = values;
      this.type = type;
      this.multiValued = multiValued;
   }

   /**
    * {@inheritDoc}
    */
   public int getType()
   {
      return type;
   }

   /**
    * {@inheritDoc}
    */
   public List<ValueData> getValues()
   {
      return values;
   }

   /**
    * {@inheritDoc}
    */
   public boolean isMultiValued()
   {
      return multiValued;
   }

   /**
    * {@inheritDoc}
    */
   public boolean isNode()
   {
      return false;
   }

   /**
    * {@inheritDoc}
    */
   public void accept(ItemDataVisitor visitor) throws RepositoryException
   {
      visitor.visit(this);
   }

   // ----------------- Externalizable

   /**
    * {@inheritDoc}
    */
   public void writeExternal(ObjectOutput out) throws IOException
   {
      super.writeExternal(out);

      out.writeInt(type);
      out.writeBoolean(multiValued);

      if (values != null)
      {
         int listSize = values.size();
         out.writeInt(listSize);
         for (int i = 0; i < listSize; i++)
         {
            out.writeObject(values.get(i));
         }
      }
      else
      {
         out.writeInt(NULL_VALUES);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
   {
      super.readExternal(in);

      type = in.readInt();

      multiValued = in.readBoolean();

      int listSize = in.readInt();
      if (listSize != NULL_VALUES)
      {
         values = new ArrayList<ValueData>();
         for (int i = 0; i < listSize; i++)
         {
            values.add((ValueData)in.readObject());
         }
      }
   }
}
