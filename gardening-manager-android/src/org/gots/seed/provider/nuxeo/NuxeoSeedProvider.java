package org.gots.seed.provider.nuxeo;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.gots.exception.GardenNotFoundException;
import org.gots.exception.NotImplementedException;
import org.gots.garden.GardenInterface;
import org.gots.garden.GotsGardenManager;
import org.gots.nuxeo.NuxeoManager;
import org.gots.nuxeo.NuxeoUtils;
import org.gots.seed.BaseSeedInterface;
import org.gots.seed.LikeStatus;
import org.gots.seed.SpeciesDocument;
import org.gots.seed.provider.local.LocalSeedProvider;
import org.gots.utils.FileUtilities;
import org.nuxeo.android.repository.DocumentManager;
import org.nuxeo.ecm.automation.client.android.AndroidAutomationClient;
import org.nuxeo.ecm.automation.client.cache.CacheBehavior;
import org.nuxeo.ecm.automation.client.jaxrs.Session;
import org.nuxeo.ecm.automation.client.jaxrs.model.Blob;
import org.nuxeo.ecm.automation.client.jaxrs.model.DocRef;
import org.nuxeo.ecm.automation.client.jaxrs.model.Document;
import org.nuxeo.ecm.automation.client.jaxrs.model.Documents;
import org.nuxeo.ecm.automation.client.jaxrs.model.FileBlob;
import org.nuxeo.ecm.automation.client.jaxrs.model.IdRef;
import org.nuxeo.ecm.automation.client.jaxrs.model.PathRef;
import org.nuxeo.ecm.automation.client.jaxrs.model.PropertyMap;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

public class NuxeoSeedProvider extends LocalSeedProvider {

    protected static final String TAG = "NuxeoSeedProvider";

    private static final String QUERY_FILTER_LANGUAGE = " AND vendorseed:language=\""
            + Locale.getDefault().getCountry().toLowerCase() + "\"";

    private static final String QUERY_FILTER_SECTION = " AND ecm:path STARTSWITH '/default-domain/sections/'";

    String myToken;

    String myLogin;

    String myDeviceId;

    protected String myApp;

    // protected LazyUpdatableDocumentsList documentsList;

    public NuxeoSeedProvider(Context context) {
        super(context);
        myToken = gotsPrefs.getToken();
        myLogin = gotsPrefs.getNuxeoLogin();
        myDeviceId = gotsPrefs.getDeviceId();
        myApp = gotsPrefs.getGardeningManagerAppname();
        NuxeoManager.getInstance().initIfNew(context);
    }

    @Override
    public List<BaseSeedInterface> getVendorSeeds(boolean force, int page, int pageSize) {
        List<BaseSeedInterface> remoteVendorSeeds = new ArrayList<BaseSeedInterface>();
        List<BaseSeedInterface> myVendorSeeds = new ArrayList<BaseSeedInterface>();

        try {
            Session session = getNuxeoClient().getSession();
            final DocumentManager service = session.getAdapter(DocumentManager.class);

            byte cacheParam = CacheBehavior.STORE;
            boolean refresh = force;
            if (refresh) {
                cacheParam = (byte) (cacheParam | CacheBehavior.FORCE_REFRESH);
                refresh = false;
            }
            Documents docs = service.query("SELECT * FROM VendorSeed WHERE ecm:currentLifeCycleState = \"approved\""
                    + QUERY_FILTER_LANGUAGE + QUERY_FILTER_SECTION, null, new String[] { "dc:modified DESC" }, "*",
                    page, pageSize, cacheParam);
            if (docs.size() < pageSize) {
                docs.addAll(service.query("SELECT * FROM VendorSeed WHERE ecm:currentLifeCycleState = \"project\""
                        + QUERY_FILTER_LANGUAGE, null, new String[] { "dc:modified DESC" }, "*", page, pageSize,
                        cacheParam));

            }
            for (Document document : docs) {
                try {
                    BaseSeedInterface seed = NuxeoSeedConverter.convert(document);
                    Blob likeStatus = service.getLikeStatus(document);
                    LikeStatus likes = NuxeoSeedConverter.getLikeStatus(likeStatus);
                    if (seed != null) {
                        seed.setLikeStatus(likes);
                        remoteVendorSeeds.add(seed);
                        Log.i(TAG, seed.toString());
                    } else {
                        Log.w(TAG, "Nuxeo Seed conversion problem " + document.getTitle() + "- " + document.getId());
                    }
                    downloadImageAsync(service, document, seed);
                } catch (NumberFormatException formatException) {
                    Log.w(TAG,
                            formatException.getMessage() + " for Document " + document.getTitle() + " - "
                                    + document.getId());
                }

            }
            myVendorSeeds = synchronize(super.getVendorSeeds(force, page, pageSize), remoteVendorSeeds);
        } catch (Exception e) {
            Log.e(TAG, "getAllSeeds " + e.getMessage(), e);
            myVendorSeeds = super.getVendorSeeds(force, 0, 25);
        }
        return myVendorSeeds;
    }

    protected void downloadImageAsync(final DocumentManager service, final Document document, BaseSeedInterface seed) {
        new AsyncTask<BaseSeedInterface, Void, FileBlob>() {

            private File imageFile;

            @Override
            protected FileBlob doInBackground(BaseSeedInterface... params) {
                BaseSeedInterface seed = params[0];
                File imageFile = new File(gotsPrefs.getFilesDir(),
                        seed.getVariety().toLowerCase().replaceAll("\\s", "").replaceAll(" ", ""));
                if (!imageFile.exists())
                    NuxeoUtils.downloadBlob(service, document, imageFile);
                else {
                    Log.d(TAG, "Image " + imageFile.getAbsolutePath() + " already exists");
                }
                return null;
            }

        }.execute(seed);
    }

    @Override
    public BaseSeedInterface getSeedByBarCode(String barecode) {
        List<BaseSeedInterface> remoteVendorSeeds = new ArrayList<BaseSeedInterface>();
        BaseSeedInterface scannedSeed = null;
        try {
            Session session = getNuxeoClient().getSession();
            DocumentManager service = session.getAdapter(DocumentManager.class);

            byte cacheParam = CacheBehavior.STORE;
            boolean refresh = true;
            if (refresh) {
                cacheParam = (byte) (cacheParam | CacheBehavior.FORCE_REFRESH);
                refresh = false;
            }
            Documents docs = service.query(
                    "SELECT * FROM VendorSeed WHERE ecm:currentLifeCycleState != \"deleted\" AND vendorseed:barcode=\""
                            + Long.parseLong(barecode) + "\"", null, new String[] { "dc:modified DESC" }, "*", 0, 200,
                    cacheParam);

            for (Iterator<Document> iterator = docs.iterator(); iterator.hasNext();) {
                Document document = iterator.next();
                BaseSeedInterface seed = NuxeoSeedConverter.convert(document);
                if (seed != null) {

                    remoteVendorSeeds.add(seed);
                    Log.i(TAG, "Nuxeo Seed: " + seed);
                } else {
                    Log.w(TAG, "Nuxeo Seed conversion problem " + document.getTitle() + "- " + document.getId());
                }
            }
            if (remoteVendorSeeds.size() > 0)
                scannedSeed = remoteVendorSeeds.get(0);

        } catch (Exception e) {
            Log.e(TAG, "getSeedByBarCode " + e.getMessage(), e);
            scannedSeed = super.getSeedByBarCode(barecode);
        }
        return scannedSeed;
    }

    protected List<BaseSeedInterface> synchronize(List<BaseSeedInterface> localVendorSeeds,
            List<BaseSeedInterface> remoteVendorSeeds) {
        newSeeds.clear();
        List<BaseSeedInterface> myVendorSeeds = new ArrayList<BaseSeedInterface>();

        for (BaseSeedInterface remoteSeed : remoteVendorSeeds) {
            boolean found = false;
            for (BaseSeedInterface localSeed : localVendorSeeds) {
                if (remoteSeed.getUUID() != null && remoteSeed.getUUID().equals(localSeed.getUUID())) {
                    // local and remote
                    // 1: overwrite remote
                    // updateRemoteGarden(localSeed);
                    // 2: TODO sync with remote instead
                    // syncGardens(localGarden,remoteGarden);
                    found = true;
                    remoteSeed.setId(localSeed.getSeedId());
                    // myVendorSeeds.add(localSeed);

                    break;
                }
            }
            if (found) {
                myVendorSeeds.add(super.updateSeed(remoteSeed));
            }
            // myVendorSeeds.add();}

            else {
                BaseSeedInterface seed = super.createSeed(remoteSeed, null);
                myVendorSeeds.add(seed);
                newSeeds.add(seed);
            }
        }

        for (BaseSeedInterface localSeed : localVendorSeeds) {

            if (localSeed.getUUID() == null) {
                myVendorSeeds.add(localSeed);
                localSeed = createNuxeoVendorSeed(localSeed, null);
            } else {
                boolean found = false;
                for (BaseSeedInterface remoteSeed : remoteVendorSeeds) {
                    if (remoteSeed.getUUID() != null && remoteSeed.getUUID().equals(localSeed.getUUID())) {
                        found = true;
                        break;
                    }
                }
                if (!found) { // local only with UUID -> delete local
                    // TODO take a decision if local seed should be remove if the remote description is removed.
                    // super.remove(localSeed);
                }
            }

            // TODO update remote stock from local stock
            if (localSeed.getNbSachet() > 0) {
                try {
                    updateStock(localSeed, GotsGardenManager.getInstance().initIfNew(mContext).getCurrentGarden());
                } catch (GardenNotFoundException e) {
                    Log.e(myApp, e.getMessage());
                }
            }
        }

        // List<BaseSeedInterface> myStock =
        // getMyStock(GardenManager.getInstance().initIfNew(mContext).getCurrentGarden());
        // for (Iterator<BaseSeedInterface> myVendorSeed = myVendorSeeds.iterator(); myVendorSeed.hasNext();) {
        // BaseSeedInterface baseSeedInterface = myVendorSeed.next();
        //
        // boolean foundStock = false;
        // for (Iterator<BaseSeedInterface> seedStock = myStock.iterator(); seedStock.hasNext();) {
        // BaseSeedInterface stockSeed = seedStock.next();
        // if (stockSeed.getUUID().equals(baseSeedInterface.getUUID())) {
        // baseSeedInterface.setNbSachet(stockSeed.getNbSachet());
        // foundStock = true;
        // break;
        // }
        //
        // }
        // if (!foundStock)
        // baseSeedInterface.setNbSachet(0);
        //
        // }
        return myVendorSeeds;
    }

    @Override
    public BaseSeedInterface getSeedById(int id) {
        return super.getSeedById(id);
    }

    @Override
    public BaseSeedInterface getSeedByUUID(String uuid) {
        BaseSeedInterface remoteSeed = null;

        Session session = getNuxeoClient().getSession();
        DocumentManager service = session.getAdapter(DocumentManager.class);
        final BaseSeedInterface localSeed = super.getSeedByUUID(uuid);
        try {
            Document doc = service.getDocument(new IdRef(uuid), true);
            remoteSeed = NuxeoSeedConverter.convert(doc);
            if (localSeed == null) {
                remoteSeed = super.createSeed(remoteSeed, null);
            } else {
                remoteSeed.setId(localSeed.getSeedId());
                remoteSeed = super.updateSeed(remoteSeed);
            }
        } catch (Exception e) {
            remoteSeed = localSeed;
            Log.e(TAG, "" + e.getMessage());
        }

        return remoteSeed;
    }

    @Override
    public BaseSeedInterface createSeed(BaseSeedInterface seed, File imageFile) {
        super.createSeed(seed, imageFile);
        return createNuxeoVendorSeed(seed, imageFile);
    }

    /*
     * Return new remote seed or null if error
     */
    protected BaseSeedInterface createNuxeoVendorSeed(BaseSeedInterface currentSeed, File file) {

        Session session = getNuxeoClient().getSession();
        DocumentManager service = session.getAdapter(DocumentManager.class);
        Document catalog = null;
        try {
            DocRef wsRef = new PathRef(service.getUserHome().getPath() + "/Catalog");
            catalog = service.getDocument(wsRef);
        } catch (Exception e) {
            Log.e(TAG, "Fetching folder Catalog " + e.getMessage(), e);

            Document folder;
            try {
                Document root = service.getDocument(service.getUserHome());

                folder = service.createDocument(root, "Hut", "Catalog");
                PropertyMap map = folder.getProperties();
                map.set("dc:title", "Catalog");
                service.update(folder, map);
                catalog = folder;

                Log.d(TAG, "create folder Catalog UUID " + folder.getId());
            } catch (Exception e1) {
                Log.e(TAG, "Creating folder Catalog " + e.getMessage(), e);
            }

        }

        if (catalog == null)
            return null;

        try {
            final Document documentVendorSeed = service.createDocument(catalog, "VendorSeed", currentSeed.getVariety(),
                    NuxeoSeedConverter.convert(catalog.getPath(), currentSeed).getProperties());
            // ****************** FILE UPLOAD ***************
            if (file != null) {
                NuxeoUtils.uploadBlob(session, documentVendorSeed, file);
            }
            currentSeed.setUUID(documentVendorSeed.getId());
            Log.d(TAG, "RemoteSeed UUID " + documentVendorSeed.getId());
            super.updateSeed(currentSeed);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }

        return currentSeed;
    }

    protected AndroidAutomationClient getNuxeoClient() {
        return NuxeoManager.getInstance().getNuxeoClient();
    }

    @Override
    public BaseSeedInterface addToStock(BaseSeedInterface vendorSeed, GardenInterface garden) {

        try {
            Session session = getNuxeoClient().getSession();
            DocumentManager service = session.getAdapter(DocumentManager.class);
            // TODO Change this when garden UUID manage uuid and not path
            Document gardenFolder = service.getDocument(new IdRef(garden.getUUID()));
            Document stockFolder = service.getDocument(new PathRef(gardenFolder.getPath() + "/My Stock"));

            Document stockitem;
            int quantity = 0;
            try {
                Document seedDoc = service.getDocument(new IdRef(vendorSeed.getUUID()));
            } catch (Exception e) {
                createNuxeoVendorSeed(vendorSeed, null);
            }
            try {
                stockitem = service.getDocument(new PathRef(stockFolder.getPath() + "/" + vendorSeed.getSpecie() + " "
                        + vendorSeed.getVariety()), true);
                quantity = Integer.valueOf(stockitem.getString("stockitem:quantity"));
            } catch (Exception e) {
                Log.i(TAG, e.getMessage() + " The seed is not in f");
                stockitem = service.createDocument(stockFolder, "StockItem",
                        vendorSeed.getSpecie() + " " + vendorSeed.getVariety());
                service.createRelation(stockitem, "http://purl.org/dc/terms/isFormatOf",
                        new PathRef(vendorSeed.getUUID()));

            }

            PropertyMap map = new PropertyMap();
            map.set("stockitem:quantity", "" + ++quantity);
            map.set("dc:title", vendorSeed.getSpecie() + " " + vendorSeed.getVariety());
            map.set("stockitem:vendorseedid", vendorSeed.getUUID());

            service.update(stockitem, map);
            vendorSeed = super.addToStock(vendorSeed, garden);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return vendorSeed;
    }

    public void updateStock(BaseSeedInterface vendorSeed, GardenInterface garden) {

        try {
            Session session = getNuxeoClient().getSession();
            DocumentManager service = session.getAdapter(DocumentManager.class);
            // TODO Change this when garden UUID manage uuid and not path
            Document gardenFolder = service.getDocument(new IdRef(garden.getUUID()));
            Document stockFolder = service.getDocument(new PathRef(gardenFolder.getPath() + "/My Stock"));

            Document stockitem;
            int quantity = vendorSeed.getNbSachet();
            try {
                stockitem = service.getDocument(new PathRef(stockFolder.getPath() + "/" + vendorSeed.getSpecie() + " "
                        + vendorSeed.getVariety()), true);
            } catch (Exception e) {
                Log.i(TAG, e.getMessage(), e);
                stockitem = service.createDocument(stockFolder, "StockItem",
                        vendorSeed.getSpecie() + " " + vendorSeed.getVariety());
                service.createRelation(stockitem, "http://purl.org/dc/terms/isFormatOf",
                        new PathRef(vendorSeed.getUUID()));

            }

            PropertyMap map = new PropertyMap();
            map.set("stockitem:quantity", "" + quantity);
            map.set("dc:title", vendorSeed.getSpecie() + " " + vendorSeed.getVariety());
            map.set("stockitem:vendorseedid", vendorSeed.getUUID());

            service.update(stockitem, map);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }

    }

    @Override
    public BaseSeedInterface removeToStock(BaseSeedInterface vendorSeed, GardenInterface garden) {

        try {
            Session session = getNuxeoClient().getSession();
            DocumentManager service = session.getAdapter(DocumentManager.class);
            // TODO Change this when garden UUID manage uuid and not path
            Document gardenFolder = service.getDocument(new IdRef(garden.getUUID()));
            Document stockFolder = service.getDocument(new PathRef(gardenFolder.getPath() + "/My Stock"));

            Document stockitem;
            int quantity = 0;
            stockitem = service.getDocument(new PathRef(stockFolder.getPath() + "/" + vendorSeed.getSpecie() + " "
                    + vendorSeed.getVariety()), true);
            // stockitem = service.getDocument(new PathRef(stockFolder.getPath() + "/" + vendorSeed.getSpecie() + " "
            // + vendorSeed.getVariety()), "*");
            quantity = Integer.valueOf(stockitem.getString("stockitem:quantity"));

            if (quantity > 0) {
                PropertyMap map = new PropertyMap();
                map.set("stockitem:quantity", "" + --quantity);
                service.update(stockitem, map);
            }
            vendorSeed = super.removeToStock(vendorSeed, garden);
            // service.remove(vendorSeed.getUUID());

        } catch (Exception e) {
            Log.e(TAG, vendorSeed.toString() + "\n" + e.getMessage(), e);
        }
        return vendorSeed;
    }

    @Override
    public List<BaseSeedInterface> getMyStock(GardenInterface garden, boolean force) {
        List<BaseSeedInterface> mySeeds = new ArrayList<BaseSeedInterface>();
        try {
            Session session = getNuxeoClient().getSession();
            DocumentManager service = session.getAdapter(DocumentManager.class);
            Document gardenFolder = service.getDocument(new IdRef(garden.getUUID()));
            Document stockFolder = service.getDocument(new PathRef(gardenFolder.getPath() + "/My Stock"));
            byte cacheParam = CacheBehavior.STORE;
            if (force) {
                cacheParam = (byte) (cacheParam | CacheBehavior.FORCE_REFRESH);
            }
            Documents stockItems = service.query(
                    "SELECT * FROM StockItem WHERE ecm:currentLifeCycleState != \"deleted\" AND ecm:parentId=\""
                            + stockFolder.getId() + "\"", null, new String[] { "dc:modified DESC" }, "*", 0, 50,
                    cacheParam);

            for (Iterator<Document> iterator = stockItems.iterator(); iterator.hasNext();) {
                // Document stockItem = service.getDocument(iterator.next(), "*");
                Document stockItem = iterator.next();
                Documents relations = service.getRelations(stockItem, "http://purl.org/dc/terms/isFormatOf", true);
                if (relations.size() >= 1) {
                    Document originalSeed = service.getDocument(relations.get(0), "*");
                    // BaseSeedInterface seed = NuxeoSeedConverter.convert(originalSeed);
                    BaseSeedInterface seed = getSeedByUUID(originalSeed.getId());
                    if (seed == null)
                        continue;
                    seed.setNbSachet(Integer.valueOf(stockItem.getString("stockitem:quantity")));

                    // seed = super.getSeedByUUID(seed.getUUID());
                    if (seed.getNbSachet() > 0) {
                        super.updateSeed(seed);
                        mySeeds.add(seed);
                        Log.i(TAG, "getMyStock: " + seed.toString());
                    }
                }

            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);

        }

        // service.getRelations(doc, predicate);
        return mySeeds;
    }

    @Override
    public BaseSeedInterface updateSeed(BaseSeedInterface seed) {
        try {
            Session session = getNuxeoClient().getSession();
            DocumentManager service = session.getAdapter(DocumentManager.class);
            Document seedDoc = service.getDocument(new IdRef(seed.getUUID()));
            service.update(new IdRef(seed.getUUID()),
                    NuxeoSeedConverter.convert(seedDoc.getParentPath(), seed).getProperties());
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return super.updateSeed(seed);
    }

    @Override
    public void deleteSeed(BaseSeedInterface vendorSeed) {
        try {
            Session session = getNuxeoClient().getSession();
            DocumentManager service = session.getAdapter(DocumentManager.class);
            service.remove(new IdRef(vendorSeed.getUUID()));
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        super.deleteSeed(vendorSeed);

    }

    public LikeStatus like(BaseSeedInterface vendorSeed, boolean cancel) {
        Blob likeStatus;
        LikeStatus likes = new LikeStatus();
        try {
            Session session = getNuxeoClient().getSession();
            DocumentManager service = session.getAdapter(DocumentManager.class);
            Document doc = service.getDocument(new IdRef(vendorSeed.getUUID()));
            if (!cancel)
                likeStatus = service.like(doc);
            else
                likeStatus = service.cancelLike(doc);

            likes = NuxeoSeedConverter.getLikeStatus(likeStatus);
            vendorSeed.setLikeStatus(likes);
            super.updateSeed(vendorSeed);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return likes;
    }

    @Override
    public synchronized String[] getArraySpecies(boolean force) {
        List<String> latinNameSpecies = new ArrayList<String>();
        try {
            Session session = getNuxeoClient().getSession();
            DocumentManager service = session.getAdapter(DocumentManager.class);
            byte cacheParam = CacheBehavior.STORE;
            if (force) {
                cacheParam = (byte) (cacheParam | CacheBehavior.FORCE_REFRESH);
            }
            Documents docSpecies = service.query(
                    "SELECT * FROM Species WHERE ecm:currentLifeCycleState != \"deleted\"", null,
                    new String[] { "species:family_uuid DESC" }, "*", 0, 50, cacheParam);
            for (Document document : docSpecies) {
                latinNameSpecies.add(document.getTitle());

            }

            // Blob blob = (Blob) session.newRequest("Directory.Entries").set("directoryName", "topic").setHeader(
            // "Content-Type", "application/json+nxrequest").setInput(null).execute();
            //
            // // Blob blobSpecie = service.getDirectoryEntries("specie", "", true);
            // Log.i(TAG, blob.toString());
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        String[] arraySpecies = new String[latinNameSpecies.size()];
        return latinNameSpecies.toArray(arraySpecies);
    }

    @Override
    public SpeciesDocument getSpecies(boolean force) throws NotImplementedException {
        SpeciesDocument doc = null;
        try {
            Session session = getNuxeoClient().getSession();
            DocumentManager service = session.getAdapter(DocumentManager.class);
            byte cacheParam = CacheBehavior.STORE;
            if (force) {
                cacheParam = (byte) (cacheParam | CacheBehavior.FORCE_REFRESH);
            }
            Documents docSpecies = service.query(
                    "SELECT * FROM Species WHERE ecm:currentLifeCycleState != \"deleted\"", null,
                    new String[] { "species:family_uuid DESC" }, "*", 0, 50, cacheParam);
            if (docSpecies.size() > 0)
                doc = (SpeciesDocument) docSpecies.get(0);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return doc;
    }

    @Override
    public List<BaseSeedInterface> getSeedBySowingMonth(int month) {
        List<BaseSeedInterface> seedsBySowingMonth = new ArrayList<>();

        try {
            Session session = getNuxeoClient().getSession();
            DocumentManager service = session.getAdapter(DocumentManager.class);
            byte cacheParam = CacheBehavior.STORE;
            Documents docSpecies = service.query(
                    "SELECT * FROM VendorSeed WHERE ecm:currentLifeCycleState != \"deleted\" AND vendorseed:datesowingmax >= "
                            + month + " AND vendorseed:datesowingmin<=" + month + "" + QUERY_FILTER_LANGUAGE, null,
                    null, "*", 0, 50, cacheParam);
            for (Document seedDoc : docSpecies) {
                BaseSeedInterface seed = NuxeoSeedConverter.convert(seedDoc);
                if (super.getSeedByUUID(seedDoc.getId()) == null)
                    seed = super.createSeed(seed, null);
                else
                    seed = super.getSeedByUUID(seedDoc.getId());

                seedsBySowingMonth.add(seed);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return seedsBySowingMonth;
    }

    @Override
    public List<BaseSeedInterface> getVendorSeedsByName(String currentFilter, boolean force) {
        List<BaseSeedInterface> remoteVendorSeeds = new ArrayList<BaseSeedInterface>();
        List<BaseSeedInterface> myVendorSeeds = new ArrayList<BaseSeedInterface>();

        try {
            Session session = getNuxeoClient().getSession();
            DocumentManager service = session.getAdapter(DocumentManager.class);

            byte cacheParam = CacheBehavior.STORE;
            boolean refresh = force;
            if (refresh) {
                cacheParam = (byte) (cacheParam | CacheBehavior.FORCE_REFRESH);
                refresh = false;
            }
            Documents docs = service.query("SELECT * FROM VendorSeed WHERE ecm:currentLifeCycleState != \"deleted\""
                    + QUERY_FILTER_LANGUAGE + "AND vendorseed:variety STARTSWITH " + currentFilter, null,
                    new String[] { "dc:modified DESC" }, "*", 0, 25, cacheParam);
            for (Document document : docs) {
                BaseSeedInterface seed = NuxeoSeedConverter.convert(document);
                Blob likeStatus = service.getLikeStatus(document);
                LikeStatus likes = NuxeoSeedConverter.getLikeStatus(likeStatus);
                if (seed != null) {
                    seed.setLikeStatus(likes);
                    remoteVendorSeeds.add(seed);
                    Log.i(TAG, seed.toString());
                } else {
                    Log.w(TAG, "Nuxeo Seed conversion problem " + document.getTitle() + "- " + document.getId());
                }

                // download custom image
                File imageFile = new File(gotsPrefs.getFilesDir(),
                        seed.getVariety().toLowerCase().replaceAll("\\s", ""));
                if (imageFile != null && !imageFile.exists()) {
                    FileBlob image = service.getBlob(document);
                    if (image != null && image.getLength() > 0)
                        FileUtilities.copy(image.getFile(), imageFile);
                }
            }
            // getNuxeoClient().shutdown();
            myVendorSeeds = synchronize(super.getVendorSeedsByName(currentFilter, force), remoteVendorSeeds);
            // myVendorSeeds = remoteVendorSeeds;
        } catch (Exception e) {
            Log.e(TAG, "getVendorSeedsByName " + e.getMessage(), e);
            myVendorSeeds = super.getVendorSeedsByName(currentFilter, force);
        }
        return myVendorSeeds;
    }
}
