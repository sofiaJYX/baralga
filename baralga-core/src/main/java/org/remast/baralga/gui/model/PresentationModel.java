package org.remast.baralga.gui.model;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.SortedList;
import com.google.common.eventbus.EventBus;
import org.joda.time.DateTime;
import org.remast.baralga.gui.BaralgaMain;
import org.remast.baralga.gui.events.BaralgaEvent;
import org.remast.baralga.gui.lists.ProjectFilterList;
import org.remast.baralga.gui.model.edit.EditStack;
import org.remast.baralga.gui.model.report.HoursByDayReport;
import org.remast.baralga.gui.model.report.HoursByMonthReport;
import org.remast.baralga.gui.model.report.HoursByProjectReport;
import org.remast.baralga.gui.model.report.HoursByQuarterReport;
import org.remast.baralga.gui.model.report.HoursByWeekReport;
import org.remast.baralga.gui.model.report.ObservingAccumulatedActivitiesReport;
import org.remast.baralga.gui.settings.UserSettings;
import org.remast.baralga.model.BaralgaDAO;
import org.remast.baralga.model.Project;
import org.remast.baralga.model.ProjectActivity;
import org.remast.baralga.model.filter.Filter;
import org.remast.baralga.model.io.DataBackup;
import org.remast.util.DateUtils;
import org.remast.util.TextResourceBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The model of the Baralga application. This is the model capturing both the
 * state of the application as well as the application logic. For further
 * information on the pattern see <a
 * href="http://www.martinfowler.com/eaaDev/PresentationModel.html">presentation
 * model</a>.
 * 
 * @author remast
 */
public class PresentationModel {

	private EventBus eventBus = new EventBus();

	public final EventBus getEventBus() {
		return eventBus;
	}

	/** The logger. */
	private static final Logger log = LoggerFactory.getLogger(PresentationModel.class);

	/** The bundle for internationalized texts. */
	private static final TextResourceBundle textBundle = TextResourceBundle.getBundle(BaralgaMain.class);

	/** The list of active projects. */
	private final SortedList<Project> projectList;

	/** The list of all projects (both active and inactive). */
	private final SortedList<Project> allProjectsList;

	/** The list of project activities. */
	private final SortedList<ProjectActivity> activitiesList;

	/** If project administration is allowed or not. */
	private boolean projectAdminAllowed;

	/** The currently selected project. */
	private Project selectedProject;

	/** The description of the current activity. */
	private String description;

	/** Flag indicating whether selected project is active or not. */
	private boolean active;

	/** Start date of activity. */
	private DateTime start;

	/** Stop date of activity. */
	private DateTime stop;

	/** Current activity filter. */
	private Filter filter;

	/** The stack of edit actions (for undo and redo). */
	private EditStack editStack;

	/** The Data Access Object for all persistent entities. */
	private BaralgaDAO baralgaDAO;

	/**
	 * Will be set to true when User wasn't actively working on the computer
	 * since threshold
	 */
	private boolean userWasInActive = false;

	/** the time in mills since user was last actively working on computer */
	private Long lastActivity = System.currentTimeMillis();

	/**
	 * Creates a new model.
	 */
	public PresentationModel() {
		this.projectList = new SortedList<>(new BasicEventList<>());
		this.allProjectsList = new SortedList<>(new BasicEventList<>());
		this.activitiesList = new SortedList<>(new BasicEventList<>());
	}

	/**
	 * Initializes the model.
	 */
	public void initialize() {
		if (log.isDebugEnabled()) {
			log.debug("Initializing the presentation model.");
		}

		this.active = UserSettings.instance().isActive();
		this.start = UserSettings.instance().getStart();
		final String activeProjectId = UserSettings.instance().getActiveProjectId();
		if (activeProjectId != null) {
			this.selectedProject = this.baralgaDAO.findProjectById(activeProjectId);
		}

		this.projectList.clear();

		final List<Project> projects = this.baralgaDAO.getAllProjects();
		for (Project project : projects) {
			if (project.isActive()) {
				this.projectList.add(project);
			}
		}

		this.allProjectsList.clear();
		this.allProjectsList.addAll(projects);

		this.projectAdminAllowed = baralgaDAO.isProjectAdministrationAllowed();

		this.activitiesList.clear();

		// Set restored filter from settings
		setFilter(UserSettings.instance().restoreFromSettings(), this);

		applyFilter();

		this.description = UserSettings.instance().getLastDescription();

		// If there is a active project that has been started on another day,
		// we end it here.
		if (active && start != null && !org.apache.commons.lang3.time.DateUtils.isSameDay(start.toDate(), DateUtils.getNow())) {
			try {
				stop();
			} catch (ProjectActivityStateException e) {
				// Ignore
			}
		}

		// Edit stack
		if (editStack == null) {
			editStack = new EditStack(this);
			eventBus.register(editStack);
		}

	}

	/**
	 * Applies the current filter to the activities.
	 */
	private void applyFilter() {
		log.debug("Applying filter to activities.");
		this.activitiesList.clear();
		this.activitiesList.addAll(this.baralgaDAO.getActivities(this.filter));
	}

	/**
	 * Add the given project.
	 * 
	 * @param project
	 *            the project to add
	 * @param source
	 *            the source of the edit activity
	 */
	public final void addProject(final Project project, final Object source) {
		log.debug("Adding project {}.", project);

		this.baralgaDAO.addProject(project);

		this.projectList.add(project);
		this.allProjectsList.add(project);

		final BaralgaEvent event = new BaralgaEvent(BaralgaEvent.PROJECT_ADDED, source);
		event.setData(project);

		notify(event);
	}

	/**
	 * Remove the given project.
	 * 
	 * @param project
	 *            the project to remove
	 * @param source
	 *            the source of the edit activity
	 */
	public final void removeProject(final Project project, final Object source) {
		log.debug("Removing project {}.", project);

		if (source.getClass().equals(BaralgaMain.class)) {
			// Don't confirm deletion during model migration.
		} else {
			if (!isProjectDeletionConfirmed(project)) {
				return;
			}
		}

		this.baralgaDAO.remove(project);

		this.projectList.remove(project);
		this.allProjectsList.remove(project);
		
		applyFilter();

		final BaralgaEvent event = new BaralgaEvent(BaralgaEvent.PROJECT_REMOVED, source);
		event.setData(project);

		notify(event);
	}

	/**
	 * Checks with user whether the given project and all associated activities
	 * should be deleted or not.
	 * 
	 * @return <code>true</code> if project shall be deleted else
	 *         <code>false</code>
	 */
	private boolean isProjectDeletionConfirmed(final Project project) {
		final JOptionPane pane = new JOptionPane(textBundle.textFor("ProjectDeleteConfirmDialog.Message", project.getTitle()), //$NON-NLS-1$
				JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION);

		final JDialog dialog = pane.createDialog(textBundle.textFor("ProjectDeleteConfirmDialog.Title")); //$NON-NLS-1$
		dialog.setVisible(true);
		dialog.dispose();

		final Object selectedValue = pane.getValue();

		return (selectedValue instanceof Integer) && (((Integer) selectedValue) == JOptionPane.YES_OPTION);
	}

	/**
	 * Start a project activity at the given time.<br/>
	 * <em>This method is meant for unit testing only!!</em>
	 * 
	 * @throws ProjectActivityStateException
	 *             if there is already a running project or if no project is
	 *             selected, currently
	 */
	public final void start(final DateTime startTime) throws ProjectActivityStateException {
		log.debug("Starting activity at {}.", startTime);

		if (getSelectedProject() == null) {
			throw new ProjectActivityStateException(textBundle.textFor("PresentationModel.NoActiveProjectSelectedError")); //$NON-NLS-1$
		}

		if (isActive()) {
			throw new ProjectActivityStateException(textBundle.textFor("PresentationModel.ActivityAllreadyRunningError")); //$NON-NLS-1$
		}

		// Mark as active
		setActive(true);

		// Set start time to now if null
		DateTime start;
		if (startTime == null) {
			start = DateUtils.getNowAsDateTime();
		} else {
			start = startTime;
		}

		setStart(start);

		// Fire start event
		final BaralgaEvent event = new BaralgaEvent(BaralgaEvent.PROJECT_ACTIVITY_STARTED, this);
		notify(event);
	}

	/**
	 * Start a project activity.
	 * 
	 * @throws ProjectActivityStateException
	 *             if there is already a running project
	 */
	public final void start() throws ProjectActivityStateException {
		start(DateUtils.getNowAsDateTime());
	}

	/**
	 * Helper method to notify all observers of an event.
	 * 
	 * @param event
	 *            the event to forward to the observers
	 */
	private void notify(final BaralgaEvent event) {
		log.debug("Sending event notification for {}.", event);
		eventBus.post(event);
	}

	/**
	 * Fires an event that a projects property has changed.
	 * 
	 * @param changedProject
	 *            the project that's changed
	 * @param propertyChangeEvent
	 *            the event to fire
	 */
	public void fireProjectChangedEvent(final Project changedProject, final PropertyChangeEvent propertyChangeEvent) {
		this.baralgaDAO.updateProject(changedProject);

		final BaralgaEvent event = new BaralgaEvent(BaralgaEvent.PROJECT_CHANGED, this);
		event.setData(changedProject);
		event.setPropertyChangeEvent(propertyChangeEvent);

		if (propertyChangeEvent.getPropertyName().equals(Project.PROPERTY_ACTIVE)) {
			if (changedProject.isActive()) {
				this.projectList.add(changedProject);
			} else {
				this.projectList.remove(changedProject);
			}
		} else if (propertyChangeEvent.getPropertyName().equals(Project.PROPERTY_TITLE)) {
			Optional<Project> origProject = this.projectList.stream().filter(p -> p.equals(changedProject)).findAny();
			if (origProject.isPresent() && this.projectList.contains(origProject.get())) {
				this.projectList.remove(origProject.get());
				this.projectList.add(changedProject);
			}

			origProject = this.allProjectsList.stream().filter(p -> p.equals(changedProject)).findAny();
			if (origProject.isPresent() &&  this.allProjectsList.contains(origProject.get())) {
				this.allProjectsList.remove(origProject.get());
				this.allProjectsList.add(changedProject);
			}
		}
	}

	/**
	 * Fires an event that a project activity's property has changed.
	 * 
	 * @param changedActivity
	 *            the project activity that's changed
	 * @param propertyChangeEvent
	 *            the event to fire
	 */
	public void fireProjectActivityChangedEvent(final ProjectActivity changedActivity, final PropertyChangeEvent propertyChangeEvent) {
		final BaralgaEvent event = new BaralgaEvent(BaralgaEvent.PROJECT_ACTIVITY_CHANGED, this);
		event.setData(changedActivity);
		event.setPropertyChangeEvent(propertyChangeEvent);

		this.baralgaDAO.updateActivity(changedActivity);

		// Check whether the activity has been filtered before and whether it is
		// filtered now (after the change).
		final boolean matchesFilter = filter != null && filter.matchesCriteria(changedActivity);

		if (activitiesList.contains(changedActivity)) {
			// Did match before but doesn't now.
			if (!matchesFilter) {
				activitiesList.remove(changedActivity);
			}
		} else {
			// Didn't match before but does now.
			if (matchesFilter) {
				activitiesList.add(changedActivity);
			}
		}

		notify(event);
	}

	/**
	 * Stop a project activity.
	 * @throws ProjectActivityStateException if there is no running project
	 * @see #stop(boolean)
	 */
	public final void stop() throws ProjectActivityStateException {
		// Stop with notifying observers.
		stop(true);
	}

	/**
	 * Stop a project activity.
	 * @throws ProjectActivityStateException if there is no running project
	 */
	public final void stop(final boolean notifyObservers) throws ProjectActivityStateException {
		final DateTime now = DateUtils.getNowAsDateTime();
		stop(now, notifyObservers);
	}

	/**
	 * Stop a project activity.
	 * @throws ProjectActivityStateException if there is no running project
	 */
	public final void stop(DateTime endDate, final boolean notifyObservers) throws ProjectActivityStateException {
		log.debug("Stopping activity.");

		if (!isActive()) {
			throw new ProjectActivityStateException(textBundle.textFor("PresentationModel.NoActiveProjectError")); //$NON-NLS-1$
		}

		DateTime now = DateUtils.getNowAsDateTime();
		if (endDate.compareTo(now) > 0) {
			throw new ProjectActivityStateException(textBundle.textFor("PresentationModel.EndDateNotInFuturError")); //$NON-NLS-1$
		}

		BaralgaEvent eventOnEndDay = null;
		DateTime stop2 = null;

		// If start is on a different day from now end the activity at 0:00 one
		// day after start.
		// Also make a new activity from 0:00 the next day until the stop time
		// of the next day.
		if (!org.apache.commons.lang3.time.DateUtils.isSameDay(start.toDate(), endDate.toDate())) {
			DateTime dt = new DateTime(start);
			dt = dt.plusDays(1);

			stop = dt.toDateMidnight().toDateTime();

			stop2 = DateUtils.getNowAsDateTime();
			final DateTime start2 = stop;

			ProjectActivity activityOnEndDay = new ProjectActivity(start2, stop2, getSelectedProject(), this.description);
			activityOnEndDay = this.baralgaDAO.addActivity(activityOnEndDay);
			this.activitiesList.add(activityOnEndDay);

			// Create Event for Project Activity
			eventOnEndDay = new BaralgaEvent(BaralgaEvent.PROJECT_ACTIVITY_ADDED, this);
			final List<ProjectActivity> activitiesOnEndDay = new ArrayList<>(1);
			activitiesOnEndDay.add(activityOnEndDay);
			eventOnEndDay.setData(activitiesOnEndDay);
		} else {
			stop = endDate;
		}

		ProjectActivity activityOnStartDay = new ProjectActivity(start, stop, getSelectedProject(), this.description);
		activityOnStartDay = this.baralgaDAO.addActivity(activityOnStartDay);
		this.activitiesList.add(activityOnStartDay);

		// Clear old activity
		description = "";
		UserSettings.instance().setLastDescription("");
		setActive(false);
		start = null;

		if (notifyObservers) {
			// Create Event for Project Activity
			BaralgaEvent event = new BaralgaEvent(BaralgaEvent.PROJECT_ACTIVITY_ADDED, this);
			final List<ProjectActivity> activitiesOnStartDay = new ArrayList<>(1);
			activitiesOnStartDay.add(activityOnStartDay);
			event.setData(activitiesOnStartDay);
			notify(event);

			if (eventOnEndDay != null) {
				notify(eventOnEndDay);
				stop = stop2;
			}

			// Create Stop Event
			event = new BaralgaEvent(BaralgaEvent.PROJECT_ACTIVITY_STOPPED, this);
			notify(event);
		}
	}

	public final void changeStopWatchVisibility() {
		final BaralgaEvent event = new BaralgaEvent(BaralgaEvent.STOPWATCH_VISIBILITY_CHANGED, this);
		notify(event);
	}

	/**
	 * Changes to the given project.
	 * @param activeProject the new active project
	 */
	public final void changeProject(final Project activeProject) {
		log.debug("Changing project to {}.", activeProject);

		// If there's no change we're done.
		if (Objects.equals(getSelectedProject(), activeProject)) {
			return;
		}

		// Store previous project
		final Project previousProject = getSelectedProject();

		// Set selected project to new project
		this.selectedProject = activeProject;

		// Set active project to new project
		if (activeProject == null) {
			UserSettings.instance().setActiveProjectId(null);
		} else {
			UserSettings.instance().setActiveProjectId(activeProject.getId());
		}

		final DateTime now = DateUtils.getNowAsDateTime();

		// If a project is currently running we create a new project activity.
		if (isActive()) {
			// 1. Stop the running project.
			setStop(now);

			// 2. Track recorded project activity.
			ProjectActivity activity = new ProjectActivity(start, stop, previousProject, description);
			activity = this.baralgaDAO.addActivity(activity);
			this.activitiesList.add(activity);

			// Clear description
			description = "";
			UserSettings.instance().setLastDescription("");

			// 3. Broadcast project activity event.
			final BaralgaEvent event = new BaralgaEvent(BaralgaEvent.PROJECT_ACTIVITY_ADDED, this);
			final List<ProjectActivity> activities = new ArrayList<>(1);
			activities.add(activity);
			event.setData(activities);

			notify(event);

			// Set start time to now.
			// :INFO: No need to clone instance because DateTime is immutable
			setStart(now);
		}

		// Fire project changed event
		final BaralgaEvent event = new BaralgaEvent(BaralgaEvent.PROJECT_CHANGED, this);
		event.setData(activeProject);
		notify(event);
	}

    public final void fireDurationFormatChanged() {
        final BaralgaEvent event = new BaralgaEvent(BaralgaEvent.DATA_CHANGED, this);
        notify(event);
    }

	/**
	 * Add a new activity to the model.
	 * 
	 * @param activity the activity to add
	 */
	public final void addActivity(final ProjectActivity activity, final Object source) {
		log.debug("Adding activity {}}.", activity);

		final List<ProjectActivity> activities = new ArrayList<>(1);
		activities.add(activity);

		this.addActivities(activities, source);
	}

	public final void addActivities(final List<ProjectActivity> newActivities, final Object source) {
		final Collection<ProjectActivity> activities = this.baralgaDAO.addActivities(newActivities);

		applyFilter();

		// Fire event
		final BaralgaEvent event = new BaralgaEvent(BaralgaEvent.PROJECT_ACTIVITY_ADDED, source);
		event.setData(activities);
		notify(event);
	}

	/**
	 * Remove an activity from the model.
	 * 
	 * @param activity
	 *            the activity to remove
	 */
	public final void removeActivity(final ProjectActivity activity, final Object source) {
		log.debug("Removing activity {}.", activity);

		final List<ProjectActivity> activities = new ArrayList<>(1);
		activities.add(activity);

		this.removeActivities(activities, source);
	}

	public final void removeActivities(final List<ProjectActivity> activities, final Object source) {
		this.baralgaDAO.removeActivities(activities);

		applyFilter();

		// Fire event
		final BaralgaEvent event = new BaralgaEvent(BaralgaEvent.PROJECT_ACTIVITY_REMOVED, source);

		event.setData(activities);
		notify(event);
	}

	public final SortedList<Project> getProjectList() {
		return projectList;
	}

	public final SortedList<Project> getAllProjectsList() {
		return allProjectsList;
	}

	public final SortedList<ProjectActivity> getActivitiesList() {
		return activitiesList;
	}

	public List<ProjectActivity> getAllActivitiesList() {
		return baralgaDAO.getActivities();
	}

	public final ProjectFilterList getProjectFilterList() {
		return new ProjectFilterList(this);
	}

	public final ObservingAccumulatedActivitiesReport getFilteredReport() {
		return new ObservingAccumulatedActivitiesReport(this);
	}

	public final HoursByWeekReport getHoursByWeekReport() {
		return new HoursByWeekReport(this);
	}

	public final HoursByQuarterReport getHoursByQuarterReport() {
	    	return new HoursByQuarterReport(this);
	}
	    
	public final HoursByMonthReport getHoursByMonthReport() {
		return new HoursByMonthReport(this);
	}

	public final HoursByDayReport getHoursByDayReport() {
		return new HoursByDayReport(this);
	}

	public final HoursByProjectReport getHoursByProjectReport() {
		return new HoursByProjectReport(this);
	}

	/**
	 * Gets the start time of the current activity.
	 */
	public final DateTime getStart() {
		return start;
	}

	/**
	 * Sets the start of a new activity.
	 */
	public final void setStart(final DateTime start) {
		this.start = start;
		UserSettings.instance().setStart(start);

		// Fire event
		final BaralgaEvent event = new BaralgaEvent(BaralgaEvent.START_CHANGED, this);
		event.setData(start);

		notify(event);
	}

	public final DateTime getStop() {
		return stop;
	}

	private void setStop(final DateTime stop) {
		this.stop = stop;
	}

	/**
	 * Checks whether a project activity is currently running.
	 */
	public boolean isActive() {
		return active;
	}

	public void setActive(final boolean active) {
		this.active = active;
		UserSettings.instance().setActive(active);
	}

	/**
	 * @return the activeProject
	 */
	public Project getSelectedProject() {
		return selectedProject;
	}

	public BaralgaDAO getDAO() {
		return baralgaDAO;
	}

	public void setDAO(BaralgaDAO baralgaDAO) {
		this.baralgaDAO = baralgaDAO;
	}

	public Filter getFilter() {
		return filter;
	}

	/**
	 * @param filter
	 *            the filter to set
	 * @param source
	 *            the source of the new filter
	 */
	public void setFilter(final Filter filter, final Object source) {
		if (Objects.equals(this.filter, filter)) {
			return;
		}

		// Store filter
		this.filter = filter;

		applyFilter();

		// Fire event
		final BaralgaEvent event = new BaralgaEvent(BaralgaEvent.FILTER_CHANGED, source);
		event.setData(filter);

		notify(event);
	}

	public final String getDescription() {
		return description;
	}

	public final void setDescription(final String description) {
		this.description = description;
	}

	public final EditStack getEditStack() {
		return editStack;
	}

	public void importData(Collection<Project> projectsForImport, Collection<ProjectActivity> activitiesForImport) {
		final Filter filter = getFilter();

		this.baralgaDAO.clearData();
		setFilter(null, this);

		this.baralgaDAO.addProjects(projectsForImport);
		this.baralgaDAO.addActivities(activitiesForImport);

		setFilter(filter, this);

		this.projectList.clear();
		this.projectList.addAll(this.baralgaDAO.getAllProjects());
        
        this.allProjectsList.clear();
		this.allProjectsList.addAll(this.baralgaDAO.getAllProjects());
		
        final BaralgaEvent event = new BaralgaEvent(BaralgaEvent.DATA_CHANGED, this);
        notify(event);
	}

	/**
	 * Save the model.
	 *
	 */
	public final void save() {
		new DataBackup(this).createBackup();
	}

	public boolean isProjectAdminAllowed() {
		return projectAdminAllowed;
	}

	/**
	 * Updates the time since last user activity and fires ActivityToggle event
	 * when threshold is reached.
	 */
	public void addUserActivity() {
		final long now = System.currentTimeMillis();
		long difference = now - this.lastActivity;

		if (difference >= UserSettings.instance().getInactivityThreshold()) {
			this.userWasInActive = true;
			final BaralgaEvent event = new BaralgaEvent(BaralgaEvent.USER_IS_INACTIVE, this);
			event.setData(this.lastActivity);
			notify(event);
		} else {
			this.lastActivity = now;
		}
	}

	public void resetUserInactivityState() {
		this.lastActivity = System.currentTimeMillis();
		this.userWasInActive = false;
	}

}
