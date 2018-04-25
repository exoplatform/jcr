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
package org.exoplatform.services.jcr.ext.organization;

import org.exoplatform.services.organization.User;
import java.util.ArrayList;
import java.util.List;


/**
 * @author <a href="dmitry.kuleshov@exoplatform.com">Dmitry Kuleshov</a>
 * @version $Id:$
 */
public class TestUserImpl extends AbstractOrganizationServiceTest
{

   /**
    * Save user.
    */
   public void testSaveUser() throws Exception
   {
      createUser(userName);

      // change name
      User u = uHandler.findUserByName(userName);
      u = uHandler.findUserByName(userName);
      u.setUserName(newUserName);
      uHandler.saveUser(u, true);

      // we should to find user with new name but not with old one
      assertNotNull(uHandler.findUserByName(newUserName));
      assertNull(uHandler.findUserByName(userName));
   }

   public void testEnableUserConcurrently() throws Exception
   {

      List<Thread> threads = new ArrayList<Thread>();

      for (int i = 0; i < 10; i++)
      {
         createUser(userName + i);
         uHandler.setEnabled(userName + i, false, false); // Disable the user.
         threads.add(new Thread(new A(userName + i)));
         threads.add(new Thread(new A(userName + i)));
         threads.add(new Thread(new A(userName + i)));
      }


      for (Thread a : threads)
      {
         a.start();
      }

      for (Thread a : threads)
      {
         a.join();
      }
      for (int i = 0; i < 10; i++)
      {
         User user= uHandler.findUserByName(userName+i);
         assertTrue(user.isEnabled());
      }
   }

   class A implements Runnable
   {
      private String name;

      public A(String userName)
      {
         this.name = userName;
      }

      public void run()
      {
         try
         {
            uHandler.setEnabled(name, true, false); // Concurrency enabling the user.
         }
         catch (Exception e)//NOSONAR
         {
            e.printStackTrace();//NOSONAR
         }
      }
   }
}
