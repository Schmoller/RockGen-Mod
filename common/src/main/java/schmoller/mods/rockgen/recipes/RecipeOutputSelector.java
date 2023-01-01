package schmoller.mods.rockgen.recipes;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import schmoller.mods.rockgen.util.SerializationUtil;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public final class RecipeOutputSelector {
    private final List<ResultBlock> outputs;
    private final int maxOutputWeight;
    private final Random random = new Random();

    public RecipeOutputSelector(List<ResultBlock> outputs) {
        assert !outputs.isEmpty() : "Must be at least one output";

        this.outputs = outputs;

        int outputWeight = 0;
        for (var output : outputs) {
            outputWeight += output.weight;
        }
        maxOutputWeight = outputWeight;
    }

    public static RecipeOutputSelector createFromJson(JsonElement element, String path) {
        if (element == null) {
            throw new IllegalArgumentException("'" + path + "' property is not string or array");
        }

        List<ResultBlock> outputs;
        if (element.isJsonPrimitive()) {
            var outputBlock = SerializationUtil.decodeBlockFromPrimitive(element.getAsJsonPrimitive(), path);
            outputs = List.of(new ResultBlock(outputBlock.getA(), outputBlock.getB(), 1));
        } else if (element.isJsonArray()) {
            var outputArrayProperty = element.getAsJsonArray();

            outputs = StreamSupport
                .stream(outputArrayProperty.spliterator(), false)
                .map(item -> ResultBlock.decodeResultBlockFromJsonElement(item, path))
                .collect(Collectors.toList());
        } else {
            throw new IllegalArgumentException("'" + path + "' property is not a string or array");
        }

        return new RecipeOutputSelector(outputs);
    }

    public static RecipeOutputSelector createFromBytes(FriendlyByteBuf input) {
        var outputCount = input.readUnsignedShort();
        var outputs = Lists.<ResultBlock>newArrayListWithExpectedSize(outputCount);

        for (var index = 0; index < outputCount; ++index) {
            var blockId = input.readResourceLocation();
            var block = Registry.BLOCK.getOptional(blockId);
            if (block.isEmpty()) {
                throw new IllegalStateException("Unknown block " + blockId);
            }

            var weight = input.readInt();
            outputs.add(new ResultBlock(block.get(), blockId, weight));
        }

        return new RecipeOutputSelector(outputs);
    }

    public void writeToByteBuf(FriendlyByteBuf output) {
        output.writeShort(outputs.size());
        for (var recipeOutput : outputs) {
            output.writeResourceLocation(recipeOutput.location);
            output.writeInt(recipeOutput.weight);
        }
    }

    public int getMaxOutputWeight() {
        return maxOutputWeight;
    }

    public List<ResultBlock> getOutputs() {
        return outputs;
    }

    public Block selectFirst() {
        return outputs.get(0).block;
    }

    public Block selectRandom() {
        if (outputs.size() == 1) {
            return outputs.get(0).block;
        }

        var offset = random.nextInt(maxOutputWeight);
        var accumulated = 0;
        for (var output : outputs) {
            accumulated += output.weight;
            if (offset < accumulated) {
                return output.block;
            }
        }

        // we should have received a result here
        return outputs.get(0).block;
    }

    @Override
    public String toString() {
        return "RecipeOutputSelector{" + "outputs=" + outputs + '}';
    }

    public record ResultBlock(Block block, ResourceLocation location, int weight) {

        static ResultBlock decodeResultBlockFromJsonElement(JsonElement element, String path) {
            if (element.isJsonObject()) {
                return decodeResultBlockFromJsonObject(element.getAsJsonObject(), path);
            }

            if (element.isJsonPrimitive()) {
                var outputBlock = SerializationUtil.decodeBlockFromPrimitive(element.getAsJsonPrimitive(), path);
                return new ResultBlock(outputBlock.getA(), outputBlock.getB(), 1);
            }

            throw new IllegalArgumentException("Invalid result entry");
        }

        static ResultBlock decodeResultBlockFromJsonObject(JsonObject object, String path) {
            var blockProperty = object.getAsJsonPrimitive("block");
            var outputBlock = SerializationUtil.decodeBlockFromPrimitive(blockProperty, path + ".block");

            var weightProperty = object.getAsJsonPrimitive("weight");
            var weight = 1;
            if (weightProperty != null) {
                if (!weightProperty.isNumber()) {
                    throw new IllegalArgumentException("'" + path + ".weight' property is not a number");
                }

                weight = weightProperty.getAsInt();

                if (weight <= 0) {
                    throw new IllegalArgumentException("'" + path + ".weight' property must be at least 1");
                }
            }

            return new ResultBlock(outputBlock.getA(), outputBlock.getB(), weight);
        }
    }
}
