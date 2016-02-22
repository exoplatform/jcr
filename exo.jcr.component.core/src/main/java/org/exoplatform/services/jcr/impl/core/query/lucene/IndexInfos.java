/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.exoplatform.services.jcr.impl.core.query.lucene;

import org.apache.lucene.store.Directory;
import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.services.jcr.impl.core.query.lucene.directory.IndexInputStream;
import org.exoplatform.services.jcr.impl.core.query.lucene.directory.IndexOutputStream;
import org.exoplatform.services.jcr.impl.util.io.DirectoryHelper;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Stores a sequence of index names.
 */
public class IndexInfos
{

   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.IndexInfos");
   /**
    * Default file name, that is used if not defined;
    */
   public final static String DEFALUT_NAME = "indexes";

   /**
    * For new segment names.
    */
   private int counter;

   /**
    * Flag that indicates if index infos needs to be written to disk.
    */
   private boolean dirty;

   /**
    * List of index names
    */
   private List<String> indexes = new ArrayList<String>();

   /**
    * Set of names for quick lookup.
    */
   private Set<String> names = new HashSet<String>();

   /**
    * Name of the file where the infos are stored.
    */
   private final String name;

   /**
    * Directory, where index names file is stored.
    */
   private Directory dir;

   /**
    * {@link MultiIndex} instance for callbacking when list of indexes changed 
    */
   protected MultiIndex multiIndex;

   /**
    * Creates a new IndexInfos using <code>"indexes"</code> as a filename.
    */
   public IndexInfos()
   {
      this(DEFALUT_NAME);
   }

   /**
    * Creates a new IndexInfos using <code>fileName</code>.
    *
    * @param fileName the name of the file where infos are stored.
    */
   public IndexInfos(String fileName)
   {
      this.name = fileName;
   }

   /**
    * Returns the name of the file where infos are stored.
    *
    * @return the name of the file where infos are stored.
    */
   public String getFileName()
   {
      return name;
   }

   /**
    * Reads the index infos. Before reading it checks if file exists
    *
    * @throws IOException if an error occurs.
    */
   public void read() throws IOException
   {
      SecurityHelper.doPrivilegedIOExceptionAction(new PrivilegedExceptionAction<Object>()
      {
         public Object run() throws Exception
         {
            // Known issue for NFS bases on ext3. Need to refresh directory to read actual data.
            dir.listAll();

            names.clear();
            indexes.clear();
            if (dir.fileExists(name))
            {
               // clear current lists
               InputStream in = new IndexInputStream(dir.openInput(name));
               DataInputStream di = null;
               try
               {
                  di = new DataInputStream(in);
                  counter = di.readInt();
                  for (int i = di.readInt(); i > 0; i--)
                  {
                     String indexName = di.readUTF();
                     indexes.add(indexName);
                     names.add(indexName);
                  }
               }
               finally
               {
                  if (di != null)
                     di.close();
                  in.close();
               }
            }
            return null;
         }
      });
   }

   /**
    * Writes the index infos to disk if they are dirty.
    *
    * @throws IOException if an error occurs.
    */
   public void write() throws IOException
   {
      SecurityHelper.doPrivilegedIOExceptionAction(new PrivilegedExceptionAction<Object>()
      {
         public Object run() throws Exception
         {
            // do not write if not dirty
            if (!dirty)
            {
               return null;
            }

            OutputStream out = new IndexOutputStream(dir.createOutput(name + ".new"));
            DataOutputStream dataOut = null;
            try
            {
               dataOut = new DataOutputStream(out);
               dataOut.writeInt(counter);
               dataOut.writeInt(indexes.size());
               for (int i = 0; i < indexes.size(); i++)
               {
                  dataOut.writeUTF(getName(i));
               }
            }
            finally
            {
               if (dataOut != null)
                  dataOut.close();
               out.close();
            }
            // delete old
            if (dir.fileExists(name))
            {
               dir.deleteFile(name);
            }
            rename(name + ".new", name);
            dirty = false;
            return null;
         }
      });
   }

   /**
    * Renames file by copying.
    * 
    * @param from
    * @param to
    * @throws IOException
    */
   private void rename(String from, String to) throws IOException
   {
      IndexOutputStream out = null;
      IndexInputStream in = null;
      try
      {
         out = new IndexOutputStream(dir.createOutput(to));
         in = new IndexInputStream(dir.openInput(from));
         DirectoryHelper.transfer(in, out);
      }
      finally
      {
         if (in != null)
         {
            in.close();
         }

         if (out != null)
         {
            out.flush();
            out.close();
         }
      }
      try
      {
         // delete old one
         if (dir.fileExists(from))
         {
            dir.deleteFile(from);
         }
      }
      catch (IOException e)
      {
         // do noting. Will be removed later
         if (LOG.isTraceEnabled())
         {
            LOG.trace("Can't deleted file: " + e.getMessage());
         }
      }
   }

   /**
    * Returns the index name at position <code>i</code>.
    * @param i the position.
    * @return the index name.
    */
   public String getName(int i)
   {
      return indexes.get(i);
   }

   /**
    * Returns the index name at position <code>i</code>.
    * @return the index name.
    */
   public Set<String> getNames()
   {
      return new HashSet<String>(indexes);
   }

   /**
    * Returns the number of index names.
    * @return the number of index names.
    */
   public int size()
   {
      return indexes.size();
   }

   /**
    * Adds a name to the index infos.
    * @param name the name to add.
    */
   public void addName(String name)
   {
      if (names.contains(name))
      {
         throw new IllegalArgumentException("already contains: " + name);
      }
      indexes.add(name);
      names.add(name);
      dirty = true;
   }

   /**
    * Removes the name from the index infos.
    * @param name the name to remove.
    */
   public void removeName(String name)
   {
      indexes.remove(name);
      names.remove(name);
      dirty = true;
   }

   /**
    * Removes the name from the index infos.
    * @param i the position.
    */
   public void removeName(int i)
   {
      Object name = indexes.remove(i);
      names.remove(name);
      dirty = true;
   }

   /**
    * Returns <code>true</code> if <code>name</code> exists in this
    * <code>IndexInfos</code>; <code>false</code> otherwise.
    *
    * @param name the name to test existence.
    * @return <code>true</code> it is exists in this <code>IndexInfos</code>.
    */
   public boolean contains(String name)
   {
      return names.contains(name);
   }

   /**
    * Returns a new unique name for an index folder.
    * @return a new unique name for an index folder.
    */
   public String newName()
   {
      dirty = true;
      return "_" + Integer.toString(counter++, Character.MAX_RADIX);
   }

   /**
    * Sets directory, where file with index names is stored. 
    * @param dir
    */
   public void setDirectory(Directory dir)
   {
      this.dir = dir;
   }

   /**
    * Sets new names, clearing existing. It is thought to be used when list of indexes can
    * be externally changed.
    * 
    * @param names
    */
   protected void setNames(Set<String> names)
   {
      this.names.clear();
      this.indexes.clear();
      this.names.addAll(names);
      this.indexes.addAll(names);
      // new list of indexes if thought to be up to date
      dirty = false;
   }

   /**
    * Sets {@link MultiIndex} instance. 
    * @param multiIndex
    */
   public void setMultiIndex(MultiIndex multiIndex)
   {
      this.multiIndex = multiIndex;
   }

   /**
    * Returns multiIndex instance 
    * @return
    */
   public MultiIndex getMultiIndex()
   {
      return multiIndex;
   }

   /**
    * Returns true, if changes weren't saved to FS. 
    * @return
    */
   protected boolean isDirty()
   {
      return dirty;
   }
}
