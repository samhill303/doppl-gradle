/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.util;

import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;


/**
 * XML utility methods.
 */
public class Xml {

    /**
     * Creates a new pull parser with namespace support.
     *
     * <p><b>Note:</b> This is actually slower than the SAX parser, and it's not
     *   fully implemented. If you need a fast, mostly implemented pull parser,
     *   use this. If you need a complete implementation, use KXML.
     */
    public static XmlPullParser newPullParser() {
        try
        {
            final XmlPullParser xmlPullParser = XmlPullParserFactory.newInstance().newPullParser();
            return xmlPullParser;
        }
        catch(XmlPullParserException e)
        {
            throw new RuntimeException(e);
        }
        /*ExpatPullParser parser = new ExpatPullParser();
        parser.setNamespaceProcessingEnabled(true);
        return parser;*/
    }

    /**
     * Creates a new xml serializer.
     */
    public static XmlSerializer newSerializer() {
        try {
            return XmlSerializerFactory.instance.newSerializer();
        } catch (XmlPullParserException e) {
            throw new AssertionError(e);
        }
    }

    /** Factory for xml serializers. Initialized on demand. */
    static class XmlSerializerFactory {
        static final String TYPE
                = "org.kxml2.io.KXmlParser,org.kxml2.io.KXmlSerializer";
        static final XmlPullParserFactory instance;
        static {
            try {
                instance = XmlPullParserFactory.newInstance(TYPE, null);
            } catch (XmlPullParserException e) {
                throw new AssertionError(e);
            }
        }
    }

    /**
     * Supported character encodings.
     */
    public enum Encoding {

        US_ASCII("US-ASCII"),
        UTF_8("UTF-8"),
        UTF_16("UTF-16"),
        ISO_8859_1("ISO-8859-1");

        final String expatName;

        Encoding(String expatName) {
            this.expatName = expatName;
        }
    }

    /**
     * Finds an encoding by name. Returns UTF-8 if you pass {@code null}.
     */
    public static Encoding findEncodingByName(String encodingName)
            throws UnsupportedEncodingException {
        if (encodingName == null) {
            return Encoding.UTF_8;
        }

        for (Encoding encoding : Encoding.values()) {
            if (encoding.expatName.equalsIgnoreCase(encodingName))
                return encoding;
        }
        throw new UnsupportedEncodingException(encodingName);
    }
}
