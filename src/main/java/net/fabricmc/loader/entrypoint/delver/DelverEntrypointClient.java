/*
 * Copyright 2016 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.fabricmc.loader.entrypoint.delver;

import net.fabricmc.loader.entrypoint.EntrypointPatch;
import net.fabricmc.loader.entrypoint.EntrypointTransformer;
import net.fabricmc.loader.launch.common.FabricLauncher;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.ListIterator;
import java.util.function.Consumer;

public class DelverEntrypointClient extends EntrypointPatch {

	public DelverEntrypointClient (EntrypointTransformer transformer) {
		super(transformer);
	}

	@Override
	public void process (FabricLauncher launcher, Consumer<ClassNode> classEmitter) {
		try {
			String entryPoint = launcher.getEntrypoint();
			ClassNode mainClass = loadClass(launcher, entryPoint);

			MethodNode mainMethod = findMethod(mainClass, (method) -> method.name.equals("main"));
			if (mainMethod == null) {
				throw new RuntimeException("Could not find main method in " + entryPoint + "!");
			}

			ListIterator<AbstractInsnNode> it = mainMethod.instructions.iterator();
			moveBefore(it, Opcodes.RETURN);

			it.add(new InsnNode(Opcodes.ACONST_NULL));
			it.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "net/fabricmc/loader/entrypoint/applet/AppletMain", "hookGameDir", "(Ljava/io/File;)Ljava/io/File;", false));
			it.add(new VarInsnNode(Opcodes.ALOAD, 0));
			it.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "net/fabricmc/loader/entrypoint/minecraft/hooks/EntrypointClient", "start", "(Ljava/io/File;Ljava/lang/Object;)V", false));

			classEmitter.accept(mainClass);

			EntrypointTransformer.appletMainClass = entryPoint;
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
