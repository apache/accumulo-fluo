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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedBytes;

/**
 * Represents bytes in Fluo. Bytes is an immutable wrapper around a byte array. Bytes always copies
 * on creation and never lets its internal byte array escape. Its modeled after Java's String which
 * is an immutable wrapper around a char array. It was created because there is nothing in Java like
 * it at the moment. Its very nice having this immutable type, it avoids having to do defensive
 * copies to ensure correctness. Maybe one day Java will have equivalents of String, StringBuilder,
 * and Charsequence for bytes.
 *
 * <p>
 * The reason Fluo did not use ByteBuffer is because its not immutable, even a read only ByteBuffer
 * has a mutable position. This makes ByteBuffer unsuitable for place where an immutable data type
 * is desirable, like a key for a map.
 *
 * <p>
 * Bytes.EMPTY is used to represent a Bytes object with no data.
 *
 * @since 1.0.0
 */
public final class Bytes implements Comparable<Bytes>, Serializable {

  private static final long serialVersionUID = 1L;

  private final byte[] data;
  private final int offset;
  private final int length;

  private transient WeakReference<String> utf8String;

  public static final Bytes EMPTY = new Bytes(new byte[0]);

  private int hashCode = 0;

  public Bytes() {
    data = EMPTY.data;
    offset = 0;
    length = 0;
  }

  private Bytes(byte[] data) {
    this.data = data;
    this.offset = 0;
    this.length = data.length;
  }

  private Bytes(byte[] data, String utf8String) {
    this.data = data;
    this.offset = 0;
    this.length = data.length;
    this.utf8String = new WeakReference<>(utf8String);
  }

  private Bytes(byte[] data, int offset, int length) {
    if (offset < 0 || offset > data.length || length < 0 || (offset + length) > data.length) {
      throw new IndexOutOfBoundsException(" Bad offset and/or length data.length = " + data.length
          + " offset = " + offset + " length = " + length);
    }
    this.data = data;
    this.offset = offset;
    this.length = length;
  }

  /**
   * Gets a byte within this sequence of bytes
   *
   * @param i index into sequence
   * @return byte
   * @throws IndexOutOfBoundsException if i is out of range
   */
  public byte byteAt(int i) {

    if (i < 0) {
      throw new IndexOutOfBoundsException("i < 0, " + i);
    }

    if (i >= length) {
      throw new IndexOutOfBoundsException("i >= length, " + i + " >= " + length);
    }

    return data[offset + i];
  }

  /**
   * Gets the length of bytes
   */
  public int length() {
    return length;
  }

  /**
   * Returns a portion of the Bytes object
   *
   * @param start index of subsequence start (inclusive)
   * @param end index of subsequence end (exclusive)
   */
  public Bytes subSequence(int start, int end) {
    if (start > end || start < 0 || end > length) {
      throw new IndexOutOfBoundsException("Bad start and/end start = " + start + " end=" + end
          + " offset=" + offset + " length=" + length);
    }
    return new Bytes(data, offset + start, end - start);
  }

  /**
   * Returns a byte array containing a copy of the bytes
   */
  public byte[] toArray() {
    byte[] copy = new byte[length];
    System.arraycopy(data, offset, copy, 0, length);
    return copy;
  }

  /**
   * Creates UTF-8 String using Bytes data
   */
  @Override
  public String toString() {
    if (utf8String != null) {
      String s = utf8String.get();
      if (s != null) {
        return s;
      }
    }

    String s = new String(data, offset, length, StandardCharsets.UTF_8);
    utf8String = new WeakReference<>(s);
    return s;
  }

  /**
   * @return A read only byte buffer that's backed by the internal byte array.
   */
  public ByteBuffer toByteBuffer() {
    return ByteBuffer.wrap(data, offset, length).asReadOnlyBuffer();
  }

  /**
   * @return An input stream that's backed by the internal byte array
   */
  public InputStream toInputStream() {
    return new ByteArrayInputStream(data, offset, length);
  }

  public void writeTo(OutputStream out) throws IOException {
    // since Bytes is immutable, its important that we do not let the internal byte array escape
    if (length <= 32) {
      int end = offset + length;
      for (int i = offset; i < end; i++) {
        out.write(data[i]);
      }
    } else {
      out.write(toArray());
    }
  }

  /**
   * Compares this to the passed bytes, byte by byte, returning a negative, zero, or positive result
   * if the first sequence is less than, equal to, or greater than the second. The comparison is
   * performed starting with the first byte of each sequence, and proceeds until a pair of bytes
   * differs, or one sequence runs out of byte (is shorter). A shorter sequence is considered less
   * than a longer one.
   *
   * @return comparison result
   */
  @Override
  public final int compareTo(Bytes other) {
    if (this == other) {
      return 0;
    } else {
      return compareToUnchecked(other.data, other.offset, other.length);
    }
  }

  /**
   * Compares this to the passed bytes, byte by byte, returning a negative, zero, or positive result
   * if the first sequence is less than, equal to, or greater than the second. The comparison is
   * performed starting with the first byte of each sequence, and proceeds until a pair of bytes
   * differs, or one sequence runs out of byte (is shorter). A shorter sequence is considered less
   * than a longer one.
   *
   * @since 1.2.0
   * @return comparison result
   */
  public int compareTo(byte[] bytes) {
    return compareToUnchecked(bytes, 0, bytes.length);
  }

  /**
   * Compares this to the passed bytes, byte by byte, returning a negative, zero, or positive result
   * if the first sequence is less than, equal to, or greater than the second. The comparison is
   * performed starting with the first byte of each sequence, and proceeds until a pair of bytes
   * differs, or one sequence runs out of byte (is shorter). A shorter sequence is considered less
   * than a longer one.
   *
   * This method checks the arguments passed to it.
   *
   * @since 1.2.0
   * @return comparison result
   */
  public int compareTo(byte[] bytes, int offset, int len) {
    Preconditions.checkArgument(offset >= 0 && len >= 0 && offset + len <= bytes.length);
    return compareToUnchecked(bytes, offset, len);
  }

  /**
   * Compares this to the passed bytes, byte by byte, returning a negative, zero, or positive result
   * if the first sequence is less than, equal to, or greater than the second. The comparison is
   * performed starting with the first byte of each sequence, and proceeds until a pair of bytes
   * differs, or one sequence runs out of byte (is shorter). A shorter sequence is considered less
   * than a longer one.
   *
   * This method does not check the arguments passed to it.
   *
   * @since 1.2.0
   * @return comparison result
   */
  private int compareToUnchecked(byte[] bytes, int offset, int len) {
    if (this.length == this.data.length && len == bytes.length) {
      return UnsignedBytes.lexicographicalComparator().compare(this.data, bytes);
    } else {
      int minLen = Math.min(this.length, len);
      for (int i = this.offset, j = offset; i < this.offset + minLen; i++, j++) {
        int a = (this.data[i] & 0xff);
        int b = (bytes[j] & 0xff);
        if (a != b) {
          return a - b;
        }
      }
      return this.length - len;
    }
  }

  /**
   * Returns true if this Bytes object equals another.
   */
  @Override
  public final boolean equals(Object other) {

    if (this == other) {
      return true;
    }

    if (other instanceof Bytes) {
      Bytes ob = (Bytes) other;

      return contentEqualsUnchecked(ob.data, ob.offset, ob.length);
    }
    return false;
  }

  /**
   * Returns true if this Bytes object equals another.
   *
   * @since 1.2.0
   */
  public boolean contentEquals(byte[] bytes) {
    return contentEqualsUnchecked(bytes, 0, bytes.length);
  }

  /**
   * Returns true if this Bytes object equals another. This method checks it's arguments.
   *
   * @since 1.2.0
   */
  public boolean contentEquals(byte[] bytes, int offset, int len) {
    Preconditions.checkArgument(len >= 0 && offset >= 0 && offset + len <= bytes.length);
    return contentEqualsUnchecked(bytes, offset, len);
  }

  /**
   * Returns true if this Bytes object equals another. This method doesn't check it's arguments.
   *
   * @since 1.2.0
   */
  private boolean contentEqualsUnchecked(byte[] bytes, int offset, int len) {
    if (length != len) {
      return false;
    }

    return compareToUnchecked(bytes, offset, len) == 0;
  }

  @Override
  public final int hashCode() {
    if (hashCode == 0) {
      int hash = 1;
      int end = offset + length;
      for (int i = offset; i < end; i++) {
        hash = (31 * hash) + data[i];
      }
      hashCode = hash;
    }
    return hashCode;
  }

  /**
   * Creates a Bytes object by copying the data of the given byte array
   */
  public static final Bytes of(byte[] array) {
    Objects.requireNonNull(array);
    if (array.length == 0) {
      return EMPTY;
    }
    byte[] copy = new byte[array.length];
    System.arraycopy(array, 0, copy, 0, array.length);
    return new Bytes(copy);
  }

  /**
   * Creates a Bytes object by copying the data of a subsequence of the given byte array
   *
   * @param data Byte data
   * @param offset Starting offset in byte array (inclusive)
   * @param length Number of bytes to include
   */
  public static final Bytes of(byte[] data, int offset, int length) {
    Objects.requireNonNull(data);
    if (length == 0) {
      return EMPTY;
    }
    byte[] copy = new byte[length];
    System.arraycopy(data, offset, copy, 0, length);
    return new Bytes(copy);
  }

  /**
   * Creates a Bytes object by copying the data of the given ByteBuffer.
   *
   * @param bb Data will be read from this ByteBuffer in such a way that its position is not
   *        changed.
   */
  public static final Bytes of(ByteBuffer bb) {
    Objects.requireNonNull(bb);
    if (bb.remaining() == 0) {
      return EMPTY;
    }
    byte[] data;
    if (bb.hasArray()) {
      data = Arrays.copyOfRange(bb.array(), bb.position() + bb.arrayOffset(),
          bb.limit() + bb.arrayOffset());
    } else {
      data = new byte[bb.remaining()];
      // duplicate so that it does not change position
      bb.duplicate().get(data);
    }
    return new Bytes(data);
  }

  /**
   * Creates a Bytes object by copying the data of the CharSequence and encoding it using UTF-8.
   */
  public static final Bytes of(CharSequence cs) {
    if (cs instanceof String) {
      return of((String) cs);
    }

    Objects.requireNonNull(cs);
    if (cs.length() == 0) {
      return EMPTY;
    }

    ByteBuffer bb = StandardCharsets.UTF_8.encode(CharBuffer.wrap(cs));

    if (bb.hasArray()) {
      // this byte buffer has never escaped so can use its byte array directly
      return new Bytes(bb.array(), bb.position() + bb.arrayOffset(), bb.limit());
    } else {
      byte[] data = new byte[bb.remaining()];
      bb.get(data);
      return new Bytes(data);
    }
  }

  /**
   * Creates a Bytes object by copying the value of the given String
   */
  public static final Bytes of(String s) {
    Objects.requireNonNull(s);
    if (s.isEmpty()) {
      return EMPTY;
    }
    byte[] data = s.getBytes(StandardCharsets.UTF_8);
    return new Bytes(data, s);
  }

  /**
   * Creates a Bytes object by copying the value of the given String with a given charset
   */
  public static final Bytes of(String s, Charset c) {
    Objects.requireNonNull(s);
    Objects.requireNonNull(c);
    if (s.isEmpty()) {
      return EMPTY;
    }
    byte[] data = s.getBytes(c);
    return new Bytes(data);
  }

  /**
   * Checks if this has the passed prefix
   *
   * @param prefix is a Bytes object to compare to this
   * @return true or false
   * @since 1.1.0
   */
  public boolean startsWith(Bytes prefix) {
    Objects.requireNonNull(prefix, "startWith(Bytes prefix) cannot have null parameter");

    if (prefix.length > this.length) {
      return false;
    } else {
      int end = this.offset + prefix.length;
      for (int i = this.offset, j = prefix.offset; i < end; i++, j++) {
        if (this.data[i] != prefix.data[j]) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Checks if this has the passed suffix
   *
   * @param suffix is a Bytes object to compare to this
   * @return true or false
   * @since 1.1.0
   */
  public boolean endsWith(Bytes suffix) {
    Objects.requireNonNull(suffix, "endsWith(Bytes suffix) cannot have null parameter");
    int startOffset = this.length - suffix.length;

    if (startOffset < 0) {
      return false;
    } else {
      int end = startOffset + this.offset + suffix.length;
      for (int i = startOffset + this.offset, j = suffix.offset; i < end; i++, j++) {
        if (this.data[i] != suffix.data[j]) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * This class provides an easy, efficient, reusable mechanism for building immutable Bytes
   * objects.
   *
   * @since 1.0.0
   */
  public static class BytesBuilder {

    private byte[] ba;
    private int len;

    BytesBuilder(int initialCapacity) {
      ba = new byte[initialCapacity];
      len = 0;
    }

    BytesBuilder() {
      this(32);
    }

    private void ensureCapacity(int min) {
      if (ba.length < min) {
        int newLen = ba.length * 2;
        if (newLen < min) {
          newLen = min;
        }

        ba = Arrays.copyOf(ba, newLen);
      }
    }

    public BytesBuilder append(CharSequence cs) {
      if (cs instanceof String) {
        return append((String) cs);
      }

      ByteBuffer bb = StandardCharsets.UTF_8.encode(CharBuffer.wrap(cs));

      int length = bb.remaining();
      ensureCapacity(len + length);
      bb.get(ba, len, length);
      len += length;
      return this;
    }

    /**
     * Converts string to bytes using UTF-8 encoding and appends bytes.
     *
     * @return self
     */
    public BytesBuilder append(String s) {
      return append(s.getBytes(StandardCharsets.UTF_8));
    }

    public BytesBuilder append(Bytes b) {
      ensureCapacity(len + b.length);
      System.arraycopy(b.data, b.offset, ba, len, b.length);
      len += b.length();
      return this;
    }

    public BytesBuilder append(byte[] bytes) {
      ensureCapacity(len + bytes.length);
      System.arraycopy(bytes, 0, ba, len, bytes.length);
      len += bytes.length;
      return this;
    }

    /**
     * Append a single byte.
     *
     * @param b take the lower 8 bits and appends it.
     * @return self
     */
    public BytesBuilder append(int b) {
      ensureCapacity(len + 1);
      ba[len] = (byte) b;
      len += 1;
      return this;
    }

    /**
     * Append a section of bytes from array
     *
     * @param bytes - bytes to be appended
     * @param offset - start of bytes to be appended
     * @param length - how many bytes from 'offset' to be appended
     * @return self
     */
    public BytesBuilder append(byte[] bytes, int offset, int length) {
      ensureCapacity(len + length);
      System.arraycopy(bytes, offset, ba, len, length);
      len += length;
      return this;
    }

    /**
     * Append a sequence of bytes from an InputStream
     *
     * @param in data source to append from
     * @param length number of bytes to read from data source
     * @return self
     */
    public BytesBuilder append(InputStream in, int length) throws IOException {
      ensureCapacity(len + length);
      // create an anonymous subclass of DataInputStream to avoid unclosed resource warnings
      // we don't close it because we don't want to close the provided InputStream it is wrapping
      new DataInputStream(in) {}.readFully(ba, len, length);
      len += length;
      return this;
    }

    /**
     * Append data from a ByteBuffer
     *
     * @param bb data is read from the ByteBuffer in such a way that its position is not changed.
     * @return self
     */
    public BytesBuilder append(ByteBuffer bb) {
      int length = bb.remaining();
      ensureCapacity(len + length);
      bb.duplicate().get(ba, len, length);
      len += length;
      return this;
    }

    /**
     * Sets the point at which appending will start. This method can shrink or grow the ByteBuilder
     * from its current state. If it grows it will zero pad.
     */
    public void setLength(int newLen) {
      Preconditions.checkArgument(newLen >= 0, "Negative length passed : " + newLen);
      if (newLen > ba.length) {
        ba = Arrays.copyOf(ba, newLen);
      }

      if (newLen > len) {
        Arrays.fill(ba, len, newLen, (byte) 0);
      }

      len = newLen;
    }

    public int getLength() {
      return len;
    }

    public Bytes toBytes() {
      return Bytes.of(ba, 0, len);
    }
  }

  /**
   * Provides an efficient and reusable way to build immutable Bytes objects.
   */
  public static BytesBuilder builder() {
    return new BytesBuilder();
  }

  /**
   * @param initialCapacity The initial size of the byte builders internal array.
   */
  public static BytesBuilder builder(int initialCapacity) {
    return new BytesBuilder(initialCapacity);
  }

  /**
   * Copy entire Bytes object to specific byte array. Uses the specified offset in the dest byte
   * array to start the copy.
   *
   * @param dest destination array
   * @param destPos starting position in the destination data.
   * @exception IndexOutOfBoundsException if copying would cause access of data outside array
   *            bounds.
   * @exception NullPointerException if either <code>src</code> or <code>dest</code> is
   *            <code>null</code>.
   * @since 1.1.0
   */
  public void copyTo(byte[] dest, int destPos) {
    arraycopy(0, dest, destPos, this.length);
  }

  /**
   * Copy a subsequence of Bytes to specific byte array. Uses the specified offset in the dest byte
   * array to start the copy.
   *
   * @param start index of subsequence start (inclusive)
   * @param end index of subsequence end (exclusive)
   * @param dest destination array
   * @param destPos starting position in the destination data.
   * @exception IndexOutOfBoundsException if copying would cause access of data outside array
   *            bounds.
   * @exception NullPointerException if either <code>src</code> or <code>dest</code> is
   *            <code>null</code>.
   * @since 1.1.0
   */
  public void copyTo(int start, int end, byte[] dest, int destPos) {
    // this.subSequence(start, end).copyTo(dest, destPos) would allocate another Bytes object
    arraycopy(start, dest, destPos, end - start);
  }

  private void arraycopy(int start, byte[] dest, int destPos, int length) {
    // since dest is byte[], we can't get the ArrayStoreException
    System.arraycopy(this.data, start + this.offset, dest, destPos, length);
  }

}
