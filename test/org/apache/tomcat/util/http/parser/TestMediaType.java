/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.tomcat.util.http.parser;

import java.io.IOException;
import java.io.StringReader;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link HttpParser} focusing on media-type as defined in
 * section 3.7 of RFC 2616.
 */
public class TestMediaType {

    // Include whitespace to ensure Parser handles it correctly (it should be
    // skipped).
    private static final String TYPE = " foo ";
    private static final String SUBTYPE = " bar ";
    private static final String TYPES = TYPE + "/" + SUBTYPE;

    private static final Parameter PARAM_TOKEN =
            new Parameter("a", "b");
    private static final Parameter PARAM_ESCAPED =
            new Parameter("v", "\"w\\\"w\"");
    private static final Parameter PARAM_QUOTED =
            new Parameter("x", "\"y\"");
    private static final Parameter PARAM_EMPTY_QUOTED =
            new Parameter("z", "\"\"");
    private static final Parameter PARAM_COMPLEX_QUOTED =
            new Parameter("w", "\"foo'bar,a=b;x=y\"");

    private static final String CHARSET = "UTF-8";
    private static final String WS_CHARSET = " \tUTF-8";
    private static final String CHARSET_WS = "UTF-8 \t";
    // Since this is quoted, it should retain the space at the end
    private static final String CHARSET_QUOTED = "\"" + CHARSET_WS + "\"";
    private static final Parameter PARAM_CHARSET =
            new Parameter("charset", CHARSET);
    private static final Parameter PARAM_WS_CHARSET =
            new Parameter("charset", WS_CHARSET);
    private static final Parameter PARAM_CHARSET_WS =
            new Parameter("charset", CHARSET_WS);
    private static final Parameter PARAM_CHARSET_QUOTED =
            new Parameter("charset", CHARSET_QUOTED);


    private static final String[] LWS_VALUES = new String[] {
            "", " ", "\t", "\r", "\n", "\r\n", " \r", " \n", " \r\n",
            "\r ", "\n ", "\r\n ", " \r ", " \n ", " \r\n " };


    @Test
    public void testSimple() throws IOException {
        doTest();
    }


    @Test
    public void testSimpleWithToken() throws IOException {
        doTest(PARAM_TOKEN);
    }


    @Test
    public void testSimpleWithEscapedString() throws IOException {
        doTest(PARAM_ESCAPED);
    }


    @Test
    public void testSimpleWithQuotedString() throws IOException {
        doTest(PARAM_QUOTED);
    }


    @Test
    public void testSimpleWithEmptyQuotedString() throws IOException {
        doTest(PARAM_EMPTY_QUOTED);
    }


    @Test
    public void testSimpleWithComplexQuotedString() throws IOException {
        doTest(PARAM_COMPLEX_QUOTED);
    }


    @Test
    public void testSimpleWithCharset() throws IOException {
        doTest(PARAM_CHARSET);
    }


    @Test
    public void testSimpleWithCharsetWhitespaceBefore() throws IOException {
        doTest(PARAM_WS_CHARSET);
    }


    @Test
    public void testSimpleWithCharsetWhitespaceAfter() throws IOException {
        doTest(PARAM_CHARSET_WS);
    }


    @Test
    public void testSimpleWithCharsetQuoted() throws IOException {
        doTest(PARAM_CHARSET_QUOTED);
    }


    @Test
    public void testSimpleWithAll() throws IOException {
        doTest(PARAM_COMPLEX_QUOTED, PARAM_EMPTY_QUOTED, PARAM_QUOTED,
                PARAM_TOKEN, PARAM_CHARSET);
    }


    @Test
    public void testCharset() throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(TYPES);
        sb.append(PARAM_CHARSET);
        sb.append(PARAM_TOKEN);

        StringReader sr = new StringReader(sb.toString());
        MediaType m = MediaType.parseMediaType(sr);

        Assert.assertEquals("foo/bar;charset=UTF-8;a=b", m.toString());
        Assert.assertEquals(CHARSET, m.getCharset());
        Assert.assertEquals("foo/bar;a=b", m.toStringNoCharset());
    }


    @Test
    public void testCharsetQuoted() throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(TYPES);
        sb.append(PARAM_CHARSET_QUOTED);

        StringReader sr = new StringReader(sb.toString());
        MediaType m = MediaType.parseMediaType(sr);

        Assert.assertEquals(CHARSET_WS, m.getCharset());
        Assert.assertEquals(TYPES.replace(" ", ""),
                m.toStringNoCharset());
    }


    @Test
    public void testBug52811() throws IOException {
        String input = "multipart/related;boundary=1_4F50BD36_CDF8C28;" +
                "Start=\"<31671603.smil>\";" +
                "Type=\"application/smil;charset=UTF-8\"";

        StringReader sr = new StringReader(input);
        MediaType m = MediaType.parseMediaType(sr);

        // Check the types
        Assert.assertEquals("multipart", m.getType());
        Assert.assertEquals("related", m.getSubtype());

        // Check the parameters
        Assert.assertTrue(m.getParameterCount() == 3);

        Assert.assertEquals("1_4F50BD36_CDF8C28", m.getParameterValue("boundary"));
        Assert.assertEquals("\"<31671603.smil>\"", m.getParameterValue("Start"));
        Assert.assertEquals("\"application/smil;charset=UTF-8\"",
                m.getParameterValue("Type"));

        String expected = "multipart/related;boundary=1_4F50BD36_CDF8C28;" +
                "start=\"<31671603.smil>\";" +
                "type=\"application/smil;charset=UTF-8\"";
        Assert.assertEquals(expected, m.toString());
        Assert.assertEquals(expected, m.toStringNoCharset());
        Assert.assertNull(m.getCharset());
    }


    @Test
    public void testBug53353() throws IOException {
        String input = "text/html; UTF-8;charset=UTF-8";

        StringReader sr = new StringReader(input);
        MediaType m = MediaType.parseMediaType(sr);

        // Check the types
        Assert.assertEquals("text", m.getType());
        Assert.assertEquals("html", m.getSubtype());

        // Check the parameters
        Assert.assertTrue(m.getParameterCount() == 2);

        Assert.assertEquals("", m.getParameterValue("UTF-8"));
        Assert.assertEquals("UTF-8", m.getCharset());

        // Note: Invalid input is filtered out
        Assert.assertEquals("text/html;charset=UTF-8", m.toString());
        Assert.assertEquals("UTF-8", m.getCharset());
    }


    @Test
    public void testBug55454() throws IOException {
        String input = "text/html;;charset=UTF-8";

        StringReader sr = new StringReader(input);
        MediaType m = MediaType.parseMediaType(sr);

        Assert.assertEquals("text", m.getType());
        Assert.assertEquals("html", m.getSubtype());

        Assert.assertTrue(m.getParameterCount() == 1);

        Assert.assertEquals("UTF-8", m.getParameterValue("charset"));
        Assert.assertEquals("UTF-8", m.getCharset());

        Assert.assertEquals("text/html;charset=UTF-8", m.toString());
    }


    private void doTest(Parameter... parameters) throws IOException {
        for (String lws : LWS_VALUES) {
            doTest(lws, parameters);
        }
    }

    private void doTest(String lws, Parameter... parameters)
            throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(TYPES);
        for (Parameter p : parameters) {
            sb.append(p.toString(lws));
        }

        StringReader sr = new StringReader(sb.toString());
        MediaType m = MediaType.parseMediaType(sr);

        // Check all expected parameters are present
        Assert.assertTrue(m.getParameterCount() == parameters.length);

        // Check the types
        Assert.assertEquals(TYPE.trim(), m.getType());
        Assert.assertEquals(SUBTYPE.trim(), m.getSubtype());

        // Check the parameters
        for (Parameter parameter : parameters) {
            Assert.assertEquals(parameter.getValue().trim(),
                    m.getParameterValue(parameter.getName().trim()));
        }
    }


    private static class Parameter {
        private final String name;
        private final String value;

        public Parameter(String name,String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return toString("");
        }

        public String toString(String lws) {
            StringBuilder sb = new StringBuilder();
            sb.append(lws);
            sb.append(";");
            sb.append(lws);
            sb.append(name);
            sb.append(lws);
            sb.append("=");
            sb.append(lws);
            sb.append(value);
            sb.append(lws);
            return sb.toString();
        }
    }

    @Test
    public void testCase() throws Exception {
        StringReader sr = new StringReader("type/sub-type;a=1;B=2");
        MediaType m = MediaType.parseMediaType(sr);

        Assert.assertEquals("1", m.getParameterValue("A"));
        Assert.assertEquals("1", m.getParameterValue("a"));
        Assert.assertEquals("2", m.getParameterValue("B"));
        Assert.assertEquals("2", m.getParameterValue("b"));
    }

    @Test
    public void testEmptyParameter() throws Exception {
        // RFC 9110
        StringReader sr = new StringReader("type/sub-type;;a=1;;b=2;;");
        MediaType m = MediaType.parseMediaType(sr);

        Assert.assertEquals("1", m.getParameterValue("a"));
        Assert.assertEquals("2", m.getParameterValue("b"));
    }
}
