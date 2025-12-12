package taethaprod.emplus.startui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StartUiScreen extends Screen {
	private CheckboxWidget dontShowCheckbox;
	private final List<KeyBinding> importantKeys = new ArrayList<>();
	private final Map<KeyBinding, ButtonWidget> keyButtons = new HashMap<>();
	private KeyBinding listening = null;

	private static final Map<String, String> LABEL_OVERRIDES = Map.ofEntries(
			Map.entry("key.ftbchunks.map", "Карта"),
			Map.entry("key.ftbquests.quests", "Квесты"),
			Map.entry("key.levelz.openskillscreen", "Уровни"),
			Map.entry("key.puffish_skills.open", "Древо навыков"),
			Map.entry("key.simplyskills.ability1", "Навык 1"),
			Map.entry("key.simplyskills.ability2", "Навык 2"),
			Map.entry("keybindings.spell_engine.spell_hotbar_1", "Заклинание 1"),
			Map.entry("keybindings.spell_engine.spell_hotbar_2", "Заклинание 2"),
			Map.entry("keybindings.spell_engine.spell_hotbar_3", "Заклинание 3"),
			Map.entry("keybindings.spell_engine.spell_hotbar_4", "Заклинание 4"),
			Map.entry("keybinds.combatroll.roll", "Кувырок"),
			Map.entry("key.emotecraft.fastchoose", "Эмоции"),
			Map.entry("key.carry.desc", "CarryOn"),
			Map.entry("key.voice_chat", "Настройки войса"),
			Map.entry("key.push_to_talk", "Говорить в войс")
	);

	private static final String[] DEFAULT_KEY_IDS = new String[] {
			"key.ftbchunks.map",
			"key.ftbquests.quests",
			"key.levelz.openskillscreen",
			"key.puffish_skills.open",
			"key.simplyskills.ability1",
			"key.simplyskills.ability2",
			"keybindings.spell_engine.spell_hotbar_1",
			"keybindings.spell_engine.spell_hotbar_2",
			"keybindings.spell_engine.spell_hotbar_3",
			"keybindings.spell_engine.spell_hotbar_4",
			"keybinds.combatroll.roll",
			"key.emotecraft.fastchoose",
			"key.carry.desc",
			"key.voice_chat",
			"key.push_to_talk"
	};

	// layout constants
	private static final int COLUMNS = 3;
	private static final int LABEL_WIDTH = 120;
	private static final int BUTTON_WIDTH = 110;
	private static final int LABEL_BUTTON_GAP = 8;
	private static final int COLUMN_SPACING = 20;
	private static final int ROW_HEIGHT = 26;
	private static final int GRID_START_Y = 120;

	public StartUiScreen() {
		super(Text.literal("EMPlus — приветствие"));
	}

	@Override
	protected void init() {
		importantKeys.clear();
		keyButtons.clear();

		for (String id : DEFAULT_KEY_IDS) {
			KeyBinding kb = findKeyBinding(id);
			if (kb != null) {
				importantKeys.add(kb);
			}
		}

		int buttonWidth = 120;
		int buttonHeight = 20;
		int spacing = 10;
		int centerX = this.width / 2;
		int y = this.height - 50;

		dontShowCheckbox = new CheckboxWidget(centerX - buttonWidth - spacing, y, buttonWidth + 40, buttonHeight, Text.literal("Больше не показывать"), false);
		this.addDrawableChild(dontShowCheckbox);

		this.addDrawableChild(ButtonWidget.builder(Text.literal("Далее"), b -> {
			if (dontShowCheckbox.isChecked()) {
				StartUiConfigManager.get().showStartScreen = false;
				StartUiConfigManager.save();
			}
			if (this.client != null) {
				this.client.setScreen(new TitleScreen());
			}
		}).dimensions(centerX + spacing, y, buttonWidth, buttonHeight).build());

		// Create buttons for keybindings (grid).
		int totalColumnWidth = LABEL_WIDTH + LABEL_BUTTON_GAP + BUTTON_WIDTH;
		int totalWidth = COLUMNS * totalColumnWidth + (COLUMNS - 1) * COLUMN_SPACING;
		int startX = centerX - totalWidth / 2;

		for (int i = 0; i < importantKeys.size(); i++) {
			KeyBinding binding = importantKeys.get(i);
			int col = i % COLUMNS;
			int row = i / COLUMNS;
			int baseX = startX + col * (totalColumnWidth + COLUMN_SPACING);
			int buttonX = baseX + LABEL_WIDTH + LABEL_BUTTON_GAP;
			int buttonY = GRID_START_Y + row * ROW_HEIGHT;

			ButtonWidget keyBtn = ButtonWidget.builder(Text.literal(getBindingLabel(binding)), btn -> {
						listening = binding;
						btn.setMessage(Text.literal("> нажмите клавишу <"));
					})
					.dimensions(buttonX, buttonY, BUTTON_WIDTH, 20)
					.build();
			this.addDrawableChild(keyBtn);
			keyButtons.put(binding, keyBtn);
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		this.renderBackground(context);
		int centerX = this.width / 2;
		int y = 40;
		context.drawCenteredTextWithShadow(this.textRenderer, "Добро пожаловать в EVERLONG", centerX, y, 0xFFFFFF);
		y += 20;
		context.drawCenteredTextWithShadow(this.textRenderer, "Для начала давай настроим вещи, без которых играть не получится:", centerX, y, 0xAAAAAA);
		y += 30;

		int totalColumnWidth = LABEL_WIDTH + LABEL_BUTTON_GAP + BUTTON_WIDTH;
		int totalWidth = COLUMNS * totalColumnWidth + (COLUMNS - 1) * COLUMN_SPACING;
		int startX = centerX - totalWidth / 2;

		for (int i = 0; i < importantKeys.size(); i++) {
			KeyBinding binding = importantKeys.get(i);
			int col = i % COLUMNS;
			int row = i / COLUMNS;
			int labelX = startX + col * (totalColumnWidth + COLUMN_SPACING);
			int labelY = GRID_START_Y + row * ROW_HEIGHT + 6; // slight vertical alignment
			String line = LABEL_OVERRIDES.getOrDefault(binding.getTranslationKey(), Text.translatable(binding.getTranslationKey()).getString());
			context.drawTextWithShadow(this.textRenderer, line, labelX, labelY, 0xCCCCCC);
		}

		context.drawCenteredTextWithShadow(this.textRenderer, "Изменить можно в Настройки -> Управление", centerX, this.height - 80, 0x777777);
		super.render(context, mouseX, mouseY, delta);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (listening != null) {
			setBinding(listening, InputUtil.fromKeyCode(keyCode, scanCode));
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (listening != null && button >= 0) {
			setBinding(listening, InputUtil.Type.MOUSE.createFromCode(button));
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	private void setBinding(KeyBinding binding, InputUtil.Key key) {
		binding.setBoundKey(key);
		KeyBinding.updateKeysByCode();
		updateButtonLabel(binding);
		listening = null;
	}

	private String getBindingLabel(KeyBinding binding) {
		return binding.getBoundKeyLocalizedText().getString();
	}

	private void updateButtonLabel(KeyBinding binding) {
		ButtonWidget btn = keyButtons.get(binding);
		if (btn != null) {
			btn.setMessage(Text.literal(getBindingLabel(binding)));
		}
	}

	private KeyBinding findKeyBinding(String translationKey) {
		if (this.client == null || this.client.options == null) {
			return null;
		}
		for (KeyBinding kb : this.client.options.allKeys) {
			if (kb != null && translationKey.equals(kb.getTranslationKey())) {
				return kb;
			}
		}
		return null;
	}
}
