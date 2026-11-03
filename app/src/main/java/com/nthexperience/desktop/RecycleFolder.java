package com.nthexperience.desktop;

import android.content.Context;
import android.net.Uri;
import android.provider.DocumentsContract;
import java.io.*;
import java.security.DigestOutputStream;
import java.util.*;
import java.util.zip.*;

/** Archives a bounded document tree, including empty folders and media types. */
final class RecycleFolder {
    private static final int MAX_ENTRIES = 2048;
    private static final int MAX_DEPTH = 32;

    static byte[] archive(Context context, Uri root, OutputStream output, long limit,
                          DocumentOps.Cancel cancel) throws Exception {
        java.security.MessageDigest digest = RecycleBin.digest();
        OutputStream bounded = new FilterOutputStream(new DigestOutputStream(output, digest)) {
            private long remaining = limit;
            @Override public void write(int value) throws IOException {
                if (--remaining < 0) throw tooLarge();
                out.write(value);
            }
            @Override public void write(byte[] bytes, int offset, int length) throws IOException {
                remaining -= length;
                if (remaining < 0) throw tooLarge();
                out.write(bytes, offset, length);
            }
        };
        try (ZipOutputStream zip = new ZipOutputStream(bounded)) {
            zip.setLevel(Deflater.NO_COMPRESSION);
            addChildren(context, root, "", zip, cancel, new HashSet<>(), 0);
        }
        return digest.digest();
    }

    private static IOException tooLarge() {
        return new IOException("The folder exceeds Recycle Bin capacity or its 100 MB item limit. Original kept.");
    }

    private static void addChildren(Context context, Uri parent, String prefix, ZipOutputStream zip,
                                    DocumentOps.Cancel cancel, Set<Uri> seen, int depth) throws Exception {
        if (depth >= MAX_DEPTH) throw new IOException("The folder tree is too deep. Original kept.");
        List<DocumentOps.Ref> children = DocumentOps.children(context, parent, cancel);
        children.sort(Comparator.comparing(ref -> ref.item.name));
        for (DocumentOps.Ref child : children) {
            cancel.check();
            if (!seen.add(child.uri) || seen.size() > MAX_ENTRIES)
                throw new IOException("The folder tree is too large or contains repeated entries. Original kept.");
            String name = safeName(child.item.name);
            boolean directory = child.item.directory();
            String path = prefix + name + (directory ? "/" : "");
            ZipEntry entry = new ZipEntry(path);
            entry.setTime(new GregorianCalendar(2000, Calendar.JANUARY, 1).getTimeInMillis());
            entry.setComment(child.item.mime);
            zip.putNextEntry(entry);
            if (!directory) try (InputStream in = RecycleBin.read(context, child.uri, cancel)) {
                long bytes = transfer(in, zip, cancel);
                if (child.item.size >= 0 && bytes != child.item.size)
                    throw new IOException("A folder file could not be read completely. Original kept.");
            }
            zip.closeEntry();
            if (directory) addChildren(context, child.uri, path, zip, cancel, seen, depth + 1);
        }
    }

    static void extract(Context context, File archive, Uri root, DocumentOps.Cancel cancel) throws Exception {
        Map<String, Uri> directories = new HashMap<>();
        directories.put("", root);
        try (ZipFile zip = new ZipFile(archive)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                cancel.check();
                ZipEntry entry = entries.nextElement();
                String path = entry.getName();
                String bare = entry.isDirectory() ? path.substring(0, path.length() - 1) : path;
                int slash = bare.lastIndexOf('/');
                Uri parent = directories.get(bare.substring(0, slash + 1));
                if (parent == null) throw new IOException("The recovery archive has a missing parent.");
                String mime = entry.isDirectory() ? DocumentsContract.Document.MIME_TYPE_DIR : entry.getComment();
                Uri created = DocumentsContract.createDocument(context.getContentResolver(), parent, mime,
                        safeName(bare.substring(slash + 1)));
                if (created == null) throw new IOException("The destination could not restore the folder contents.");
                if (entry.isDirectory()) directories.put(path, created);
                else try (InputStream in = zip.getInputStream(entry);
                          OutputStream out = RecycleBin.write(context, created, cancel)) {
                    transfer(in, out, cancel);
                }
            }
        }
    }

    private static String safeName(String name) throws IOException {
        try {
            if (DocumentOps.name(name).equals(name)) return name;
        } catch (IllegalArgumentException ignored) {}
        throw new IOException("A folder item has a name this provider cannot safely restore.");
    }

    private static long transfer(InputStream in, OutputStream out, DocumentOps.Cancel cancel) throws IOException {
        byte[] buffer = new byte[65536];
        int count;
        long total = 0;
        while ((count = in.read(buffer)) != -1) {
            cancel.check();
            out.write(buffer, 0, count);
            total += count;
        }
        return total;
    }
}
