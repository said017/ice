package org.eclipse.ice.client.widgets;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.ice.client.common.ActionTree;
import org.eclipse.ice.client.widgets.viz.service.IPlot;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * A {@code PlotGridComposite} is designed to display a grid of drawn
 * {@link IPlot}s. It includes widgets to customize the grid-based layout of the
 * plots. The order of a plot in the grid is based on its add order, and the
 * same plot can be added to the grid more than once.
 * 
 * @author Jordan Deyton
 *
 */
public class PlotGridComposite extends Composite {

	// FIXME There is a bug where plots are added more than expected. This may
	// be an issue with duplicate selection events from the ICEResourceView.

	/**
	 * The {@code ToolBar} that contains widgets to update the grid layout and
	 * clear the grid.
	 */
	private final ToolBar toolBar;

	/**
	 * The grid of drawn plots.
	 */
	private final Composite gridComposite;
	/**
	 * The user-customizable layout for {@link #gridComposite}, the grid of
	 * drawn plots.
	 */
	private final GridLayout gridLayout;
	/**
	 * The number of rows to display in the grid.
	 */
	private int rows = 2;
	/**
	 * The number of columns to display in the grid.
	 */
	private int columns = 2;

	/**
	 * This is a button used to close the drawn plot over which the mouse is
	 * currently hovering. It should only be visible in the top-right corner of
	 * a drawn plot when the mouse cursor is over the plot.
	 */
	private Button closeButton;
	/**
	 * The listener responsible for showing and hiding the {@link #closeButton}.
	 */
	private final MouseTrackListener plotHoverListener;

	/**
	 * The list of currently drawn plots. A nested class, {@link DrawnPlot}, is
	 * used to contain meta-data about each drawn plot or cell in the grid.
	 */
	private final List<DrawnPlot> drawnPlots;

	/**
	 * The toolkit used to decorate SWT components. May be null.
	 */
	private final FormToolkit toolkit;

	// ---- Context Menu Management ---- //
	/**
	 * The index of the current plot on which the user clicked the mouse. This
	 * is set when the {@link #plotClickListener} is triggered.
	 */
	private int clickedPlotIndex;
	/**
	 * Whether or not the context {@code Menu} is stale. This should be true if
	 * a different plot was clicked since the last time the context {@code Menu}
	 * was populated.
	 */
	private boolean menuStale = true;
	/**
	 * The listener responsible for setting the {@link #clickedPlotIndex} when a
	 * plot is right-clicked.
	 */
	private final MouseListener plotClickListener;

	/**
	 * Handles the context menu that will be used by both this plot grid
	 * {@code Composite} and all drawn plots.
	 */
	private final MenuManager contextMenuManager;

	/**
	 * The {@code ActionTree} containing the plot categories and types.
	 */
	private ActionTree plotTypeTree;

	// --------------------------------- //

	/**
	 * The default constructor. Creates a {@code Composite} designed to display
	 * a grid of {@link IPlot} renderings.
	 * 
	 * @param parent
	 *            A widget that will be the parent of the new instance (cannot
	 *            be null).
	 * @param style
	 *            The style of widget to construct.
	 */
	public PlotGridComposite(Composite parent, int style) {
		this(parent, style, null);
	}

	/**
	 * The full constructor. Children of this {@code Composite} will be
	 * decorated with the specified {@code FormToolkit}.
	 * 
	 * @param parent
	 *            A widget that will be the parent of the new instance (cannot
	 *            be null).
	 * @param style
	 *            The style of widget to construct.
	 * @param toolkit
	 *            The toolkit used to decorate SWT components.
	 */
	public PlotGridComposite(Composite parent, int style, FormToolkit toolkit) {
		super(parent, style);

		// Set the form toolkit for decorating widgets in this Composite.
		this.toolkit = toolkit;
		adapt(this);

		// Initialize the list of drawn plots.
		drawnPlots = new ArrayList<DrawnPlot>();

		// Set up the ToolBar.
		toolBar = createToolBar(this);

		// Create the context Menu that will be used for this and all child plot
		// Composites.
		contextMenuManager = createContextMenu();

		// Set up the Composite containing the grid of plots.
		gridComposite = new Composite(this, SWT.NONE);
		adapt(gridComposite);
		gridLayout = new GridLayout();
		gridComposite.setLayout(gridLayout);

		// Lay out this Composite. The ToolBar should be across the top, while
		// the Composite containing the plot grid should grab all available
		// space.
		GridData gridData;
		setLayout(new GridLayout());
		gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
		toolBar.setLayoutData(gridData);
		gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridComposite.setLayoutData(gridData);

		// Create the listener that will be used to show/hide the close button.
		plotHoverListener = new MouseTrackAdapter() {
			@Override
			public void mouseEnter(MouseEvent e) {
				showCloseButton(e);
			}

			@Override
			public void mouseExit(MouseEvent e) {
				hideCloseButton(e);
			}
		};

		// Create a listener to get the right-clicked plot.
		plotClickListener = new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent e) {
				if (e.button == 3) {
					// Determine the index of the plot that was clicked. If a
					// plot could not be found, then use -1 for the index.
					Composite comp = (Composite) e.widget;
					int i;
					for (i = 0; i < drawnPlots.size(); i++) {
						DrawnPlot drawnPlot = drawnPlots.get(i);
						if (comp == drawnPlot.childComposite) {
							break;
						}
					}
					final int plotIndex = (i < drawnPlots.size() ? i : -1);

					// The Menu may need to be updated if a new plot was
					// clicked.
					if (plotIndex != clickedPlotIndex) {
						clickedPlotIndex = plotIndex;
						menuStale = true;
					}
				}

				return;
			}
		};

		return;
	}

	/**
	 * A convenience method to use the {@link #toolkit}, if available, to
	 * decorate a given {@code Composite}.
	 */
	private void adapt(Composite composite) {
		if (toolkit != null) {
			toolkit.adapt(composite);
		}
	}

	/**
	 * Creates a {@code ToolBar} for this {@code Composite}. It includes the
	 * following controls:
	 * <ol>
	 * <li>Grid rows and columns</li>
	 * <li>A button to clear plots from the grid</li>
	 * </ol>
	 * 
	 * @param parent
	 *            The parent {@code Composite} in which to draw the
	 *            {@code ToolBar}.
	 * @return The newly created {@code ToolBar}.
	 */
	private ToolBar createToolBar(Composite parent) {

		// Create and adapt the ToolBar first so that the default styles will be
		// passed down to the widgets created by the ToolBarManager.
		ToolBar toolBar = new ToolBar(parent, SWT.WRAP | SWT.FLAT
				| SWT.HORIZONTAL);
		adapt(toolBar);
		ToolBarManager toolBarManager = new ToolBarManager(toolBar);

		// Add a "Rows" label next to the row Spinner.
		LabelContribution rowLabel = new LabelContribution("rows.label");
		rowLabel.setText("Rows:");
		toolBarManager.add(rowLabel);

		// Add a Spinner for setting the grid rows to the ToolBarManager (this
		// requires a JFace ControlContribution).
		SpinnerContribution rowSpinner = new SpinnerContribution("rows.spinner");
		rowSpinner.setMinimum(1);
		rowSpinner.setMaximum(4);
		rowSpinner.setSelection(rows);
		rowSpinner.setIncrement(1);
		rowSpinner.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				rows = ((Spinner) e.widget).getSelection();
				refreshLayout();
			}
		});
		toolBarManager.add(rowSpinner);

		// Add a "Columns" label next to the row Spinner.
		LabelContribution columnLabel = new LabelContribution("columns.label");
		columnLabel.setText("Columns:");
		toolBarManager.add(columnLabel);

		// Add a Spinner for setting the grid columns to the ToolBarManager
		// (this requires a JFace ControlContribution).
		SpinnerContribution columnSpinner = new SpinnerContribution(
				"columns.spinner");
		columnSpinner.setMinimum(1);
		columnSpinner.setMaximum(4);
		columnSpinner.setSelection(columns);
		columnSpinner.setIncrement(1);
		columnSpinner.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				columns = ((Spinner) e.widget).getSelection();
				refreshLayout();
			}
		});
		toolBarManager.add(columnSpinner);

		// Add a separator between the spinners and the clear button.
		toolBarManager.add(new Separator());

		// Add a ToolBar button to clear the plots.
		toolBarManager.add(new Action("Clear") {
			@Override
			public void run() {
				clearPlots();
			}
		});

		// Apply the ToolBarManager changes to the ToolBar.
		toolBarManager.update(true);

		return toolBar;
	}

	/**
	 * Creates a context {@code Menu} for this {@code Composite} and all of the
	 * child plots. It includes the following controls by default:
	 * <ol>
	 * <li>Remove Plot</li>
	 * <li>Separator</li>
	 * <li>Set Plot Type</li>
	 * <li>Separator</li>
	 * <li>Any implementation-specific plot actions...</li>
	 * </ol>
	 * 
	 * @return The JFace {@code MenuManager} for the context {@code Menu}.
	 */
	private MenuManager createContextMenu() {
		MenuManager contextMenuManager = new MenuManager();
		contextMenuManager.createContextMenu(this);

		// Create an action to remove the moused-over or clicked plot rendering.
		final Action removeAction = new Action("Remove") {
			@Override
			public void run() {
				// Now we can remove the plot.
				removePlot(clickedPlotIndex);
				clickedPlotIndex = -1;
			}
		};

		// Create the root ActionTree for setting the plot category and type.
		plotTypeTree = new ActionTree("Set Plot Type");

		// When the Menu is about to be opened, we may need to refresh its
		// contents based on the currently selected plot.
		contextMenuManager.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(IMenuManager manager) {
				// If the context Menu needs to be updated, re-populate it.
				if (menuStale) {

					manager.removeAll();

					// Add all actions that require a drawn plot to be
					// "selected"/mouse-over/clicked.
					if (clickedPlotIndex != -1) {
						// Add the remove action.
						manager.add(removeAction);

						// Add any plot-specific properties.
						manager.add(new Separator());
						refreshContextMenu(drawnPlots.get(clickedPlotIndex));
					}

					menuStale = false;
				}

				return;
			}
		});

		return contextMenuManager;
	}

	/**
	 * Refreshes the {@link #contextMenuManager} based on the specified
	 * {@link DrawnPlot}. This includes adding the {@link #plotTypeTree} so that
	 * the plot category and type can be set and any {@code IPlot}
	 * -implementation-specific actions.
	 * 
	 * @param drawnPlot
	 *            A reference to the drawn plot for which the context
	 *            {@code Menu} should be populated.
	 */
	private void refreshContextMenu(DrawnPlot drawnPlot) {

		// Refresh the ActionTree of plot types and categories.
		plotTypeTree.removeAll();

		// A final reference to the DrawnPlot for use in Actions/Runnables.
		final DrawnPlot drawnPlotRef = drawnPlot;

		try {
			// Add an ActionTree for each category, and then add ActionTree leaf
			// nodes for each type.
			Map<String, String[]> plotTypes = drawnPlot.plot.getPlotTypes();
			for (Entry<String, String[]> entry : plotTypes.entrySet()) {
				String category = entry.getKey();
				String[] types = entry.getValue();

				if (category != null && types != null && types.length > 0) {
					// Create the category ActionTree.
					ActionTree categoryTree = new ActionTree(category);
					plotTypeTree.add(categoryTree);

					// Add all types to the category ActionTree. Each Action
					// should try to set the plot category and type of the drawn
					// plot.
					final String categoryRef = category;
					for (String type : types) {
						final String typeRef = type;
						categoryTree.add(new ActionTree(new Action(type) {
							@Override
							public void run() {
								try {
									drawnPlotRef.setPlotType(categoryRef,
											typeRef);
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						}));
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Finally, add the plot category/type ActionTree to the context Menu.
		contextMenuManager.add(plotTypeTree.getContributionItem());

		// TODO Add any implementation-specific actions.

		return;
	}

	/**
	 * Adds a plot to be drawn inside the plot grid. Note that the same plot can
	 * be added more than once.
	 * 
	 * @param plot
	 *            The plot to draw inside the grid.
	 * @return The index of the plot in the grid, or -1 if the plot could not be
	 *         drawn.
	 * @throws Exception
	 *             An exception is thrown if the {@link IPlot} implementation
	 *             cannot be rendered.
	 */
	public int addPlot(IPlot plot) throws Exception {
		int index = -1;

		// Proceed if the plot is not null and there is still space available in
		// the grid.
		if (plot != null && drawnPlots.size() < rows * columns) {

			// Try to get the available categories and plot types, then try to
			// plot the first available one.
			Map<String, String[]> plotTypes = plot.getPlotTypes();

			// Find the first category and plot type.
			String category = null;
			String type = null;
			for (Entry<String, String[]> entry : plotTypes.entrySet()) {
				category = entry.getKey();
				String[] types = entry.getValue();
				if (category != null && types != null) {
					for (int i = 0; i < types.length && type == null; i++) {
						type = types[i];
					}
					if (type != null) {
						break;
					}
				}
			}

			// If a category and type could be found, try to draw the plot in a
			// new cell in the grid.
			if (category != null && type != null) {
				// Create the Composite to contain the plot rendering.
				Composite composite = new Composite(gridComposite, SWT.BORDER);
				adapt(composite);

				// Try to create the plot rendering.
				DrawnPlot drawnPlot;
				try {
					drawnPlot = new DrawnPlot(plot, composite, category, type);
				} catch (Exception e) {
					// In case of failure, dispose the Composite that was just
					// created before sending the Exception on to the calling
					// class.
					composite.dispose();
					throw e;
				}

				// Add the drawn plot to the list.
				index = drawnPlots.size();
				drawnPlots.add(drawnPlot);

				// Hook the new drawn plot up to the close button.
				drawnPlot.childComposite
						.addMouseTrackListener(plotHoverListener);

				// Hook the new drawn plot up to the context Menu.
				Menu contextMenu = contextMenuManager.getMenu();
				drawnPlot.childComposite.setMenu(contextMenu);
				drawnPlot.childComposite.addMouseListener(plotClickListener);

				// Set up the layout and layout data for the new drawn plot.
				GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
				drawnPlot.composite.setLayoutData(gridData);
				drawnPlot.composite.setLayout(new FillLayout());

				// Since a new plot was added, refresh the grid layout.
				refreshLayout();
			}
		}

		return index;
	}

	/**
	 * Removes the drawn plot at the specified index in the grid.
	 * 
	 * @param index
	 *            The index of the drawn plot to remove. If invalid, nothing is
	 *            done.
	 */
	public void removePlot(int index) {

		if (index >= 0 && index < drawnPlots.size()) {
			// Pull the associated drawn plot from the list.
			DrawnPlot drawnPlot = drawnPlots.remove(index);

			// Dispose the plot's associated cell in the grid.
			drawnPlot.dispose();

			// Since a plot was removed, refresh the grid layout.
			refreshLayout();
		}

		return;
	}

	/**
	 * Removes all renderings of the specified plot from the grid.
	 * 
	 * @param plot
	 *            The plot whose renderings should be removed from the grid. If
	 *            invalid or not rendered, nothing is done.
	 */
	public void removeDrawnPlots(IPlot plot) {

		if (plot != null) {
			boolean compositeDisposed = false;

			Iterator<DrawnPlot> iterator = drawnPlots.iterator();
			while (iterator.hasNext()) {
				DrawnPlot drawnPlot = iterator.next();

				if (drawnPlot.plot == plot) {
					// Pull the drawn plot from the list, then dispose it.
					iterator.remove();
					drawnPlot.dispose();
					compositeDisposed = true;
				}
			}

			// If a Composite was removed, refresh the grid layout.
			if (compositeDisposed) {
				refreshLayout();
			}
		}

		return;
	}

	/**
	 * Removes all renderings from the grid.
	 */
	public void clearPlots() {

		if (!drawnPlots.isEmpty()) {

			// Dispose all of the cells in the grid.
			for (DrawnPlot drawnPlot : drawnPlots) {
				drawnPlot.dispose();
			}
			// Clear the list of drawn plots.
			drawnPlots.clear();

			// Since a Composite was removed, refresh the grid layout.
			refreshLayout();
		}

		return;
	}

	/**
	 * Shows the close button in the correct location if necessary.
	 * 
	 * @param e
	 *            The {@code MouseEvent} (usually mouse enter) that triggered
	 *            this call.
	 */
	private void showCloseButton(MouseEvent e) {

		if (closeButton == null || closeButton.isDisposed()) {
			// Get the Composite that triggered the event.
			Composite comp = (Composite) e.widget;

			// Determine the index of the plot that was moused over. If a plot
			// could not be found, then use -1 for the index.
			int i;
			for (i = 0; i < drawnPlots.size(); i++) {
				if (comp == drawnPlots.get(i).childComposite) {
					break;
				}
			}
			final int plotIndex = (i < drawnPlots.size() ? i : -1);

			// Set up the close button
			closeButton = new Button(comp, SWT.FLAT | SWT.CENTER);

			// Add the close button listener to the close button, too.
			closeButton.addMouseTrackListener(plotHoverListener);

			closeButton.setText("X");
			FontData[] smallFont = closeButton.getFont().getFontData();
			for (FontData fd : smallFont) {
				fd.setHeight(7);
			}
			closeButton.setFont(new Font(comp.getDisplay(), smallFont));
			closeButton.setToolTipText("Close plot");

			// Add a selection listener on it to dispose the composite
			closeButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					removePlot(plotIndex);
				}
			});

			closeButton.pack();

			// Set the location of the button to the upper right-hand corner
			closeButton.setLocation(
					comp.getBounds().width - closeButton.getBounds().width - 4,
					0);
		}

		return;
	}

	/**
	 * Hides (and disposes) the close button if necessary.
	 * 
	 * @param e
	 *            The {@code MouseEvent} (usually a mouse exit) that triggered
	 *            this call.
	 */
	private void hideCloseButton(MouseEvent e) {

		// If the cursor has left the canvas area and the close button, dispose
		// the button.
		if (closeButton != null && !closeButton.isDisposed()
				&& !closeButton.getBounds().contains(e.x, e.y)) {
			closeButton.dispose();
			closeButton = null;
		}

		return;
	}

	/**
	 * Refreshes the {@link #gridLayout} and drawn plot {@code GridData} based
	 * on the {@link #rows} and {@link #columns} and the number of drawn plots.
	 */
	private void refreshLayout() {

		// Remove all excess drawn plots.
		int limit = rows * columns;
		for (int i = drawnPlots.size() - 1; i >= limit; i--) {
			drawnPlots.remove(i).dispose();
		}

		// Reset all cells to only take up one grid cell.
		GridData gridData;
		for (DrawnPlot drawnPlot : drawnPlots) {
			gridData = (GridData) drawnPlot.composite.getLayoutData();
			gridData.verticalSpan = 1;
		}

		// Set the user-defined number of columns. The rows are handled already
		// because we've removed all excess plots.
		gridLayout.numColumns = columns;

		// If the last row has empty cells, then all of the cells directly above
		// those empty cells should grab the excess vertical space by updating
		// the verticalSpan property.
		int lastRowSize = drawnPlots.size() % columns;
		// We shouldn't do anything if the last row is full or if there is only
		// one row.
		if (lastRowSize > 0 && drawnPlots.size() > columns) {
			int lastIndex = drawnPlots.size() - 1 - lastRowSize;
			for (int i = 0; i < columns - lastRowSize; i++) {
				DrawnPlot drawnPlot = drawnPlots.get(lastIndex - i);
				gridData = (GridData) drawnPlot.composite.getLayoutData();
				gridData.verticalSpan = 2;
			}
		}

		// Refresh the grid layout.
		gridComposite.layout();

		return;
	}

	/**
	 * This nested class contains meta-data about each drawn plot.
	 * 
	 * @author Jordan Deyton
	 *
	 */
	private class DrawnPlot {

		/**
		 * The {@link IPlot} instance/implementation that is drawn.
		 */
		public final IPlot plot;
		/**
		 * A {@code Composite} that contains the rendering of the plot. This is
		 * passed, as the parent, to
		 * {@link IPlot#draw(String, String, Composite)}.
		 */
		public final Composite composite;
		/**
		 * The child {@code Composite} in which the plot is rendered. This is
		 * returned from {@link IPlot#draw(String, String, Composite)}.
		 */
		public final Composite childComposite;

		/**
		 * The current category.
		 */
		private String category;
		/**
		 * The current type.
		 */
		private String type;

		/**
		 * Creates a new {@code DrawnPlot} to manage the plot's meta-data and
		 * renders the plot in the specified parent {@code Composite}.
		 * 
		 * @param plot
		 *            The {@code IPlot} to be drawn.
		 * @param composite
		 *            The {@code Composite} in which to draw the plot.
		 * @param category
		 *            The initial category for the plot.
		 * @param type
		 *            The initial type for the plot category.
		 * @throws Exception
		 *             An exception may be thrown by the {@code IPlot}
		 *             implementation if the plot could not be drawn.
		 */
		public DrawnPlot(IPlot plot, Composite composite, String category,
				String type) throws Exception {
			this.plot = plot;
			this.composite = composite;

			// Render the plot.
			childComposite = plot.draw(category, type, composite);

			return;
		}

		/**
		 * Sets the current category and type of the drawn plot.
		 * 
		 * @param category
		 *            The new category for the plot.
		 * @param type
		 *            The new type for the plot category.
		 * @throws Exception
		 *             An exception may be thrown by the {@code IPlot}
		 *             implementation if the plot could not be drawn. An
		 *             exception may also be thrown if the {@code IPlot}
		 *             implementation returns a new child {@code Composite} when
		 *             drawing the plot.
		 */
		public void setPlotType(String category, String type) throws Exception {
			// Only process new, non-null plot category and type.
			if (category != null
					&& type != null
					&& (!category.equals(this.category) || !type
							.equals(this.type))) {

				// Try to draw the plot with the new category and type. Note
				// that the parent and child Composites should remain the same.
				if (childComposite != plot.draw(category, type, composite)) {
					throw new Exception("IPlot error: "
							+ "The plot was drawn in a new Composite.");
				}
			}

			return;
		}

		/**
		 * A convenience method to dispose of resources used by this drawn plot.
		 */
		public void dispose() {
			childComposite.setMenu(null);
			composite.dispose();
			category = null;
			type = null;
		}
	}
}
