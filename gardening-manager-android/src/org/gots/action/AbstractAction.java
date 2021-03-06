package org.gots.action;

import android.content.Context;

import org.gots.action.provider.GotsActionSeedProvider;
import org.gots.allotment.GotsAllotmentManager;
import org.gots.allotment.provider.AllotmentProvider;
import org.gots.context.GotsContext;
import org.gots.garden.GotsGardenManager;
import org.gots.preferences.GotsPreferences;
import org.gots.seed.GotsGrowingSeedManager;
import org.gots.seed.GotsSeedManager;

import java.util.Comparator;
import java.util.Date;

public class AbstractAction implements BaseAction, Comparator<AbstractActionSeed> {

    protected String name;
    protected GotsPreferences gotsPrefs;
    protected AllotmentProvider allotmentProvider;
    protected Context mContext;
    protected GotsSeedManager seedManager;
    protected GotsGardenManager gardenManager;
    protected GotsActionSeedProvider actionSeedManager;
    protected GotsGrowingSeedManager growingSeedManager;
    protected GotsActionManager actionManager;
    private String description;
    private int duration;
    private Date dateActionDone;
    private int id = -1;
    private String UUID;
    private int state;
    private Date dateActionTodo;
    private Object data;

    public AbstractAction() {
    }

    public AbstractAction(Context context) {
        this.mContext = context;
        gotsPrefs = getGotsContext().getServerConfig();
        seedManager = GotsSeedManager.getInstance().initIfNew(mContext);
        gardenManager = GotsGardenManager.getInstance().initIfNew(mContext);
        actionManager = GotsActionManager.getInstance().initIfNew(mContext);
        actionSeedManager = GotsActionSeedManager.getInstance().initIfNew(mContext);
        growingSeedManager = GotsGrowingSeedManager.getInstance().initIfNew(mContext);
        allotmentProvider = GotsAllotmentManager.getInstance().initIfNew(mContext);
    }

    protected GotsContext getGotsContext() {
        return GotsContext.get(mContext);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public int getDuration() {
        return duration;
    }

    @Override
    public void setDuration(int duration) {
        this.duration = duration;
    }

    @Override
    public Date getDateActionDone() {
        return dateActionDone;
    }

    @Override
    public void setDateActionDone(Date dateActionDone) {
        this.dateActionDone = dateActionDone;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public int getState() {

        return state;
    }

    @Override
    public void setState(int state) {
        this.state = state;
    }

    @Override
    public Date getDateActionTodo() {

        return this.dateActionTodo;
    }

    @Override
    public void setDateActionTodo(Date dateActionTodo) {
        this.dateActionTodo = dateActionTodo;
    }

    @Override
    public int compare(AbstractActionSeed lhs, AbstractActionSeed rhs) {

        return lhs.getDateActionDone().getTime() > rhs.getDateActionDone().getTime() ? -1 : 1;
    }

    @Override
    public Object getData() {
        return data;
    }

    @Override
    public void setData(Object data) {
        this.data = data;
    }

    protected Context getContext() {
        return mContext;
    }

    @Override
    public String getUUID() {
        return this.UUID;
    }

    @Override
    public void setUUID(String uuid) {
        this.UUID = uuid;
    }

    @Override
    public String toString() {
        String txt = new String();
        txt = txt.concat("[" + getId() + "]" + getName());
        txt = txt.concat("" + getUUID());
        txt = txt.concat(" / ");
        txt = txt.concat("Duration=" + getDuration());
        if (getDateActionDone() != null)
            txt = txt.concat(" / Done on " + getDateActionDone());
        return super.toString();
    }

}