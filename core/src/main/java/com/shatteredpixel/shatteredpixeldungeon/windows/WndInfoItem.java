/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.CowArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.CowhideArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CowBell;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.OxHorn;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.ItemSlot;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.noosa.ui.Component;

public class WndInfoItem extends Window {
	
	private static final float GAP	= 2;

	private static final int WIDTH_MIN = 120;
	private static final int WIDTH_MAX = 220;

	//only one WndInfoItem can appear at a time
	private static WndInfoItem INSTANCE;
	private ScrollPane descriptionPane;

	public WndInfoItem( Heap heap ) {

		super();

		if (INSTANCE != null){
			INSTANCE.hide();
		}
		INSTANCE = this;

		if (heap.type == Heap.Type.HEAP) {
			fillFields( heap.peek() );

		} else {
			fillFields( heap );

		}
	}
	
	public WndInfoItem( Item item ) {
		super();

		if (INSTANCE != null){
			INSTANCE.hide();
		}
		INSTANCE = this;
		
		fillFields( item );
	}

	@Override
	public void hide() {
		super.hide();
		if (INSTANCE == this){
			INSTANCE = null;
		}
	}

	private void fillFields(Heap heap ) {
		
		IconTitle titlebar = new IconTitle( heap );
		titlebar.color( TITLE_COLOR );
		
		RenderedTextBlock txtInfo = PixelScene.renderTextBlock( heap.info(), 6 );

		layoutFields(titlebar, txtInfo);
	}
	
	private void fillFields( Item item ) {
		
		int color = TITLE_COLOR;
		if (item.levelKnown && item.level() > 0) {
			color = ItemSlot.UPGRADED;
		} else if (item.levelKnown && item.level() < 0) {
			color = ItemSlot.DEGRADED;
		}

		IconTitle titlebar = new IconTitle( item );
		titlebar.color( color );
		
		RenderedTextBlock txtInfo = PixelScene.renderTextBlock( item.info(), 6 );

		if (item instanceof CowBell || item instanceof OxHorn
				|| item instanceof CowhideArmor || item instanceof CowArmor) {
			layoutCowFields(titlebar, txtInfo, item);
		} else {
			layoutFields(titlebar, txtInfo);
		}
	}

	/** Keep the title and WndUseItem's action rows fixed while the long description scrolls. */
	private void layoutCowFields(IconTitle title, RenderedTextBlock info, Item item) {
		int availableWidth = Math.max(1, PixelScene.uiCamera.width - chrome.marginHor() - 12);
		int width = Math.min(PixelScene.landscape() ? 220 : 160, availableWidth);
		int maxHeight = Math.max(1, PixelScene.uiCamera.height - chrome.marginVer() - 12);
		int buttonSpace = actionRowsHeight(item, width);
		title.setRect(0, 0, this instanceof WndUseItem ? width - 16 : width, 0);
		add(title);

		float top = title.bottom() + GAP;
		info.maxWidth(Math.max(1, width - 4));
		info.setPos(0, 0);
		Component content = new Component();
		content.add(info);
		content.setSize(width, Math.max(1, info.height()));
		int viewport = Math.max(1, Math.min((int)Math.ceil(content.height()),
				(int)Math.floor(maxHeight - buttonSpace - top - 2)));
		resize(width, (int)Math.ceil(viewport + top + 2));
		descriptionPane = new ScrollPane(content);
		add(descriptionPane);
		descriptionPane.setRect(0, top, width, viewport);
	}

	/** Match WndUseItem's measured button widths, one-pixel gaps and three-button row limit. */
	private int actionRowsHeight(Item item, int width) {
		if (!(this instanceof WndUseItem) || Dungeon.hero == null
				|| !Dungeon.hero.isAlive() || !Dungeon.hero.belongings.contains(item)) return 0;
		int rows = 0, inRow = 0;
		float remaining = width;
		for (String action : item.actions(Dungeon.hero)) {
			RenderedTextBlock label = PixelScene.renderTextBlock(item.actionName(action, Dungeon.hero), 8);
			float buttonWidth = label.width() + 2;
			label.destroy();
			if (inRow > 0 && remaining < buttonWidth + 1) {
				rows++;
				inRow = 0;
				remaining = width;
			}
			remaining -= buttonWidth + (inRow == 0 ? 0 : 1);
			inRow++;
			if (inRow >= 3 || remaining <= 0) {
				rows++;
				inRow = 0;
				remaining = width;
			}
		}
		if (inRow > 0) rows++;
		return rows*17 + 1;
	}

	@Override public void resize(int width, int height) {
		super.resize(width, height);
		refreshDescriptionPane();
	}

	@Override public void offset(int xOffset, int yOffset) {
		super.offset(xOffset, yOffset);
		refreshDescriptionPane();
	}

	private void refreshDescriptionPane() {
		if (descriptionPane != null) descriptionPane.setRect(descriptionPane.left(), descriptionPane.top(),
				descriptionPane.width(), descriptionPane.height());
	}

	private void layoutFields(IconTitle title, RenderedTextBlock info){
		int width = WIDTH_MIN;

		info.maxWidth(width);

		//window can go out of the screen on landscape, so widen it as appropriate
		while (PixelScene.landscape()
				&& info.height() > 100
				&& width < WIDTH_MAX){
			width += 20;
			info.maxWidth(width);
		}

		//leaves some space to add the journal button in WndUseItem. This is messy I know.
		if (this instanceof WndUseItem){
			title.setRect( 0, 0, width-16, 0 );
		} else {
			title.setRect( 0, 0, width, 0 );
		}
		add( title );

		info.setPos(title.left(), title.bottom() + GAP);
		add( info );

		resize( width, (int)(info.bottom() + 2) );
	}
}
