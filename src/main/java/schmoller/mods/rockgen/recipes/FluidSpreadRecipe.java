package schmoller.mods.rockgen.recipes;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistryEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class FluidSpreadRecipe implements Recipe<Container> {
    private enum FluidSourceState {
        RequireSource,
        RequireFlowing,
        DontCare
    }

    private record ResultBlock(Block block, int weight) {
    }

    public static final Lazy<ItemStack> LazyLava = Lazy.of(() -> new ItemStack(Items.LAVA_BUCKET));
    public static final String TypeId = "fluid_spread";
    public static final RecipeType<FluidSpreadRecipe> Type = new Type();
    public static final Serializer SerializerInstance = new Serializer();

    private final ResourceLocation id;
    private final TagKey<Fluid> fluid;
    private final List<ResultBlock> outputs;
    private final int maxOutputWeight;
    private final Optional<Block> requireBelow;
    private final Optional<TagKey<Fluid>> intoFluid;
    private final FluidSourceState fluidState;
    private final Optional<Block> intoBlock;

    private final Random random = new Random();

    private FluidSpreadRecipe(ResourceLocation id, TagKey<Fluid> fluid, List<ResultBlock> outputs, Optional<Block> requireBelow,
                              Optional<TagKey<Fluid>> intoFluid, FluidSourceState fluidState, Optional<Block> intoBlock) {
        this.id = id;
        this.fluid = fluid;
        this.outputs = outputs;
        this.requireBelow = requireBelow;
        this.intoFluid = intoFluid;
        this.fluidState = fluidState;
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

    @Override
    public boolean matches(@NotNull Container inventory, @NotNull Level world) {
        return false;
    }

    public Optional<Block> tryMatch(Level level, BlockPos position) {
        if (requireBelow.isPresent()) {
            var blockBelow = level.getBlockState(position.below());
            if (!blockBelow.is(requireBelow.get())) {
                return Optional.empty();
            }
        }

        boolean isSourceBlock = false;
        if (intoFluid.isPresent()) {
            // only look this once since it's location is already determined
            isSourceBlock = level.getFluidState(position).isSource();
        }

        for (Direction direction : LiquidBlock.POSSIBLE_FLOW_DIRECTIONS) {
            BlockPos adjacent = position.relative(direction.getOpposite());

            if (doesPositionMatchRequirements(level, adjacent, isSourceBlock)) {
                var outputBlock = chooseOutputBlock();
                return Optional.of(outputBlock);
            }
        }

        return Optional.empty();
    }

    private boolean doesPositionMatchRequirements(Level level, BlockPos position, boolean isSourceBlock) {
        if (intoFluid.isPresent()) {
            if (!level.getFluidState(position).is(intoFluid.get())) {
                return false;
            }

            if (fluidState != FluidSourceState.DontCare) {
                if (fluidState == FluidSourceState.RequireSource && !isSourceBlock) {
                    return false;
                }
                return fluidState != FluidSourceState.RequireFlowing || !isSourceBlock;
            }
            return true;
        } else if (intoBlock.isPresent()) {
            return level.getBlockState(position).is(intoBlock.get());
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

    @Override
    public String toString() {
        return "FluidSpreadRecipe{" +
                "id=" + id +
                ", fluid=" + fluid +
                ", outputs=" + outputs +
                ", requireBelow=" + requireBelow +
                ", intoFluid=" + intoFluid +
                ", fluidState=" + fluidState +
                ", intoBlock=" + intoBlock +
                '}';
    }

    private static class Serializer extends ForgeRegistryEntry<RecipeSerializer<?>> implements RecipeSerializer<FluidSpreadRecipe> {
        Serializer() {
            setRegistryName(new ResourceLocation("rockgen", TypeId));
        }

        @Override
        public @NotNull FluidSpreadRecipe fromJson(@NotNull ResourceLocation id, JsonObject document) {
            var inputFluidProperty = document.getAsJsonPrimitive("fluid");
            var aboveBlockProperty = document.getAsJsonPrimitive("above_block");
            var intoObject = document.getAsJsonObject("into");

            // Input fluid
            if (inputFluidProperty == null || !inputFluidProperty.isString()) {
                throw new IllegalArgumentException("'fluid' property is not a string");
            }

            var inputFluidId = new ResourceLocation(inputFluidProperty.getAsString());
            var inputFluid = FluidTags.create(inputFluidId);

            // Output
            var resultProperty = document.get("result");
            var outputs = decodeResults(resultProperty);

            // Above block
            Optional<Block> aboveBlock = Optional.empty();

            if (aboveBlockProperty != null) {
                if (!aboveBlockProperty.isString()) {
                    throw new IllegalArgumentException("'result' property is not a string");
                }

                var aboveBlockId = new ResourceLocation(aboveBlockProperty.getAsString());
                var block = ForgeRegistries.BLOCKS.getValue(aboveBlockId);
                if (block == null || block == Blocks.AIR) {
                    throw new IllegalStateException("Unknown block " + aboveBlockId);
                }

                aboveBlock = Optional.of(block);
            }

            // Conditions
            if (intoObject == null) {
                throw new IllegalArgumentException("'into' property is not an object");
            }

            Optional<TagKey<Fluid>> intoFluid = Optional.empty();
            Optional<Block> intoBlock = Optional.empty();

            FluidSourceState intoFluidState = FluidSourceState.DontCare;

            var intoFluidProperty = intoObject.getAsJsonPrimitive("fluid");
            var intoBlockProperty = intoObject.getAsJsonPrimitive("block");

            if (intoFluidProperty != null) {
                if (!intoFluidProperty.isString()) {
                    throw new IllegalArgumentException("'into.fluid' property is not a string");
                }

                var intoFluidId = new ResourceLocation(intoFluidProperty.getAsString());
                var fluid = FluidTags.create(intoFluidId);
                intoFluid = Optional.of(fluid);

                var fluidTypeProperty = intoObject.getAsJsonPrimitive("type");
                if (fluidTypeProperty != null) {
                    if (!fluidTypeProperty.isString()) {
                        throw new IllegalArgumentException("'into.type' property is not a string");
                    }

                    var rawValue = fluidTypeProperty.getAsString();
                    intoFluidState = switch (rawValue) {
                        case "source" -> FluidSourceState.RequireSource;
                        case "flowing" -> FluidSourceState.RequireFlowing;
                        case "dont-care" -> FluidSourceState.DontCare;
                        default -> throw new IllegalArgumentException("'into.type' property is not one of: 'source', 'flowing', or 'dont-care'");
                    };
                }
            } else if (intoBlockProperty != null) {
                if (!intoBlockProperty.isString()) {
                    throw new IllegalArgumentException("'into.block' property is not a string");
                }

                var intoBlockId = new ResourceLocation(intoBlockProperty.getAsString());
                var block = ForgeRegistries.BLOCKS.getValue(intoBlockId);
                if (block == null || block == Blocks.AIR) {
                    throw new IllegalStateException("Unknown block " + intoBlockId);
                }

                intoBlock = Optional.of(block);

            } else {
                throw new IllegalStateException("Missing 'into.fluid' or 'into.block'");
            }

            return new FluidSpreadRecipe(id, inputFluid, outputs, aboveBlock, intoFluid, intoFluidState, intoBlock);
        }

        private List<ResultBlock> decodeResults(JsonElement element) {
            if (element == null) {
                throw new IllegalArgumentException("'result' property is not string or array");
            }

            if (element.isJsonPrimitive()) {
                var outputBlock = decodeBlockFromPrimitive(element.getAsJsonPrimitive(), "result");
                return List.of(new ResultBlock(outputBlock, 1));
            }

            if (element.isJsonArray()) {
                var outputArrayProperty = element.getAsJsonArray();

                return StreamSupport.stream(outputArrayProperty.spliterator(), false)
                        .map(this::decodeResultBlockFromJsonElement)
                        .collect(Collectors.toList());
            }

            throw new IllegalArgumentException("'result' property is not a string or array");
        }

        private ResultBlock decodeResultBlockFromJsonElement(JsonElement element) {
            if (element.isJsonObject()) {
                return decodeResultBlockFromJsonObject(element.getAsJsonObject());
            }

            if (element.isJsonPrimitive()) {
                var outputBlock = decodeBlockFromPrimitive(element.getAsJsonPrimitive(), "result");
                return new ResultBlock(outputBlock, 1);
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

            return new ResultBlock(outputBlock, weight);
        }

        private Block decodeBlockFromPrimitive(JsonPrimitive primitive, String path) {
            if (primitive == null || !primitive.isString()) {
                throw new IllegalArgumentException("'" + path + "' property is not a string");
            }

            var blockId = new ResourceLocation(primitive.getAsString());
            var block = ForgeRegistries.BLOCKS.getValue(blockId);
            if (block == null || block == Blocks.AIR) {
                throw new IllegalStateException("Unknown block " + blockId);
            }

            return block;
        }

        @Nullable
        @Override
        public FluidSpreadRecipe fromNetwork(@NotNull ResourceLocation id, FriendlyByteBuf input) {
            TagKey<Fluid> inputFluid;
            Block outputBlock;
            Optional<TagKey<Fluid>> intoFluid = Optional.empty();
            Optional<Block> intoBlock = Optional.empty();
            Optional<Block> aboveBlock = Optional.empty();

            FluidSourceState intoFluidState = FluidSourceState.DontCare;

            var inputFluidId = input.readResourceLocation();
            inputFluid = FluidTags.create(inputFluidId);

            var outputCount = input.readUnsignedShort();
            List<ResultBlock> outputs = Lists.newArrayListWithExpectedSize(outputCount);

            for (var index = 0; index < outputCount; ++index) {
                var blockId = input.readResourceLocation();
                var block = ForgeRegistries.BLOCKS.getValue(blockId);
                if (block == null) {
                    throw new IllegalStateException("Unknown block " + blockId);
                }

                var weight = input.readInt();
                outputs.add(new ResultBlock(block, weight));
            }

            var hasAboveBlock = input.readBoolean();
            if (hasAboveBlock) {
                var blockId = input.readResourceLocation();
                var block = ForgeRegistries.BLOCKS.getValue(blockId);
                if (block == null) {
                    throw new IllegalStateException("Unknown block " + blockId);
                }
                aboveBlock = Optional.of(block);
            }

            var usesIntoFluid = input.readBoolean();
            if (usesIntoFluid) {
                var fluidId = input.readResourceLocation();
                var fluid = FluidTags.create(fluidId);
                intoFluid = Optional.of(fluid);

                intoFluidState = input.readEnum(FluidSourceState.class);
            } else {
                var blockId = input.readResourceLocation();
                var block = ForgeRegistries.BLOCKS.getValue(blockId);
                if (block == null) {
                    throw new IllegalStateException("Unknown block " + blockId);
                }
                intoBlock = Optional.of(block);
            }

            return new FluidSpreadRecipe(id, inputFluid, outputs, aboveBlock, intoFluid, intoFluidState, intoBlock);
        }

        @Override
        public void toNetwork(FriendlyByteBuf output, FluidSpreadRecipe recipe) {
            // NOTE: These cannot be null
            output.writeResourceLocation(recipe.fluid.location());

            output.writeShort(recipe.outputs.size());
            for (var recipeOutput : recipe.outputs) {
                output.writeResourceLocation(recipeOutput.block.getRegistryName());
                output.writeInt(recipeOutput.weight);
            }

            output.writeBoolean(recipe.requireBelow.isPresent());
            if (recipe.requireBelow.isPresent()) {
                output.writeResourceLocation(recipe.requireBelow.get().getRegistryName());
            }

            output.writeBoolean(recipe.intoFluid.isPresent());
            if (recipe.intoFluid.isPresent()) {
                output.writeResourceLocation(recipe.intoFluid.get().location());
                output.writeEnum(recipe.fluidState);
            } else {
                assert (recipe.intoBlock.isPresent());
                output.writeResourceLocation(recipe.intoBlock.get().getRegistryName());
            }
        }
    }

    private static class Type implements RecipeType<FluidSpreadRecipe> {
        @Override
        public String toString() {
            return TypeId;
        }
    }
}
