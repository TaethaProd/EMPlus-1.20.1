package taethaprod.emplus.shop;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

public class ShopScreen extends Screen {
	private static final int CARD_WIDTH = 160;
	private static final int CARD_HEIGHT = 36;
	private static final int CARD_GAP = 8;
	private static final int BASE_HEADER_HEIGHT = 52;
	private static final int BUY_TAB_WIDTH = 90;
	private static final int BUY_TAB_HEIGHT = 18;
	private static final int BUY_TAB_GAP = 6;
	private static final int BUY_TAB_SPACING = 8;
	private static final int FOOTER_HEIGHT = 30;

	private final List<ShopCard> cards = new ArrayList<>();
	private final List<CaseCard> caseCards = new ArrayList<>();
	private Tab activeTab = Tab.BUY;
	private String activeBuyTabId = "";
	private int scrollOffset = 0;
	private int maxScroll = 0;
	private int lastRevision = -1;
	private ButtonWidget buyButton;
	private ButtonWidget sellButton;
	private ButtonWidget casesButton;

	public ShopScreen() {
		super(Text.translatable("ui.emplus.shop.title"));
	}

	@Override
	protected void init() {
		ShopClientNetworking.requestSync();
		setupTabButtons();
		rebuildCards();
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		this.renderBackground(context);

		if (ShopClientData.getRevision() != lastRevision) {
			lastRevision = ShopClientData.getRevision();
			rebuildCards();
		}

		context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, 0xFFFFFF);
		drawBalance(context);
		drawBuyTabs(context, mouseX, mouseY);
		drawCards(context, mouseX, mouseY);

		super.render(context, mouseX, mouseY, delta);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && handleBuyTabClick(mouseX, mouseY)) {
			return true;
		}
		if (button == 0 && handleCardClick(mouseX, mouseY)) {
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
		if (maxScroll <= 0) {
			return super.mouseScrolled(mouseX, mouseY, amount);
		}
		int next = MathHelper.clamp(scrollOffset - (int) (amount * 12), 0, maxScroll);
		if (next != scrollOffset) {
			scrollOffset = next;
			rebuildCards();
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, amount);
	}

	private void setupTabButtons() {
		int buttonWidth = 80;
		int buttonHeight = 20;
		int gap = 10;
		int totalWidth = buttonWidth * 3 + gap * 2;
		int startX = (this.width - totalWidth) / 2;
		int y = 28;
		buyButton = ButtonWidget.builder(Text.translatable("ui.emplus.shop.buy"), button -> setTab(Tab.BUY))
				.dimensions(startX, y, buttonWidth, buttonHeight)
				.build();
		sellButton = ButtonWidget.builder(Text.translatable("ui.emplus.shop.sell"), button -> setTab(Tab.SELL))
				.dimensions(startX + buttonWidth + gap, y, buttonWidth, buttonHeight)
				.build();
		casesButton = ButtonWidget.builder(Text.translatable("ui.emplus.shop.cases"), button -> setTab(Tab.CASES))
				.dimensions(startX + (buttonWidth + gap) * 2, y, buttonWidth, buttonHeight)
				.build();
		addDrawableChild(buyButton);
		addDrawableChild(sellButton);
		addDrawableChild(casesButton);
		updateTabButtons();
	}

	private void setTab(Tab tab) {
		if (this.activeTab != tab) {
			this.activeTab = tab;
			this.scrollOffset = 0;
			updateTabButtons();
			rebuildCards();
		}
	}

	private void updateTabButtons() {
		if (buyButton != null) {
			buyButton.active = activeTab != Tab.BUY;
		}
		if (sellButton != null) {
			sellButton.active = activeTab != Tab.SELL;
		}
		if (casesButton != null) {
			casesButton.active = activeTab != Tab.CASES;
		}
	}

	private void drawBalance(DrawContext context) {
		Text balance = Text.translatable("ui.emplus.shop.balance", ShopClientData.getBalance());
		int width = this.textRenderer.getWidth(balance);
		context.drawTextWithShadow(this.textRenderer, balance, this.width - width - 10, 10, 0xF5D27A);
	}

	private void drawCards(DrawContext context, int mouseX, int mouseY) {
		if (activeTab == Tab.CASES) {
			drawCaseCards(context, mouseX, mouseY);
			return;
		}
		int listTop = getListTop();
		int listBottom = getListBottom();
		ShopCard hoveredCard = null;

		if (cards.isEmpty()) {
			context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("ui.emplus.shop.empty"), this.width / 2,
					listTop + 10, 0xAAAAAA);
			return;
		}

		for (ShopCard card : cards) {
			if (card.y + CARD_HEIGHT < listTop || card.y > listBottom) {
				continue;
			}
			boolean hovered = isMouseOverCard(mouseX, mouseY, card);
			if (hovered) {
				hoveredCard = card;
			}
			int background = hovered ? 0xFF3A3A3A : 0xFF2B2B2B;
			int border = hovered ? 0xFFE0B56E : 0xFF4A4A4A;

			context.fill(card.x, card.y, card.x + CARD_WIDTH, card.y + CARD_HEIGHT, background);
			context.fill(card.x, card.y, card.x + CARD_WIDTH, card.y + 1, border);
			context.fill(card.x, card.y + CARD_HEIGHT - 1, card.x + CARD_WIDTH, card.y + CARD_HEIGHT, border);
			context.fill(card.x, card.y, card.x + 1, card.y + CARD_HEIGHT, border);
			context.fill(card.x + CARD_WIDTH - 1, card.y, card.x + CARD_WIDTH, card.y + CARD_HEIGHT, border);

			if (!card.stack.isEmpty()) {
				context.drawItem(card.stack, card.x + 6, card.y + 10);
			}

			String name = this.textRenderer.trimToWidth(card.displayName, CARD_WIDTH - 36);
			context.drawTextWithShadow(this.textRenderer, name, card.x + 28, card.y + 6, 0xFFFFFF);
			Text cost = Text.translatable("ui.emplus.shop.cost", card.cost);
			context.drawTextWithShadow(this.textRenderer, cost, card.x + 28, card.y + 20, 0xFFD48A);
		}

		if (hoveredCard != null) {
			showDescriptionTooltip(context, hoveredCard, mouseX, mouseY);
		}
	}

	private void drawCaseCards(DrawContext context, int mouseX, int mouseY) {
		int listTop = getListTop();
		int listBottom = getListBottom();
		CaseCard hoveredCard = null;

		if (caseCards.isEmpty()) {
			context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("ui.emplus.shop.empty"), this.width / 2,
					listTop + 10, 0xAAAAAA);
			return;
		}

		for (CaseCard card : caseCards) {
			if (card.y + CARD_HEIGHT < listTop || card.y > listBottom) {
				continue;
			}
			boolean hovered = isMouseOverCard(mouseX, mouseY, card.x, card.y);
			if (hovered) {
				hoveredCard = card;
			}
			int background = hovered ? 0xFF3A3A3A : 0xFF2B2B2B;
			int border = hovered ? 0xFFE0B56E : 0xFF4A4A4A;

			context.fill(card.x, card.y, card.x + CARD_WIDTH, card.y + CARD_HEIGHT, background);
			context.fill(card.x, card.y, card.x + CARD_WIDTH, card.y + 1, border);
			context.fill(card.x, card.y + CARD_HEIGHT - 1, card.x + CARD_WIDTH, card.y + CARD_HEIGHT, border);
			context.fill(card.x, card.y, card.x + 1, card.y + CARD_HEIGHT, border);
			context.fill(card.x + CARD_WIDTH - 1, card.y, card.x + CARD_WIDTH, card.y + CARD_HEIGHT, border);

			if (!card.stack.isEmpty()) {
				context.drawItem(card.stack, card.x + 6, card.y + 10);
			}

			String name = this.textRenderer.trimToWidth(card.displayName, CARD_WIDTH - 36);
			context.drawTextWithShadow(this.textRenderer, name, card.x + 28, card.y + 6, 0xFFFFFF);
			Text cost = Text.translatable("ui.emplus.shop.cost", card.cost);
			context.drawTextWithShadow(this.textRenderer, cost, card.x + 28, card.y + 20, 0xFFD48A);
		}

		if (hoveredCard != null && !hoveredCard.displayName.isBlank()) {
			context.drawTooltip(this.textRenderer, Text.literal(hoveredCard.displayName), mouseX, mouseY);
		}
	}

	private void drawBuyTabs(DrawContext context, int mouseX, int mouseY) {
		if (activeTab != Tab.BUY) {
			return;
		}
		List<ShopTab> tabs = ShopClientData.getBuyTabs();
		if (tabs.isEmpty()) {
			return;
		}
		ensureActiveBuyTabId(tabs);

		int totalWidth = tabs.size() * BUY_TAB_WIDTH + (tabs.size() - 1) * BUY_TAB_GAP;
		int startX = (this.width - totalWidth) / 2;
		int y = BASE_HEADER_HEIGHT;

		for (int i = 0; i < tabs.size(); i++) {
			ShopTab tab = tabs.get(i);
			int x = startX + i * (BUY_TAB_WIDTH + BUY_TAB_GAP);
			boolean hovered = mouseX >= x && mouseX <= x + BUY_TAB_WIDTH && mouseY >= y && mouseY <= y + BUY_TAB_HEIGHT;
			boolean active = tab.id != null && tab.id.equals(activeBuyTabId);
			int background = active ? 0xFF3A3A3A : hovered ? 0xFF333333 : 0xFF252525;
			int border = active ? 0xFFE0B56E : 0xFF4A4A4A;

			context.fill(x, y, x + BUY_TAB_WIDTH, y + BUY_TAB_HEIGHT, background);
			context.fill(x, y, x + BUY_TAB_WIDTH, y + 1, border);
			context.fill(x, y + BUY_TAB_HEIGHT - 1, x + BUY_TAB_WIDTH, y + BUY_TAB_HEIGHT, border);
			context.fill(x, y, x + 1, y + BUY_TAB_HEIGHT, border);
			context.fill(x + BUY_TAB_WIDTH - 1, y, x + BUY_TAB_WIDTH, y + BUY_TAB_HEIGHT, border);

			String label = tab.label != null && !tab.label.isBlank() ? tab.label : tab.id;
			String labelText = this.textRenderer.trimToWidth(label, BUY_TAB_WIDTH - 8);
			int textWidth = this.textRenderer.getWidth(labelText);
			int textX = x + (BUY_TAB_WIDTH - textWidth) / 2;
			int textY = y + (BUY_TAB_HEIGHT - 8) / 2;
			context.drawTextWithShadow(this.textRenderer, labelText, textX, textY, 0xFFFFFF);
		}
	}

	private boolean handleBuyTabClick(double mouseX, double mouseY) {
		if (activeTab != Tab.BUY) {
			return false;
		}
		List<ShopTab> tabs = ShopClientData.getBuyTabs();
		if (tabs.isEmpty()) {
			return false;
		}
		int totalWidth = tabs.size() * BUY_TAB_WIDTH + (tabs.size() - 1) * BUY_TAB_GAP;
		int startX = (this.width - totalWidth) / 2;
		int y = BASE_HEADER_HEIGHT;
		if (mouseY < y || mouseY > y + BUY_TAB_HEIGHT) {
			return false;
		}
		for (int i = 0; i < tabs.size(); i++) {
			int x = startX + i * (BUY_TAB_WIDTH + BUY_TAB_GAP);
			if (mouseX >= x && mouseX <= x + BUY_TAB_WIDTH) {
				ShopTab tab = tabs.get(i);
				if (tab != null && tab.id != null && !tab.id.equals(activeBuyTabId)) {
					activeBuyTabId = tab.id;
					scrollOffset = 0;
					rebuildCards();
				}
				return true;
			}
		}
		return false;
	}

	private boolean handleCardClick(double mouseX, double mouseY) {
		if (activeTab == Tab.CASES) {
			return handleCaseCardClick(mouseX, mouseY);
		}
		int listTop = getListTop();
		int listBottom = getListBottom();
		if (mouseY < listTop || mouseY > listBottom) {
			return false;
		}
		for (ShopCard card : cards) {
			if (isMouseOverCard(mouseX, mouseY, card)) {
				byte action = activeTab == Tab.BUY ? ShopNetworking.ACTION_BUY : ShopNetworking.ACTION_SELL;
				ShopClientNetworking.sendAction(action, card.entry != null ? card.entry.id : "");
				return true;
			}
		}
		return false;
	}

	private boolean handleCaseCardClick(double mouseX, double mouseY) {
		int listTop = getListTop();
		int listBottom = getListBottom();
		if (mouseY < listTop || mouseY > listBottom) {
			return false;
		}
		for (CaseCard card : caseCards) {
			if (isMouseOverCard(mouseX, mouseY, card.x, card.y)) {
				if (card.entry != null && card.entry.id != null && !card.entry.id.isBlank()) {
					taethaprod.emplus.cases.CaseClientNetworking.requestOpen(card.entry.id);
				}
				return true;
			}
		}
		return false;
	}

	private boolean isMouseOverCard(double mouseX, double mouseY, ShopCard card) {
		return mouseX >= card.x && mouseX <= card.x + CARD_WIDTH && mouseY >= card.y && mouseY <= card.y + CARD_HEIGHT;
	}

	private boolean isMouseOverCard(double mouseX, double mouseY, int x, int y) {
		return mouseX >= x && mouseX <= x + CARD_WIDTH && mouseY >= y && mouseY <= y + CARD_HEIGHT;
	}

	private void rebuildCards() {
		cards.clear();
		caseCards.clear();
		List<ShopEntry> entries = activeTab == Tab.BUY ? getBuyEntriesForTab() : ShopClientData.getSellEntries();
		List<ShopCaseEntry> cases = activeTab == Tab.CASES ? ShopClientData.getCaseEntries() : List.of();
		int columns = getColumns();
		int startX = (this.width - (columns * CARD_WIDTH + (columns - 1) * CARD_GAP)) / 2;
		int listTop = getListTop();

		int size = activeTab == Tab.CASES ? cases.size() : entries.size();
		int rows = (int) Math.ceil(size / (double) columns);
		int contentHeight = Math.max(0, rows * (CARD_HEIGHT + CARD_GAP) - CARD_GAP);
		int viewHeight = getListBottom() - listTop;
		maxScroll = Math.max(0, contentHeight - viewHeight);
		scrollOffset = MathHelper.clamp(scrollOffset, 0, maxScroll);

		if (activeTab == Tab.CASES) {
			for (int i = 0; i < cases.size(); i++) {
				ShopCaseEntry entry = cases.get(i);
				int col = i % columns;
				int row = i / columns;
				int x = startX + col * (CARD_WIDTH + CARD_GAP);
				int y = listTop + row * (CARD_HEIGHT + CARD_GAP) - scrollOffset;
				caseCards.add(new CaseCard(entry, x, y));
			}
		} else {
			for (int i = 0; i < entries.size(); i++) {
				ShopEntry entry = entries.get(i);
				int col = i % columns;
				int row = i / columns;
				int x = startX + col * (CARD_WIDTH + CARD_GAP);
				int y = listTop + row * (CARD_HEIGHT + CARD_GAP) - scrollOffset;
				cards.add(new ShopCard(entry, x, y));
			}
		}
	}

	private int getColumns() {
		return this.width < 420 ? 2 : 3;
	}

	private int getListTop() {
		boolean hasBuyTabs = activeTab == Tab.BUY && !ShopClientData.getBuyTabs().isEmpty();
		return BASE_HEADER_HEIGHT + (hasBuyTabs ? BUY_TAB_HEIGHT + BUY_TAB_SPACING : 0);
	}

	private int getListBottom() {
		return this.height - FOOTER_HEIGHT;
	}

	private static ItemStack resolveStack(ShopEntry entry) {
		if (entry == null) {
			return ItemStack.EMPTY;
		}
		String iconId = entry.icon != null && !entry.icon.isBlank() ? entry.icon : entry.item;
		if (iconId == null || iconId.isBlank()) {
			return ItemStack.EMPTY;
		}
		Identifier id = Identifier.tryParse(iconId);
		if (id == null) {
			return ItemStack.EMPTY;
		}
		var item = Registries.ITEM.get(id);
		if (item == net.minecraft.item.Items.AIR) {
			return ItemStack.EMPTY;
		}
		return new ItemStack(item);
	}

	private static String resolveName(ShopEntry entry, ItemStack stack) {
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

	private void showDescriptionTooltip(DrawContext context, ShopCard card, int mouseX, int mouseY) {
		if (card == null || card.entry == null || !card.entry.isService()) {
			return;
		}
		String description = card.entry.description;
		if (description == null || description.isBlank()) {
			return;
		}
		List<Text> lines = new ArrayList<>();
		for (String part : description.replace("\r\n", "\n").split("\n")) {
			lines.add(Text.literal(part));
		}
		if (!lines.isEmpty()) {
			context.drawTooltip(this.textRenderer, lines, mouseX, mouseY);
		}
	}

	private List<ShopEntry> getBuyEntriesForTab() {
		List<ShopEntry> entries = ShopClientData.getBuyEntries();
		List<ShopTab> tabs = ShopClientData.getBuyTabs();
		if (tabs.isEmpty()) {
			return entries;
		}
		String activeTabId = ensureActiveBuyTabId(tabs);
		if (activeTabId.isEmpty()) {
			return entries;
		}
		List<ShopEntry> filtered = new ArrayList<>();
		for (ShopEntry entry : entries) {
			if (entry == null) {
				continue;
			}
			String tab = entry.tab != null ? entry.tab : "";
			if (tab.isBlank() || activeTabId.equals(tab)) {
				filtered.add(entry);
			}
		}
		return filtered;
	}

	private String ensureActiveBuyTabId(List<ShopTab> tabs) {
		if (tabs.isEmpty()) {
			activeBuyTabId = "";
			return "";
		}
		boolean found = false;
		for (ShopTab tab : tabs) {
			if (tab != null && tab.id != null && tab.id.equals(activeBuyTabId)) {
				found = true;
				break;
			}
		}
		if (!found) {
			ShopTab first = tabs.get(0);
			activeBuyTabId = first != null && first.id != null ? first.id : "";
		}
		return activeBuyTabId;
	}

	private enum Tab {
		BUY,
		SELL,
		CASES
	}

	private static final class ShopCard {
		private final ShopEntry entry;
		private final ItemStack stack;
		private final String displayName;
		private final int cost;
		private final int x;
		private final int y;

		private ShopCard(ShopEntry entry, int x, int y) {
			this.entry = entry;
			this.x = x;
			this.y = y;
			this.stack = resolveStack(entry);
			this.displayName = resolveName(entry, this.stack);
			this.cost = entry != null ? Math.max(0, entry.cost) : 0;
		}
	}

	private static final class CaseCard {
		private final ShopCaseEntry entry;
		private final ItemStack stack;
		private final String displayName;
		private final int cost;
		private final int x;
		private final int y;

		private CaseCard(ShopCaseEntry entry, int x, int y) {
			this.entry = entry;
			this.x = x;
			this.y = y;
			this.stack = resolveCaseStack(entry);
			this.displayName = resolveCaseName(entry);
			this.cost = entry != null ? Math.max(0, entry.cost) : 0;
		}
	}

	private static ItemStack resolveCaseStack(ShopCaseEntry entry) {
		String id = entry != null ? entry.id : "";
		net.minecraft.item.Item item = switch (id) {
			case "rare" -> Items.ENDER_CHEST;
			case "epic" -> Items.PURPLE_SHULKER_BOX;
			case "legendary" -> Items.NETHER_STAR;
			default -> Items.CHEST;
		};
		return new ItemStack(item);
	}

	private static String resolveCaseName(ShopCaseEntry entry) {
		String id = entry != null && entry.id != null ? entry.id : "";
		if (id.isBlank()) {
			return "Case";
		}
		String key = "ui.emplus.case.type." + id;
		if (I18n.hasTranslation(key)) {
			return Text.translatable(key).getString();
		}
		return id;
	}
}
