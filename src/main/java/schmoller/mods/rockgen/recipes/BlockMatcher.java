package schmoller.mods.rockgen.recipes;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public record BlockMatcher(ResourceLocation location, boolean isTag, BlockMatchTest matcher) {
    BlockMatcher(ResourceLocation location, Block block) {
        this(location, false, state -> state.is(block));
    }

    BlockMatcher(TagKey<Block> tag) {
        this(tag.location(), true, state -> state.is(tag));
    }

    public boolean matches(BlockState state) {
        return matcher.test(state);
    }
}

interface BlockMatchTest {
    boolean test(BlockState state);
}