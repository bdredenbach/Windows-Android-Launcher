# Using Nth Explorer from another Android app

Nth Desktop 0.11.0 offers a file chooser through `ACTION_GET_CONTENT` and an explicit Nth contract. This selects existing files; it does not replace another application's private browser or Android's Storage Access Framework permission screens. Existing Nth applications need to adopt the contract in their own code; this repository changes only Nth Desktop.

## Generic chooser

Launch `Intent.ACTION_GET_CONTENT`, category `Intent.CATEGORY_OPENABLE`, a MIME type (for example `image/*`) and optionally `Intent.EXTRA_ALLOW_MULTIPLE`. Android may offer Nth Explorer with other installed compatible handlers. The user chooses the handler. There is no universal Android default-file-manager role that redirects every application's file flow.

## Explicit Nth integration

Show your app's optional “Use Nth Explorer” choice, then launch:

```java
Intent request = new Intent("com.nthexperience.desktop.action.PICK_FILE")
    .setComponent(new ComponentName("com.nthexperience.desktop",
        "com.nthexperience.desktop.ExplorerPickerActivity"))
    .addCategory(Intent.CATEGORY_OPENABLE)
    .setType("image/*")
    .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
try {
    fileChooser.launch(request); // Activity Result API launcher
} catch (ActivityNotFoundException missingNth) {
    fileChooser.launch(new Intent(Intent.ACTION_OPEN_DOCUMENT)
        .addCategory(Intent.CATEGORY_OPENABLE).setType("image/*")
        .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true));
}
```

Use your Activity Result callback's `RESULT_OK` data. Read every URI in `ClipData` for multiple selection, or `data.getData()` for a single file. Do not rely on filesystem paths. Use `ContentResolver.openInputStream(uri)` and close the stream. Handle revoked/unavailable content and I/O failures. Cancellation returns `RESULT_CANCELED` with no selection.

`Intent.EXTRA_MIME_TYPES` can specify a MIME list (up to 32). Nth supports at most 256 selected files per result. Directories and virtual documents are not returned. The user chooses from linked folders, or from primary device storage when Nth has separately approved all-files access. They can explicitly authorize another linked folder through Android's system picker. Device access is not transferred to the requesting app.

Nth returns **temporary read-only grants** for the selected `content://` URIs. It returns no write, persistable or prefix grants and no access to unselected files. Do not call `takePersistableUriPermission` on this result. If your app needs a durable copy, import the stream into its own storage while access remains available. If it needs long-term editable access, use the Android system `ACTION_OPEN_DOCUMENT` flow instead.

For save/create operations use `ACTION_CREATE_DOCUMENT`; for folder authorization use `ACTION_OPEN_DOCUMENT_TREE`. Nth deliberately does not register those system picker actions. A `DocumentsProvider` controls which folders and operations are available. Granting access to a folder in Nth does not automatically give other apps that folder.

The isolated emulator caller verifies Android's cross-UID grant enforcement. The test fixture provider, test caller, and Nth Desktop all have different UIDs. Neither test APK is shipped in the user installer.
