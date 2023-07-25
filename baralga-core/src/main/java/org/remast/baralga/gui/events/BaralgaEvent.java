package org.remast.baralga.gui.events;

import java.beans.PropertyChangeEvent;
import java.util.List;

import org.remast.baralga.model.ProjectActivity;
import org.remast.util.TextResourceBundle;

/**
 * Events of Baralga.
 * @author remast
 */
public class BaralgaEvent {

    private static final TextResourceBundle textBundle = TextResourceBundle.getBundle(BaralgaEvent.class);

    // Constants for ProTrack Events
    /** A project has been changed. I.e. a new project is active now. */
    public static final int PROJECT_CHANGED = 0;

    /** A project activity has been started. */
    public static final int PROJECT_ACTIVITY_STARTED = 1;

    /** A project activity has been stopped. */
    public static final int PROJECT_ACTIVITY_STOPPED = 2;

    /** A project has been added. */
    public static final int PROJECT_ADDED = 3;

    /** A project has been removed. */
    public static final int PROJECT_REMOVED = 4;

    /** A project activity has been added. */
    public static final int PROJECT_ACTIVITY_ADDED = 5;

    /** A project activity has been removed. */
    public static final int PROJECT_ACTIVITY_REMOVED = 6;

    /** A project activity has been changed. */
    public static final int PROJECT_ACTIVITY_CHANGED = 7;

    /** The filter has been changed. */
    public static final int FILTER_CHANGED = 8;

    /** The data has changed. */
    public static final int DATA_CHANGED = 9;

    /** The start time has changed. */
    public static final int START_CHANGED = 10;

    /** The stopwatch visibility has changed. */
    public static final int STOPWATCH_VISIBILITY_CHANGED = 11;

    /** mouse action caught from windows, user seems to be actively working with the computer. */
    public static final int USER_IS_INACTIVE = 12;

    /** The type of the event. */
    private final int type;

    /** The data of the event. */
    private Object data;

    /** A property hint of the event. */
    private PropertyChangeEvent propertyChangeEvent;

    /** The source that fired the event. */
    private Object source;

    public BaralgaEvent(final int type, final Object source) {
        this.type = type;
        this.source = source;
    }

    /**
     * Checks whether the event can be undone.
     * @return <code>true</code> if undoing the event is possible else <code>false</code>
     */
    public final boolean canBeUndone() {
        return this.type == PROJECT_ACTIVITY_REMOVED || this.type == PROJECT_ACTIVITY_ADDED;
    }

    public String getUndoText() {
        switch (this.type) {
            case PROJECT_ACTIVITY_REMOVED:
                return createUndoRedoActivityText("BaralgaEvent.UndoRemoveActivityText",
                        "BaralgaEvent.UndoRemoveActivitiesText");
            case PROJECT_ACTIVITY_ADDED:
                return createUndoRedoActivityText("BaralgaEvent.UndoAddActivityText",
                        "BaralgaEvent.UndoAddActivitiesText");
            default:
                return "-impossible-";
        }
    }

    public String getRedoText() {
        switch (this.type) {
            case PROJECT_ACTIVITY_REMOVED:
                return createUndoRedoActivityText("BaralgaEvent.RedoRemoveActivityText",
                        "BaralgaEvent.RedoRemoveActivitiesText");
            case PROJECT_ACTIVITY_ADDED:
                return createUndoRedoActivityText("BaralgaEvent.RedoAddActivityText",
                        "BaralgaEvent.RedoAddActivitiesText");
            default:
                return "-impossible-";
        }
    }

    private String createUndoRedoActivityText(String singleTextKey, String pluralTextKey) {
        @SuppressWarnings("unchecked")
        final List<ProjectActivity> projectActivities = (List<ProjectActivity>) this.data;
        final int activityCount = projectActivities.size();
        if (activityCount == 1) {
            final ProjectActivity projectActivity = projectActivities.get(0);
            return textBundle.textFor(singleTextKey, projectActivity.toString());
        } else {
            return textBundle.textFor(pluralTextKey, activityCount);
        }
    }

    @Override
    public String toString() {
        return "BaralgaEvent{" + "data: " + data + ", type: " + type + "}";
    }

    /**
     * Getter for the data.
     * @return the data
     */
    public Object getData() {
        return data;
    }

    /**
     * Setter for the data.
     * @param data the data to set
     */
    public void setData(final Object data) {
        this.data = data;
    }

    /**
     * @return the type
     */
    public int getType() {
        return type;
    }

    /**
     * @return the propertyHint
     */
    public PropertyChangeEvent getPropertyChangeEvent() {
        return propertyChangeEvent;
    }

    /**
     * @param propertyHint the propertyHint to set
     */
    public void setPropertyChangeEvent(final PropertyChangeEvent propertyHint) {
        this.propertyChangeEvent = propertyHint;
    }

    /**
     * @return the source
     */
    public Object getSource() {
        return source;
    }

    /**
     * @param source the source to set
     */
    public void setSource(final Object source) {
        this.source = source;
    }

}
