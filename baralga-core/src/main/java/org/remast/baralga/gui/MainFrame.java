package org.remast.baralga.gui;

import com.google.common.eventbus.Subscribe;
import info.clearthought.layout.TableLayout;
import org.remast.baralga.FormatUtils;
import org.remast.baralga.gui.actions.AboutAction;
import org.remast.baralga.gui.actions.AbstractBaralgaAction;
import org.remast.baralga.gui.actions.AddActivityAction;
import org.remast.baralga.gui.actions.ExitAction;
import org.remast.baralga.gui.actions.ExportCsvAction;
import org.remast.baralga.gui.actions.ExportExcelAction;
import org.remast.baralga.gui.actions.ExportXmlAction;
import org.remast.baralga.gui.actions.ICalExportAction;
import org.remast.baralga.gui.actions.ImportXmlAction;
import org.remast.baralga.gui.actions.ManageProjectsAction;
import org.remast.baralga.gui.actions.SettingsAction;
import org.remast.baralga.gui.events.BaralgaEvent;
import org.remast.baralga.gui.model.PresentationModel;
import org.remast.baralga.gui.panels.ActivityPanel;
import org.remast.baralga.gui.panels.ReportPanel;
import org.remast.baralga.gui.settings.UserSettings;
import org.remast.util.TextResourceBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * The main frame of the application.
 * @author remast
 */
@SuppressWarnings("serial")//$NON-NLS-1$
public class MainFrame extends JFrame {
    
    /** The logger. */
    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(MainFrame.class);

    /** The bundle for internationalized texts. */
    private static final TextResourceBundle textBundle = TextResourceBundle.getBundle(MainFrame.class);

    /** The standard icon image. */
    private static final Image NORMAL_ICON = new ImageIcon(BaralgaMain.class.getResource("/icons/baralga_logo_32.png")).getImage(); //$NON-NLS-1$

    /** The icon image when an activity is running. */
    private static final Image ACTIVE_ICON = new ImageIcon(BaralgaMain.class.getResource("/icons/baralga_logo_red_32.png")).getImage(); //$NON-NLS-1$

    /** The model. */
    private final transient PresentationModel model;

    /** The tool bar. */
    private JToolBar toolBar = null;

    /**
     * The panel with details about the current activity. Like the current project and description.
     */
    private JPanel currentActivityPanel = null;

    // ------------------------------------------------
    // Other stuff
    // ------------------------------------------------

    /** The filtered report. */
    private ReportPanel reportPanel;


    // ------------------------------------------------
    // The menus
    // ------------------------------------------------

    /** The menu bar containing all menus. */
    private JMenuBar mainMenuBar = null;

    /** The file menu. */
    private JMenu fileMenu = null;

    /** The help menu. */
    private JMenu helpMenu = null;

    /** The export menu. */
    private JMenu exportMenu = null;
    
    /** The import menu. */
    private JMenu importMenu = null;

    /** The edit menu. */
    private JMenu editMenu = null;


    // ------------------------------------------------
    // The menu items
    // ------------------------------------------------

    private JMenuItem aboutMenuItem = null;

    private JMenuItem addActivityMenuItem = null;

    private JMenuItem editProjectsMenuItem = null;

    private JMenuItem exportExcelItem = null;
    
    private JMenuItem exportiCalItem = null;

    private JMenuItem exportCsvItem = null;
    
    private JMenuItem exportXmlItem = null;
    
    private JMenuItem importXmlItem = null;

    private JMenuItem exitItem = null;

    /**
     * This is the default constructor.
     * @param model the model
     */
    public MainFrame(final PresentationModel model) {
        super();

        this.model = model;
        this.model.getEventBus().register(this);

        initialize();
    }
    
    /**
     * Set up GUI components.
     */
    private void initialize() {
        this.setResizable(true);
        
        if (UserSettings.instance().isRememberWindowSizeLocation()) {
            this.setSize(UserSettings.instance().getWindowSize());
            this.setLocation(UserSettings.instance().getWindowLocation());
        } else {
            this.setSize(530, 720);
        }
        
        this.addComponentListener(new ComponentAdapter() {

            @Override
            public void componentMoved(final ComponentEvent e) {
                UserSettings.instance().setWindowLocation(MainFrame.this.getLocation());
            }

            @Override
            public void componentResized(final ComponentEvent e) {
                UserSettings.instance().setWindowSize(MainFrame.this.getSize());
            }
            
        });
        
        this.setJMenuBar(getMainMenuBar());

        this.addWindowListener(new WindowAdapter() {
            
            @Override
            public void windowIconified(final WindowEvent e) {
                MainFrame.this.windowIconified(e);
            }
            
            @Override
            public void windowClosed(final WindowEvent e) {
                MainFrame.this.windowClosing(e);
            }
            
            @Override
            public void windowClosing(WindowEvent e) {
                MainFrame.this.windowClosing(e);
            }
            
        });

        // 1. Init title and icon image
        if (this.model.isActive()) {
            this.setIconImage(ACTIVE_ICON);
            this.setTitle(
                    textBundle.textFor("Global.Title") + " - " + this.model.getSelectedProject() + textBundle.textFor("MainFrame.9") + FormatUtils.formatTime(this.model.getStart()) //$NON-NLS-1$ //$NON-NLS-2$
            );
        } else {
            this.setIconImage(NORMAL_ICON);
            this.setTitle(textBundle.textFor("Global.Title")); //$NON-NLS-1$
        }

        // 2. Set layout
        final double[][] size = { 
                {TableLayout.FILL }, // Columns
                {TableLayout.PREFERRED, TableLayout.PREFERRED, 15, TableLayout.FILL} // Rows
        };

        final TableLayout tableLayout = new TableLayout(size);
        this.setLayout(tableLayout);
        this.add(getToolBar(), "0, 0");
        this.add(getCurrentActivityPanel(), "0, 1");
        this.add(getReportPanel(), "0, 3");
        
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    private ReportPanel getReportPanel() {
        if (reportPanel == null) {
            reportPanel = new ReportPanel(this.model);
        }
        return reportPanel;
    }

    /**
     * This method initializes mainMenuBar.
     * @return javax.swing.JMenuBar
     */
    private JMenuBar getMainMenuBar() {
        if (mainMenuBar == null) {
            mainMenuBar = new JMenuBar();
            mainMenuBar.add(getFileMenu());
            mainMenuBar.add(getEditMenu());
            mainMenuBar.add(getHelpMenu());
        }
        return mainMenuBar;
    }

    /**
     * This method initializes toolBar.
     * @return javax.swing.JToolBar
     */
    public JToolBar getToolBar() {
        if (toolBar == null) {
            toolBar = new JToolBar();
            toolBar.setFloatable(false);
        }

        toolBar.add(new ManageProjectsAction(this, this.model));
        toolBar.add(new ExportExcelAction(this, this.model));
        toolBar.add(new AddActivityAction(this, this.model));
        toolBar.add(new JToolBar.Separator());
        toolBar.add(this.model.getEditStack().getUndoAction());
        toolBar.add(this.model.getEditStack().getRedoAction());

        return toolBar;
    }

    /**
     * This method initializes currentPanel.
     * @return javax.swing.JPanel
     */
    private JPanel getCurrentActivityPanel() {
        if (currentActivityPanel == null) {
            currentActivityPanel = new ActivityPanel(model);
        }
        return currentActivityPanel;
    }

    /**
     * This method initializes aboutMenu.
     * @return javax.swing.JMenu
     */
    private JMenu getHelpMenu() {
        if (helpMenu == null) {
            helpMenu = new JMenu(textBundle.textFor("MainFrame.HelpMenu.Title"));
            helpMenu.setMnemonic(textBundle.textFor("MainFrame.HelpMenu.Title").charAt(0));
            helpMenu.add(getAboutMenuItem());
        }
        return helpMenu;
    }

    /**
     * This method initializes fileMenu.
     * @return javax.swing.JMenu
     */
    private JMenu getFileMenu() {
        if (fileMenu == null) {
            fileMenu = new JMenu();
            fileMenu.setText(textBundle.textFor("MainFrame.FileMenu.Title")); //$NON-NLS-1$
            fileMenu.setMnemonic(textBundle.textFor("MainFrame.FileMenu.Title").charAt(0)); //$NON-NLS-1$

            fileMenu.add(getExportMenu());
            fileMenu.add(getImportMenu());
            fileMenu.addSeparator();

            fileMenu.add(getExitItem());
        }
        return fileMenu;
    }

    /**
     * This method initializes exitItem.
     * @return javax.swing.JMenuItem
     */
    private JMenuItem getImportItem() {
        if (importXmlItem == null) {
            final AbstractBaralgaAction exitAction = new ImportXmlAction(this, this.model);
            importXmlItem = new JMenuItem(exitAction);
            importXmlItem.setMnemonic(exitAction.getMnemonic());
            importXmlItem.setEnabled(model.isProjectAdminAllowed());
        }
        return importXmlItem;
    }

    /**
     * This method initializes editMenu.
     * @return javax.swing.JMenu
     */
    private JMenu getEditMenu() {
        if (editMenu == null) {
            editMenu = new JMenu();
            editMenu.setText(textBundle.textFor("MainFrame.EditMenu.Title")); //$NON-NLS-1$
            editMenu.setMnemonic(textBundle.textFor("MainFrame.EditMenu.Title").charAt(0)); //$NON-NLS-1$

            editMenu.add(this.model.getEditStack().getRedoAction());
            editMenu.add(this.model.getEditStack().getUndoAction());

            editMenu.addSeparator();

            editMenu.add(getEditProjectsMenuItem());
            editMenu.add(getAddActivityMenuItem());

            editMenu.addSeparator();
            editMenu.add(new JMenuItem(new SettingsAction(this, model)));
        }
        return editMenu;
    }

    /**
     * This method initializes addActivityMenuItem.
     * @return javax.swing.JMenuItem
     */
    private JMenuItem getAddActivityMenuItem() {
        if (addActivityMenuItem == null) {
            AbstractBaralgaAction addActivityAction = new AddActivityAction(this, this.model);
            addActivityMenuItem = new JMenuItem(addActivityAction);
            addActivityMenuItem.setMnemonic(addActivityAction.getMnemonic());
        }
        return addActivityMenuItem;
    }

    /**
     * This method initializes editProjectsMenuItem.
     * @return javax.swing.JMenuItem
     */
    private JMenuItem getEditProjectsMenuItem() {
        if (editProjectsMenuItem == null) {
            AbstractBaralgaAction manageProjectsAction = new ManageProjectsAction(this, this.model);
            editProjectsMenuItem = new JMenuItem(manageProjectsAction);
            editProjectsMenuItem.setMnemonic(manageProjectsAction.getMnemonic());
        }
        return editProjectsMenuItem;
    }

    /**
     * This method initializes aboutMenuItem.
     * @return javax.swing.JMenuItem
     */
    private JMenuItem getAboutMenuItem() {
        if (aboutMenuItem == null) {
            AbstractBaralgaAction aboutAction = new AboutAction(this);
            aboutMenuItem = new JMenuItem(aboutAction);
            aboutMenuItem.setMnemonic(aboutAction.getMnemonic());
        }
        return aboutMenuItem;
    }

    @Subscribe
    public void update(final Object eventObject) {
        if (!(eventObject instanceof BaralgaEvent)) {
            return;
        }

        final BaralgaEvent event = (BaralgaEvent) eventObject;
        
        switch (event.getType()) {

        case BaralgaEvent.PROJECT_ACTIVITY_STARTED:
            this.updateStart();
            break;

        case BaralgaEvent.PROJECT_ACTIVITY_STOPPED:
            this.updateStop();
            break;

        case BaralgaEvent.PROJECT_CHANGED:
            // If there is no active project leave everything as is
            if (model.isActive()) {
                this.updateTitle();
            }
            break;

        case BaralgaEvent.PROJECT_ADDED:
            break;

        case BaralgaEvent.PROJECT_REMOVED:
            break;

        case BaralgaEvent.START_CHANGED:
            this.updateTitle();
            break;
        }
    }

    /**
     * Executed on events that change the title.
     */
    private void updateTitle() {
        if (this.model.isActive()) {
            this.setTitle(textBundle.textFor("Global.Title") + " - " + this.model.getSelectedProject() + textBundle.textFor("MainFrame.9") + FormatUtils.formatTime(this.model.getStart())); //$NON-NLS-1$ //$NON-NLS-2$
        } else {
            this.setTitle(textBundle.textFor("Global.Title") + " " + textBundle.textFor("MainFrame.12") + FormatUtils.formatTime(this.model.getStop())); //$NON-NLS-1$
        }
    }

    /**
     * Executed on start event.
     */
    private void updateStart() {
        setIconImage(ACTIVE_ICON);
        updateTitle();
    }

    /**
     * Executed on stop event.
     */
    private void updateStop() {
        setIconImage(NORMAL_ICON);
        updateTitle();
    }

    /**
     * This method initializes exportiCalItem.
     * @return javax.swing.JMenuItem
     */
    private JMenuItem getExportiCalItem() {
        if (exportiCalItem == null) {
            final AbstractBaralgaAction iCalExportAction = new ICalExportAction(this, this.model);
            exportiCalItem = new JMenuItem(iCalExportAction);
            exportiCalItem.setMnemonic(iCalExportAction.getMnemonic());
        }
        return exportiCalItem;
    }

    /**
     * This method initializes exportExcelItem.
     * @return javax.swing.JMenuItem
     */
    private JMenuItem getExportExcelItem() {
        if (exportExcelItem == null) {
            final AbstractBaralgaAction excelExportAction = new ExportExcelAction(this, this.model);
            exportExcelItem = new JMenuItem(excelExportAction);
            exportExcelItem.setMnemonic(excelExportAction.getMnemonic());
        }
        return exportExcelItem;
    }

    /**
     * This method initializes exportCsvItem.
     * @return javax.swing.JMenuItem
     */
    private JMenuItem getExportCsvItem() {
        if (exportCsvItem == null) {
            final AbstractBaralgaAction csvExportAction = new ExportCsvAction(this, this.model);
            exportCsvItem = new JMenuItem(csvExportAction);
            exportCsvItem.setMnemonic(csvExportAction.getMnemonic());
        }
        return exportCsvItem;
    }
    
    /**
     * This method initializes exportDataItem.
     * @return javax.swing.JMenuItem
     */
    private JMenuItem getExportDataItem() {
    	if (exportXmlItem == null) {
    		final AbstractBaralgaAction dataExportAction = new ExportXmlAction(this, this.model);
    		exportXmlItem = new JMenuItem(dataExportAction);
    		exportXmlItem.setMnemonic(dataExportAction.getMnemonic());
    	}
    	return exportXmlItem;
    }

    /**
     * This method initializes exportMenu.
     * @return javax.swing.JMenu
     */
    private JMenu getExportMenu() {
        if (exportMenu == null) {
            exportMenu = new JMenu();
            exportMenu.setIcon(new ImageIcon(MainFrame.class.getResource("/icons/export-menu.png"))); //$NON-NLS-1$
            exportMenu.setText(textBundle.textFor("MainFrame.ExportMenu.Title")); //$NON-NLS-1$
            exportMenu.setMnemonic(textBundle.textFor("MainFrame.ExportMenu.Title").charAt(0)); //$NON-NLS-1$

            exportMenu.add(getExportExcelItem());
            exportMenu.add(getExportiCalItem());
            exportMenu.add(getExportCsvItem());
            exportMenu.add(getExportDataItem());
        }
        return exportMenu;
    }

    /**
     * This method initializes importMenu.
     * @return javax.swing.JMenu
     */
    private JMenu getImportMenu() {
        if (importMenu == null) {
            importMenu = new JMenu();
            importMenu.setIcon(new ImageIcon(MainFrame.class.getResource("/icons/import-menu.png"))); //$NON-NLS-1$
            importMenu.setText(textBundle.textFor("MainFrame.ImportMenu.Title")); //$NON-NLS-1$
            importMenu.setMnemonic(textBundle.textFor("MainFrame.ImportMenu.Title").charAt(0)); //$NON-NLS-1$
            importMenu.add(getImportItem());
        }
        return importMenu;
    }

    public void windowIconified(final java.awt.event.WindowEvent e) {
        if (BaralgaMain.getTray() != null) {
            this.setVisible(false);
        }
    }

    public void windowClosing(final java.awt.event.WindowEvent e) {
        if (BaralgaMain.getTray() != null) {
            this.setVisible(false);
        } else {
            boolean quit = true;

            if (model.isActive()) {
                final int dialogResult = JOptionPane.showConfirmDialog(
                        getOwner(), 
                        textBundle.textFor("ExitConfirmDialog.Message"), //$NON-NLS-1$
                        textBundle.textFor("ExitConfirmDialog.Title"), //$NON-NLS-1$
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.INFORMATION_MESSAGE
                );
                quit = JOptionPane.YES_OPTION == dialogResult;
            } 

            if (quit) {
                System.exit(0);
            }
        }
    }

    /**
     * This method initializes exitItem.
     * @return javax.swing.JMenuItem
     */
    private JMenuItem getExitItem() {
        if (exitItem == null) {
            final AbstractBaralgaAction exitAction = new ExitAction(this, this.model);
            exitItem = new JMenuItem(exitAction);
            exitItem.setMnemonic(exitAction.getMnemonic());
        }
        return exitItem;
    }

}
