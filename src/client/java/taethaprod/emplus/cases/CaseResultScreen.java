package taethaprod.emplus.cases;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

public class CaseResultScreen extends Screen {
	private static final int ICON_SIZE = 24;

	private final CaseEntry entry;
	private final ItemStack stack;
	private int panelX;
	private int panelY;
	private int panelWidth;
	private int panelHeight;

	public CaseResultScreen(CaseEntry entry) {
		super(Text.translatable("ui.emplus.case.result_title"));
		this.entry = entry;
		this.stack = resolveStack(entry);
	}

	@Override
	protected void init() {
		this.clearChildren();
		panelWidth = Math.min(360, this.width - 40);
		panelHeight = Math.min(200, this.height - 60);
		panelX = (this.width - panelWidth) / 2;
		panelY = (this.height - panelHeight) / 2;

		int buttonWidth = 80;
		int buttonHeight = 20;
		int buttonX = panelX + panelWidth - buttonWidth - 12;
		int buttonY = panelY + panelHeight - buttonHeight - 10;
		addDrawableChild(ButtonWidget.builder(Text.translatable("ui.emplus.case.close"), b -> close())
				.dimensions(buttonX, buttonY, buttonWidth, buttonHeight)
				.build());
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		this.renderBackground(context);

		int background = 0xEE1E1E1E;
		int border = 0xFF4A4A4A;
		context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, background);
		context.fill(panelX, panelY, panelX + panelWidth, panelY + 1, border);
		context.fill(panelX, panelY + panelHeight - 1, panelX + panelWidth, panelY + panelHeight, border);
		context.fill(panelX, panelY, panelX + 1, panelY + panelHeight, border);
		context.fill(panelX + panelWidth - 1, panelY, panelX + panelWidth, panelY + panelHeight, border);

		context.drawTextWithShadow(this.textRenderer, this.title, panelX + 12, panelY + 10, 0xFFFFFF);

		int iconX = panelX + (panelWidth - ICON_SIZE) / 2;
		int iconY = panelY + 30;
		if (!stack.isEmpty()) {
			context.drawItem(stack, iconX + (ICON_SIZE - 16) / 2, iconY + (ICON_SIZE - 16) / 2);
		}

		String name = resolveName(entry, stack);
		if (name != null && !name.isBlank()) {
			int textWidth = this.textRenderer.getWidth(name);
			context.drawTextWithShadow(this.textRenderer, name, panelX + (panelWidth - textWidth) / 2, iconY + ICON_SIZE + 6, 0xFFFFFF);
		}

		int textY = iconY + ICON_SIZE + 20;
		for (OrderedText line : wrapDescriptionLines()) {
			context.drawTextWithShadow(this.textRenderer, line, panelX + 12, textY, 0xCCCCCC);
			textY += this.textRenderer.fontHeight + 2;
		}

		super.render(context, mouseX, mouseY, delta);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	private List<OrderedText> wrapDescriptionLines() {
		List<OrderedText> wrapped = new ArrayList<>();
		if (entry == null || entry.description == null || entry.description.isBlank()) {
			return wrapped;
		}
		int width = panelWidth - 24;
		for (String part : entry.description.replace("\r\n", "\n").split("\n")) {
			if (part == null || part.isBlank()) {
				continue;
			}
			wrapped.addAll(this.textRenderer.wrapLines(Text.literal(part).formatted(Formatting.GRAY), width));
		}
		return wrapped;
	}

	private static ItemStack resolveStack(CaseEntry entry) {
		if (entry == null) {
			return ItemStack.EMPTY;
		}
		String iconId = entry.icon != null && !entry.icon.isBlank() ? entry.icon : entry.item;
		if (iconId == null || iconId.isBlank()) {
			return entry.isCommand() ? new ItemStack(Items.COMMAND_BLOCK) : ItemStack.EMPTY;
		}
		Identifier id = Identifier.tryParse(iconId);
		if (id == null) {
			return ItemStack.EMPTY;
		}
		var item = Registries.ITEM.get(id);
		if (item == Items.AIR) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = new ItemStack(item);
		int count = entry.count;
		if (count > 1) {
			stack.setCount(Math.min(count, stack.getMaxCount()));
		}
		return stack;
	}

	private static String resolveName(CaseEntry entry, ItemStack stack) {
		if (entry != null && entry.name != null && !entry.name.isBlank()) {
			return entry.name;
		}
		if (stack != null && !stack.isEmpty()) {
			return stack.getName().getString();
		}
		if (entry != null && entry.item != null && !entry.item.isBlank()) {
			return entry.item;
		}
		return entry != null && entry.id != null ? entry.id : "";
	}
}
