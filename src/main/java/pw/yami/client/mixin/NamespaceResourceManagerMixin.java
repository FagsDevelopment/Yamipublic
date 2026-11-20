package pw.yami.client.mixin;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.resource.InputSupplier;
import net.minecraft.resource.NamespaceResourceManager;
import net.minecraft.resource.Resource;
import net.minecraft.resource.metadata.ResourceMetadata;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pw.yami.client.resource.ForcedTotemPack;

@Mixin(NamespaceResourceManager.class)
public abstract class NamespaceResourceManagerMixin {
   private static final Identifier TOTEM_PNG = Identifier.of("minecraft", "textures/item/totem_of_undying.png");
   private static final Identifier TOTEM_MCMETA = Identifier.of("minecraft", "textures/item/totem_of_undying.png.mcmeta");
   private static final Identifier TOTEM_MODEL = Identifier.of("minecraft", "models/item/totem_of_undying.json");
   private static final byte[] VANILLA_TOTEM_MODEL = "{\"parent\":\"minecraft:item/generated\",\"textures\":{\"layer0\":\"minecraft:item/totem_of_undying\"}}".getBytes(StandardCharsets.UTF_8);

   @Inject(method = "getResource", at = @At("HEAD"), cancellable = true)
   private void yami$forceTotemResource(Identifier id, CallbackInfoReturnable<Optional<Resource>> cir) {
      if (id.getNamespace().equals("minecraft")) {
         if (id.getPath().equals("textures/item/totem_of_undying.png")) {
            InputSupplier<InputStream> inputSupplier = () -> NamespaceResourceManagerMixin.class.getResourceAsStream("/assets/minecraft/textures/item/totem_of_undying.png");
            InputSupplier<ResourceMetadata> metadataSupplier = () -> {
               try (InputStream is = NamespaceResourceManagerMixin.class.getResourceAsStream("/assets/minecraft/textures/item/totem_of_undying.png.mcmeta")) {
                  if (is != null) {
                     return ResourceMetadata.create(is);
                  }
               } catch (Exception ignored) {}
               return ResourceMetadata.NONE;
            };
            cir.setReturnValue(Optional.of(new Resource(ForcedTotemPack.INSTANCE, inputSupplier, metadataSupplier)));
         } else if (id.getPath().equals("textures/item/totem_of_undying.png.mcmeta")) {
            InputSupplier<InputStream> inputSupplier = () -> NamespaceResourceManagerMixin.class.getResourceAsStream("/assets/minecraft/textures/item/totem_of_undying.png.mcmeta");
            cir.setReturnValue(Optional.of(new Resource(ForcedTotemPack.INSTANCE, inputSupplier)));
         } else if (id.getPath().equals("models/item/totem_of_undying.json")) {
            InputSupplier<InputStream> inputSupplier = () -> new ByteArrayInputStream(VANILLA_TOTEM_MODEL);
            cir.setReturnValue(Optional.of(new Resource(ForcedTotemPack.INSTANCE, inputSupplier)));
         }
      }
   }

   @Inject(method = "getAllResources", at = @At("HEAD"), cancellable = true)
   private void yami$forceAllTotemResources(Identifier id, CallbackInfoReturnable<List<Resource>> cir) {
      if (id.getNamespace().equals("minecraft") && id.getPath().equals("textures/item/totem_of_undying.png")) {
         InputSupplier<InputStream> inputSupplier = () -> NamespaceResourceManagerMixin.class.getResourceAsStream("/assets/minecraft/textures/item/totem_of_undying.png");
         InputSupplier<ResourceMetadata> metadataSupplier = () -> {
            try (InputStream is = NamespaceResourceManagerMixin.class.getResourceAsStream("/assets/minecraft/textures/item/totem_of_undying.png.mcmeta")) {
               if (is != null) {
                  return ResourceMetadata.create(is);
               }
            } catch (Exception ignored) {}
            return ResourceMetadata.NONE;
         };
         cir.setReturnValue(List.of(new Resource(ForcedTotemPack.INSTANCE, inputSupplier, metadataSupplier)));
      }
   }

   @Inject(method = "findResources", at = @At("RETURN"))
   private void yami$forceFindResources(String startingPath, Predicate<Identifier> allowedPathPredicate, CallbackInfoReturnable<Map<Identifier, Resource>> cir) {
      Map<Identifier, Resource> map = cir.getReturnValue();
      if (map != null && (startingPath.contains("textures") || startingPath.isEmpty())) {
         InputSupplier<InputStream> inputSupplier = () -> NamespaceResourceManagerMixin.class.getResourceAsStream("/assets/minecraft/textures/item/totem_of_undying.png");
         InputSupplier<ResourceMetadata> metadataSupplier = () -> {
            try (InputStream is = NamespaceResourceManagerMixin.class.getResourceAsStream("/assets/minecraft/textures/item/totem_of_undying.png.mcmeta")) {
               if (is != null) {
                  return ResourceMetadata.create(is);
               }
            } catch (Exception ignored) {}
            return ResourceMetadata.NONE;
         };
         map.put(TOTEM_PNG, new Resource(ForcedTotemPack.INSTANCE, inputSupplier, metadataSupplier));
      }
   }
}
