package com.sakyvo.nestedpackfix.forge189.client;

import java.lang.reflect.Field;
import java.util.List;
import net.minecraft.client.gui.GuiResourcePackAvailable;
import net.minecraft.client.gui.GuiResourcePackList;
import net.minecraft.client.gui.GuiResourcePackSelected;
import net.minecraft.client.gui.GuiScreenResourcePacks;

@SuppressWarnings("rawtypes")
final class ResourcePackScreenBridge {
    private static final Field AVAILABLE_ENTRIES = findField(
        GuiScreenResourcePacks.class,
        "field_146966_g",
        "availableResourcePacks"
    );
    private static final Field SELECTED_ENTRIES = findField(
        GuiScreenResourcePacks.class,
        "field_146969_h",
        "selectedResourcePacks"
    );
    private static final Field AVAILABLE_LIST = findField(
        GuiScreenResourcePacks.class,
        "field_146970_i",
        "availableResourcePacksList"
    );
    private static final Field SELECTED_LIST = findField(
        GuiScreenResourcePacks.class,
        "field_146967_r",
        "selectedResourcePacksList"
    );
    private static final Field DISPLAY_ENTRIES = findField(
        GuiResourcePackList.class,
        "field_148204_l",
        "resourcePacks"
    );

    private ResourcePackScreenBridge() {
    }

    static Access open(GuiScreenResourcePacks screen) throws ReflectiveOperationException {
        List available = requireList(AVAILABLE_ENTRIES.get(screen), "Available entries");
        List selected = requireList(SELECTED_ENTRIES.get(screen), "Selected entries");
        Object availableWidget = AVAILABLE_LIST.get(screen);
        Object selectedWidget = SELECTED_LIST.get(screen);
        if (!(availableWidget instanceof GuiResourcePackAvailable)
            || !(selectedWidget instanceof GuiResourcePackSelected)) {
            throw new ReflectiveOperationException("Resource-pack list widgets are unavailable");
        }
        if (DISPLAY_ENTRIES.get(availableWidget) != available
            || DISPLAY_ENTRIES.get(selectedWidget) != selected) {
            throw new ReflectiveOperationException("Resource-pack list backing fields changed");
        }
        return new Access(available, selected, (GuiResourcePackList) availableWidget);
    }

    private static List requireList(Object value, String label)
        throws ReflectiveOperationException {
        if (!(value instanceof List)) {
            throw new ReflectiveOperationException(label + " field is not a List");
        }
        return (List) value;
    }

    private static Field findField(Class<?> owner, String... names) {
        for (String name : names) {
            try {
                Field field = owner.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new IllegalStateException("Missing field on " + owner.getName());
    }

    static final class Access {
        private final List availableEntries;
        private final List selectedEntries;
        private final GuiResourcePackList availableWidget;

        private Access(
            List availableEntries,
            List selectedEntries,
            GuiResourcePackList availableWidget
        ) {
            this.availableEntries = availableEntries;
            this.selectedEntries = selectedEntries;
            this.availableWidget = availableWidget;
        }

        List getAvailableEntries() {
            return this.availableEntries;
        }

        List getSelectedEntries() {
            return this.selectedEntries;
        }

        void useDisplayEntries(List displayEntries) throws IllegalAccessException {
            DISPLAY_ENTRIES.set(this.availableWidget, displayEntries);
        }
    }
}
