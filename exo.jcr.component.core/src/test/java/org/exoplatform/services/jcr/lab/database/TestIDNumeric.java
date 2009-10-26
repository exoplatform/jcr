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
package org.exoplatform.services.jcr.lab.database;

import junit.framework.TestCase;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Created by The eXo Platform SAS Author : Peter Nedonosko peter.nedonosko@exoplatform.com.ua
 * 06.11.2007
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: TestIDNumeric.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class TestIDNumeric extends TestCase
{

   public static final int RECORDS_COUNT = 100;

   public static String[] CREATE_ITEMS_SQL_NUMERIC_ANSI_SQL =
      {
         "CREATE TABLE JCR_MITEM_N( " + "ID NUMERIC NOT NULL, " + "PARENT_ID NUMERIC NOT NULL, "
            + "NAME VARCHAR(512) NOT NULL, " + "VERSION INTEGER NOT NULL, " + "I_CLASS INTEGER NOT NULL, "
            + "I_INDEX INTEGER NOT NULL, " + "N_ORDER_NUM INTEGER, " + "P_TYPE INTEGER,  " + "P_MULTIVALUED INTEGER, "
            + "CONSTRAINT JCR_PK_MITEM_N PRIMARY KEY(ID), "
            + "CONSTRAINT JCR_FK_MITEM_PARENT_N FOREIGN KEY(PARENT_ID) REFERENCES JCR_MITEM_N(ID) " + ")",
         "CREATE UNIQUE INDEX JCR_IDX_MITEM_PARENT_N ON JCR_MITEM_N(PARENT_ID, NAME, I_INDEX, I_CLASS, VERSION)",
         "CREATE UNIQUE INDEX JCR_IDX_MITEM_PARENT_NAME_N ON JCR_MITEM_N(I_CLASS, PARENT_ID, NAME, I_INDEX, VERSION)",
         "CREATE UNIQUE INDEX JCR_IDX_MITEM_PARENT_ID_N ON JCR_MITEM_N(I_CLASS, PARENT_ID, ID, VERSION)"};

   public static String[] CREATE_ITEMS_SQL_NUMERIC_DERBY =
      {
         "CREATE TABLE JCR_MITEM_N( " + "ID NUMERIC(31,0) NOT NULL, " + "PARENT_ID NUMERIC(31,0) NOT NULL, "
            + "NAME VARCHAR(512) NOT NULL, " + "VERSION INTEGER NOT NULL, " + "I_CLASS INTEGER NOT NULL, "
            + "I_INDEX INTEGER NOT NULL, " + "N_ORDER_NUM INTEGER, " + "P_TYPE INTEGER,  " + "P_MULTIVALUED INTEGER, "
            + "CONSTRAINT JCR_PK_MITEM_N PRIMARY KEY(ID), "
            + "CONSTRAINT JCR_FK_MITEM_PARENT_N FOREIGN KEY(PARENT_ID) REFERENCES JCR_MITEM_N(ID) " + ")",
         "CREATE UNIQUE INDEX JCR_IDX_MITEM_PARENT_N ON JCR_MITEM_N(PARENT_ID, NAME, I_INDEX, I_CLASS, VERSION)",
         "CREATE UNIQUE INDEX JCR_IDX_MITEM_PARENT_NAME_N ON JCR_MITEM_N(I_CLASS, PARENT_ID, NAME, I_INDEX, VERSION)",
         "CREATE UNIQUE INDEX JCR_IDX_MITEM_PARENT_ID_N ON JCR_MITEM_N(I_CLASS, PARENT_ID, ID, VERSION)"};

   public static String[] CREATE_ITEMS_SQL_NUMERIC_MYSQL =
      {
         "CREATE TABLE JCR_MITEM_N( " + "ID NUMERIC(40) NOT NULL, " + "PARENT_ID NUMERIC(40) NOT NULL, "
            + "NAME VARCHAR(512) NOT NULL, " + "VERSION INTEGER NOT NULL, " + "I_CLASS INTEGER NOT NULL, "
            + "I_INDEX INTEGER NOT NULL, " + "N_ORDER_NUM INTEGER, " + "P_TYPE INTEGER,  " + "P_MULTIVALUED INTEGER, "
            + "CONSTRAINT JCR_PK_MITEM_N PRIMARY KEY(ID), "
            + "CONSTRAINT JCR_FK_MITEM_PARENT_N FOREIGN KEY(PARENT_ID) REFERENCES JCR_MITEM_N(ID) " + ")",
         "CREATE UNIQUE INDEX JCR_IDX_MITEM_PARENT_N ON JCR_MITEM_N(PARENT_ID, NAME, I_INDEX, I_CLASS, VERSION)",
         "CREATE UNIQUE INDEX JCR_IDX_MITEM_PARENT_NAME_N ON JCR_MITEM_N(I_CLASS, PARENT_ID, NAME, I_INDEX, VERSION)",
         "CREATE UNIQUE INDEX JCR_IDX_MITEM_PARENT_ID_N ON JCR_MITEM_N(I_CLASS, PARENT_ID, ID, VERSION)"};

   public static String[] CREATE_ITEMS_SQL_INT_ANSI_SQL =
      {
         "CREATE TABLE JCR_MITEM_N( " + "ID INTEGER NOT NULL, " + "PARENT_ID INTEGER NOT NULL, "
            + "NAME VARCHAR(512) NOT NULL, " + "VERSION INTEGER NOT NULL, " + "I_CLASS INTEGER NOT NULL, "
            + "I_INDEX INTEGER NOT NULL, " + "N_ORDER_NUM INTEGER, " + "P_TYPE INTEGER,  " + "P_MULTIVALUED INTEGER, "
            + "CONSTRAINT JCR_PK_MITEM_N PRIMARY KEY(ID), "
            + "CONSTRAINT JCR_FK_MITEM_PARENT_N FOREIGN KEY(PARENT_ID) REFERENCES JCR_MITEM_N(ID) " + ")",
         "CREATE UNIQUE INDEX JCR_IDX_MITEM_PARENT_N ON JCR_MITEM_N(PARENT_ID, NAME, I_INDEX, I_CLASS, VERSION)",
         "CREATE UNIQUE INDEX JCR_IDX_MITEM_PARENT_NAME_N ON JCR_MITEM_N(I_CLASS, PARENT_ID, NAME, I_INDEX, VERSION)",
         "CREATE UNIQUE INDEX JCR_IDX_MITEM_PARENT_ID_N ON JCR_MITEM_N(I_CLASS, PARENT_ID, ID, VERSION)"};

   public static String[] CREATE_ITEMS_SQL_BIGINT_ANSI_SQL =
      {
         "CREATE TABLE JCR_MITEM_N( " + "ID1 BIGINT NOT NULL, " + "ID2 BIGINT NOT NULL, "
            + "PARENT_ID1 BIGINT NOT NULL, " + "PARENT_ID2 BIGINT NOT NULL, " + "NAME VARCHAR(512) NOT NULL, "
            + "VERSION INTEGER NOT NULL, " + "I_CLASS INTEGER NOT NULL, " + "I_INDEX INTEGER NOT NULL, "
            + "N_ORDER_NUM INTEGER, " + "P_TYPE INTEGER,  " + "P_MULTIVALUED INTEGER, "
            + "CONSTRAINT JCR_PK_MITEM_N PRIMARY KEY(ID1,ID2), "
            + "CONSTRAINT JCR_FK_MITEM_PARENT_N FOREIGN KEY(PARENT_ID1, PARENT_ID2) REFERENCES JCR_MITEM_N(ID1, ID2) "
            + ")",
         "CREATE UNIQUE INDEX JCR_IDX_MITEM_PARENT_N ON JCR_MITEM_N(PARENT_ID1, PARENT_ID2, NAME, I_INDEX, I_CLASS, VERSION)",
         "CREATE UNIQUE INDEX JCR_IDX_MITEM_PARENT_NAME_N ON JCR_MITEM_N(I_CLASS, PARENT_ID1, PARENT_ID2, NAME, I_INDEX, VERSION)",
         "CREATE UNIQUE INDEX JCR_IDX_MITEM_PARENT_ID_N ON JCR_MITEM_N(I_CLASS, PARENT_ID1, PARENT_ID2, ID1, ID2, VERSION)"};

   public static String[] CREATE_ITEMS_SQL_BIGINT_ORACLE =
      {
         "CREATE TABLE JCR_MITEM_N( " + "ID1 INTEGER NOT NULL, " + "ID2 INTEGER NOT NULL, "
            + "PARENT_ID1 INTEGER NOT NULL, " + "PARENT_ID2 INTEGER NOT NULL, " + "NAME VARCHAR(512) NOT NULL, "
            + "VERSION INTEGER NOT NULL, " + "I_CLASS INTEGER NOT NULL, " + "I_INDEX INTEGER NOT NULL, "
            + "N_ORDER_NUM INTEGER, " + "P_TYPE INTEGER,  " + "P_MULTIVALUED INTEGER, "
            + "CONSTRAINT JCR_PK_MITEM_N PRIMARY KEY(ID1,ID2), "
            + "CONSTRAINT JCR_FK_MITEM_PARENT_N FOREIGN KEY(PARENT_ID1, PARENT_ID2) REFERENCES JCR_MITEM_N(ID1, ID2) "
            + ")",
         "CREATE UNIQUE INDEX JCR_IDX_MITEM_PARENT_N ON JCR_MITEM_N(PARENT_ID1, PARENT_ID2, NAME, I_INDEX, I_CLASS, VERSION)",
         "CREATE UNIQUE INDEX JCR_IDX_MITEM_PARENT_NAME_N ON JCR_MITEM_N(I_CLASS, PARENT_ID1, PARENT_ID2, NAME, I_INDEX, VERSION)",
         "CREATE UNIQUE INDEX JCR_IDX_MITEM_PARENT_ID_N ON JCR_MITEM_N(I_CLASS, PARENT_ID1, PARENT_ID2, ID1, ID2, VERSION)"};

   public static String[] CREATE_ITEMS_SQL_VARCHAR_ANSI_SQL =
      {
         "CREATE TABLE JCR_MITEM_N( " + "ID VARCHAR(96) NOT NULL, " + "PARENT_ID VARCHAR(96) NOT NULL, "
            + "NAME VARCHAR(512) NOT NULL, " + "VERSION INTEGER NOT NULL, " + "I_CLASS INTEGER NOT NULL, "
            + "I_INDEX INTEGER NOT NULL, " + "N_ORDER_NUM INTEGER, " + "P_TYPE INTEGER,  " + "P_MULTIVALUED INTEGER, "
            + "CONSTRAINT JCR_PK_MITEM_N PRIMARY KEY(ID), "
            + "CONSTRAINT JCR_FK_MITEM_PARENT_N FOREIGN KEY(PARENT_ID) REFERENCES JCR_MITEM_N(ID) " + ")",
         "CREATE UNIQUE INDEX JCR_IDX_MITEM_PARENT_N ON JCR_MITEM_N(PARENT_ID, NAME, I_INDEX, I_CLASS, VERSION)",
         "CREATE UNIQUE INDEX JCR_IDX_MITEM_PARENT_NAME_N ON JCR_MITEM_N(I_CLASS, PARENT_ID, NAME, I_INDEX, VERSION)",
         "CREATE UNIQUE INDEX JCR_IDX_MITEM_PARENT_ID_N ON JCR_MITEM_N(I_CLASS, PARENT_ID, ID, VERSION)"};

   public static String[] DROP_ITEMS_SQL_ANSI_SQL = {"DROP TABLE JCR_MITEM_N"};

   abstract class TestTask
   {
      List<Long> insertStats = new ArrayList<Long>();

      List<Long> selectStats = new ArrayList<Long>();

      long testNodesCount;

      abstract void execute() throws Exception;
   }

   class Id
   {
      final UUID id;

      BigDecimal bdid;

      Id()
      {
         id = UUID.randomUUID();
      }

      Id(long mostSignificantBits, long leastSignificantBits)
      {
         id = new UUID(mostSignificantBits, leastSignificantBits);
      }

      Id(BigDecimal numeric)
      {
         BigInteger nvalue = numeric.toBigInteger();

         long lsb_bi = nvalue.longValue(); // low-order 8 bytes out (first 64bit)
         long msb_bi = nvalue.shiftRight(8 * 8).longValue(); // most sognificant 8 bytes out (last
         // 64bit)

         id = new UUID(msb_bi, lsb_bi);
      }

      void assertSame(Id expected)
      {
         assertEquals("MostSignificantBits should be same ", expected.id.getMostSignificantBits(), id
            .getMostSignificantBits());
         assertEquals("LeastSignificantBits should be same ", expected.id.getLeastSignificantBits(), id
            .getLeastSignificantBits());
      }

      BigDecimal getBigDecimal()
      {

         if (bdid == null)
         {
            long msb = id.getMostSignificantBits();
            long lsb = id.getLeastSignificantBits();

            // big-endian byte-order: the most significant byte is in the zeroth element
            byte[] bytes =
               {(byte)((msb & 0xFF00000000000000L) >>> 56), (byte)((msb & 0x00FF000000000000L) >>> 48),
                  (byte)((msb & 0x0000FF0000000000L) >>> 40), (byte)((msb & 0x000000FF00000000L) >>> 32),
                  (byte)((msb & 0x00000000FF000000L) >>> 24), (byte)((msb & 0x0000000000FF0000L) >>> 16),
                  (byte)((msb & 0x000000000000FF00L) >>> 8), (byte)(msb & 0x00000000000000FFL),
                  (byte)((lsb & 0xFF00000000000000L) >>> 56), (byte)((lsb & 0x00FF000000000000L) >>> 48),
                  (byte)((lsb & 0x0000FF0000000000L) >>> 40), (byte)((lsb & 0x000000FF00000000L) >>> 32),
                  (byte)((lsb & 0x00000000FF000000L) >>> 24), (byte)((lsb & 0x0000000000FF0000L) >>> 16),
                  (byte)((lsb & 0x000000000000FF00L) >>> 8), (byte)(lsb & 0x00000000000000FFL)};

            // String hexb = "";
            // for (byte b: bytes) {
            // String hx = Integer.toHexString(b);
            // hx = hx.length() < 2 ? "0" + hx : hx;
            // hexb += (hx.length() > 2 ? hx.substring(hx.length() - 2) : hx);
            // }
            // System.out.println("Bytes: \t" + hexb);

            // String hexi = "";
            // int[] ints = stripLeadingZeroBytes(bytes);
            // for (int i: ints) {
            // hexi += Integer.toHexString(i);
            // }
            // System.out.println("Ints: \t" + hexi);
            // assertEquals("Should be " + hexb, hexb, hexi);

            BigDecimal numeric = new BigDecimal(new BigInteger(bytes));

            BigInteger nvalue = numeric.toBigInteger();

            long lsb_bi = nvalue.longValue(); // low-order 8 bytes out (first 64bit)
            long msb_bi = nvalue.shiftRight(8 * 8).longValue(); // most sognificant 8 bytes out (last
            // 64bit)
            // String hexbi = Long.toHexString(msb_bi) + Long.toHexString(lsb_bi) ;

            // System.out.println(Long.toHexString(lsb_bi) + " - " + Long.toHexString(lsb));
            // System.out.println(Long.toHexString(msb_bi) + " - " + Long.toHexString(msb));
            // System.out.println("Ints: \t" + hexbi);

            // assertEquals("Should be " + hexb, hexb, hexbi);

            assertEquals("Should be same ", msb, msb_bi);
            assertEquals("Should be same ", lsb, lsb_bi);

            return bdid = numeric;
         }

         return bdid;
      }

      private int[] stripLeadingZeroBytes(byte a[])
      {
         int byteLength = a.length;
         int keep;

         // Find first nonzero byte
         for (keep = 0; keep < a.length && a[keep] == 0; keep++);

         // Allocate new array and copy relevant part of input array
         int intLength = ((byteLength - keep) + 3) / 4;
         int[] result = new int[intLength];
         int b = byteLength - 1;
         for (int i = intLength - 1; i >= 0; i--)
         {
            result[i] = a[b--] & 0xff;
            int bytesRemaining = b - keep + 1;
            int bytesToTransfer = Math.min(3, bytesRemaining);
            for (int j = 8; j <= 8 * bytesToTransfer; j += 8)
               result[i] |= ((a[b--] & 0xff) << j);
         }
         return result;
      }
   }

   class SId
   {
      final static int LONG_STRING_LENGTH = 16;

      final static int ID_STRING_LENGTH = LONG_STRING_LENGTH * 2;

      final String id;

      SId()
      {
         UUID uuid = UUID.randomUUID();
         this.id = string(uuid);
      }

      SId(String idString)
      {
         assertEquals("Id string length is vrong", ID_STRING_LENGTH, idString.length());

         this.id = idString;
      }

      void assertSame(SId expected)
      {
         assertEquals("String ID should be same ", expected.id, id);
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

      String getString()
      {
         return id;
      }
   }

   private Connection openDatabase(String driver, String url, String user, String passwd) throws Exception
   {

      Class.forName(driver);

      return (user == null || passwd == null) ? DriverManager.getConnection(url) : DriverManager.getConnection(url,
         user, passwd);
   }

   private void runDDL(Connection con, String[] ddl) throws Exception
   {
      for (String sql : ddl)
      {
         con.createStatement().executeUpdate(sql);
      }
   }

   public Connection createHSQLDB(String[] ddl) throws Exception
   {
      // autocommit=true
      Connection con = openDatabase("org.hsqldb.jdbcDriver", "jdbc:hsqldb:file:target/temp/data/idtest", "sa", "");

      runDDL(con, ddl);

      return con;
   }

   public Connection createOracle(String[] ddl) throws Exception
   {
      // autocommit=true
      Connection con =
         openDatabase("oracle.jdbc.OracleDriver", "jdbc:oracle:thin:@tornado.exoua-int:1523:orcl", "exoadmin",
            "exo12321");

      runDDL(con, ddl);

      return con;
   }

   public Connection createMysql(String[] ddl) throws Exception
   {
      // autocommit=true
      Connection con = openDatabase("com.mysql.jdbc.Driver", "jdbc:mysql://localhost/portal", "exoadmin", "exo12321");

      runDDL(con, ddl);

      return con;
   }

   public Connection createPostgres(String[] ddl) throws Exception
   {
      // autocommit=true
      Connection con =
         openDatabase("org.postgresql.Driver", "jdbc:postgresql://localhost/portal", "exoadmin", "exo12321");

      runDDL(con, ddl);

      return con;
   }

   public Connection createDerby(String[] ddl) throws Exception
   {
      // autocommit=true
      Connection con =
         openDatabase("org.apache.derby.jdbc.EmbeddedDriver", "jdbc:derby:target/temp/derby/idtest;create=true", null,
            null);

      runDDL(con, ddl);

      return con;
   }

   private void doTest(Connection con, long nodesCount, TestTask test) throws Exception
   {
      long start = System.currentTimeMillis();
      try
      {
         System.out.println("Start " + getName());

         test.execute();

         // con.commit();
      }
      catch (Exception e)
      {
         System.err.println("Test error " + e);
         e.printStackTrace();
         try
         {
            con.rollback();
         }
         catch (SQLException e1)
         {
            System.err.println("Rollback error " + e);
            e.printStackTrace();
         }
         throw new Exception(e);
      }
      finally
      {
         try
         {
            for (String sql : DROP_ITEMS_SQL_ANSI_SQL)
            {
               con.createStatement().executeUpdate(sql);
            }
            // con.commit();
         }
         catch (Exception e)
         {
            System.err.println("Delete error " + e);
            e.printStackTrace();
            // not matter
         }
         finally
         {
            try
            {
               con.close();
            }
            catch (Exception e)
            {
               System.err.println("Close error " + e);
            }
         }

         // stats
         long isum = 0;
         for (long istat : test.insertStats)
            isum += istat;
         double iavg = Math.round(isum * 1000d / test.insertStats.size()) / 1000d;
         long ssum = 0;
         for (long sstat : test.selectStats)
            ssum += sstat;
         double savg = Math.round(ssum * 1000d / test.selectStats.size()) / 1000d;
         System.out.println("Stop " + getName() + " records:" + test.testNodesCount + " "
            + (System.currentTimeMillis() - start) + "ms "
            + (test.insertStats.size() > 0 ? "avg insert:" + iavg + "ms" : "")
            + (test.selectStats.size() > 0 ? " select:" + savg + "ms" : "") + "\n");
      }
   }

   private void doTestNumeric(final Connection con, final long nodesCount) throws Exception
   {

      TestTask test = new TestTask()
      {

         void execute() throws Exception
         {

            this.testNodesCount = nodesCount;

            String insertSql =
               "insert into JCR_MITEM_N(ID, PARENT_ID, NAME, VERSION, I_CLASS, I_INDEX, N_ORDER_NUM, P_TYPE, P_MULTIVALUED) VALUES(?,?,?,?,?,?,?,?,?)";
            String selectSql = "select * from JCR_MITEM_N where ID=?";

            PreparedStatement insert = con.prepareStatement(insertSql);
            PreparedStatement select = con.prepareStatement(selectSql);

            long istart = System.currentTimeMillis();
            Id parentId = new Id(); // parentId.toBigInt().doubleValue().toString()
            // insert parent
            insert.clearParameters();
            insert.setBigDecimal(1, parentId.getBigDecimal());
            insert.setBigDecimal(2, parentId.getBigDecimal());
            insert.setString(3, "[]:root");
            insert.setInt(4, 0);
            insert.setInt(5, 0);
            insert.setInt(6, 1);
            insert.setInt(7, 1);
            insert.setNull(8, Types.INTEGER);
            insert.setNull(9, Types.INTEGER);
            insert.executeUpdate();
            insertStats.add(System.currentTimeMillis() - istart);

            for (int i = 1; i <= nodesCount; i++)
            {
               // INSERT
               istart = System.currentTimeMillis();

               Id id = new Id();

               insert.clearParameters();
               insert.setBigDecimal(1, id.getBigDecimal());
               insert.setBigDecimal(2, parentId.getBigDecimal());
               insert.setInt(4, 0);
               if (i % 10 == 0)
               {
                  parentId = id;
                  insert.setString(3, "[]:node" + i + ":1");
                  insert.setInt(5, 0);
                  insert.setInt(7, 1);
                  insert.setNull(8, Types.INTEGER);
                  insert.setNull(9, Types.INTEGER);
               }
               else
               {
                  insert.setString(3, "[]:property" + i + ":1");
                  insert.setInt(5, 1);
                  insert.setNull(7, Types.INTEGER);
                  insert.setInt(8, 1);
                  insert.setInt(9, 0);
               }
               insert.setInt(6, 1);

               insert.executeUpdate();
               insertStats.add(System.currentTimeMillis() - istart);

               // SELECT
               long sstart = System.currentTimeMillis();
               select.clearParameters();
               select.setBigDecimal(1, id.getBigDecimal());
               ResultSet rs = select.executeQuery();
               assertTrue("A record should exists", rs.next());

               Id dbid = new Id(rs.getBigDecimal("ID"));
               rs.close();

               dbid.assertSame(id);

               selectStats.add(System.currentTimeMillis() - sstart);
            }
         }
      };

      doTest(con, nodesCount, test);
   }

   private void doTestBigInt(final Connection con, final long nodesCount) throws Exception
   {

      TestTask test = new TestTask()
      {

         void execute() throws Exception
         {

            this.testNodesCount = nodesCount;

            String insertSql =
               "insert into JCR_MITEM_N(ID1, ID2, PARENT_ID1, PARENT_ID2, NAME, VERSION, I_CLASS, I_INDEX, N_ORDER_NUM, P_TYPE, P_MULTIVALUED) VALUES(?,?,?,?,?,?,?,?,?,?,?)";
            String selectSql = "select * from JCR_MITEM_N where ID1=? and ID2=?";

            PreparedStatement insert = con.prepareStatement(insertSql);
            PreparedStatement select = con.prepareStatement(selectSql);

            long istart = System.currentTimeMillis();
            Id parentId = new Id(); // parentId.toBigInt().doubleValue().toString()
            // insert parent
            insert.clearParameters();
            insert.setLong(1, parentId.id.getMostSignificantBits());
            insert.setLong(2, parentId.id.getLeastSignificantBits());
            insert.setLong(3, parentId.id.getMostSignificantBits());
            insert.setLong(4, parentId.id.getLeastSignificantBits());
            insert.setString(5, "[]:root");
            insert.setInt(6, 0);
            insert.setInt(7, 0);
            insert.setInt(8, 1);
            insert.setInt(9, 1);
            insert.setNull(10, Types.INTEGER);
            insert.setNull(11, Types.INTEGER);
            insert.executeUpdate();
            insertStats.add(System.currentTimeMillis() - istart);

            for (int i = 1; i <= nodesCount; i++)
            {
               // INSERT
               istart = System.currentTimeMillis();

               Id id = new Id();

               insert.clearParameters();
               insert.setLong(1, id.id.getMostSignificantBits());
               insert.setLong(2, id.id.getLeastSignificantBits());
               insert.setLong(3, parentId.id.getMostSignificantBits());
               insert.setLong(4, parentId.id.getLeastSignificantBits());
               insert.setInt(6, 0);
               if (i % 10 == 0)
               {
                  parentId = id;
                  insert.setString(5, "[]:node" + i + ":1");
                  insert.setInt(7, 0);
                  insert.setInt(9, 1);
                  insert.setNull(10, Types.INTEGER);
                  insert.setNull(11, Types.INTEGER);
               }
               else
               {
                  insert.setString(5, "[]:property" + i + ":1");
                  insert.setInt(7, 1);
                  insert.setNull(9, Types.INTEGER);
                  insert.setInt(10, 1);
                  insert.setInt(11, 0);
               }
               insert.setInt(8, 1);

               insert.executeUpdate();
               insertStats.add(System.currentTimeMillis() - istart);

               // SELECT
               long sstart = System.currentTimeMillis();
               select.clearParameters();
               select.setLong(1, id.id.getMostSignificantBits());
               select.setLong(2, id.id.getLeastSignificantBits());
               ResultSet rs = select.executeQuery();
               assertTrue("A record should exists", rs.next());

               Id dbid = new Id(rs.getLong("ID1"), rs.getLong("ID2"));
               rs.close();

               dbid.assertSame(id);

               selectStats.add(System.currentTimeMillis() - sstart);
            }
         }
      };

      doTest(con, nodesCount, test);
   }

   private void doTestVarchar(final Connection con, final long nodesCount) throws Exception
   {

      TestTask test = new TestTask()
      {

         void execute() throws Exception
         {

            this.testNodesCount = nodesCount;

            String insertSql =
               "insert into JCR_MITEM_N(ID, PARENT_ID, NAME, VERSION, I_CLASS, I_INDEX, N_ORDER_NUM, P_TYPE, P_MULTIVALUED) VALUES(?,?,?,?,?,?,?,?,?)";
            String selectSql = "select * from JCR_MITEM_N where ID=?";

            PreparedStatement insert = con.prepareStatement(insertSql);
            PreparedStatement select = con.prepareStatement(selectSql);

            long istart = System.currentTimeMillis();
            SId parentId = new SId();
            // insert parent
            insert.clearParameters();
            insert.setString(1, parentId.getString());
            insert.setString(2, parentId.getString());
            insert.setString(3, "[]:root");
            insert.setInt(4, 0);
            insert.setInt(5, 0);
            insert.setInt(6, 1);
            insert.setInt(7, 1);
            insert.setNull(8, Types.INTEGER);
            insert.setNull(9, Types.INTEGER);
            insert.executeUpdate();
            insertStats.add(System.currentTimeMillis() - istart);

            for (int i = 1; i <= nodesCount; i++)
            {
               // INSERT
               istart = System.currentTimeMillis();

               SId id = new SId();

               insert.clearParameters();
               insert.setString(1, id.getString());
               insert.setString(2, parentId.getString());
               insert.setInt(4, 0);
               if (i % 10 == 0)
               {
                  parentId = id;
                  insert.setString(3, "[]:node" + i + ":1");
                  insert.setInt(5, 0);
                  insert.setInt(7, 1);
                  insert.setNull(8, Types.INTEGER);
                  insert.setNull(9, Types.INTEGER);
               }
               else
               {
                  insert.setString(3, "[]:property" + i + ":1");
                  insert.setInt(5, 1);
                  insert.setNull(7, Types.INTEGER);
                  insert.setInt(8, 1);
                  insert.setInt(9, 0);
               }
               insert.setInt(6, 1);

               insert.executeUpdate();
               insertStats.add(System.currentTimeMillis() - istart);

               // SELECT
               long sstart = System.currentTimeMillis();
               select.clearParameters();
               select.setString(1, id.getString());
               ResultSet rs = select.executeQuery();
               assertTrue("A record should exists", rs.next());

               SId dbid = new SId(rs.getString("ID"));
               rs.close();

               dbid.assertSame(id);

               selectStats.add(System.currentTimeMillis() - sstart);
            }
         }
      };

      doTest(con, nodesCount, test);
   }

   // ========== HSQL =========

   public void testHSQLDB_Numeric() throws Exception
   {
      doTestNumeric(createHSQLDB(CREATE_ITEMS_SQL_NUMERIC_ANSI_SQL), RECORDS_COUNT);
   }

   public void testHSQLDB_Bigint() throws Exception
   {
      doTestBigInt(createHSQLDB(CREATE_ITEMS_SQL_BIGINT_ANSI_SQL), RECORDS_COUNT);
   }

   public void testHSQLDB_Varchar() throws Exception
   {
      doTestVarchar(createHSQLDB(CREATE_ITEMS_SQL_VARCHAR_ANSI_SQL), RECORDS_COUNT);
   }

   // ========= Postgres ==========

   public void testPostgres_Numeric() throws Exception
   {
      doTestNumeric(createPostgres(CREATE_ITEMS_SQL_NUMERIC_ANSI_SQL), RECORDS_COUNT);
   }

   public void testPostgres_Bigint() throws Exception
   {
      doTestBigInt(createPostgres(CREATE_ITEMS_SQL_BIGINT_ANSI_SQL), RECORDS_COUNT);
   }

   public void testPostgres_Varchar() throws Exception
   {
      doTestVarchar(createPostgres(CREATE_ITEMS_SQL_VARCHAR_ANSI_SQL), RECORDS_COUNT);
   }

   // ========== Oracle =========

   // public void testOracle_Numeric() throws Exception {
   // doTestNumeric(createOracle(CREATE_ITEMS_SQL_NUMERIC_ANSI_SQL), RECORDS_COUNT);
   // }
   //  
   // public void testOracle_Bigint() throws Exception {
   // doTestBigInt(createOracle(CREATE_ITEMS_SQL_BIGINT_ORACLE), RECORDS_COUNT);
   // }
   //  
   // public void testOracle_Varchar() throws Exception {
   // doTestVarchar(createOracle(CREATE_ITEMS_SQL_VARCHAR_ANSI_SQL), RECORDS_COUNT);
   // }

   // ========== Mysql ==========

   public void testMysql_Numeric() throws Exception
   {
      doTestNumeric(createMysql(CREATE_ITEMS_SQL_NUMERIC_MYSQL), RECORDS_COUNT);
   }

   public void testMysql_Bigint() throws Exception
   {
      doTestBigInt(createMysql(CREATE_ITEMS_SQL_BIGINT_ANSI_SQL), RECORDS_COUNT);
   }

   public void testMysql_Varchar() throws Exception
   {
      doTestVarchar(createMysql(CREATE_ITEMS_SQL_VARCHAR_ANSI_SQL), RECORDS_COUNT);
   }

   // ========== Derby ==========

   // public void testDerby_Numeric() throws Exception {
   // // Derby can't store more 31 digit numerics
   // //doTestNumeric(createDerby(CREATE_ITEMS_SQL_NUMERIC_DERBY), RECORDS_COUNT);
   // }
   //  
   // public void testDerby_Bigint() throws Exception {
   // doTestBigInt(createDerby(CREATE_ITEMS_SQL_BIGINT_ANSI_SQL), RECORDS_COUNT);
   // }
   //  
   // public void testDerby_Varchar() throws Exception {
   // doTestVarchar(createDerby(CREATE_ITEMS_SQL_VARCHAR_ANSI_SQL), RECORDS_COUNT);
   // }
}

/*
 * Running org.exoplatform.services.jcr.lab.database.TestIDNumeric Start testHSQLDB_Numeric Stop
 * testHSQLDB_Numeric records:1000000 73907ms avg insert:0.064ms select:0.0090ms Start
 * testHSQLDB_Bigint Stop testHSQLDB_Bigint records:1000000 50625ms avg insert:0.042ms
 * select:0.0080ms Start testHSQLDB_Varchar Stop testHSQLDB_Varchar records:1000000 53297ms avg
 * insert:0.047ms select:0.0060ms Start testPostgres_Numeric Stop testPostgres_Numeric
 * records:1000000 3838782ms avg insert:3.629ms select:0.207ms Start testPostgres_Bigint Stop
 * testPostgres_Bigint records:1000000 2995500ms avg insert:2.775ms select:0.22ms Start
 * testPostgres_Varchar Stop testPostgres_Varchar records:1000000 4397406ms avg insert:4.191ms
 * select:0.206ms Start testMysql_Numeric Stop testMysql_Numeric records:1000000 1315829ms avg
 * insert:1.135ms select:0.178ms Start testMysql_Bigint Stop testMysql_Bigint records:1000000
 * 1325968ms avg insert:1.138ms select:0.185ms Start testMysql_Varchar Stop testMysql_Varchar
 * records:1000000 1391000ms avg insert:1.211ms select:0.178ms Tests run: 9, Failures: 0, Errors: 0,
 * Skipped: 0, Time elapsed: 15,449.625 sec Results : Tests run: 9, Failures: 0, Errors: 0, Skipped:
 * 0 [INFO] ------------------------------------------------------------------------ [INFO] BUILD
 * SUCCESSFUL [INFO] ------------------------------------------------------------------------ [INFO]
 * Total time: 257 minutes 41 seconds [INFO] Finished at: Thu Nov 08 22:33:12 EET 2007 [INFO] Final
 * Memory: 11M/821M [INFO] ------------------------------------------------------------------------
 */
