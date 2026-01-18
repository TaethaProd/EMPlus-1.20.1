package taethaprod.emplus.cases;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CaseScreen extends Screen {
	private static final int ROULETTE_HEIGHT = 70;
	private static final int ROULETTE_SLOT_SIZE = 24;
	private static final int ROULETTE_SLOT_GAP = 6;
	private static final int ROULETTE_SLOTS = 9;
	private static final int CARD_WIDTH = 44;
	private static final int CARD_HEIGHT = 44;
	private static final int CARD_GAP = 8;
	private static final int CARD_COLUMNS = 5;
	private static final int CARD_MAX_ITEMS = 20;
	private static final int FOOTER_HEIGHT = 40;
	private static final int SPIN_DURATION_TICKS = 100;
	private static final int ROULETTE_GAP = 18;
	private static final int HOVER_BRIGHTNESS = 18;

	private final String caseId;
	private final List<CaseEntry> entries;
	private final int cost;
	private final List<CaseCard> cards = new ArrayList<>();
	private List<CaseEntry> rouletteSequence = new ArrayList<>();
	private float spinPos = 0.0f;
	private float spinTarget = 0.0f;
	private int spinTicks = 0;
	private boolean spinning = false;
	private boolean awaitingResult = false;
	private boolean resultShown = false;
	private ButtonWidget openButton;
	private ButtonWidget backButton;

	public CaseScreen(String caseId, List<CaseEntry> entries, int cost) {
		super(Text.literal(caseId == null ? "Case" : caseId));
		this.caseId = caseId == null ? "" : caseId;
		this.entries = entries == null ? List.of() : new ArrayList<>(entries);
		this.cost = Math.max(0, cost);
	}

	public boolean matchesCase(String caseId) {
		return this.caseId.equals(caseId);
	}

	@Override
	protected void init() {
		this.clearChildren();
		setupFooterButtons();
		buildCards();
		buildIdleRoulette();
		int pending = CaseClientState.consumePendingResult(caseId);
		if (pending >= 0) {
			onResult(pending);
		}
	}

	private void setupFooterButtons() {
		int buttonWidth = 120;
		int buttonHeight = 20;
		int gap = 12;
		int totalWidth = buttonWidth * 2 + gap;
		int startX = (this.width - totalWidth) / 2;
		int y = getListBottom() + (FOOTER_HEIGHT - buttonHeight) / 2;
		backButton = addDrawableChild(ButtonWidget.builder(Text.translatable("ui.emplus.case.back"), b -> close())
				.dimensions(startX, y, buttonWidth, buttonHeight)
				.build());
		openButton = addDrawableChild(ButtonWidget.builder(buildOpenLabel(), b -> requestSpin())
				.dimensions(startX + buttonWidth + gap, y, buttonWidth, buttonHeight)
				.build());
		updateButtonState();
	}

	private void updateButtonState() {
		if (openButton != null) {
			openButton.active = !spinning && !awaitingResult && !entries.isEmpty();
		}
		if (backButton != null) {
			backButton.active = !spinning && !awaitingResult;
		}
	}

	private Text buildOpenLabel() {
		return Text.translatable("ui.emplus.case.open_for", cost);
	}

	private void requestSpin() {
		if (awaitingResult || spinning) {
			return;
		}
		if (openButton != null) {
			openButton.visible = false;
		}
		awaitingResult = true;
		updateButtonState();
		CaseClientNetworking.sendSpinRequest(caseId);
	}

	public void onResult(int entryIndex) {
		CaseEntry entry = findEntry(entryIndex);
		if (entry == null) {
			awaitingResult = false;
			if (openButton != null) {
				openButton.visible = true;
			}
			updateButtonState();
			return;
		}
		awaitingResult = false;
		startSpin(entry);
		updateButtonState();
	}

	private CaseEntry findEntry(int entryIndex) {
		if (entryIndex < 0 || entryIndex >= entries.size()) {
			return null;
		}
		return entries.get(entryIndex);
	}

	private void startSpin(CaseEntry entry) {
		resultShown = false;
		buildRouletteSequence(entry);
		spinPos = 0.0f;
		spinTarget = Math.max(0.0f, rouletteSequence.size() - 5);
		spinTicks = 0;
		spinning = true;
	}

	private void buildRouletteSequence(CaseEntry entry) {
		rouletteSequence = new ArrayList<>();
		if (entries.isEmpty()) {
			return;
		}
		int size = Math.max(30, entries.size() * 3);
		Random random = new Random();
		for (int i = 0; i < size; i++) {
			rouletteSequence.add(entries.get(random.nextInt(entries.size())));
		}
		int stopIndex = Math.max(0, rouletteSequence.size() - 5);
		rouletteSequence.set(stopIndex, entry);
		spinTarget = stopIndex;
	}

	private void buildIdleRoulette() {
		rouletteSequence = new ArrayList<>();
		if (entries.isEmpty()) {
			return;
		}
		Random random = new Random();
		for (int i = 0; i < 20; i++) {
			rouletteSequence.add(entries.get(random.nextInt(entries.size())));
		}
	}

	private void buildCards() {
		cards.clear();
		int columns = getColumns();
		int contentWidth = columns * CARD_WIDTH + (columns - 1) * CARD_GAP;
		int startX = (this.width - contentWidth) / 2;
		int listTop = getListTop();
		int count = getVisibleEntryCount();

		for (int i = 0; i < count; i++) {
			CaseEntry entry = entries.get(i);
			int col = i % columns;
			int row = i / columns;
			int x = startX + col * (CARD_WIDTH + CARD_GAP);
			int y = listTop + row * (CARD_HEIGHT + CARD_GAP);
			cards.add(new CaseCard(entry, x, y));
		}
	}

	private int getColumns() {
		return CARD_COLUMNS;
	}

	@Override
	public void tick() {
		super.tick();
		if (!spinning) {
			return;
		}
		spinTicks++;
		float progress = MathHelper.clamp(spinTicks / (float) SPIN_DURATION_TICKS, 0.0f, 1.0f);
		float ease = 1.0f - (float) Math.pow(1.0f - progress, 3.0f);
		spinPos = spinTarget * ease;
		if (progress >= 1.0f) {
			spinning = false;
			spinPos = spinTarget;
			updateButtonState();
			openResultScreen();
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		this.renderBackground(context);
		drawPanel(context);
		drawRoulette(context);
		drawCards(context, mouseX, mouseY);
		super.render(context, mouseX, mouseY, delta);
	}

	private void drawPanel(DrawContext context) {
		int background = 0xFF141414;
		context.fill(0, 0, this.width, this.height, background);
	}

	private void drawRoulette(DrawContext context) {
		int totalWidth = ROULETTE_SLOTS * ROULETTE_SLOT_SIZE + (ROULETTE_SLOTS - 1) * ROULETTE_SLOT_GAP;
		int startX = (this.width - totalWidth) / 2;
		int y = getRouletteTop();
		int background = 0xFF1F1F1F;
		int border = 0xFF4A4A4A;
		context.fill(startX - 10, y - 10, startX + totalWidth + 10, y + ROULETTE_HEIGHT - 6, background);
		context.fill(startX - 10, y - 10, startX + totalWidth + 10, y - 9, border);
		context.fill(startX - 10, y + ROULETTE_HEIGHT - 7, startX + totalWidth + 10, y + ROULETTE_HEIGHT - 6, border);
		context.fill(startX - 10, y - 10, startX - 9, y + ROULETTE_HEIGHT - 6, border);
		context.fill(startX + totalWidth + 9, y - 10, startX + totalWidth + 10, y + ROULETTE_HEIGHT - 6, border);

		int centerX = startX + totalWidth / 2;
		context.fill(centerX - 1, y - 8, centerX + 1, y + ROULETTE_HEIGHT - 8, 0xFFE0B56E);

		if (rouletteSequence.isEmpty()) {
			return;
		}
		context.enableScissor(startX, y - 8, startX + totalWidth, y + ROULETTE_HEIGHT - 8);
		int centerIndex = (int) Math.floor(spinPos);
		float offset = (spinPos - centerIndex) * (ROULETTE_SLOT_SIZE + ROULETTE_SLOT_GAP);
		int firstIndex = centerIndex - ROULETTE_SLOTS / 2;
		for (int i = 0; i < ROULETTE_SLOTS; i++) {
			int index = MathHelper.clamp(firstIndex + i, 0, rouletteSequence.size() - 1);
			CaseEntry entry = rouletteSequence.get(index);
			ItemStack stack = resolveStack(entry);
			int slotX = startX + Math.round(i * (ROULETTE_SLOT_SIZE + ROULETTE_SLOT_GAP) - offset);
			int slotY = y + 10;
			context.fill(slotX - 2, slotY - 2, slotX + ROULETTE_SLOT_SIZE + 2, slotY + ROULETTE_SLOT_SIZE + 2, 0xFF2B2B2B);
			if (!stack.isEmpty()) {
				int iconX = slotX + (ROULETTE_SLOT_SIZE - 16) / 2;
				int iconY = slotY + (ROULETTE_SLOT_SIZE - 16) / 2;
				context.drawItem(stack, iconX, iconY);
			}
		}
		context.disableScissor();
	}

	private void drawCards(DrawContext context, int mouseX, int mouseY) {
		CaseCard hovered = null;
		int listBottom = getListBottom();
		for (CaseCard card : cards) {
			if (card.y + CARD_HEIGHT > listBottom) {
				continue;
			}
			boolean hoveredCard = isMouseOverCard(mouseX, mouseY, card);
			if (hoveredCard) {
				hovered = card;
			}
			int background = getCardBackground(card.entry, hoveredCard);
			int border = hoveredCard ? 0xFFE0B56E : 0xFF4A4A4A;

			context.fill(card.x, card.y, card.x + CARD_WIDTH, card.y + CARD_HEIGHT, background);
			context.fill(card.x, card.y, card.x + CARD_WIDTH, card.y + 1, border);
			context.fill(card.x, card.y + CARD_HEIGHT - 1, card.x + CARD_WIDTH, card.y + CARD_HEIGHT, border);
			context.fill(card.x, card.y, card.x + 1, card.y + CARD_HEIGHT, border);
			context.fill(card.x + CARD_WIDTH - 1, card.y, card.x + CARD_WIDTH, card.y + CARD_HEIGHT, border);

			if (!card.stack.isEmpty()) {
				int iconX = card.x + (CARD_WIDTH - 16) / 2;
				int iconY = card.y + (CARD_HEIGHT - 16) / 2;
				context.drawItem(card.stack, iconX, iconY);
			}
		}

		if (hovered != null) {
			showDescriptionTooltip(context, hovered, mouseX, mouseY);
		}
	}

	private boolean isMouseOverCard(double mouseX, double mouseY, CaseCard card) {
		return mouseX >= card.x && mouseX <= card.x + CARD_WIDTH && mouseY >= card.y && mouseY <= card.y + CARD_HEIGHT;
	}

	private void showDescriptionTooltip(DrawContext context, CaseCard card, int mouseX, int mouseY) {
		if (card.entry == null) {
			return;
		}
		String description = card.entry.description != null ? card.entry.description : "";
		String name = resolveName(card.entry, card.stack);
		if ((description.isBlank()) && (name == null || name.isBlank())) {
			return;
		}
		List<Text> lines = new ArrayList<>();
		if (name != null && !name.isBlank()) {
			lines.add(Text.literal(name).formatted(Formatting.WHITE));
		}
		for (String part : description.replace("\r\n", "\n").split("\n")) {
			if (!part.isBlank()) {
				lines.add(Text.literal(part).formatted(Formatting.GRAY));
			}
		}
		if (!lines.isEmpty()) {
			context.drawTooltip(this.textRenderer, lines, mouseX, mouseY);
		}
	}

	private static int getCardBackground(CaseEntry entry, boolean hovered) {
		String rarity = entry != null && entry.rarity != null ? entry.rarity.trim().toLowerCase() : "common";
		int base = switch (rarity) {
			case "rare" -> 0xFF2B3444;
			case "epic" -> 0xFF3A2B44;
			case "legendary" -> 0xFF443821;
			default -> 0xFF2B3A2E;
		};
		if (!hovered) {
			return base;
		}
		return brighten(base, HOVER_BRIGHTNESS);
	}

	private static int brighten(int color, int amount) {
		int r = Math.min(255, ((color >> 16) & 0xFF) + amount);
		int g = Math.min(255, ((color >> 8) & 0xFF) + amount);
		int b = Math.min(255, (color & 0xFF) + amount);
		return (color & 0xFF000000) | (r << 16) | (g << 8) | b;
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

	private void openResultScreen() {
		if (resultShown) {
			return;
		}
		resultShown = true;
		if (this.client == null) {
			return;
		}
		CaseEntry entry = getResultEntry();
		if (entry == null) {
			return;
		}
		this.client.setScreen(new CaseResultScreen(entry));
	}

	private CaseEntry getResultEntry() {
		if (rouletteSequence.isEmpty()) {
			return null;
		}
		int stopIndex = MathHelper.clamp(Math.round(spinTarget), 0, rouletteSequence.size() - 1);
		return rouletteSequence.get(stopIndex);
	}

	private int getRouletteTop() {
		return getLayoutTop();
	}

	private int getListTop() {
		return getRouletteTop() + ROULETTE_HEIGHT + ROULETTE_GAP;
	}

	private int getListBottom() {
		return getListTop() + getCardsHeight();
	}

	private int getLayoutTop() {
		int contentHeight = ROULETTE_HEIGHT + ROULETTE_GAP + getCardsHeight() + FOOTER_HEIGHT;
		int top = (this.height - contentHeight) / 2;
		return Math.max(12, top);
	}

	private int getCardsHeight() {
		int rows = getCardRows();
		return rows * CARD_HEIGHT + (rows - 1) * CARD_GAP;
	}

	private int getCardRows() {
		int columns = getColumns();
		int count = getVisibleEntryCount();
		if (count == 0) {
			return 1;
		}
		return (count + columns - 1) / columns;
	}

	private int getVisibleEntryCount() {
		return Math.min(entries.size(), CARD_MAX_ITEMS);
	}

	private static final class CaseCard {
		private final CaseEntry entry;
		private final ItemStack stack;
		private final int x;
		private final int y;

		private CaseCard(CaseEntry entry, int x, int y) {
			this.entry = entry;
			this.x = x;
			this.y = y;
			this.stack = resolveStack(entry);
		}
	}
}
