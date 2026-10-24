/* 犟牛英雄扩展，遵循项目 GPL-3.0 许可证。 */
package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.noosa.ui.Component;

/** A bounded, scrollable notebook. Reading a long guide must not hide its last lines. */
public class WndCowNotes extends Window {
    private static final int MARGIN = 4;
    private ScrollPane notes;

    public WndCowNotes(String text) {
        super();

        int availableWidth = Math.max(1, PixelScene.uiCamera.width - chrome.marginHor() - 12);
        int width = Math.min(PixelScene.landscape() ? 220 : 160, availableWidth);
        int maxHeight = Math.max(1, PixelScene.uiCamera.height - chrome.marginVer() - 12);
        int contentWidth = Math.max(1, width - 2*MARGIN);
        float top = MARGIN;
        String[] paragraphs = text.split("\n\n", 2);
        String body = text;
        if (paragraphs.length == 2) {
            RenderedTextBlock heading = PixelScene.renderTextBlock(paragraphs[0], 7);
            heading.maxWidth(contentWidth);
            heading.setPos(MARGIN, MARGIN);
            add(heading);
            top = heading.bottom() + MARGIN;
            body = paragraphs[1];
        }

        RenderedTextBlock info = PixelScene.renderTextBlock(body, 6);
        info.maxWidth(Math.max(1, contentWidth - 3));
        info.setPos(0, 0);
        Component content = new Component();
        content.add(info);
        content.setSize(contentWidth, Math.max(1, info.height()));
        int viewportHeight = Math.max(1, Math.min((int)Math.ceil(content.height()),
                (int)Math.floor(maxHeight - top - MARGIN)));
        resize(width, (int)Math.ceil(viewportHeight + top + MARGIN));
        notes = new ScrollPane(content);
        add(notes);
        notes.setRect(MARGIN, top, contentWidth, viewportHeight);
    }

    public ScrollPane scrollPane() { return notes; }

    @Override public void resize(int width, int height) {
        super.resize(width, height);
        refreshScrollPane();
    }

    @Override public void offset(int xOffset, int yOffset) {
        super.offset(xOffset, yOffset);
        refreshScrollPane();
    }

    private void refreshScrollPane() {
        if (notes != null) notes.setRect(notes.left(), notes.top(), notes.width(), notes.height());
    }
}
