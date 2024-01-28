package malilib.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import malilib.gui.tab.ScreenTab;
import malilib.gui.tab.TabbedScreenState;
import malilib.gui.widget.CyclableContainerWidget;
import malilib.gui.widget.DropDownListWidget;
import malilib.gui.widget.button.GenericButton;
import malilib.registry.Registry;
import malilib.util.data.ModInfo;

public abstract class BaseTabbedScreen extends BaseScreen
{
    protected static final Map<String, TabbedScreenState> CURRENT_STATE = new HashMap<>();
    protected static final Object2IntOpenHashMap<ScreenTab> SCROLLBAR_POSITIONS = new Object2IntOpenHashMap<>();

    protected final List<? extends ScreenTab> screenTabs;
    protected final List<GenericButton> tabButtons = new ArrayList<>();
    protected final String screenId;
    @Nullable protected final ScreenTab defaultTab;
    @Nullable protected DropDownListWidget<ModInfo> modConfigScreenSwitcherDropdown;
    @Nullable protected CyclableContainerWidget tabButtonContainerWidget;
    protected boolean shouldCreateTabButtons;
    protected boolean shouldRestoreScrollbarPosition;
    protected int tabButtonContainerWidgetX = 10;
    protected int tabButtonContainerWidgetY = 22;

    public BaseTabbedScreen(String screenId,
                            List<? extends ScreenTab> screenTabs,
                            @Nullable ScreenTab defaultTab)
    {
        this.screenId = screenId;
        this.defaultTab = defaultTab;
        this.screenTabs = screenTabs;

        this.addPreScreenCloseListener(this::saveScreenState);
    }

    @Override
    protected void initScreen()
    {
        ScreenTab tab = this.getCurrentTab();

        if (tab != null && tab.canUseCurrentScreen(this) == false)
        {
            tab.createAndOpenScreen();
            return;
        }

        this.restoreScrollBarPositionForCurrentTab();

        super.initScreen();

        if (this.shouldCreateTabButtons())
        {
            this.createTabButtonContainerWidget();
        }
    }

    protected void saveScreenState()
    {
        if (this.tabButtonContainerWidget != null)
        {
            this.getTabState().visibleTabsStartIndex = this.tabButtonContainerWidget.getStartIndex();
        }

        this.saveScrollBarPositionForCurrentTab();
    }

    @Override
    protected void reAddActiveWidgets()
    {
        super.reAddActiveWidgets();

        if (this.modConfigScreenSwitcherDropdown != null)
        {
            this.addWidget(this.modConfigScreenSwitcherDropdown);
        }
    }

    @Override
    protected void updateWidgetPositions()
    {
        super.updateWidgetPositions();

        int x = this.x + this.tabButtonContainerWidgetX;
        int y = this.y + this.tabButtonContainerWidgetY;

        if (this.tabButtonContainerWidget != null)
        {
            this.tabButtonContainerWidget.setPosition(x, y);
        }

        if (this.modConfigScreenSwitcherDropdown != null)
        {
            this.modConfigScreenSwitcherDropdown.setRight(this.getRight() - 16);
            this.modConfigScreenSwitcherDropdown.setY(this.y + 2);
        }
    }

    public TabbedScreenState getTabState()
    {
        return getTabState(this.screenId);
    }

    @Nullable
    public ScreenTab getCurrentTab()
    {
        TabbedScreenState state = this.getTabState();

        if (state.currentTab == null)
        {
            state.currentTab = this.defaultTab;
        }

        return state.currentTab;
    }

    public void setCurrentTab(ScreenTab tab)
    {
        setCurrentTab(this.screenId, tab);
    }

    public void switchToTab(ScreenTab tab)
    {
        this.setCurrentTab(tab);
    }

    public void saveScrollBarPositionForCurrentTab()
    {
        if (this.shouldRestoreScrollBarPosition())
        {
            ScreenTab tab = this.getCurrentTab();
            int position = this.getCurrentScrollbarPosition();

            if (tab != null && position >= 0)
            {
                setScrollBarPosition(tab, position);
            }
        }
    }

    public void restoreScrollBarPositionForCurrentTab()
    {
        if (this.shouldRestoreScrollBarPosition())
        {
            ScreenTab tab = this.getCurrentTab();

            if (tab != null)
            {
                this.setCurrentScrollbarPosition(getScrollBarPosition(tab));
            }
        }
    }

    protected int getCurrentScrollbarPosition()
    {
        return 0;
    }

    protected void setCurrentScrollbarPosition(int position)
    {
    }

    protected void createSwitchModConfigScreenDropDown(ModInfo modInfo)
    {
        this.modConfigScreenSwitcherDropdown = new DropDownListWidget<>(16, 10, Registry.CONFIG_SCREEN.getAllModsWithConfigScreens(), ModInfo::getModName);
        this.modConfigScreenSwitcherDropdown.setSelectedEntry(modInfo);
        this.modConfigScreenSwitcherDropdown.setSelectionListener(this::switchConfigScreenToMod);
    }

    protected void switchConfigScreenToMod(@Nullable ModInfo modInfo)
    {
        if (modInfo != null)
        {
            Supplier<BaseScreen> factory = Registry.CONFIG_SCREEN.getConfigScreenFactoryFor(modInfo);

            if (factory != null)
            {
                BaseScreen screen = factory.get();

                if (screen != null)
                {
                    // Copy over the current parent screen, so that for example opening the
                    // config menu via Mod Menu would still also return to the Mod Menu screen.
                    screen.setParent(this.getParent());
                    openScreen(screen);
                }
            }
        }
    }

    protected boolean shouldRestoreScrollBarPosition()
    {
        return this.shouldRestoreScrollbarPosition;
    }

    protected boolean shouldCreateTabButtons()
    {
        return this.shouldCreateTabButtons;
    }

    protected List<? extends ScreenTab> getScreenTabs()
    {
        return this.screenTabs;
    }

    protected int getTabButtonContainerWidgetWidth()
    {
        return this.screenWidth - 20;
    }

    protected void createTabButtonContainerWidget()
    {
        // This stores the value when resizing the window
        if (this.tabButtonContainerWidget != null)
        {
            this.getTabState().visibleTabsStartIndex = this.tabButtonContainerWidget.getStartIndex();
        }

        int x = this.x + this.tabButtonContainerWidgetX;
        int y = this.y + this.tabButtonContainerWidgetY;
        int width = this.getTabButtonContainerWidgetWidth();

        this.tabButtonContainerWidget = new CyclableContainerWidget(width, 20, this.createTabButtons());
        this.tabButtonContainerWidget.setPosition(x, y);
        this.tabButtonContainerWidget.setStartIndex(this.getTabState().visibleTabsStartIndex);
        this.addWidget(this.tabButtonContainerWidget);
    }

    protected List<GenericButton> createTabButtons()
    {
        this.tabButtons.clear();

        for (ScreenTab tab : this.getScreenTabs())
        {
            this.tabButtons.add(this.createTabButton(tab));
        }

        return this.tabButtons;
    }

    protected GenericButton createTabButton(final ScreenTab tab)
    {
        // The CyclableContainerWidget will re-position all the fitting buttons
        GenericButton button = GenericButton.create(tab.getDisplayName());
        button.setActionListener(tab.getButtonActionListener(this));
        button.setEnabledStatusSupplier(() -> this.getCurrentTab() != tab);

        String hoverText = tab.getHoverText();

        if (hoverText != null)
        {
            button.translateAndAddHoverString(hoverText);
        }

        return button;
    }

    public static TabbedScreenState getTabState(String screenId)
    {
        return CURRENT_STATE.computeIfAbsent(screenId, (id) -> new TabbedScreenState(null));
    }

    @Nullable
    public static ScreenTab getCurrentTab(String screenId)
    {
        return getTabState(screenId).currentTab;
    }

    public static void setCurrentTab(String screenId, ScreenTab tab)
    {
        getTabState(screenId).currentTab = tab;
    }

    public static int getScrollBarPosition(ScreenTab tab)
    {
        return SCROLLBAR_POSITIONS.getInt(tab);
    }

    public static void setScrollBarPosition(ScreenTab tab, int position)
    {
        SCROLLBAR_POSITIONS.put(tab, position);
    }
}
