package taethaprod.emplus.onboarding;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FactionSelectScreen extends Screen {
	private static final Identifier FACTION_A = new Identifier("emplus", "faction_a");
	private static final Identifier FACTION_B = new Identifier("emplus", "faction_b");
	private final List<FactionInfo> factions = new ArrayList<>();
	private final Set<String> expanded = new HashSet<>();
	private static final ItemStack DEFAULT_ICON = new ItemStack(Items.APPLE);

	public FactionSelectScreen() {
		super(Text.literal("Выбор фракции"));
		factions.add(mockFactionA());
		factions.add(mockFactionB());
	}

	@Override
	protected void init() {
		this.clearChildren();
		int buttonWidth = 140;
		int buttonHeight = 20;
		int y = this.height - 40;

		int columnWidth = this.width / 2;
		int leftX = 30;
		int rightX = this.width / 2 + 10;
		int leftBtnX = leftX + (columnWidth - buttonWidth) / 2 - 30;
		int rightBtnX = rightX + (columnWidth - buttonWidth) / 2 - 30;

		this.addDrawableChild(ButtonWidget.builder(Text.literal("Примкнуть к Умбралис"), b -> chooseFaction(FACTION_A.toString()))
				.dimensions(leftBtnX, y, buttonWidth, buttonHeight)
				.build());
		this.addDrawableChild(ButtonWidget.builder(Text.literal("Примкнуть к Королевству"), b -> chooseFaction(FACTION_B.toString()))
				.dimensions(rightBtnX, y, buttonWidth, buttonHeight)
				.build());
	}

	private void chooseFaction(String factionId) {
		var buf = PacketByteBufs.create();
		buf.writeString(factionId);
		ClientPlayNetworking.send(OnboardingNetworking.ONBOARDING_DONE, buf);
		if (this.client != null) {
			this.client.setScreen(null);
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		this.renderBackground(context);
		int centerX = this.width / 2;
		int y = 30;
		context.drawCenteredTextWithShadow(this.textRenderer, "Поздравляем с прибытием! (плейсхолдер лора)", centerX, y, 0xFFFFFF);
		y += 20;
		context.drawCenteredTextWithShadow(this.textRenderer, "Выберите фракцию и изучите её классы:", centerX, y, 0xAAAAAA);

		int leftX = 30;
		int rightX = this.width / 2 + 10;
		int topY = 80;

		int dividerX = this.width / 2;
		context.fill(dividerX - 1, topY - 10, dividerX + 1, this.height - 60, 0xFF555555);

		renderFaction(context, factions.get(0), leftX, topY, 0);
		renderFaction(context, factions.get(1), rightX, topY, 1);

		super.render(context, mouseX, mouseY, delta);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0) {
			for (int f = 0; f < factions.size(); f++) {
				FactionInfo faction = factions.get(f);
				for (int i = 0; i < faction.classes.size(); i++) {
					ClassInfo cls = faction.classes.get(i);
					if (cls.lastBounds != null && cls.lastBounds.isPointInside(mouseX, mouseY)) {
						toggleClass(f, i);
						return true;
					}
				}
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	private void toggleClass(int factionIndex, int classIndex) {
		String key = factionIndex + ":" + classIndex;
		if (!expanded.add(key)) {
			expanded.remove(key);
		}
	}

	private void renderFaction(DrawContext ctx, FactionInfo faction, int x, int yStart, int factionIndex) {
		int titleY = yStart;
		ctx.drawCenteredTextWithShadow(this.textRenderer, faction.title, x + 160, titleY, 0xFFFFFF);
		titleY += 15;
		ctx.drawCenteredTextWithShadow(this.textRenderer, faction.description, x + 160, titleY, 0xAAAAAA);
		titleY += 25;

		int y = titleY;
		for (int i = 0; i < faction.classes.size(); i++) {
			ClassInfo cls = faction.classes.get(i);
			int iconX = x;
			int nameX = x + 20;
			int arrowX = nameX + 140;
			int rowHeight = 18;

			ctx.drawItem(cls.icon, iconX, y);
			ctx.drawTextWithShadow(this.textRenderer, cls.name, nameX, y + 4, 0xFFFFFF);
			ctx.drawTextWithShadow(this.textRenderer, "->", arrowX, y + 4, 0xAAAAAA);

			boolean isExpanded = expanded.contains(factionIndex + ":" + i);
			int blockTop = y;
			if (isExpanded) {
				int descY = y + rowHeight + 2;
				ctx.drawTextWrapped(this.textRenderer, Text.literal(cls.description), x, descY, 260, 0xCCCCCC);
				y = descY + this.textRenderer.getWrappedLinesHeight(cls.description, 260) + 8;
			} else {
				y += rowHeight + 8;
			}
			cls.lastBounds = new Rect(nameX, blockTop, y, x + 260);
		}
	}

	private FactionInfo mockFactionA() {
		FactionInfo info = new FactionInfo();
		info.title = "Умбралис";
		info.description = "Плейсхолдер описания фракции.";
		info.classes.add(new ClassInfo("Воин", "Описание.", DEFAULT_ICON));
		info.classes.add(new ClassInfo("Лучник", "Описание.", DEFAULT_ICON));
		info.classes.add(new ClassInfo("Разбойник", "Описание.", DEFAULT_ICON));
		info.classes.add(new ClassInfo("Маг", "Описание.", DEFAULT_ICON));
		info.classes.add(new ClassInfo("Рыцарь смерти", "Описание.", DEFAULT_ICON));
		info.classes.add(new ClassInfo("Ведьмак", "Описание.", DEFAULT_ICON));
		return info;
	}

	private FactionInfo mockFactionB() {
		FactionInfo info = new FactionInfo();
		info.title = "Королевство";
		info.description = "Плейсхолдер описания фракции.";
		info.classes.add(new ClassInfo("Воин", "Описание.", DEFAULT_ICON));
		info.classes.add(new ClassInfo("Лучник", "Описание.", DEFAULT_ICON));
		info.classes.add(new ClassInfo("Разбойник", "Описание.", DEFAULT_ICON));
		info.classes.add(new ClassInfo("Паладин", "Описание.", DEFAULT_ICON));
		info.classes.add(new ClassInfo("Жрец", "Описание.", DEFAULT_ICON));
		info.classes.add(new ClassInfo("Стрелок", "Описание.", DEFAULT_ICON));
		return info;
	}

	private static class FactionInfo {
		String title;
		String description;
		List<ClassInfo> classes = new ArrayList<>();
	}

	private static class ClassInfo {
		String name;
		String description;
		ItemStack icon;
		Rect lastBounds;

		ClassInfo(String name, String description, ItemStack icon) {
			this.name = name;
			this.description = description;
			this.icon = icon;
		}
	}

	private static class Rect {
		final double x1;
		final double y1;
		final double y2;
		final double x2;

		Rect(double x1, double yStart, double y2, double x2) {
			this.x1 = x1;
			this.y1 = yStart;
			this.y2 = y2;
			this.x2 = x2;
		}

		boolean isPointInside(double x, double y) {
			return x >= x1 && x <= x2 && y >= y1 && y <= y2;
		}
	}
}
