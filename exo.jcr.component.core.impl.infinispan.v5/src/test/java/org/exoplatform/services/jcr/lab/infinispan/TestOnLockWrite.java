/*
 * Copyright (C) 2010 eXo Platform SAS.
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
package org.exoplatform.services.jcr.lab.infinispan;

import org.exoplatform.services.jcr.core.CredentialsImpl;
import org.exoplatform.services.jcr.impl.core.RepositoryImpl;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.usecases.BaseUsecasesTest;
import org.exoplatform.services.log.Log;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.jcr.Node;

public class TestOnLockWrite extends BaseUsecasesTest {
   
   public static final AtomicBoolean IS_OVER = new AtomicBoolean();
   
   public void testOnLock() throws Exception
   {
      for (int i = 0; i < 3; i++)
      {
         Writer writer = new Writer(repository, log, "" + i);
         writer.start();
      }  

      System.in.read();
   }
  

   class Writer extends Thread
   {
      RepositoryImpl repo;
      CredentialsImpl credo;
      Log lg;
      private String id;  

      public Writer(RepositoryImpl repo, Log lg, String id)
      {
         super("Writer " + id);
         this.repo = repo;
         this.credo = new CredentialsImpl("admin", "admin".toCharArray());
         this.lg = lg;
         this.id = id;
      }
  
      public void run()
      {
         try
         {
            SessionImpl sesImpl = (SessionImpl)repo.login(credo, "ws");
            long c = 0;        

            while (!IS_OVER.get())
            {
               Node forLock = sesImpl.getRootNode().addNode("rootNode" + id);
               long time;
               time = System.currentTimeMillis();
               if (IS_OVER.get())
                  return;
               lg.info("#### Trying to Add the Node");
               sesImpl.save();
               lg.info("#### Node added in " + (System.currentTimeMillis() - time) + " ms");

               forLock.remove();
               if (IS_OVER.get())
                  return;
               lg.info("#### Trying to Remove the Node");
               sesImpl.save();
               lg.info("#### Node removed in " + (System.currentTimeMillis() - time) + " ms");

               if (++c % 1000 == 0)
                 lg.info(this.getName() + " :" + c);
            }
         }
         catch (Exception e)
         {
            lg.error(this.getName(), e);
         }
         finally
         {
            IS_OVER.set(true);
         }
      }
   }
}