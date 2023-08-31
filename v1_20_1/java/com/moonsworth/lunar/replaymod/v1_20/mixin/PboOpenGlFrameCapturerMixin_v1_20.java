package com.moonsworth.lunar.replaymod.v1_20.mixin;

import com.moonsworth.lunar.client.util.Ref;
import com.replaymod.render.capturer.*;
import com.replaymod.render.frame.OpenGlFrame;
import com.replaymod.render.rendering.Frame;
import com.replaymod.render.utils.ByteBufferPool;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.swing.*;
import java.nio.ByteBuffer;

@Mixin(PboOpenGlFrameCapturer.class)
public abstract class PboOpenGlFrameCapturerMixin_v1_20<F extends Frame, D extends Enum<D> & CaptureData> extends OpenGlFrameCapturer<F, D> {

    @Shadow
    public D[] data;

    @Shadow
    public abstract F create(OpenGlFrame[] openGlFrames);

    public PboOpenGlFrameCapturerMixin_v1_20(WorldRenderer worldRenderer, RenderInfo renderInfo) {
        super(worldRenderer, renderInfo);
    }

    @Inject(
            method = "readFromPbo",
            at = @At("HEAD"),
            cancellable = true
    )
    public void ichor$read(ByteBuffer pboBuffer, int bytesPerPixel, CallbackInfoReturnable<Object> cir) {
        OpenGlFrame[] frames = new OpenGlFrame[this.data.length];
        int frameBufferSize = getFrameWidth() * this.getFrameHeight() * bytesPerPixel;

        for (int i = 0; i < frames.length; ++i) {
            ByteBuffer frameBuffer;
            try {
                frameBuffer = ByteBufferPool.allocate(frameBufferSize);
            } catch (OutOfMemoryError ex) {
                ex.printStackTrace();
                shutdown();
                return;
            }
            pboBuffer.limit(pboBuffer.position() + frameBufferSize);
            frameBuffer.put(pboBuffer);
            frameBuffer.rewind();
            frames[i] = new OpenGlFrame(this.framesDone - 2, this.frameSize, bytesPerPixel, frameBuffer);
        }

        cir.setReturnValue(create(frames));
    }

    private void shutdown() {
        (new Thread(() -> JOptionPane.showMessageDialog(null,
                "Your client has ran out of memory while rendering a video.\nYou can increase memory allocation in the launcher.\n Another common fix is to reduce the Anti-Aliasing to 2x or 4x before rendering the replay.",
                "Out of Memory",
                JOptionPane.WARNING_MESSAGE))).start();

        try {
            Thread.sleep(7000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Ref.mc().bridge$shutdownMinecraftApplet();
    }
}
