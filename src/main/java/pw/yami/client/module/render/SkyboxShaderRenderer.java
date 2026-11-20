package pw.yami.client.module.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.io.File;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;
import pw.yami.client.model.render.SkyboxShaderMode;

public class SkyboxShaderRenderer {
   public static final SkyboxShaderRenderer INSTANCE = new SkyboxShaderRenderer();

   private final Map<SkyboxShaderMode, ProgramInfo> programs = new HashMap<>();
   private final Set<SkyboxShaderMode> failedModes = new HashSet<>();
   private int vao = -1;
   private int vbo = -1;
   private boolean glInitialized = false;
   private final long startTime = System.currentTimeMillis();

   private static class ProgramInfo {
      final int programId;
      final int uColor;
      final int uResolution;
      final int uMouse;
      final int uScale;
      final int uTime;
      final int uInvVP;
      final int uWorldSpace;

      ProgramInfo(int programId, int uColor, int uResolution, int uMouse, int uScale, int uTime, int uInvVP, int uWorldSpace) {
         this.programId = programId;
         this.uColor = uColor;
         this.uResolution = uResolution;
         this.uMouse = uMouse;
         this.uScale = uScale;
         this.uTime = uTime;
         this.uInvVP = uInvVP;
         this.uWorldSpace = uWorldSpace;
      }
   }

   private void initGL() {
      if (this.glInitialized) {
         return;
      }

      try {
         this.vao = GL30C.glGenVertexArrays();
         this.vbo = GL15C.glGenBuffers();

         GL30C.glBindVertexArray(this.vao);
         GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, this.vbo);

         FloatBuffer buffer = BufferUtils.createFloatBuffer(12);
         buffer.put(new float[]{
            -1.0F, -1.0F,
             1.0F, -1.0F,
            -1.0F,  1.0F,
            -1.0F,  1.0F,
             1.0F, -1.0F,
             1.0F,  1.0F
         });
         buffer.flip();

         GL15C.glBufferData(GL15C.GL_ARRAY_BUFFER, buffer, GL15C.GL_STATIC_DRAW);
         GL20C.glEnableVertexAttribArray(0);
         GL20C.glVertexAttribPointer(0, 2, GL20C.GL_FLOAT, false, 0, 0L);

         GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, 0);
         GL30C.glBindVertexArray(0);

         this.glInitialized = true;
      } catch (Throwable t) {
         System.err.println("[Skybox] Failed to init OpenGL VAO/VBO: " + t.getMessage());
      }
   }

   private int compileShader(int type, String source) {
      int shader = GL20C.glCreateShader(type);
      GL20C.glShaderSource(shader, source);
      GL20C.glCompileShader(shader);
      int status = GL20C.glGetShaderi(shader, GL20C.GL_COMPILE_STATUS);
      if (status == 0) {
         String log = GL20C.glGetShaderInfoLog(shader);
         GL20C.glDeleteShader(shader);
         throw new RuntimeException("Shader compilation failed: " + log);
      }
      return shader;
   }

   private ProgramInfo getOrCompile(SkyboxShaderMode mode) {
      if (mode == SkyboxShaderMode.None || this.failedModes.contains(mode)) {
         return null;
      }
      if (this.programs.containsKey(mode)) {
         return this.programs.get(mode);
      }

      String fileName = this.getShaderFileName(mode);
      if (fileName == null) {
         this.failedModes.add(mode);
         return null;
      }

      try {
         String fragSource = this.loadShaderSource(fileName);
         if (fragSource == null || fragSource.trim().isEmpty()) {
            System.err.println("[Skybox] Shader source not found or empty: " + fileName);
            this.failedModes.add(mode);
            return null;
         }

         int versionIdx = fragSource.indexOf("#version");
         if (versionIdx > 0) {
            fragSource = fragSource.substring(versionIdx);
         }

         if (fragSource.startsWith("#version 330")) {
            fragSource = "#version 150\n" + fragSource.substring(fragSource.indexOf('\n') + 1);
         }

         String vertSource = "#version 150\n"
            + "in vec2 position;\n"
            + "void main() {\n"
            + "    gl_Position = vec4(position, 0.0, 1.0);\n"
            + "}\n";

         int vertShader = this.compileShader(GL20C.GL_VERTEX_SHADER, vertSource);
         int fragShader = this.compileShader(GL20C.GL_FRAGMENT_SHADER, fragSource);

         int program = GL20C.glCreateProgram();
         GL20C.glAttachShader(program, vertShader);
         GL20C.glAttachShader(program, fragShader);
         GL20C.glBindAttribLocation(program, 0, "position");
         GL20C.glLinkProgram(program);

         int status = GL20C.glGetProgrami(program, GL20C.GL_LINK_STATUS);
         if (status == 0) {
            String log = GL20C.glGetProgramInfoLog(program);
            GL20C.glDeleteProgram(program);
            throw new RuntimeException("Shader program link failed: " + log);
         }

         int uColor = GL20C.glGetUniformLocation(program, "u_Color");
         int uResolution = GL20C.glGetUniformLocation(program, "u_Resolution");
         int uMouse = GL20C.glGetUniformLocation(program, "u_Mouse");
         int uScale = GL20C.glGetUniformLocation(program, "u_Scale");
         int uTime = GL20C.glGetUniformLocation(program, "u_Time");
         int uInvVP = GL20C.glGetUniformLocation(program, "u_InvVP");
         int uWorldSpace = GL20C.glGetUniformLocation(program, "u_WorldSpace");

         ProgramInfo info = new ProgramInfo(program, uColor, uResolution, uMouse, uScale, uTime, uInvVP, uWorldSpace);
         this.programs.put(mode, info);
         System.out.println("[Skybox] Successfully compiled shader: " + fileName);
         return info;
      } catch (Throwable t) {
         System.err.println("[Skybox] Error compiling shader " + fileName + ": " + t.getMessage());
         this.failedModes.add(mode);
         return null;
      }
   }

   private String getShaderFileName(SkyboxShaderMode mode) {
      switch (mode) {
         case Default:
            return "sky.frag";
         case BlurpleAurora:
            return "BlurpleAurora.sky.frag";
         case CrimsonAurora:
            return "CrimsonAurora.sky.frag";
         case RainbowAurora:
            return "RainbowAurora.sky.frag";
         default:
            return null;
      }
   }

   private String loadShaderSource(String fileName) {
      String[] searchPaths = new String[]{
         "/assets/yamiclient/skybox/" + fileName,
         "/assets/hachimiclient/skybox/" + fileName,
         "assets/yamiclient/skybox/" + fileName
      };

      for (String p : searchPaths) {
         try (InputStream is = SkyboxShaderRenderer.class.getResourceAsStream(p)) {
            if (is != null) {
               byte[] bytes = is.readAllBytes();
               return new String(bytes, StandardCharsets.UTF_8);
            }
         } catch (Throwable ignored) {
         }

         try (InputStream is = SkyboxShaderRenderer.class.getClassLoader().getResourceAsStream(p.startsWith("/") ? p.substring(1) : p)) {
            if (is != null) {
               byte[] bytes = is.readAllBytes();
               return new String(bytes, StandardCharsets.UTF_8);
            }
         } catch (Throwable ignored) {
         }
      }

      File f = new File("src/main/resources/assets/yamiclient/skybox/" + fileName);
      if (f.exists()) {
         try {
            return Files.readString(f.toPath(), StandardCharsets.UTF_8);
         } catch (Throwable ignored) {
         }
      }

      return null;
   }

   public void render(
      SkyboxShaderMode mode,
      Color skyColor,
      float scale,
      float speed,
      Camera camera,
      boolean worldSpace,
      Matrix4f modelViewMatrix,
      Matrix4f projectionMatrix
   ) {
      if (mode == SkyboxShaderMode.None) {
         return;
      }

      this.initGL();
      if (!this.glInitialized) {
         return;
      }

      ProgramInfo program = this.getOrCompile(mode);
      if (program == null) {
         return;
      }

      MinecraftClient client = MinecraftClient.getInstance();
      if (client == null || client.getWindow() == null) {
         return;
      }

      int width = client.getWindow().getFramebufferWidth();
      int height = client.getWindow().getFramebufferHeight();
      if (width <= 0 || height <= 0) {
         return;
      }

      int prevProgram = GL11C.glGetInteger(GL20C.GL_CURRENT_PROGRAM);
      int prevVao = GL11C.glGetInteger(GL30C.GL_VERTEX_ARRAY_BINDING);
      int prevArrayBuffer = GL11C.glGetInteger(GL15C.GL_ARRAY_BUFFER_BINDING);

      RenderSystem.disableCull();
      RenderSystem.depthMask(false);
      RenderSystem.disableDepthTest();
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();

      GL20C.glUseProgram(program.programId);

      if (program.uResolution != -1) {
         GL20C.glUniform2f(program.uResolution, (float)width, (float)height);
      }

      if (program.uTime != -1) {
         float time = (float)((System.currentTimeMillis() - this.startTime) % 10000000L) / 1000.0F * speed;
         GL20C.glUniform1f(program.uTime, time);
      }

      if (program.uColor != -1 && skyColor != null) {
         GL20C.glUniform4f(
            program.uColor,
            skyColor.getRed() / 255.0F,
            skyColor.getGreen() / 255.0F,
            skyColor.getBlue() / 255.0F,
            Math.max(0.01F, skyColor.getAlpha() / 255.0F)
         );
      }

      if (program.uScale != -1) {
         GL20C.glUniform1f(program.uScale, scale);
      }

      if (program.uMouse != -1) {
         GL20C.glUniform2f(program.uMouse, 0.0F, 0.0F);
      }

      if (program.uWorldSpace != -1) {
         GL20C.glUniform1i(program.uWorldSpace, worldSpace ? 1 : 0);
      }

      if (program.uInvVP != -1 && modelViewMatrix != null && projectionMatrix != null) {
         try {
            Matrix4f invVP = new Matrix4f(projectionMatrix).mul(modelViewMatrix).invert();
            FloatBuffer buf = BufferUtils.createFloatBuffer(16);
            invVP.get(buf);
            GL20C.glUniformMatrix4fv(program.uInvVP, false, buf);
         } catch (Throwable t) {
            // fallback
         }
      }

      GL30C.glBindVertexArray(this.vao);
      GL20C.glDrawArrays(GL20C.GL_TRIANGLES, 0, 6);

      GL30C.glBindVertexArray(prevVao);
      GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, prevArrayBuffer);
      GL20C.glUseProgram(prevProgram);

      RenderSystem.enableDepthTest();
      RenderSystem.depthMask(true);
      RenderSystem.enableCull();
   }
}
