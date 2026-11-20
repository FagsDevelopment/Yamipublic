package pw.yami.client.model.render;

import pw.yami.client.render.IdentifierNativeImageBackedTextureManager;

public record TextureWidthTextureHeight(int textureWidth, int textureHeight, int width, int height, char value, IdentifierNativeImageBackedTextureManager owner) {
}
