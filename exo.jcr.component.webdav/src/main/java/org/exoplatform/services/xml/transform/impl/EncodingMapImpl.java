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
package org.exoplatform.services.xml.transform.impl;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.xml.transform.EncodingMap;

import java.util.Hashtable;

/**
 * Created by The eXo Platform SAS . Conversions between IANA encoding names and
 * Java encoding names,
 * See http://java.sun.com/j2se/1.5.0/docs/guide/intl/encoding.doc.html
 *
 * @author <a href="mailto:alex.kravchuk@gmail.com">Alexander Kravchuk</a>
 * @version $Id:
 */

public class EncodingMapImpl implements EncodingMap
{
   private static final Log LOG = ExoLogger.getLogger("exo.core.component.xml-processing.EncodingMapImpl");

   protected final static Hashtable<String, String> IANA2JavaMap = new Hashtable<String, String>();

   protected final static Hashtable<String, String> Java2IANAMap = new Hashtable<String, String>();

   public static void addIANA2JavaMapping(String iana, String java)
   {
      IANA2JavaMap.put(iana, java);
      if (Java2IANAMap.get(java) == null)
      {
         Java2IANAMap.put(java, iana);
      }
   }

   public static void removeIANA2JavaMapping(String iana, String java)
   {
      IANA2JavaMap.remove(iana);
      Java2IANAMap.remove(java);
   }

   public static void IANA2JavaMapping(String iana, String java)
   {
      IANA2JavaMap.put(iana, java);
      Java2IANAMap.put(java, iana);
   }

   public String convertIANA2Java(String iana)
   {
      return IANA2JavaMap.get(iana);
   }

   public String convertJava2IANA(String java)
   {
      LOG.debug("convert [" + java + "] to iana coding [" + Java2IANAMap.get(java) + "]");
      return Java2IANAMap.get(java);
   }

   static
   {
      addIANA2JavaMapping("BIG5", "Big5");
      addIANA2JavaMapping("CSBIG5", "Big5");
      addIANA2JavaMapping("CP037", "CP037");
      addIANA2JavaMapping("IBM037", "CP037");
      addIANA2JavaMapping("CSIBM037", "CP037");
      addIANA2JavaMapping("EBCDIC-CP-US", "CP037");
      addIANA2JavaMapping("EBCDIC-CP-CA", "CP037");
      addIANA2JavaMapping("EBCDIC-CP-NL", "CP037");
      addIANA2JavaMapping("EBCDIC-CP-WT", "CP037");
      addIANA2JavaMapping("IBM273", "CP273");
      addIANA2JavaMapping("CP273", "CP273");
      addIANA2JavaMapping("CSIBM273", "CP273");
      addIANA2JavaMapping("IBM277", "CP277");
      addIANA2JavaMapping("CP277", "CP277");
      addIANA2JavaMapping("CSIBM277", "CP277");
      addIANA2JavaMapping("EBCDIC-CP-DK", "CP277");
      addIANA2JavaMapping("EBCDIC-CP-NO", "CP277");
      addIANA2JavaMapping("IBM278", "CP278");
      addIANA2JavaMapping("CP278", "CP278");
      addIANA2JavaMapping("CSIBM278", "CP278");
      addIANA2JavaMapping("EBCDIC-CP-FI", "CP278");
      addIANA2JavaMapping("EBCDIC-CP-SE", "CP278");
      addIANA2JavaMapping("IBM280", "CP280");
      addIANA2JavaMapping("CP280", "CP280");
      addIANA2JavaMapping("CSIBM280", "CP280");
      addIANA2JavaMapping("EBCDIC-CP-IT", "CP280");
      addIANA2JavaMapping("IBM284", "CP284");
      addIANA2JavaMapping("CP284", "CP284");
      addIANA2JavaMapping("CSIBM284", "CP284");
      addIANA2JavaMapping("EBCDIC-CP-ES", "CP284");
      addIANA2JavaMapping("EBCDIC-CP-GB", "CP285");
      addIANA2JavaMapping("IBM285", "CP285");
      addIANA2JavaMapping("CP285", "CP285");
      addIANA2JavaMapping("CSIBM285", "CP285");
      addIANA2JavaMapping("EBCDIC-JP-KANA", "CP290");
      addIANA2JavaMapping("IBM290", "CP290");
      addIANA2JavaMapping("CP290", "CP290");
      addIANA2JavaMapping("CSIBM290", "CP290");
      addIANA2JavaMapping("EBCDIC-CP-FR", "CP297");
      addIANA2JavaMapping("IBM297", "CP297");
      addIANA2JavaMapping("CP297", "CP297");
      addIANA2JavaMapping("CSIBM297", "CP297");
      addIANA2JavaMapping("EBCDIC-CP-AR1", "CP420");
      addIANA2JavaMapping("IBM420", "CP420");
      addIANA2JavaMapping("CP420", "CP420");
      addIANA2JavaMapping("CSIBM420", "CP420");
      addIANA2JavaMapping("EBCDIC-CP-HE", "CP424");
      addIANA2JavaMapping("IBM424", "CP424");
      addIANA2JavaMapping("CP424", "CP424");
      addIANA2JavaMapping("CSIBM424", "CP424");
      addIANA2JavaMapping("IBM437", "CP437");
      addIANA2JavaMapping("437", "CP437");
      addIANA2JavaMapping("CP437", "CP437");
      addIANA2JavaMapping("CSPC8CODEPAGE437", "CP437");
      addIANA2JavaMapping("EBCDIC-CP-CH", "CP500");
      addIANA2JavaMapping("IBM500", "CP500");
      addIANA2JavaMapping("CP500", "CP500");
      addIANA2JavaMapping("CSIBM500", "CP500");
      addIANA2JavaMapping("EBCDIC-CP-CH", "CP500");
      addIANA2JavaMapping("EBCDIC-CP-BE", "CP500");
      addIANA2JavaMapping("IBM775", "CP775");
      addIANA2JavaMapping("CP775", "CP775");
      addIANA2JavaMapping("CSPC775BALTIC", "CP775");
      addIANA2JavaMapping("IBM850", "CP850");
      addIANA2JavaMapping("850", "CP850");
      addIANA2JavaMapping("CP850", "CP850");
      addIANA2JavaMapping("CSPC850MULTILINGUAL", "CP850");
      addIANA2JavaMapping("IBM852", "CP852");
      addIANA2JavaMapping("852", "CP852");
      addIANA2JavaMapping("CP852", "CP852");
      addIANA2JavaMapping("CSPCP852", "CP852");
      addIANA2JavaMapping("IBM855", "CP855");
      addIANA2JavaMapping("855", "CP855");
      addIANA2JavaMapping("CP855", "CP855");
      addIANA2JavaMapping("CSIBM855", "CP855");
      addIANA2JavaMapping("IBM857", "CP857");
      addIANA2JavaMapping("857", "CP857");
      addIANA2JavaMapping("CP857", "CP857");
      addIANA2JavaMapping("CSIBM857", "CP857");
      addIANA2JavaMapping("IBM00858", "CP858");
      addIANA2JavaMapping("CP00858", "CP858");
      addIANA2JavaMapping("CCSID00858", "CP858");
      addIANA2JavaMapping("IBM860", "CP860");
      addIANA2JavaMapping("860", "CP860");
      addIANA2JavaMapping("CP860", "CP860");
      addIANA2JavaMapping("CSIBM860", "CP860");
      addIANA2JavaMapping("IBM861", "CP861");
      addIANA2JavaMapping("861", "CP861");
      addIANA2JavaMapping("CP861", "CP861");
      addIANA2JavaMapping("CP-IS", "CP861");
      addIANA2JavaMapping("CSIBM861", "CP861");
      addIANA2JavaMapping("IBM862", "CP862");
      addIANA2JavaMapping("862", "CP862");
      addIANA2JavaMapping("CP862", "CP862");
      addIANA2JavaMapping("CSPC862LATINHEBREW", "CP862");
      addIANA2JavaMapping("IBM863", "CP863");
      addIANA2JavaMapping("863", "CP863");
      addIANA2JavaMapping("CP863", "CP863");
      addIANA2JavaMapping("CSIBM863", "CP863");
      addIANA2JavaMapping("IBM864", "CP864");
      addIANA2JavaMapping("CP864", "CP864");
      addIANA2JavaMapping("CSIBM864", "CP864");
      addIANA2JavaMapping("IBM865", "CP865");
      addIANA2JavaMapping("865", "CP865");
      addIANA2JavaMapping("CP865", "CP865");
      addIANA2JavaMapping("CSIBM865", "CP865");
      addIANA2JavaMapping("IBM866", "CP866");
      addIANA2JavaMapping("866", "CP866");
      addIANA2JavaMapping("CP866", "CP866");
      addIANA2JavaMapping("CSIBM866", "CP866");
      addIANA2JavaMapping("IBM868", "CP868");
      addIANA2JavaMapping("CP868", "CP868");
      addIANA2JavaMapping("CSIBM868", "CP868");
      addIANA2JavaMapping("CP-AR", "CP868");
      addIANA2JavaMapping("IBM869", "CP869");
      addIANA2JavaMapping("CP869", "CP869");
      addIANA2JavaMapping("CSIBM869", "CP869");
      addIANA2JavaMapping("CP-GR", "CP869");
      addIANA2JavaMapping("IBM870", "CP870");
      addIANA2JavaMapping("CP870", "CP870");
      addIANA2JavaMapping("CSIBM870", "CP870");
      addIANA2JavaMapping("EBCDIC-CP-ROECE", "CP870");
      addIANA2JavaMapping("EBCDIC-CP-YU", "CP870");
      addIANA2JavaMapping("IBM871", "CP871");
      addIANA2JavaMapping("CP871", "CP871");
      addIANA2JavaMapping("CSIBM871", "CP871");
      addIANA2JavaMapping("EBCDIC-CP-IS", "CP871");
      addIANA2JavaMapping("IBM918", "CP918");
      addIANA2JavaMapping("CP918", "CP918");
      addIANA2JavaMapping("CSIBM918", "CP918");
      addIANA2JavaMapping("EBCDIC-CP-AR2", "CP918");
      addIANA2JavaMapping("IBM00924", "CP924");
      addIANA2JavaMapping("CP00924", "CP924");
      addIANA2JavaMapping("CCSID00924", "CP924");
      // is this an error???
      addIANA2JavaMapping("EBCDIC-LATIN9--EURO", "CP924");
      addIANA2JavaMapping("IBM1026", "CP1026");
      addIANA2JavaMapping("CP1026", "CP1026");
      addIANA2JavaMapping("CSIBM1026", "CP1026");
      addIANA2JavaMapping("IBM01140", "Cp1140");
      addIANA2JavaMapping("CP01140", "Cp1140");
      addIANA2JavaMapping("CCSID01140", "Cp1140");
      addIANA2JavaMapping("IBM01141", "Cp1141");
      addIANA2JavaMapping("CP01141", "Cp1141");
      addIANA2JavaMapping("CCSID01141", "Cp1141");
      addIANA2JavaMapping("IBM01142", "Cp1142");
      addIANA2JavaMapping("CP01142", "Cp1142");
      addIANA2JavaMapping("CCSID01142", "Cp1142");
      addIANA2JavaMapping("IBM01143", "Cp1143");
      addIANA2JavaMapping("CP01143", "Cp1143");
      addIANA2JavaMapping("CCSID01143", "Cp1143");
      addIANA2JavaMapping("IBM01144", "Cp1144");
      addIANA2JavaMapping("CP01144", "Cp1144");
      addIANA2JavaMapping("CCSID01144", "Cp1144");
      addIANA2JavaMapping("IBM01145", "Cp1145");
      addIANA2JavaMapping("CP01145", "Cp1145");
      addIANA2JavaMapping("CCSID01145", "Cp1145");
      addIANA2JavaMapping("IBM01146", "Cp1146");
      addIANA2JavaMapping("CP01146", "Cp1146");
      addIANA2JavaMapping("CCSID01146", "Cp1146");
      addIANA2JavaMapping("IBM01147", "Cp1147");
      addIANA2JavaMapping("CP01147", "Cp1147");
      addIANA2JavaMapping("CCSID01147", "Cp1147");
      addIANA2JavaMapping("IBM01148", "Cp1148");
      addIANA2JavaMapping("CP01148", "Cp1148");
      addIANA2JavaMapping("CCSID01148", "Cp1148");
      addIANA2JavaMapping("IBM01149", "Cp1149");
      addIANA2JavaMapping("CP01149", "Cp1149");
      addIANA2JavaMapping("CCSID01149", "Cp1149");
      addIANA2JavaMapping("EUC-JP", "EUCJIS");
      addIANA2JavaMapping("CSEUCPKDFMTJAPANESE", "EUCJIS");
      addIANA2JavaMapping("EXTENDED_UNIX_CODE_PACKED_FORMAT_FOR_JAPANESE", "EUCJIS");
      addIANA2JavaMapping("EUC-KR", "KSC5601");
      addIANA2JavaMapping("CSEUCKR", "KSC5601");
      addIANA2JavaMapping("KS_C_5601-1987", "KS_C_5601-1987");
      addIANA2JavaMapping("ISO-IR-149", "KS_C_5601-1987");
      addIANA2JavaMapping("KS_C_5601-1989", "KS_C_5601-1987");
      addIANA2JavaMapping("KSC_5601", "KS_C_5601-1987");
      addIANA2JavaMapping("KOREAN", "KS_C_5601-1987");
      addIANA2JavaMapping("CSKSC56011987", "KS_C_5601-1987");
      addIANA2JavaMapping("GB2312", "GB2312");
      addIANA2JavaMapping("CSGB2312", "GB2312");
      addIANA2JavaMapping("ISO-2022-JP", "JIS");
      addIANA2JavaMapping("CSISO2022JP", "JIS");
      addIANA2JavaMapping("ISO-2022-KR", "ISO2022KR");
      addIANA2JavaMapping("CSISO2022KR", "ISO2022KR");
      addIANA2JavaMapping("ISO-2022-CN", "ISO2022CN");

      addIANA2JavaMapping("X0201", "JIS0201");
      addIANA2JavaMapping("CSISO13JISC6220JP", "JIS0201");
      addIANA2JavaMapping("X0208", "JIS0208");
      addIANA2JavaMapping("ISO-IR-87", "JIS0208");
      addIANA2JavaMapping("X0208dbiJIS_X0208-1983", "JIS0208");
      addIANA2JavaMapping("CSISO87JISX0208", "JIS0208");
      addIANA2JavaMapping("X0212", "JIS0212");
      addIANA2JavaMapping("ISO-IR-159", "JIS0212");
      addIANA2JavaMapping("CSISO159JISX02121990", "JIS0212");
      addIANA2JavaMapping("GB18030", "GB18030");
      addIANA2JavaMapping("GBK", "GBK");
      addIANA2JavaMapping("CP936", "GBK");
      addIANA2JavaMapping("MS936", "GBK");
      addIANA2JavaMapping("WINDOWS-936", "GBK");
      addIANA2JavaMapping("SHIFT_JIS", "SJIS");
      addIANA2JavaMapping("CSSHIFTJIS", "SJIS");
      addIANA2JavaMapping("MS_KANJI", "SJIS");
      addIANA2JavaMapping("WINDOWS-31J", "MS932");
      addIANA2JavaMapping("CSWINDOWS31J", "MS932");

      // Add support for Cp1252 and its friends
      addIANA2JavaMapping("WINDOWS-1250", "Cp1250");
      addIANA2JavaMapping("WINDOWS-1251", "Cp1251");
      addIANA2JavaMapping("WINDOWS-1252", "Cp1252");
      addIANA2JavaMapping("WINDOWS-1253", "Cp1253");
      addIANA2JavaMapping("WINDOWS-1254", "Cp1254");
      addIANA2JavaMapping("WINDOWS-1255", "Cp1255");
      addIANA2JavaMapping("WINDOWS-1256", "Cp1256");
      addIANA2JavaMapping("WINDOWS-1257", "Cp1257");
      addIANA2JavaMapping("WINDOWS-1258", "Cp1258");
      addIANA2JavaMapping("TIS-620", "TIS620");

      addIANA2JavaMapping("ISO-8859-1", "ISO8859_1");
      addIANA2JavaMapping("ISO-IR-100", "ISO8859_1");
      addIANA2JavaMapping("ISO_8859-1", "ISO8859_1");
      addIANA2JavaMapping("LATIN1", "ISO8859_1");
      addIANA2JavaMapping("CSISOLATIN1", "ISO8859_1");
      addIANA2JavaMapping("L1", "ISO8859_1");
      addIANA2JavaMapping("IBM819", "ISO8859_1");
      addIANA2JavaMapping("CP819", "ISO8859_1");

      addIANA2JavaMapping("ISO-8859-2", "ISO8859_2");
      addIANA2JavaMapping("ISO-IR-101", "ISO8859_2");
      addIANA2JavaMapping("ISO_8859-2", "ISO8859_2");
      addIANA2JavaMapping("LATIN2", "ISO8859_2");
      addIANA2JavaMapping("CSISOLATIN2", "ISO8859_2");
      addIANA2JavaMapping("L2", "ISO8859_2");

      addIANA2JavaMapping("ISO-8859-3", "ISO8859_3");
      addIANA2JavaMapping("ISO-IR-109", "ISO8859_3");
      addIANA2JavaMapping("ISO_8859-3", "ISO8859_3");
      addIANA2JavaMapping("LATIN3", "ISO8859_3");
      addIANA2JavaMapping("CSISOLATIN3", "ISO8859_3");
      addIANA2JavaMapping("L3", "ISO8859_3");

      addIANA2JavaMapping("ISO-8859-4", "ISO8859_4");
      addIANA2JavaMapping("ISO-IR-110", "ISO8859_4");
      addIANA2JavaMapping("ISO_8859-4", "ISO8859_4");
      addIANA2JavaMapping("LATIN4", "ISO8859_4");
      addIANA2JavaMapping("CSISOLATIN4", "ISO8859_4");
      addIANA2JavaMapping("L4", "ISO8859_4");

      addIANA2JavaMapping("ISO-8859-5", "ISO8859_5");
      addIANA2JavaMapping("ISO-IR-144", "ISO8859_5");
      addIANA2JavaMapping("ISO_8859-5", "ISO8859_5");
      addIANA2JavaMapping("CYRILLIC", "ISO8859_5");
      addIANA2JavaMapping("CSISOLATINCYRILLIC", "ISO8859_5");

      addIANA2JavaMapping("ISO-8859-6", "ISO8859_6");
      addIANA2JavaMapping("ISO-IR-127", "ISO8859_6");
      addIANA2JavaMapping("ISO_8859-6", "ISO8859_6");
      addIANA2JavaMapping("ECMA-114", "ISO8859_6");
      addIANA2JavaMapping("ASMO-708", "ISO8859_6");
      addIANA2JavaMapping("ARABIC", "ISO8859_6");
      addIANA2JavaMapping("CSISOLATINARABIC", "ISO8859_6");

      addIANA2JavaMapping("ISO-8859-7", "ISO8859_7");
      addIANA2JavaMapping("ISO-IR-126", "ISO8859_7");
      addIANA2JavaMapping("ISO_8859-7", "ISO8859_7");
      addIANA2JavaMapping("ELOT_928", "ISO8859_7");
      addIANA2JavaMapping("ECMA-118", "ISO8859_7");
      addIANA2JavaMapping("GREEK", "ISO8859_7");
      addIANA2JavaMapping("CSISOLATINGREEK", "ISO8859_7");
      addIANA2JavaMapping("GREEK8", "ISO8859_7");

      addIANA2JavaMapping("ISO-8859-8", "ISO8859_8");
      addIANA2JavaMapping("ISO-8859-8-I", "ISO8859_8"); // added since this
      // encoding only differs
      // w.r.t. presentation
      addIANA2JavaMapping("ISO-IR-138", "ISO8859_8");
      addIANA2JavaMapping("ISO_8859-8", "ISO8859_8");
      addIANA2JavaMapping("HEBREW", "ISO8859_8");
      addIANA2JavaMapping("CSISOLATINHEBREW", "ISO8859_8");

      addIANA2JavaMapping("ISO-8859-9", "ISO8859_9");
      addIANA2JavaMapping("ISO-IR-148", "ISO8859_9");
      addIANA2JavaMapping("ISO_8859-9", "ISO8859_9");
      addIANA2JavaMapping("LATIN5", "ISO8859_9");
      addIANA2JavaMapping("CSISOLATIN5", "ISO8859_9");
      addIANA2JavaMapping("L5", "ISO8859_9");

      addIANA2JavaMapping("ISO-8859-13", "ISO8859_13");

      addIANA2JavaMapping("ISO-8859-15", "ISO8859_15_FDIS");
      addIANA2JavaMapping("ISO_8859-15", "ISO8859_15_FDIS");
      addIANA2JavaMapping("LATIN-9", "ISO8859_15_FDIS");

      addIANA2JavaMapping("KOI8-R", "KOI8_R");
      addIANA2JavaMapping("CSKOI8R", "KOI8_R");
      addIANA2JavaMapping("US-ASCII", "ASCII");
      addIANA2JavaMapping("ISO-IR-6", "ASCII");
      addIANA2JavaMapping("ANSI_X3.4-1968", "ASCII");
      addIANA2JavaMapping("ANSI_X3.4-1986", "ASCII");
      addIANA2JavaMapping("ISO_646.IRV:1991", "ASCII");
      addIANA2JavaMapping("ASCII", "ASCII");
      addIANA2JavaMapping("CSASCII", "ASCII");
      addIANA2JavaMapping("ISO646-US", "ASCII");
      addIANA2JavaMapping("US", "ASCII");
      addIANA2JavaMapping("IBM367", "ASCII");
      addIANA2JavaMapping("CP367", "ASCII");
      addIANA2JavaMapping("UTF-8", "UTF-8");
      addIANA2JavaMapping("UTF-16", "UTF-16");
      addIANA2JavaMapping("UTF-16BE", "UnicodeBig");
      addIANA2JavaMapping("UTF-16LE", "UnicodeLittle");

      // support for 1047, as proposed to be added to the
      // IANA registry in
      // http://lists.w3.org/Archives/Public/ietf-charset/2002JulSep/0049.html
      addIANA2JavaMapping("IBM-1047", "Cp1047");
      addIANA2JavaMapping("IBM1047", "Cp1047");
      addIANA2JavaMapping("CP1047", "Cp1047");

      // Adding new aliases as proposed in
      // http://lists.w3.org/Archives/Public/ietf-charset/2002JulSep/0058.html
      addIANA2JavaMapping("IBM-37", "CP037");
      addIANA2JavaMapping("IBM-273", "CP273");
      addIANA2JavaMapping("IBM-277", "CP277");
      addIANA2JavaMapping("IBM-278", "CP278");
      addIANA2JavaMapping("IBM-280", "CP280");
      addIANA2JavaMapping("IBM-284", "CP284");
      addIANA2JavaMapping("IBM-285", "CP285");
      addIANA2JavaMapping("IBM-290", "CP290");
      addIANA2JavaMapping("IBM-297", "CP297");
      addIANA2JavaMapping("IBM-420", "CP420");
      addIANA2JavaMapping("IBM-424", "CP424");
      addIANA2JavaMapping("IBM-437", "CP437");
      addIANA2JavaMapping("IBM-500", "CP500");
      addIANA2JavaMapping("IBM-775", "CP775");
      addIANA2JavaMapping("IBM-850", "CP850");
      addIANA2JavaMapping("IBM-852", "CP852");
      addIANA2JavaMapping("IBM-855", "CP855");
      addIANA2JavaMapping("IBM-857", "CP857");
      addIANA2JavaMapping("IBM-858", "CP858");
      addIANA2JavaMapping("IBM-860", "CP860");
      addIANA2JavaMapping("IBM-861", "CP861");
      addIANA2JavaMapping("IBM-862", "CP862");
      addIANA2JavaMapping("IBM-863", "CP863");
      addIANA2JavaMapping("IBM-864", "CP864");
      addIANA2JavaMapping("IBM-865", "CP865");
      addIANA2JavaMapping("IBM-866", "CP866");
      addIANA2JavaMapping("IBM-868", "CP868");
      addIANA2JavaMapping("IBM-869", "CP869");
      addIANA2JavaMapping("IBM-870", "CP870");
      addIANA2JavaMapping("IBM-871", "CP871");
      addIANA2JavaMapping("IBM-918", "CP918");
      addIANA2JavaMapping("IBM-924", "CP924");
      addIANA2JavaMapping("IBM-1026", "CP1026");
      addIANA2JavaMapping("IBM-1140", "Cp1140");
      addIANA2JavaMapping("IBM-1141", "Cp1141");
      addIANA2JavaMapping("IBM-1142", "Cp1142");
      addIANA2JavaMapping("IBM-1143", "Cp1143");
      addIANA2JavaMapping("IBM-1144", "Cp1144");
      addIANA2JavaMapping("IBM-1145", "Cp1145");
      addIANA2JavaMapping("IBM-1146", "Cp1146");
      addIANA2JavaMapping("IBM-1147", "Cp1147");
      addIANA2JavaMapping("IBM-1148", "Cp1148");
      addIANA2JavaMapping("IBM-1149", "Cp1149");
      addIANA2JavaMapping("IBM-819", "ISO8859_1");
      addIANA2JavaMapping("IBM-367", "ASCII");
      // https://jira.jboss.org/jira/browse/EXOJCR-588
      addIANA2JavaMapping("x-MacRoman", "MacRoman");
   }
}
