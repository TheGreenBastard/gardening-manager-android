package org.gots.nuxeo;

import android.os.AsyncTask;
import android.util.Log;

import org.gots.utils.FileUtilities;
import org.nuxeo.android.cache.blob.BlobWithProperties;
import org.nuxeo.android.repository.DocumentManager;
import org.nuxeo.android.upload.FileUploader;
import org.nuxeo.ecm.automation.client.jaxrs.AsyncCallback;
import org.nuxeo.ecm.automation.client.jaxrs.Session;
import org.nuxeo.ecm.automation.client.jaxrs.model.Document;
import org.nuxeo.ecm.automation.client.jaxrs.model.FileBlob;
import org.nuxeo.ecm.automation.client.jaxrs.model.PropertyMap;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Random;

public class NuxeoUtils {

    private static final String TAG = NuxeoUtils.class.getSimpleName();

    public static void attachBlobToDocument(Session session, Document document, PropertyMap blobProp) {

        try {
            StringBuilder toJSON = new StringBuilder("{ ");
            toJSON.append(" \"type\" : \"blob\"");
            toJSON.append(", \"length\" : " + blobProp.get("length"));
            toJSON.append(", \"mime-type\" : \"" + blobProp.get("mime-type") + "\"");
            toJSON.append(", \"name\" : \"" + blobProp.get("name") + "\"");
            toJSON.append(", \"upload-batch\" : \"" + blobProp.get("upload-batch") + "\"");
            toJSON.append(", \"upload-fileId\" : \"" + blobProp.get("upload-fileId") + "\" ");
            toJSON.append("}");
            PropertyMap properties = new PropertyMap();
            // properties.set("dc:title", blobProp.getString("name"));
            properties.set("file:content", toJSON.toString());
            Document doc = session.getAdapter(DocumentManager.class).update(document, properties);
        } catch (Exception e) {
            Log.i(TAG, e.getMessage());
        }

    }

    public static void downloadBlob(final DocumentManager service, final Document doc, final File file, final OnDownloadBlob mCallBack) {

        if (file != null && file.exists()) {
            Log.d(TAG, "downloadBlob: File already exists");
            return;
        }
        if (doc.getProperties().getString("file:filename") != null && !"null".equals(doc.getProperties().getString("file:filename"))) {
            return;
        }
        new AsyncTask<File, Void, FileBlob>() {
            @Override
            protected FileBlob doInBackground(File... params) {
                FileBlob image = null;

                try {
                    image = service.getBlob(doc);
                    Log.d(TAG, "downloadBlob temporary file = " + image.getFile().getAbsolutePath());
                    if (image != null && image.getLength() > 0) {
                        try {
                            FileUtilities.copy(image.getFile(), file);
                            Log.d(TAG, "downloadBlob " + image.getFileName());
                        } catch (IOException e) {
                            Log.w(TAG, "downloadBlob cannot copy " + image.getFile().getAbsolutePath() + " to " + file.getAbsolutePath());
                        }
                    } else {
                        Log.d(TAG, "downloadBlob didn't find image for document " + doc.getName());
                    }
                } catch (Exception e) {
                    Log.w(TAG, "downloadBlob Image " + file.getAbsolutePath() + " cannot be downloaded for document " + doc);
                }
                return image;
            }

            @Override
            protected void onPostExecute(FileBlob fileBlob) {
                if (mCallBack != null) {
                    if (fileBlob != null && fileBlob.getLength() > 0)
                        mCallBack.onDownloadSuccess(fileBlob);
                    else
                        mCallBack.onDownloadFailed(fileBlob);
                }
                super.onPostExecute(fileBlob);
            }
        }.execute();


    }

    public void uploadBlob(final Session session, final Document document, final File file, final OnBlobUpload callBack) {
        final FileBlob blobToUpload = new FileBlob(file);
        blobToUpload.setMimeType("image/jpeg");

        String batchId = String.valueOf(new Random().nextInt(1000));
        final String fileId = blobToUpload.getFileName();

        FileUploader uploader = session.getAdapter(FileUploader.class);
        BlobWithProperties blobUploading = uploader.storeFileForUpload(batchId, fileId, blobToUpload);
        String uploadUUID = blobUploading.getProperty(FileUploader.UPLOAD_UUID);
        Log.i(TAG, "Started blob upload UUID " + uploadUUID);
        final PropertyMap blobProp = new PropertyMap();
        blobProp.set("type", "blob");
        blobProp.set("length", Long.valueOf(blobUploading.getLength()));
        blobProp.set("mime-type", blobUploading.getMimeType());
        blobProp.set("name", blobToUpload.getFileName());
        // set information for server side Blob mapping
        blobProp.set("upload-batch", batchId);
        blobProp.set("upload-fileId", fileId);
        // set information for the update query to know it's
        // dependencies
        blobProp.set("android-require-type", "upload");
        blobProp.set("android-require-uuid", uploadUUID);
        uploader.startUpload(uploadUUID, new AsyncCallback<Serializable>() {
            @Override
            public void onSuccess(String executionId, Serializable data) {
                NuxeoUtils.attachBlobToDocument(session, document, blobProp);
                if (callBack != null)
                    callBack.onUploadSuccess(document, file, data);
                Log.i(TAG, "success");
            }

            @Override
            public void onError(String executionId, Throwable e) {
                if (callBack != null)
                    callBack.onUploadFailed(e.getMessage());
                Log.i(TAG, "errdroior");

            }
        });
    }

    public interface OnBlobUpload {
        void onUploadSuccess(Document document, File file, Serializable data);

        void onUploadFailed(String message);
    }

    public interface OnDownloadBlob {
        void onDownloadSuccess(FileBlob fileBlob);

        void onDownloadFailed(FileBlob fileBlob);
    }
}
