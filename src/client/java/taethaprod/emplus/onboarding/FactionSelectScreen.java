package taethaprod.emplus.onboarding;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class FactionSelectScreen extends Screen {
	private static final Identifier FACTION_A = new Identifier("emplus", "faction_a");
	private static final Identifier FACTION_B = new Identifier("emplus", "faction_b");

	public FactionSelectScreen() {
		super(Text.translatable("ui.emplus.faction.title"));
	}

	@Override
	protected void init() {
		this.clearChildren();
		int buttonWidth = 180;
		int buttonHeight = 20;
		int centerX = this.width / 2;
		int y = this.height / 2 - 12;

		this.addDrawableChild(ButtonWidget.builder(Text.translatable("ui.emplus.faction.join_umbralis"), b -> openClassScreen(FACTION_A))
				.dimensions(centerX - buttonWidth / 2, y, buttonWidth, buttonHeight)
				.build());
		this.addDrawableChild(ButtonWidget.builder(Text.translatable("ui.emplus.faction.join_kingdom"), b -> openClassScreen(FACTION_B))
				.dimensions(centerX - buttonWidth / 2, y + buttonHeight + 8, buttonWidth, buttonHeight)
				.build());
	}

	private void openClassScreen(Identifier factionId) {
		if (this.client == null) return;
		if (FACTION_A.equals(factionId)) {
			this.client.setScreen(new FactionAClassSelectScreen(factionId));
		} else if (FACTION_B.equals(factionId)) {
			this.client.setScreen(new FactionBClassSelectScreen(factionId));
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		this.renderBackground(context);
		super.render(context, mouseX, mouseY, delta);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}
}
