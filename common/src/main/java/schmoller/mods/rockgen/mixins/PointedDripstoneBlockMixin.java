package schmoller.mods.rockgen.mixins;

import com.google.common.annotations.VisibleForTesting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import schmoller.mods.rockgen.api.CurrentPlatform;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Predicate;

@Mixin(PointedDripstoneBlock.class)
public class PointedDripstoneBlockMixin {
    /**
     * @author Schmoller
     * @reason Replaces the dripstone logic with a recipe based system
     */
    @VisibleForTesting
    @Overwrite
    public static void maybeTransferFluid(
        BlockState blockState, ServerLevel level, BlockPos position, float randomNumber
    ) {
        final float LavaDripProbability = 0.05859375F;
        final float WaterDripProbability = 0.17578125F;

        if (randomNumber > CurrentPlatform.getInstance().getDripstoneDrainRecipeCache().getMaxProbability() && randomNumber > LavaDripProbability && randomNumber > WaterDripProbability) {
            return;
        }

        // Only the top block of a stalactite can perform the transfer
        if (!isStalactiteStartPos(blockState, level, position)) {
            return;
        }

        var rootBlock = position.above();
        var drainBlock = rootBlock.above();

        var drainBlockState = level.getBlockState(drainBlock);

        // Some recipes (and standard cauldron filling) requires fluid info
        var drainFluid = level.getFluidState(drainBlock);

        for (var recipe : CurrentPlatform.getInstance().getDripstoneDrainRecipeCache().get(drainBlockState, level)) {
            var blockToSet = recipe.tryMatch(drainBlockState, level, randomNumber);
            if (blockToSet.isEmpty()) {
                continue;
            }

            BlockPos tipPosition = findTip(blockState, level, position, 11, false);

            if (tipPosition == null) {
                continue;
            }

            BlockState newBlockState = blockToSet.get().defaultBlockState();
            level.setBlockAndUpdate(drainBlock, newBlockState);
            Block.pushEntitiesUp(drainBlockState, newBlockState, level, drainBlock);
            level.gameEvent(GameEvent.BLOCK_CHANGE, drainBlock, GameEvent.Context.of(newBlockState));
            level.levelEvent(1504, tipPosition, 0);

            return;
        }

        if (drainFluid.getType() == Fluids.WATER) {
            if (randomNumber > WaterDripProbability) {
                return;
            }
        } else if (drainFluid.getType() == Fluids.LAVA) {
            if (randomNumber > LavaDripProbability) {
                return;
            }
        } else {
            return;
        }

        // Try to fill cauldrons
        BlockPos tipPosition = findTip(blockState, level, position, 11, false);

        if (tipPosition == null) {
            return;
        }

        BlockPos cauldronPosition = findFillableCauldronBelowStalactiteTip(level, tipPosition, drainFluid.getType());
        if (cauldronPosition == null) {
            return;
        }

        level.levelEvent(1504, tipPosition, 0);
        int i = tipPosition.getY() - cauldronPosition.getY();
        int j = 50 + i;
        BlockState blockstate = level.getBlockState(cauldronPosition);
        level.scheduleTick(cauldronPosition, blockstate.getBlock(), j);
    }

    @Shadow
    private static boolean isStalactiteStartPos(BlockState p_154204_, LevelReader p_154205_, BlockPos p_154206_) {
        return false;
    }

    @Shadow
    private static BlockPos findTip(
        BlockState p_154131_, LevelAccessor p_154132_, BlockPos p_154133_, int p_154134_, boolean p_154135_
    ) {
        return null;
    }

    @Shadow
    private static BlockPos findFillableCauldronBelowStalactiteTip(
        Level p_154077_, BlockPos p_154078_, Fluid p_154079_
    ) {
        return null;
    }

    /**
     * @author Schmoller
     * @reason To replace the particles with something appropriate for the given recipe
     */
    @Overwrite
    public static void spawnDripParticle(Level level, BlockPos position, BlockState blockState) {
        maybeSpawnParticle(level, position, blockState, null);
    }

    private static void maybeSpawnParticle(
        Level level, BlockPos position, BlockState blockState, @Nullable Predicate<Fluid> shouldSpawn
    ) {
        if (!isStalactite(blockState)) {
            return;
        }

        var optionalRootBlock = findRootBlock(level, position, blockState, 11);

        if (optionalRootBlock.isEmpty()) {
            return;
        }

        var rootBlock = optionalRootBlock.get();
        var drainBlock = rootBlock.above();
        var fluidBeingDrained = level.getFluidState(drainBlock).getType();

        ParticleOptions outputParticle = null;

        if (fluidBeingDrained.isSame(Fluids.EMPTY)) {
            // Try to get the particle from a matching recipe
            var drainBlockState = level.getBlockState(drainBlock);

            for (var recipe : CurrentPlatform.getInstance().getDripstoneDrainRecipeCache().get(drainBlockState, level)) {
                if (!recipe.doesMatch(drainBlockState, level)) {
                    continue;
                }

                outputParticle = recipe.getParticleType();
                break;
            }
        }

        if (outputParticle == null) {
            // Use the standard particle type
            Fluid fluid = getDripFluid(level, fluidBeingDrained);
            if (fluid.is(FluidTags.LAVA)) {
                outputParticle = ParticleTypes.DRIPPING_DRIPSTONE_LAVA;
            } else {
                outputParticle = ParticleTypes.DRIPPING_DRIPSTONE_WATER;
            }

            // Having a recipe will always spawn the particles, otherwise, the test is required
            if (shouldSpawn != null && !shouldSpawn.test(fluid)) {
                return;
            }
        }

        // Spawn the particle
        Vec3 offsetPosition = blockState.getOffset(level, position);
        double d0 = 0.0625D;
        double d1 = (double) position.getX() + 0.5D + offsetPosition.x;
        double d2 = (double) ((float) (position.getY() + 1) - 0.6875F) - 0.0625D;
        double d3 = (double) position.getZ() + 0.5D + offsetPosition.z;
        level.addParticle(outputParticle, d1, d2, d3, 0.0D, 0.0D, 0.0D);
    }

    @Shadow
    private static boolean isStalactite(BlockState p_154241_) {
        return false;
    }

    @Shadow
    private static Optional<BlockPos> findRootBlock(
        Level p_154067_, BlockPos p_154068_, BlockState p_154069_, int p_154070_
    ) {
        return Optional.empty();
    }

    @Shadow
    private static Fluid getDripFluid(Level p_154053_, Fluid p_154054_) {
        return null;
    }

    @Inject(method = "animateTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/PointedDripstoneBlock;getFluidAboveStalactite(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Ljava/util/Optional;"), cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD)
    private void animateTickSpawnParticle(
        BlockState blockState, Level level, BlockPos position, RandomSource random, CallbackInfo context, float f
    ) {
        maybeSpawnParticle(level, position, blockState, fluid -> f < 0.02F || canFillCauldron(fluid));

        // Never want to do the normal spawn routine
        context.cancel();
    }

    @Shadow
    private static boolean canFillCauldron(Fluid p_154159_) {
        return false;
    }
}
