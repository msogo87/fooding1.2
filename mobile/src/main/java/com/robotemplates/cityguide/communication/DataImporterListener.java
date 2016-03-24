package com.robotemplates.cityguide.communication;

import java.util.List;

/**
 * Created by msogolovsky on 20/03/2016.
 */
public interface DataImporterListener {
    void onDataImporterTaskCompleted(final List<MainDbObjectData> dIpoiList);
    void onDataImporterTaskFailed(int retVal);
}
