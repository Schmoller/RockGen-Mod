package schmoller.mods.rockgen.recipes;

import com.google.gson.JsonPrimitive;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import schmoller.mods.rockgen.api.CurrentPlatform;

import java.util.function.Consumer;

interface BlockMatchTest {
    boolean test(BlockState state);
}

public record BlockMatcher(ResourceLocation location, boolean isTag, BlockMatchTest matcher) {
    BlockMatcher(ResourceLocation location, Block block) {
        this(location, false, state -> state.is(block));
    }

    BlockMatcher(TagKey<Block> tag) {
        this(tag.location(), true, state -> state.is(tag));
    }

    public static BlockMatcher createFromJson(JsonPrimitive element, String path) {
        if (!element.isString()) {
            throw new IllegalArgumentException("'" + path + "' property is not a string");
        }

        var blockIdOrTag = element.getAsString();

        if (blockIdOrTag.startsWith("#")) {
            // This is a block tag
            return new BlockMatcher(CurrentPlatform.getInstance().getPlatformHandlers().getOrCreateBlockTag(new ResourceLocation(blockIdOrTag.substring(1))));
        }

        // just a regular block id
        var blockId = new ResourceLocation(blockIdOrTag);
        var block = Registry.BLOCK.getOptional(blockId);
        if (block.isEmpty() || block.get() == Blocks.AIR) {
            throw new IllegalStateException("Unknown block " + blockId);
        }

        return new BlockMatcher(blockId, block.get());
    }

    public static BlockMatcher createFromByteBuf(FriendlyByteBuf input) {
        var idOrTag = input.readResourceLocation();
        var isTag = input.readBoolean();

        if (isTag) {
            return new BlockMatcher(CurrentPlatform.getInstance().getPlatformHandlers().getOrCreateBlockTag(idOrTag));
        }

        var block = Registry.BLOCK.getOptional(idOrTag);
        if (block.isEmpty()) {
            throw new IllegalStateException("Unknown block " + idOrTag);
        }

        return new BlockMatcher(idOrTag, block.get());
    }

    public void writeToByteBuf(FriendlyByteBuf output) {
        output.writeResourceLocation(location);
        output.writeBoolean(isTag);
    }

    /**
     * NOTE: This only matches against the default block state. This is ok for as long as the BlockMatcher only
     * supports id and tag based matching. If we add other forms of matching, we won't be able to use this
     *
     * @param callback Called for each registered block that matches
     */
    public void forEachMatchingRegisteredBlock(Consumer<Block> callback) {
        Registry.BLOCK.forEach(block -> {
            if (matches(block.defaultBlockState())) {
                callback.accept(block);
            }
        });
    }

    public boolean matches(BlockState state) {
        return matcher.test(state);
    }
}