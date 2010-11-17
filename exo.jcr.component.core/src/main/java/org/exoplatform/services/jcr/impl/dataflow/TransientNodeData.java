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

/**
 * Created by The eXo Platform SAS. <br>
 * Newly added Node's data (used for mock inmemory repository as well). Besides
 * NodeData's methods includes child items adders
 * 
 * @author Gennady Azarenkov
 * @version $Id: TransientNodeData.java 11907 2008-03-13 15:36:21Z ksm $
 */

import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.dataflow.ItemDataVisitor;
import org.exoplatform.services.jcr.datamodel.IllegalNameException;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.util.IdGenerator;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import javax.jcr.RepositoryException;

public class TransientNodeData extends TransientItemData implements Comparable, NodeData, ItemData, Externalizable
{

   private static final long serialVersionUID = -8675118546441306180L;

   protected AccessControlList acl;

   protected InternalQName primaryTypeName;

   protected InternalQName[] mixinTypeNames;

   protected int orderNum;

   public TransientNodeData(QPath path, String identifier, int version, InternalQName primaryTypeName,
      InternalQName[] mixinTypeNames, int orderNum, String parentIdentifier, AccessControlList acl)
   {
      super(path, identifier, version, parentIdentifier);
      this.primaryTypeName = primaryTypeName;
      this.mixinTypeNames = mixinTypeNames;
      this.orderNum = orderNum;
      this.acl = acl;
   }

   // --------------- ItemData ------------

   /**
    * {@inheritDoc}
    */
   public boolean isNode()
   {
      return true;
   }

   // ---------------- NodeData -------------

   /**
    * {@inheritDoc}
    */
   public int getOrderNumber()
   {
      return orderNum;
   }

   /**
    * {@inheritDoc}
    */
   public InternalQName getPrimaryTypeName()
   {
      return primaryTypeName;
   }

   /**
    * {@inheritDoc}
    */
   public InternalQName[] getMixinTypeNames()
   {
      return mixinTypeNames;
   }

   /**
    * {@inheritDoc}
    */
   public AccessControlList getACL()
   {
      return acl;
   }

   /**
    * {@inheritDoc}
    */
   public void accept(ItemDataVisitor visitor) throws RepositoryException
   {
      visitor.visit(this);
   }

   /**
    * Factory method.
    * 
    * @param parent NodeData
    * @param name  InternalQName
    * @param primaryTypeName InternalQName
    * @param mixinTypeNames InteralnQName[]
    * @param identifier String
    * @return TransientNodeData
    */
   public static TransientNodeData createNodeData(NodeData parent, InternalQName name, InternalQName primaryTypeName,
      InternalQName[] mixinTypeNames, String identifier)
   {
      TransientNodeData nodeData = null;
      QPath path = QPath.makeChildPath(parent.getQPath(), name);
      nodeData =
         new TransientNodeData(path, identifier, -1, primaryTypeName, mixinTypeNames, 0, parent.getIdentifier(), parent
            .getACL());
      return nodeData;
   }

   /**
    * Factory method.
    * 
    * @param parent NodeData
    * @param name  InternalQName
    * @param primaryTypeName InternalQName
    * @return TransientNodeData
    */
   public static TransientNodeData createNodeData(NodeData parent, InternalQName name, InternalQName primaryTypeName)
   {
      TransientNodeData nodeData = null;
      QPath path = QPath.makeChildPath(parent.getQPath(), name);
      nodeData =
         new TransientNodeData(path, IdGenerator.generate(), -1, primaryTypeName, new InternalQName[0], 0, parent
            .getIdentifier(), parent.getACL());
      return nodeData;
   }

   /**
    * Factory method.
    * 
    * @param parent NodeData
    * @param name  InternalQName
    * @param primaryTypeName InternalQName
    * @param mixinTypesName InternalQName[]
    * @return
    */
   public static TransientNodeData createNodeData(NodeData parent, InternalQName name, InternalQName primaryTypeName,
      InternalQName[] mixinTypesName)
   {
      TransientNodeData nodeData = null;
      QPath path = QPath.makeChildPath(parent.getQPath(), name);
      nodeData =
         new TransientNodeData(path, IdGenerator.generate(), -1, primaryTypeName, mixinTypesName, 0, parent
            .getIdentifier(), parent.getACL());
      return nodeData;
   }

   /**
    * Factory method
    * 
    * @param parent NodeData
    * @param name  InternalQName
    * @param primaryTypeName InternalQName
    * @param index int
    * @return
    */
   public static TransientNodeData createNodeData(NodeData parent, InternalQName name, InternalQName primaryTypeName,
      int index)
   {
      TransientNodeData nodeData = null;
      QPath path = QPath.makeChildPath(parent.getQPath(), name, index);
      nodeData =
         new TransientNodeData(path, IdGenerator.generate(), -1, primaryTypeName, new InternalQName[0], 0, parent
            .getIdentifier(), parent.getACL());
      return nodeData;
   }

   /**
    * Factory method
    * 
    * @param parent NodeData
    * @param name  InternalQName
    * @param primaryTypeName InternalQName
    * @param index int
    * @param orderNumber int
    * @return
    */
   public static TransientNodeData createNodeData(NodeData parent, InternalQName name, InternalQName primaryTypeName,
      int index, int orderNumber)
   {
      TransientNodeData nodeData = null;
      QPath path = QPath.makeChildPath(parent.getQPath(), name, index);
      nodeData =
         new TransientNodeData(path, IdGenerator.generate(), -1, primaryTypeName, new InternalQName[0], orderNumber,
            parent.getIdentifier(), parent.getACL());
      return nodeData;
   }

   /**
    * Factory method
    * 
    * @param parent NodeData
    * @param name  InternalQName
    * @param primaryTypeName InternalQName
    * @param identifier String
    * @return
    */
   public static TransientNodeData createNodeData(NodeData parent, InternalQName name, InternalQName primaryTypeName,
      String identifier)
   {
      TransientNodeData nodeData = null;
      QPath path = QPath.makeChildPath(parent.getQPath(), name);
      nodeData =
         new TransientNodeData(path, identifier, -1, primaryTypeName, new InternalQName[0], 0, parent.getIdentifier(),
            parent.getACL());
      return nodeData;
   }

   /**
    * Factory method
    * 
    * @param parent NodeData
    * @param name  InternalQName
    * @param primaryTypeName InternalQName
    * @param identifier String
    * @param acl AccessControlList
    * @return
    */
   public static TransientNodeData createNodeData(NodeData parent, InternalQName name, InternalQName primaryTypeName,
      String identifier, AccessControlList acl)
   {
      TransientNodeData nodeData = null;
      QPath path = QPath.makeChildPath(parent.getQPath(), name);
      nodeData =
         new TransientNodeData(path, identifier, -1, primaryTypeName, new InternalQName[0], 0, parent.getIdentifier(),
            acl);
      return nodeData;
   }

   // ------------- Comparable /////

   public int compareTo(Object obj)
   {
      return ((NodeData)obj).getOrderNumber() - orderNum;
   }

   // Need for Externalizable
   // ------------------ [ BEGIN ] ------------------
   public TransientNodeData()
   {
      super();
      this.acl = new AccessControlList();
   }

   @Override
   public void writeExternal(ObjectOutput out) throws IOException
   {
      super.writeExternal(out);

      out.writeInt(orderNum);

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

      acl.writeExternal(out);
   }

   @Override
   public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
   {
      super.readExternal(in);

      orderNum = in.readInt();

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
      acl.readExternal(in);
   }

   // ------------------ [ END ] ------------------

}
