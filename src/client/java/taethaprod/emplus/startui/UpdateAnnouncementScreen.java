package taethaprod.emplus.startui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class UpdateAnnouncementScreen extends Screen {
	private final String titleText;
	private final List<String> lines;
	private int panelX;
	private int panelY;
	private int panelWidth;
	private int panelHeight;

	public UpdateAnnouncementScreen(String title, List<String> lines) {
		super(Text.literal(title == null || title.isBlank() ? "Update" : title));
		this.titleText = title == null ? "" : title;
		this.lines = lines == null ? List.of() : new ArrayList<>(lines);
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
		addDrawableChild(ButtonWidget.builder(Text.translatable("ui.emplus.update.close"), b -> close())
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

		String header = titleText == null || titleText.isBlank() ? "Update" : titleText;
		context.drawTextWithShadow(this.textRenderer, header, panelX + 12, panelY + 10, 0xFFFFFF);

		int textX = panelX + 12;
		int textY = panelY + 28;
		for (OrderedText line : wrapLines()) {
			context.drawTextWithShadow(this.textRenderer, line, textX, textY, 0xCCCCCC);
			textY += this.textRenderer.fontHeight + 2;
		}

		super.render(context, mouseX, mouseY, delta);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	private List<OrderedText> wrapLines() {
		List<OrderedText> wrapped = new ArrayList<>();
		if (lines.isEmpty()) {
			return wrapped;
		}
		int width = panelWidth - 24;
		for (String line : lines) {
			Text text = Text.literal(line == null ? "" : line);
			wrapped.addAll(this.textRenderer.wrapLines(text, width));
		}
		return wrapped;
	}
}
