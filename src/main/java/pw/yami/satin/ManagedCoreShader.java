package pw.yami.satin;

import net.minecraft.client.gl.ShaderProgram;
import pw.yami.satin.uniform.UniformFinder;

public interface ManagedCoreShader extends UniformFinder {
   ShaderProgram getProgram();

   void release();
}
