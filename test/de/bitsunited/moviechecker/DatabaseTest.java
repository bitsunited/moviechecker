package de.bitsunited.moviechecker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class DatabaseTest {

    private static final Path PERSISTENCE_PATH = Paths.get("test/tmp/db.xml");

    private static final Path P1 = Paths.get("test/tmp/1.file").toAbsolutePath();

    private static final Path P2 = Paths.get("test/tmp/2.file").toAbsolutePath();

    private static final Instant T1 = LocalDateTime.of(2016, 10, 26, 11, 03, 46).toInstant(ZoneOffset.UTC);

    private static final Instant T2 = T1.plus(5, ChronoUnit.DAYS).plus(135, ChronoUnit.MINUTES);

    private Database testee;

    @Before
    public void setup() throws IOException {
        Path directory = PERSISTENCE_PATH.getParent();
        if (!Files.isDirectory(directory)) {
            Files.createDirectories(directory);
        }

        this.testee = new Database(PERSISTENCE_PATH, null);
    }

    @Test
    public void testEmptyDatabase() {
        assertFalse(testee.isAutoSave());

        List<Path> list = testee.getPathList();
        assertNotNull(list);
        assertTrue(list.isEmpty());

        assertFalse(testee.containsEntry(P1));
        assertNull(testee.getEntryTime(P1));
        assertNull(testee.getEntryStdOut(P1));
        assertNull(testee.getEntryErrOut(P1));
        assertFalse(testee.isDirty());
    }

    @Test
    public void testAddEntry() {
        testee.addEntry(P1, T1, "Std Out 1", "Err Out 1");

        List<Path> list = testee.getPathList();
        assertNotNull(list);
        assertEquals(1, list.size());
        assertTrue(list.contains(P1));

        assertTrue(testee.containsEntry(P1));
        assertEquals(T1, testee.getEntryTime(P1));
        assertEquals("Std Out 1", testee.getEntryStdOut(P1));
        assertEquals("Err Out 1", testee.getEntryErrOut(P1));
        assertTrue(testee.isDirty());

        assertFalse(testee.containsEntry(P2));
        assertNull(testee.getEntryTime(P2));
        assertNull(testee.getEntryStdOut(P2));
        assertNull(testee.getEntryErrOut(P2));
    }

    @Test
    public void testUpdateEntry() {
        testee.addEntry(P1, T1, "Std Out 1", "Err Out 1");

        testee.addEntry(P1, T2, "Std Out 2", "Err Out 2");

        List<Path> list = testee.getPathList();
        assertNotNull(list);
        assertEquals(1, list.size());
        assertTrue(list.contains(P1));

        assertTrue(testee.containsEntry(P1));
        assertEquals(T2, testee.getEntryTime(P1));
        assertEquals("Std Out 2", testee.getEntryStdOut(P1));
        assertEquals("Err Out 2", testee.getEntryErrOut(P1));
        assertTrue(testee.isDirty());
    }

    @Test
    public void testUpdateSameEntry() throws IOException {
        testee.addEntry(P1, T1, "Std Out 1", "Err Out 1");
        assertTrue(testee.isDirty());

        testee.save();

        assertFalse(testee.isDirty());

        testee.addEntry(P1, T1, "Std Out 1", "Err Out 1");

        assertFalse(testee.isDirty());
    }

    @Test
    public void testRemoveExistingEntry() throws IOException {
        testee.addEntry(P1, T1, "Std Out 1", "Err Out 1");

        testee.save();
        assertFalse(testee.isDirty());

        testee.removeEntry(P1);
        assertTrue(testee.isDirty());

        List<Path> list = testee.getPathList();
        assertNotNull(list);
        assertTrue(list.isEmpty());

        assertFalse(testee.containsEntry(P1));
        assertNull(testee.getEntryTime(P1));
        assertNull(testee.getEntryStdOut(P1));
        assertNull(testee.getEntryErrOut(P1));
    }

    @Test
    public void testRemoveNotExistingEntry() {
        testee.removeEntry(P1);

        assertFalse(testee.isDirty());
    }

    @Test(expected = NoSuchFileException.class)
    public void testLoadNoFile() throws IOException {
        if (Files.exists(PERSISTENCE_PATH)) {
            Files.delete(PERSISTENCE_PATH);
        }

        testee.load();
    }

    @Test
    public void testSaveAndLoad() throws IOException {
        testee.addEntry(P1, T1, "Std Out 1", "Err Out 1");

        assertTrue(testee.isDirty());
        testee.save();
        assertFalse(testee.isDirty());

        testee = new Database(PERSISTENCE_PATH, null);
        testee.load();

        List<Path> list = testee.getPathList();
        assertNotNull(list);
        assertEquals(1, list.size());
        assertTrue(list.contains(P1));

        assertTrue(testee.containsEntry(P1));
        assertEquals(T1, testee.getEntryTime(P1));
        assertEquals("Std Out 1", testee.getEntryStdOut(P1));
        assertEquals("Err Out 1", testee.getEntryErrOut(P1));
        assertFalse(testee.isDirty());

        assertFalse(testee.containsEntry(P2));
        assertNull(testee.getEntryTime(P2));
        assertNull(testee.getEntryStdOut(P2));
        assertNull(testee.getEntryErrOut(P2));
    }

    @Test
    public void testSaveAgain() throws IOException, InterruptedException {
        testee.addEntry(P1, T1, "Std Out 1", "Err Out 1");

        testee.save();

        FileTime time = Files.getLastModifiedTime(PERSISTENCE_PATH);

        Thread.sleep(100);

        testee.save();

        assertEquals(time, Files.getLastModifiedTime(PERSISTENCE_PATH));
    }

    @Test
    public void testAutoSave() throws IOException, InterruptedException {
        testee.setAutoSave(true);
        assertTrue(testee.isAutoSave());

        testee.addEntry(P1, T1, "Std Out 1", "Err Out 1");

        assertFalse(testee.isDirty());

        FileTime time = Files.getLastModifiedTime(PERSISTENCE_PATH);
        Thread.sleep(100);

        testee.save();

        assertEquals(time, Files.getLastModifiedTime(PERSISTENCE_PATH));
    }
}
