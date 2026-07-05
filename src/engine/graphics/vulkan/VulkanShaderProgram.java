package engine.graphics.vulkan;

import engine.logging.Logger;
import engine.util.Pair;
import engine.logging.SkyRuntimeException;
import engine.asset.Asset;
import engine.graphics.*;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.spvc.SpvcErrorCallback;
import org.lwjgl.util.spvc.SpvcReflectedResource;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.*;
import java.util.stream.IntStream;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.util.spvc.Spv.SpvDecorationBinding;
import static org.lwjgl.util.spvc.Spv.SpvDecorationDescriptorSet;
import static org.lwjgl.util.spvc.Spvc.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK12.*;
import static org.lwjgl.vulkan.VK13.VK_DYNAMIC_STATE_CULL_MODE;

public class VulkanShaderProgram extends ShaderProgram {

    private VkPipelineShaderStageCreateInfo.Buffer shaderStageCreateInfos;
    private int stageCount = 0;
    private List<List<Long>> descriptorSetLayoutHandles = new ArrayList<>();
    private List<List<Long>> descriptorSetHandles = new ArrayList<>();
    private List<Long> descriptorPoolHandles = new ArrayList<>();
    private HashMap<ShaderType, byte[]> shaders = new HashMap<>();
    private List<Long> shaderModuleHandles = new ArrayList<>();
    private VulkanPipeline pipeline;
    private long context;
    private SpvcErrorCallback spvcErrorCallback;


    public VulkanShaderProgram(Disposable parent) {
        super(parent);

        try(MemoryStack stack = stackPush()) {
            PointerBuffer pContext = stack.callocPointer(1);
            spvc_context_create(pContext);
            context = pContext.get(0);


            spvcErrorCallback = new SpvcErrorCallback() {
                @Override
                public void invoke(long l, long l1) {
                    String str = spvc_context_get_last_error_string(context);
                    Logger.error(VulkanShaderProgram.class, str);
                }
            };
            spvc_context_set_error_callback(context, spvcErrorCallback, 0);


        }
    }

    private VulkanPipeline createComputePipeline(VkDevice device) {

        long pipelineLayoutHandle;
        long computePipelineHandle = 0;

        try(MemoryStack stack = stackPush()) {


            VkPipelineLayoutCreateInfo pipelineLayoutInfo = VkPipelineLayoutCreateInfo.calloc(stack);
            {
                pipelineLayoutInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);
                pipelineLayoutInfo.setLayoutCount(getDescriptorSetsSpec().size());
                pipelineLayoutInfo.pSetLayouts(stack.longs(getAllDescriptorSetLayoutHandles()));

            }
            if(pushConstantsSizeBytes > 0) {
                VkPushConstantRange.Buffer pushConstantRanges = VkPushConstantRange.calloc(1, stack);
                {
                    VkPushConstantRange shaderModePushConstantRange = pushConstantRanges.get(0);
                    {
                        shaderModePushConstantRange.offset(0);
                        shaderModePushConstantRange.size(pushConstantsSizeBytes);
                        shaderModePushConstantRange.stageFlags(VK_SHADER_STAGE_ALL);
                    }
                }
                pipelineLayoutInfo.pPushConstantRanges(pushConstantRanges);
            }

            LongBuffer pPipelineLayout = stack.longs(VK_NULL_HANDLE);
            if(vkCreatePipelineLayout(device, pipelineLayoutInfo, null, pPipelineLayout) != VK_SUCCESS) {
                throw new SkyRuntimeException("Failed to create pipeline layout");
            }
            pipelineLayoutHandle = pPipelineLayout.get(0);


            VkComputePipelineCreateInfo.Buffer pipelineInfo = VkComputePipelineCreateInfo.calloc(1, stack);
            {
                pipelineInfo.sType(VK_STRUCTURE_TYPE_COMPUTE_PIPELINE_CREATE_INFO);
                pipelineInfo.layout(pipelineLayoutHandle);
                pipelineInfo.stage(shaderStageCreateInfos.get(0));
            }


            LongBuffer pComputePipeline = stack.mallocLong(1);


            if(vkCreateComputePipelines(device, VK_NULL_HANDLE, pipelineInfo, null, pComputePipeline) != VK_SUCCESS) {
                throw new SkyRuntimeException("Failed to create compute pipeline");
            }


            computePipelineHandle = pComputePipeline.get(0);


        }

        return new VulkanPipeline(this, pipelineLayoutHandle, computePipelineHandle);
    }
    private VulkanPipeline createGraphicsPipeline(VkDevice device) {
        long pipelineLayoutHandle;
        long graphicsPipelineHandle = 0;

        try(MemoryStack stack = stackPush()) {

            VkPipelineVertexInputStateCreateInfo vertexInputInfo = VkPipelineVertexInputStateCreateInfo.calloc(stack);
            vertexInputInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO);
            {



                int vertexSize = 0;
                for(VertexAttribute vertexAttribute : vertexAttributes){
                    vertexSize += vertexAttribute.getSize();
                }


                VkVertexInputBindingDescription.Buffer bindingDescription =
                        VkVertexInputBindingDescription.calloc(1, stack);


                bindingDescription.binding(0);
                bindingDescription.stride(vertexSize * Float.BYTES);
                bindingDescription.inputRate(VK_VERTEX_INPUT_RATE_VERTEX);
                vertexInputInfo.pVertexBindingDescriptions(bindingDescription);



                VkVertexInputAttributeDescription.Buffer attributeDescriptions = VkVertexInputAttributeDescription.calloc(vertexAttributes.size(), stack);

                int offset = 0;

                for (int i = 0; i < vertexAttributes.size(); i++) {
                    VertexAttribute vertexAttribute = vertexAttributes.get(i);
                    VkVertexInputAttributeDescription attribute = attributeDescriptions.get(i);
                    attribute.binding(0);
                    attribute.location(i);
                    attribute.format(VulkanUtil.getVulkanVertexAttributeType(vertexAttribute.getSize()));
                    attribute.offset(offset);

                    offset += vertexAttribute.getSize() * Float.BYTES;
                }

                vertexInputInfo.pVertexAttributeDescriptions(attributeDescriptions.rewind());
            }

            VkPipelineInputAssemblyStateCreateInfo inputAssembly = VkPipelineInputAssemblyStateCreateInfo.calloc(stack);
            {
                inputAssembly.sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO);
                inputAssembly.topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST);
                inputAssembly.primitiveRestartEnable(false);
            }

            VkPipelineViewportStateCreateInfo viewportState = VkPipelineViewportStateCreateInfo.calloc(stack);
            {
                viewportState.sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO);
                viewportState.viewportCount(1);
                viewportState.scissorCount(1);
            }

            VkPipelineDynamicStateCreateInfo dynamicState = VkPipelineDynamicStateCreateInfo.calloc(stack);
            {
                dynamicState.sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO);
                dynamicState.pDynamicStates(stack.ints(VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR, VK_DYNAMIC_STATE_CULL_MODE));
            }

            VkPipelineRasterizationStateCreateInfo rasterizer = VkPipelineRasterizationStateCreateInfo.calloc(stack);
            {
                rasterizer.sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO);
                rasterizer.depthClampEnable(false);
                rasterizer.rasterizerDiscardEnable(false);

                rasterizer.lineWidth(1);
                rasterizer.polygonMode(VK_POLYGON_MODE_FILL);
                rasterizer.cullMode(VK_CULL_MODE_BACK_BIT);
                rasterizer.frontFace(VK_FRONT_FACE_COUNTER_CLOCKWISE);
                rasterizer.depthBiasEnable(false);
            }

            VkPipelineMultisampleStateCreateInfo multisampling = VkPipelineMultisampleStateCreateInfo.calloc(stack);
            {
                multisampling.sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO);
                multisampling.sampleShadingEnable(false);
                multisampling.rasterizationSamples(VK_SAMPLE_COUNT_1_BIT);
            }

            int attachmentCount = attachmentTextureFormatTypes.size();

            VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachments = VkPipelineColorBlendAttachmentState.calloc(attachmentCount, stack);
            {
                for(VkPipelineColorBlendAttachmentState colorBlendAttachment : colorBlendAttachments) {
                    colorBlendAttachment.colorWriteMask(VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT | VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT);
                    colorBlendAttachment.blendEnable(enableBlending);
                    colorBlendAttachment.srcColorBlendFactor(VK_BLEND_FACTOR_SRC_ALPHA);
                    colorBlendAttachment.dstColorBlendFactor(VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA);
                    colorBlendAttachment.colorBlendOp(VK_BLEND_OP_ADD);
                    colorBlendAttachment.srcAlphaBlendFactor(VK_BLEND_FACTOR_ONE);
                    colorBlendAttachment.dstAlphaBlendFactor(VK_BLEND_FACTOR_ZERO);
                    colorBlendAttachment.alphaBlendOp(VK_BLEND_OP_ADD);
                }

            }


            VkPipelineColorBlendStateCreateInfo colorBlending = VkPipelineColorBlendStateCreateInfo.calloc(stack);
            {
                colorBlending.sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO);
                colorBlending.logicOpEnable(false);
                colorBlending.logicOp(VK_LOGIC_OP_COPY);
                colorBlending.pAttachments(colorBlendAttachments);
                colorBlending.blendConstants(stack.floats(0.0f, 0.0f, 0.0f, 0.0f));
                colorBlending.attachmentCount(attachmentCount);
            }

            VkPipelineDepthStencilStateCreateInfo depthStencil = VkPipelineDepthStencilStateCreateInfo.calloc(stack);
            {
                depthStencil.sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO);
                depthStencil.depthTestEnable(true);
                depthStencil.depthWriteEnable(true);
                depthStencil.depthCompareOp(VulkanUtil.getVulkanDepthTestType(depthTestType));
                depthStencil.minDepthBounds(0f);
                depthStencil.maxDepthBounds(1f);


                depthStencil.stencilTestEnable(false);


            }





            VkPipelineLayoutCreateInfo pipelineLayoutInfo = VkPipelineLayoutCreateInfo.calloc(stack);
            {
                pipelineLayoutInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);
                pipelineLayoutInfo.setLayoutCount(getDescriptorSetsSpec().size());
                pipelineLayoutInfo.pSetLayouts(stack.longs(getAllDescriptorSetLayoutHandles()));
            }

            if(pushConstantsSizeBytes > 0) {
                VkPushConstantRange.Buffer pushConstantRanges = VkPushConstantRange.calloc(1, stack);
                {
                    VkPushConstantRange shaderModePushConstantRange = pushConstantRanges.get(0);
                    {
                        shaderModePushConstantRange.offset(0);
                        shaderModePushConstantRange.size(pushConstantsSizeBytes);
                        shaderModePushConstantRange.stageFlags(VK_SHADER_STAGE_ALL);
                    }
                }
                pipelineLayoutInfo.pPushConstantRanges(pushConstantRanges);
            }


            LongBuffer pPipelineLayout = stack.longs(VK_NULL_HANDLE);

            if(vkCreatePipelineLayout(device, pipelineLayoutInfo, null, pPipelineLayout) != VK_SUCCESS) {
                throw new SkyRuntimeException("Failed to create pipeline layout");
            }

            pipelineLayoutHandle = pPipelineLayout.get(0);

            VkPipelineRenderingCreateInfoKHR pipelineRenderingCreateInfoKHR = VkPipelineRenderingCreateInfoKHR.calloc(stack);
            pipelineRenderingCreateInfoKHR.colorAttachmentCount(attachmentCount);
            pipelineRenderingCreateInfoKHR.sType(KHRDynamicRendering.VK_STRUCTURE_TYPE_PIPELINE_RENDERING_CREATE_INFO_KHR);

            int[] vulkanAttachmentImageTypes = new int[attachmentCount];
            for (int i = 0; i < vulkanAttachmentImageTypes.length; i++) {
                vulkanAttachmentImageTypes[i] = VulkanUtil.getVulkanImageFormat(attachmentTextureFormatTypes.get(i));
            }

            pipelineRenderingCreateInfoKHR.pColorAttachmentFormats(stack.ints(vulkanAttachmentImageTypes));
            pipelineRenderingCreateInfoKHR.depthAttachmentFormat(VulkanUtil.getVulkanImageFormat(depthAttachmentTextureFormatType));


            VkGraphicsPipelineCreateInfo.Buffer pipelineInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack);
            {
                pipelineInfo.sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO);
                pipelineInfo.layout(pipelineLayoutHandle);
                pipelineInfo.pInputAssemblyState(inputAssembly);
                pipelineInfo.pRasterizationState(rasterizer);
                pipelineInfo.pColorBlendState(colorBlending);
                pipelineInfo.pMultisampleState(multisampling);
                pipelineInfo.pViewportState(viewportState);
                pipelineInfo.pDepthStencilState(depthStencil);
                pipelineInfo.pDynamicState(dynamicState);
                pipelineInfo.stageCount(stageCount);
                pipelineInfo.pStages(shaderStageCreateInfos);
                pipelineInfo.pVertexInputState(vertexInputInfo);
                pipelineInfo.pNext(pipelineRenderingCreateInfoKHR);
            }


            LongBuffer pGraphicsPipeline = stack.mallocLong(1);


            if(vkCreateGraphicsPipelines(device, VK_NULL_HANDLE, pipelineInfo, null, pGraphicsPipeline) != VK_SUCCESS) {
                throw new SkyRuntimeException("Failed to create graphics pipeline");
            }


            graphicsPipelineHandle = pGraphicsPipeline.get(0);


        }

        return new VulkanPipeline(this, pipelineLayoutHandle, graphicsPipelineHandle);
    }

    private ReflectedResourcesInfo getResources(int type, long shaderResourcesHandle) {
        try(MemoryStack stack = stackPush()) {
            PointerBuffer pResourceList = stack.callocPointer(1), pResourceCount = stack.callocPointer(1);

            spvc_resources_get_resource_list_for_type(shaderResourcesHandle, type, pResourceList, pResourceCount);
            return new ReflectedResourcesInfo((int) pResourceCount.get(0), pResourceList.get(0), type);
        }
    }

    private static int getBufferSize(long compiler, long structTypeHandle) {

        int memberCount = spvc_type_get_num_member_types(structTypeHandle);

        long size = 0;

        try(MemoryStack stack = stackPush()) {
            for (int i = 0; i < memberCount; i++) {
                IntBuffer pOffset = stack.callocInt(1);
                spvc_compiler_type_struct_member_offset(compiler, structTypeHandle, i, pOffset);

                PointerBuffer pSize = stack.callocPointer(1);
                spvc_compiler_get_declared_struct_member_size(compiler, structTypeHandle, i, pSize);
                size += pSize.get(0);

            }
        }

        if(size == 0) size = spvc_type_get_vector_size(structTypeHandle);


        return (int) size;
    }

    private class ReflectedResourcesInfo {
        public int count;
        public long start;
        public int type;

        public ReflectedResourcesInfo(int count, long start, int type) {
            this.count = count;
            this.start = start;
            this.type = type;
        }
    }
    @Override
    public void add(Asset<byte[]> bytecode, ShaderType shaderType) {
        shaders.put(shaderType, bytecode.getObject());
        try(MemoryStack stack = stackPush()) {

            long compiler;
            long shaderResourcesHandle;

            {
                IntBuffer spirvData = stack.bytes(bytecode.getObject()).asIntBuffer();

                PointerBuffer pIR = stack.callocPointer(1);
                PointerBuffer pCompiler = stack.callocPointer(1);

                spvc_context_parse_spirv(context, spirvData, bytecode.getObject().length / Integer.BYTES, pIR);
                spvc_context_create_compiler(context, SPVC_BACKEND_HLSL, pIR.get(0), SPVC_CAPTURE_MODE_COPY, pCompiler);

                compiler = pCompiler.get(0);

                PointerBuffer pCompilerOptions = stack.callocPointer(1);
                spvc_compiler_create_compiler_options(compiler, pCompilerOptions);

                long compilerOptions = pCompilerOptions.get(0);

                spvc_compiler_options_set_uint(compilerOptions, SPVC_COMPILER_OPTION_HLSL_SHADER_MODEL, 51);
                spvc_compiler_install_compiler_options(compiler, compilerOptions);


                PointerBuffer pShaderResources = stack.callocPointer(1);
                spvc_compiler_create_shader_resources(compiler, pShaderResources);

                shaderResourcesHandle = pShaderResources.get(0);
            }

            //Generate all descriptor spec information including sets and bindings
            {

                ReflectedResourcesInfo[] resourcesInfos = {
                    getResources(SPVC_RESOURCE_TYPE_STORAGE_BUFFER, shaderResourcesHandle),
                    getResources(SPVC_RESOURCE_TYPE_UNIFORM_BUFFER, shaderResourcesHandle),
                    getResources(SPVC_RESOURCE_TYPE_SAMPLED_IMAGE, shaderResourcesHandle),
                    getResources(SPVC_RESOURCE_TYPE_STORAGE_IMAGE, shaderResourcesHandle),
                    getResources(SPVC_RESOURCE_TYPE_PUSH_CONSTANT, shaderResourcesHandle),
                    getResources(SPVC_RESOURCE_TYPE_SEPARATE_IMAGE, shaderResourcesHandle),
                    getResources(SPVC_RESOURCE_TYPE_SEPARATE_SAMPLERS, shaderResourcesHandle)
                };

                for (ReflectedResourcesInfo resourcesInfo : resourcesInfos) {
                    SpvcReflectedResource.Buffer spvcReflectedResources = SpvcReflectedResource.create(resourcesInfo.start, resourcesInfo.count);


                    for (int i = 0; i < resourcesInfo.count; i++) {
                        SpvcReflectedResource spvcReflectedResource = spvcReflectedResources.get(i);

                        if (resourcesInfo.type != SPVC_RESOURCE_TYPE_PUSH_CONSTANT) {

                            int set = spvc_compiler_get_decoration(compiler, spvcReflectedResource.id(), SpvDecorationDescriptorSet);
                            int binding = spvc_compiler_get_decoration(compiler, spvcReflectedResource.id(), SpvDecorationBinding);
                            String name = spvc_compiler_get_name(compiler, spvcReflectedResource.id());

                            DescriptorSet descriptorSet = getDescriptorSetSpecBySetNum(set);
                            if (descriptorSet == null) {
                                descriptorSet = new DescriptorSet(set);
                                descriptorSetsSpec.add(descriptorSet);
                            }

                            Descriptor descriptor = getDescriptorSpecByBindingNum(binding, descriptorSet);
                            if (descriptor == null) {
                                //TODO(Shayan): All stages? Bleh. Might be worth looking into specializing this every time a shader uses an existing descriptor
                                descriptor = new Descriptor(name, binding, getDescriptorType(resourcesInfo.type), Descriptor.ShaderStage.AllStages);
                                descriptorSet.addDescriptor(descriptor);
                            }

                            if (resourcesInfo.type == SPVC_RESOURCE_TYPE_STORAGE_BUFFER || resourcesInfo.type == SPVC_RESOURCE_TYPE_UNIFORM_BUFFER) {
                                long typeHandle = spvc_compiler_get_type_handle(compiler, spvcReflectedResource.type_id());
                                //The first entry in the buffer is used to read
                                long bufferStructTypeHandle = spvc_compiler_get_type_handle(
                                        compiler,
                                        spvc_type_get_member_type(typeHandle, 0)
                                );
                                descriptor.sizeBytes(getBufferSize(compiler, bufferStructTypeHandle));
                            } else if (resourcesInfo.type == SPVC_RESOURCE_TYPE_SEPARATE_IMAGE ||
                                    resourcesInfo.type == SPVC_RESOURCE_TYPE_SEPARATE_SAMPLERS ||
                                    resourcesInfo.type == SPVC_RESOURCE_TYPE_STORAGE_IMAGE ||
                                    resourcesInfo.type == SPVC_RESOURCE_TYPE_SAMPLED_IMAGE) {
                                long typeHandle = spvc_compiler_get_type_handle(
                                        compiler,
                                        spvcReflectedResource.type_id()
                                );


                                //Only one dimensional arrays are supported rn
                                if (spvc_type_get_num_array_dimensions(typeHandle) == 1)
                                    descriptor.count(spvc_type_get_array_dimension(typeHandle, 0));
                            }
                        }
                        else {
                            long typeHandle = spvc_compiler_get_type_handle(compiler, spvcReflectedResource.base_type_id());
                            PointerBuffer pSize = stack.callocPointer(1);
                            spvc_compiler_get_declared_struct_size(compiler, typeHandle, pSize);
                            pushConstantsSizeBytes = (int) pSize.get(0);
                        }

                    }
                }
            }

            //Get stage specific information (vertex attributes/attachment count/etc)
            {

                if(shaderType == ShaderType.VertexShader) {
                    ReflectedResourcesInfo stageInputsInfo = getResources(SPVC_RESOURCE_TYPE_STAGE_INPUT, shaderResourcesHandle);
                    SpvcReflectedResource.Buffer spvcReflectedResources = SpvcReflectedResource.create(stageInputsInfo.start, stageInputsInfo.count);

                    for (int i = 0; i < stageInputsInfo.count; i++) {
                        SpvcReflectedResource spvcReflectedResource = spvcReflectedResources.get(i);
                        long resourceTypeHandle = spvc_compiler_get_type_handle(compiler, spvcReflectedResource.type_id());
                        long baseType = spvc_type_get_basetype(resourceTypeHandle);
                        int columnCount = spvc_type_get_vector_size(resourceTypeHandle);

                        if(baseType == SPVC_BASETYPE_FP32) {
                            vertexAttributes.add(new VertexAttribute(spvcReflectedResource.nameString(), columnCount));
                        }
                    }


                }
                else if(shaderType == ShaderType.FragmentShader) {
                    ReflectedResourcesInfo stageOutputsInfo = getResources(SPVC_RESOURCE_TYPE_STAGE_OUTPUT, shaderResourcesHandle);
                    SpvcReflectedResource.Buffer spvcReflectedResources = SpvcReflectedResource.create(stageOutputsInfo.start, stageOutputsInfo.count);

                    for (int i = 0; i < stageOutputsInfo.count; i++) {
                        SpvcReflectedResource spvcReflectedResource = spvcReflectedResources.get(i);
                        long resourceTypeHandle = spvc_compiler_get_type_handle(compiler, spvcReflectedResource.type_id());
                        long baseType = spvc_type_get_basetype(resourceTypeHandle);
                        int columnCount = spvc_type_get_vector_size(resourceTypeHandle);

                        if(baseType == SPVC_BASETYPE_FP32) {
                            if(columnCount == 4) {


                                if(spvcReflectedResource.nameString().endsWith("SW"))
                                    attachmentTextureFormatTypes.add(i, TextureFormatType.ColorR8G8B8A8);
                                else
                                    attachmentTextureFormatTypes.add(i, Session.isDiscrete() ? TextureFormatType.ColorR32G32B32A32 : TextureFormatType.ColorR16G16B16A16);
                            }
                        }
                    }
                }


            }


            stageCount++;
        }
    }

    @Override
    public void assemble() {
        try(MemoryStack stack = stackPush()) {

            descriptorSetsSpec.sort(Comparator.comparingInt(DescriptorSet::getSetNum));
            for(DescriptorSet descriptorSet : descriptorSetsSpec) {
                descriptorSet.getDescriptors().sort(Comparator.comparingInt(Descriptor::getBinding));
            }

            VkDescriptorPoolCreateInfo descriptorPoolCreateInfo = VkDescriptorPoolCreateInfo.calloc(stack);

            //Configure the descriptor pools
            {


                int totalDescriptorCountPerFrame = 0;

                for (DescriptorSet descriptorSet : descriptorSetsSpec) {
                    totalDescriptorCountPerFrame += descriptorSet.getDescriptors().size();
                }




                VkDescriptorPoolSize.Buffer descriptorPoolSizes = VkDescriptorPoolSize.calloc(totalDescriptorCountPerFrame, stack);


                int poolSizeIndex = 0;
                for (DescriptorSet descriptorSet : descriptorSetsSpec) {

                    for (Descriptor descriptor : descriptorSet.getDescriptors()) {
                        VkDescriptorPoolSize poolSize = descriptorPoolSizes.get(poolSizeIndex);
                        poolSize.type(VulkanUtil.getVulkanDescriptorType(descriptor.getType()));
                        poolSize.descriptorCount(descriptor.getCount());

                        poolSizeIndex++;
                    }
                }

                descriptorPoolCreateInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO);
                descriptorPoolCreateInfo.pPoolSizes(descriptorPoolSizes);
                descriptorPoolCreateInfo.maxSets(descriptorSetsSpec.size());


                for (int i = 0; i < VulkanRenderer.FRAMES_IN_FLIGHT; i++) {
                    LongBuffer pDescriptorPool = stack.callocLong(1);
                    if (vkCreateDescriptorPool(VulkanRuntime.getCurrentDevice(), descriptorPoolCreateInfo, null, pDescriptorPool) != VK_SUCCESS) {
                        throw new SkyRuntimeException("Failed to create descriptor pool");
                    }
                    descriptorPoolHandles.add(i, pDescriptorPool.get(0));
                }

            }

            //Configure descriptor set layout bindings and layouts
            {
                for(DescriptorSet descriptorSet : descriptorSetsSpec) {
                    VkDescriptorSetLayoutBinding.Buffer descriptorSetLayoutBindings = VkDescriptorSetLayoutBinding.calloc(descriptorSet.getDescriptors().size(), stack);
                    List<Descriptor> descriptors = descriptorSet.getDescriptors();
                    for (int i = 0; i < descriptors.size(); i++) {
                        Descriptor descriptor = descriptors.get(i);

                        VkDescriptorSetLayoutBinding binding = descriptorSetLayoutBindings.get(i);
                        binding.descriptorType(VulkanUtil.getVulkanDescriptorType(descriptor.getType()));
                        binding.stageFlags(VK_SHADER_STAGE_ALL);
                        binding.binding(descriptor.getBinding());
                        binding.descriptorCount(descriptor.getCount());
                    }

                    for (int frameIndex = 0; frameIndex < VulkanRenderer.FRAMES_IN_FLIGHT; frameIndex++) {
                        descriptorSetLayoutHandles.add(new ArrayList<>());
                        descriptorSetHandles.add(new ArrayList<>());


                        LongBuffer pDescriptorSetLayout = stack.callocLong(1);



                        VkDescriptorSetLayoutCreateInfo descriptorSetLayoutCreateInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack);
                        descriptorSetLayoutCreateInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO);
                        descriptorSetLayoutCreateInfo.pBindings(descriptorSetLayoutBindings);

                        VkDescriptorSetLayoutBindingFlagsCreateInfo descriptorSetLayoutBindingFlagsCreateInfo = VkDescriptorSetLayoutBindingFlagsCreateInfo.calloc(stack);
                        descriptorSetLayoutBindingFlagsCreateInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_BINDING_FLAGS_CREATE_INFO);
                        descriptorSetLayoutBindingFlagsCreateInfo.pBindingFlags(stack.ints(IntStream.generate(() -> VK_DESCRIPTOR_BINDING_PARTIALLY_BOUND_BIT)
                                .limit(descriptorSetLayoutBindings.capacity())
                                .toArray()));
                        descriptorSetLayoutBindingFlagsCreateInfo.bindingCount(descriptorSetLayoutBindings.capacity());

                        descriptorSetLayoutCreateInfo.pNext(descriptorSetLayoutBindingFlagsCreateInfo);
                        if (vkCreateDescriptorSetLayout(VulkanRuntime.getCurrentDevice(), descriptorSetLayoutCreateInfo, null, pDescriptorSetLayout) != VK_SUCCESS) {
                            throw new SkyRuntimeException("Failed to create descriptor set layout");
                        }

                        long descriptorSetLayout = pDescriptorSetLayout.get(0);

                        descriptorSetLayoutHandles.get(frameIndex).add(descriptorSetLayout);

                        VkDescriptorSetAllocateInfo descriptorSetAllocateInfo = VkDescriptorSetAllocateInfo.calloc(stack);
                        descriptorSetAllocateInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO);
                        descriptorSetAllocateInfo.descriptorPool(descriptorPoolHandles.get(frameIndex));
                        descriptorSetAllocateInfo.pSetLayouts(pDescriptorSetLayout);

                        LongBuffer pDescriptorSet = stack.callocLong(1);
                        if (vkAllocateDescriptorSets(VulkanRuntime.getCurrentDevice(), descriptorSetAllocateInfo, pDescriptorSet) != VK_SUCCESS) {
                            throw new SkyRuntimeException("Failed to allocate descriptor set");
                        }
                        VulkanUtil.nameObject("DescriptorSet " + descriptorSet.getSetNum(), VK_OBJECT_TYPE_DESCRIPTOR_SET, pDescriptorSet.get(0), stack);
                        descriptorSetHandles.get(frameIndex).add(pDescriptorSet.get(0));
                    }

                }




            }

            //Generate shader stages
            {
                shaderStageCreateInfos = VkPipelineShaderStageCreateInfo.calloc(stageCount, stack);

                int stageIndex = 0;

                for(ShaderType shaderType : shaders.keySet()) {
                    byte[] bytecode = shaders.get(shaderType);

                    VkShaderModuleCreateInfo createInfo = VkShaderModuleCreateInfo.calloc(stack);

                    createInfo.sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO);
                    createInfo.pCode(stack.bytes(bytecode));

                    LongBuffer pShaderModule = stack.mallocLong(1);

                    if(vkCreateShaderModule(VulkanRuntime.getCurrentDevice(), createInfo, null, pShaderModule) != VK_SUCCESS) {
                        throw new SkyRuntimeException("Failed to create " + shaderType + " shader module");
                    }

                    long shaderModule = pShaderModule.get(0);
                    shaderModuleHandles.add(shaderModule);

                    VkPipelineShaderStageCreateInfo shaderStageCreateInfo = shaderStageCreateInfos.get(stageIndex);

                    shaderStageCreateInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
                    shaderStageCreateInfo.stage(VulkanUtil.getVulkanShaderStage(shaderType));
                    shaderStageCreateInfo.module(shaderModule);
                    shaderStageCreateInfo.pName(stack.UTF8("main"));

                    stageIndex++;
                }
                programType = shaders.containsKey(ShaderType.ComputeShader) ? ShaderPipelineType.ComputePipeline : ShaderPipelineType.GraphicsPipeline;

                if(programType == ShaderPipelineType.GraphicsPipeline)
                    pipeline = createGraphicsPipeline(VulkanRuntime.getCurrentDevice());
                else
                    pipeline = createComputePipeline(VulkanRuntime.getCurrentDevice());
            }
        }
    }



    public void setBuffers(int frameIndex, DescriptorUpdate<Buffer>... bufferUpdates){
        try(MemoryStack stack = stackPush()) {

            VkWriteDescriptorSet.Buffer descriptorSetsWrites = VkWriteDescriptorSet.calloc(bufferUpdates.length, stack);
            for (int i = 0; i < bufferUpdates.length; i++) {
                DescriptorUpdate<Buffer> bufferUpdate = bufferUpdates[i];
                VulkanBuffer buffer = (VulkanBuffer) bufferUpdate.getResource();

                VkDescriptorBufferInfo.Buffer bufferInfo = VkDescriptorBufferInfo.calloc(1, stack);
                bufferInfo.buffer(buffer.getHandle());
                bufferInfo.offset(0);
                bufferInfo.range(buffer.getSizeBytes());

                Descriptor descriptor = getDescriptorByName(bufferUpdate.getName());
                VkWriteDescriptorSet descriptorSetsWrite = descriptorSetsWrites.get(i);

                long descriptorSetHandle = descriptorSetHandles.get(frameIndex).get(descriptor.getDescriptorSet().getSetNum());

                descriptorSetsWrite.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
                descriptorSetsWrite.dstSet(descriptorSetHandle);
                descriptorSetsWrite.dstBinding(descriptor.getBinding());
                descriptorSetsWrite.dstArrayElement(0);
                descriptorSetsWrite.descriptorType(VulkanUtil.getVulkanDescriptorType(descriptor.getType()));
                descriptorSetsWrite.pBufferInfo(bufferInfo);
                descriptorSetsWrite.descriptorCount(bufferUpdate.getUpdateCount());
                VulkanUtil.nameObject(bufferUpdate.getName(), VK_OBJECT_TYPE_BUFFER, buffer.getHandle(), stack);
            }

            vkUpdateDescriptorSets(VulkanRuntime.getCurrentDevice(), descriptorSetsWrites, null);
        }
    }
    public void setCombinedTextureSamplers(int frameIndex, DescriptorUpdate<Pair<Texture, Sampler>>... combinedTextureSamplerUpdates){
        try(MemoryStack stack = stackPush()) {

            VkWriteDescriptorSet.Buffer descriptorSetsWrites = VkWriteDescriptorSet.calloc(combinedTextureSamplerUpdates.length, stack);
            for (int i = 0; i < combinedTextureSamplerUpdates.length; i++) {
                DescriptorUpdate<Pair<Texture, Sampler>> combinedTextureSamplerUpdate = combinedTextureSamplerUpdates[i];

                VulkanTexture texture = ((VulkanTexture) combinedTextureSamplerUpdate.getResource().key);
                VulkanSampler sampler = ((VulkanSampler) combinedTextureSamplerUpdate.getResource().value);

                VkDescriptorImageInfo.Buffer descriptorImageInfo = VkDescriptorImageInfo.calloc(1, stack);

                //todo(shayan) Sometimes fragment shaders in the rendergraph
                // will want to use VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL on a storage image
                // but our descriptor update doesn't know that and will still use VK_IMAGE_LAYOUT_GENERAL
                // which throws a validation error, maybe update descriptors after the graph has processed resources?
                // maybe in a separate graphFinished() function
                descriptorImageInfo.imageLayout(VK_IMAGE_LAYOUT_GENERAL);
                descriptorImageInfo.imageView(texture.getImageView().getHandle());
                descriptorImageInfo.sampler(sampler.getHandle());

                Descriptor descriptor = getDescriptorByName(combinedTextureSamplerUpdate.getName());

                assert descriptor.getType() == Descriptor.Type.CombinedTextureSampler
                        : "Descriptor of type " + descriptor.getType() + " is not a combined image sampler";

                VkWriteDescriptorSet descriptorSetsWrite = descriptorSetsWrites.get(i);

                long descriptorSetHandle = descriptorSetHandles.get(frameIndex).get(descriptor.getDescriptorSet().getSetNum());

                descriptorSetsWrite.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
                descriptorSetsWrite.dstSet(descriptorSetHandle);
                descriptorSetsWrite.dstBinding(descriptor.getBinding());
                descriptorSetsWrite.dstArrayElement(combinedTextureSamplerUpdate.getArrayIndex());
                descriptorSetsWrite.descriptorType(VulkanUtil.getVulkanDescriptorType(descriptor.getType()));
                descriptorSetsWrite.pImageInfo(descriptorImageInfo);
                descriptorSetsWrite.descriptorCount(combinedTextureSamplerUpdate.getUpdateCount());

                VulkanUtil.nameObject(combinedTextureSamplerUpdate.getName(), VK_OBJECT_TYPE_IMAGE, texture.getImage().getHandle(), stack);

            }

            vkUpdateDescriptorSets(VulkanRuntime.getCurrentDevice(), descriptorSetsWrites, null);

        }
    }

    @Override
    public void setTextures(int frameIndex, DescriptorUpdate<Texture>... textureUpdates) {
        try(MemoryStack stack = stackPush()) {

            VkWriteDescriptorSet.Buffer descriptorSetsWrites = VkWriteDescriptorSet.calloc(textureUpdates.length, stack);
            for (int i = 0; i < textureUpdates.length; i++) {
                DescriptorUpdate<Texture> textureUpdate = textureUpdates[i];

                VulkanTexture texture = ((VulkanTexture) textureUpdate.getResource());

                VkDescriptorImageInfo.Buffer descriptorImageInfo = VkDescriptorImageInfo.calloc(1, stack);

                //todo(shayan) Sometimes fragment shaders in the rendergraph
                // will want to use VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL on a storage image
                // but our descriptor update doesn't know that and will still use VK_IMAGE_LAYOUT_GENERAL
                // which throws a validation error, maybe update descriptors after the graph has processed resources?
                // maybe in a separate graphFinished() function
                descriptorImageInfo.imageLayout(VK_IMAGE_LAYOUT_GENERAL);
                descriptorImageInfo.imageView(texture.getImageView().getHandle());

                Descriptor descriptor = getDescriptorByName(textureUpdate.getName());
                VkWriteDescriptorSet descriptorSetsWrite = descriptorSetsWrites.get(i);

                long descriptorSetHandle = descriptorSetHandles.get(frameIndex).get(descriptor.getDescriptorSet().getSetNum());

                descriptorSetsWrite.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
                descriptorSetsWrite.dstSet(descriptorSetHandle);
                descriptorSetsWrite.dstBinding(descriptor.getBinding());
                descriptorSetsWrite.dstArrayElement(textureUpdate.getArrayIndex());
                descriptorSetsWrite.descriptorType(VulkanUtil.getVulkanDescriptorType(descriptor.getType()));
                descriptorSetsWrite.pImageInfo(descriptorImageInfo);
                descriptorSetsWrite.descriptorCount(textureUpdate.getUpdateCount());

                VulkanUtil.nameObject(textureUpdate.getName(), VK_OBJECT_TYPE_IMAGE, texture.getImage().getHandle(), stack);

            }

            vkUpdateDescriptorSets(VulkanRuntime.getCurrentDevice(), descriptorSetsWrites, null);

        }
    }

    @Override
    public void setSamplers(int frameIndex, DescriptorUpdate<Sampler>... samplerUpdates) {
        try(MemoryStack stack = stackPush()) {

            VkWriteDescriptorSet.Buffer descriptorSetsWrites = VkWriteDescriptorSet.calloc(samplerUpdates.length, stack);
            for (int i = 0; i < samplerUpdates.length; i++) {
                DescriptorUpdate<Sampler> samplerUpdate = samplerUpdates[i];

                VulkanSampler sampler = (VulkanSampler) samplerUpdate.getResource();

                VkDescriptorImageInfo.Buffer descriptorImageInfo = VkDescriptorImageInfo.calloc(1, stack);
                descriptorImageInfo.sampler(sampler.getHandle());

                Descriptor descriptor = getDescriptorByName(samplerUpdate.getName());
                VkWriteDescriptorSet descriptorSetsWrite = descriptorSetsWrites.get(i);

                long descriptorSetHandle = descriptorSetHandles.get(frameIndex).get(descriptor.getDescriptorSet().getSetNum());

                descriptorSetsWrite.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
                descriptorSetsWrite.dstSet(descriptorSetHandle);
                descriptorSetsWrite.dstBinding(descriptor.getBinding());
                descriptorSetsWrite.dstArrayElement(samplerUpdate.getArrayIndex());
                descriptorSetsWrite.descriptorType(VulkanUtil.getVulkanDescriptorType(descriptor.getType()));
                descriptorSetsWrite.pImageInfo(descriptorImageInfo);
                descriptorSetsWrite.descriptorCount(samplerUpdate.getUpdateCount());

                VulkanUtil.nameObject(samplerUpdate.getName(), VK_OBJECT_TYPE_SAMPLER, sampler.getHandle(), stack);

            }

            vkUpdateDescriptorSets(VulkanRuntime.getCurrentDevice(), descriptorSetsWrites, null);

        }
    }

    private Descriptor.Type getDescriptorType(int spvcResourceType) {
        switch (spvcResourceType) {
            case SPVC_RESOURCE_TYPE_STORAGE_BUFFER -> { return Descriptor.Type.ShaderStorageBuffer; }
            case SPVC_RESOURCE_TYPE_UNIFORM_BUFFER -> { return Descriptor.Type.UniformBuffer; }
            case SPVC_RESOURCE_TYPE_SAMPLED_IMAGE -> { return Descriptor.Type.CombinedTextureSampler; }
            case SPVC_RESOURCE_TYPE_STORAGE_IMAGE -> { return Descriptor.Type.StorageTexture; }
            case SPVC_RESOURCE_TYPE_SEPARATE_IMAGE -> { return Descriptor.Type.SeparateTexture; }
            case SPVC_RESOURCE_TYPE_SEPARATE_SAMPLERS -> { return Descriptor.Type.SeparateSampler; }
        }

        return null;
    }
    public VulkanPipeline getPipeline() {
        return pipeline;
    }
    private Descriptor getDescriptorSpecByBindingNum(int binding, DescriptorSet descriptorSet) {
        for(Descriptor descriptor : descriptorSet.getDescriptors()) {
            if(descriptor.getBinding() == binding) {
                return descriptor;
            }
        }
        return null;
    }
    private DescriptorSet getDescriptorSetSpecBySetNum(int set) {
        for (DescriptorSet descriptorSet : descriptorSetsSpec) {
            if (descriptorSet.getSetNum() == set) return descriptorSet;
        }

        return null;
    }
    public long[] getDescriptorSetsHandles(int frameIndex) {
        return descriptorSetHandles.get(frameIndex).stream().mapToLong(i -> i).toArray();
    }
    public long[] getDescriptorSetLayoutHandles(int frameIndex){
        return descriptorSetLayoutHandles.get(frameIndex).stream().mapToLong(i -> i).toArray();
    }
    public long[] getAllDescriptorSetLayoutHandles(){
        List<Long> allDescriptorSetLayouts = new ArrayList<>();

        for(List<Long> descriptorSetLayouts : descriptorSetLayoutHandles){
            allDescriptorSetLayouts.addAll(descriptorSetLayouts);
        }

        return allDescriptorSetLayouts.stream().mapToLong(i -> i).toArray();
    }



    @Override
    public void dispose() {

        vkDeviceWaitIdle(VulkanRuntime.getCurrentDevice());

        for(long shaderModuleHandle : shaderModuleHandles) {
            vkDestroyShaderModule(VulkanRuntime.getCurrentDevice(), shaderModuleHandle, null);
        }

        for(List<Long> frameDescriptorSetLayouts : descriptorSetLayoutHandles){
            for(long descriptorSetLayouts : frameDescriptorSetLayouts)
                vkDestroyDescriptorSetLayout(VulkanRuntime.getCurrentDevice(), descriptorSetLayouts, null);
        }

        for(long descriptorPoolHandle : descriptorPoolHandles)
            vkDestroyDescriptorPool(VulkanRuntime.getCurrentDevice(), descriptorPoolHandle, null);

        spvc_context_destroy(context);
        spvcErrorCallback.free();
    }

}
