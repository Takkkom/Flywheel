#include "flywheel:internal/common.vert"
#include "flywheel:internal/packed_material.glsl"
#include "flywheel:internal/indirect/buffer_bindings.glsl"
#include "flywheel:internal/indirect/draw_command.glsl"
#include "flywheel:internal/indirect/light.glsl"
#include "flywheel:internal/indirect/matrices.glsl"

layout(std430, binding = _FLW_TARGET_BUFFER_BINDING) restrict readonly buffer TargetBuffer {
    uint _flw_instanceIndices[];
};

layout(std430, binding = _FLW_DRAW_BUFFER_BINDING) restrict readonly buffer DrawBuffer {
    MeshDrawCommand _flw_drawCommands[];
};

#ifdef FLW_EMBEDDED
layout(std430, binding = _FLW_MATRIX_BUFFER_BINDING) restrict buffer MatrixBuffer {
    Matrices _flw_matrices[];
};
#endif

uniform uint _flw_baseDraw;

flat out uvec3 _flw_packedMaterial;

void main() {
#if __VERSION__ < 460
    uint drawIndex = gl_DrawIDARB + _flw_baseDraw;
#else
    uint drawIndex = gl_DrawID + _flw_baseDraw;
#endif
    MeshDrawCommand draw = _flw_drawCommands[drawIndex];

    _flw_uberMaterialVertexIndex = draw.materialVertexIndex;
    uint packedMaterialProperties = draw.packedMaterialProperties;
    _flw_unpackMaterialProperties(packedMaterialProperties, flw_material);
    _flw_packedMaterial = uvec3(draw.materialFragmentIndex, draw.packedFogAndCutout, packedMaterialProperties);

    #ifdef FLW_EMBEDDED
    _flw_unpackMatrices(_flw_matrices[draw.matrixIndex], _flw_modelMatrix, _flw_normalMatrix);
    //    _flw_modelMatrix = mat4(1.);
    //    _flw_normalMatrix = mat3(1.);
    #endif

    #if __VERSION__ < 460
    uint instanceIndex = _flw_instanceIndices[gl_BaseInstanceARB + gl_InstanceID];
#else
    uint instanceIndex = _flw_instanceIndices[gl_BaseInstance + gl_InstanceID];
#endif
    FlwInstance instance = _flw_unpackInstance(instanceIndex);

    _flw_main(instance, instanceIndex);
}
