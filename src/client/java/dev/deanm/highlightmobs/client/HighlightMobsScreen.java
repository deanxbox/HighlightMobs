package dev.deanm.highlightmobs.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import org.jspecify.annotations.Nullable;

public final class HighlightMobsScreen extends Screen {
	private static final int LIST_TOP = 105;
	private static final int BUTTON_HEIGHT = 20;
	private static final int ROW_SPACING = 24;

	private final @Nullable Screen parent;
	private final List<Button> entityWidgets = new ArrayList<>();
	private String search = "";
	private int page;
	private int pageCount = 1;
	private int contentLeft;
	private int contentWidth;
	private EditBox searchBox;
	private boolean showSelectedOnly;
	private List<EntityType<?>> visibleEntityTypes = List.of();

	public HighlightMobsScreen(@Nullable Screen parent) {
		super(Component.translatable("highlightmobs.settings.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		this.contentWidth = Math.min(308, this.width - 20);
		this.contentLeft = (this.width - this.contentWidth) / 2;
		int clearSearchWidth = Math.min(90, this.contentWidth / 3);
		int searchWidth = this.contentWidth - clearSearchWidth - 4;

		this.searchBox = new EditBox(
			this.font,
			this.contentLeft,
			38,
			searchWidth,
			BUTTON_HEIGHT,
			Component.translatable("highlightmobs.settings.search")
		);
		this.searchBox.setHint(Component.translatable("highlightmobs.settings.search").setStyle(EditBox.SEARCH_HINT_STYLE));
		this.searchBox.setValue(this.search);
		this.searchBox.setResponder(value -> {
			this.search = value;
			this.page = 0;
			this.refreshEntityWidgets();
		});
		this.addRenderableWidget(this.searchBox);

		this.addRenderableWidget(Button.builder(
			Component.translatable("highlightmobs.settings.clear_search"),
			button -> this.searchBox.setValue("")
		).bounds(this.contentLeft + searchWidth + 4, 38, clearSearchWidth, BUTTON_HEIGHT).build());

		this.addRenderableWidget(Button.builder(
			this.enabledMessage(),
			button -> {
				HighlightMobsClient.CONFIG.enabled = !HighlightMobsClient.CONFIG.enabled;
				HighlightMobsClient.CONFIG.save();
				button.setMessage(this.enabledMessage());
			}
		).bounds(this.contentLeft, 64, this.contentWidth, BUTTON_HEIGHT).build());

		int footerY = this.height - 30;
		int footerButtonWidth = (this.contentWidth - 8) / 3;
		this.addRenderableWidget(Button.builder(
			Component.translatable("highlightmobs.settings.clear_selected"),
			button -> {
				HighlightMobsClient.CONFIG.highlightedEntityTypes.clear();
				HighlightMobsClient.CONFIG.save();
				this.refreshEntityWidgets();
			}
		).bounds(this.contentLeft, footerY, footerButtonWidth, BUTTON_HEIGHT).build());
		this.addRenderableWidget(Button.builder(
			this.showSelectedMessage(),
			button -> {
				this.showSelectedOnly = !this.showSelectedOnly;
				button.setMessage(this.showSelectedMessage());
				this.page = 0;
				this.refreshEntityWidgets();
			}
		).bounds(this.contentLeft + footerButtonWidth + 4, footerY, footerButtonWidth, BUTTON_HEIGHT).build());
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
			.bounds(this.contentLeft + (footerButtonWidth + 4) * 2, footerY, this.contentWidth - (footerButtonWidth + 4) * 2, BUTTON_HEIGHT)
			.build());

		this.refreshEntityWidgets();
	}

	@Override
	protected void setInitialFocus() {
		this.setInitialFocus(this.searchBox);
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(this.parent);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		graphics.centeredText(this.font, this.title, this.width / 2, 15, -1);

		Component section = Component.translatable("highlightmobs.settings.entities");
		Component count = Component.translatable(
			"highlightmobs.settings.selected_count",
			HighlightMobsClient.CONFIG.highlightedEntityTypes.size()
		);
		graphics.text(this.font, section, this.contentLeft, 91, -1);
		graphics.text(this.font, count, this.contentLeft + this.contentWidth - this.font.width(count), 91, -6250336);

		if (this.visibleEntityTypes.isEmpty()) {
			graphics.centeredText(this.font, Component.translatable("highlightmobs.settings.no_results"), this.width / 2, LIST_TOP + 6, -6250336);
		}

		graphics.centeredText(
			this.font,
			Component.translatable("highlightmobs.settings.page", this.page + 1, this.pageCount),
			this.width / 2,
			this.height - 52,
			-1
		);
	}

	private void refreshEntityWidgets() {
		for (Button widget : this.entityWidgets) {
			this.removeWidget(widget);
		}
		this.entityWidgets.clear();

		this.visibleEntityTypes = this.filteredEntityTypes();
		List<EntityType<?>> matches = this.visibleEntityTypes;
		int navigationY = this.height - 56;
		int rows = Math.max(1, (navigationY - LIST_TOP + 4) / ROW_SPACING);
		int pageSize = rows * 2;
		this.pageCount = Math.max(1, (matches.size() + pageSize - 1) / pageSize);
		this.page = Math.min(this.page, this.pageCount - 1);

		int entityButtonWidth = (this.contentWidth - 4) / 2;
		int start = this.page * pageSize;
		int end = Math.min(start + pageSize, matches.size());
		for (int index = start; index < end; index++) {
			EntityType<?> type = matches.get(index);
			int pageIndex = index - start;
			int x = this.contentLeft + (pageIndex % 2) * (entityButtonWidth + 4);
			int y = LIST_TOP + (pageIndex / 2) * ROW_SPACING;
			Button button = Button.builder(this.entityMessage(type), pressed -> {
				HighlightMobsClient.CONFIG.toggle(type);
				pressed.setMessage(this.entityMessage(type));
			}).bounds(x, y, entityButtonWidth, BUTTON_HEIGHT)
				.tooltip(Tooltip.create(Component.literal(EntityType.getKey(type).toString())))
				.build();
			this.entityWidgets.add(this.addRenderableWidget(button));
		}

		Button previous = Button.builder(Component.translatable("highlightmobs.settings.previous"), button -> {
			this.page--;
			this.refreshEntityWidgets();
		}).bounds(this.contentLeft, navigationY, 90, BUTTON_HEIGHT).build();
		previous.active = this.page > 0;
		this.entityWidgets.add(this.addRenderableWidget(previous));

		Button next = Button.builder(Component.translatable("highlightmobs.settings.next"), button -> {
			this.page++;
			this.refreshEntityWidgets();
		}).bounds(this.contentLeft + this.contentWidth - 90, navigationY, 90, BUTTON_HEIGHT).build();
		next.active = this.page + 1 < this.pageCount;
		this.entityWidgets.add(this.addRenderableWidget(next));
	}

	private List<EntityType<?>> filteredEntityTypes() {
		String query = this.search.trim().toLowerCase(Locale.ROOT);
		return BuiltInRegistries.ENTITY_TYPE.stream()
			.filter(type -> !this.showSelectedOnly || HighlightMobsClient.CONFIG.isSelected(type))
			.filter(type -> {
				String id = EntityType.getKey(type).toString();
				String name = type.getDescription().getString();
				return query.isEmpty()
					|| id.toLowerCase(Locale.ROOT).contains(query)
					|| name.toLowerCase(Locale.ROOT).contains(query);
			})
			.sorted(Comparator.comparing((EntityType<?> type) -> type.getDescription().getString(), String.CASE_INSENSITIVE_ORDER)
				.thenComparing(type -> EntityType.getKey(type).toString()))
			.toList();
	}

	private Component showSelectedMessage() {
		return Component.translatable(
			"highlightmobs.settings.show_selected",
			Component.translatable(this.showSelectedOnly ? "highlightmobs.settings.on" : "highlightmobs.settings.off")
		);
	}

	private Component enabledMessage() {
		return Component.translatable(
			"highlightmobs.settings.enabled",
			Component.translatable(HighlightMobsClient.CONFIG.enabled ? "highlightmobs.settings.on" : "highlightmobs.settings.off")
		);
	}

	private Component entityMessage(EntityType<?> type) {
		return Component.translatable(
			HighlightMobsClient.CONFIG.isSelected(type) ? "highlightmobs.settings.selected" : "highlightmobs.settings.unselected",
			type.getDescription()
		);
	}
}
