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

package org.exoplatform.services.jcr.impl.dataflow;

import org.exoplatform.services.jcr.JcrImplBaseTest;
import org.exoplatform.services.jcr.dataflow.persistent.PersistedItemData;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.impl.core.NodeImpl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br>Date: 21.01.2009
 *
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a> 
 * @version $Id$
 */
public class TestExternalizabeItemData extends JcrImplBaseTest
{

   public void testPersistedItemData() throws Exception
   {
      ItemData idSrc = ((NodeImpl)root).getData();

      ByteArrayOutputStream os = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(os);

      assertTrue(idSrc instanceof PersistedItemData);

      oos.writeObject(idSrc);

      ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
      ObjectInputStream ois = new ObjectInputStream(is);

      ItemData idDest = (ItemData)ois.readObject();

      assertNotNull(idDest);
   }

   public void testTransientItemData() throws Exception
   {
      ItemData idSrc = ((NodeImpl)root.addNode("test")).getData();

      assertTrue(idSrc instanceof TransientItemData);

      ByteArrayOutputStream os = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(os);

      oos.writeObject(idSrc);

      ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
      ObjectInputStream ois = new ObjectInputStream(is);

      ItemData idDest = (ItemData)ois.readObject();

      assertNotNull(idDest);
   }
}
