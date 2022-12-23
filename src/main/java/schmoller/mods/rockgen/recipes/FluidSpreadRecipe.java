package schmoller.mods.rockgen.recipes;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.annotations.SerializedName;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Tuple;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class FluidSpreadRecipe implements Recipe<Container>, Comparable<FluidSpreadRecipe> {
    public static final Lazy<ItemStack> LazyLava = Lazy.of(() -> new ItemStack(Items.LAVA_BUCKET));
    public static final String TypeId = "fluid_spread";
    public static final RecipeType<FluidSpreadRecipe> Type = new Type();
    public static final Serializer SerializerInstance = new Serializer();
    private final ResourceLocation id;
    private final TagKey<Fluid> fluid;
    private final FluidSourceState fluidState;
    private final FluidSpreadDirection spreadDirection;
    private final List<ResultBlock> outputs;
    private final int maxOutputWeight;
    private final Optional<BlockMatcher> requireBelow;
    private final Optional<TagKey<Fluid>> intoFluid;
    private final Optional<BlockMatcher> intoBlock;
    private final Random random = new Random();

    private FluidSpreadRecipe(
        ResourceLocation id, TagKey<Fluid> fluid, FluidSpreadDirection spreadDirection, List<ResultBlock> outputs,
        Optional<BlockMatcher> requireBelow, Optional<TagKey<Fluid>> intoFluid, FluidSourceState fluidState,
        Optional<BlockMatcher> intoBlock
    ) {
        this.id = id;
        this.fluid = fluid;
        this.fluidState = fluidState;
        this.spreadDirection = spreadDirection;
        this.outputs = outputs;
        this.requireBelow = requireBelow;
        this.intoFluid = intoFluid;
        this.intoBlock = intoBlock;

        int outputWeight = 0;
        for (var output : outputs) {
            outputWeight += output.weight;
        }
        maxOutputWeight = outputWeight;
    }

    public TagKey<Fluid> fluidToMatch() {
        return fluid;
    }

    public FluidSpreadDirection fluidSpreadDirection() {
        return spreadDirection;
    }

    @Override
    public boolean matches(@NotNull Container inventory, @NotNull Level world) {
        return false;
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull Container inventory) {
        throw new IllegalStateException("Recipe cannot be used with inventories");
    }

    @Override
    public boolean canCraftInDimensions(int p_43999_, int p_44000_) {
        return true;
    }

    @Override
    public @NotNull ItemStack getResultItem() {
        return new ItemStack(outputs.get(0).block.asItem());
    }

    @Override
    public @NotNull NonNullList<ItemStack> getRemainingItems(@NotNull Container p_44004_) {
        return Recipe.super.getRemainingItems(p_44004_);
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public @NotNull ItemStack getToastSymbol() {
        return LazyLava.get();
    }

    @Override
    public @NotNull ResourceLocation getId() {
        return id;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return SerializerInstance;
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return Type;
    }

    public Optional<Block> tryMatch(LevelAccessor level, BlockPos position) {
        if (requireBelow.isPresent()) {
            var blockBelow = level.getBlockState(position.below());
            if (!requireBelow.get().matches(blockBelow)) {
                return Optional.empty();
            }
        }

        if (fluidState != FluidSourceState.DontCare) {
            var isSourceBlock = level.getFluidState(position).isSource();

            if (fluidState == FluidSourceState.RequireSource && !isSourceBlock) {
                return Optional.empty();
            }
            if (fluidState == FluidSourceState.RequireFlowing && isSourceBlock) {
                return Optional.empty();
            }
        }

        if (spreadDirection == FluidSpreadDirection.Regular) {
            for (Direction direction : LiquidBlock.POSSIBLE_FLOW_DIRECTIONS) {
                BlockPos adjacent = position.relative(direction.getOpposite());

                if (doesPositionMatchRequirements(level, adjacent)) {
                    var outputBlock = chooseOutputBlock();
                    return Optional.of(outputBlock);
                }
            }
        } else {
            if (doesPositionMatchRequirements(level, position)) {
                var outputBlock = chooseOutputBlock();
                return Optional.of(outputBlock);
            }
        }

        return Optional.empty();
    }

    private boolean doesPositionMatchRequirements(LevelAccessor level, BlockPos position) {
        if (intoFluid.isPresent()) {
            if (!level.getFluidState(position).is(intoFluid.get())) {
                return false;
            }

            return true;
        } else if (intoBlock.isPresent()) {
            var state = level.getBlockState(position);
            return intoBlock.get().matches(state);
        }

        return false;
    }

    private Block chooseOutputBlock() {
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
        return "FluidSpreadRecipe{" + "id=" + id + ", fluid=" + fluid + ", outputs=" + outputs + ", requireBelow=" + requireBelow + ", intoFluid=" + intoFluid + ", fluidState=" + fluidState + ", intoBlock=" + intoBlock + '}';
    }

    /**
     * This comparator is here to order by specificity so that more specific recipes are checked first.
     *
     * @param other The other recipe to compare
     * @return The sort order
     */
    @Override
    public int compareTo(@NotNull FluidSpreadRecipe other) {
        // If one requires a block below to match, then it should be checked above one that doesn't
        if (requireBelow.isPresent() && other.requireBelow.isEmpty()) {
            return -1;
        } else if (requireBelow.isEmpty() && other.requireBelow.isPresent()) {
            return 1;
        }

        // If one requires a specific fluid state to match, then that should be checked above ones that don't care.
        if (fluidState == FluidSourceState.DontCare && other.fluidState != FluidSourceState.DontCare) {
            return 1;
        } else if (fluidState != FluidSourceState.DontCare && other.fluidState == FluidSourceState.DontCare) {
            return -1;
        }

        // Recipes have equal specificity
        return 0;
    }

    private enum FluidSourceState {
        @SerializedName("source") RequireSource,
        @SerializedName("flowing") RequireFlowing,
        @SerializedName("any") DontCare
    }

    public enum FluidSpreadDirection {
        Down, Regular
    }

    private record ResultBlock(Block block, ResourceLocation location, int weight) {}

    public static class Serializer implements RecipeSerializer<FluidSpreadRecipe> {
        private Gson gson = new Gson();

        @Override
        public @NotNull FluidSpreadRecipe fromJson(@NotNull ResourceLocation id, JsonObject document) {
            var inputProperty = document.get("input");
            var aboveBlockProperty = document.getAsJsonPrimitive("above_block");
            var intoObject = document.getAsJsonObject("into");

            var input = gson.fromJson(inputProperty, RecipeInput.class);

            // Output
            var resultProperty = document.get("result");
            var outputs = decodeResults(resultProperty);

            var isFlowingDownProperty = document.getAsJsonPrimitive("flowing_down");
            var spreadDirection = FluidSpreadDirection.Regular;

            if (isFlowingDownProperty != null) {
                if (!isFlowingDownProperty.isBoolean()) {
                    throw new IllegalArgumentException("'flowing_down' property is not a boolean");
                }
                if (isFlowingDownProperty.getAsBoolean()) {
                    spreadDirection = FluidSpreadDirection.Down;
                }
            }

            // Above block
            Optional<BlockMatcher> aboveBlock = Optional.empty();

            if (aboveBlockProperty != null) {
                aboveBlock = Optional.of(decodeBlockMatcherFromJson(aboveBlockProperty, "above_block"));
            }

            // Conditions
            if (intoObject == null) {
                throw new IllegalArgumentException("'into' property is not an object");
            }

            Optional<TagKey<Fluid>> intoFluid = Optional.empty();
            Optional<BlockMatcher> intoBlock = Optional.empty();

            var intoFluidProperty = intoObject.getAsJsonPrimitive("fluid");
            var intoBlockProperty = intoObject.getAsJsonPrimitive("block");

            if (intoFluidProperty != null) {
                if (!intoFluidProperty.isString()) {
                    throw new IllegalArgumentException("'into.fluid' property is not a string");
                }

                var intoFluidId = new ResourceLocation(intoFluidProperty.getAsString());
                var fluid = FluidTags.create(intoFluidId);
                intoFluid = Optional.of(fluid);
            } else if (intoBlockProperty != null) {
                intoBlock = Optional.of(decodeBlockMatcherFromJson(intoBlockProperty, "into.block"));
            } else {
                throw new IllegalStateException("Missing 'into.fluid' or 'into.block'");
            }

            return new FluidSpreadRecipe(id,
                input.fluidTag(),
                spreadDirection,
                outputs,
                aboveBlock,
                intoFluid,
                input.state,
                intoBlock
            );
        }

        private BlockMatcher decodeBlockMatcherFromJson(JsonPrimitive element, String path) {
            if (!element.isString()) {
                throw new IllegalArgumentException("'" + path + "' property is not a string");
            }

            var blockIdOrTag = element.getAsString();

            if (blockIdOrTag.startsWith("#")) {
                // This is a block tag
                return new BlockMatcher(BlockTags.create(new ResourceLocation(blockIdOrTag.substring(1))));
            }

            // just a regular block id
            var blockId = new ResourceLocation(blockIdOrTag);
            var block = ForgeRegistries.BLOCKS.getValue(blockId);
            if (block == null || block == Blocks.AIR) {
                throw new IllegalStateException("Unknown block " + blockId);
            }

            return new BlockMatcher(blockId, block);
        }

        private List<ResultBlock> decodeResults(JsonElement element) {
            if (element == null) {
                throw new IllegalArgumentException("'result' property is not string or array");
            }

            if (element.isJsonPrimitive()) {
                var outputBlock = decodeBlockFromPrimitive(element.getAsJsonPrimitive(), "result");
                return List.of(new ResultBlock(outputBlock.getA(), outputBlock.getB(), 1));
            }

            if (element.isJsonArray()) {
                var outputArrayProperty = element.getAsJsonArray();

                return StreamSupport.stream(outputArrayProperty.spliterator(), false)
                    .map(this::decodeResultBlockFromJsonElement)
                    .collect(Collectors.toList());
            }

            throw new IllegalArgumentException("'result' property is not a string or array");
        }

        private Tuple<Block, ResourceLocation> decodeBlockFromPrimitive(JsonPrimitive primitive, String path) {
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

        private ResultBlock decodeResultBlockFromJsonElement(JsonElement element) {
            if (element.isJsonObject()) {
                return decodeResultBlockFromJsonObject(element.getAsJsonObject());
            }

            if (element.isJsonPrimitive()) {
                var outputBlock = decodeBlockFromPrimitive(element.getAsJsonPrimitive(), "result");
                return new ResultBlock(outputBlock.getA(), outputBlock.getB(), 1);
            }

            throw new IllegalArgumentException("Invalid result entry");
        }

        private ResultBlock decodeResultBlockFromJsonObject(JsonObject object) {
            var blockProperty = object.getAsJsonPrimitive("block");
            var outputBlock = decodeBlockFromPrimitive(blockProperty, "result.block");

            var weightProperty = object.getAsJsonPrimitive("weight");
            var weight = 1;
            if (weightProperty != null) {
                if (!weightProperty.isNumber()) {
                    throw new IllegalArgumentException("'result.weight' property is not a number");
                }

                weight = weightProperty.getAsInt();

                if (weight <= 0) {
                    throw new IllegalArgumentException("'result.weight' property must be at least 1");
                }
            }

            return new ResultBlock(outputBlock.getA(), outputBlock.getB(), weight);
        }

        @Nullable
        @Override
        public FluidSpreadRecipe fromNetwork(@NotNull ResourceLocation id, FriendlyByteBuf input) {
            TagKey<Fluid> inputFluid;
            Optional<TagKey<Fluid>> intoFluid = Optional.empty();
            Optional<BlockMatcher> intoBlock = Optional.empty();
            Optional<BlockMatcher> aboveBlock = Optional.empty();

            FluidSourceState intoFluidState;

            var inputFluidId = input.readResourceLocation();
            inputFluid = FluidTags.create(inputFluidId);
            intoFluidState = input.readEnum(FluidSourceState.class);

            var outputCount = input.readUnsignedShort();
            List<ResultBlock> outputs = Lists.newArrayListWithExpectedSize(outputCount);

            for (var index = 0; index < outputCount; ++index) {
                var blockId = input.readResourceLocation();
                var block = ForgeRegistries.BLOCKS.getValue(blockId);
                if (block == null) {
                    throw new IllegalStateException("Unknown block " + blockId);
                }

                var weight = input.readInt();
                outputs.add(new ResultBlock(block, blockId, weight));
            }

            var spreadDirection = input.readEnum(FluidSpreadDirection.class);

            var hasAboveBlock = input.readBoolean();
            if (hasAboveBlock) {
                aboveBlock = Optional.of(decodeBlockMatcherFromNetwork(input));
            }

            var usesIntoFluid = input.readBoolean();
            if (usesIntoFluid) {
                var fluidId = input.readResourceLocation();
                var fluid = FluidTags.create(fluidId);
                intoFluid = Optional.of(fluid);
            } else {
                intoBlock = Optional.of(decodeBlockMatcherFromNetwork(input));
            }

            return new FluidSpreadRecipe(id,
                inputFluid,
                spreadDirection,
                outputs,
                aboveBlock,
                intoFluid,
                intoFluidState,
                intoBlock
            );
        }

        private BlockMatcher decodeBlockMatcherFromNetwork(FriendlyByteBuf input) {
            var idOrTag = input.readResourceLocation();
            var isTag = input.readBoolean();

            if (isTag) {
                return new BlockMatcher(BlockTags.create(idOrTag));
            }

            var block = ForgeRegistries.BLOCKS.getValue(idOrTag);
            if (block == null) {
                throw new IllegalStateException("Unknown block " + idOrTag);
            }

            return new BlockMatcher(idOrTag, block);
        }

        private void encodeBlockMatcherToNetwork(BlockMatcher matcher, FriendlyByteBuf output) {
            output.writeResourceLocation(matcher.location());
            output.writeBoolean(matcher.isTag());
        }

        @Override
        public void toNetwork(FriendlyByteBuf output, FluidSpreadRecipe recipe) {
            // NOTE: These cannot be null
            output.writeResourceLocation(recipe.fluid.location());
            output.writeEnum(recipe.fluidState);

            output.writeShort(recipe.outputs.size());
            for (var recipeOutput : recipe.outputs) {
                output.writeResourceLocation(recipeOutput.location);
                output.writeInt(recipeOutput.weight);
            }

            output.writeEnum(recipe.spreadDirection);

            output.writeBoolean(recipe.requireBelow.isPresent());
            if (recipe.requireBelow.isPresent()) {
                encodeBlockMatcherToNetwork(recipe.requireBelow.get(), output);
            }

            output.writeBoolean(recipe.intoFluid.isPresent());
            if (recipe.intoFluid.isPresent()) {
                output.writeResourceLocation(recipe.intoFluid.get().location());
            } else {
                assert (recipe.intoBlock.isPresent());
                encodeBlockMatcherToNetwork(recipe.intoBlock.get(), output);
            }
        }
    }

    private static class Type implements RecipeType<FluidSpreadRecipe> {
        @Override
        public String toString() {
            return TypeId;
        }
    }

    private static class RecipeInput {
        public String fluid;
        public FluidSourceState state;

        public TagKey<Fluid> fluidTag() {
            return FluidTags.create(new ResourceLocation(fluid));
        }
    }
}
