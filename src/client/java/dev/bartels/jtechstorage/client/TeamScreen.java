package dev.bartels.jtechstorage.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import dev.bartels.jtechstorage.client.gui.GuiTheme;
import dev.bartels.jtechstorage.client.gui.ThemedContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import dev.bartels.jtechstorage.block.StorageTerminalBlockEntity;
import dev.bartels.jtechstorage.block.StorageTerminalBlockEntity.Role;
import dev.bartels.jtechstorage.menu.TeamMenu;
import dev.bartels.jtechstorage.net.TeamActionPayload;
import dev.bartels.jtechstorage.net.TeamStatePayload;

/**
 * Oberfläche des Team-Terminals: Team-Name + Farbe, Zugriff, Kanal-Schalter, Mitgliederliste mit Rollen,
 * Einladen per Name. Wer nicht verwalten darf, sieht alles nur an.
 */
public class TeamScreen extends ThemedContainerScreen<TeamMenu> {
	private static final int W = 256;
	private static final int H = 224;
	private static final int ROW_H = 13;
	private static final int NAME_Y = 20;
	private static final int SAVE_X = 166;
	private static final int COLOR_X = 188;
	private static final int COLOR_W = 60;
	private static final int ACCESS_Y = 38;
	private static final int HALF_W = 118;
	private static final int ROLE2_X = 130;
	private static final int CHANNEL_LABEL_Y = 58;
	private static final int CHANNEL_Y = 69;
	private static final int CHANNEL_X = 8;
	private static final int CHANNEL_GAP = 3;
	private static final int CHANNEL_W = (W - 16 - 4 * CHANNEL_GAP) / 5;
	private static final int MEMBERS_Y = 90;
	private static final int LIST_Y = 102;
	private static final int ROWS = 7;
	private static final int ROLE_X = 140;
	private static final int ROLE_W = 90;
	private static final int REMOVE_X = 234;
	private static final int ADD_Y = 200;
	private static final int INVITE_X = 176;
	private static final int INVITE_W = 72;
	private static final String[] ACCESS_KEYS = {"jtechstorage.access.private", "jtechstorage.access.friends", "jtechstorage.access.public"};

	private EditBox nameBox;
	private EditBox addBox;
	private int scroll;
	private int seenVersion = -1;

	public TeamScreen(TeamMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, W, H);
		inventoryLabelY = -10000;
	}

	private TeamStatePayload.State state() {
		return menu.getClientState();
	}

	private Role myRole() {
		return Role.byId(state().myRole());
	}

	private boolean canSettings() {
		return myRole().atLeast(Role.MANAGER);
	}

	private boolean isOwner() {
		return myRole() == Role.OWNER;
	}

	@Override
	protected void init() {
		super.init();
		nameBox = new EditBox(font, leftPos + 10, topPos + NAME_Y + 1, 150, 12, Component.translatable("jtechstorage.team.name"));
		nameBox.setMaxLength(StorageTerminalBlockEntity.MAX_NAME_LENGTH);
		nameBox.setHint(Component.translatable("jtechstorage.team.name.hint").withStyle(ChatFormatting.GRAY));
		nameBox.setValue(state().teamName());
		addRenderableWidget(nameBox);

		addBox = new EditBox(font, leftPos + 10, topPos + ADD_Y + 2, 160, 12, Component.translatable("jtechstorage.friends.name"));
		addBox.setMaxLength(16);
		addBox.setHint(Component.translatable("jtechstorage.friends.name.hint").withStyle(ChatFormatting.GRAY));
		addRenderableWidget(addBox);
		updateEditable();
	}

	private void updateEditable() {
		nameBox.setEditable(canSettings());
		addBox.visible = canSettings();
	}

	// ------------------------------------------------------------------
	// Zeichnen
	// ------------------------------------------------------------------

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);
		int x = leftPos;
		int y = topPos;
		GuiTheme.window(graphics, x, y, W, H);
		// Farb-Streifen oben in der Team-Farbe
		graphics.fill(x + 2, y + 2, x + W - 2, y + 5, 0xFF000000 | state().color());

		// Mitgliederliste
		GuiTheme.sunken(graphics, x + 7, y + LIST_Y - 2, W - 14, ROWS * ROW_H + 3);
	}

	@Override
	public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		if (seenVersion != menu.getClientVersion()) {
			seenVersion = menu.getClientVersion();

			if (!nameBox.isFocused()) {
				nameBox.setValue(state().teamName());
			}

			updateEditable();
		}

		super.extractContents(graphics, mouseX, mouseY, delta);
		TeamStatePayload.State state = state();
		int x = leftPos;
		int y = topPos;

		// Name speichern + Farbe
		button(graphics, SAVE_X, NAME_Y, 18, 14, Component.literal("✔"), canSettings(), mouseX, mouseY);
		boolean colorHover = canSettings() && over(COLOR_X, NAME_Y, COLOR_W, 14, mouseX, mouseY);
		int colorFace = colorHover ? GuiTheme.accentDarkHover() : GuiTheme.accentDark();
		GuiTheme.face(graphics, x + COLOR_X, y + NAME_Y, COLOR_W, 14, colorFace);
		graphics.fill(x + COLOR_X + 3, y + NAME_Y + 3, x + COLOR_X + 13, y + NAME_Y + 11, 0xFF000000);
		graphics.fill(x + COLOR_X + 4, y + NAME_Y + 4, x + COLOR_X + 12, y + NAME_Y + 10, 0xFF000000 | state.color());
		int colorText = GuiTheme.onColor(colorFace);
		graphics.text(font, GuiTheme.fit(font, Component.translatable("jtechstorage.team.color").getString(), COLOR_W - 20),
				x + COLOR_X + 16, y + NAME_Y + 3, colorText, colorText == 0xFFFFFFFF);

		// Zugriff + Rolle für Fremde
		int mode = Math.floorMod(state.access(), ACCESS_KEYS.length);
		button(graphics, 8, ACCESS_Y, HALF_W, 15, Component.translatable("jtechstorage.friends.access",
				Component.translatable(ACCESS_KEYS[mode])), isOwner(), mouseX, mouseY);
		boolean isPublic = mode == StorageTerminalBlockEntity.AccessMode.PUBLIC.ordinal();
		button(graphics, ROLE2_X, ACCESS_Y, HALF_W, 15, Component.translatable("jtechstorage.team.public_role",
				Component.translatable(Role.byId(state.publicRole()).translationKey())), isOwner() && isPublic, mouseX, mouseY);

		// Kanal
		graphics.text(font, Component.translatable("jtechstorage.team.channel"), x + 9, y + CHANNEL_LABEL_Y, GuiTheme.text(), false);

		for (StorageTerminalBlockEntity.Channel channel : StorageTerminalBlockEntity.Channel.values()) {
			int k = channel.ordinal();
			boolean on = (state.channels() & (1 << k)) != 0;
			toggle(graphics, channelX(k), CHANNEL_Y, CHANNEL_W, 15, Component.translatable(channel.translationKey()), on, canSettings(),
					mouseX, mouseY);
		}

		// Mitglieder
		graphics.text(font, Component.translatable("jtechstorage.team.members", state.members().size() + 1),
				x + 9, y + MEMBERS_Y, GuiTheme.text(), false);
		List<Row> rows = rows();
		scroll = Mth.clamp(scroll, 0, Math.max(0, rows.size() - ROWS));
		int listText = GuiTheme.onColor(GuiTheme.slot());
		boolean listShadow = listText == 0xFFFFFFFF;

		for (int i = 0; i < ROWS && scroll + i < rows.size(); i++) {
			Row row = rows.get(scroll + i);
			int ry = LIST_Y + i * ROW_H;

			if (i % 2 == 1) {
				graphics.fill(x + 8, y + ry, x + W - 8, y + ry + ROW_H, GuiTheme.mix(GuiTheme.slot(), listText, 0.06f));
			}

			graphics.fill(x + 11, y + ry + 4, x + 16, y + ry + 9, 0xFF000000);
			graphics.fill(x + 12, y + ry + 5, x + 15, y + ry + 8, row.online ? 0xFF3EE05A : 0xFF7A7A7A);
			String name = GuiTheme.fit(font, row.name, ROLE_X - 24);
			graphics.text(font, name, x + 20, y + ry + 3, listText, listShadow);

			Role role = Role.byId(row.role);
			boolean manageable = !row.owner && TeamMenu.mayManage(myRole(), role);

			if (row.owner) {
				graphics.text(font, Component.literal("♛ ").append(role.displayName()), x + ROLE_X + 4, y + ry + 3, 0xFFE0A020, listShadow);
			} else {
				roleButton(graphics, ROLE_X, ry, role, manageable, mouseX, mouseY);
			}

			if (manageable) {
				boolean hover = over(REMOVE_X, ry, 14, ROW_H, mouseX, mouseY);
				graphics.text(font, "×", x + REMOVE_X + 4, y + ry + 3, hover ? 0xFFFF5050 : listText, listShadow);
			}
		}

		if (rows.size() > ROWS) {
			String range = (scroll + 1) + "–" + Math.min(rows.size(), scroll + ROWS) + "/" + rows.size();
			graphics.text(font, range, x + W - 9 - font.width(range), y + MEMBERS_Y, GuiTheme.subtle(), false);
		}

		// Einladen
		if (canSettings()) {
			button(graphics, INVITE_X, ADD_Y, INVITE_W, 15, Component.translatable("jtechstorage.team.invite"), true, mouseX, mouseY);
		} else {
			GuiTheme.label(graphics, font, Component.translatable("jtechstorage.team.read_only", myRole().displayName()),
					x + 9, y + ADD_Y + 4, W - 18, GuiTheme.text());
		}
	}

	private static int channelX(int index) {
		return CHANNEL_X + index * (CHANNEL_W + CHANNEL_GAP);
	}

	private record Row(String name, int role, boolean online, boolean owner) {
	}

	private List<Row> rows() {
		TeamStatePayload.State state = state();
		List<Row> rows = new ArrayList<>();
		rows.add(new Row(state.ownerName(), Role.OWNER.ordinal(), state.ownerOnline(), true));

		for (TeamStatePayload.MemberRow member : state.members()) {
			rows.add(new Row(member.name(), member.role(), member.online(), false));
		}

		return rows;
	}

	private void button(GuiGraphicsExtractor graphics, int bx, int by, int w, int h, Component label, boolean enabled,
			int mouseX, int mouseY) {
		GuiTheme.button(graphics, font, leftPos + bx, topPos + by, w, h, label, enabled, enabled && over(bx, by, w, h, mouseX, mouseY));
	}

	private void toggle(GuiGraphicsExtractor graphics, int bx, int by, int w, int h, Component label, boolean on, boolean enabled,
			int mouseX, int mouseY) {
		boolean hover = enabled && over(bx, by, w, h, mouseX, mouseY);
		int face = on ? (hover ? 0xFF3FB862 : 0xFF23803F) : (hover ? 0xFF74747C : 0xFF5A5A62);
		Component text = Component.literal(on ? "✔ " : "").append(label);
		GuiTheme.labeled(graphics, font, leftPos + bx, topPos + by, w, h, font.width(text) <= w - 6 ? text : label, face, true);
	}

	private void roleButton(GuiGraphicsExtractor graphics, int bx, int by, Role role, boolean enabled, int mouseX, int mouseY) {
		boolean hover = enabled && over(bx, by, ROLE_W, ROW_H, mouseX, mouseY);
		Component label = enabled ? Component.literal("◀ ").append(role.displayName()).append(" ▶") : role.displayName();
		int face = enabled ? (hover ? GuiTheme.accentHover() : GuiTheme.accent()) : GuiTheme.accentDark();
		GuiTheme.labeled(graphics, font, leftPos + bx, topPos + by + 1, ROLE_W, ROW_H - 1, label, face, true);
	}

	private boolean over(int x, int y, int w, int h, double mouseX, double mouseY) {
		return mouseX >= leftPos + x && mouseX < leftPos + x + w && mouseY >= topPos + y && mouseY < topPos + y + h;
	}

	@Override
	protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractTooltip(graphics, mouseX, mouseY);

		for (StorageTerminalBlockEntity.Channel channel : StorageTerminalBlockEntity.Channel.values()) {
			if (over(channelX(channel.ordinal()), CHANNEL_Y, CHANNEL_W, 15, mouseX, mouseY)) {
				graphics.setComponentTooltipForNextFrame(font, List.of(
						Component.translatable(channel.translationKey()).withStyle(ChatFormatting.LIGHT_PURPLE),
						Component.translatable(channel.translationKey() + ".tooltip").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
				return;
			}
		}

		if (over(8, ACCESS_Y, HALF_W, 15, mouseX, mouseY)) {
			graphics.setComponentTooltipForNextFrame(font, List.of(
					Component.translatable("jtechstorage.access.tooltip.title"),
					Component.translatable("jtechstorage.access.tooltip.private").withStyle(ChatFormatting.GRAY),
					Component.translatable("jtechstorage.access.tooltip.friends").withStyle(ChatFormatting.GRAY),
					Component.translatable("jtechstorage.access.tooltip.public").withStyle(ChatFormatting.GRAY),
					Component.translatable("jtechstorage.team.owner_only").withStyle(ChatFormatting.DARK_PURPLE)), mouseX, mouseY);
			return;
		}

		if (over(ROLE2_X, ACCESS_Y, HALF_W, 15, mouseX, mouseY)) {
			graphics.setComponentTooltipForNextFrame(font, List.of(
					Component.translatable("jtechstorage.team.public_role.tooltip").withStyle(ChatFormatting.GRAY),
					Component.translatable("jtechstorage.team.owner_only").withStyle(ChatFormatting.DARK_PURPLE)), mouseX, mouseY);
			return;
		}

		if (over(ROLE_X, LIST_Y, ROLE_W, ROWS * ROW_H, mouseX, mouseY)) {
			List<Component> lines = new ArrayList<>();
			lines.add(Component.translatable("jtechstorage.role.tooltip.title"));

			for (Role role : Role.values()) {
				lines.add(Component.empty().append(role.displayName()).append(Component.literal(": "))
						.append(Component.translatable(role.translationKey() + ".desc").withStyle(ChatFormatting.GRAY)));
			}

			lines.add(Component.translatable("jtechstorage.role.tooltip.click").withStyle(ChatFormatting.DARK_PURPLE));
			graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
		}
	}

	// ------------------------------------------------------------------
	// Eingabe
	// ------------------------------------------------------------------

	private void send(TeamActionPayload.Action action, String text, int value) {
		ClientPlayNetworking.send(new TeamActionPayload(menu.containerId, action, text, value));
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mx = event.x();
		double my = event.y();
		TeamStatePayload.State state = state();

		if (nameBox.isMouseOver(mx, my) && canSettings()) {
			addBox.setFocused(false);
			setFocused(nameBox);
			nameBox.setFocused(true);
			return nameBox.mouseClicked(event, doubleClick);
		}

		if (addBox.visible && addBox.isMouseOver(mx, my)) {
			nameBox.setFocused(false);
			setFocused(addBox);
			addBox.setFocused(true);
			return addBox.mouseClicked(event, doubleClick);
		}

		nameBox.setFocused(false);
		addBox.setFocused(false);
		setFocused(null);

		if (canSettings() && over(SAVE_X, NAME_Y, 18, 14, mx, my)) {
			send(TeamActionPayload.Action.RENAME, nameBox.getValue().trim(), 0);
			return true;
		}

		if (canSettings() && over(COLOR_X, NAME_Y, COLOR_W, 14, mx, my)) {
			send(TeamActionPayload.Action.COLOR, "", state.colorIndex() + (event.button() == 1 ? -1 : 1));
			return true;
		}

		if (isOwner() && over(8, ACCESS_Y, HALF_W, 15, mx, my)) {
			send(TeamActionPayload.Action.ACCESS, "", state.access() + (event.button() == 1 ? -1 : 1));
			return true;
		}

		if (isOwner() && state.access() == StorageTerminalBlockEntity.AccessMode.PUBLIC.ordinal() && over(ROLE2_X, ACCESS_Y, HALF_W, 15, mx, my)) {
			send(TeamActionPayload.Action.PUBLIC_ROLE, "", state.publicRole() + (event.button() == 1 ? -1 : 1));
			return true;
		}

		if (canSettings()) {
			for (StorageTerminalBlockEntity.Channel channel : StorageTerminalBlockEntity.Channel.values()) {
				if (over(channelX(channel.ordinal()), CHANNEL_Y, CHANNEL_W, 15, mx, my)) {
					send(TeamActionPayload.Action.CHANNEL, "", channel.ordinal());
					return true;
				}
			}
		}

		List<Row> rows = rows();

		for (int i = 0; i < ROWS && scroll + i < rows.size(); i++) {
			Row row = rows.get(scroll + i);
			int ry = LIST_Y + i * ROW_H;
			Role role = Role.byId(row.role);

			if (row.owner || !TeamMenu.mayManage(myRole(), role)) {
				continue;
			}

			if (over(ROLE_X, ry, ROLE_W, ROW_H, mx, my)) {
				send(TeamActionPayload.Action.SET_ROLE, row.name, nextRole(role, event.button() == 1 ? -1 : 1).ordinal());
				return true;
			}

			if (over(REMOVE_X, ry, 14, ROW_H, mx, my)) {
				send(TeamActionPayload.Action.REMOVE, row.name, 0);
				return true;
			}
		}

		if (canSettings() && over(INVITE_X, ADD_Y, INVITE_W, 15, mx, my)) {
			submitAdd();
			return true;
		}

		return super.mouseClicked(event, doubleClick);
	}

	/** Nächste Rolle, die der Betrachter vergeben darf (im Kreis). */
	private Role nextRole(Role current, int direction) {
		int limit = isOwner() ? Role.MANAGER.ordinal() : Role.BUILDER.ordinal();
		return Role.byId(Math.floorMod(current.ordinal() + direction, limit + 1));
	}

	private void submitAdd() {
		String name = addBox.getValue().trim();

		if (!name.isEmpty()) {
			send(TeamActionPayload.Action.ADD, name, 0);
			addBox.setValue("");
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (over(8, LIST_Y, W - 16, ROWS * ROW_H, mouseX, mouseY)) {
			scroll = Math.max(0, scroll - (int) Math.signum(scrollY));
			return true;
		}

		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		EditBox focused = nameBox.isFocused() ? nameBox : addBox.isFocused() ? addBox : null;

		if (focused != null) {
			if (event.isEscape()) {
				focused.setFocused(false);
				setFocused(null);
			} else if (event.key() == 257 || event.key() == 335) { // Enter
				if (focused == nameBox) {
					send(TeamActionPayload.Action.RENAME, nameBox.getValue().trim(), 0);
					nameBox.setFocused(false);
					setFocused(null);
				} else {
					submitAdd();
				}
			} else {
				focused.keyPressed(event);
			}

			return true;
		}

		return super.keyPressed(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (nameBox.isFocused()) {
			return nameBox.charTyped(event);
		}

		if (addBox.isFocused()) {
			return addBox.charTyped(event);
		}

		return super.charTyped(event);
	}
}
