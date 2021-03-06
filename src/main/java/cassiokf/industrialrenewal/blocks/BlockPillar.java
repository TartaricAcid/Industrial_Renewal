package cassiokf.industrialrenewal.blocks;

import cassiokf.industrialrenewal.tileentity.alarm.BlockAlarm;
import com.google.common.collect.ImmutableList;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BlockPillar extends BlockBase {

    public static final ImmutableList<IProperty<Boolean>> CONNECTED_PROPERTIES = ImmutableList.copyOf(
            Stream.of(EnumFacing.VALUES).map(facing -> PropertyBool.create(facing.getName())).collect(Collectors.toList()));
    private static float NORTHZ1 = 0.250f;
    private static float SOUTHZ2 = 0.750f;
    private static float WESTX1 = 0.250f;
    private static float EASTX2 = 0.750f;
    private static float DOWNY1 = 0.0f;
    private static float UPY2 = 1.0f;

    public BlockPillar(String name, CreativeTabs tab) {
        super(Material.IRON, name, tab);
        setSoundType(SoundType.METAL);
        setHardness(0.8f);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, CONNECTED_PROPERTIES.toArray(new IProperty[CONNECTED_PROPERTIES.size()]));
    }

    @SuppressWarnings("deprecation")
    @Override
    public IBlockState getStateFromMeta(final int meta) {
        return getDefaultState();
    }

    @Override
    public int getMetaFromState(final IBlockState state) {
        return 0;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean isOpaqueCube(final IBlockState state) {
        return false;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean isFullCube(final IBlockState state) {
        return false;
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
        if (player.inventory.getCurrentItem().getItem() == ItemBlock.getItemFromBlock(ModBlocks.pillar)) {
            Integer n = 1;
            while (world.getBlockState(pos.up(n)).getBlock() instanceof BlockPillar) {
                n++;
            }
            if (world.getBlockState(pos.up(n)).getBlock().isAir(world.getBlockState(pos.up(n)), world, pos.up(n))) {
                world.setBlockState(pos.up(n), ModBlocks.pillar.getDefaultState(), 3);
                if (!player.isCreative()) {
                    player.inventory.clearMatchingItems(net.minecraft.item.ItemBlock.getItemFromBlock(ModBlocks.pillar), 0, 1, null);
                }
                return true;
            }
            return false;
        }
        return false;
    }
    /**
     * Is the neighbouring block a valid connection for this pipe?
     *
     * @param ownState           This pipe's state
     * @param neighbourState     The neighbouring block's state
     * @param world              The world
     * @param ownPos             This pipe's position
     * @param neighbourDirection The direction of the neighbouring block
     * @return Is the neighbouring block a valid connection?
     */
    protected boolean isValidConnection(final IBlockState ownState, final IBlockState neighbourState, final IBlockAccess world, final BlockPos ownPos, final EnumFacing neighbourDirection) {
        Block nb = neighbourState.getBlock();
        if (neighbourDirection != EnumFacing.UP && neighbourDirection != EnumFacing.DOWN) {
            return nb instanceof BlockLever
                    || nb instanceof BlockRedstoneTorch
                    || nb instanceof BlockTripWireHook
                    || nb instanceof BlockColumn
                    || nb instanceof BlockLadder
                    || (nb instanceof BlockLight && neighbourState.getValue(BlockLight.FACING) == neighbourDirection.getOpposite())
                    || nb instanceof BlockRoof
                    || (nb instanceof BlockBrace && Objects.equals(neighbourState.getValue(BlockBrace.FACING).getName(), neighbourDirection.getOpposite().getName()))
                    || (nb instanceof BlockBrace && Objects.equals(neighbourState.getValue(BlockBrace.FACING).getName(), "down_" + neighbourDirection.getName()))
                    || (nb instanceof BlockAlarm && neighbourState.getValue(BlockAlarm.FACING) == neighbourDirection)
                    || (nb instanceof BlockSignBase && neighbourState.getValue(BlockSignBase.ONWALL) && Objects.equals(neighbourState.getValue(BlockSignBase.FACING).getName(), neighbourDirection.getOpposite().getName()))
                    || Objects.requireNonNull(nb.getRegistryName()).toString().matches("immersiveengineering:connector")
                    || Objects.requireNonNull(nb.getRegistryName()).toString().matches("immersiveengineering:metal_decoration2")
                    || Objects.requireNonNull(nb.getRegistryName()).toString().matches("immersiveengineering:wooden_device1")
                    || Objects.requireNonNull(nb.getRegistryName()).toString().matches("immersiveengineering:metal_device1")
                    //start Industrial floor side connection
                    || nb instanceof BlockIndustrialFloor || nb instanceof BlockFloorLamp
                    || nb instanceof BlockFloorPipe || nb instanceof BlockFloorCable;
            //end
        }
        if (neighbourDirection == EnumFacing.DOWN) {
            return nb.isFullCube(neighbourState)
                    || nb.isTopSolid(neighbourState);
        }
        return nb.isFullCube(neighbourState) || nb instanceof BlockIndustrialFloor || nb instanceof BlockFloorLamp
                || nb instanceof BlockFloorPipe || nb instanceof BlockFloorCable || nb instanceof BlockCatWalk;
    }

    /**
     * Can this pipe connect to the neighbouring block?
     *
     * @param ownState           This pipe's state
     * @param worldIn            The world
     * @param ownPos             This pipe's position
     * @param neighbourDirection The direction of the neighbouring block
     * @return Can this pipe connect?
     */
    private boolean canConnectTo(final IBlockState ownState, final IBlockAccess worldIn, final BlockPos ownPos, final EnumFacing neighbourDirection) {
        final BlockPos neighbourPos = ownPos.offset(neighbourDirection);
        final IBlockState neighbourState = worldIn.getBlockState(neighbourPos);
        final Block neighbourBlock = neighbourState.getBlock();

        final boolean neighbourIsValidForThis = isValidConnection(ownState, neighbourState, worldIn, ownPos, neighbourDirection);
        final boolean thisIsValidForNeighbour = !(neighbourBlock instanceof BlockPillar) || ((BlockPillar) neighbourBlock).isValidConnection(neighbourState, ownState, worldIn, neighbourPos, neighbourDirection.getOpposite());

        return neighbourIsValidForThis && thisIsValidForNeighbour;
    }

    @SuppressWarnings("deprecation")
    @Override
    public IBlockState getActualState(IBlockState state, final IBlockAccess world, final BlockPos pos) {
        for (final EnumFacing facing : EnumFacing.VALUES) {
            state = state.withProperty(CONNECTED_PROPERTIES.get(facing.getIndex()),
                    canConnectTo(state, world, pos, facing));
        }
        return state;
    }

    public final boolean isConnected(final IBlockState state, final EnumFacing facing) {
        return state.getValue(CONNECTED_PROPERTIES.get(facing.getIndex()));
    }

    @SuppressWarnings("deprecation")
    @Override
    public void addCollisionBoxToList(IBlockState state, final World worldIn, final BlockPos pos, final AxisAlignedBB entityBox, final List<AxisAlignedBB> collidingBoxes, @Nullable final Entity entityIn, final boolean isActualState) {
        if (!isActualState) {
            state = state.getActualState(worldIn, pos);
        }
        if (isConnected(state, EnumFacing.NORTH)) {
            NORTHZ1 = 0.0f;
        } else if (!isConnected(state, EnumFacing.NORTH)) {
            NORTHZ1 = 0.250f;
        }
        if (isConnected(state, EnumFacing.SOUTH)) {
            SOUTHZ2 = 1.0f;
        } else if (!isConnected(state, EnumFacing.SOUTH)) {
            SOUTHZ2 = 0.750f;
        }
        if (isConnected(state, EnumFacing.WEST)) {
            WESTX1 = 0.0f;
        } else if (!isConnected(state, EnumFacing.WEST)) {
            WESTX1 = 0.250f;
        }
        if (isConnected(state, EnumFacing.EAST)) {
            EASTX2 = 1.0f;
        } else if (!isConnected(state, EnumFacing.EAST)) {
            EASTX2 = 0.750f;
        }
        final AxisAlignedBB AA_BB = new AxisAlignedBB(WESTX1, DOWNY1, NORTHZ1, EASTX2, UPY2, SOUTHZ2);
        addCollisionBoxToList(pos, entityBox, collidingBoxes, AA_BB);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        IBlockState actualState = state.getActualState(source, pos);

        if (isConnected(actualState, EnumFacing.NORTH)) {
            NORTHZ1 = 0.0f;
        } else if (!isConnected(actualState, EnumFacing.NORTH)) {
            NORTHZ1 = 0.250f;
        }
        if (isConnected(actualState, EnumFacing.SOUTH)) {
            SOUTHZ2 = 1.0f;
        } else if (!isConnected(actualState, EnumFacing.SOUTH)) {
            SOUTHZ2 = 0.750f;
        }
        if (isConnected(actualState, EnumFacing.WEST)) {
            WESTX1 = 0.0f;
        } else if (!isConnected(actualState, EnumFacing.WEST)) {
            WESTX1 = 0.250f;
        }
        if (isConnected(actualState, EnumFacing.EAST)) {
            EASTX2 = 1.0f;
        } else if (!isConnected(actualState, EnumFacing.EAST)) {
            EASTX2 = 0.750f;
        }
        return new AxisAlignedBB(WESTX1, DOWNY1, NORTHZ1, EASTX2, UPY2, SOUTHZ2);
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
        if (face == EnumFacing.EAST || face == EnumFacing.WEST || face == EnumFacing.NORTH || face == EnumFacing.SOUTH) {
            return BlockFaceShape.SOLID;
        } else {
            return BlockFaceShape.UNDEFINED;
        }
    }
}
