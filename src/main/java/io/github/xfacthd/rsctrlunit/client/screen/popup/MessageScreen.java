package io.github.xfacthd.rsctrlunit.client.screen.popup;

import io.github.xfacthd.rsctrlunit.client.util.ClientUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.*;

public sealed class MessageScreen extends Screen permits ConfirmationScreen
{
    private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("textures/gui/demo_background.png");
    public static final Component INFO_TITLE = Component.translatable("title.rsctrlunit.message.info");
    public static final Component ERROR_TITLE = Component.translatable("title.rsctrlunit.message.error");
    public static final Component CONFIRM_TITLE = Component.translatable("title.rsctrlunit.message.confirm");
    protected static final int WIDTH = 176;
    private static final int BASE_HEIGHT = 64;
    protected static final int TEXT_WIDTH = WIDTH - 12;
    private static final int BTN_WIDTH = 60;
    protected static final int BTN_HEIGHT = 20;
    protected static final int BTN_BOTTOM_OFFSET = 6 + BTN_HEIGHT;
    private static final int TITLE_X = 8;
    private static final int TITLE_Y = 6;

    private final List<Component> messages;
    private final List<List<FormattedCharSequence>> textBlocks = new ArrayList<>();
    protected int leftPos;
    protected int topPos;
    protected int imageHeight;

    public static MessageScreen info(List<Component> message)
    {
        return new MessageScreen(INFO_TITLE, message);
    }

    public static MessageScreen error(List<Component> message)
    {
        return new MessageScreen(ERROR_TITLE, message);
    }

    public static MessageScreen confirm(List<Component> message, Runnable action)
    {
        return new ConfirmationScreen(message, action);
    }

    public MessageScreen(Component title, List<Component> messages)
    {
        super(title);
        this.messages = messages;
    }

    @Override
    protected void init()
    {
        textBlocks.clear();

        imageHeight = BASE_HEIGHT;
        for (Component msg : messages)
        {
            imageHeight += ClientUtils.getWrappedHeight(font, msg, TEXT_WIDTH);
            imageHeight += font.lineHeight;

            textBlocks.add(font.split(msg, TEXT_WIDTH));
        }
        imageHeight -= font.lineHeight;

        leftPos = (this.width - WIDTH) / 2;
        topPos = (this.height - imageHeight) / 2;

        addButtons();
    }

    protected void addButtons()
    {
        addRenderableWidget(Button.builder(CommonComponents.GUI_OK, btn -> onClose())
                .pos(leftPos + (WIDTH / 2) - (BTN_WIDTH / 2), topPos + imageHeight - BTN_BOTTOM_OFFSET)
                .size(BTN_WIDTH, BTN_HEIGHT)
                .build()
        );
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks)
    {
        super.renderBackground(graphics, mouseX, mouseY, partialTicks);

        graphics.blitWithBorder(BACKGROUND, leftPos, topPos, 0, 0, WIDTH, imageHeight, 248, 166, 4, 4, 4, 4);
        graphics.drawString(font, title, leftPos + TITLE_X, topPos + TITLE_Y, 0x404040, false);

        int y = topPos + TITLE_Y + font.lineHeight * 2;
        for (List<FormattedCharSequence> block : textBlocks)
        {
            for (FormattedCharSequence line : block)
            {
                graphics.drawString(font, line, leftPos + TITLE_X, y, 0, false);
                y += font.lineHeight;
            }
            y += font.lineHeight;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks)
    {
        super.render(graphics, mouseX, mouseY, partialTicks);

        Style style = findTextLine(mouseX, mouseY);
        if (style != null)
        {
            graphics.renderComponentHoverEffect(font, style, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        Style style = findTextLine((int) mouseX, (int) mouseY);
        if (style != null && handleComponentClicked(style))
        {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private Style findTextLine(int mouseX, int mouseY)
    {
        int localX = mouseX - leftPos - TITLE_X;
        if (localX < 0) { return null; }

        int y = topPos + TITLE_Y + font.lineHeight * 2;
        for (List<FormattedCharSequence> block : textBlocks)
        {
            int height = block.size() * font.lineHeight;
            if (mouseY >= y && mouseY <= y + height)
            {
                int idx = (mouseY - y) / font.lineHeight;
                if (idx >= block.size()) { return null; }
                return font.getSplitter().componentStyleAtWidth(block.get(idx), localX);
            }

            y += height + font.lineHeight;
        }
        return null;
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}