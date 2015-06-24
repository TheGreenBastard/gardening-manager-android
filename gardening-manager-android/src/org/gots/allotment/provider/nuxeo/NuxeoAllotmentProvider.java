package org.gots.allotment.provider.nuxeo;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.gots.allotment.provider.local.LocalAllotmentProvider;
import org.gots.bean.BaseAllotmentInterface;
import org.gots.garden.GotsGardenManager;
import org.gots.nuxeo.NuxeoManager;
import org.gots.nuxeo.NuxeoUtils;
import org.nuxeo.android.repository.DocumentManager;
import org.nuxeo.ecm.automation.client.android.AndroidAutomationClient;
import org.nuxeo.ecm.automation.client.cache.CacheBehavior;
import org.nuxeo.ecm.automation.client.jaxrs.Session;
import org.nuxeo.ecm.automation.client.jaxrs.model.DocRef;
import org.nuxeo.ecm.automation.client.jaxrs.model.Document;
import org.nuxeo.ecm.automation.client.jaxrs.model.Documents;
import org.nuxeo.ecm.automation.client.jaxrs.model.IdRef;
import org.nuxeo.ecm.automation.client.jaxrs.model.PathRef;

import android.content.Context;
import android.util.Log;

public class NuxeoAllotmentProvider extends LocalAllotmentProvider {
    protected static final String TAG = "NuxeoAllotmentProvider";

    String myToken;

    String myLogin;

    String myDeviceId;

    protected String myApp;

    // protected LazyUpdatableDocumentsList documentsList = null;

    // protected BaseAllotmentInterface currentAllotment;

    public NuxeoAllotmentProvider(Context context) {
        super(context);
        myToken = gotsPrefs.getToken();
        myLogin = gotsPrefs.getNuxeoLogin();
        myDeviceId = gotsPrefs.getDeviceId();
        myApp = gotsPrefs.getGardeningManagerAppname();
        NuxeoManager.getInstance().initIfNew(context);
    }

    @Override
    public BaseAllotmentInterface getCurrentAllotment() {
        return super.getCurrentAllotment();
    }

    @Override
    public List<BaseAllotmentInterface> getMyAllotments(boolean force) {
        List<BaseAllotmentInterface> remoteAllotments = new ArrayList<BaseAllotmentInterface>();
        List<BaseAllotmentInterface> myAllotments = new ArrayList<BaseAllotmentInterface>();
        try {
            Session session = getNuxeoClient().getSession();
            DocumentManager service = session.getAdapter(DocumentManager.class);

            byte cacheParam = CacheBehavior.STORE;
            // TODO refresh depending on call

            if (force) {
                cacheParam = (byte) (cacheParam | CacheBehavior.FORCE_REFRESH);

            }
            // TODO Change this when garden UUID manage uuid and not path
            Document gardenFolder = service.getDocument(new IdRef(
                    GotsGardenManager.getInstance().initIfNew(mContext).getCurrentGarden().getUUID()));
            Document allotmentsFolder = service.getDocument(new PathRef(gardenFolder.getPath() + "/My Allotment"));

            Documents docs = service.query(
                    "SELECT * FROM Allotment WHERE ecm:currentLifeCycleState != \"deleted\" AND ecm:parentId=\""
                            + allotmentsFolder.getId() + "\"", null, new String[] { "dc:modified true" }, "*", 0, 50,
                    cacheParam);
            // documentsList = docs.asUpdatableDocumentsList();

            for (Iterator<Document> iterator = docs.iterator(); iterator.hasNext();) {
                Document document = iterator.next();
                BaseAllotmentInterface allotment = NuxeoAllotmentConverter.convert(document);
                remoteAllotments.add(allotment);
                File file = new File(gotsPrefs.getFilesDir().getAbsolutePath() + document.getId());
                allotment.setImagePath(file.getAbsolutePath());
                if (!file.exists())
                    NuxeoUtils.downloadBlob(service, document, file);
                Log.i(TAG, "Nuxeo Allotment " + allotment.toString());
            }

            List<BaseAllotmentInterface> localAllotments = super.getMyAllotments(force);
            myAllotments = synchronize(remoteAllotments, localAllotments);
        } catch (Exception e) {
            Log.e(TAG, "getMyAllotments " + e.getMessage(), e);
            return super.getMyAllotments(force);
        }
        return myAllotments;
    }

    protected List<BaseAllotmentInterface> synchronize(List<BaseAllotmentInterface> remoteAllotments,
            List<BaseAllotmentInterface> localAllotments) {
        boolean found;
        List<BaseAllotmentInterface> myAllotments = new ArrayList<BaseAllotmentInterface>();

        for (BaseAllotmentInterface remoteAllotment : remoteAllotments) {
            found = false;
            for (BaseAllotmentInterface localAllotment : localAllotments) {

                if (remoteAllotment.getUUID().equals(localAllotment.getUUID())) {
                    found = true;
                    break;
                }
            }
            if (found)
                myAllotments.add(super.getAllotmentByUUID(remoteAllotment.getUUID()));
            else
                myAllotments.add(super.createAllotment(remoteAllotment));

        }

        for (BaseAllotmentInterface localAllotment : localAllotments) {
            found = false;
            if (localAllotment.getUUID() == null)
                myAllotments.add(createNuxeoAllotment(localAllotment));
            else {
                for (BaseAllotmentInterface remoteAllotment : remoteAllotments) {
                    if (localAllotment.getUUID().equals(remoteAllotment.getUUID())) {
                        found = true;
                        break;
                    }

                }
                if (!found)
                    super.removeAllotment(localAllotment);
            }
        }

        return myAllotments;
    }

    @Override
    public BaseAllotmentInterface createAllotment(BaseAllotmentInterface allotment) {
        return createNuxeoAllotment(super.createAllotment(allotment));
    }

    protected AndroidAutomationClient getNuxeoClient() {
        return NuxeoManager.getInstance().getNuxeoClient();
    }

    private BaseAllotmentInterface createNuxeoAllotment(BaseAllotmentInterface allotment) {

        try {
            Session session = getNuxeoClient().getSession();
            DocumentManager service = session.getAdapter(DocumentManager.class);
            // TODO Change this when garden UUID manage uuid and not path
            Document gardenFolder = service.getDocument(new IdRef(
                    GotsGardenManager.getInstance().getCurrentGarden().getUUID()));
            Document allotmentsFolder = service.getDocument(new PathRef(gardenFolder.getPath() + "/My Allotment"));

            Document newAllotment = service.createDocument(allotmentsFolder, "Allotment", allotment.getName(),
                    NuxeoAllotmentConverter.convertToProperties(allotment));
            if (allotment.getImagePath() != null) {
                File f = new File(allotment.getImagePath());
                if (f.exists())
                    NuxeoUtils.uploadBlob(session, newAllotment, f);
            }
            allotment.setUUID(newAllotment.getId());
            allotment = super.updateAllotment(allotment);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return allotment;
    }

    @Override
    public int removeAllotment(BaseAllotmentInterface allotment) {
        super.removeAllotment(allotment);
        try {
            Log.d(TAG, "Removing document " + allotment.getUUID());
            Session session = getNuxeoClient().getSession();
            DocumentManager service = session.getAdapter(DocumentManager.class);
            service.remove(allotment.getUUID());

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return 0;
    }

    @Override
    public BaseAllotmentInterface updateAllotment(BaseAllotmentInterface allotment) {
        try {
            Log.d(TAG, "Updating document " + allotment.getUUID());
            Session session = getNuxeoClient().getSession();
            DocumentManager service = session.getAdapter(DocumentManager.class);
            Document updatedDoc = service.update(new DocRef(allotment.getUUID()),
                    NuxeoAllotmentConverter.convertToProperties(allotment));
            if (allotment.getImagePath() != null) {
                File f = new File(allotment.getImagePath());
                if (f.exists())
                    NuxeoUtils.uploadBlob(session, updatedDoc, f);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }

        return super.updateAllotment(allotment);
    }

    @Override
    public void setCurrentAllotment(BaseAllotmentInterface allotmentInterface) {
        super.setCurrentAllotment(allotmentInterface);
    }
}
