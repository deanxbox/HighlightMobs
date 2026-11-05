package dev.deanm.highlightmobs.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;

/** Per-variant selection for one mob type (wolf, cat, frog, ...). */
final class HighlightMobsVariantScreen extends Screen {
	private static final int LIST_TOP = 64;
	private static final int BUTTON_HEIGHT = 20;
	private static final int ROW_SPACING = 24;

	private final Screen parent;
	private final EntityType<?> type;
	private final List<String> variants;
	private final List<Button> variantWidgets = new ArrayList<>();
	private final List<Object[]> iconSpots = new ArrayList<>();
	private int page;
	private int pageCount = 1;
	private int contentLeft;
	private int contentWidth;

	HighlightMobsVariantScreen(Screen parent, EntityType<?> type) {
		super(Component.translatable("highlightmobs.variants.title", type.getDescription()));
		this.parent = parent;
		this.type = type;
		this.variants = MobVariants.options(type);
	}

	@Override
	protected void init() {
		this.contentWidth = Math.min(308, this.width - 20);
		this.contentLeft = (this.width - this.contentWidth) / 2;
		int half = (this.contentWidth - 4) / 2;

		Button all = Button.builder(Component.translatable("highlightmobs.variants.select_all"), button -> {
			HighlightMobsClient.CONFIG.selectAllVariants(this.type);
			this.refreshVariantWidgets();
		}).bounds(this.contentLeft, 38, half, BUTTON_HEIGHT).build();
		Button none = Button.builder(Component.translatable("highlightmobs.variants.clear"), button -> {
			HighlightMobsClient.CONFIG.deselectType(this.type);
			this.refreshVariantWidgets();
		}).bounds(this.contentLeft + half + 4, 38, this.contentWidth - half - 4, BUTTON_HEIGHT).build();
		this.addRenderableWidget(all);
		this.addRenderableWidget(none);

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
			.bounds(this.contentLeft, this.height - 30, this.contentWidth, BUTTON_HEIGHT).build());
		this.refreshVariantWidgets();
	}

	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(this.parent);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		int titleWidth = this.font.width(this.title);
		graphics.centeredText(this.font, this.title, this.width / 2 + 10, 15, -1);
		MobIcons.draw(graphics, this.type, this.width / 2 - titleWidth / 2 - 10, 11, 16);

		if (this.variants.isEmpty()) {
			graphics.centeredText(this.font, Component.translatable("highlightmobs.variants.needs_world"), this.width / 2, LIST_TOP + 6, -6250336);
		} else {
			for (Object[] spot : this.iconSpots) {
				MobIcons.draw(graphics, this.type, (String) spot[0], (int) spot[1], (int) spot[2], 16);
			}
			graphics.centeredText(this.font,
				Component.translatable("highlightmobs.settings.page", this.page + 1, this.pageCount), this.width / 2, this.height - 52, -1);
		}
	}

	private void refreshVariantWidgets() {
		for (Button widget : this.variantWidgets) {
			this.removeWidget(widget);
		}
		this.variantWidgets.clear();
		this.iconSpots.clear();
		if (this.variants.isEmpty()) {
			return;
		}

		int navigationY = this.height - 56;
		int rows = Math.max(1, (navigationY - LIST_TOP + 4) / ROW_SPACING);
		int pageSize = rows * 2;
		this.pageCount = Math.max(1, (this.variants.size() + pageSize - 1) / pageSize);
		this.page = Math.min(this.page, this.pageCount - 1);

		int buttonWidth = (this.contentWidth - 4) / 2;
		int start = this.page * pageSize;
		int end = Math.min(start + pageSize, this.variants.size());
		for (int index = start; index < end; index++) {
			String variant = this.variants.get(index);
			int pageIndex = index - start;
			int x = this.contentLeft + (pageIndex % 2) * (buttonWidth + 4);
			int y = LIST_TOP + (pageIndex / 2) * ROW_SPACING;
			Button button = Button.builder(this.variantMessage(variant), pressed -> {
				HighlightMobsClient.CONFIG.toggleVariant(this.type, variant, this.variants);
				this.refreshVariantWidgets();
			}).bounds(x, y, buttonWidth, BUTTON_HEIGHT).tooltip(Tooltip.create(Component.literal(variant))).build();
			this.variantWidgets.add(this.addRenderableWidget(button));
			this.iconSpots.add(new Object[] {variant, x + 2, y + 2});
		}

		Button previous = Button.builder(Component.translatable("highlightmobs.settings.previous"), button -> {
			this.page--;
			this.refreshVariantWidgets();
		}).bounds(this.contentLeft, navigationY, 90, BUTTON_HEIGHT).build();
		previous.active = this.page > 0;
		this.variantWidgets.add(this.addRenderableWidget(previous));

		Button next = Button.builder(Component.translatable("highlightmobs.settings.next"), button -> {
			this.page++;
			this.refreshVariantWidgets();
		}).bounds(this.contentLeft + this.contentWidth - 90, navigationY, 90, BUTTON_HEIGHT).build();
		next.active = this.page + 1 < this.pageCount;
		this.variantWidgets.add(this.addRenderableWidget(next));
	}

	private Component variantMessage(String variant) {
		return MobIcons.fit(this.font, Component.translatable(
			HighlightMobsClient.CONFIG.isVariantSelected(this.type, variant) ? "highlightmobs.settings.selected" : "highlightmobs.settings.unselected",
			MobVariants.label(variant)
		), (this.contentWidth - 4) / 2);
	}
}
