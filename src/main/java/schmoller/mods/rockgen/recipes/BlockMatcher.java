package schmoller.mods.rockgen.recipes;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

record BlockMatcher(ResourceLocation location, BlockMatchTest matcher) {
    BlockMatcher(ResourceLocation location, Block block) {
        this(location, state -> state.is(block));
    }

    public boolean matches(BlockState state) {
        return matcher.test(state);
    }
}

interface BlockMatchTest {
    boolean test(BlockState state);
}