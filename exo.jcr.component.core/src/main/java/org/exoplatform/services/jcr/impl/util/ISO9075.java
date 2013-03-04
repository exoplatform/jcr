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
package org.exoplatform.services.jcr.impl.util;

import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.impl.xml.XMLChar;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implements the encode and decode routines as specified for XML name to SQL identifier conversion
 * in ISO 9075-14:2003.<br/> If a character <code>c</code> is not valid at a certain position in an
 * XML 1.0 NCName it is encoded in the form: '_x' + hexValueOf(c) + '_' <p/> Note that only the
 * local part of a {@link org.apache.jackrabbit.name.QName} is encoded / decoded. A URI namespace
 * will always be valid and does not need encoding.
 */
public class ISO9075
{

   /** Hidden constructor. */
   private ISO9075()
   {
   }

   /** Pattern on an encoded character */
   private static final Pattern ENCODE_PATTERN = Pattern.compile("_x\\p{XDigit}{4}_");

   /** Padding characters */
   private static final char[] PADDING = new char[]{'0', '0', '0'};

   /** All the possible hex digits */
   private static final String HEX_DIGITS = "0123456789abcdefABCDEF";

   /**
    * Encodes the local part of <code>name</code> as specified in ISO 9075.
    * 
    * @param name
    *          the <code>QName</code> to encode.
    * @return the encoded <code>QName</code> or <code>name</code> if it does not need encoding.
    */
   public static InternalQName encode(InternalQName name)
   {
      String encoded = encode(name.getName());
      if (encoded.equals(name.getName()))
      {
         return name;
      }
      else
      {
         return new InternalQName(name.getNamespace(), encoded);
      }
   }

   /**
    * Encodes <code>name</code> as specified in ISO 9075.
    * 
    * @param name
    *          the <code>String</code> to encode.
    * @return the encoded <code>String</code> or <code>name</code> if it does not need encoding.
    */
   public static String encode(String name)
   {
      // quick check for root node name
      if (name.length() == 0)
      {
         return name;
      }
      if (XMLChar.isValidName(name) && name.indexOf("_x") < 0)
      {
         // already valid
         return name;
      }
      else
      {
         // encode
         StringBuffer encoded = new StringBuffer();
         for (int i = 0; i < name.length(); i++)
         {
            if (i == 0)
            {
               // first character of name
               if (XMLChar.isNameStart(name.charAt(i)))
               {
                  if (needsEscaping(name, i))
                  {
                     // '_x' must be encoded
                     encode('_', encoded);
                  }
                  else
                  {
                     encoded.append(name.charAt(i));
                  }
               }
               else
               {
                  // not valid as first character -> encode
                  encode(name.charAt(i), encoded);
               }
            }
            else if (!XMLChar.isName(name.charAt(i)))
            {
               encode(name.charAt(i), encoded);
            }
            else
            {
               if (needsEscaping(name, i))
               {
                  // '_x' must be encoded
                  encode('_', encoded);
               }
               else
               {
                  encoded.append(name.charAt(i));
               }
            }
         }
         return encoded.toString();
      }
   }

   /**
    * Decodes the <code>name</code>.
    * 
    * @param name
    *          the <code>QName</code> to decode.
    * @return the decoded <code>QName</code>.
    */
   public static InternalQName decode(InternalQName name)
   {
      String decoded = decode(name.getName());
      if (decoded.equals(name.getName()))
      {
         return name;
      }
      else
      {
         return new InternalQName(name.getNamespace(), decoded.toString());
      }
   }

   /**
    * Decodes the <code>name</code>.
    * 
    * @param name
    *          the <code>String</code> to decode.
    * @return the decoded <code>String</code>.
    */
   public static String decode(String name)
   {
      // quick check
      if (name.indexOf("_x") < 0)
      {
         // not encoded
         return name;
      }
      StringBuffer decoded = new StringBuffer();
      Matcher m = ENCODE_PATTERN.matcher(name);
      while (m.find())
      {
         m.appendReplacement(decoded, Character.toString((char)Integer.parseInt(m.group().substring(2, 6), 16)));
      }
      m.appendTail(decoded);
      return decoded.toString();
   }

   // -------------------------< internal >-------------------------------------

   /**
    * Encodes the character <code>c</code> as a String in the following form:
    * <code>"_x" + hex value of c + "_"</code>. Where the hex value has four digits if the character
    * with possibly leading zeros. <p/> Example: ' ' (the space character) is encoded to: _x0020_
    * 
    * @param c
    *          the character to encode
    * @param b
    *          the encoded character is appended to <code>StringBuffer</code> <code>b</code>.
    */
   private static void encode(char c, StringBuffer b)
   {
      b.append("_x");
      String hex = Integer.toHexString(c);
      b.append(PADDING, 0, 4 - hex.length());
      b.append(hex);
      b.append("_");
   }

   /**
    * Returns true if <code>name.charAt(location)</code> is the underscore character and the
    * following character sequence is 'xHHHH_' where H is a hex digit.
    * 
    * @param name
    *          the name to check.
    * @param location
    *          the location to look at.
    * @throws ArrayIndexOutOfBoundsException
    *           if location > name.length()
    */
   private static boolean needsEscaping(String name, int location) throws ArrayIndexOutOfBoundsException
   {
      if (name.charAt(location) == '_' && name.length() >= location + 6)
      {
         return name.charAt(location + 1) == 'x' && HEX_DIGITS.indexOf(name.charAt(location + 2)) != -1
            && HEX_DIGITS.indexOf(name.charAt(location + 3)) != -1
            && HEX_DIGITS.indexOf(name.charAt(location + 4)) != -1
            && HEX_DIGITS.indexOf(name.charAt(location + 5)) != -1;
      }
      else
      {
         return false;
      }
   }
}
