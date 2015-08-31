package am2.blocks;

import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import am2.blocks.tileentities.TileEntitySlipstreamGenerator;
import am2.texture.ResourceManager;

public class BlockSlipstreamGenerator extends PoweredBlock{

	public BlockSlipstreamGenerator() {
		super(Material.wood);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int i) {
		return new TileEntitySlipstreamGenerator();
	}

	@Override
	public void registerBlockIcons(IIconRegister par1IconRegister) {
		this.blockIcon = ResourceManager.RegisterTexture("slipstreamGenerator", par1IconRegister);
	}

	@Override
	public int getRenderBlockPass() {
		return 1;
	}

	@Override
	public boolean isOpaqueCube() {
		return false;
	}
}
