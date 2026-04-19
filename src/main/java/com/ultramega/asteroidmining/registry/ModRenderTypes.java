//package com.ultramega.asteroidmining.registry;
//
//import com.ultramega.asteroidmining.AsteroidMining;
//
//import java.util.OptionalDouble;
//
//import com.mojang.blaze3d.vertex.DefaultVertexFormat;
//import com.mojang.blaze3d.vertex.VertexFormat;
//import net.minecraft.client.renderer.rendertype.RenderType;
//
//public class ModRenderTypes extends RenderType {
//    public static final RenderType LINES_NON_TRANSLUCENT = create(
//        AsteroidMining.MOD_ID + ":lines_non_translucent",
//        DefaultVertexFormat.POSITION_COLOR_NORMAL,
//        VertexFormat.Mode.LINES,
//        1536,
//        RenderType.CompositeState.builder()
//            .setShaderState(RENDERTYPE_LINES_SHADER)
//            .setLineState(new LineStateShard(OptionalDouble.empty()))
//            .setLayeringState(VIEW_OFFSET_Z_LAYERING)
//            .setTransparencyState(NO_TRANSPARENCY)
//            .setOutputState(ITEM_ENTITY_TARGET)
//            .setWriteMaskState(COLOR_DEPTH_WRITE)
//            .setCullState(NO_CULL)
//            .createCompositeState(false)
//    );
//
//    private ModRenderTypes(final String name,
//                           final VertexFormat format,
//                           final VertexFormat.Mode mode,
//                           final int bufferSize,
//                           final boolean affectsCrumbling,
//                           final boolean sortOnUpload,
//                           final Runnable setupState,
//                           final Runnable clearState) {
//        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
//        throw new IllegalStateException("This class is not meant to be constructed!");
//    }
//}
