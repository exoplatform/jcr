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
package org.exoplatform.services.jcr.impl.xml;

/**
 * This class defines the basic XML character properties. The data in this class can be used to
 * verify that a character is a valid XML character or if the character is a space, name start, or
 * name character.
 */
public class XMLChar
{
   /**
    * Returns true if the specified character is a supplemental character.
    * 
    * @param ch
    *          The character to check.
    * @return
    */
   public static boolean isSupplemental(int ch)
   {
      return (ch >= 0x10000 && ch <= 0x10FFFF);
   }

   /**
    * Returns true the supplemental character corresponding to the given surrogates.
    * 
    * @param h
    *          The high surrogate.
    * @param l
    *          The low surrogate.
    */
   public static int supplemental(char h, char l)
   {
      return (h - 0xD800) * 0x400 + (l - 0xDC00) + 0x10000;
   }

   /**
    * Returns the high surrogate of a supplemental character
    * 
    * @param c
    *          The supplemental character to "split".
    */
   public static char highSurrogate(int ch)
   {
      return (char)(((ch - 0x00010000) >> 10) + 0xD800);
   }

   /**
    * Returns the low surrogate of a supplemental character
    * 
    * @param c
    *          The supplemental character to "split".
    */
   public static char lowSurrogate(int ch)
   {
      return (char)(((ch - 0x00010000) & 0x3FF) + 0xDC00);
   }

   /**
    * Returns whether the given character is a high surrogate
    * 
    * @param c
    *          The character to check.
    */
   public static boolean isHighSurrogate(int ch)
   {
      return (0xD800 <= ch && ch <= 0xDBFF);
   }

   /**
    * Returns whether the given character is a low surrogate
    * 
    * @param ch
    *          The character to check.
    */
   public static boolean isLowSurrogate(int ch)
   {
      return (0xDC00 <= ch && ch <= 0xDFFF);
   }

   /**
    * Returns true if the specified character can be considered markup. Markup characters include
    * '&lt;', '&amp;', and '%'.
    * 
    * @param c
    *          The character to check.
    */
   public static boolean isMarkup(int c)
   {
      return c == '<' || c == '&' || c == '%';
   }

   /**
    * Returns true if the specified character is a character which may represent markup or character
    * data as defined by production [2] in the XML 1.0 specification. <b>Char
    * ::=#x9|#xA|#xD|[#x20-#xD7FF]|[#xE000-#xFFFD]|[#x10000-#x10FFFF]</b>
    * 
    * @param ch
    *          The character to check.
    */
   public static boolean isChar(int ch)
   {
      return 0x09 == ch // TAB
         || 0x0a == ch // LF
         || 0x0d == ch // CR
         || (0x20 <= ch && ch <= 0xD7FF) || (0xE000 <= ch && ch <= 0xFFFD) || (0x10000 <= ch && ch <= 0x10FFFF);

   }

   /**
    * Returns true if the specified character is valid as defined by production [2] in the XML 1.0
    * specification. Include for backward compatibility with org.apache.xerces.util.XMLChar
    * 
    * @param ch
    *          The character to check.
    * @see org.apache.xerces.util.XMLChar#isValid
    */
   public static boolean isValid(int ch)
   {
      return isChar(ch);
   }

   /**
    * Returns true if the specified character is invalid.
    * 
    * @param ch
    *          The character to check.
    */
   public static boolean isInvalid(int ch)
   {
      return !isValid(ch);
   }

   /**
    * Returns true if the specified character is a space character as defined by production [3] in
    * the XML 1.0 specification. <b>S ::= (#x20 | #x9 | #xD | #xA)+</b>
    * 
    * @param ch
    *          The character to check.
    */
   public static boolean isSpace(int ch)
   {
      return 0x20 == ch // SPACE
         || 0x09 == ch // TAB
         || 0x0d == ch // CR
         || 0x0a == ch; // LF
   }

   /**
    * Returns true if the specified character is a digit as defined by production [88] in the XML 1.0
    * specification.
    * 
    * @param ch
    *          The character to check.
    */
   public static boolean isDigit(int ch)
   {
      return (0x0030 <= ch && ch <= 0x0039) || (0x0660 <= ch && ch <= 0x0669) || (0x06F0 <= ch && ch <= 0x06F9)
         || (0x0966 <= ch && ch <= 0x096F) || (0x09E6 <= ch && ch <= 0x09EF) || (0x0A66 <= ch && ch <= 0x0A6F)
         || (0x0AE6 <= ch && ch <= 0x0AEF) || (0x0B66 <= ch && ch <= 0x0B6F) || (0x0BE7 <= ch && ch <= 0x0BEF)
         || (0x0C66 <= ch && ch <= 0x0C6F) || (0x0CE6 <= ch && ch <= 0x0CEF) || (0x0D66 <= ch && ch <= 0x0D6F)
         || (0x0E50 <= ch && ch <= 0x0E59) || (0x0ED0 <= ch && ch <= 0x0ED9) || (0x0F20 <= ch && ch <= 0x0F29);
   }

   /**
    * Returns true if the specified character is a BaseChar as defined by production [85] in the XML
    * 1.0 specification.
    * 
    * @param ch
    *          The character to check.
    */
   public static boolean isBaseChar(int ch)
   {
      return (0x0041 <= ch && ch <= 0x005A) || (0x0061 <= ch && ch <= 0x007A) || (0x00C0 <= ch && ch <= 0x00D6)
         || (0x00D8 <= ch && ch <= 0x00F6) || (0x00F8 <= ch && ch <= 0x00FF) || (0x0100 <= ch && ch <= 0x0131)
         || (0x0134 <= ch && ch <= 0x013E) || (0x0141 <= ch && ch <= 0x0148) || (0x014A <= ch && ch <= 0x017E)
         || (0x0180 <= ch && ch <= 0x01C3) || (0x01CD <= ch && ch <= 0x01F0) || (0x01F4 <= ch && ch <= 0x01F5)
         || (0x01FA <= ch && ch <= 0x0217) || (0x0250 <= ch && ch <= 0x02A8) || (0x02BB <= ch && ch <= 0x02C1)
         || 0x0386 == ch || (0x0388 <= ch && ch <= 0x038A) || 0x038C == ch || (0x038E <= ch && ch <= 0x03A1)
         || (0x03A3 <= ch && ch <= 0x03CE) || (0x03D0 <= ch && ch <= 0x03D6) || 0x03DA == ch || 0x03DC == ch
         || 0x03DE == ch || 0x03E0 == ch || (0x03E2 <= ch && ch <= 0x03F3) || (0x0401 <= ch && ch <= 0x040C)
         || (0x040E <= ch && ch <= 0x044F) || (0x0451 <= ch && ch <= 0x045C) || (0x045E <= ch && ch <= 0x0481)
         || (0x0490 <= ch && ch <= 0x04C4) || (0x04C7 <= ch && ch <= 0x04C8) || (0x04CB <= ch && ch <= 0x04CC)
         || (0x04D0 <= ch && ch <= 0x04EB) || (0x04EE <= ch && ch <= 0x04F5) || (0x04F8 <= ch && ch <= 0x04F9)
         || (0x0531 <= ch && ch <= 0x0556) || 0x0559 == ch || (0x0561 <= ch && ch <= 0x0586)
         || (0x05D0 <= ch && ch <= 0x05EA) || (0x05F0 <= ch && ch <= 0x05F2) || (0x0621 <= ch && ch <= 0x063A)
         || (0x0641 <= ch && ch <= 0x064A) || (0x0671 <= ch && ch <= 0x06B7) || (0x06BA <= ch && ch <= 0x06BE)
         || (0x06C0 <= ch && ch <= 0x06CE) || (0x06D0 <= ch && ch <= 0x06D3) || 0x06D5 == ch
         || (0x06E5 <= ch && ch <= 0x06E6) || (0x0905 <= ch && ch <= 0x0939) || 0x093D == ch
         || (0x0958 <= ch && ch <= 0x0961) || (0x0985 <= ch && ch <= 0x098C) || (0x098F <= ch && ch <= 0x0990)
         || (0x0993 <= ch && ch <= 0x09A8) || (0x09AA <= ch && ch <= 0x09B0) || 0x09B2 == ch
         || (0x09B6 <= ch && ch <= 0x09B9) || (0x09DC <= ch && ch <= 0x09DD) || (0x09DF <= ch && ch <= 0x09E1)
         || (0x09F0 <= ch && ch <= 0x09F1) || (0x0A05 <= ch && ch <= 0x0A0A) || (0x0A0F <= ch && ch <= 0x0A10)
         || (0x0A13 <= ch && ch <= 0x0A28) || (0x0A2A <= ch && ch <= 0x0A30) || (0x0A32 <= ch && ch <= 0x0A33)
         || (0x0A35 <= ch && ch <= 0x0A36) || (0x0A38 <= ch && ch <= 0x0A39) || (0x0A59 <= ch && ch <= 0x0A5C)
         || 0x0A5E == ch || (0x0A72 <= ch && ch <= 0x0A74) || (0x0A85 <= ch && ch <= 0x0A8B) || 0x0A8D == ch
         || (0x0A8F <= ch && ch <= 0x0A91) || (0x0A93 <= ch && ch <= 0x0AA8) || (0x0AAA <= ch && ch <= 0x0AB0)
         || (0x0AB2 <= ch && ch <= 0x0AB3) || (0x0AB5 <= ch && ch <= 0x0AB9) || 0x0ABD == ch || 0x0AE0 == ch
         || (0x0B05 <= ch && ch <= 0x0B0C) || (0x0B0F <= ch && ch <= 0x0B10) || (0x0B13 <= ch && ch <= 0x0B28)
         || (0x0B2A <= ch && ch <= 0x0B30) || (0x0B32 <= ch && ch <= 0x0B33) || (0x0B36 <= ch && ch <= 0x0B39)
         || 0x0B3D == ch || (0x0B5C <= ch && ch <= 0x0B5D) || (0x0B5F <= ch && ch <= 0x0B61)
         || (0x0B85 <= ch && ch <= 0x0B8A) || (0x0B8E <= ch && ch <= 0x0B90) || (0x0B92 <= ch && ch <= 0x0B95)
         || (0x0B99 <= ch && ch <= 0x0B9A) || 0x0B9C == ch || (0x0B9E <= ch && ch <= 0x0B9F)
         || (0x0BA3 <= ch && ch <= 0x0BA4) || (0x0BA8 <= ch && ch <= 0x0BAA) || (0x0BAE <= ch && ch <= 0x0BB5)
         || (0x0BB7 <= ch && ch <= 0x0BB9) || (0x0C05 <= ch && ch <= 0x0C0C) || (0x0C0E <= ch && ch <= 0x0C10)
         || (0x0C12 <= ch && ch <= 0x0C28) || (0x0C2A <= ch && ch <= 0x0C33) || (0x0C35 <= ch && ch <= 0x0C39)
         || (0x0C60 <= ch && ch <= 0x0C61) || (0x0C85 <= ch && ch <= 0x0C8C) || (0x0C8E <= ch && ch <= 0x0C90)
         || (0x0C92 <= ch && ch <= 0x0CA8) || (0x0CAA <= ch && ch <= 0x0CB3) || (0x0CB5 <= ch && ch <= 0x0CB9)
         || 0x0CDE == ch || (0x0CE0 <= ch && ch <= 0x0CE1) || (0x0D05 <= ch && ch <= 0x0D0C)
         || (0x0D0E <= ch && ch <= 0x0D10) || (0x0D12 <= ch && ch <= 0x0D28) || (0x0D2A <= ch && ch <= 0x0D39)
         || (0x0D60 <= ch && ch <= 0x0D61) || (0x0E01 <= ch && ch <= 0x0E2E) || 0x0E30 == ch
         || (0x0E32 <= ch && ch <= 0x0E33) || (0x0E40 <= ch && ch <= 0x0E45) || (0x0E81 <= ch && ch <= 0x0E82)
         || 0x0E84 == ch || (0x0E87 <= ch && ch <= 0x0E88) || 0x0E8A == ch || 0x0E8D == ch
         || (0x0E94 <= ch && ch <= 0x0E97) || (0x0E99 <= ch && ch <= 0x0E9F) || (0x0EA1 <= ch && ch <= 0x0EA3)
         || 0x0EA5 == ch || 0x0EA7 == ch || (0x0EAA <= ch && ch <= 0x0EAB) || (0x0EAD <= ch && ch <= 0x0EAE)
         || 0x0EB0 == ch || (0x0EB2 <= ch && ch <= 0x0EB3) || 0x0EBD == ch || (0x0EC0 <= ch && ch <= 0x0EC4)
         || (0x0F40 <= ch && ch <= 0x0F47) || (0x0F49 <= ch && ch <= 0x0F69) || (0x10A0 <= ch && ch <= 0x10C5)
         || (0x10D0 <= ch && ch <= 0x10F6) || 0x1100 == ch || (0x1102 <= ch && ch <= 0x1103)
         || (0x1105 <= ch && ch <= 0x1107) || 0x1109 == ch || (0x110B <= ch && ch <= 0x110C)
         || (0x110E <= ch && ch <= 0x1112) || 0x113C == ch || 0x113E == ch || 0x1140 == ch || 0x114C == ch
         || 0x114E == ch || 0x1150 == ch || (0x1154 <= ch && ch <= 0x1155) || 0x1159 == ch
         || (0x115F <= ch && ch <= 0x1161) || 0x1163 == ch || 0x1165 == ch || 0x1167 == ch || 0x1169 == ch
         || (0x116D <= ch && ch <= 0x116E) || (0x1172 <= ch && ch <= 0x1173) || 0x1175 == ch || 0x119E == ch
         || 0x11A8 == ch || 0x11AB == ch || (0x11AE <= ch && ch <= 0x11AF) || (0x11B7 <= ch && ch <= 0x11B8)
         || 0x11BA == ch || (0x11BC <= ch && ch <= 0x11C2) || 0x11EB == ch || 0x11F0 == ch || 0x11F9 == ch
         || (0x1E00 <= ch && ch <= 0x1E9B) || (0x1EA0 <= ch && ch <= 0x1EF9) || (0x1F00 <= ch && ch <= 0x1F15)
         || (0x1F18 <= ch && ch <= 0x1F1D) || (0x1F20 <= ch && ch <= 0x1F45) || (0x1F48 <= ch && ch <= 0x1F4D)
         || (0x1F50 <= ch && ch <= 0x1F57) || 0x1F59 == ch || 0x1F5B == ch || 0x1F5D == ch
         || (0x1F5F <= ch && ch <= 0x1F7D) || (0x1F80 <= ch && ch <= 0x1FB4) || (0x1FB6 <= ch && ch <= 0x1FBC)
         || 0x1FBE == ch || (0x1FC2 <= ch && ch <= 0x1FC4) || (0x1FC6 <= ch && ch <= 0x1FCC)
         || (0x1FD0 <= ch && ch <= 0x1FD3) || (0x1FD6 <= ch && ch <= 0x1FDB) || (0x1FE0 <= ch && ch <= 0x1FEC)
         || (0x1FF2 <= ch && ch <= 0x1FF4) || (0x1FF6 <= ch && ch <= 0x1FFC) || 0x2126 == ch
         || (0x212A <= ch && ch <= 0x212B) || 0x212E == ch || (0x2180 <= ch && ch <= 0x2182)
         || (0x3041 <= ch && ch <= 0x3094) || (0x30A1 <= ch && ch <= 0x30FA) || (0x3105 <= ch && ch <= 0x312C)
         || (0xAC00 <= ch && ch <= 0xD7A3);
   }

   /**
    * Returns true if the specified character is a Ideographic as defined by production [86] in the
    * XML 1.0 specification.
    * 
    * @param ch
    *          The character to check.
    */
   public static boolean isIdeographic(int ch)
   {
      return (0x4E00 <= ch && ch <= 0x9FA5) || 0x3007 == ch || (0x3021 <= ch && ch <= 0x3029);
   }

   /**
    * Returns true if the specified character is a letter as defined by production [84] in the XML
    * 1.0 specification. <b>Letter ::= BaseChar | Ideographic</b>
    * 
    * @param ch
    *          The character to check.
    */
   public static boolean isLetter(int ch)
   {
      return isBaseChar(ch) || isIdeographic(ch);
   }

   /**
    * Returns true if the specified character is a combining char as defined by production [87] in
    * the XML 1.0 specification.
    * 
    * @param ch
    *          The character to check.
    */
   public static boolean isCombiningChar(int ch)
   {
      return (0x0300 <= ch && ch <= 0x0345) || (0x0360 <= ch && ch <= 0x0361) || (0x0483 <= ch && ch <= 0x0486)
         || (0x0591 <= ch && ch <= 0x05A1) || (0x05A3 <= ch && ch <= 0x05B9) || (0x05BB <= ch && ch <= 0x05BD)
         || 0x05BF == ch || (0x05C1 <= ch && ch <= 0x05C2) || 0x05C4 == ch || (0x064B <= ch && ch <= 0x0652)
         || 0x0670 == ch || (0x06D6 <= ch && ch <= 0x06DC) || (0x06DD <= ch && ch <= 0x06DF)
         || (0x06E0 <= ch && ch <= 0x06E4) || (0x06E7 <= ch && ch <= 0x06E8) || (0x06EA <= ch && ch <= 0x06ED)
         || (0x0901 <= ch && ch <= 0x0903) || 0x093C == ch || (0x093E <= ch && ch <= 0x094C) || 0x094D == ch
         || (0x0951 <= ch && ch <= 0x0954) || (0x0962 <= ch && ch <= 0x0963) || (0x0981 <= ch && ch <= 0x0983)
         || 0x09BC == ch || 0x09BE == ch || 0x09BF == ch || (0x09C0 <= ch && ch <= 0x09C4)
         || (0x09C7 <= ch && ch <= 0x09C8) || (0x09CB <= ch && ch <= 0x09CD) || 0x09D7 == ch
         || (0x09E2 <= ch && ch <= 0x09E3) || 0x0A02 == ch || 0x0A3C == ch || 0x0A3E == ch || 0x0A3F == ch
         || (0x0A40 <= ch && ch <= 0x0A42) || (0x0A47 <= ch && ch <= 0x0A48) || (0x0A4B <= ch && ch <= 0x0A4D)
         || (0x0A70 <= ch && ch <= 0x0A71) || (0x0A81 <= ch && ch <= 0x0A83) || 0x0ABC == ch
         || (0x0ABE <= ch && ch <= 0x0AC5) || (0x0AC7 <= ch && ch <= 0x0AC9) || (0x0ACB <= ch && ch <= 0x0ACD)
         || (0x0B01 <= ch && ch <= 0x0B03) || 0x0B3C == ch || (0x0B3E <= ch && ch <= 0x0B43)
         || (0x0B47 <= ch && ch <= 0x0B48) || (0x0B4B <= ch && ch <= 0x0B4D) || (0x0B56 <= ch && ch <= 0x0B57)
         || (0x0B82 <= ch && ch <= 0x0B83) || (0x0BBE <= ch && ch <= 0x0BC2) || (0x0BC6 <= ch && ch <= 0x0BC8)
         || (0x0BCA <= ch && ch <= 0x0BCD) || 0x0BD7 == ch || (0x0C01 <= ch && ch <= 0x0C03)
         || (0x0C3E <= ch && ch <= 0x0C44) || (0x0C46 <= ch && ch <= 0x0C48) || (0x0C4A <= ch && ch <= 0x0C4D)
         || (0x0C55 <= ch && ch <= 0x0C56) || (0x0C82 <= ch && ch <= 0x0C83) || (0x0CBE <= ch && ch <= 0x0CC4)
         || (0x0CC6 <= ch && ch <= 0x0CC8) || (0x0CCA <= ch && ch <= 0x0CCD) || (0x0CD5 <= ch && ch <= 0x0CD6)
         || (0x0D02 <= ch && ch <= 0x0D03) || (0x0D3E <= ch && ch <= 0x0D43) || (0x0D46 <= ch && ch <= 0x0D48)
         || (0x0D4A <= ch && ch <= 0x0D4D) || 0x0D57 == ch || 0x0E31 == ch || (0x0E34 <= ch && ch <= 0x0E3A)
         || (0x0E47 <= ch && ch <= 0x0E4E) || 0x0EB1 == ch || (0x0EB4 <= ch && ch <= 0x0EB9)
         || (0x0EBB <= ch && ch <= 0x0EBC) || (0x0EC8 <= ch && ch <= 0x0ECD) || (0x0F18 <= ch && ch <= 0x0F19)
         || 0x0F35 == ch || 0x0F37 == ch || 0x0F39 == ch || 0x0F3E == ch || 0x0F3F == ch
         || (0x0F71 <= ch && ch <= 0x0F84) || (0x0F86 <= ch && ch <= 0x0F8B) || (0x0F90 <= ch && ch <= 0x0F95)
         || 0x0F97 == ch || (0x0F99 <= ch && ch <= 0x0FAD) || (0x0FB1 <= ch && ch <= 0x0FB7) || 0x0FB9 == ch
         || (0x20D0 <= ch && ch <= 0x20DC) || 0x20E1 == ch || (0x302A <= ch && ch <= 0x302F) || 0x3099 == ch
         || 0x309A == ch;
   }

   /**
    * Returns true if the specified character is a extender as defined by production [89] in the XML
    * 1.0 specification.
    * 
    * @param ch
    *          The character to check.
    */
   public static boolean isExtender(int ch)
   {
      return 0x00B7 == ch || 0x02D0 == ch || 0x02D1 == ch || 0x0387 == ch || 0x0640 == ch || 0x0E46 == ch
         || 0x0EC6 == ch || 0x3005 == ch || (0x3031 <= ch && ch <= 0x3035) || (0x309D <= ch && ch <= 0x309E)
         || (0x30FC <= ch && ch <= 0x30FE);
   }

   /**
    * Returns true if the specified character is a valid name character as defined by production [4]
    * in the XML 1.0 specification. <b>NameChar ::= Letter | Digit | '.' | '-' | '_' | ':' |
    * CombiningChar | Extender</b>
    * 
    * @param ch
    *          The character to check.
    */
   public static boolean isName(int ch)
   {
      return isLetter(ch) || isDigit(ch) || '.' == ch || '-' == ch || '_' == ch || ':' == ch || isCombiningChar(ch)
         || isExtender(ch);
   }

   /**
    * Returns true if the specified character is a valid name start character as defined by
    * production [5] in the XML 1.0 specification.<b> (Letter | '_' | ':')</b>
    * 
    * @param ch
    *          The character to check.
    */
   public static boolean isNameStart(int ch)
   {
      return isLetter(ch) || '_' == ch || ':' == ch;
   }

   /**
    * Check to see if a string is a valid Name according to [5] in the XML 1.0 Recommendation <b>Name
    * ::= (Letter | '_' | ':') (NameChar)*</b>
    * 
    * @param name
    *          string to check
    * @return true if name is a valid Name
    */
   public static boolean isValidName(String name)
   {
      if (name.length() == 0)
         return false;
      char ch = name.charAt(0);
      if (isNameStart(ch) == false)
         return false;
      for (int i = 1; i < name.length(); i++)
      {
         ch = name.charAt(i);
         if (isName(ch) == false)
         {
            return false;
         }
      }
      return true;
   }

   /**
    * Returns true if the specified character is a valid NCName start character as defined by
    * production [6] in Namespaces in XML recommendation. <b>NCNameStartChar ::= Letter | '_'</b>
    * 
    * @param ch
    *          The character to check.
    */
   public static boolean isNCNameStart(int ch)
   {
      return isLetter(ch) || '_' == ch;
   }

   /**
    * Returns true if the specified character is a valid NCName character as defined by production
    * [5] in Namespaces in XML recommendation. <b>NCNameChar ::= NameChar - ':'</b>
    * 
    * @param c
    *          The character to check.
    */
   public static boolean isNCName(int ch)
   {
      return isName(ch) && ':' != ch;
   }

   /**
    * Check to see if a string is a valid NCName according to [4] from the XML Namespaces 1.0
    * Recommendation <b>NCName ::= NCNameStartChar NCNameChar*</b>
    * 
    * @param ncName
    *          string to check
    * @return true if name is a valid NCName
    */
   public static boolean isValidNCName(String ncName)
   {
      if (ncName.length() == 0)
         return false;
      char ch = ncName.charAt(0);
      if (isNCNameStart(ch) == false)
         return false;
      for (int i = 1; i < ncName.length(); i++)
      {
         ch = ncName.charAt(i);
         if (isNCName(ch) == false)
         {
            return false;
         }
      }
      return true;
   }

   /**
    * Returns true if the specified character is a valid Pubid character as defined by production
    * [13] in the XML 1.0 specification. <b>PubidChar ::= #x20 | #xD | #xA | [a-zA-Z0-9] |
    * [-'()+,./:=?;!*#@$_%]</b>
    * 
    * @param c
    *          The character to check.
    */
   public static boolean isPubid(int ch)
   {
      return 0x09 == ch
         || 0x0a == ch // LF
         || 0x0d == ch // CR
         || ('a' <= ch && ch <= 'z') || ('A' <= ch && ch <= 'Z') || ('0' <= ch && ch <= '9') || '-' == ch || '\'' == ch
         || '(' == ch || ')' == ch || '+' == ch || ',' == ch || '.' == ch || '/' == ch || ':' == ch || '=' == ch
         || '?' == ch || ';' == ch || '!' == ch || '*' == ch || '#' == ch || '$' == ch || '@' == ch || '_' == ch
         || '%' == ch;
   }

   /**
    * Check to see if a string is a valid Nmtoken according to [7] in the XML 1.0 Recommendation
    * <b>Nmtoken ::= (NameChar)+</b>
    * 
    * @param nmtoken
    *          string to check
    * @return true if nmtoken is a valid Nmtoken
    */
   public static boolean isValidNmtoken(String nmtoken)
   {
      if (nmtoken.length() == 0)
         return false;
      for (int i = 0; i < nmtoken.length(); i++)
      {
         char ch = nmtoken.charAt(i);
         if (!isName(ch))
         {
            return false;
         }
      }
      return true;
   }

   /**
    * Returns true if the encoding name is a valid IANA encoding. This method does not verify that
    * there is a decoder available for this encoding, only that the characters are valid for an IANA
    * encoding name.
    * 
    * @param ianaEncoding
    *          The IANA encoding name.
    */
   public static boolean isValidIANAEncoding(String ianaEncoding)
   {
      if (ianaEncoding != null)
      {
         int length = ianaEncoding.length();
         if (length > 0)
         {
            char c = ianaEncoding.charAt(0);
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z'))
            {
               for (int i = 1; i < length; i++)
               {
                  c = ianaEncoding.charAt(i);
                  if ((c < 'A' || c > 'Z') && (c < 'a' || c > 'z') && (c < '0' || c > '9') && c != '.' && c != '_'
                     && c != '-')
                  {
                     return false;
                  }
               }
               return true;
            }
         }
      }
      return false;
   }

   /**
    * Returns true if the encoding name is a valid Java encoding. This method does not verify that
    * there is a decoder available for this encoding, only that the characters are valid for an Java
    * encoding name.
    * 
    * @param javaEncoding
    *          The Java encoding name.
    */
   public static boolean isValidJavaEncoding(String javaEncoding)
   {
      if (javaEncoding != null)
      {
         int length = javaEncoding.length();
         if (length > 0)
         {
            for (int i = 1; i < length; i++)
            {
               char c = javaEncoding.charAt(i);
               if ((c < 'A' || c > 'Z') && (c < 'a' || c > 'z') && (c < '0' || c > '9') && c != '.' && c != '_'
                  && c != '-')
               {
                  return false;
               }
            }
            return true;
         }
      }
      return false;
   }
}
