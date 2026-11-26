
package com.projectoverseer.ui;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.function.Consumer;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Disposable;

import com.projectoverseer.config.ClientConfig;
import com.projectoverseer.shared.chat.ChatMessage;

public final class ChatUI implements Disposable {

    private static final int MAX_HISTORY =
        ClientConfig.getInt("ui.chat.max_history", 50);

    private static final float PANEL_WIDTH =
        ClientConfig.getFloat("ui.chat.panel_width", 250f);

    private static final float HISTORY_HEIGHT =
        ClientConfig.getFloat("ui.chat.history_height", 80f);

    private static final float INPUT_HEIGHT =
        ClientConfig.getFloat("ui.chat.input_height", 26f);

    private static final float PREVIEW_WIDTH =
        Math.min(PANEL_WIDTH, 230f);

    private static final float PREVIEW_HEIGHT = 24f;

    private final Stage stage;
    private final Consumer<String> onSubmit;
    private final ArrayDeque<String> history = new ArrayDeque<>();

    private final BitmapFont font;
    private final Texture panelTexture;
    private final Texture inputTexture;
    private final Texture cursorTexture;
    private final Texture selectionTexture;

    private final Table root;
    private final Table panel;
    private final Table preview;
    private final Table messagesTable;

    private final ScrollPane messagesScroll;
    private final Label.LabelStyle messageStyle;
    private final Label previewLabel;
    private final TextField input;
    private final Cell<TextField> inputCell;

    private boolean typing;

    public ChatUI(Stage stage, Consumer<String> onSubmit) {
        this.stage = Objects.requireNonNull(stage, "stage");
        this.onSubmit = Objects.requireNonNull(onSubmit, "onSubmit");

        font = new BitmapFont();
        font.getData().setScale(0.7f);

        panelTexture = createTexture(
            ClientConfig.getString("ui.chat.panel_color", "17212ECC")
        );

        inputTexture = createTexture(
            ClientConfig.getString("ui.chat.input_color", "304257FF")
        );

        cursorTexture = createTexture(
            ClientConfig.getString("ui.chat.cursor_color", "FFFFFFFF")
        );

        selectionTexture = createTexture(
            ClientConfig.getString("ui.chat.selection_color", "5477A6AA")
        );

        messageStyle = new Label.LabelStyle(font, Color.WHITE);

        TextField.TextFieldStyle inputStyle =
            new TextField.TextFieldStyle();

        inputStyle.font = font;
        inputStyle.fontColor = Color.WHITE;
        inputStyle.messageFontColor = Color.LIGHT_GRAY;

        inputStyle.background = new TextureRegionDrawable(
            new TextureRegion(inputTexture)
        );

        inputStyle.cursor = new TextureRegionDrawable(
            new TextureRegion(cursorTexture)
        );

        inputStyle.selection = new TextureRegionDrawable(
            new TextureRegion(selectionTexture)
        );

        input = new TextField("", inputStyle);
        input.setMessageText("Type a message...");
        input.setMaxLength(ChatMessage.MAX_TEXT_LENGTH);
        input.setFocusTraversal(false);
        input.setVisible(false);

        input.setTextFieldListener((field, character) -> {
            if (character == '\n' || character == '\r') {
                submit();
            }
        });

        input.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.ESCAPE) {
                    close();
                    return true;
                }

                return false;
            }
        });

        messagesTable = new Table();
        messagesTable.top().left();

        messagesScroll = new ScrollPane(
            messagesTable,
            new ScrollPane.ScrollPaneStyle()
        );

        messagesScroll.setScrollingDisabled(true, false);
        messagesScroll.setOverscroll(false, false);

        panel = new Table();
        panel.setBackground(
            new TextureRegionDrawable(
                new TextureRegion(panelTexture)
            )
        );
        panel.pad(8f);

        Label title = new Label("CHAT", messageStyle);

        panel.add(title)
            .left()
            .padBottom(5f)
            .row();

        panel.add(messagesScroll)
            .width(PANEL_WIDTH - 16f)
            .height(HISTORY_HEIGHT)
            .row();

        inputCell = panel.add(input)
            .width(PANEL_WIDTH - 16f)
            .height(0f)
            .padTop(0f);

        previewLabel = new Label("", messageStyle);
        previewLabel.setEllipsis(true);

        preview = new Table();
        preview.setBackground(
            new TextureRegionDrawable(
                new TextureRegion(panelTexture)
            )
        );
        preview.setTouchable(Touchable.disabled);
        preview.pad(5f);

        preview.add(previewLabel)
            .width(PREVIEW_WIDTH - 10f)
            .left();

        root = new Table();
        root.setFillParent(true);
        root.setTouchable(Touchable.childrenOnly);
        root.bottom().left()
            .padLeft(12f)
            .padBottom(90f);

        stage.addActor(root);

        refreshHistory();
    }

    public void open() {
        if (typing) {
            return;
        }

        typing = true;

        input.setText("");
        input.setVisible(true);

        inputCell.height(INPUT_HEIGHT).padTop(6f);

        root.clearChildren();
        root.add(panel).width(PANEL_WIDTH);

        root.invalidateHierarchy();
        root.toFront();

        stage.setKeyboardFocus(input);
    }

    public void close() {
        typing = false;

        if (stage.getKeyboardFocus() == input) {
            stage.setKeyboardFocus(null);
        }

        input.setText("");
        input.setVisible(false);

        inputCell.height(0f).padTop(0f);

        showPreview();
    }

    public boolean isTyping() {
        return typing;
    }

    public void addMessage(String sender, String text) {
        if (sender == null
            || sender.isBlank()
            || text == null
            || text.isBlank()) {
            return;
        }

        if (history.size() == MAX_HISTORY) {
            history.removeFirst();
        }

        history.addLast(sender + ": " + text);

        refreshHistory();
    }

    public void clearHistory() {
        history.clear();
        refreshHistory();
    }

    private void submit() {
        String text = input.getText().strip();

        close();

        if (!text.isEmpty()) {
            onSubmit.accept(text);
        }
    }

    private void refreshHistory() {
        messagesTable.clearChildren();

        for (String message : history) {
            Label label = new Label(message, messageStyle);
            label.setWrap(true);

            messagesTable.add(label)
                .width(PANEL_WIDTH - 28f)
                .left()
                .padBottom(3f)
                .row();
        }

        messagesTable.invalidateHierarchy();
        messagesTable.pack();
        messagesScroll.layout();
        messagesScroll.setScrollPercentY(1f);

        if (!typing) {
            showPreview();
        }
    }

    private void showPreview() {
        root.clearChildren();

        if (!history.isEmpty()) {
            previewLabel.setText(history.peekLast());

            root.add(preview)
                .width(PREVIEW_WIDTH)
                .height(PREVIEW_HEIGHT);
        }

        root.invalidateHierarchy();
    }

    private static Texture createTexture(String hexColor) {
        Pixmap pixmap = new Pixmap(
            1, 1, Pixmap.Format.RGBA8888
        );

        pixmap.setColor(Color.valueOf(hexColor));
        pixmap.fill();

        Texture texture = new Texture(pixmap);
        pixmap.dispose();

        return texture;
    }

    @Override
    public void dispose() {
        close();
        root.remove();

        font.dispose();
        panelTexture.dispose();
        inputTexture.dispose();
        cursorTexture.dispose();
        selectionTexture.dispose();
    }
}
