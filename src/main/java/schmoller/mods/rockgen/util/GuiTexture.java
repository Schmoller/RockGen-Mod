package schmoller.mods.rockgen.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public record GuiTexture(ResourceLocation location, int x, int y, int width, int height) {
    @OnlyIn(Dist.CLIENT)
    public void render(PoseStack stack, int x, int y) {
        RenderSystem.setShaderTexture(0, location);

        GuiComponent.blit(stack, x, y, 0, this.x, this.y, this.width, this.height, 256, 256);
    }
}
