package org.gots.ui.fragment;

import org.gots.R;
import org.gots.bean.RouteNode;
import org.gots.bean.TaskButton;
import org.gots.nuxeo.NuxeoWorkflowProvider;
import org.nuxeo.ecm.automation.client.jaxrs.model.Document;
import org.nuxeo.ecm.automation.client.jaxrs.model.Documents;
import org.nuxeo.ecm.automation.client.jaxrs.model.PropertyMap;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

public class WorkflowTaskFragment extends BaseGotsFragment {
    public static final String GOTS_TASKWORKFLOW_ID = "org.gots.task.id";

    public static final String GOTS_DOC_ID = "org.gots.doc.id";

    // private TaskInfo taskWorkflow;

    TextView workflowTaskName;

    TextView workflowTaskDirective;

    TextView workflowTaskInitiator;

    LinearLayout buttonLayout;

    String docId;

    private String taskId;

    private RouteNode node;

    private String TAG = WorkflowTaskFragment.class.getSimpleName();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.workflow_task, null);
        workflowTaskDirective = (TextView) view.findViewById(R.id.textWorkflowTaskDirective);
        workflowTaskName = (TextView) view.findViewById(R.id.textWorkflowTaskTitle);
        workflowTaskInitiator = (TextView) view.findViewById(R.id.textWorkflowTaskInitiator);
        buttonLayout = (LinearLayout) view.findViewById(R.id.buttonWorkflowLayout);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        // if (getActivity().getIntent().getSerializableExtra(GOTS_TASKWORKFLOW_ID) != null)
        // taskWorkflow = (TaskInfo) getActivity().getIntent().getSerializableExtra(GOTS_TASKWORKFLOW_ID);
        if (getActivity().getIntent() != null)
            docId = getActivity().getIntent().getExtras().getString(GOTS_DOC_ID);

        // Button buttonRefuse = (Button) view.findViewById(R.id.buttonRefused);
        // buttonRefuse.setOnClickListener(this);
        // Button buttonApprove = (Button) view.findViewById(R.id.buttonApproved);
        // buttonApprove.setOnClickListener(this);

        super.onViewCreated(view, savedInstanceState);
    }

    // @Override
    // public void onClick(View v) {
    // switch (v.getId()) {
    // case R.id.buttonRefused:
    // new AsyncTask<Void, Void, Void>() {
    // String comment = "";
    //
    // protected void onPreExecute() {
    // };
    //
    // @Override
    // protected Void doInBackground(Void... params) {
    // NuxeoWorkflowProvider nuxeoWorkflowProvider = new NuxeoWorkflowProvider(getActivity());
    // nuxeoWorkflowProvider.completeTaskRefuse(taskId, comment);
    // return null;
    // }
    // }.execute();
    // break;
    // case R.id.buttonApproved:
    // new AsyncTask<Void, Void, Void>() {
    // String comment = "";
    //
    // @Override
    // protected Void doInBackground(Void... params) {
    // NuxeoWorkflowProvider nuxeoWorkflowProvider = new NuxeoWorkflowProvider(getActivity());
    // nuxeoWorkflowProvider.completeTaskValidate(taskId, comment);
    // return null;
    // }
    // }.execute();
    // break;
    //
    // default:
    // break;
    // }
    // }

    @Override
    public void update() {

    }

    @Override
    protected void onNuxeoDataRetrievalStarted() {
        super.onNuxeoDataRetrievalStarted();
    }

    @Override
    protected Object retrieveNuxeoData() throws Exception {
        NuxeoWorkflowProvider workflowProvider = new NuxeoWorkflowProvider(getActivity());
        Documents taskDocs = workflowProvider.getWorkflowOpenTasks(docId, true);
        Document currentTask = null;
        if (taskDocs != null && taskDocs.size() > 0) {
            currentTask = workflowProvider.getTaskDoc(taskDocs.get(0).getId());
            node = workflowProvider.getRouteNode(currentTask.getId());
        }

        return currentTask;
    }

    @Override
    protected void onNuxeoDataRetrieved(Object data) {
        if (getActivity() == null)
            return;
        Document doc = (Document) data;
        PropertyMap map = doc.getProperties();
        workflowTaskDirective.setText(map.getString("nt:directive"));
        workflowTaskName.setText(map.getString("nt:name"));
        workflowTaskInitiator.setText(map.getString("nt:initiator"));
        taskId = doc.getId();
        if (node != null) {
            buttonLayout.removeAllViews();
            for (final TaskButton taskButton : node.getTaskButtons()) {
                Button b = new Button(getActivity());
                b.setText(taskButton.getLabel());
                b.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        new AsyncTask<Void, Void, Document>() {

                            @Override
                            protected Document doInBackground(Void... params) {
                                NuxeoWorkflowProvider nuxeoWorkflowProvider = new NuxeoWorkflowProvider(getActivity());
                                Document doc = nuxeoWorkflowProvider.completeTask(taskId, taskButton.getName(),
                                        "un commentaire");
                                return doc;
                            }

                            protected void onPostExecute(Document result) {
                                if (result == null) {
                                    Log.w(TAG, "Error processing workflow " + taskButton.getName());

                                } else {
                                    Log.i(TAG, result.getId() + " follow workflow with task : " + taskButton.getName());

                                }
                                runAsyncDataRetrieval();
                            };

                        }.execute();
                    }
                });
                b.setPadding(5, 5, 5, 5);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.setMargins(5, 2, 5, 2);
                b.setLayoutParams(lp);
                buttonLayout.addView(b);

            }
        }
        super.onNuxeoDataRetrieved(data);
    }

    @Override
    protected void onNuxeoDataRetrieveFailed() {
        super.onNuxeoDataRetrieveFailed();
    }

    @Override
    protected boolean requireAsyncDataRetrieval() {
        return true;
    }

    // private class AsyncWorkflow extends AsyncTask<Void, Void, Void>{
    //
    // @Override
    // protected Void doInBackground(Void... params) {
    // return null;
    // }
    //
    // }
    // private void closeFragment() {
    // FragmentTransaction transaction = getFragmentManager().beginTransaction();
    // getFragmentManager().popBackStack();
    // transaction.hide(this);
    // transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
    // transaction.commit();
    // }

}
