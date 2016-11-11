//   Copyright 2016 @bitsunited
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
//
package de.bitsunited.moviechecker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.file.Paths;
import java.time.Duration;

import org.junit.Test;

public class UtilTest {
    @Test
    public void testPrintDuration() {
        assertEquals("", Util.print(null));
        assertEquals("17 seconds", Util.print(Duration.ofSeconds(17).plusMillis(6)));
        assertEquals("17 minutes 1 second", Util.print(Duration.ofMinutes(17).plusSeconds(1)));
        assertEquals("17 minutes", Util.print(Duration.ofMinutes(17)));
        assertEquals("1 minute 12 seconds", Util.print(Duration.ofSeconds(72)));
        assertEquals("2 hours 13 minutes 23 seconds", Util.print(Duration.ofHours(2).plusMinutes(13).plusSeconds(23)));
        assertEquals("1 hour 1 minute 1 second", Util.print(Duration.ofHours(1).plusMinutes(1).plusSeconds(1)));
    }

    @Test
    public void testHasFileExtension() {
        assertTrue(Util.hasFileExtension(Paths.get("file.mkv"), "mkv"));
        assertFalse(Util.hasFileExtension(Paths.get("file.mkv"), "mpg"));
        assertTrue(Util.hasFileExtension(Paths.get("file.mkv").toAbsolutePath(), "mkv"));
        assertFalse(Util.hasFileExtension(Paths.get("file.mkv").toAbsolutePath(), "mpg"));
    }

    @Test
    public void testReplaceFileExtension() {
        assertEquals(Paths.get("file.mkv"), Util.replaceFileExtension(Paths.get("file.mpg"), "mkv"));
        assertEquals(Paths.get("file.mkv").toAbsolutePath(), Util.replaceFileExtension(Paths.get("file.mpg").toAbsolutePath(), "mkv"));
        assertEquals(Paths.get("file.mkv").toAbsolutePath(), Util.replaceFileExtension(Paths.get("file.").toAbsolutePath(), "mkv"));
        assertEquals(Paths.get("file.mkv").toAbsolutePath(), Util.replaceFileExtension(Paths.get("file").toAbsolutePath(), "mkv"));
        assertEquals(Paths.get(".file.mkv").toAbsolutePath(), Util.replaceFileExtension(Paths.get(".file").toAbsolutePath(), "mkv"));
    }

    @Test
    public void testPrintFileSize() {
        assertEquals("1024 B", Util.printFileSize(1024));
        assertEquals("9024 B", Util.printFileSize(9024));
        assertEquals("19 kB", Util.printFileSize(19024));
        assertEquals("1024 kB", Util.printFileSize(1024000));
        assertEquals("1024 MB", Util.printFileSize(1024000000));
        assertEquals("14 GB", Util.printFileSize(14000000000l));
    }

    @Test
    public void testFindParameter() {
        String[] args = new String[] { "mode", "-d", "database.xml", "--count", "10", "-y" };
        assertEquals("database.xml", Util.findParameter(args, "-d", "--database"));
        assertEquals("10", Util.findParameter(args, "-c", "--count"));
        assertNull(Util.findParameter(args, "-x"));
        assertNull(Util.findParameter(args, "-y"));
    }
}
