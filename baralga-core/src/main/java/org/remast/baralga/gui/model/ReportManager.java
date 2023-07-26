package org.remast.baralga.gui.model;

import org.remast.baralga.gui.model.report.*;

public class ReportManager {
    private final PresentationModel presentationModel;

    public ReportManager(PresentationModel presentationModel) {
        this.presentationModel = presentationModel;
    }

    public ObservingAccumulatedActivitiesReport getFilteredReport() {
        return new ObservingAccumulatedActivitiesReport(presentationModel);
    }

    public HoursByWeekReport getHoursByWeekReport() {
        return new HoursByWeekReport(presentationModel);
    }

    public HoursByQuarterReport getHoursByQuarterReport() {
        return new HoursByQuarterReport(presentationModel);
    }

    public HoursByMonthReport getHoursByMonthReport() {
        return new HoursByMonthReport(presentationModel);
    }

    public HoursByDayReport getHoursByDayReport() {
        return new HoursByDayReport(presentationModel);
    }

    public HoursByProjectReport getHoursByProjectReport() {
        return new HoursByProjectReport(presentationModel);
    }
}
