/*
 * Copyright (C) 2011 eXo Platform SAS.
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
package org.exoplatform.services.jcr.ext.repository.creation;

import org.exoplatform.services.jcr.impl.Constants;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: DBCreationProperties.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public class DBCreationProperties implements StorageCreationProperties
{
   private String serverUrl;

   private Map<String, String> connProps;

   private String dbScriptPath;

   private String dbUserName;

   private String dbPassword;

   /**
    * Constructor DBCreationProperties. 
    */
   public DBCreationProperties(String serverUrl, Map<String, String> connProps, String dbScriptPath, String dbUserName,
      String dbPassword)
   {
      this.serverUrl = serverUrl;
      this.connProps = connProps;
      this.dbScriptPath = dbScriptPath;
      this.dbUserName = dbUserName;
      this.dbPassword = dbPassword;
   }

   /**
    * Constructor DBCreationProperties. 
    */
   public DBCreationProperties()
   {
   }

   /**
    * Returns script path.
    */
   public String getDBScriptPath()
   {
      return dbScriptPath;
   }

   /**
    * Returns return username for new database.
    */
   public String getDBUserName()
   {
      return dbUserName;
   }

   /**
    * Returns new user's password.
    */
   public String getDBPassword()
   {
      return dbPassword;
   }

   /**
    * Returns url to db server.
    */
   public String getServerUrl()
   {
      return serverUrl;
   }

   /**
    * Returns connection properties.
    */
   public Map<String, String> getConnProps()
   {
      return Collections.unmodifiableMap(connProps);
   }

   /**
    * {@inheritDoc}
    */
   public void writeExternal(ObjectOutput out) throws IOException
   {
      byte[] buf = serverUrl.getBytes(Constants.DEFAULT_ENCODING);
      out.writeInt(buf.length);
      out.write(buf);

      buf = dbScriptPath.getBytes(Constants.DEFAULT_ENCODING);
      out.writeInt(buf.length);
      out.write(buf);

      buf = dbUserName.getBytes(Constants.DEFAULT_ENCODING);
      out.writeInt(buf.length);
      out.write(buf);

      buf = dbPassword.getBytes(Constants.DEFAULT_ENCODING);
      out.writeInt(buf.length);
      out.write(buf);

      out.writeInt(connProps.size());
      for (Entry<String, String> entry : connProps.entrySet())
      {
         buf = entry.getKey().getBytes(Constants.DEFAULT_ENCODING);
         out.writeInt(buf.length);
         out.write(buf);

         buf = entry.getValue().getBytes(Constants.DEFAULT_ENCODING);
         out.writeInt(buf.length);
         out.write(buf);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
   {
      byte[] buf = new byte[in.readInt()];
      in.readFully(buf);
      serverUrl = new String(buf, Constants.DEFAULT_ENCODING);

      buf = new byte[in.readInt()];
      in.readFully(buf);
      dbScriptPath = new String(buf, Constants.DEFAULT_ENCODING);

      buf = new byte[in.readInt()];
      in.readFully(buf);
      dbUserName = new String(buf, Constants.DEFAULT_ENCODING);

      buf = new byte[in.readInt()];
      in.readFully(buf);
      dbPassword = new String(buf, Constants.DEFAULT_ENCODING);

      int count = in.readInt();
      connProps = new HashMap<String, String>(count);

      for (int i = 0; i < count; i++)
      {
         buf = new byte[in.readInt()];
         in.readFully(buf);
         String key = new String(buf, Constants.DEFAULT_ENCODING);

         buf = new byte[in.readInt()];
         in.readFully(buf);
         String value = new String(buf, Constants.DEFAULT_ENCODING);

         connProps.put(key, value);
      }
   }
}
