package schmoller.mods.rockgen.util;

import com.google.gson.JsonPrimitive;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;

public final class SerializationUtil {
    private SerializationUtil() {
    }

    public static Tuple<Block, ResourceLocation> decodeBlockFromPrimitive(JsonPrimitive primitive, String path) {
        if (primitive == null || !primitive.isString()) {
            throw new IllegalArgumentException("'" + path + "' property is not a string");
        }

        var blockId = new ResourceLocation(primitive.getAsString());
        var block = ForgeRegistries.BLOCKS.getValue(blockId);
        if (block == null || block == Blocks.AIR) {
            throw new IllegalStateException("Unknown block " + blockId);
        }

        return new Tuple<>(block, blockId);
    }
}
