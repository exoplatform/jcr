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
package org.exoplatform.services.jcr.impl.dataflow.persistent;

import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.impl.Constants;

/**
 * Created by The eXo Platform SAS. <br/>
 * 
 * Store QPath as key in cache.
 * 
 * 15.06.07
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: CacheQPath.java 13869 2008-05-05 08:40:10Z pnedonosko $
 */
class CacheQPath
   extends CacheKey
{

   // private static final String BASE = String.valueOf(UUID.randomUUID().hashCode()) + "-";

   // static {
   // UUID rnd = UUID.randomUUID();
   //    
   // byte[] rb = new byte[16];
   // rb[0] = (byte) (rnd.getMostSignificantBits() >>> 56 & 0x0f);
   // rb[1] = (byte) (rnd.getMostSignificantBits() >>> 48 & 0x0f);
   // rb[2] = (byte) (rnd.getMostSignificantBits() >>> 40 & 0x0f);
   // rb[3] = (byte) (rnd.getMostSignificantBits() >>> 32 & 0xff);
   // rb[4] = (byte) (rnd.getMostSignificantBits() >>> 24 & 0xff);
   // rb[5] = (byte) (rnd.getMostSignificantBits() >>> 16 & 0xff);
   // rb[6] = (byte) (rnd.getMostSignificantBits() >>> 8 & 0xff);
   // rb[7] = (byte) (rnd.getMostSignificantBits() & 0xff);
   //    
   // rb[8] = (byte) (rnd.getLeastSignificantBits() >>> 56 & 0x0f);
   // rb[9] = (byte) (rnd.getLeastSignificantBits() >>> 48 & 0xff);
   // rb[10] = (byte) (rnd.getLeastSignificantBits() >>> 40 & 0xff);
   // rb[11] = (byte) (rnd.getLeastSignificantBits() >>> 32 & 0xff);
   // rb[12] = (byte) (rnd.getLeastSignificantBits() >>> 24 & 0xff);
   // rb[13] = (byte) (rnd.getLeastSignificantBits() >>> 16 & 0xff);
   // rb[14] = (byte) (rnd.getLeastSignificantBits() >>> 8 & 0xff);
   // rb[15] = (byte) (rnd.getLeastSignificantBits() & 0xff);
   //    
   // //BASE = new String(rb);
   // }

   private final String parentId;

   private final QPath path;

   private final String key;

   /**
    * For CPath will be stored in cache C
    */
   CacheQPath(String parentId, QPath path)
   {
      this.parentId = parentId;
      this.path = path;
      this.key = key(this.parentId, this.path.getEntries());
   }

   /**
    * For CPath will be searched in cache C
    */
   CacheQPath(String parentId, QPathEntry name)
   {
      this.parentId = parentId;
      this.path = null;
      this.key = key(this.parentId, name);
   }

   protected String key(final String parentId, final QPathEntry[] pathEntries)
   {
      return key(parentId, pathEntries[pathEntries.length - 1]);
   }

   protected String key(final String parentId, final QPathEntry name)
   {
      StringBuilder sk = new StringBuilder();
      // sk.append(BASE); for strong hash code, skip it when equals uses String.equals
      sk.append(parentId != null ? parentId : Constants.ROOT_PARENT_UUID);
      sk.append(name.getAsString(true));
      return sk.toString();
   }

   @Override
   public boolean equals(Object obj)
   {
      if (key.hashCode() == obj.hashCode() && obj instanceof CacheQPath)
         return key.equals(((CacheQPath) obj).key);
      return false;
   }

   @Override
   public int hashCode()
   {
      return key.hashCode();
   }

   @Override
   public String toString()
   {
      final StringBuilder s = new StringBuilder();
      s.append((this.parentId != null ? this.parentId : Constants.ROOT_PARENT_UUID));
      s.append((path != null ? path.getEntries()[path.getEntries().length - 1] : "null"));
      s.append(", ");
      s.append(key);
      return s.toString();
   }

   QPath getQPath()
   {
      return path;
   }

   @Override
   boolean isDescendantOf(QPath path)
   {
      return this.path.isDescendantOf(path);
   }
}
