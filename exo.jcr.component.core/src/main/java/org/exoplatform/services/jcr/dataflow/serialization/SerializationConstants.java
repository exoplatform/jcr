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

import org.exoplatform.services.jcr.impl.util.io.PrivilegedSystemHelper;

import java.io.File;

/**
 * Created by The eXo Platform SAS. <br/>
 * Date: 13.02.2009
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: SerializationConstants.java 34445 2009-07-24 07:51:18Z dkatayev $
 */
public class SerializationConstants
{

   /**
    * Serialization temp dir.
    */
   public static final String TEMP_DIR =
      PrivilegedSystemHelper.getProperty("java.io.tmpdir") + File.separator + "_jcrser.tmp";

   /**
    * TransientValueData class.
    */
   public static final int TRANSIENT_VALUE_DATA = 1;

   /**
    * AccessControlList class.
    */
   public static final int ACCESS_CONTROL_LIST = 2;

   /**
    * ItemState class.
    */
   public static final int ITEM_STATE = 3;

   /**
    * PlainChangesLogImpl class.
    */
   public static final int PLAIN_CHANGES_LOG_IMPL = 4;

   /**
    * TransactionChangesLog class.
    */
   public static final int TRANSACTION_CHANGES_LOG = 5;

   /**
    * TransientNodeData class.
    */
   public static final int TRANSIENT_NODE_DATA = 7;

   /**
    * TransientPropertyData class.
    */
   public static final int TRANSIENT_PROPERTY_DATA = 8;

   /**
    * PersistedNodeData class.
    */
   public static final int PERSISTED_NODE_DATA = 9;

   /**
    * PersistedPropertyData class.
    */
   public static final int PERSISTED_PROPERTY_DATA = 10;

   /**
    * PersistedValueData class.
    */
   public static final int PERSISTED_VALUE_DATA = 11;

   /**
    * Null data.
    */
   public static final byte NULL_DATA = 0;

   /**
    * Not null data.
    */
   public static final byte NOT_NULL_DATA = 1;

   /**
    * Serialization bytebuffer size.
    */
   public static final int INTERNAL_BUFFER_SIZE = 2048;
}
