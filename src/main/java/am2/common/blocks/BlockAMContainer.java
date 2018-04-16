package am2.common.blocks;

import am2.common.defs.CreativeTabsDefs;
import am2.common.registry.Registry;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

public abstract class BlockAMContainer extends BlockContainer {

	protected BlockAMContainer(Material materialIn) {
		super(materialIn);
		setCreativeTab(CreativeTabsDefs.tabAM2Blocks);
	}
	
	public BlockAMContainer registerAndName(ResourceLocation rl) {
		this.setUnlocalizedName(rl.toString());
		Registry.GetBlocksToRegister().add(this);
		return this;
	}
	
	protected AxisAlignedBB boundingBox = new AxisAlignedBB(0, 0, 0, 1, 1, 1);
	
	public void setBlockBounds(float xStart, float yStart, float zStart, float xEnd, float yEnd, float zEnd) {
		boundingBox = new AxisAlignedBB(xStart, yStart, zStart, xEnd, yEnd, zEnd);
	}
	
	@Override
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
		return boundingBox;
	}
}
