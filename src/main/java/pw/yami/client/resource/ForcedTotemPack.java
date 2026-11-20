package pw.yami.client.resource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import net.minecraft.registry.VersionedIdentifier;
import net.minecraft.resource.InputSupplier;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourcePackInfo;
import net.minecraft.resource.ResourcePackSource;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.metadata.ResourceMetadataReader;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ForcedTotemPack implements ResourcePack {
   public static final ForcedTotemPack INSTANCE = new ForcedTotemPack();
   private final ResourcePackInfo info = new ResourcePackInfo("yami_totem", Text.literal("Yami Forced Totem"), ResourcePackSource.BUILTIN, Optional.empty());

   @Override
   public InputSupplier<InputStream> openRoot(String... segments) {
      return null;
   }

   @Override
   public InputSupplier<InputStream> open(ResourceType type, Identifier id) {
      if (type == ResourceType.CLIENT_RESOURCES && id.getNamespace().equals("minecraft")) {
         if (id.getPath().equals("textures/item/totem_of_undying.png")) {
            return () -> ForcedTotemPack.class.getResourceAsStream("/assets/minecraft/textures/item/totem_of_undying.png");
         }
         if (id.getPath().equals("textures/item/totem_of_undying.png.mcmeta")) {
            return () -> ForcedTotemPack.class.getResourceAsStream("/assets/minecraft/textures/item/totem_of_undying.png.mcmeta");
         }
         if (id.getPath().equals("models/item/totem_of_undying.json")) {
            byte[] modelBytes = "{\"parent\":\"minecraft:item/generated\",\"textures\":{\"layer0\":\"minecraft:item/totem_of_undying\"}}".getBytes(StandardCharsets.UTF_8);
            return () -> new ByteArrayInputStream(modelBytes);
         }
      }
      return null;
   }

   @Override
   public void findResources(ResourceType type, String namespace, String prefix, ResultConsumer consumer) {
      if (type == ResourceType.CLIENT_RESOURCES && "minecraft".equals(namespace)) {
         if (prefix.isEmpty() || "textures".startsWith(prefix) || "textures/item".startsWith(prefix) || prefix.startsWith("textures")) {
            consumer.accept(Identifier.of("minecraft", "textures/item/totem_of_undying.png"), open(type, Identifier.of("minecraft", "textures/item/totem_of_undying.png")));
         }
      }
   }

   @Override
   public Set<String> getNamespaces(ResourceType type) {
      return Set.of("minecraft");
   }

   @Override
   public <T> T parseMetadata(ResourceMetadataReader<T> metaReader) {
      return null;
   }

   @Override
   public ResourcePackInfo getInfo() {
      return this.info;
   }

   @Override
   public String getId() {
      return "yami_totem";
   }

   @Override
   public Optional<VersionedIdentifier> getKnownPackInfo() {
      return Optional.empty();
   }

   @Override
   public void close() {
   }
}
