package fi.dy.masa.malilib.gui.config;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.lwjgl.input.Keyboard;
import net.minecraft.client.gui.GuiScreen;
import fi.dy.masa.malilib.MaLiLibConfigs;
import fi.dy.masa.malilib.config.ConfigManagerImpl;
import fi.dy.masa.malilib.config.option.ConfigInfo;
import fi.dy.masa.malilib.gui.BaseListScreen;
import fi.dy.masa.malilib.gui.BaseScreen;
import fi.dy.masa.malilib.gui.tab.ScreenTab;
import fi.dy.masa.malilib.gui.util.GuiUtils;
import fi.dy.masa.malilib.gui.widget.DropDownListWidget;
import fi.dy.masa.malilib.gui.widget.button.GenericButton;
import fi.dy.masa.malilib.gui.widget.button.KeyBindConfigButton;
import fi.dy.masa.malilib.gui.widget.list.ConfigOptionListWidget;
import fi.dy.masa.malilib.listener.EventListener;
import fi.dy.masa.malilib.registry.Registry;
import fi.dy.masa.malilib.util.data.ModInfo;

public class BaseConfigScreen extends BaseListScreen<ConfigOptionListWidget<? extends ConfigInfo>> implements ConfigScreen, KeybindEditingScreen
{
    protected final ModInfo modInfo;
    protected final DropDownListWidget<ModInfo> modSwitcherDropdown;
    @Nullable protected EventListener configSaveListener;
    @Nullable protected KeyBindConfigButton activeKeyBindButton;
    protected boolean addModSwitcherDropdown = true;
    protected int configElementsWidth = 120;

    public BaseConfigScreen(ModInfo modInfo, @Nullable GuiScreen parent,
                            List<? extends ScreenTab> configTabs,
                            @Nullable ConfigTab defaultTab,
                            String titleKey, Object... args)
    {
        super(10, 46, 20, 62, modInfo.getModId(), configTabs, defaultTab);

        this.modInfo = modInfo;
        this.shouldRestoreScrollbarPosition = MaLiLibConfigs.Generic.REMEMBER_CONFIG_TAB_SCROLL_POSITIONS.getBooleanValue();
        this.modSwitcherDropdown = new DropDownListWidget<>(16, 10, Registry.CONFIG_SCREEN.getAllModsWithConfigScreens(), ModInfo::getModName);
        this.modSwitcherDropdown.setSelectedEntry(modInfo);
        this.modSwitcherDropdown.setSelectionListener(this::switchConfigScreenToMod);

        this.setTitle(titleKey, args);
        this.setParent(parent);
    }

    @Override
    protected void onScreenClosed()
    {
        if (((ConfigManagerImpl) Registry.CONFIG_MANAGER).saveIfDirty())
        {
            this.onSettingsChanged();
        }

        super.onScreenClosed();
    }

    @Override
    protected void reAddActiveWidgets()
    {
        super.reAddActiveWidgets();

        if (this.addModSwitcherDropdown)
        {
            this.addWidget(this.modSwitcherDropdown);
        }
    }

    @Override
    protected void updateWidgetPositions()
    {
        super.updateWidgetPositions();

        int x = this.x + this.screenWidth - 16;
        this.modSwitcherDropdown.setRight(x);
        this.modSwitcherDropdown.setY(this.y + 2);
    }

    @Override
    public boolean onKeyTyped(int keyCode, int scanCode, int modifiers)
    {
        if (this.activeKeyBindButton != null)
        {
            this.activeKeyBindButton.onKeyTyped(keyCode, scanCode, modifiers);
            return true;
        }
        else
        {
            if (this.getListWidget().onKeyTyped(keyCode, scanCode, modifiers))
            {
                return true;
            }

            if (keyCode == Keyboard.KEY_ESCAPE && this.getParent() != GuiUtils.getCurrentScreen())
            {
                BaseScreen.openScreen(this.getParent());
                return true;
            }

            return super.onKeyTyped(keyCode, scanCode, modifiers);
        }
    }

    @Override
    public boolean onMouseClicked(int mouseX, int mouseY, int mouseButton)
    {
        if (super.onMouseClicked(mouseX, mouseY, mouseButton))
        {
            return true;
        }

        // When clicking on not-a-button, clear the selection
        if (this.activeKeyBindButton != null && mouseButton == 0)
        {
            this.setActiveKeyBindButton(null);
            return true;
        }

        return false;
    }

    public void setConfigSaveListener(@Nullable EventListener configSaveListener)
    {
        this.configSaveListener = configSaveListener;
    }

    public int getDefaultConfigElementWidth()
    {
        ScreenTab tab = this.getCurrentTab();

        if (tab instanceof ConfigTab)
        {
            return ((ConfigTab) tab).getConfigWidgetsWidth();
        }

        return this.configElementsWidth;
    }

    /**
     * Sets the requested config elements width for this screen.
     * Use -1 to indicate automatic/default width decided by the widgets.
     */
    public BaseConfigScreen setConfigElementsWidth(int configElementsWidth)
    {
        this.configElementsWidth = configElementsWidth;
        return this;
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
                    openScreen(screen);
                }
            }
        }
    }

    @Override
    public List<? extends ConfigInfo> getConfigs()
    {
        ScreenTab tab = this.getCurrentTab();

        if (tab instanceof ConfigTab)
        {
            return ((ConfigTab) tab).getConfigs();
        }

        return Collections.emptyList();
    }

    @Override
    public ModInfo getModInfo()
    {
        return this.modInfo;
    }

    @Override
    public void switchToTab(ScreenTab tab)
    {
        this.saveScrollBarPositionForCurrentTab();

        super.switchToTab(tab);

        this.restoreScrollBarPositionForCurrentTab();
        this.reCreateConfigWidgets();
    }

    public void reCreateConfigWidgets()
    {
        for (GenericButton tabButton : this.tabButtons)
        {
            tabButton.updateButtonState();
        }

        ConfigOptionListWidget<?> listWidget = this.getListWidget();

        if (listWidget != null)
        {
            listWidget.refreshEntries();
        }
    }

    protected void onSettingsChanged()
    {
        Registry.HOTKEY_MANAGER.updateUsedKeys();

        if (this.configSaveListener != null)
        {
            this.configSaveListener.onEvent();
        }
    }

    @Override
    protected ConfigOptionListWidget<? extends ConfigInfo> createListWidget()
    {
        ConfigWidgetContext ctx = new ConfigWidgetContext(this::getListWidget, this, 0);
        ConfigOptionListWidget<? extends ConfigInfo> widget = ConfigOptionListWidget.createWithExpandedGroups(
                this::getDefaultConfigElementWidth, this.modInfo, this::getConfigs, ctx);

        widget.addConfigSearchBarWidget(this);

        return widget;
    }

    @Override
    protected void clearElements()
    {
        super.clearElements();
        this.clearOptions();
    }

    @Override
    public void clearOptions()
    {
        this.setActiveKeyBindButton(null);
    }

    @Override
    public void setActiveKeyBindButton(@Nullable KeyBindConfigButton button)
    {
        if (this.activeKeyBindButton != null)
        {
            this.activeKeyBindButton.onClearSelection();
        }

        this.activeKeyBindButton = button;

        if (this.activeKeyBindButton != null)
        {
            this.activeKeyBindButton.onSelected();
        }
    }
}
