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
package org.exoplatform.services.jcr.ext.app;

import org.exoplatform.services.jcr.ext.BaseStandaloneTest;
import org.exoplatform.services.jcr.ext.common.SessionProvider;

import java.util.concurrent.CountDownLatch;

/**
 * Created by The eXo Platform SAS
 * 
 * Date: 21.04.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: SessionProviderServiceTest.java 14112 2008-05-12 15:29:59Z gazarenkov $
 */
public class SessionProviderServiceTest extends BaseStandaloneTest
{

   private SessionProviderService provider;

   @Override
   public void setUp() throws Exception
   {
      super.setUp();

      provider = new ThreadLocalSessionProviderService();
      // provider.setSessionProvider(null, SessionProvider.createAnonimProvider());
   }

   @Override
   protected void tearDown() throws Exception
   {
      provider = null;

      super.tearDown();
   }

   public void testGetSystemSesssion()
   {

      SessionProvider ssp = provider.getSystemSessionProvider(null);

      assertNotNull("System session provider must be reachable anyway ", ssp);

      assertEquals("Same system session provider should be returned to this thread ", ssp, provider
         .getSystemSessionProvider(null));
   }

   public void testGetSystemSesssionWithSet()
   {

      SessionProvider ssp = provider.getSystemSessionProvider(null);

      provider.setSessionProvider(null, SessionProvider.createAnonimProvider());

      assertEquals("Same system session provider should be returned to this thread ", ssp, provider
         .getSystemSessionProvider(null));
   }

   public void testGetSystemSesssionAnotherTrhead() throws InterruptedException
   {

      SessionProvider ssp = provider.getSystemSessionProvider(null);

      final SessionProviderService provider = this.provider;

      final SessionProvider[] atSSP = new SessionProvider[]{ssp};

      final CountDownLatch testerLatch = new CountDownLatch(1);

      Thread tester = new Thread()
      {

         @Override
         public void run()
         {
            atSSP[0] = provider.getSystemSessionProvider(null);
            testerLatch.countDown();
         }

      };

      tester.start();

      testerLatch.await();

      assertNotSame("Another thread should obtain another system session provider from the service", ssp, atSSP[0]);
   }

}
