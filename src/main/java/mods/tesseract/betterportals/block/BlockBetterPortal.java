package mods.tesseract.betterportals.block;

import mods.tesseract.betterportals.BetterPortals;
import net.minecraft.block.Block;
import net.minecraft.block.BlockPortal;
import net.minecraft.block.BlockStainedHardenedClay;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.server.SPacketPlayerPosLook;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import java.util.EnumSet;
import java.util.Set;

public class BlockBetterPortal extends BlockPortal {
    public final int portalCooldown = -500;

    public static final PropertyEnum<EnumType> VARIANT = PropertyEnum.<EnumType>create("variant", EnumType.class);

    public BlockBetterPortal() {
        super();
        this.setDefaultState(this.blockState.getBaseState().withProperty(AXIS, EnumFacing.Axis.X).withProperty(VARIANT, EnumType.NETHER));
        this.setTickRandomly(true);
        setSoundType(SoundType.GLASS);
        setUnlocalizedName("portal");
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, AXIS, VARIANT);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        IBlockState b;
        if (meta >= EnumType.length) {
            meta -= EnumType.length;
            b = this.getDefaultState().withProperty(AXIS, EnumFacing.Axis.Z);
        } else {
            b = this.getDefaultState().withProperty(AXIS, EnumFacing.Axis.X);
        }
        return b.withProperty(VARIANT, EnumType.byMetadata(meta));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int a = state.getValue(VARIANT).getMetadata();
        if (state.getValue(AXIS) == EnumFacing.Axis.Z)
            a += EnumType.length;
        return a;
    }

    @Override
    public boolean trySpawnPortal(World worldIn, BlockPos pos) {
        BlockBetterPortal.Size size = new BlockBetterPortal.Size(worldIn, pos, EnumFacing.Axis.X);

        if (size.isValid() && size.portalBlockCount == 0) {
            size.placePortalBlocks();
            return true;
        } else {
            size = new BlockBetterPortal.Size(worldIn, pos, EnumFacing.Axis.Z);

            if (size.isValid() && size.portalBlockCount == 0) {
                size.placePortalBlocks();
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    public void onEntityCollidedWithBlock(World worldIn, BlockPos pos, IBlockState state, Entity entityIn) {
        if (!entityIn.isRiding() && !entityIn.isBeingRidden() && entityIn.isNonBoss()) {
            int d = worldIn.provider.getDimension();
            EnumType t = state.getValue(VARIANT);
            if (t == EnumType.NETHER) {
                if (d == 0 || d == -1) {
                    entityIn.setPortal(pos);
                }
            } else {
                entityIn.inPortal = true;
                entityIn.portalCounter = 0;
                if (entityIn.timeUntilPortal == 0) {
                    entityIn.timeUntilPortal = portalCooldown;
                } else if (++entityIn.timeUntilPortal >= 0) {
                    BlockPos p;
                    if (t == EnumType.RETURN) {
                        p = worldIn.getTopSolidOrLiquidBlock(worldIn.getSpawnPoint());
                    } else {
                        BlockBetterPortal.Size size = new BlockBetterPortal.Size(worldIn, pos, EnumFacing.Axis.X);
                        if (!size.isValid() || size.portalBlockCount <= 0 || !size.isValidGateway()) {
                            size = new Size(worldIn, pos, EnumFacing.Axis.Z);
                            if (!size.isValid() || size.portalBlockCount <= 0 || !size.isValidGateway()) {
                                worldIn.destroyBlock(pos, true);
                                return;
                            }
                        }
                        double a = (double) size.calculateStele(size.height) / 128 * Math.PI;
                        int l = (size.calculateStele(-1) + 1) * 100;
                        p = getTopBlock(worldIn, pos.add(Math.cos(a) * l, 0, Math.sin(a) * l));
                    }
                    doTeleport(entityIn, p.getX() + 0.5, p.getY() + 1, p.getZ() + 0.5, 0, 0);
                }
            }
        }
    }

    private static void doTeleport(Entity e, double x, double y, double z, int yaw, int pitch) {
        if (e instanceof EntityPlayerMP) {
            Set<SPacketPlayerPosLook.EnumFlags> set = EnumSet.<SPacketPlayerPosLook.EnumFlags>noneOf(SPacketPlayerPosLook.EnumFlags.class);

            e.dismountRidingEntity();
            ((EntityPlayerMP) e).connection.setPlayerLocation(x, y, z, yaw, pitch, set);
            e.setRotationYawHead(pitch);
        } else {
            float f2 = (float) MathHelper.wrapDegrees(yaw);
            float f3 = (float) MathHelper.wrapDegrees(pitch);
            f3 = MathHelper.clamp(f3, -90.0F, 90.0F);
            e.setLocationAndAngles(x, y, z, f2, f3);
            e.setRotationYawHead(f2);
        }

        if (!(e instanceof EntityLivingBase) || !((EntityLivingBase) e).isElytraFlying()) {
            e.motionY = 0.0D;
            e.onGround = true;
        }
    }

    private static BlockPos getTopBlock(World w, BlockPos pos) {
        Chunk chunk = w.getChunkFromBlockCoords(pos);
        BlockPos blockpos;
        BlockPos blockpos1;
        for (blockpos = new BlockPos(pos.getX(), chunk.getTopFilledSegment() + 16, pos.getZ()); blockpos.getY() >= 0; blockpos = blockpos1) {
            blockpos1 = blockpos.down();
            IBlockState state = chunk.getBlockState(blockpos1);

            if (state.getMaterial() != Material.AIR) {
                break;
            }
        }
        return blockpos;
    }

    public static class Size {
        private final World world;
        private final EnumFacing.Axis axis;
        private final EnumFacing rightDir;
        private final EnumFacing leftDir;
        private int portalBlockCount;
        private BlockPos bottomLeft;
        public int height;
        public int width;

        private Size(World worldIn, BlockPos p, EnumFacing.Axis x) {
            world = worldIn;
            axis = x;

            if (x == EnumFacing.Axis.X) {
                leftDir = EnumFacing.EAST;
                rightDir = EnumFacing.WEST;
            } else {
                leftDir = EnumFacing.NORTH;
                rightDir = EnumFacing.SOUTH;
            }

            for (BlockPos blockpos = p; p.getY() > blockpos.getY() - 21 && p.getY() > 0 && isEmptyBlock(worldIn.getBlockState(p.down()).getBlock()); p = p.down()) {
                ;
            }

            int i = getDistanceUntilEdge(p, leftDir) - 1;

            if (i >= 0) {
                bottomLeft = p.offset(leftDir, i);
                width = getDistanceUntilEdge(bottomLeft, rightDir);

                if (width < 2 || width > 21) {
                    bottomLeft = null;
                    width = 0;
                }
            }

            if (bottomLeft != null) {
                height = calculatePortalHeight();
            }
        }

        private int getDistanceUntilEdge(BlockPos p, EnumFacing f) {
            int i;

            for (i = 0; i < 22; ++i) {
                BlockPos blockpos = p.offset(f, i);

                if (!isEmptyBlock(world.getBlockState(blockpos).getBlock()) || world.getBlockState(blockpos.down()).getBlock() != Blocks.OBSIDIAN) {
                    break;
                }
            }

            Block block = world.getBlockState(p.offset(f, i)).getBlock();
            return block == Blocks.OBSIDIAN ? i : 0;
        }

        private int calculatePortalHeight() {
            label56:

            for (height = 0; height < 21; ++height) {
                for (int i = 0; i < width; ++i) {
                    BlockPos blockpos = bottomLeft.offset(rightDir, i).up(height);
                    Block block = world.getBlockState(blockpos).getBlock();

                    if (!isEmptyBlock(block)) {
                        break label56;
                    }

                    if (block == Blocks.PORTAL) {
                        ++portalBlockCount;
                    }

                    if (i == 0) {
                        block = world.getBlockState(blockpos.offset(leftDir)).getBlock();

                        if (block != Blocks.OBSIDIAN) {
                            break label56;
                        }
                    } else if (i == width - 1) {
                        block = world.getBlockState(blockpos.offset(rightDir)).getBlock();

                        if (block != Blocks.OBSIDIAN) {
                            break label56;
                        }
                    }
                }
            }

            for (int j = 0; j < width; ++j) {
                if (world.getBlockState(bottomLeft.offset(rightDir, j).up(height)).getBlock() != Blocks.OBSIDIAN) {
                    height = 0;
                    break;
                }
            }

            if (height <= 21 && height >= 3) {
                return height;
            } else {
                bottomLeft = null;
                width = 0;
                height = 0;
                return 0;
            }
        }

        private boolean isEmptyBlock(Block blockIn) {
            return blockIn == Blocks.AIR || blockIn == Blocks.FIRE || blockIn == Blocks.PORTAL;
        }

        public boolean isValid() {
            return bottomLeft != null && width >= 2 && width <= 21 && height >= 3 && height <= 21;
        }

        public boolean isValidGateway() {
            return isSteleBlock(world, bottomLeft.offset(rightDir, -1).down()) && isSteleBlock(world, bottomLeft.offset(rightDir, width).down()) && isSteleBlock(world, bottomLeft.offset(rightDir, -1).up(height)) && isSteleBlock(world, bottomLeft.offset(rightDir, width).up(height));
        }

        public int calculateStele(int y) {
            IBlockState s = world.getBlockState(bottomLeft.offset(rightDir, -1).up(y)), t = world.getBlockState(bottomLeft.offset(rightDir, width).up(y));
            return s.getBlock().getMetaFromState(s) << 4 | t.getBlock().getMetaFromState(t);
        }

        public void placePortalBlocks() {
            IBlockState b = Blocks.PORTAL.getDefaultState();
            boolean f = true;
            switch (world.provider.getDimension()) {
                case 0:
                    for (int i = 0; i < width; ++i) {
                        if (world.getBlockState(bottomLeft.offset(rightDir, i).down(2)) == Blocks.BEDROCK.getDefaultState()) {
                            f = false;
                            break;
                        }
                    }
                    break;
                case -1:
                    f = false;
            }

            if (BetterPortals.config.GATEWAY_PORTAL && isValidGateway()) {
                b = b.withProperty(BlockBetterPortal.VARIANT, EnumType.GATEWAY);
            } else if (BetterPortals.config.RETURN_PORTAL && f) {
                b = b.withProperty(BlockBetterPortal.VARIANT, EnumType.RETURN);
            } else {
                b = b.withProperty(BlockBetterPortal.VARIANT, EnumType.NETHER);
            }
            for (int i = 0; i < width; ++i) {
                BlockPos blockpos = bottomLeft.offset(rightDir, i);

                for (int j = 0; j < height; ++j) {
                    world.setBlockState(blockpos.up(j), b.withProperty(BlockPortal.AXIS, axis), 2);
                }
            }
        }
    }

    private static boolean isSteleBlock(World w, BlockPos p) {
        return w.getBlockState(p).getBlock() instanceof BlockStainedHardenedClay;
    }

    public enum EnumType implements IStringSerializable {

        NETHER(0, "nether"),
        RETURN(1, "return"),
        GATEWAY(2, "gateway");

        private static final BlockBetterPortal.EnumType[] METADATA_LOOKUP = new BlockBetterPortal.EnumType[values().length];
        public static final int length = values().length;
        private final int metadata;
        private final String name;

        EnumType(int metadataIn, String nameIn) {
            metadata = metadataIn;
            name = nameIn;
        }

        public int getMetadata() {
            return metadata;
        }

        public String toString() {
            return name;
        }

        public static BlockBetterPortal.EnumType byMetadata(int metadata) {
            if (metadata < 0 || metadata >= METADATA_LOOKUP.length) {
                metadata = 0;
            }

            return METADATA_LOOKUP[metadata];
        }

        public String getName() {
            return name;
        }

        static {
            for (BlockBetterPortal.EnumType v : values()) {
                METADATA_LOOKUP[v.getMetadata()] = v;
            }
        }
    }
}
