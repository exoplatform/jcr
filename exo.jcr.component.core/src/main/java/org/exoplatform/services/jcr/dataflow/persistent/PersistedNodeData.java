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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import javax.jcr.RepositoryException;

import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.dataflow.ItemDataVisitor;
import org.exoplatform.services.jcr.datamodel.IllegalNameException;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.impl.Constants;


/**
 * Created by The eXo Platform SAS.</br>
 * 
 * Immutable NodeData from persistense storage
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id$
 */

public class PersistedNodeData extends PersistedItemData implements NodeData, Externalizable
{

   /**
    * serialVersionUID to serialization. 
    */
   private static final long serialVersionUID = 3033563403958948338L;
   
   private static final int ACL_IS_NULL = -1;
   
   private static final int ACL_IS_NOT_NULL = 1;

   protected int orderNumber;

   protected InternalQName primaryTypeName;

   protected InternalQName[] mixinTypeNames;

   protected AccessControlList acl;
   
   public PersistedNodeData()
   {
      super();
   }

   public PersistedNodeData(String id, QPath qpath, String parentId, int version, int orderNumber,
      InternalQName primaryTypeName, InternalQName[] mixinTypeNames, AccessControlList acl)
   {
      super(id, qpath, parentId, version);
      this.primaryTypeName = primaryTypeName;
      this.mixinTypeNames = mixinTypeNames;
      this.orderNumber = orderNumber;
      this.acl = acl;
   }

   /**
    * @see org.exoplatform.services.jcr.datamodel.NodeData#getOrderNumber()
    */
   public int getOrderNumber()
   {
      return orderNumber;
   }

   /**
    * @see org.exoplatform.services.jcr.datamodel.NodeData#getPrimaryTypeName()
    */
   public InternalQName getPrimaryTypeName()
   {
      return primaryTypeName;
   }

   /**
    * @see org.exoplatform.services.jcr.datamodel.NodeData#getMixinTypeNames()
    */
   public InternalQName[] getMixinTypeNames()
   {
      return mixinTypeNames;
   }

   /**
    * @see org.exoplatform.services.jcr.datamodel.NodeData#getACL()
    */
   public AccessControlList getACL()
   {
      return acl;
   }

   /**
    * @see org.exoplatform.services.jcr.datamodel.ItemData#accept(org.exoplatform.services.jcr.dataflow.ItemDataVisitor)
    */
   public void accept(ItemDataVisitor visitor) throws RepositoryException
   {
      visitor.visit(this);
   }

   /**
    * @see org.exoplatform.services.jcr.datamodel.ItemData#isNode()
    */
   public boolean isNode()
   {
      return true;
   }

   // ----------------- Externalizable

   /**
    * {@inheritDoc}
    */
   public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
   {
      super.readExternal(in);
      
      orderNumber = in.readInt();

      // primary type
      byte[] buf;
      try
      {
         buf = new byte[in.readInt()];
         in.readFully(buf);
         primaryTypeName = InternalQName.parse(new String(buf, Constants.DEFAULT_ENCODING));
      }
      catch (final IllegalNameException e)
      {
         throw new IOException(e.getMessage())
         {
            private static final long serialVersionUID = 3489809179234435267L;

            /**
             * {@inheritDoc}
             */
            @Override
            public Throwable getCause()
            {
               return e;
            }
         };
      }

      // mixins
      int count = in.readInt();
      mixinTypeNames = new InternalQName[count];
      for (int i = 0; i < count; i++)
      {
         try
         {
            buf = new byte[in.readInt()];
            in.readFully(buf);
            mixinTypeNames[i] = InternalQName.parse(new String(buf, Constants.DEFAULT_ENCODING));
         }
         catch (final IllegalNameException e)
         {
            throw new IOException(e.getMessage())
            {
               private static final long serialVersionUID = 3489809179234435268L; // eclipse

               // gen

               /**
                * {@inheritDoc}
                */
               @Override
               public Throwable getCause()
               {
                  return e;
               }
            };
         }
      }

      // acl
      if (in.readInt() == ACL_IS_NOT_NULL)
      {
         acl = new AccessControlList();
         acl.readExternal(in);
      }
      
   }

   public void writeExternal(ObjectOutput out) throws IOException
   {
      super.writeExternal(out);
      
      out.writeInt(orderNumber);

      // primary type
      byte[] ptbuf = primaryTypeName.getAsString().getBytes(Constants.DEFAULT_ENCODING);
      out.writeInt(ptbuf.length);
      out.write(ptbuf);

      // mixins
      out.writeInt(mixinTypeNames.length);
      for (int i = 0; i < mixinTypeNames.length; i++)
      {
         byte[] buf = mixinTypeNames[i].getAsString().getBytes(Constants.DEFAULT_ENCODING);
         out.writeInt(buf.length);
         out.write(buf);
      }

      if (acl == null)
      {
         out.writeInt(ACL_IS_NULL);
      } 
      else
      {
         out.writeInt(ACL_IS_NOT_NULL);
         acl.writeExternal(out);
      }
   }
}
