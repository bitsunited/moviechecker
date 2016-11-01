package de.bitsunited.moviechecker;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

public class Database {

    private static class Entry {
        private final Path path;
        private final Instant time;
        private final String stdOut;
        private final String errOut;

        public Entry(Path path, Instant time, String stdOut, String errOut) {
            this.path = Objects.requireNonNull(path);
            this.time = time;
            this.stdOut = stdOut;
            this.errOut = errOut;
        }

        public Instant getTime() {
            return time;
        }

        public String getStdOut() {
            return stdOut;
        }

        public String getErrOut() {
            return errOut;
        }

        public Path getPath() {
            return path;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((errOut == null) ? 0 : errOut.hashCode());
            result = prime * result + ((path == null) ? 0 : path.hashCode());
            result = prime * result + ((stdOut == null) ? 0 : stdOut.hashCode());
            result = prime * result + ((time == null) ? 0 : time.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Entry other = (Entry) obj;
            if (errOut == null) {
                if (other.errOut != null) {
                    return false;
                }
            } else if (!errOut.equals(other.errOut)) {
                return false;
            }
            if (path == null) {
                if (other.path != null) {
                    return false;
                }
            } else if (!path.equals(other.path)) {
                return false;
            }
            if (stdOut == null) {
                if (other.stdOut != null) {
                    return false;
                }
            } else if (!stdOut.equals(other.stdOut)) {
                return false;
            }
            if (time == null) {
                if (other.time != null) {
                    return false;
                }
            } else if (!time.equals(other.time)) {
                return false;
            }
            return true;
        }
    }

    private final Map<Path, Entry> entryMap;

    private boolean dirty;

    private boolean autoSave;

    private final Path persistencePath;

    private final ReadWriteLock lock;

    private final Executor autoSaveExecutor;

    public Database(Path persistencePath, Executor autoSaveExecutor) {
        this.persistencePath = Objects.requireNonNull(persistencePath);
        this.autoSaveExecutor = autoSaveExecutor;

        this.entryMap = new HashMap<>();
        this.lock = new ReentrantReadWriteLock();

        this.dirty = false;
        this.autoSave = false;
    }

    public boolean containsEntry(Path path) {
        lock.readLock().lock();
        try {
            return entryMap.get(path) != null;
        } finally {
            lock.readLock().unlock();
        }
    }

    public Instant getEntryTime(Path path) {
        lock.readLock().lock();
        try {
            Entry entry = entryMap.get(path);
            return entry != null ? entry.getTime() : null;
        } finally {
            lock.readLock().unlock();
        }
    }

    public String getEntryStdOut(Path path) {
        lock.readLock().lock();
        try {
            Entry entry = entryMap.get(path);
            return entry != null ? entry.getStdOut() : null;
        } finally {
            lock.readLock().unlock();
        }
    }

    public String getEntryErrOut(Path path) {
        lock.readLock().lock();
        try {
            Entry entry = entryMap.get(path);
            return entry != null ? entry.getErrOut() : null;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void addEntry(Path path, Instant time, String stdOut, String errOut) {
        Objects.requireNonNull(path);

        lock.writeLock().lock();
        try {
            Entry entry = entryMap.get(path);
            if (entry == null) {
                entry = new Entry(path, time, stdOut, errOut);
                entryMap.put(path, entry);
                dirty = true;
            } else {
                Entry newEntry = new Entry(path, time, stdOut, errOut);
                if (!entry.equals(newEntry)) {
                    entryMap.put(path, newEntry);
                    dirty = true;
                }
            }
        } finally {
            lock.writeLock().unlock();
        }

        autoSave();
    }

    public void removeEntry(Path path) {
        Objects.requireNonNull(path);

        lock.writeLock().lock();
        try {
            dirty |= entryMap.remove(path) != null;
        } finally {
            lock.writeLock().unlock();
        }

        autoSave();
    }

    public List<Path> getPathList() {
        lock.readLock().lock();
        try {
            List<Path> list = new ArrayList<>(entryMap.keySet());
            Collections.sort(list);
            return list;
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean isDirty() {
        lock.readLock().lock();
        try {
            return dirty;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void save() throws IOException {
        if (isDirty()) {
            lock.writeLock().lock();
            try {
                if (!dirty) {
                    return;
                }

                List<Entry> list = new ArrayList<>(entryMap.values());
                Collections.sort(list, (e1, e2) -> e1.getPath().compareTo(e2.getPath()));
                doSave(list);

                dirty = false;
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    private List<Entry> doLoad() throws IOException {
        List<Entry> entryList = new LinkedList<>();

        try {
            XMLInputFactory factory = XMLInputFactory.newFactory();
            BufferedReader bufferedReader = Files.newBufferedReader(persistencePath);
            try {
                XMLStreamReader reader = factory.createXMLStreamReader(bufferedReader);

                while (reader.hasNext()) {
                    int type = reader.next();
                    if (type == XMLStreamReader.START_ELEMENT) {
                        String elementName = reader.getLocalName();
                        Map<String, String> attributeMap = getAttributes(reader);

                        if ("entry".equals(elementName)) {
                            String pathValue = attributeMap.get("path");
                            if (pathValue != null) {
                                URI pathURI = URI.create(pathValue);
                                Path path = Paths.get(pathURI);

                                String timeValue = attributeMap.get("time");
                                Instant time = timeValue != null ? Instant.parse(timeValue) : null;

                                String stdOut = attributeMap.get("stdout");
                                String errOut = attributeMap.get("errout");

                                Entry entry = new Entry(path, time, stdOut, errOut);
                                entryList.add(entry);
                            }
                        }
                    }
                }

                reader.close();
            } finally {
                bufferedReader.close();
            }
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
        return entryList;
    }

    private static Map<String, String> getAttributes(XMLStreamReader reader) {
        Map<String, String> attributeMap = new HashMap<>();
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String name = reader.getAttributeLocalName(i);
            String value = reader.getAttributeValue(i);
            attributeMap.put(name, value);
        }
        return attributeMap;
    }

    public void load() throws IOException {
        List<Entry> entryList = doLoad();

        lock.writeLock().lock();
        try {
            entryMap.clear();
            entryList.forEach(e -> entryMap.put(e.getPath(), e));

            dirty = false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void doSave(List<Entry> entryList) throws IOException {
        XMLOutputFactory factory = XMLOutputFactory.newFactory();
        try {
            BufferedWriter bufferedWriter = Files.newBufferedWriter(persistencePath);
            try {
                XMLStreamWriter writer = factory.createXMLStreamWriter(bufferedWriter);

                writer.writeStartDocument("UTF-8", "1.0");
                writer.writeStartElement("database");

                for (Entry entry : entryList) {
                    String path = entry.getPath().toUri().toString();
                    String time = entry.getTime() != null ? entry.getTime().toString() : "";

                    writer.writeStartElement("entry");
                    writer.writeAttribute("path", path);
                    writer.writeAttribute("time", time);
                    writer.writeAttribute("stdout", entry.getStdOut());
                    writer.writeAttribute("errout", entry.getErrOut());
                    writer.writeEndElement();
                }

                writer.writeEndElement();
                writer.writeEndDocument();

                writer.close();
            } finally {
                bufferedWriter.close();
            }
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }

    public boolean isAutoSave() {
        return autoSave;
    }

    private void autoSave() {
        if (autoSave) {
            Runnable command = new Runnable() {

                @Override
                public void run() {
                    try {
                        save();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };
            if (autoSaveExecutor != null) {
                autoSaveExecutor.execute(command);
            } else {
                command.run();
            }
        }
    }

    public void setAutoSave(boolean autoSave) {
        this.autoSave = autoSave;

        autoSave();
    }
}
