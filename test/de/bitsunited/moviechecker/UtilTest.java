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
        assertEquals("17.006sec", Util.print(Duration.ofSeconds(17).plusMillis(6)));
        assertEquals("17.060sec", Util.print(Duration.ofSeconds(17).plusMillis(60)));
        assertEquals("17.600sec", Util.print(Duration.ofSeconds(17).plusMillis(600)));
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
        String[] args = new String[] { "mode" , "-d", "database.xml", "--count" , "10", "-y"};
        assertEquals("database.xml", Util.findParameter(args, "-d", "--database"));
        assertEquals("10", Util.findParameter(args, "-c", "--count"));
        assertNull(Util.findParameter(args, "-x"));
        assertNull(Util.findParameter(args, "-y"));
    }
}
