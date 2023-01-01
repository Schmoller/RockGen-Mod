package schmoller.mods.rockgen.gametests;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import schmoller.mods.rockgen.RockGenerationMod;

@GameTestHolder(RockGenerationMod.Id)
public class VanillaInteractions {
    @GameTest
    public void cobblestoneTest(GameTestHelper helper) {
        helper.setBlock(1, 2, 1, Blocks.LAVA);
        helper.succeedWhenBlockPresent(Blocks.COBBLESTONE, 2, 2, 1);
    }

    @GameTest
    public void basaltTest(GameTestHelper helper) {
        helper.setBlock(1, 2, 1, Blocks.LAVA);
        helper.succeedWhenBlockPresent(Blocks.BASALT, 2, 2, 1);
    }

    @GameTest(timeoutTicks = 60)
    public void basaltIncorrectTest(GameTestHelper helper) {
        helper.setBlock(1, 2, 1, Blocks.LAVA);
        helper.runAfterDelay(31, () -> {
            helper.assertBlockPresent(Blocks.LAVA, 2, 2, 1);
            helper.succeed();
        });
    }

    @GameTest
    public void obsidianTest(GameTestHelper helper) {
        helper.setBlock(1, 2, 1, Blocks.LAVA);
        helper.succeedWhenBlockPresent(Blocks.OBSIDIAN, 1, 2, 1);
    }

    @GameTest
    public void stoneTest(GameTestHelper helper) {
        helper.setBlock(1, 3, 1, Blocks.LAVA);
        helper.succeedWhenBlockPresent(Blocks.STONE, 1, 2, 1);
    }
}
