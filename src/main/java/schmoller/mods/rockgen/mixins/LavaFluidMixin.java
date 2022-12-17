package schmoller.mods.rockgen.mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.LavaFluid;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LavaFluid.class)
public abstract class LavaFluidMixin extends FlowingFluid {
    /**
     * @author Steven Schmoll
     * @reason To take control of stone generation
     */
    @Overwrite
    protected void spreadTo(@NotNull LevelAccessor p_76220_, @NotNull BlockPos p_76221_, @NotNull BlockState p_76222_, @NotNull Direction p_76223_, @NotNull FluidState p_76224_) {
        // Get rid of stone generation here. We will make this more generic for FlowingFluid
        super.spreadTo(p_76220_, p_76221_, p_76222_, p_76223_, p_76224_);
    }

    @Shadow
    private void fizz(LevelAccessor p_76213_, BlockPos p_76214_) {
    }
}
