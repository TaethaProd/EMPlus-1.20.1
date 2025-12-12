package taethaprod.emplus.onboarding;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import taethaprod.emplus.startui.FactionClassCommandsConfig;
import taethaprod.emplus.startui.FactionClassCommandsConfigManager;

import java.util.ArrayList;
import java.util.List;

public class FactionAClassSelectScreen extends Screen {
	private final Identifier factionId;
	private final List<FactionClassCommandsConfig.ClassEntry> entries;

	public FactionAClassSelectScreen(Identifier factionId) {
		super(Text.literal("Классы фракции А"));
		this.factionId = factionId;
		this.entries = prepareEntries(FactionClassCommandsConfigManager.get().factionA);
	}

	@Override
	protected void init() {
		this.clearChildren();
		int count = 6;
		int spacing = 8;
		int totalWidth = this.width - 40 - spacing * (count - 1);
		int buttonWidth = Math.max(90, totalWidth / count);
		int buttonHeight = 20;
		int y = this.height - 40;
		int startX = (this.width - (buttonWidth * count + spacing * (count - 1))) / 2;

		for (int i = 0; i < count; i++) {
			FactionClassCommandsConfig.ClassEntry entry = entries.get(i);
			String label = entry.label != null && !entry.label.isBlank() ? entry.label : "Класс A" + (i + 1);
			String classId = entry.id != null && !entry.id.isBlank() ? entry.id : "class_a" + (i + 1);
			int x = startX + i * (buttonWidth + spacing);
			this.addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> chooseClass(classId))
					.dimensions(x, y, buttonWidth, buttonHeight)
					.build());
		}
	}

	private void chooseClass(String classId) {
		var buf = PacketByteBufs.create();
		buf.writeString(factionId.toString());
		buf.writeString(classId == null ? "" : classId);
		ClientPlayNetworking.send(OnboardingNetworking.ONBOARDING_DONE, buf);
		if (this.client != null) {
			this.client.setScreen(null);
		}
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		this.renderBackground(context);
		super.render(context, mouseX, mouseY, delta);
	}

	private List<FactionClassCommandsConfig.ClassEntry> prepareEntries(List<FactionClassCommandsConfig.ClassEntry> source) {
		List<FactionClassCommandsConfig.ClassEntry> list = new ArrayList<>();
		if (source != null) {
			list.addAll(source);
		}
		while (list.size() < 6) {
			FactionClassCommandsConfig.ClassEntry entry = new FactionClassCommandsConfig.ClassEntry();
			entry.id = "class_a" + (list.size() + 1);
			entry.label = "Класс A" + (list.size() + 1);
			list.add(entry);
		}
		if (list.size() > 6) {
			return list.subList(0, 6);
		}
		return list;
	}
}
