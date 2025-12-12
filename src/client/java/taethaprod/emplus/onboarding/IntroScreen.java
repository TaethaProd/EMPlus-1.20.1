package taethaprod.emplus.onboarding;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class IntroScreen extends Screen {
	public IntroScreen() {
		super(Text.literal("Welcome"));
	}

	@Override
	protected void init() {
		int buttonWidth = 120;
		int buttonHeight = 20;
		int x = (this.width - buttonWidth) / 2;
		int y = this.height - 40;

		this.addDrawableChild(ButtonWidget.builder(Text.literal("Далее"), b -> {
			if (this.client != null) {
				this.client.setScreen(new FactionSelectScreen());
			}
		}).dimensions(x, y, buttonWidth, buttonHeight).build());
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		this.renderBackground(context);
		int centerX = this.width / 2;
		int y = 60;
		context.drawCenteredTextWithShadow(this.textRenderer, "Добро пожаловать (плейсхолдер)", centerX, y, 0xFFFFFF);
		y += 20;
		context.drawCenteredTextWithShadow(this.textRenderer, "Небольшое описание перед выбором фракции", centerX, y, 0xAAAAAA);
		super.render(context, mouseX, mouseY, delta);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}
}
