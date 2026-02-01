package xerca.xercapaint.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import xerca.xercapaint.entity.EntityCanvas;

import javax.annotation.ParametersAreNonnullByDefault;

@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
@ParametersAreNonnullByDefault
public class RenderEntityCanvas extends EntityRenderer<EntityCanvas> {
    public static RenderEntityCanvas theInstance;

    public RenderEntityCanvas(EntityRendererProvider.Context ctx) {
        super(ctx);
        theInstance = this;
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(EntityCanvas entity) {
        CanvasTextureManager.CanvasInstance instance = getCanvasInstance(entity);
        return instance != null ? instance.location : CanvasTextureManager.BACK_TEXTURE;
    }

    @Override
    public void render(EntityCanvas canvas, float entityYaw, float partialTick, PoseStack ms, MultiBufferSource buffer, int packedLight) {
        CanvasTextureManager.CanvasInstance instance = getCanvasInstance(canvas);
        if (instance != null) {
            float yaw = canvas.getYRot();
            float pitch = canvas.getXRot();
            instance.render(canvas, yaw, pitch, ms, buffer, canvas.getDirection(), packedLight);
        }
    }

    private CanvasTextureManager.CanvasInstance getCanvasInstance(EntityCanvas canvas) {
        return CanvasTextureManager.INSTANCE.getCanvasInstance(canvas.getCanvasID(), canvas.getVersion(), canvas.getWidth(), canvas.getHeight());
    }
}
