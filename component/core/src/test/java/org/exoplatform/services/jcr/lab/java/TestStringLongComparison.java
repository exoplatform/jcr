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
package org.exoplatform.services.jcr.lab.java;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import junit.framework.TestCase;

/**
 * Created by The eXo Platform SAS Author : Peter Nedonosko peter.nedonosko@exoplatform.com.ua
 * 09.11.2007
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: TestStringLongComparison.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class TestStringLongComparison
   extends TestCase
{

   public static final int RECORDS_COUNT = 1000;

   class Id
   {
      final static int LONG_STRING_LENGTH = 16;

      final static int ID_STRING_LENGTH = LONG_STRING_LENGTH * 2;

      final UUID id;

      Id()
      {
         id = UUID.randomUUID();
      }

      private String trailZeros(String hex)
      {
         if (hex.length() < LONG_STRING_LENGTH)
         {
            int d = LONG_STRING_LENGTH - hex.length();
            char[] zrs = new char[d];
            Arrays.fill(zrs, '0');
            return new String(zrs) + hex;
         }

         return hex;
      }

      private String string(UUID id)
      {
         String msb = trailZeros(Long.toHexString(id.getMostSignificantBits()));
         String lsb = trailZeros(Long.toHexString(id.getLeastSignificantBits()));
         return msb + lsb;
      }

      @Override
      public boolean equals(Object obj)
      {
         return obj instanceof Id ? id.equals(((Id) obj).id) : false;
      }

      @Override
      public int hashCode()
      {
         return id.hashCode();
      }

      @Override
      public String toString()
      {
         return string(id);
      }
   }

   private String memoryInfo() throws Exception
   {
      runGC();

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

   // =========== http://www.javaworld.com/javaworld/javatips/jw-javatip130.html ===========

   private static void runGC() throws Exception
   {
      // It helps to call Runtime.gc()
      // using several method calls:
      for (int r = 0; r < 4; ++r)
         _runGC();
   }

   private static long usedMemory()
   {
      return s_runtime.totalMemory() - s_runtime.freeMemory();
   }

   private static void _runGC() throws Exception
   {
      long usedMem1 = usedMemory(), usedMem2 = Long.MAX_VALUE;
      for (int i = 0; (usedMem1 < usedMem2) && (i < 500); ++i)
      {
         s_runtime.runFinalization();
         s_runtime.gc();
         Thread.currentThread().yield();

         usedMem2 = usedMem1;
         usedMem1 = usedMemory();
      }
   }

   private static final Runtime s_runtime = Runtime.getRuntime();

   // =======================================================================================

   private static Boolean gcInit = false;

   @Override
   protected void setUp() throws Exception
   {
      if (!gcInit)
      {
         runGC();
         gcInit = true;
      }
   }

   public void testUUIDInMap() throws Exception
   {

      Map<Id, Integer> select = new HashMap<Id, Integer>();
      Map<Id, Integer> all = new HashMap<Id, Integer>();

      long start = System.currentTimeMillis();
      for (int i = 1; i <= RECORDS_COUNT; i++)
      {
         Id id = new Id();
         all.put(id, i);
         if (i % 100 == 0)
            select.put(id, i);
      }
      long d = (System.currentTimeMillis() - start);
      double avg = Math.round(d * 1000d / RECORDS_COUNT) / 1000d;
      System.out.println("Puts done " + getName() + " items:" + RECORDS_COUNT + ", time:" + d + "ms, avg.time:" + avg
               + "ms, " + memoryInfo());

      start = System.currentTimeMillis();
      for (Map.Entry<Id, Integer> ide : select.entrySet())
      {
         Id id = ide.getKey();
         Integer i = all.get(id);
         assertEquals("Indexes should be same", ide.getValue(), i);
         // System.out.println("Get id:" + id.toString() + " -- " + i);
      }
      d = (System.currentTimeMillis() - start);
      avg = Math.round(d * 1000d / select.size()) / 1000d;
      System.out.println("Gets done " + getName() + " items:" + select.size() + ", time:" + d + "ms, avg.time:" + avg
               + "ms, " + memoryInfo() + "\n");

   }

   public void testUUIDInList() throws Exception
   {

      Map<Id, Integer> select = new HashMap<Id, Integer>();
      List<Id> all = new ArrayList<Id>();

      long start = System.currentTimeMillis();
      for (int i = 0; i < RECORDS_COUNT; i++)
      {
         Id id = new Id();
         all.add(id);
         if (i % 100 == 0)
            select.put(id, i);
      }
      long d = (System.currentTimeMillis() - start);
      double avg = Math.round(d * 1000d / RECORDS_COUNT) / 1000d;
      System.out.println("Adds done " + getName() + " items:" + RECORDS_COUNT + ", time:" + d + "ms, avg.time:" + avg
               + "ms, " + memoryInfo());

      start = System.currentTimeMillis();
      for (Map.Entry<Id, Integer> ide : select.entrySet())
      {
         Integer i = all.indexOf(ide.getKey());
         assertEquals("Indexes should be same", ide.getValue(), i);
         // System.out.println("Get id:" + id.toString() + " -- " + i);
      }
      d = (System.currentTimeMillis() - start);
      avg = Math.round(d * 1000d / select.size()) / 1000d;
      System.out.println("Gets done " + getName() + " items:" + select.size() + ", time:" + d + "ms, avg.time:" + avg
               + "ms, " + memoryInfo() + "\n");
   }

   public void testStringInMap() throws Exception
   {

      Map<String, Integer> select = new HashMap<String, Integer>();
      Map<String, Integer> all = new HashMap<String, Integer>();

      long start = System.currentTimeMillis();
      for (int i = 1; i <= RECORDS_COUNT; i++)
      {
         String id = new Id().toString();
         all.put(id, i);
         if (i % 100 == 0)
            select.put(new String(id.toCharArray()), i);
      }
      long d = (System.currentTimeMillis() - start);
      double avg = Math.round(d * 1000d / RECORDS_COUNT) / 1000d;
      System.out.println("Puts done " + getName() + " items:" + RECORDS_COUNT + ", time:" + d + "ms, avg.time:" + avg
               + "ms, " + memoryInfo());

      start = System.currentTimeMillis();
      for (Map.Entry<String, Integer> ide : select.entrySet())
      {
         String id = ide.getKey();
         Integer i = all.get(id);
         assertEquals("Indexes should be same", ide.getValue(), i);
         // System.out.println("Get id:" + id.toString() + " -- " + i);
      }
      d = (System.currentTimeMillis() - start);
      avg = Math.round(d * 1000d / select.size()) / 1000d;
      System.out.println("Gets done " + getName() + " items:" + select.size() + ", time:" + d + "ms, avg.time:" + avg
               + "ms, " + memoryInfo() + "\n");
   }

   public void testStringInList() throws Exception
   {

      Map<String, Integer> select = new HashMap<String, Integer>();
      List<String> all = new ArrayList<String>();

      long start = System.currentTimeMillis();
      for (int i = 0; i < RECORDS_COUNT; i++)
      {
         String id = new Id().toString();
         all.add(id);
         if (i % 100 == 0)
            select.put(id, i);
      }
      long d = (System.currentTimeMillis() - start);
      double avg = Math.round(d * 1000d / RECORDS_COUNT) / 1000d;
      System.out.println("Adds done " + getName() + " items:" + RECORDS_COUNT + ", time:" + d + "ms, avg.time:" + avg
               + "ms, " + memoryInfo());

      start = System.currentTimeMillis();
      for (Map.Entry<String, Integer> ide : select.entrySet())
      {
         Integer i = all.indexOf(ide.getKey());
         assertEquals("Indexes should be same", ide.getValue(), i);
         // System.out.println("Get id:" + id.toString() + " -- " + i);
      }
      d = (System.currentTimeMillis() - start);
      avg = Math.round(d * 1000d / select.size()) / 1000d;
      System.out.println("Gets done " + getName() + " items:" + select.size() + ", time:" + d + "ms, avg.time:" + avg
               + "ms, " + memoryInfo() + "\n");
   }

}
