package am2.items;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.init.Blocks;
import am2.texture.ResourceManager;

public class ItemFocusCharge extends ItemFocus {

	public ItemFocusCharge() {
		super();
	}

	@Override
	public Object[] getRecipeItems() {
		return new Object[]{
			"CFC",
			'F', ItemsCommonProxy.standardFocus,
			'C', Blocks.glass
		};
	}

	@Override
	public String getInGameName(){
		return "Charge Focus";
	}

	@Override
	public void registerIcons(IIconRegister par1IconRegister) {
		this.itemIcon = ResourceManager.RegisterTexture("focus_machinery_charge", par1IconRegister);
	}

}
