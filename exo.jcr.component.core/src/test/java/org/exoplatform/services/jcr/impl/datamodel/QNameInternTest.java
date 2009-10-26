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
package org.exoplatform.services.jcr.impl.datamodel;

import junit.framework.TestCase;

import org.exoplatform.services.jcr.util.SIDGenerator;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by The eXo Platform SAS Author : Peter Nedonosko peter.nedonosko@exoplatform.com.ua
 * 11.10.2007
 * 
 * -XX:MaxPermSize=128m
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: QNameInternTest.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class QNameInternTest extends TestCase
{

   static public final int INTERN_SIZE = 100000;

   static public final int SAMPLE_MOD = INTERN_SIZE / 100;

   static public final int NOTSAMPLE_MOD = SAMPLE_MOD / 10;

   private String memoryInfo()
   {
      String info = "";
      info =
         "free: " + mb(Runtime.getRuntime().freeMemory()) + "M of " + mb(Runtime.getRuntime().totalMemory())
            + "M (max: " + mb(Runtime.getRuntime().maxMemory()) + "M)";
      return info;
   }

   private String mb(long mem)
   {
      return String.valueOf(Math.round(mem * 100d / (1024d * 1024d)) / 100d);
   }

   public void testInternReferenced() throws Exception
   {
      System.out.println("START " + getName() + ", " + memoryInfo());

      Set<String> samples = new HashSet<String>();

      long dsum = 0;

      String[] interList = new String[INTERN_SIZE];
      for (int i = 0; i < INTERN_SIZE; i++)
      {
         String s = SIDGenerator.generate().intern();
         interList[i] = s; // save ref to prevent GCing
         if (i % SAMPLE_MOD == 0)
            samples.add(s);
      }

      // add not containing strings
      for (int i = 0; i < NOTSAMPLE_MOD; i++)
      {
         samples.add(SIDGenerator.generate());
      }

      for (String sample : samples)
      {
         long start = System.currentTimeMillis();
         String sint = sample.intern(); // ask already interned object (most of the samples set)
         long d = System.currentTimeMillis() - start;
         dsum += d;
      }

      System.out.println("\tSample avg. get time " + (dsum * 1f / samples.size()));
      System.out.println("FINISH " + getName() + ", " + memoryInfo());
   }

   public void testInternEquals() throws Exception
   {
      System.out.println("START " + getName() + ", " + memoryInfo());

      Set<String> samples = new HashSet<String>();

      long dsum = 0;

      String[] interList = new String[INTERN_SIZE];
      for (int i = 0; i < INTERN_SIZE; i++)
      {
         String s = SIDGenerator.generate().intern();
         interList[i] = s; // save ref to prevent GCing
         if (i % SAMPLE_MOD == 0)
            samples.add(s);
      }

      // add not containing strings
      for (int i = 0; i < NOTSAMPLE_MOD; i++)
      {
         samples.add(SIDGenerator.generate());
      }

      for (String sample : samples)
      {
         long start = System.currentTimeMillis();
         String sint = new String(sample.toCharArray()).intern(); // ask already interned content (most
         // of the samples set)
         long d = System.currentTimeMillis() - start;
         dsum += d;
         // System.out.println("Sample found " + d);
      }

      System.out.println("\tSample avg. get time " + (dsum * 1f / samples.size()));
      System.out.println("FINISH " + getName() + ", " + memoryInfo());
   }

   public void testStringArrayTraverse() throws Exception
   {

      System.out.println("START " + getName() + ", " + memoryInfo());

      Set<String> samples = new HashSet<String>();

      long dsum = 0;

      String[] interList = new String[INTERN_SIZE];
      for (int i = 0; i < interList.length; i++)
      {
         String s = SIDGenerator.generate();
         interList[i] = s; // save ref to prevent GCing
         interList[i] = s;
         if (i % SAMPLE_MOD == 0)
            samples.add(s);
      }

      // add not containing strings
      for (int i = 0; i < NOTSAMPLE_MOD; i++)
      {
         samples.add(SIDGenerator.generate());
      }

      next : for (String sample : samples)
      {
         long start = System.currentTimeMillis();
         for (String is : interList)
         {
            if (is == sample)
            {
               long d = System.currentTimeMillis() - start;
               dsum += d;
               // System.out.println("Sample found " + d);
               continue next;
            }
         }
         dsum += System.currentTimeMillis() - start;
      }

      System.out.println("\tSample avg. get time " + (dsum * 1f / samples.size()));
      System.out.println("FINISH " + getName() + ", " + memoryInfo());
   }

}
