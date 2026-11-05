package com.flowstudio.app;

import android.content.*;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.*;

/** Read-only sharing, scoped to one granted export URI. */
public final class ExportProvider extends ContentProvider {
    public boolean onCreate() { return true; }
    private File resolve(Uri uri) throws FileNotFoundException {
        String name = uri.getLastPathSegment();
        if (name == null || !name.matches("foxy-[0-9]+\\.mp4")) throw new FileNotFoundException();
        File file = new File(getContext().getFilesDir(), "exports/" + name);
        if (!file.isFile()) throw new FileNotFoundException(); return file;
    }
    public String getType(Uri uri) { return "video/mp4"; }
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) throw new FileNotFoundException("Read only");
        return ParcelFileDescriptor.open(resolve(uri), ParcelFileDescriptor.MODE_READ_ONLY);
    }
    public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
        try { File file = resolve(uri); MatrixCursor cursor = new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE}); cursor.addRow(new Object[]{file.getName(), file.length()}); return cursor; }
        catch (FileNotFoundException e) { return null; }
    }
    public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException(); }
    public int update(Uri uri, ContentValues values, String where, String[] args) { throw new UnsupportedOperationException(); }
    public int delete(Uri uri, String where, String[] args) { throw new UnsupportedOperationException(); }
}
