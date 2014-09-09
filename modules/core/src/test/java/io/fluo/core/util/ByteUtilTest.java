/*
 * Copyright 2014 Fluo authors (see AUTHORS)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fluo.core.util;

import io.fluo.api.data.Bytes;
import org.apache.accumulo.core.data.ArrayByteSequence;
import org.apache.accumulo.core.data.ByteSequence;
import org.apache.hadoop.io.Text;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test for ByteUtil class
 */
public class ByteUtilTest {

  @Test
  public void testHadoopText() {
    String s1 = "test1";
    Text t1 = new Text(s1);
    Bytes b1 = ByteUtil.toBytes(t1);
    Assert.assertEquals(Bytes.wrap(s1), b1);
    Assert.assertEquals(t1, ByteUtil.toText(b1));
  }

  @Test
  public void testByteSequence() {
    String s2 = "test2";
    ByteSequence bs2 = new ArrayByteSequence(s2);
    Bytes b2 = ByteUtil.toBytes(bs2);
    Assert.assertEquals(Bytes.wrap(s2), b2);
    Assert.assertEquals(bs2, ByteUtil.toByteSequence(b2));
  }
}
