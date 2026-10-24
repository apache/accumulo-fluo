/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.apache.fluo.api.data;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * Unit test for {@link Bytes}
 */
public class BytesTest {

  @Test
  public void testCompareSubsequences() {
    Bytes a = Bytes.of("xxabc").subSequence(2, 5);
    Bytes b = Bytes.of("yyabd").subSequence(2, 5);
    Assert.assertTrue(a.compareTo(b) < 0);
    Assert.assertTrue(b.compareTo(a) > 0);
    Assert.assertFalse(a.equals(b));
    Assert.assertFalse(a.contentEquals(b.toArray()));
    Assert.assertTrue(a.compareTo(Bytes.of("abd")) < 0);
    Assert.assertTrue(a.compareTo(Bytes.of("yyabd").toArray(), 2, 3) < 0);
    Assert.assertEquals(Bytes.of("abc"), a);
  }

  @Test
  public void testBytesWrap() {

    String s1 = "test1";
    Bytes b1 = Bytes.of(s1);
    Assert.assertArrayEquals(s1.getBytes(), b1.toArray());
    Assert.assertEquals(s1, b1.toString());

    String s2 = "test2";
    ByteBuffer bb = ByteBuffer.wrap(s2.getBytes());
    Bytes b2 = Bytes.of(bb);
    Assert.assertArrayEquals(s2.getBytes(), b2.toArray());
    Assert.assertEquals(s2, b2.toString());

    // call again to ensure that position was not changed by previous call
    b2 = Bytes.of(bb);
    Assert.assertArrayEquals(s2.getBytes(), b2.toArray());
    Assert.assertEquals(s2, b2.toString());

    String s3 = "test3";
    Bytes b3 = Bytes.of(s3.getBytes());
    Assert.assertArrayEquals(s3.getBytes(), b3.toArray());
    Assert.assertEquals(s3, b3.toString());

    String s4 = "test4";
    byte[] d4 = s4.getBytes();
    Bytes b4 = Bytes.of(d4, 0, d4.length);
    Assert.assertArrayEquals(s4.getBytes(), b4.toArray());
    Assert.assertEquals(s4, b4.toString());
  }

  @Test
  public void testImmutable() {
    byte[] d1 = Bytes.of("mydata").toArray();

    Bytes imm = Bytes.of(d1);
    Assert.assertNotSame(d1, imm.toArray());
  }

  @Test
  public void testHashSubsequence() {
    Bytes b1 = Bytes.of("abcde");
    Bytes b2 = Bytes.of("cde");

    Assert.assertEquals(b2.hashCode(), b1.subSequence(2, 5).hashCode());
  }

  @Test
  public void testPrefixSuffix() {
    Bytes b1 = Bytes.of("abcde");
    Bytes prefix = Bytes.of("ab");
    Bytes suffix = Bytes.of("de");
    Bytes empty = new Bytes();
    Bytes mid = Bytes.of("cd");

    Assert.assertTrue(b1.startsWith(prefix));
    Assert.assertTrue(b1.endsWith(suffix));
    Assert.assertFalse(b1.startsWith(mid));
    Assert.assertFalse(b1.endsWith(mid));
    Assert.assertTrue(empty.startsWith(empty));
    Assert.assertTrue(empty.endsWith(empty));
    Assert.assertTrue(b1.startsWith(b1));
    Assert.assertTrue(b1.endsWith(b1));
    Assert.assertTrue(b1.startsWith(empty));
    Assert.assertTrue(b1.endsWith(empty));
    Assert.assertFalse(empty.startsWith(b1));
    Assert.assertFalse(empty.endsWith(b1));
    Assert.assertFalse(prefix.startsWith(b1));
    Assert.assertFalse(prefix.endsWith(b1));
    Assert.assertTrue(b1.startsWith(b1.subSequence(0, 2)));
    Assert.assertFalse(b1.subSequence(0, 2).startsWith(b1));
    Assert.assertTrue(b1.endsWith(b1.subSequence(3, 5)));
    Assert.assertFalse(b1.endsWith(b1.subSequence(0, 2)));
    Assert.assertFalse(b1.subSequence(0, 2).endsWith(b1));
  }

  @Test
  public void testContentEquals() {
    Bytes b1 = Bytes.of("a");
    byte[] b2 = b1.toArray();
    byte[] b3 = Bytes.of("b").toArray();
    Bytes b5 = Bytes.of("qwerty");
    byte[] b6 = b5.toArray();
    Bytes b7 = Bytes.of("");
    byte[] b8 = b7.toArray();


    Assert.assertTrue(b1.contentEquals(b2));
    Assert.assertTrue(b5.contentEquals(b6));
    Assert.assertTrue(b7.contentEquals(b8));

    Assert.assertFalse(b1.contentEquals(b3));
    Assert.assertFalse(b1.contentEquals(b6));
    Assert.assertFalse(b5.contentEquals(b2));
    Assert.assertFalse(b1.contentEquals(b8));
    Assert.assertFalse(b5.contentEquals(b8));
    Assert.assertFalse(b7.contentEquals(b2));
    Assert.assertFalse(b7.contentEquals(b6));
  }

  @Test
  public void testCompare() {
    Bytes b1 = Bytes.of("a");
    Bytes b2 = Bytes.of("b");
    Bytes b3 = Bytes.of("a");
    Assert.assertEquals(-1, b1.compareTo(b2));
    Assert.assertEquals(1, b2.compareTo(b1));
    Assert.assertEquals(0, b1.compareTo(b3));
    Assert.assertEquals(0, b1.compareTo(b1));
    Assert.assertEquals(1, b1.compareTo(Bytes.EMPTY));

    byte[] b1Arr = b1.toArray();
    byte[] b2Arr = b2.toArray();
    byte[] b3Arr = b3.toArray();
    Assert.assertEquals(-1, b1.compareTo(b2Arr));
    Assert.assertEquals(1, b2.compareTo(b1Arr));
    Assert.assertEquals(0, b1.compareTo(b3Arr));
    Assert.assertEquals(0, b1.compareTo(b1Arr));
    Assert.assertEquals(1, b1.compareTo(Bytes.EMPTY));

    Bytes b4 = Bytes.of("abc");
    byte[] b4Arr = b4.toArray();
    Bytes b5 = Bytes.of("baz");
    byte[] b5Arr = b5.toArray();
    Bytes b6 = Bytes.of("ab");
    byte[] b7Arr = Bytes.of("dabc").toArray();

    Assert.assertEquals(0, b4.compareTo(b4Arr, 0, 3));
    Assert.assertTrue(b4.compareTo(b4Arr, 1, 2) < 0);
    Assert.assertTrue(b5.compareTo(b4Arr, 1, 2) < 0);
    Assert.assertTrue(b5.compareTo(b5Arr, 1, 2) > 0);
    Assert.assertTrue(b4.compareTo(b4Arr, 0, 2) > 0);
    Assert.assertTrue(b6.compareTo(b7Arr, 1, 3) < 0);
    Assert.assertTrue(b6.compareTo(b1Arr) > 0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCompareNegOffset() {
    Bytes b1 = Bytes.of("abc");
    byte[] b2 = b1.toArray();

    b1.compareTo(b2, -4, 1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCompareNegLen() {
    Bytes b1 = Bytes.of("abc");
    byte[] b2 = b1.toArray();

    b1.compareTo(b2, 0, -1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCompareBadArgs() {
    Bytes b1 = Bytes.of("abc");
    byte[] b2 = b1.toArray();

    b1.compareTo(b2, 2, 2);
  }

  @Test
  public void testCompareSubsequence() {
    Bytes b1 = Bytes.of("abcd");
    Bytes b2 = b1.subSequence(0, 3);
    Bytes b3 = Bytes.of("abc");
    Bytes b4 = Bytes.of("~abcde");
    Bytes b5 = b4.subSequence(1, 4);
    Bytes b6 = b4.subSequence(1, 5);

    for (Bytes ba : Arrays.asList(b2, b3, b5, b1.subSequence(0, 3))) {
      for (Bytes bb : Arrays.asList(b2, b3, b5)) {
        Assert.assertEquals(0, ba.compareTo(bb));
      }
    }

    Assert.assertEquals(1, b1.compareTo(b2));
    Assert.assertEquals(-1, b2.compareTo(b1));

    for (Bytes less : Arrays.asList(b2, b3, b5)) {
      for (Bytes greater : Arrays.asList(b1, b4, b6)) {
        Assert.assertTrue(less.compareTo(greater) < 0);
        Assert.assertTrue(greater.compareTo(less) > 0);
      }
    }
  }

  @Test
  public void testToByteBuffer() {
    Bytes b1 = Bytes.of("fluofluo");
    ByteBuffer buffer = b1.toByteBuffer();
    Assert.assertFalse(buffer.hasArray());
    Assert.assertEquals(buffer.remaining(), 8);

    byte[] copy = new byte[8];
    buffer.duplicate().get(copy);
    Assert.assertEquals("fluofluo", new String(copy, StandardCharsets.UTF_8));

    try {
      buffer.put((byte) 6);
      Assert.fail();
    } catch (ReadOnlyBufferException e) {
    }
  }

  @Test
  public void testWrite() throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    byte[] ba1 = "abc".getBytes(StandardCharsets.UTF_8);
    Bytes little = Bytes.of(ba1);
    byte[] ba2 = new byte[1024];
    Arrays.fill(ba2, (byte) 42);
    Bytes big = Bytes.of(ba2);

    little.writeTo(out);
    big.writeTo(out);

    byte[] expected = new byte[ba1.length + ba2.length];
    System.arraycopy(ba1, 0, expected, 0, ba1.length);
    System.arraycopy(ba2, 0, expected, ba1.length, ba2.length);

    Assert.assertArrayEquals(expected, out.toByteArray());
    out.close();
  }

  @Test
  public void testBounds() {
    Bytes bytes = Bytes.of("abcdefg").subSequence(2, 5);
    Assert.assertEquals("abcdefg".substring(2, 5), bytes.toString());
    Assert.assertEquals('c', bytes.byteAt(0));
    Assert.assertEquals('d', bytes.byteAt(1));
    Assert.assertEquals('e', bytes.byteAt(2));

    Assert.assertEquals("de", bytes.subSequence(1, 3).toString());

    try {
      bytes.byteAt(-1);
      Assert.fail();
    } catch (IndexOutOfBoundsException e) {
    }

    try {
      bytes.byteAt(3);
      Assert.fail();
    } catch (IndexOutOfBoundsException e) {
    }

    try {
      bytes.subSequence(-1, 2);
      Assert.fail();
    } catch (IndexOutOfBoundsException e) {
    }

    try {
      bytes.subSequence(0, 5);
      Assert.fail();
    } catch (IndexOutOfBoundsException e) {
    }

    try {
      bytes.subSequence(2, 1);
      Assert.fail();
    } catch (IndexOutOfBoundsException e) {
    }
  }

  @Test
  public void testCharSequence() {
    AsciiSequence cs1 = new AsciiSequence("abc123");

    Bytes b1 = Bytes.of(cs1);

    Assert.assertEquals("abc123", b1.toString());

    Bytes b3 = Bytes.of((CharSequence) "abc123");

    Assert.assertEquals("abc123", b3.toString());

    AsciiSequence cs2 = new AsciiSequence("");
    Assert.assertSame(Bytes.EMPTY, Bytes.of(cs2));
  }

  @Test
  public void testSameString() {
    String s1 = "abc";
    String s2 = "xyZ";

    Bytes b1 = Bytes.of(s1);
    Bytes b2 = Bytes.of(s2);

    Assert.assertSame(s1, b1.toString());
    Assert.assertSame(s2, b2.toString());
  }

  @Test
  public void testCopyTo() {
    Bytes field1 = Bytes.of("foo");
    Bytes field2 = Bytes.of("bar");

    byte[] dest = new byte[field1.length() + field2.length() + 1];

    field1.copyTo(dest, 0);
    dest[field1.length()] = ':';
    field2.copyTo(dest, field1.length() + 1);

    Assert.assertEquals("foo:bar", new String(dest));
  }

  @Test
  public void testCopyToOutOfBounds() {
    Bytes field = Bytes.of("abcdefg");
    byte[] dest = new byte[field.length() - 1];
    String initialDest = new String(dest);

    try {
      field.copyTo(dest, 0);
      fail("Should not get here");
    } catch (ArrayIndexOutOfBoundsException e) {
      // dest should not have changed
      Assert.assertEquals(new String(dest), initialDest);
    }
  }

  @Test
  public void testCopyToSubset() {
    Bytes field = Bytes.of("abcdefg");
    byte[] dest = new byte[4];

    field.copyTo(3, 6, dest, 0);
    String expected = "def\0";
    String actual = new String(dest);

    Assert.assertEquals(expected, actual);

    field.subSequence(3, 6).copyTo(dest, 1);
    // because offset was 1, it will replace ef\0 with def and leave d at position 0
    Assert.assertEquals("ddef", new String(dest));
  }

  @Test
  public void testCopyToArgsReversed() {
    Bytes field = Bytes.of("abcdefg");
    byte[] dest = new byte[4];

    try {
      field.copyTo(6, 3, dest, 0);
      fail("should not get here");
    } catch (java.lang.ArrayIndexOutOfBoundsException e) {
      Assert.assertEquals("\0\0\0\0", new String(dest));
    }
  }

  @Test
  public void testCopyToNothing() {
    Bytes field = Bytes.of("abcdefg");
    byte[] dest = new byte[4];

    field.copyTo(3, 3, dest, 0);
    // should not have changed
    Assert.assertEquals("\0\0\0\0", new String(dest));
  }

  @Test
  public void testCopyToWithUnicode() {
    // first observe System.arraycopy
    String begin = "abc"; // 3 chars, 3 bytes
    String mid1 = "†"; // 1 char, 3 bytes
    String mid2 = "𝔊"; // 2 chars, 4 bytes
    String end = "efghi"; // 5 chars, 5 bytes
    Assert.assertEquals(11, begin.length() + mid1.length() + mid2.length() + end.length());

    byte[] copyFrom = (begin + mid1 + mid2 + end).getBytes(StandardCharsets.UTF_8);
    //@formatter:off
    // [ a,  b,  c,              †,                    𝔊,   e,   f,   g,   h,   i]
    // [97, 98, 99, -30, -128, -96, -16, -99, -108, -118, 101, 102, 103, 104, 105]
    //@formatter:on
    Assert.assertEquals(15, copyFrom.length);

    byte[] copyTo = new byte[9];
    System.arraycopy(copyFrom, 2, copyTo, 0, 9);
    Assert.assertEquals("c†𝔊e", new String(copyTo));

    // now make a Bytes out of the craziness
    Bytes allBytes = Bytes.of(copyFrom);
    Assert.assertEquals(15, allBytes.length());

    // and test Bytes.arraycopy works the same
    byte[] copyTo2 = new byte[9];
    allBytes.copyTo(2, 11, copyTo2, 0);
    Assert.assertEquals("c†𝔊e", new String(copyTo2));
  }

}
