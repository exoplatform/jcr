/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.impl.core.query.ispn;

import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.infinispan.CacheKey;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Created by The eXo Platform SAS.
 *
 * Date: 22.02.011
 * 
 * @author <a href="mailto:anatoliy.bazko@exoplatform.com.ua">Anatoliy Bazko</a>
 * @version $Id: ChangesKey.java 34360 2010-11-11 11:11:11Z tolusha $
 */
public class ChangesKey extends CacheKey
{
   private String wsId;

   public ChangesKey()
   {
      super();
   }

   ChangesKey(String wsId, String id)
   {
      super(null, id);
      this.wsId = wsId;
   }

   /**
    * @return unique workspace identifier 
    */
   public String getWsId()
   {
      return wsId;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void writeExternal(ObjectOutput out) throws IOException
   {
      super.writeExternal(out);
      byte[] buf = wsId.getBytes(Constants.DEFAULT_ENCODING);
      out.writeInt(buf.length);
      out.write(buf);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
   {
      super.readExternal(in);
      byte[] buf = new byte[in.readInt()];
      in.readFully(buf);
      wsId = new String(buf, Constants.DEFAULT_ENCODING);
   }
}
